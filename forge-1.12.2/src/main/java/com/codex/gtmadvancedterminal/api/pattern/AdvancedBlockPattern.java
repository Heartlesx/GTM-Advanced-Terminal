package com.codex.gtmadvancedterminal.api.pattern;

import com.codex.gtmadvancedterminal.api.ae.AeItemSource;
import com.codex.gtmadvancedterminal.common.item.AutoBuildSetting;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.BlockWorldState;
import gregtech.api.pattern.TraceabilityPredicate;
import gregtech.api.pattern.TraceabilityPredicate.SimplePredicate;
import gregtech.api.util.BlockInfo;
import gregtech.api.util.RelativeDirection;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * 增强版多方块自动建造。
 * 在 1.12.2 原生 BlockPattern#autoBuild 的基础上增加四项能力：
 * 自定义可重复结构次数、线圈等级选择、无仓室模式、线圈替换模式；材料来源额外支持 AE 网络。
 * 结构数据用反射从原 pattern 读取（blockMatches / centerOffset 不可从外部直接访问）。
 */
public class AdvancedBlockPattern extends BlockPattern {

    private static final EnumFacing[] FACINGS = { EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.WEST,
            EnumFacing.EAST, EnumFacing.UP, EnumFacing.DOWN };
    private static final Logger LOGGER = LogManager.getLogger("gtmadvancedterminal");

    private final int[] centerOffset;
    /** 附属结构分组：组名 → 序号（按组名排序），每次建造前重算。 */
    private final Map<String, Integer> optionalGroups = new TreeMap<>();

    public AdvancedBlockPattern(TraceabilityPredicate[][][] blockMatches, RelativeDirection[] structureDir,
                                int[][] aisleRepetitions, int[] centerOffset) {
        super(blockMatches, structureDir, aisleRepetitions);
        this.centerOffset = centerOffset;
    }

    public static AdvancedBlockPattern from(BlockPattern pattern) {
        if (pattern == null) {
            return null;
        }
        try {
            Field blockMatchesField = BlockPattern.class.getDeclaredField("blockMatches");
            blockMatchesField.setAccessible(true);
            TraceabilityPredicate[][][] blockMatches = (TraceabilityPredicate[][][]) blockMatchesField.get(pattern);

            Field centerOffsetField = BlockPattern.class.getDeclaredField("centerOffset");
            centerOffsetField.setAccessible(true);
            int[] centerOffset = (int[]) centerOffsetField.get(pattern);

            return new AdvancedBlockPattern(blockMatches, pattern.structureDir, pattern.aisleRepetitions, centerOffset);
        } catch (Exception e) {
            // 结构数据取不到（GT 改过内部字段）时返回 null，调用方跳过这次建造。
            return null;
        }
    }

    public int autoBuild(EntityPlayer player, MultiblockControllerBase controller, AutoBuildSetting setting,
                         boolean mirrored) {
        World world = player.world;
        BlockPos centerPos = controller.getPos();
        EnumFacing facing = controller.getFrontFacing().getOpposite();
        EnumFacing upwardsFacing = controller.getUpwardsFacing();
        // 镜像模式强制按 GT 的 flip 布局建造；不开启时保持原行为（跟随控制器当前是否已 flip）
        boolean isFlipped = mirrored || controller.isFlipped();

        BlockWorldState worldState = new BlockWorldState();
        Map<SimplePredicate, Integer> cacheGlobal = new HashMap<>();
        Map<BlockPos, Object> blocks = new HashMap<>();
        blocks.put(centerPos, controller);
        int placedCount = 0;
        // 诊断统计（定位 placed 0 时卡在哪一步）
        int scanned = 0;
        int occupied = 0;
        int noCandidates = 0;
        int noMaterial = 0;
        int placeFailed = 0;
        ItemStack firstMissing = null;
        // 可重复结构段：按设置值裁剪到 [min, max]；min == max 时固定为 min（与 1.20.1 版一致）
        int[] repeat = new int[this.fingerLength];
        for (int h = 0; h < this.fingerLength; h++) {
            int min = this.aisleRepetitions[h][0];
            int max = this.aisleRepetitions[h][1];
            repeat[h] = min != max ? Math.max(min, Math.min(max, setting.getRepeatCount())) : min;
        }

        // 起点层必须按本次“实际放置的重复数”反推。原版取的 -centerOffset[4] 是按最大重复数算出来的，
        // 中心层之前存在可重复结构时会整体平移 (max - 实际重复数) 格：大型火箭引擎（控制器在最后一段、
        // 前面有 2..16 段可重复）会被造到 14 格之外，并且连控制器一起复制一份完整的机器。
        int z = 0;
        for (int c = 0; c < this.centerOffset[2]; c++) {
            z -= repeat[c];
        }
        LOGGER.info("[GTM-AT] pattern: finger={} thumb={} palm={} centerOffset={} aisleRepetitions={} repeat={} startZ={} flip={} upwardsFacing={}",
                this.fingerLength, this.thumbLength, this.palmLength, Arrays.toString(this.centerOffset),
                Arrays.deepToString(this.aisleRepetitions), Arrays.toString(repeat), z, isFlipped, upwardsFacing);

        // 附属结构分组：先把整张结构表扫一遍，把没有候选表的谓词按捕获的组名（GTLite 的
        // optionalStates 第一个参数）排序编号，设置值 = 只搭前 N 组，0 就是只搭主体。
        optionalGroups.clear();
        collectOptionalGroups();
        LOGGER.info("[GTM-AT] optional structure groups: {} (allow {})", optionalGroups,
                setting.getAdditionalStructureCount());

        for (int c = 0; c < this.fingerLength; c++) {
            for (int r = 0; r < repeat[c]; r++) {
                Map<SimplePredicate, Integer> cacheLayer = new HashMap<>();
                for (int b = 0, y = -this.centerOffset[1]; b < this.thumbLength; b++, y++) {
                    for (int a = 0, x = -this.centerOffset[0]; a < this.palmLength; a++, x++) {
                        TraceabilityPredicate predicate = this.blockMatches[c][b][a];
                        BlockPos pos = RelativeDirection
                                .setActualRelativeOffset(x, y, z, facing, upwardsFacing, isFlipped, this.structureDir)
                                .add(centerPos);
                        scanned++;
                        worldState.update(world, pos, this.matchContext, this.globalCount, this.layerCount,
                                predicate);

                        // 已经被占用的格子（与原版判定一致：材质不可替换才算占用，草/雪/流体仍可建造）
                        ItemStack replacedCoil = null;
                        IBlockState existing = world.getBlockState(pos);
                        if (!existing.getMaterial().isReplaceable()) {
                            if (setting.isReplaceCoilMode() && AutoBuildSetting.isHeatingCoil(existing)) {
                                replacedCoil = new ItemStack(existing.getBlock(), 1,
                                        existing.getBlock().getMetaFromState(existing));
                            } else {
                                blocks.put(pos, existing);
                                occupied++;
                                for (SimplePredicate limit : predicate.limited) {
                                    limit.testLimited(worldState);
                                }
                                continue;
                            }
                        }

                        BlockInfo[] infos = collectCandidates(predicate, setting, cacheGlobal, cacheLayer, worldState);
                        if (infos.length == 0) {
                            noCandidates++;
                            continue;
                        }
                        List<ItemStack> candidates = setting.apply(infos);
                        if (candidates.isEmpty()) {
                            noCandidates++;
                            continue;
                        }
                        if (replacedCoil != null && ItemStack.areItemsEqual(candidates.get(0), replacedCoil)) {
                            // 该位置已经是指定等级的线圈，无需替换
                            blocks.put(pos, world.getBlockState(pos));
                            continue;
                        }

                        // 材料来源：创造模式直接取用（与原版 autoBuild 一致）；生存模式背包优先，
                        // 勾选了 AE 供给且背包里没有时再从无线终端所连网络提取。
                        ItemStack found = null;
                        if (player.isCreative()) {
                            for (int i = candidates.size() - 1; i >= 0; i--) {
                                ItemStack candidate = candidates.get(i);
                                if (!candidate.isEmpty() && candidate.getItem() instanceof ItemBlock) {
                                    found = candidate.copy();
                                    break;
                                }
                            }
                        } else {
                            found = findInInventory(player, candidates);
                            if (found == null && setting.getIsUseAE() == 1) {
                                found = AeItemSource.extract(player, candidates);
                            }
                        }
                        if (found == null) {
                            noMaterial++;
                            if (firstMissing == null) {
                                firstMissing = candidates.get(0);
                            }
                            continue;
                        }

                        if (replacedCoil != null) {
                            // 回收旧线圈；背包放不下就跳过这次替换（与 1.20.1 版行为一致）
                            if (!player.inventory.addItemStackToInventory(replacedCoil)) {
                                continue;
                            }
                            world.setBlockToAir(pos);
                        }

                        // 放置：不能走 ItemBlock#onItemUse —— 它取的是玩家手持物品（会吞掉终端），
                        // 并用玩家手持物品的 metadata 去算方块状态（放出的会是错误变体）。
                        // 改为 placeBlockAt：元数据与 NBT 都取自候选物品本身，且不动玩家背包。
                        found.setCount(1);
                        ItemBlock itemBlock = (ItemBlock) found.getItem();
                        IBlockState placedState = itemBlock.getBlock()
                                .getStateFromMeta(itemBlock.getMetadata(found.getMetadata()));
                        blocks.put(pos, placedState);
                        if (!itemBlock.placeBlockAt(found, player, world, pos, EnumFacing.UP, 0.5F, 0.5F, 0.5F,
                                placedState)) {
                            placeFailed++;
                            continue;
                        }
                        blocks.put(pos, world.getBlockState(pos));
                        placedCount++;
                    }
                }
                z++;
            }
        }

        fixFrontFacings(world, controller, blocks);
        LOGGER.info("[GTM-AT] scan summary: scanned={} occupied={} noCandidates={} noMaterial={} placeFailed={} placed={} firstMissing={}",
                scanned, occupied, noCandidates, noMaterial, placeFailed, placedCount,
                firstMissing == null ? "-" : firstMissing.getDisplayName());
        if (placedCount == 0 && firstMissing != null) {
            // 一个方块都没放下去时明确告诉玩家缺什么，避免"毫无反应"的困惑
            player.sendMessage(new TextComponentTranslation(
                    "message.gtmadvancedterminal.missing_material", firstMissing.getDisplayName()));
        }
        return placedCount;
    }

    /** 候选方块收集：沿用原版 autoBuild 的三段式顺序，并套用无仓室过滤。 */
    private BlockInfo[] collectCandidates(TraceabilityPredicate predicate, AutoBuildSetting setting,
                                          Map<SimplePredicate, Integer> cacheGlobal,
                                          Map<SimplePredicate, Integer> cacheLayer,
                                          BlockWorldState worldState) {
        BlockInfo[] infos = new BlockInfo[0];
        boolean find = false;

        for (SimplePredicate limit : predicate.limited) {
            if (limit.minLayerCount > 0) {
                BlockInfo[] limitCandidates = candidatesOf(limit, setting);
                if (!setting.isPlaceHatch(limitCandidates)) {
                    continue;
                }
                Integer curr = cacheLayer.get(limit);
                if (curr == null) {
                    cacheLayer.put(limit, 1);
                } else {
                    if (curr >= limit.minLayerCount || (limit.maxLayerCount != -1 && curr >= limit.maxLayerCount)) {
                        continue;
                    }
                    cacheLayer.put(limit, curr + 1);
                }
                infos = limitCandidates;
                find = true;
                break;
            }
        }
        if (!find) {
            for (SimplePredicate limit : predicate.limited) {
                if (limit.minGlobalCount > 0) {
                    BlockInfo[] limitCandidates = candidatesOf(limit, setting);
                    if (!setting.isPlaceHatch(limitCandidates)) {
                        continue;
                    }
                    Integer curr = cacheGlobal.get(limit);
                    if (curr == null) {
                        cacheGlobal.put(limit, 1);
                        infos = limitCandidates;
                        find = true;
                        break;
                    }
                    if (curr < limit.minGlobalCount && (limit.maxGlobalCount == -1 || curr < limit.maxGlobalCount)) {
                        cacheGlobal.put(limit, curr + 1);
                        infos = limitCandidates;
                        find = true;
                        break;
                    }
                }
            }
        }
        if (!find) {
            for (SimplePredicate limit : predicate.limited) {
                BlockInfo[] limitCandidates = candidatesOf(limit, setting);
                if (!setting.isPlaceHatch(limitCandidates)) {
                    continue;
                }
                if (limit.maxLayerCount != -1
                        && cacheLayer.getOrDefault(limit, Integer.MAX_VALUE) == limit.maxLayerCount) {
                    continue;
                }
                if (limit.maxGlobalCount != -1
                        && cacheGlobal.getOrDefault(limit, Integer.MAX_VALUE) == limit.maxGlobalCount) {
                    continue;
                }
                infos = ArrayUtils.addAll(infos, limitCandidates);
                cacheLayer.merge(limit, 1, Integer::sum);
                cacheGlobal.merge(limit, 1, Integer::sum);
            }
            for (SimplePredicate common : predicate.common) {
                // 无仓室模式必须逐个谓词判断：像消声仓这种“只有一个候选”的谓词以前被漏掉，
                // 结果是无仓室模式照样放出仓室。
                BlockInfo[] commonCandidates = candidatesOf(common, setting);
                if (!setting.isPlaceHatch(commonCandidates)) {
                    continue;
                }
                infos = ArrayUtils.addAll(infos, commonCandidates);
            }
        }
        return infos == null ? new BlockInfo[0] : infos;
    }

    /**
     * 取谓词的候选方块：优先用谓词自带的候选表；没有候选表时（例如 GTLite Core 的
     * TraceabilityPredicates.optionalStates，工业土高炉的附属结构用的就是它）从谓词对象
     * 捕获的 IBlockState[] 里还原允许的方块状态，否则这些位置永远放不了东西。
     * 带组名的谓词还要受“附属结构数量”限制：只有序号 < 设置值的组才会放置。
     */
    private BlockInfo[] candidatesOf(SimplePredicate limit, AutoBuildSetting setting) {
        if (limit.candidates != null) {
            BlockInfo[] infos = limit.candidates.get();
            return infos == null ? new BlockInfo[0] : infos;
        }
        String group = groupName(limit.predicate);
        if (group != null
                && optionalGroups.getOrDefault(group, Integer.MAX_VALUE) >= setting.getAdditionalStructureCount()) {
            return new BlockInfo[0];
        }
        return inferCandidates(limit.predicate);
    }

    /** 扫描整张结构表，收集“没有候选表但带组名”的谓词，组名排序后编号。 */
    private void collectOptionalGroups() {
        Set<String> names = new TreeSet<>();
        for (TraceabilityPredicate[][] layers : this.blockMatches) {
            for (TraceabilityPredicate[] rows : layers) {
                for (TraceabilityPredicate predicate : rows) {
                    for (SimplePredicate simple : predicate.common) {
                        addGroupName(names, simple);
                    }
                    for (SimplePredicate simple : predicate.limited) {
                        addGroupName(names, simple);
                    }
                }
            }
        }
        int index = 0;
        for (String name : names) {
            optionalGroups.put(name, index++);
        }
    }

    private static void addGroupName(Set<String> names, SimplePredicate simple) {
        if (simple.candidates == null) {
            String name = groupName(simple.predicate);
            if (name != null) {
                names.add(name);
            }
        }
    }

    /** 谓词捕获的组名；只有同时捕获了 IBlockState[] 才算附属结构分组，避免误伤无关字符串。 */
    private static String groupName(Predicate<BlockWorldState> predicate) {
        if (predicate == null) {
            return null;
        }
        String name = null;
        boolean hasStates = false;
        for (Field field : predicate.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                if (field.getType() == IBlockState[].class) {
                    IBlockState[] states = (IBlockState[]) field.get(predicate);
                    hasStates = states != null && states.length > 0;
                } else if (field.getType() == String.class) {
                    name = (String) field.get(predicate);
                }
            } catch (Exception ignored) {
                // 取不到就当作没有分组
            }
        }
        return hasStates ? name : null;
    }

    private static BlockInfo[] inferCandidates(Predicate<BlockWorldState> predicate) {
        if (predicate == null) {
            return new BlockInfo[0];
        }
        for (Field field : predicate.getClass().getDeclaredFields()) {
            if (field.getType() != IBlockState[].class) {
                continue;
            }
            try {
                field.setAccessible(true);
                IBlockState[] states = (IBlockState[]) field.get(predicate);
                if (states == null || states.length == 0) {
                    continue;
                }
                BlockInfo[] infos = new BlockInfo[states.length];
                for (int i = 0; i < states.length; i++) {
                    infos[i] = new BlockInfo(states[i]);
                }
                return infos;
            } catch (Exception ignored) {
                // 取不到就按没有候选处理（与原版行为一致）
            }
        }
        return new BlockInfo[0];
    }

    private ItemStack findInInventory(EntityPlayer player, List<ItemStack> candidates) {
        // 两遍匹配：先严格（含 NBT），再宽松（只比物品 + metadata），兼容带 NBT 的方块物品
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
                ItemStack invStack = player.inventory.mainInventory.get(i);
                if (invStack.isEmpty() || !(invStack.getItem() instanceof ItemBlock)) {
                    continue;
                }
                for (ItemStack candidate : candidates) {
                    boolean match = pass == 0
                            ? ItemStack.areItemsEqual(candidate, invStack)
                                    && ItemStack.areItemStackTagsEqual(candidate, invStack)
                            : ItemStack.areItemsEqual(candidate, invStack);
                    if (match) {
                        return invStack.splitStack(1);
                    }
                }
            }
        }
        return null;
    }

    /** 放置完成后统一修正机器正面，与原版 autoBuild 的收尾逻辑一致。 */
    private void fixFrontFacings(World world, MultiblockControllerBase controller, Map<BlockPos, Object> blocks) {
        EnumFacing[] facings = ArrayUtils.addAll(new EnumFacing[] { controller.getFrontFacing() }, FACINGS);
        for (BlockPos pos : blocks.keySet()) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (!(tileEntity instanceof IGregTechTileEntity)) {
                continue;
            }
            MetaTileEntity metaTileEntity = ((IGregTechTileEntity) tileEntity).getMetaTileEntity();
            if (metaTileEntity == null || metaTileEntity == controller) {
                continue;
            }
            boolean found = false;
            for (EnumFacing enumFacing : facings) {
                if (metaTileEntity.isValidFrontFacing(enumFacing) && !blocks.containsKey(pos.offset(enumFacing))) {
                    metaTileEntity.setFrontFacing(enumFacing);
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (EnumFacing enumFacing : FACINGS) {
                    if (world.isAirBlock(pos.offset(enumFacing)) && metaTileEntity.isValidFrontFacing(enumFacing)) {
                        metaTileEntity.setFrontFacing(enumFacing);
                        break;
                    }
                }
            }
        }
    }
}

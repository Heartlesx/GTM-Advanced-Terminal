package com.codex.gtmadvancedterminal.common.item;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.HandGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;

import com.codex.gtmadvancedterminal.api.pattern.AdvancedBlockPattern;

import gregtech.api.GregTechAPI;
import gregtech.api.items.gui.ItemUIFactory;
import gregtech.api.items.metaitem.stats.IItemBehaviour;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.mui.GTGuiTextures;
import gregtech.api.mui.GTGuis;
import gregtech.api.mui.factory.MetaItemGuiFactory;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * 高级终端的行为：
 * 潜行右键多方块控制器 = 按设置自动建造（已成型且开了线圈替换模式时替换线圈）；
 * 右键空中 = 打开设置界面。NBT 键与 1.20.1 版保持一致。
 */
public class AdvancedTerminalBehavior implements IItemBehaviour, ItemUIFactory {

    private static final String LANG_PREFIX = "item.gtmadvancedterminal.advanced_terminal";
    private static final Logger LOGGER = LogManager.getLogger("gtmadvancedterminal");

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                           float hitX, float hitY, float hitZ, EnumHand hand) {
        if (!player.isSneaking()) {
            // 非潜行不拦截，交给 GT 机器自身的右键行为（打开机器界面等）
            return EnumActionResult.PASS;
        }
        TileEntity tileEntity = world.getTileEntity(pos);
        if (!(tileEntity instanceof IGregTechTileEntity)) {
            return EnumActionResult.PASS;
        }
        MetaTileEntity metaTileEntity = ((IGregTechTileEntity) tileEntity).getMetaTileEntity();
        if (!(metaTileEntity instanceof MultiblockControllerBase)) {
            return EnumActionResult.PASS;
        }
        MultiblockControllerBase controller = (MultiblockControllerBase) metaTileEntity;
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        ItemStack stack = player.getHeldItem(hand);
        if (!player.canPlayerEdit(pos, side, stack)) {
            return EnumActionResult.FAIL;
        }
        AutoBuildSetting setting = AutoBuildSetting.read(stack);
        boolean formed = controller.isStructureFormed();
        boolean mirrored = setting.isMirrorMode();
        LOGGER.info("[GTM-AT] build request: controller={} formed={} coilTier={} repeat={} noHatch={} replaceCoil={} useAE={} additional={} mirror={}",
                metaTileEntity.getClass().getSimpleName(), formed, setting.getCoilTier(), setting.getRepeatCount(),
                setting.getNoHatchMode(), setting.getReplaceCoilMode(), setting.getIsUseAE(),
                setting.getAdditionalStructureCount(), mirrored);
        if (mirrored && formed) {
            // 镜像布局与现有布局的格子不重合，硬搭会把两套布局的并集搭出来，所以直接拒绝
            player.sendMessage(new TextComponentTranslation("message.gtmadvancedterminal.mirror_formed"));
            return EnumActionResult.SUCCESS;
        }
        if (mirrored && !controller.allowsFlip()) {
            // 机器禁止镜像布局：提示后按普通布局建造，避免点了没反应
            player.sendMessage(new TextComponentTranslation("message.gtmadvancedterminal.mirror_unsupported"));
            mirrored = false;
        }
        // 已成型的机器不再直接跳过：只补空地（occupied 的格子不动），方便分阶段补附属结构等部件。
        AdvancedBlockPattern pattern = AdvancedBlockPattern.from(controller.structurePattern);
        if (pattern == null) {
            LOGGER.warn("[GTM-AT] failed to read structure pattern via reflection, falling back to vanilla autoBuild");
            if (!formed && controller.structurePattern != null) {
                controller.structurePattern.autoBuild(player, controller);
            }
            return EnumActionResult.SUCCESS;
        }
        int placed = pattern.autoBuild(player, controller, setting, mirrored);
        LOGGER.info("[GTM-AT] autoBuild finished, placed {} blocks", placed);
        // 只有真的补了方块才重新检测：让新补的附属结构计入加成，空点一下不打断运行中的机器。
        if (formed && placed > 0) {
            controller.invalidateStructure();
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            MetaItemGuiFactory.open(player, hand);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(ItemStack stack, List<String> lines) {
        AutoBuildSetting setting = AutoBuildSetting.read(stack);
        lines.add(I18n.format(LANG_PREFIX + ".tooltip.usage"));
        lines.add(I18n.format(LANG_PREFIX + ".setting.1") + ": " + setting.getCoilTier());
        lines.add(I18n.format(LANG_PREFIX + ".setting.2") + ": " + setting.getRepeatCount());
        lines.add(I18n.format(LANG_PREFIX + ".setting.3") + ": " + setting.getNoHatchMode());
        lines.add(I18n.format(LANG_PREFIX + ".setting.4") + ": " + setting.getReplaceCoilMode());
        lines.add(I18n.format(LANG_PREFIX + ".setting.5") + ": " + setting.getIsUseAE());
        lines.add(I18n.format(LANG_PREFIX + ".setting.6") + ": " + setting.getAdditionalStructureCount());
        lines.add(I18n.format(LANG_PREFIX + ".setting.7") + ": " + (setting.isMirrorMode() ? 1 : 0));
    }

    @Override
    public ModularPanel buildUI(HandGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
        ItemStack stack = guiData.getMainHandItem();
        ModularPanel panel = GTGuis.createPanel(stack, 176, 172).background(GTGuiTextures.BACKGROUND);

        int coilCount = Math.max(1, GregTechAPI.HEATING_COILS.size());

        // sync handler 必须交给 widget 树自动注册（WidgetTree.collectSyncValues）。
        // 手动 syncManager.syncValue(...) 会与自动注册产生 "auto" 标志冲突，
        // 在 GuiManager.open 时抛 IllegalStateException，导致界面打不开。
        IntSyncValue coilTier = createSyncValue(stack, AutoBuildSetting.NBT_COIL_TIER, 0);
        IntSyncValue repeatCount = createSyncValue(stack, AutoBuildSetting.NBT_REPEAT_COUNT, 0);
        IntSyncValue noHatchMode = createSyncValue(stack, AutoBuildSetting.NBT_NO_HATCH_MODE, 1);
        IntSyncValue replaceCoilMode = createSyncValue(stack, AutoBuildSetting.NBT_REPLACE_COIL_MODE, 0);
        IntSyncValue isUseAE = createSyncValue(stack, AutoBuildSetting.NBT_IS_USE_AE, 0);
        IntSyncValue additionalStructureCount = createSyncValue(stack,
                AutoBuildSetting.NBT_ADDITIONAL_STRUCTURE_COUNT, 0);
        IntSyncValue mirrorMode = createSyncValue(stack, AutoBuildSetting.NBT_MIRROR_MODE, 0);

        panel.child(new TextWidget<>(IKey.lang(LANG_PREFIX + ".setting.title")).pos(8, 6));

        addSettingRow(panel, 0, "1", coilTier, coilCount + 1);
        addSettingRow(panel, 1, "2", repeatCount, 100);
        addSettingRow(panel, 2, "3", noHatchMode, 2);
        addSettingRow(panel, 3, "4", replaceCoilMode, 2);
        addSettingRow(panel, 4, "5", isUseAE, 2);
        addSettingRow(panel, 5, "6", additionalStructureCount, 5);
        addSettingRow(panel, 6, "7", mirrorMode, 2);

        return panel;
    }

    private static IntSyncValue createSyncValue(ItemStack stack, String nbtKey, int defaultValue) {
        return new IntSyncValue(
                () -> AutoBuildSetting.get(stack, nbtKey, defaultValue),
                v -> AutoBuildSetting.set(stack, nbtKey, v));
    }

    private static void addSettingRow(ModularPanel panel, int row, String keySuffix, IntSyncValue value,
                                      int stateCount) {
        int y = 22 + row * 20;
        panel.child(new TextWidget<>(IKey.lang(LANG_PREFIX + ".setting." + keySuffix)).pos(8, y + 4));
        CycleButtonWidget button = new CycleButtonWidget()
                .value(value)
                .stateCount(stateCount)
                .pos(120, y)
                .size(48, 16);
        // 每个状态都用同一份动态文本，按钮上始终显示当前值
        for (int i = 0; i < stateCount; i++) {
            button.stateOverlay(i, IKey.dynamic(() -> String.valueOf(value.getIntValue())));
        }
        panel.child(button);
    }
}

package com.codex.gtmadvancedterminal.common.item;

import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.util.BlockInfo;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * 高级终端的五项建造设置。
 * NBT 键与 1.20.1 版完全一致（CoilTier / RepeatCount / NoHatchMode / ReplaceCoilMode / IsUseAE），
 * 默认值也与 1.20.1 版一致：NoHatchMode 默认 1，其余默认 0。
 */
public class AutoBuildSetting {

    public static final String NBT_COIL_TIER = "CoilTier";
    public static final String NBT_REPEAT_COUNT = "RepeatCount";
    public static final String NBT_NO_HATCH_MODE = "NoHatchMode";
    public static final String NBT_REPLACE_COIL_MODE = "ReplaceCoilMode";
    public static final String NBT_IS_USE_AE = "IsUseAE";
    public static final String NBT_ADDITIONAL_STRUCTURE_COUNT = "AdditionalStructureCount";
    public static final String NBT_MIRROR_MODE = "MirrorMode";

    private int coilTier;
    private int repeatCount;
    private int noHatchMode = 1;
    private int replaceCoilMode;
    private int isUseAE;
    private int additionalStructureCount;
    private int mirrorMode;

    public static AutoBuildSetting read(ItemStack stack) {
        AutoBuildSetting setting = new AutoBuildSetting();
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return setting;
        }
        setting.coilTier = tag.getInteger(NBT_COIL_TIER);
        setting.repeatCount = tag.getInteger(NBT_REPEAT_COUNT);
        setting.noHatchMode = tag.hasKey(NBT_NO_HATCH_MODE) ? tag.getInteger(NBT_NO_HATCH_MODE) : 1;
        setting.replaceCoilMode = tag.getInteger(NBT_REPLACE_COIL_MODE);
        setting.isUseAE = tag.getInteger(NBT_IS_USE_AE);
        setting.additionalStructureCount = tag.getInteger(NBT_ADDITIONAL_STRUCTURE_COUNT);
        setting.mirrorMode = tag.getInteger(NBT_MIRROR_MODE);
        return setting;
    }

    public static int get(ItemStack stack, String key, int defaultValue) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey(key) ? tag.getInteger(key) : defaultValue;
    }

    public static void set(ItemStack stack, String key, int value) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(key, value);
    }

    public int getCoilTier() {
        return coilTier;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public int getNoHatchMode() {
        return noHatchMode;
    }

    public int getReplaceCoilMode() {
        return replaceCoilMode;
    }

    public int getIsUseAE() {
        return isUseAE;
    }

    /** 允许搭建的附属结构组数：按组名排序取前 N 组，0 = 只搭主体。 */
    public int getAdditionalStructureCount() {
        return additionalStructureCount;
    }

    /** 镜像模式：按 GT 的 flip 布局建造（水平机器表现为左右镜像）。 */
    public boolean isMirrorMode() {
        return mirrorMode == 1;
    }

    public boolean isReplaceCoilMode() {
        return replaceCoilMode == 1;
    }

    /**
     * 按线圈等级从候选方块里挑出要放置的物品。语义与 1.20.1 版一致：
     * 线圈位置的候选按等级排序，CoilTier 为 0 时排除最后一个（最高等级），否则取第 CoilTier-1 个。
     */
    public List<ItemStack> apply(BlockInfo[] infos) {
        List<ItemStack> candidates = new ArrayList<>();
        if (infos == null) {
            return candidates;
        }
        boolean isCoil = false;
        for (BlockInfo info : infos) {
            if (info != null && GregTechAPI.HEATING_COILS.containsKey(info.getBlockState())) {
                isCoil = true;
                break;
            }
        }
        if (isCoil) {
            int tier = Math.min(coilTier - 1, infos.length - 1);
            if (tier == -1) {
                for (int i = 0; i < infos.length - 1; i++) {
                    candidates.add(getItemStackForm(infos[i]));
                }
            } else {
                candidates.add(getItemStackForm(infos[tier]));
            }
            return candidates;
        }
        for (BlockInfo info : infos) {
            if (info != null && info.getBlockState().getBlock() != Blocks.AIR) {
                candidates.add(getItemStackForm(info));
            }
        }
        return candidates;
    }

    /** 无仓室模式：候选里只要含仓室就不允许放置（以前只看第一个候选，混合候选时会漏）。 */
    public boolean isPlaceHatch(BlockInfo[] infos) {
        if (this.noHatchMode == 0) {
            return true;
        }
        if (infos != null) {
            for (BlockInfo info : infos) {
                if (getMetaTileEntity(info) instanceof IMultiblockPart) {
                    return false;
                }
            }
        }
        return true;
    }

    public static MetaTileEntity getMetaTileEntity(BlockInfo info) {
        if (info == null || !(info.getTileEntity() instanceof IGregTechTileEntity)) {
            return null;
        }
        return ((IGregTechTileEntity) info.getTileEntity()).getMetaTileEntity();
    }

    /** 把 BlockInfo 转成物品形态；GT 机器走 getStackForm，普通方块走 Item.getItemFromBlock。 */
    public static ItemStack getItemStackForm(BlockInfo info) {
        IBlockState blockState = info.getBlockState();
        MetaTileEntity metaTileEntity = getMetaTileEntity(info);
        if (metaTileEntity != null) {
            return metaTileEntity.getStackForm();
        }
        return new ItemStack(Item.getItemFromBlock(blockState.getBlock()), 1,
                blockState.getBlock().getMetaFromState(blockState));
    }

    /** 该方块状态是不是 GT 加热线圈（用于线圈替换模式的识别）。 */
    public static boolean isHeatingCoil(IBlockState state) {
        return GregTechAPI.HEATING_COILS.containsKey(state);
    }
}

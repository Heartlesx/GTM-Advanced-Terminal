package com.codex.gtmadvancedterminal.data;

import com.codex.gtmadvancedterminal.GTMAdvancedTerminalMod;
import com.codex.gtmadvancedterminal.common.item.AdvancedTerminalBehavior;

import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.items.metaitem.StandardMetaItem;
import gregtech.common.creativetab.GTCreativeTabs;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

/**
 * 高级终端所属的 MetaItem。
 * GT 1.12.2 的 MetaItems 会在物品注册阶段遍历 MetaItem.getMetaItems()，
 * 自动 register + registerSubItems；模型注册同理（MetaItems.registerModels）。
 */
public class TerminalMetaItem extends StandardMetaItem {

    // GT 自己的 MetaItem1 占用 offset 0。这里用一个远离所有已知占用的 offset，避免 meta 值冲突。
    public static final short META_ITEM_OFFSET = 32000;

    public static TerminalMetaItem INSTANCE;

    public TerminalMetaItem() {
        super(META_ITEM_OFFSET);
    }

    public static void init() {
        INSTANCE = new TerminalMetaItem();
        INSTANCE.setRegistryName(GTMAdvancedTerminalMod.MODID, "meta_item");
    }

    @Override
    public void registerSubItems() {
        TerminalItems.ADVANCED_TERMINAL = this.addItem(0, "advanced_terminal")
                .addComponents(new AdvancedTerminalBehavior())
                .setMaxStackSize(1)
                .setCreativeTabs(GTCreativeTabs.TAB_GREGTECH_TOOLS);
    }

    /** 模型放到本模组命名空间下：assets/gtmadvancedterminal/models/item/metaitems/advanced_terminal.json。 */
    @Override
    public ResourceLocation createItemModelPath(MetaItem<?>.MetaValueItem metaValueItem, String postfix) {
        return new ResourceLocation(GTMAdvancedTerminalMod.MODID,
                "metaitems/" + metaValueItem.unlocalizedName + postfix);
    }

    /** StandardMetaItem 默认不填充创造标签，这里补上（对齐其它 MetaItem 子类的做法）。 */
    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) {
            return;
        }
        for (MetaItem<?>.MetaValueItem item : metaItems.values()) {
            if (item.isInCreativeTab(tab)) {
                items.add(item.getStackForm());
            }
        }
    }
}

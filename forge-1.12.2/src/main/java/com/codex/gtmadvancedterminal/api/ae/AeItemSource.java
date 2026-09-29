package com.codex.gtmadvancedterminal.api.ae;

import appeng.api.config.Actionable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.me.helpers.PlayerSource;
import appeng.util.item.AEItemStack;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * AE2 物品供给：从玩家主背包/饰品栏里的 AE 无线终端所连网络提取一件候选物品。
 * 语义对齐 1.20.1 版：需要身上有能绑定的无线终端，且访问点在范围内（rangeCheck）。
 */
public final class AeItemSource {

    private AeItemSource() {}

    public static ItemStack extract(EntityPlayer player, List<ItemStack> candidates) {
        List<ItemStack> sources = new ArrayList<>(player.inventory.mainInventory);
        IInventory baubles = getBaublesInventory(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSizeInventory(); i++) {
                sources.add(baubles.getStackInSlot(i));
            }
        }
        for (ItemStack stack : sources) {
            if (stack.isEmpty() || !(stack.getItem() instanceof IWirelessTermHandler)) {
                continue;
            }
            IWirelessTermHandler handler = (IWirelessTermHandler) stack.getItem();
            if (!handler.canHandle(stack)) {
                continue;
            }
            try {
                WirelessTerminalGuiObject terminal = new WirelessTerminalGuiObject(handler, stack, player, player.world,
                        (int) player.posX, (int) player.posY, (int) player.posZ);
                if (!terminal.rangeCheck()) {
                    continue;
                }
                for (ItemStack candidate : candidates) {
                    IAEItemStack request = AEItemStack.fromItemStack(candidate);
                    if (request == null) {
                        continue;
                    }
                    request.setStackSize(1);
                    IAEItemStack extracted = terminal.extractItems(request, Actionable.MODULATE,
                            new PlayerSource(player, null));
                    if (extracted != null && extracted.getStackSize() > 0) {
                        ItemStack result = extracted.createItemStack();
                        result.setCount(1);
                        return result;
                    }
                }
            } catch (Exception ignored) {
                // 无线终端未绑定 / 没电 / 维度不符等情况直接跳过，不影响其它材料来源
            }
        }
        return null;
    }

    /** Bubbles（modid baubles）的饰品栏：反射调 BaublesApi.getBaubles，避免对可选模组产生硬依赖。 */
    private static IInventory getBaublesInventory(EntityPlayer player) {
        if (!Loader.isModLoaded("baubles")) {
            return null;
        }
        try {
            Class<?> api = Class.forName("baubles.api.BaublesApi");
            Method getBaubles = api.getMethod("getBaubles", EntityPlayer.class);
            Object inventory = getBaubles.invoke(null, player);
            return inventory instanceof IInventory ? (IInventory) inventory : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}

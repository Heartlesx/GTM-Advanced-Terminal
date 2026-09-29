package com.codex.gtmadvancedterminal;

import com.codex.gtmadvancedterminal.data.TerminalMetaItem;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * GTM Advanced Terminal 的 1.12.2 版（目标整合包 GT Lite）。
 * 功能与 1.20.1 版对齐：手持高级终端潜行右键多方块控制器自动建造，右键空中打开设置界面。
 */
@Mod(modid = GTMAdvancedTerminalMod.MODID,
        name = "GTM Advanced Terminal",
        version = GTMAdvancedTerminalMod.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:gregtech")
public class GTMAdvancedTerminalMod {

    public static final String MODID = "gtmadvancedterminal";
    /** 必须与 gradle.properties 的 mod_version 保持一致。 */
    public static final String VERSION = "1.0.0-forge-1.12.2";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 只构造 MetaItem 实例并设置注册名；实际注册由 GT 的 MetaItems 注册流程统一完成
        // （GT 在 RegistryEvent.Register<Item> 里遍历 MetaItem.getMetaItems()）。
        TerminalMetaItem.init();
    }
}

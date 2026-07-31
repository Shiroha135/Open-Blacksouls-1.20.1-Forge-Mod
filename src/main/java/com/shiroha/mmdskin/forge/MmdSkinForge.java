package com.shiroha.mmdskin.forge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.shiroha.mmdskin.MmdSkin;
import com.shiroha.mmdskin.forge.register.MmdSkinRegisterCommon;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MmdSkin.MOD_ID)
public class MmdSkinForge {
    public static final Logger logger = LogManager.getLogger();

    public MmdSkinForge() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
    }

    public void preInit(FMLCommonSetupEvent event) {
        MmdSkinRegisterCommon.Register();
    }
}

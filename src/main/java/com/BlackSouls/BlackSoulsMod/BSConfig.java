package com.BlackSouls.BlackSoulsMod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BSConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static ForgeConfigSpec.BooleanValue ENABLE_ORIGINAL_WINDOW_BRANDING;
    public static ForgeConfigSpec.BooleanValue ENABLE_MMD_MODELS;
    public static ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_HEALTH_BAR;
    public static ForgeConfigSpec.BooleanValue ENABLE_ENTITY_STATUS_BAR;
    public static ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_MAIN_MENU;
    public static ForgeConfigSpec.BooleanValue ALLOW_PLAYER_EXTRA_MODES;
    public static ForgeConfigSpec.BooleanValue ENABLE_LOW_SEN_JUMPSCARE;
    public static ForgeConfigSpec.BooleanValue SHOW_COMBAT_DAMAGE_CHAT;
    public static ForgeConfigSpec.EnumValue<CombatMode> COMBAT_MODE;

    public enum CombatMode {
        BLACK_SOULS_TURN_BASED,
        MINECRAFT_REALTIME
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.translation("config.blacksouls.general").push("general");

        ENABLE_CUSTOM_MAIN_MENU = builder
                .translation("config.blacksouls.enable_custom_main_menu")
                .define("enableCustomMainMenu", true);

        ALLOW_PLAYER_EXTRA_MODES = builder
                .translation("config.blacksouls.allow_player_extra_modes")
                .define("allowPlayerExtraModes", false);

        ENABLE_LOW_SEN_JUMPSCARE = builder
                .translation("config.blacksouls.enable_low_sen_jumpscare")
                .define("enableLowSenJumpscare", true);

        SHOW_COMBAT_DAMAGE_CHAT = builder
                .translation("config.blacksouls.show_combat_damage_chat")
                .define("showCombatDamageChat", true);

        COMBAT_MODE = builder
                .translation("config.blacksouls.combat_mode")
                .defineEnum("combatMode", CombatMode.BLACK_SOULS_TURN_BASED);

        builder.pop();

        COMMON_SPEC = builder.build();

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();

        clientBuilder.translation("config.blacksouls.client").push("client");

        ENABLE_ORIGINAL_WINDOW_BRANDING = clientBuilder
                .translation("config.blacksouls.enable_original_window_branding")
                .define("enableOriginalWindowBranding", true);

        ENABLE_MMD_MODELS = clientBuilder
                .translation("config.blacksouls.enable_mmd_models")
                .define("enableMMDModels", true);

        ENABLE_CUSTOM_HEALTH_BAR = clientBuilder
                .translation("config.blacksouls.enable_custom_health_bar")
                .define("enableCustomHealthBar", true);

        ENABLE_ENTITY_STATUS_BAR = clientBuilder
                .translation("config.blacksouls.enable_entity_status_bar")
                .define("enableEntityStatusBar", true);

        clientBuilder.pop();

        CLIENT_SPEC = clientBuilder.build();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event) {
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
    }
}

package com.BlackSouls.BlackSoulsMod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BSConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static ForgeConfigSpec.BooleanValue ENABLE_CUSTOM_MAIN_MENU;
    public static ForgeConfigSpec.BooleanValue ALLOW_PLAYER_EXTRA_MODES;
    public static ForgeConfigSpec.BooleanValue ENABLE_LOW_SEN_JUMPSCARE;
    public static ForgeConfigSpec.BooleanValue SHOW_COMBAT_DAMAGE_CHAT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("BLACKSOULS II 模组常规配置").push("general");

        ENABLE_CUSTOM_MAIN_MENU = builder
                .comment("是否开启 BLACKSOULS II 风格的自定义主菜单")
                .define("enableCustomMainMenu", true);

        ALLOW_PLAYER_EXTRA_MODES = builder
                .comment("是否允许普通玩家切换额外难度模式，并在首次进入存档时赠送开发者模式物品")
                .define("allowPlayerExtraModes", false);

        ENABLE_LOW_SEN_JUMPSCARE = builder
                .comment("是否启用SEN低于30时随机出现的一闪而过鬼图效果")
                .define("enableLowSenJumpscare", true);

        SHOW_COMBAT_DAMAGE_CHAT = builder
                .comment("是否在聊天栏显示玩家造成伤害的战斗提示")
                .define("showCombatDamageChat", true);

        builder.pop();

        COMMON_SPEC = builder.build();

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();

        CLIENT_SPEC = clientBuilder.build();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event) {
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
    }
}

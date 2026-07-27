package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.client.gui.GuiAdvancedSkill;
import com.BlackSouls.BlackSoulsMod.client.gui.GuiCovenant;
import com.BlackSouls.BlackSoulsMod.client.gui.GuiPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketCastSkill;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import com.BlackSouls.BlackSoulsMod.util.skill.AbstractSkill;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public class KeyHandler {

    public static final KeyMapping KEY_OPEN_MENU = new KeyMapping("key.blacksouls.ui", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_K, "key.categories.blacksouls");
    public static final KeyMapping KEY_CAST_SKILL = new KeyMapping("key.blacksouls.cast", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_Z, "key.categories.blacksouls");
    public static final KeyMapping KEY_CAST_SKILL_X = new KeyMapping("key.blacksouls.cast_x", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_X, "key.categories.blacksouls");
    public static final KeyMapping KEY_CAST_SKILL_C = new KeyMapping("key.blacksouls.cast_c", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_C, "key.categories.blacksouls");
    public static final KeyMapping KEY_CAST_SKILL_V = new KeyMapping("key.blacksouls.cast_v", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_V, "key.categories.blacksouls");
    public static final KeyMapping KEY_STATS = new KeyMapping("key.blacksouls.stats", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_P, "key.categories.blacksouls");
    public static final KeyMapping KEY_COVENANT = new KeyMapping("key.blacksouls.covenant", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_U, "key.categories.blacksouls");

    @Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class KeyMappingRegistry {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(KEY_OPEN_MENU);
            event.register(KEY_CAST_SKILL);
            event.register(KEY_CAST_SKILL_X);
            event.register(KEY_CAST_SKILL_C);
            event.register(KEY_CAST_SKILL_V);
            event.register(KEY_STATS);
            event.register(KEY_COVENANT);
        }
    }
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null) return;
        boolean blacksoulsLoaded = ModList.get().isLoaded("blacksouls");

        while (KEY_OPEN_MENU.consumeClick()) {
            if (blacksoulsLoaded) {
                mc.setScreen(new GuiAdvancedSkill());
            }
        }
        while (KEY_STATS.consumeClick()) mc.setScreen(new GuiPlayerStats());
        while (KEY_COVENANT.consumeClick()) {
            player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.get(), 1.0F, 1.0F);
            mc.setScreen(new GuiCovenant());
        }

        while (KEY_CAST_SKILL.consumeClick()) if (blacksoulsLoaded) handleSkillCast(player, "Z");
        while (KEY_CAST_SKILL_X.consumeClick()) if (blacksoulsLoaded) handleSkillCast(player, "X");
        while (KEY_CAST_SKILL_C.consumeClick()) if (blacksoulsLoaded) handleSkillCast(player, "C");
        while (KEY_CAST_SKILL_V.consumeClick()) if (blacksoulsLoaded) handleSkillCast(player, "V");
    }

    private static void handleSkillCast(Player player, String keyBind) {
        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            String skillToCast = "";
            if (keyBind.equals("Z")) skillToCast = stats.skillZ;
            else if (keyBind.equals("X")) skillToCast = stats.skillX;
            else if (keyBind.equals("C")) skillToCast = stats.skillC;
            else if (keyBind.equals("V")) skillToCast = stats.skillV;

            if (skillToCast == null || skillToCast.isEmpty()) return;

            boolean canCast = false;

            if (SkillUtils.hasLearnedSkill(player, skillToCast)) {
                canCast = true;
            } else if (skillToCast.equals("bs2_skill_shotgun_blast")) {
                net.minecraft.world.item.ItemStack offhand = player.getOffhandItem();
                if (!offhand.isEmpty() && offhand.getItem() == BlackSouls.MURDERERS_SHOTGUN.get()) canCast = true;
            } else if (skillToCast.equals("bs2_skill_vorpal_slash")) {
                net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
                if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.VORPAL_BLADE.get()
                        || mainHand.getItem() == BlackSouls.VORPAL_SWORD.get())) canCast = true;
            } else if (skillToCast.equals("bs2_skill_aura_blade")) {
                net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
                if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.ANDOR_SWORD.get()) canCast = true;
            }
            
            else if (skillToCast.equals("bs2_skill_weapon_break") || skillToCast.equals("bs2_skill_armor_break")) {
                net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
                if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.KNIGHT_SWORD.get() || mainHand.getItem() == BlackSouls.KNIGHT_KING_SWORD.get())) canCast = true;
            }
            
            else if (skillToCast.equals("bs2_skill_knights_glory")) {
                net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
                if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.KNIGHT_KING_SWORD.get()) canCast = true;
            }
            else if (skillToCast.equals("bs2_skill_dragon_shockwave")) {
                net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
                if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DRAKE_SWORD.get()) canCast = true;
            }
            
            else if (skillToCast.equals("bs2_skill_radiant_blade")) {
                net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
                if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.ANDOR_SWORD.get()) {
                    if (mainHand.hasTag() && mainHand.getTag().contains("bs2_upgrade_level")) {
                        if (mainHand.getTag().getInt("bs2_upgrade_level") >= 5) canCast = true;
                    }
                }
            }
            
            else if (skillToCast.equals("bs2_skill_hellfire_blade")) {
                net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
                if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.DRAKE_SWORD.get()) {
                    if (mainHand.hasTag() && mainHand.getTag().getInt("bs2_upgrade_level") >= 5) canCast = true;
                }
            }
            
            else if (skillToCast.equals("bs2_skill_ultimate_triple_slash")) {
                net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
                if (!mainHand.isEmpty() && mainHand.getItem() == BlackSouls.BRAVE_SWORD_VORPAL.get()) canCast = true;
            }
            
            else if (skillToCast.equals("bs2_skill_reinforce")) {
                net.minecraft.world.item.ItemStack mainHand = player.getMainHandItem();
                if (!mainHand.isEmpty() && (mainHand.getItem() == BlackSouls.BRAVE_SWORD_VORPAL.get()
                        || mainHand.getItem() == BlackSouls.VORPAL_BLADE.get()
                        || mainHand.getItem() == BlackSouls.VORPAL_SWORD.get())) {
                    canCast = true;
                }
            } else if (skillToCast.equals("bs2_skill_chrono_clock")) {
                canCast = SkillUtils.hasChronoClockEquipped(player);
            } else {
                
                AbstractSkill skill = SkillRegistry.SKILLS.get(skillToCast);
                canCast = skill != null && skill.isUnlockedForGUI(player);
            }
            if (canCast) {
                NetworkHandler.INSTANCE.sendToServer(new PacketCastSkill(skillToCast));
            } else {
                
                player.displayClientMessage(Component.translatable("message.blacksouls.skill.cast_fail").withStyle(ChatFormatting.RED), true);
            }
        });
    }
}

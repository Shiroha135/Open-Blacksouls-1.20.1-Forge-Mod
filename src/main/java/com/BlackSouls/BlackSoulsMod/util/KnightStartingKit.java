package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundAdviceVisibilityPacket;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillSeekAdvice;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public final class KnightStartingKit {
    private static final String GRANTED_TAG = "bs2_knight_starting_kit_granted";
    private static final List<String> STARTING_SKILLS = List.of(
            "bs2_skill_dodge",
            "bs2_skill_crush",
            "bs2_skill_seek_advice"
    );

    public static void grant(ServerPlayer player) {
        var data = SkillUtils.getPersistedData(player);
        if (data.getBoolean(GRANTED_TAG)) {
            return;
        }

        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            for (String skillId : STARTING_SKILLS) {
                if (!stats.unlockedSkills.contains(skillId)) {
                    stats.unlockedSkills.add(skillId);
                }
            }
        });

        equipHand(player, InteractionHand.MAIN_HAND, new ItemStack(BlackSouls.KNIGHT_SWORD.get()));
        equipHand(player, InteractionHand.OFF_HAND, new ItemStack(BlackSouls.KNIGHT_SHIELD.get()));
        equipCurioOrGive(player, "head", new ItemStack(BlackSouls.KNIGHT_HELMET.get()));
        equipCurioOrGive(player, "body", new ItemStack(BlackSouls.KNIGHT_ARMOR.get()));

        data.putBoolean(SkillSeekAdvice.CONTROLLED_TAG, true);
        data.putBoolean(SkillSeekAdvice.VISIBLE_TAG, false);
        data.putBoolean(GRANTED_TAG, true);
        NetworkHandler.sendToPlayer(new ClientboundAdviceVisibilityPacket(true, false), player);
        player.containerMenu.broadcastChanges();
        StatEventHandler.applyStats(player);
        StatEventHandler.syncToClient(player);
    }

    private static void equipHand(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if (player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, stack);
        } else {
            giveOrDrop(player, stack);
        }
    }

    private static void equipCurioOrGive(ServerPlayer player, String slot, ItemStack stack) {
        boolean equipped = CuriosApi.getCuriosInventory(player).map(handler ->
                handler.getStacksHandler(slot).map(stacks -> {
                    for (int index = 0; index < stacks.getSlots(); index++) {
                        if (stacks.getStacks().getStackInSlot(index).isEmpty()) {
                            stacks.getStacks().setStackInSlot(index, stack);
                            return true;
                        }
                    }
                    return false;
                }).orElse(false)
        ).orElse(false);
        if (!equipped) {
            giveOrDrop(player, stack);
        }
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private KnightStartingKit() {
    }
}

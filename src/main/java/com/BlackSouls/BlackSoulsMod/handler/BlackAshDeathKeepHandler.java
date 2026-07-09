package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.item.consumables.ItemBlackAsh;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class BlackAshDeathKeepHandler {

    private static final Map<UUID, List<ItemStack>> BLACK_ASH_TO_RESTORE = new ConcurrentHashMap<>();

    private BlackAshDeathKeepHandler() {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        List<ItemStack> savedStacks = new ArrayList<>();

        event.getDrops().removeIf(itemEntity -> {
            ItemStack stack = itemEntity.getItem();

            if (isBlackAsh(stack)) {
                savedStacks.add(stack.copy());
                return true;
            }

            return false;
        });

        if (!savedStacks.isEmpty()) {
            BLACK_ASH_TO_RESTORE
                    .computeIfAbsent(player.getUUID(), uuid -> new ArrayList<>())
                    .addAll(savedStacks);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        Player newPlayer = event.getEntity();

        List<ItemStack> savedStacks = BLACK_ASH_TO_RESTORE.remove(newPlayer.getUUID());

        if (savedStacks == null || savedStacks.isEmpty()) {
            return;
        }

        for (ItemStack savedStack : savedStacks) {
            if (savedStack.isEmpty() || !isBlackAsh(savedStack)) {
                continue;
            }

            ItemStack copy = savedStack.copy();

            if (!newPlayer.getInventory().add(copy)) {
                newPlayer.drop(copy, false);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        BLACK_ASH_TO_RESTORE.remove(event.getEntity().getUUID());
    }

    private static boolean isBlackAsh(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemBlackAsh;
    }
}
package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBossData;
import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBossState;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundBossVictoryPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SceneSpawnerBossHandler {
    private static final String PLAYER_SCENE_TAG = "Blacksouls2CurrentScene";

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        ServerPlayer creditedPlayer = event.getSource().getEntity() instanceof ServerPlayer player
                ? player : event.getEntity().getKillCredit() instanceof ServerPlayer player ? player : null;
        markDefeated(event.getEntity(), creditedPlayer);
    }

    public static void markDefeated(LivingEntity entity, @Nullable ServerPlayer creditedPlayer) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        String spawnerKey = data.getString(SceneSpawnerBossState.ENTITY_SPAWNER_KEY_TAG);
        if (!data.getBoolean(SceneSpawnerBossState.ENTITY_BOSS_TAG)
                || spawnerKey.isEmpty()
                || !SceneSpawnerBossData.get(level.getServer()).markDefeated(spawnerKey)) {
            return;
        }

        String sceneId = data.getString(SceneSpawnerBossState.ENTITY_SCENE_ID_TAG);
        String sceneKey = sceneId.isEmpty() ? "" : level.dimension().location() + "#" + sceneId;
        Set<UUID> notified = new HashSet<>();
        if (!sceneKey.isEmpty()) {
            for (ServerPlayer player : level.players()) {
                if (sceneKey.equals(player.getPersistentData().getString(PLAYER_SCENE_TAG))) {
                    NetworkHandler.sendToPlayer(new ClientboundBossVictoryPacket(), player);
                    notified.add(player.getUUID());
                }
            }
        }
        if (creditedPlayer != null && notified.add(creditedPlayer.getUUID())) {
            NetworkHandler.sendToPlayer(new ClientboundBossVictoryPacket(), creditedPlayer);
        }
    }

    private SceneSpawnerBossHandler() {
    }
}

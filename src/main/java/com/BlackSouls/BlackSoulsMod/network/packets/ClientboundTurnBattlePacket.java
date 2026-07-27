package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.client.gui.GuiTurnBattle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

public class ClientboundTurnBattlePacket {
    private final boolean active;
    private final int rootEntityId;
    private final int battleProfileId;
    private final List<EnemySnapshot> enemies;
    private final int actingEnemyIndex;
    private final int enemyAnimationId;
    private final boolean phaseChanged;
    private final boolean awaitingPresentation;
    private final List<DamageHit> playerHits;
    private final Component message;
    private final boolean canAct;
    private final Outcome outcome;
    private final long soulReward;
    private final Map<String, Integer> skillCooldowns;

    public ClientboundTurnBattlePacket(boolean active, int rootEntityId, int battleProfileId,
                                       List<EnemySnapshot> enemies, int actingEnemyIndex,
                                       int enemyAnimationId, boolean phaseChanged,
                                       boolean awaitingPresentation, List<DamageHit> playerHits,
                                       Component message, boolean canAct, Outcome outcome,
                                       long soulReward, Map<String, Integer> skillCooldowns) {
        this.active = active;
        this.rootEntityId = rootEntityId;
        this.battleProfileId = battleProfileId;
        this.enemies = List.copyOf(enemies);
        this.actingEnemyIndex = actingEnemyIndex;
        this.enemyAnimationId = enemyAnimationId;
        this.phaseChanged = phaseChanged;
        this.awaitingPresentation = awaitingPresentation;
        this.playerHits = List.copyOf(playerHits);
        this.message = message;
        this.canAct = canAct;
        this.outcome = outcome;
        this.soulReward = soulReward;
        this.skillCooldowns = Map.copyOf(skillCooldowns);
    }

    public ClientboundTurnBattlePacket(FriendlyByteBuf buffer) {
        this.active = buffer.readBoolean();
        this.rootEntityId = buffer.readVarInt();
        this.battleProfileId = buffer.readVarInt();
        int enemyCount = buffer.readVarInt();
        List<EnemySnapshot> snapshots = new ArrayList<>(enemyCount);
        for (int i = 0; i < enemyCount; i++) {
            snapshots.add(EnemySnapshot.read(buffer));
        }
        this.enemies = List.copyOf(snapshots);
        this.actingEnemyIndex = buffer.readVarInt();
        this.enemyAnimationId = buffer.readVarInt();
        this.phaseChanged = buffer.readBoolean();
        this.awaitingPresentation = buffer.readBoolean();
        int playerHitCount = buffer.readVarInt();
        List<DamageHit> hits = new ArrayList<>(playerHitCount);
        for (int i = 0; i < playerHitCount; i++) {
            hits.add(DamageHit.read(buffer));
        }
        this.playerHits = List.copyOf(hits);
        this.message = buffer.readComponent();
        this.canAct = buffer.readBoolean();
        this.outcome = PacketHandlers.readEnum(buffer, Outcome.values());
        this.soulReward = buffer.readVarLong();
        int cooldownCount = buffer.readVarInt();
        Map<String, Integer> cooldowns = new HashMap<>();
        for (int i = 0; i < cooldownCount; i++) {
            cooldowns.put(buffer.readUtf(), buffer.readVarInt());
        }
        this.skillCooldowns = Map.copyOf(cooldowns);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.active);
        buffer.writeVarInt(this.rootEntityId);
        buffer.writeVarInt(this.battleProfileId);
        buffer.writeVarInt(this.enemies.size());
        this.enemies.forEach(enemy -> enemy.write(buffer));
        buffer.writeVarInt(this.actingEnemyIndex);
        buffer.writeVarInt(this.enemyAnimationId);
        buffer.writeBoolean(this.phaseChanged);
        buffer.writeBoolean(this.awaitingPresentation);
        buffer.writeVarInt(this.playerHits.size());
        this.playerHits.forEach(hit -> hit.write(buffer));
        buffer.writeComponent(this.message);
        buffer.writeBoolean(this.canAct);
        PacketHandlers.writeEnum(buffer, this.outcome);
        buffer.writeVarLong(this.soulReward);
        buffer.writeVarInt(this.skillCooldowns.size());
        this.skillCooldowns.forEach((skillId, rounds) -> {
            buffer.writeUtf(skillId);
            buffer.writeVarInt(rounds);
        });
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleClient(supplier, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof GuiTurnBattle battle
                    && battle.matches(this.rootEntityId)) {
                battle.applyState(this.active, this.battleProfileId, this.enemies,
                        this.actingEnemyIndex, this.phaseChanged,
                        this.awaitingPresentation, this.playerHits, this.message,
                        this.canAct, this.outcome, this.soulReward,
                        this.skillCooldowns, this.enemyAnimationId);
            } else if (this.active) {
                minecraft.setScreen(new GuiTurnBattle(this.rootEntityId,
                        this.battleProfileId, this.enemies, this.message,
                        this.canAct, this.skillCooldowns, this.enemyAnimationId));
            }
        });
    }

    public record DamageHit(int targetEntityId, int damage, boolean critical, int wave) {
        private static DamageHit read(FriendlyByteBuf buffer) {
            return new DamageHit(buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readBoolean(), buffer.readVarInt());
        }

        private void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(this.targetEntityId);
            buffer.writeVarInt(this.damage);
            buffer.writeBoolean(this.critical);
            buffer.writeVarInt(this.wave);
        }
    }

    public record EnemySnapshot(int entityId, Component name, float health,
                                float maxHealth, int profileId, List<Integer> states) {
        private static EnemySnapshot read(FriendlyByteBuf buffer) {
            int entityId = buffer.readVarInt();
            Component name = buffer.readComponent();
            float health = buffer.readFloat();
            float maxHealth = buffer.readFloat();
            int profileId = buffer.readVarInt();
            int stateCount = buffer.readVarInt();
            List<Integer> states = new ArrayList<>(stateCount);
            for (int i = 0; i < stateCount; i++) {
                states.add(buffer.readVarInt());
            }
            return new EnemySnapshot(entityId, name, health, maxHealth,
                    profileId, List.copyOf(states));
        }

        private void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(this.entityId);
            buffer.writeComponent(this.name);
            buffer.writeFloat(this.health);
            buffer.writeFloat(this.maxHealth);
            buffer.writeVarInt(this.profileId);
            buffer.writeVarInt(this.states.size());
            this.states.forEach(buffer::writeVarInt);
        }
    }

    public enum Outcome {
        NONE,
        VICTORY,
        DEFEAT,
        ESCAPED
    }
}

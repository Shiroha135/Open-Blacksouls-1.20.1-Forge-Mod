package com.BlackSouls.BlackSoulsMod.story;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalBillPlacement;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalNpcSymbolService {
    public static final String STEALTH_STEPS = "BSOriginalSymbolStealthSteps";
    private static final String STEALTH_ANCHOR = "BSOriginalSymbolStealthAnchor";

    private OriginalNpcSymbolService() {
    }

    public static int stealthSteps(ServerPlayer player) {
        var data = com.BlackSouls.BlackSoulsMod.util.SkillUtils.getPersistedData(player);
        if (!data.contains(STEALTH_STEPS)) return 0;
        if (!data.contains(STEALTH_STEPS, Tag.TAG_INT) || data.getInt(STEALTH_STEPS) < 0) throw new IllegalArgumentException("Invalid original symbol stealth");
        return data.getInt(STEALTH_STEPS);
    }

    public static void setStealthSteps(ServerPlayer player, int steps) {
        if (steps < 0) throw new IllegalArgumentException("Invalid original symbol stealth steps");
        var data = com.BlackSouls.BlackSoulsMod.util.SkillUtils.getPersistedData(player);
        if (steps == 0) {
            data.remove(STEALTH_STEPS);
            data.remove(STEALTH_ANCHOR);
        } else {
            data.putInt(STEALTH_STEPS, steps);
            synchronizeStealthAnchor(player);
        }
    }

    public static void consumeSourceStep(ServerPlayer player) {
        int steps = stealthSteps(player);
        if (steps > 0) setStealthSteps(player, steps - 1);
    }

    public static void synchronizeStealthAnchor(ServerPlayer player) {
        if (stealthSteps(player) == 0) return;
        var anchor = new CompoundTag();
        anchor.putString("Dimension", player.level().dimension().location().toString());
        anchor.putInt("X", player.blockPosition().getX());
        anchor.putInt("Z", player.blockPosition().getZ());
        com.BlackSouls.BlackSoulsMod.util.SkillUtils.getPersistedData(player).put(STEALTH_ANCHOR, anchor);
    }

    private static void updateStealthSteps(ServerPlayer player) {
        if (stealthSteps(player) == 0) return;
        var data = com.BlackSouls.BlackSoulsMod.util.SkillUtils.getPersistedData(player);
        if (!data.contains(STEALTH_ANCHOR, Tag.TAG_COMPOUND)) {
            synchronizeStealthAnchor(player);
            return;
        }
        var previous = data.getCompound(STEALTH_ANCHOR);
        if (!previous.getAllKeys().equals(java.util.Set.of("Dimension", "X", "Z")) || !previous.contains("Dimension", Tag.TAG_STRING)
                || !previous.contains("X", Tag.TAG_INT) || !previous.contains("Z", Tag.TAG_INT)) throw new IllegalArgumentException("Invalid original stealth anchor");
        int dx = Math.abs(player.blockPosition().getX() - previous.getInt("X"));
        int dz = Math.abs(player.blockPosition().getZ() - previous.getInt("Z"));
        var movement = OriginalNpcMovementData.get(player.server);
        var ticket = movement.tickets.get(movement.latestByTarget.get(player.getUUID()));
        if (previous.getString("Dimension").equals(player.level().dimension().location().toString())
                && Math.max(dx, dz) == 1 && (ticket == null || ticket.complete)
                && !OriginalWorldTransferService.pending(player)) consumeSourceStep(player);
        synchronizeStealthAnchor(player);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) return;
        var previous = event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (previous.contains(STEALTH_STEPS, Tag.TAG_INT)) setStealthSteps(target, previous.getInt(STEALTH_STEPS));
    }

    public static boolean active(EntityOriginalNpc npc) {
        return npc.getNpcId().equals("map037_ev053") && npc.currentPage() != null && npc.currentPage().trigger() == 2
                && npc.originalAutonomousRoute().symbol() != null && npc.originalAutonomousRoute().symbol().enabled();
    }

    public static int contactCondition(ServerPlayer player, EntityOriginalNpc npc) {
        if (!active(npc) || player.level() != npc.level()) return 0;
        double dx = npc.getX() - player.getX(), dy = npc.getZ() - player.getZ();
        if (Math.abs(dx) < 1.0E-7D) dx = 0;
        if (Math.abs(dy) < 1.0E-7D) dy = 0;
        return OriginalNpcSymbolState.contactCondition(npc.getSpriteDirection(), OriginalBillPlacement.playerDirection(player), dx, dy);
    }

    public static boolean tryContact(ServerPlayer player, EntityOriginalNpc npc) {
        if (!active(npc) || !npc.isAlive() || npc.isRemoved() || npc.isErasedForCurrentVisit() || !player.isAlive() || player.isSpectator()
                || player.level() != npc.level() || OriginalNpcService.isConversing(player) || TurnBattleManager.isInBattle(player)
                || npc.originalAutonomousRoute().motion != null && npc.originalAutonomousRoute().motion.snapshot().get("jump").getAsBoolean()
                || Math.abs(player.getY() - npc.getY()) >= 1.0D) return false;
        int distance = Math.abs(player.blockPosition().getX() - npc.blockPosition().getX())
                + Math.abs(player.blockPosition().getZ() - npc.blockPosition().getZ());
        if (distance > 1) return false;
        OriginalNpcService.interactContact(player, npc);
        return OriginalNpcService.isConversing(player) || TurnBattleManager.isInBattle(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
        updateStealthSteps(player);
        if (OriginalNpcService.isConversing(player) || TurnBattleManager.isInBattle(player)) return;
        for (var npc : player.serverLevel().getEntitiesOfClass(EntityOriginalNpc.class, player.getBoundingBox().inflate(0.1D))) {
            if (active(npc) && player.getBoundingBox().inflate(0.025D).intersects(npc.getBoundingBox()) && tryContact(player, npc)) return;
        }
    }
}

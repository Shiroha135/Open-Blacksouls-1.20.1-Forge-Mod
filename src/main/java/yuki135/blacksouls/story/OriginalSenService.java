package yuki135.blacksouls.story;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.capability.OriginalNpcProgress;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.handler.StatEventHandler;
import yuki135.blacksouls.network.NetworkHandler;
import yuki135.blacksouls.network.packets.ClientboundOriginalSenInfoPacket;
import yuki135.blacksouls.network.packets.PacketSyncStats;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.nbt.Tag;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalSenService {
    private static final Map<UUID, Long> REVISIONS = new HashMap<>();
    private static final String PAGE_OWNER = "BlacksoulsOriginalSenPageIdentity";
    private static final String PAGE_THRESHOLDS = "BlacksoulsOriginalSenPageThresholds";

    private OriginalSenService() {
    }

    public static boolean isThresholdSwitch(int id) {
        return id >= 14 && id <= 16;
    }

    private static BSPlayerStats stats(ServerPlayer player) {
        return player.getCapability(BSPlayerStats.CAPABILITY).orElseThrow(() -> new IllegalStateException("Missing original SEN stats"));
    }

    public static boolean thresholdValue(ServerPlayer player, int id) {
        if (!isThresholdSwitch(id)) throw new IllegalArgumentException("original_sen_switch");
        BSPlayerStats stats = stats(player);
        return stats.originalSenThresholdsKnown ? (stats.originalSenThresholdMask & 1 << (id - 14)) != 0
                : OriginalNpcProgress.get(player.server).switchValue(id);
    }

    public static void writeThreshold(ServerPlayer player, int id, boolean value) {
        if (!isThresholdSwitch(id)) throw new IllegalArgumentException("original_sen_switch");
        BSPlayerStats stats = stats(player);
        if (!stats.originalSenThresholdsKnown) {
            int mask = 0;
            for (int switchId = 14; switchId <= 16; switchId++) {
                if (OriginalNpcProgress.get(player.server).switchValue(switchId)) mask |= 1 << (switchId - 14);
            }
            stats.originalSenThresholdMask = mask;
            stats.originalSenThresholdsKnown = true;
        }
        int bit = 1 << (id - 14);
        stats.originalSenThresholdMask = value ? stats.originalSenThresholdMask | bit : stats.originalSenThresholdMask & ~bit;
    }

    public static void writeValue(ServerPlayer player, long value) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw new UnsupportedOperationException("original_sen_capability_range");
        BSPlayerStats stats = stats(player);
        if (stats.sen == (int) value) return;
        stats.sen = (int) value;
        StatEventHandler.applyStats(player);
        NetworkHandler.sendToPlayer(new PacketSyncStats(stats.serializeNBT()), player);
    }

    public static void afterVariableOperation(ServerPlayer player, int operation, long operand, long before, long after) {
        BSPlayerStats stats = stats(player);
        if (after != stats.sen || before < Integer.MIN_VALUE || before > Integer.MAX_VALUE
                || operation < 0 || operation > 5) throw new IllegalArgumentException("original_sen_operation");
        stats.originalSenThresholdsKnown = true;
        stats.originalSenThresholdMask = (after <= 0 ? 1 : 0) | (after <= 30 ? 2 : 0) | (after <= 70 ? 4 : 0);
        NetworkHandler.sendToPlayer(new PacketSyncStats(stats.serializeNBT()), player);
        long delta = Math.subtractExact(after, before);
        if (delta != 0 && !(operation == 0 && operand == 100)) {
            NetworkHandler.sendToPlayer(new ClientboundOriginalSenInfoPacket(next(player), player.level().dimension().location(),
                    stats.sen, delta, false), player);
        }
    }

    public static void rememberPageOwner(EntityOriginalNpc actor, ServerPlayer player) {
        rememberPageOwner(actor, actor.getNpcId(), player);
    }

    public static void rememberPageOwner(PathfinderMob actor, String identity, ServerPlayer player) {
        if (stats(player).originalSenThresholdsKnown) {
            actor.getPersistentData().putString(PAGE_OWNER, identity);
            actor.getPersistentData().putInt(PAGE_THRESHOLDS, stats(player).originalSenThresholdMask);
        } else if (identity.equals(actor.getPersistentData().getString(PAGE_OWNER))) {
            actor.getPersistentData().remove(PAGE_OWNER);
            actor.getPersistentData().remove(PAGE_THRESHOLDS);
        }
    }

    public static boolean hasPersonalPage(EntityOriginalNpc actor) {
        return cachedPageThresholds(actor, actor.getNpcId()) != null;
    }

    public static void copyPageOwner(PathfinderMob source, String sourceIdentity, PathfinderMob target, String targetIdentity) {
        Integer mask = cachedPageThresholds(source, sourceIdentity);
        if (mask != null) {
            target.getPersistentData().putString(PAGE_OWNER, targetIdentity);
            target.getPersistentData().putInt(PAGE_THRESHOLDS, mask);
        } else if (targetIdentity.equals(target.getPersistentData().getString(PAGE_OWNER))) {
            target.getPersistentData().remove(PAGE_OWNER);
            target.getPersistentData().remove(PAGE_THRESHOLDS);
        }
    }

    public static Integer cachedPageThresholds(PathfinderMob actor, String identity) {
        var data = actor.getPersistentData();
        if (identity.isEmpty() || !identity.equals(data.getString(PAGE_OWNER)) || !data.contains(PAGE_THRESHOLDS, Tag.TAG_INT)) return null;
        int mask = data.getInt(PAGE_THRESHOLDS);
        return mask >= 0 && mask <= 7 ? mask : null;
    }

    public static ServerPlayer nearestPageOwner(PathfinderMob actor) {
        if (!(actor.level() instanceof ServerLevel level)) return null;
        return level.getServer().getPlayerList().getPlayers().stream()
                .filter(player -> player.serverLevel() == level && player.isAlive() && !player.isSpectator() && player.distanceToSqr(actor) <= 1024.0D)
                .min(java.util.Comparator.comparingDouble(player -> player.distanceToSqr(actor))).orElse(null);
    }

    public static void clear(ServerPlayer player) {
        if (!REVISIONS.containsKey(player.getUUID())) return;
        NetworkHandler.sendToPlayer(new ClientboundOriginalSenInfoPacket(next(player), player.level().dimension().location(), 0, 0, true), player);
    }

    private static long next(ServerPlayer player) {
        long revision = Math.addExact(REVISIONS.getOrDefault(player.getUUID(), 0L), 1L);
        REVISIONS.put(player.getUUID(), revision);
        return revision;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        REVISIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        REVISIONS.clear();
    }
}

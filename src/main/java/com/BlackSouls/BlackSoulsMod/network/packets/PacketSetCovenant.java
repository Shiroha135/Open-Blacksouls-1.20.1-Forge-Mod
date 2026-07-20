package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.entity.EntityNoden;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class PacketSetCovenant {
    private static final int MAX_COVENANT_ID_LENGTH = 64;

    private final String covenantName;
    private final int level;

    public PacketSetCovenant(String covenantName, int level) {
        this.covenantName = covenantName;
        this.level = level;
    }

    public PacketSetCovenant(FriendlyByteBuf buf) {
        this.covenantName = buf.readUtf(MAX_COVENANT_ID_LENGTH);
        this.level = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.covenantName, MAX_COVENANT_ID_LENGTH);
        buf.writeInt(this.level);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                if (applyChange(player, stats)) {
                    NetworkHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new PacketSyncStats(stats.serializeNBT())
                    );
                }
            });
        });
        context.setPacketHandled(true);
    }

    private boolean applyChange(ServerPlayer player, BSPlayerStats stats) {
        if (level == -1) {
            if (!stats.unlockedCovenants.contains(covenantName) || covenantName.equals(stats.activeCovenant)) {
                return false;
            }
            stats.activeCovenant = covenantName;
            stats.recalculateStats();
            return true;
        }

        if (level == -2 && "noden".equals(covenantName)) {
            boolean changed = stats.unlockedCovenants.remove("noden");
            if ("noden".equals(stats.activeCovenant)) {
                stats.activeCovenant = "";
                changed = true;
            }
            if (stats.nodenCovenantLevel != 0) {
                stats.nodenCovenantLevel = 0;
                changed = true;
            }
            if (changed) {
                stats.recalculateStats();
            }
            return changed;
        }

        if (!"noden".equals(covenantName) || !isNodenNearby(player)) {
            return false;
        }

        if (level == 0) {
            if (stats.unlockedCovenants.contains("noden")) {
                return false;
            }
            stats.unlockedCovenants.add("noden");
            if (stats.activeCovenant == null || stats.activeCovenant.isEmpty()) {
                stats.activeCovenant = "noden";
            }
            stats.nodenCovenantLevel = 0;
            stats.recalculateStats();
            playSuccessSound(player);
            return true;
        }

        if (level < 1 || level > 3 || !stats.unlockedCovenants.contains("noden")
                || level != stats.nodenCovenantLevel + 1) {
            return false;
        }

        long cost = switch (level) {
            case 1 -> 1000L;
            case 2 -> 3000L;
            case 3 -> 7000L;
            default -> 0L;
        };
        if (stats.souls < cost) {
            return false;
        }

        stats.souls -= cost;
        stats.nodenCovenantLevel = level;
        stats.recalculateStats();
        playSuccessSound(player);
        return true;
    }

    private static boolean isNodenNearby(ServerPlayer player) {
        return !player.level().getEntitiesOfClass(
                EntityNoden.class,
                player.getBoundingBox().inflate(8.0D),
                noden -> noden.isAlive() && !noden.isRemoved() && noden.distanceToSqr(player) <= 64.0D
        ).isEmpty();
    }

    private static void playSuccessSound(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}

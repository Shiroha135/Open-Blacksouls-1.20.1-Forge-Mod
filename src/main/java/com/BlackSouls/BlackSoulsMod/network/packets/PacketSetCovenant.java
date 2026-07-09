package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSetCovenant {
    private final String covenantName;
    private final int level;

    public PacketSetCovenant(String covenantName, int level) {
        this.covenantName = covenantName;
        this.level = level;
    }

    public PacketSetCovenant(FriendlyByteBuf buf) {
        this.covenantName = buf.readUtf(32767);
        this.level = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.covenantName);
        buf.writeInt(this.level);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats -> {
                
                if (level == -1) {
                    stats.activeCovenant = covenantName;
                    stats.recalculateStats();
                }
                
                else if (level == -2 && covenantName.equals("noden")) {
                    
                    stats.unlockedCovenants.remove("noden");

                    
                    if ("noden".equals(stats.activeCovenant)) {
                        stats.activeCovenant = "";
                    }

                    
                    stats.nodenCovenantLevel = 0;
                    stats.recalculateStats(); 
                }
                
                else if (covenantName.equals("noden")) {
                    
                    if (level == 0) {
                        if (!stats.unlockedCovenants.contains("noden")) {
                            stats.unlockedCovenants.add("noden");

                            
                            if (stats.activeCovenant == null || stats.activeCovenant.isEmpty()) {
                                stats.activeCovenant = "noden";
                            }
                            stats.nodenCovenantLevel = 0;
                            stats.recalculateStats();

                            
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                        }
                    }
                    
                    else if (level > 0 && level <= 3) {
                        long cost = 0;
                        if (level == 1) cost = 1000;
                        else if (level == 2) cost = 3000;
                        else if (level == 3) cost = 7000;

                        if (stats.souls >= cost && stats.nodenCovenantLevel < level) {
                            stats.souls -= cost;              
                            stats.nodenCovenantLevel = level; 
                            stats.recalculateStats();         

                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                        }
                    }
                }

                
                com.BlackSouls.BlackSoulsMod.network.NetworkHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new PacketSyncStats(stats.serializeNBT())
                );
            });
        });
        context.setPacketHandled(true);
    }
}
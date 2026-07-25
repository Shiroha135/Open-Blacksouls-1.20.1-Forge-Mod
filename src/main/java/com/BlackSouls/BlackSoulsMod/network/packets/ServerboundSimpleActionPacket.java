package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerboundSimpleActionPacket {
    private final Action action;

    public ServerboundSimpleActionPacket(Action action) {
        this.action = action;
    }

    public ServerboundSimpleActionPacket(FriendlyByteBuf buf) {
        this.action = PacketHandlers.readEnum(buf, Action.values());
    }

    public void toBytes(FriendlyByteBuf buf) {
        PacketHandlers.writeEnum(buf, this.action);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player == null || this.action == null) {
                return;
            }

            switch (this.action) {
                case REFRESH_PURGE_COMMISSIONS -> StatEventHandler.rerollPurgeTasks(player);
                case REQUEST_RESPAWN -> {
                    if (player.isDeadOrDying()) {
                        player.connection.handleClientCommand(
                                new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN)
                        );
                    }
                }
                case VISIT_DIFFICULTY_STATUE -> {
                    com.BlackSouls.BlackSoulsMod.capability.BSWorldData data =
                            com.BlackSouls.BlackSoulsMod.capability.BSWorldData.get(player.server.overworld());
                    int vanillaDeaths = player.getStats().getValue(
                            net.minecraft.stats.Stats.CUSTOM, net.minecraft.stats.Stats.DEATHS);
                    if (vanillaDeaths > data.deathCount) {
                        data.deathCount = vanillaDeaths;
                        data.setDirty();
                    }
                    net.minecraft.world.item.Item ring =
                            com.BlackSouls.BlackSoulsMod.BlackSouls.RING_MASOCHIST.get();
                    boolean ownsRing = player.getInventory().contains(new net.minecraft.world.item.ItemStack(ring))
                            || StatEventHandler.getBaubleCount(player, ring) > 0;
                    if (data.deathCount > 0 && !ownsRing) {
                        net.minecraft.world.item.ItemStack reward = new net.minecraft.world.item.ItemStack(ring);
                        if (!player.getInventory().add(reward)) {
                            player.drop(reward, false);
                        }
                    }
                    com.BlackSouls.BlackSoulsMod.network.NetworkHandler.sendToPlayer(
                            new PacketSyncDifficulty(data), player);
                }
            }
        });
    }

    public enum Action {
        REFRESH_PURGE_COMMISSIONS,
        REQUEST_RESPAWN,
        VISIT_DIFFICULTY_STATUE
    }
}

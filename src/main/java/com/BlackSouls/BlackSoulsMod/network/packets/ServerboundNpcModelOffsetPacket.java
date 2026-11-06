package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.handler.DevNpcInteractionHandler;
import com.shiroha.mmdskin.render.entity.CustomEntityModelOffsetProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class ServerboundNpcModelOffsetPacket {
    private final int entityId;
    private final UUID entityUuid;
    private final Vec3 offset;

    public ServerboundNpcModelOffsetPacket(int entityId, UUID entityUuid, Vec3 offset) {
        this.entityId = entityId;
        this.entityUuid = entityUuid;
        this.offset = offset;
    }

    public ServerboundNpcModelOffsetPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readVarInt();
        this.entityUuid = buffer.readUUID();
        this.offset = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeUUID(entityUuid);
        buffer.writeDouble(offset.x);
        buffer.writeDouble(offset.y);
        buffer.writeDouble(offset.z);
    }

    public boolean apply(ServerPlayer player) {
        if (player == null || !player.isAlive() || !player.isCreative() || !player.hasPermissions(4)
                || !player.getMainHandItem().is(BlackSouls.DEV_STAT_TOOL.get())
                || !CustomEntityModelOffsetProvider.isValidOffset(offset)) return false;
        Entity target = player.level().getEntity(entityId);
        if (target == null || !target.isAlive() || target.isRemoved() || !target.getUUID().equals(entityUuid)
                || !player.canReach(target, 0.0D) || !DevNpcInteractionHandler.supportsSitting(target)
                || !(target instanceof CustomEntityModelOffsetProvider provider)) return false;
        provider.setMmdModelOffset(offset);
        return true;
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                boolean accepted = apply(player);
                Entity target = player.level().getEntity(entityId);
                if (target != null && target.getUUID().equals(entityUuid) && target instanceof CustomEntityModelOffsetProvider provider) {
                    com.BlackSouls.BlackSoulsMod.network.NetworkHandler.sendToPlayer(new ClientboundNpcModelOffsetPacket(
                            entityId, entityUuid, provider.getMmdModelOffset(), accepted), player);
                }
                player.displayClientMessage(Component.translatable(accepted
                        ? "message.blacksouls.dev.model_offset.saved" : "message.blacksouls.dev.model_offset.rejected"), true);
            }
        });
    }
}

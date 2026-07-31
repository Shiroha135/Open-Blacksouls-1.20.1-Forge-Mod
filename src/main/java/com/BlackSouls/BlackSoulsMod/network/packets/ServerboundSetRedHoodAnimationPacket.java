package com.BlackSouls.BlackSoulsMod.network.packets;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.EntityRedHood;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ServerboundSetRedHoodAnimationPacket {
    private final int entityId;
    private final String animation;

    public ServerboundSetRedHoodAnimationPacket(int entityId, String animation) {
        this.entityId = entityId;
        this.animation = animation == null ? "" : animation;
    }

    public ServerboundSetRedHoodAnimationPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readVarInt();
        this.animation = buffer.readUtf(EntityRedHood.MAX_MMD_ANIMATION_LENGTH);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.entityId);
        buffer.writeUtf(this.animation, EntityRedHood.MAX_MMD_ANIMATION_LENGTH);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        PacketHandlers.handleServer(supplier, context -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.getMainHandItem().is(BlackSouls.DEV_STAT_TOOL.get())
                    || !isValidAnimation(this.animation)) {
                return;
            }
            Entity target = player.level().getEntity(this.entityId);
            if (target instanceof EntityRedHood redHood && player.distanceToSqr(redHood) <= 64.0D) {
                redHood.setMmdAnimation(this.animation);
            }
        });
    }

    private static boolean isValidAnimation(String animation) {
        if (animation == null || animation.length() > EntityRedHood.MAX_MMD_ANIMATION_LENGTH) {
            return false;
        }
        return !animation.contains("..")
                && animation.indexOf('/') < 0
                && animation.indexOf('\\') < 0
                && animation.chars().noneMatch(Character::isISOControl);
    }
}

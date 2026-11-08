package yuki135.blacksouls.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.tags.BlockTags;
import yuki135.blacksouls.story.OriginalOpeningBonfireFlow;

import java.util.function.Supplier;

public class PacketTeleportToBonfire {
    private static final int MAX_DIMENSION_ID_LENGTH = 128;

    private final ResourceLocation dimension;
    private final BlockPos pos;

    public PacketTeleportToBonfire(GlobalPos globalPos) {
        this.dimension = globalPos.dimension().location();
        this.pos = globalPos.pos();
    }

    public PacketTeleportToBonfire(FriendlyByteBuf buf) {
        this.dimension = ResourceLocation.tryParse(buf.readUtf(MAX_DIMENSION_ID_LENGTH));
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(dimension.toString(), MAX_DIMENSION_ID_LENGTH);
        buf.writeBlockPos(pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        PacketHandlers.handleServer(ctx, context -> teleport(context.getSender()));
        return true;
    }

    public boolean teleport(ServerPlayer player) {
        if (player == null || this.dimension == null) return false;
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, this.dimension);
        ServerLevel targetLevel = player.server.getLevel(dimKey);
        if (targetLevel == null || !targetLevel.isInWorldBounds(this.pos)
                || !OriginalOpeningBonfireFlow.canTeleport(player, dimKey, this.pos)
                || !targetLevel.getBlockState(this.pos).is(BlockTags.CAMPFIRES)) return false;

        BlockPos safePos = this.pos;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = this.pos.relative(dir);
            if (targetLevel.getBlockState(candidate).getCollisionShape(targetLevel, candidate).isEmpty()
                    && targetLevel.getBlockState(candidate.above()).getCollisionShape(targetLevel, candidate.above()).isEmpty()) {
                safePos = candidate;
                break;
            }
        }
        if (safePos.equals(this.pos)) safePos = this.pos.north();
        if (OriginalOpeningBonfireFlow.isFallingBonfire(dimKey, this.pos)) {
            safePos = OriginalOpeningBonfireFlow.FALLING_LANDING;
            var body = new net.minecraft.world.phys.AABB(safePos.getX() + .2D, safePos.getY(), safePos.getZ() + .2D,
                    safePos.getX() + .8D, safePos.getY() + 1.8D, safePos.getZ() + .8D);
            if (!targetLevel.noCollision(player, body) || targetLevel.noCollision(player, body.move(0, -.05D, 0))
                    || !targetLevel.getEntities(player, body, entity -> !entity.isRemoved() && !entity.isSpectator()).isEmpty()) return false;
        }

        double dx = this.pos.getX() - safePos.getX();
        double dz = this.pos.getZ() - safePos.getZ();
        float defaultYaw = (float)(Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
        if (yuki135.blacksouls.BlackSouls.FIRE6_EVENT.isPresent()) {
            player.level().playSound(null, player.blockPosition(), yuki135.blacksouls.BlackSouls.FIRE6_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        player.teleportTo(targetLevel, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, defaultYaw, 15.0F);
        OriginalOpeningBonfireFlow.returnedByBonfire(player, dimKey, this.pos);
        if (yuki135.blacksouls.BlackSouls.FIRE6_EVENT.isPresent()) {
            targetLevel.playSound(null, player.blockPosition(), yuki135.blacksouls.BlackSouls.FIRE6_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return true;
    }
}

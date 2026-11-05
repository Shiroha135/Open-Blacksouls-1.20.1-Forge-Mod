package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalWhiteBearPlacement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EntityOriginalWhiteBear extends EntityOriginalNpc {
    private static final EntityDataAccessor<Boolean> NATIVE_BLOCK_BOUND = SynchedEntityData.defineId(
            EntityOriginalWhiteBear.class, EntityDataSerializers.BOOLEAN);
    private final Map<UUID, Long> interactionTicks = new HashMap<>();

    public EntityOriginalWhiteBear(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNpcId(OriginalWhiteBearPlacement.NPC_ID);
        setNoGravity(true);
        noPhysics = true;
        setInvisible(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(NATIVE_BLOCK_BOUND, false);
    }

    @Override
    public void setNpcId(String id) {
        if (!OriginalWhiteBearPlacement.NPC_ID.equals(id)) {
            throw new IllegalArgumentException("White bear source identity is immutable");
        }
        super.setNpcId(id);
    }

    public void bindNativeBlock() {
        if (level().isClientSide || position().distanceToSqr(OriginalWhiteBearPlacement.POSITION) > 1.0E-8D) {
            throw new IllegalStateException("Invalid white bear anchor");
        }
        entityData.set(NATIVE_BLOCK_BOUND, true);
    }

    public boolean isNativeBlockBound() {
        return entityData.get(NATIVE_BLOCK_BOUND);
    }

    public boolean claimInteractionTick(UUID playerId, long tick) {
        interactionTicks.entrySet().removeIf(entry -> entry.getValue() != tick);
        return interactionTicks.putIfAbsent(playerId, tick) == null;
    }

    @Override
    public void setInvisible(boolean invisible) {
        super.setInvisible(true);
    }

    @Override
    public boolean isInvisible() {
        return true;
    }

    @Override
    public float getMmdRenderOpacity() {
        return 0.0F;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
    }

    @Override
    public void tick() {
        setNoGravity(true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        super.tick();
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("OriginalWhiteBearBlockBound", isNativeBlockBound());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(NATIVE_BLOCK_BOUND, tag.getBoolean("OriginalWhiteBearBlockBound"));
        interactionTicks.clear();
        setNoGravity(true);
        noPhysics = true;
        setInvisible(true);
        setDeltaMovement(Vec3.ZERO);
    }
}

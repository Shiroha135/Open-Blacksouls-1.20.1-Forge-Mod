package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.compat.scene.OriginalReceptionistPlacement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.registries.Registries;

public final class EntityOriginalReceptionist extends EntityOriginalNpc {
    private static final EntityDataAccessor<Boolean> VISUAL_BOUND = SynchedEntityData.defineId(
            EntityOriginalReceptionist.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockState> VISUAL_BLOCK = SynchedEntityData.defineId(
            EntityOriginalReceptionist.class, EntityDataSerializers.BLOCK_STATE);

    public EntityOriginalReceptionist(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNpcId(OriginalReceptionistPlacement.NPC_ID);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(VISUAL_BOUND, false);
        entityData.define(VISUAL_BLOCK, Blocks.AIR.defaultBlockState());
    }

    public boolean isOriginalVisualBound() {
        return entityData.get(VISUAL_BOUND);
    }

    public BlockState visualBlockState() {
        return entityData.get(VISUAL_BLOCK);
    }

    public void bindOriginalVisual(BlockState state) {
        if (level().isClientSide || !OriginalReceptionistPlacement.isOriginalDoll(state)) {
            throw new IllegalArgumentException("Invalid original receptionist visual");
        }
        entityData.set(VISUAL_BLOCK, state);
        entityData.set(VISUAL_BOUND, true);
    }

    @Override
    public void setNpcId(String id) {
        if (!id.isEmpty() && !OriginalReceptionistPlacement.NPC_ID.equals(id)) {
            throw new IllegalArgumentException("Receptionist source identity is immutable");
        }
        super.setNpcId(id);
    }

    @Override
    protected AABB makeBoundingBox() {
        return new AABB(getX() - .25D, getY(), getZ() - .1875D,
                getX() + .25D, getY() + 1.125D, getZ() + .1875D);
    }

    @Override
    public boolean canBeCollidedWith() {
        return isOriginalVisualBound() && !isInvisible() && getSpriteOpacity() > 0 && super.canBeCollidedWith();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("OriginalReceptionistVisualBound", isOriginalVisualBound());
        if (isOriginalVisualBound()) tag.put("OriginalReceptionistVisualBlock", NbtUtils.writeBlockState(visualBlockState()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(VISUAL_BOUND, false);
        entityData.set(VISUAL_BLOCK, Blocks.AIR.defaultBlockState());
        if (tag.getBoolean("OriginalReceptionistVisualBound")) {
            BlockState state = NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK),
                    tag.getCompound("OriginalReceptionistVisualBlock"));
            if (!OriginalReceptionistPlacement.isOriginalDoll(state)) {
                throw new IllegalArgumentException("Invalid saved receptionist visual");
            }
            entityData.set(VISUAL_BLOCK, state);
            entityData.set(VISUAL_BOUND, true);
        }
    }
}

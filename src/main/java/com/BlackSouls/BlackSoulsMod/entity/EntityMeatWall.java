package com.BlackSouls.BlackSoulsMod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class EntityMeatWall extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(EntityMeatWall.class, EntityDataSerializers.OPTIONAL_UUID);
    private long expireGameTime;

    public EntityMeatWall(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER, Optional.empty());
    }

    public void setOwner(Player player) {
        this.entityData.set(DATA_OWNER, Optional.of(player.getUUID()));
    }

    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(DATA_OWNER);
    }

    public boolean isOwnedBy(Player player) {
        return getOwnerUUID().filter(player.getUUID()::equals).isPresent();
    }

    public void setLifetimeTicks(int ticks) {
        this.expireGameTime = this.level().getGameTime() + Math.max(1, ticks);
    }

    public boolean isFriendlyTo(Entity entity) {
        if (entity == null) {
            return false;
        }
        Optional<UUID> ownerId = getOwnerUUID();
        if (ownerId.isEmpty()) {
            return false;
        }
        if (ownerId.get().equals(entity.getUUID())) {
            return true;
        }
        if (entity instanceof EntityMeatWall wall && ownerId.equals(wall.getOwnerUUID())) {
            return true;
        }
        if (entity instanceof TamableAnimal tamable && ownerId.get().equals(tamable.getOwnerUUID())) {
            return true;
        }
        Player owner = this.level().getPlayerByUUID(ownerId.get());
        return owner != null && owner.isAlliedTo(entity);
    }

    @Override
    public boolean isAlliedTo(@NotNull Entity entity) {
        return isFriendlyTo(entity) || super.isAlliedTo(entity);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker == null && source.getDirectEntity() instanceof Projectile projectile) {
            attacker = projectile.getOwner();
        }
        if (isFriendlyTo(attacker)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        if (!this.level().isClientSide && this.expireGameTime > 0L
                && this.level().getGameTime() >= this.expireGameTime) {
            this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        getOwnerUUID().ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putLong("ExpireGameTime", this.expireGameTime);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        this.expireGameTime = tag.getLong("ExpireGameTime");
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}

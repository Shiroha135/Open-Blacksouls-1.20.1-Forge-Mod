package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import com.BlackSouls.BlackSoulsMod.util.BSMobStatManager;
import com.BlackSouls.BlackSoulsMod.util.DifficultyManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public class EntityThrownBlade extends ThrowableItemProjectile {
    public static final int MODE_THROWING_KNIFE = 0;
    public static final int MODE_SCALPEL = 1;

    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(EntityThrownBlade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SURE_HIT = SynchedEntityData.defineId(EntityThrownBlade.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BLEED_TICKS = SynchedEntityData.defineId(EntityThrownBlade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ANIMATION_ID = SynchedEntityData.defineId(EntityThrownBlade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_RENDER_STACK = SynchedEntityData.defineId(EntityThrownBlade.class, EntityDataSerializers.ITEM_STACK);

    public EntityThrownBlade(EntityType<? extends EntityThrownBlade> type, Level level) {
        super(type, level);
    }

    public EntityThrownBlade(Level level, LivingEntity shooter, ItemStack renderStack, int mode, boolean sureHit, int bleedTicks) {
        super(BSEntityRegistry.THROWN_BLADE.get(), shooter, level);
        this.setOwner(shooter);
        this.entityData.set(DATA_RENDER_STACK, renderStack.copy());
        this.entityData.set(DATA_MODE, mode);
        this.entityData.set(DATA_SURE_HIT, sureHit);
        this.entityData.set(DATA_BLEED_TICKS, bleedTicks);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MODE, MODE_THROWING_KNIFE);
        this.entityData.define(DATA_SURE_HIT, false);
        this.entityData.define(DATA_BLEED_TICKS, 0);
        this.entityData.define(DATA_ANIMATION_ID, 0);
        this.entityData.define(DATA_RENDER_STACK, ItemStack.EMPTY);
    }

    public boolean isSureHit() {
        return this.entityData.get(DATA_SURE_HIT);
    }

    public void setAnimationId(int animationId) {
        this.entityData.set(DATA_ANIMATION_ID, animationId);
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        if (!this.level().isClientSide() && this.tickCount > 40) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide()) {
            return;
        }

        Entity owner = this.getOwner();
        Entity target = result.getEntity();
        if (target == owner || !(target instanceof LivingEntity livingTarget)) {
            this.discard();
            return;
        }

        float damage = computeDamage(livingTarget);
        if (damage <= 0.0F) {
            this.discard();
            return;
        }

        DamageSource source = owner instanceof LivingEntity livingOwner
                ? this.damageSources().thrown(this, livingOwner)
                : this.damageSources().thrown(this, this);

        livingTarget.invulnerableTime = 0;
        int animationId = this.entityData.get(DATA_ANIMATION_ID);
        if (animationId > 0) {
            NetworkHandler.sendToAllAround(
                    new PacketPlayAnim(animationId, livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() / 2.0F, livingTarget.getZ()),
                    livingTarget
            );
        }
        if (livingTarget.hurt(source, damage)) {
            int bleedTicks = this.entityData.get(DATA_BLEED_TICKS);
            if (bleedTicks > 0 && BlackSouls.BUFF_BLEEDING.isPresent()) {
                livingTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(BlackSouls.BUFF_BLEEDING.get(), bleedTicks, 0));
            }
        }

        this.discard();
    }

    private float computeDamage(LivingEntity target) {
        int mode = this.entityData.get(DATA_MODE);
        if (mode == MODE_SCALPEL) {
            return 10.0F;
        }

        Entity owner = this.getOwner();
        if (!(owner instanceof Player player)) {
            return 1.0F;
        }

        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) {
            return 1.0F;
        }

        double attackerAgi = stats.speed;
        double targetDefense = resolveTargetDefense(target);
        double rawDamage = 50.0D + attackerAgi - targetDefense * 2.0D;
        rawDamage *= 0.8D + Math.random() * 0.4D;
        return (float) Math.max(1.0D, rawDamage);
    }

    private double resolveTargetDefense(LivingEntity target) {
        if (target instanceof Player targetPlayer) {
            BSPlayerStats targetStats = targetPlayer.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            return targetStats != null ? targetStats.defense : target.getArmorValue();
        }
        return target.getArmorValue() + DifficultyManager.scaleManagedStat(
                target.level(),
                BSMobStatManager.getStats(target).defense
        );
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide() && result.getType() != HitResult.Type.ENTITY) {
            this.discard();
        }
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        ItemStack stack = this.entityData.get(DATA_RENDER_STACK);
        return stack.isEmpty() ? BlackSouls.THROWING_KNIFE.get() : stack.getItem();
    }

    @Override
    public @NotNull ItemStack getItem() {
        ItemStack stack = this.entityData.get(DATA_RENDER_STACK);
        return stack.isEmpty() ? new ItemStack(getDefaultItem()) : stack;
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Mode", this.entityData.get(DATA_MODE));
        tag.putBoolean("SureHit", this.entityData.get(DATA_SURE_HIT));
        tag.putInt("BleedTicks", this.entityData.get(DATA_BLEED_TICKS));
        tag.putInt("AnimationId", this.entityData.get(DATA_ANIMATION_ID));
        if (!getItem().isEmpty()) {
            tag.put("RenderStack", getItem().save(new CompoundTag()));
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_MODE, tag.getInt("Mode"));
        this.entityData.set(DATA_SURE_HIT, tag.getBoolean("SureHit"));
        this.entityData.set(DATA_BLEED_TICKS, tag.getInt("BleedTicks"));
        this.entityData.set(DATA_ANIMATION_ID, tag.getInt("AnimationId"));
        if (tag.contains("RenderStack")) {
            this.entityData.set(DATA_RENDER_STACK, ItemStack.of(tag.getCompound("RenderStack")));
        }
    }
}

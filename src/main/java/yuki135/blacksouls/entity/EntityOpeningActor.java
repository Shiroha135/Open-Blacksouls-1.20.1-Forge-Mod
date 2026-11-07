package yuki135.blacksouls.entity;

import yuki135.blacksouls.story.OriginalOpeningScenes;
import yuki135.blacksouls.story.OriginalOpeningService;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public final class EntityOpeningActor extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(EntityOpeningActor.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> ORIGIN=SynchedEntityData.defineId(EntityOpeningActor.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RELATIONSHIP=SynchedEntityData.defineId(EntityOpeningActor.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LEASE_SLOT=SynchedEntityData.defineId(EntityOpeningActor.class,EntityDataSerializers.INT);

    public EntityOpeningActor(EntityType<? extends EntityOpeningActor> type,Level level) {
        super(type,level);setPersistenceRequired();setNoAi(true);setNoGravity(true);setCanPickUpLoot(false);noPhysics=true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,1.0D).add(Attributes.MOVEMENT_SPEED,0.0D).add(Attributes.FOLLOW_RANGE,1.0D);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();entityData.define(OWNER,Optional.empty());entityData.define(ORIGIN,-1);entityData.define(RELATIONSHIP,0);entityData.define(LEASE_SLOT,-1);
    }

    public UUID ownerId() { return entityData.get(OWNER).orElse(null); }
    public int originId() { return entityData.get(ORIGIN); }
    public int relationshipId() { return entityData.get(RELATIONSHIP); }
    public int leaseSlot() { return entityData.get(LEASE_SLOT); }

    public void configure(UUID owner,int origin,int relationship,int slot) {
        if (owner==null || origin<0 || origin>9 || relationship<0 || relationship>3 || slot<0 || slot>=OriginalOpeningScenes.MAX_SLOTS) throw new IllegalArgumentException("Invalid opening actor identity");
        entityData.set(OWNER,Optional.of(owner));entityData.set(ORIGIN,origin);entityData.set(RELATIONSHIP,relationship);entityData.set(LEASE_SLOT,slot);
        setPersistenceRequired();setNoAi(true);setNoGravity(true);setCanPickUpLoot(false);noPhysics=true;
    }

    @Override public void tick() {
        if (!level().isClientSide() && (!OriginalOpeningScenes.DIMENSION.equals(level().dimension()) || ownerId()==null || originId()<0 || leaseSlot()<0
                || !OriginalOpeningScenes.contains(new yuki135.blacksouls.story.OriginalOpeningSceneData.Lease(ownerId(),leaseSlot()),
                        originId()==0?OriginalOpeningScenes.SceneKind.ISLAND_110:OriginalOpeningScenes.SceneKind.OP,blockPosition()))) { discard();return; }
        noPhysics=true;setNoAi(true);setNoGravity(true);setDeltaMovement(0,0,0);fallDistance=0;super.tick();setDeltaMovement(0,0,0);
    }

    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand) {
        if (hand!=InteractionHand.MAIN_HAND || ownerId()==null || !ownerId().equals(player.getUUID()) || !OriginalOpeningScenes.DIMENSION.equals(level().dimension())
                || player.level()!=level() || player.distanceToSqr(this)>36.0D) return InteractionResult.PASS;
        return player instanceof ServerPlayer serverPlayer ? OriginalOpeningService.interactActor(serverPlayer,this) : InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.putInt("OpeningActorVersion",1);
        if (ownerId()!=null) tag.putUUID("OpeningActorOwner",ownerId());
        tag.putInt("OpeningActorOrigin",originId());tag.putInt("OpeningActorRelationship",relationshipId());tag.putInt("OpeningActorLeaseSlot",leaseSlot());
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("OpeningActorVersion",Tag.TAG_INT) || tag.getInt("OpeningActorVersion")!=1 || !tag.hasUUID("OpeningActorOwner")
                || !tag.contains("OpeningActorOrigin",Tag.TAG_INT) || !tag.contains("OpeningActorRelationship",Tag.TAG_INT) || !tag.contains("OpeningActorLeaseSlot",Tag.TAG_INT)) throw new IllegalArgumentException("Invalid persisted opening actor schema");
        configure(tag.getUUID("OpeningActorOwner"),tag.getInt("OpeningActorOrigin"),tag.getInt("OpeningActorRelationship"),tag.getInt("OpeningActorLeaseSlot"));
    }

    @Override protected ResourceLocation getDefaultLootTable() { return BuiltInLootTables.EMPTY; }
    @Override protected boolean shouldDropLoot() { return false; }
    @Override public boolean hurt(DamageSource source,float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPickable() { return ownerId()!=null && isAlive(); }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean canCollideWith(Entity entity) { return false; }
    @Override public boolean canChangeDimensions() { return false; }
    @Override public boolean canBeLeashed(Player player) { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
}

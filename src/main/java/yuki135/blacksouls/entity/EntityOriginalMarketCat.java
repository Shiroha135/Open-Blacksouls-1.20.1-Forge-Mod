package yuki135.blacksouls.entity;

import yuki135.blacksouls.compat.scene.OriginalMarketCatPlacement;
import net.minecraft.nbt.CompoundTag;
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

public final class EntityOriginalMarketCat extends EntityOriginalNpc {
    private boolean nativeBlockBound;
    private final Map<UUID, Long> interactionTicks = new HashMap<>();

    public EntityOriginalMarketCat(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNpcId(OriginalMarketCatPlacement.NPC_ID);
        setNoGravity(true);
        noPhysics = true;
        setInvisible(true);
    }

    @Override
    public void setNpcId(String id) {
        if (!OriginalMarketCatPlacement.NPC_ID.equals(id)) {
            throw new IllegalArgumentException("Market cat source identity is immutable");
        }
        super.setNpcId(id);
    }

    public void bindNativeBlock() {
        if (level().isClientSide || position().distanceToSqr(OriginalMarketCatPlacement.POSITION) > 1.0E-8D) {
            throw new IllegalStateException("Invalid market cat anchor");
        }
        nativeBlockBound = true;
    }

    public boolean isNativeBlockBound() {
        return nativeBlockBound;
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
        tag.putBoolean("OriginalMarketCatBlockBound", nativeBlockBound);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nativeBlockBound = tag.getBoolean("OriginalMarketCatBlockBound");
        interactionTicks.clear();
        setNoGravity(true);
        noPhysics = true;
        setInvisible(true);
        setDeltaMovement(Vec3.ZERO);
    }
}

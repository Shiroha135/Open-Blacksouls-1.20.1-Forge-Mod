package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.handler.RedHoodStoryHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketOpenDialogue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class EntityRedHood extends PathfinderMob implements DialogueResettable {
    private int storyStage;
    private String dialogueScene = "intro";
    private boolean storyInitialized;
    private String anchorDimension = "";
    private long anchorPosition;

    public EntityRedHood(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCustomName(Component.translatable("entity.blacksouls.red_hood.name"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1024.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void setStoryContext(int stage, String scene, GlobalPos anchor) {
        this.storyStage = Math.max(0, stage);
        this.dialogueScene = scene == null || scene.isBlank() ? "intro" : scene;
        this.storyInitialized = true;
        if (anchor == null) {
            this.anchorDimension = "";
            this.anchorPosition = 0L;
        } else {
            this.anchorDimension = anchor.dimension().location().toString();
            this.anchorPosition = anchor.pos().asLong();
        }
    }

    public int getStoryStage() {
        return this.storyStage;
    }

    public boolean isStoryInitialized() {
        return this.storyInitialized;
    }

    public Optional<GlobalPos> getAnchorBonfire() {
        if (this.anchorDimension.isBlank()) {
            return Optional.empty();
        }
        ResourceLocation location = ResourceLocation.tryParse(this.anchorDimension);
        if (location == null) {
            return Optional.empty();
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, location);
        return Optional.of(GlobalPos.of(dimension, BlockPos.of(this.anchorPosition)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && !this.storyInitialized) {
            RedHoodStoryHandler.initializePlacedEntity(this);
        }
        this.setDeltaMovement(0.0D, this.onGround() ? 0.0D : this.getDeltaMovement().y, 0.0D);
        this.yBodyRot = this.getYRot();
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180F / Math.PI)) - 90F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (!this.storyInitialized) {
                RedHoodStoryHandler.initializePlacedEntity(this);
            }
            NetworkHandler.sendToPlayer(
                    new PacketOpenDialogue(
                            "entity.blacksouls.red_hood.name",
                            "red_riding_hood",
                            RedHoodDialogue.keysFor(this.dialogueScene, this.storyStage),
                            false,
                            this.getId(),
                            -1,
                            true,
                            this.storyStage
                    ),
                    serverPlayer
            );
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void resetDialogue(ServerPlayer player) {
        RedHoodStoryHandler.reset(player, this);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RedHoodStoryStage", this.storyStage);
        tag.putString("RedHoodDialogueScene", this.dialogueScene);
        tag.putBoolean("RedHoodStoryInitialized", this.storyInitialized);
        tag.putString("RedHoodAnchorDimension", this.anchorDimension);
        tag.putLong("RedHoodAnchorPosition", this.anchorPosition);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.storyStage = Math.max(0, tag.getInt("RedHoodStoryStage"));
        this.dialogueScene = tag.getString("RedHoodDialogueScene");
        if (this.dialogueScene.isBlank()) {
            this.dialogueScene = RedHoodDialogue.resolveScene("", this.storyStage);
        }
        this.storyInitialized = tag.getBoolean("RedHoodStoryInitialized");
        this.anchorDimension = tag.getString("RedHoodAnchorDimension");
        this.anchorPosition = tag.getLong("RedHoodAnchorPosition");
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }
}

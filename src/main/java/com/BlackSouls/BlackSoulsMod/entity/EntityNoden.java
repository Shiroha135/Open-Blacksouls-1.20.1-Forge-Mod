package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketOpenDialogue;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

public class EntityNoden extends PathfinderMob implements DialogueResettable {

    private static final double NODEN_MAX_HEALTH = Integer.MAX_VALUE;

    private static final EntityDataAccessor<Integer> DATA_TALK_COUNT = SynchedEntityData.defineId(EntityNoden.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SITTING = SynchedEntityData.defineId(EntityNoden.class, EntityDataSerializers.BOOLEAN);

    public EntityNoden(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCustomName(Component.translatable("entity.blacksouls.noden.name"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, NODEN_MAX_HEALTH);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TALK_COUNT, 0);
        this.entityData.define(DATA_SITTING, false);
    }

    public boolean isSitting() {
        return this.entityData.get(DATA_SITTING);
    }

    public void setSitting(boolean sitting) {
        this.entityData.set(DATA_SITTING, sitting);
    }

    @Override
    public void resetDialogue(ServerPlayer player) {
        this.entityData.set(DATA_TALK_COUNT, 0);
        player.getCapability(com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats.CAPABILITY).ifPresent(stats -> {
            stats.unlockedCovenants.remove("noden");
            if ("noden".equals(stats.activeCovenant)) {
                stats.activeCovenant = "";
            }
            stats.nodenCovenantLevel = 0;
            NetworkHandler.sendToPlayer(
                    new com.BlackSouls.BlackSoulsMod.network.packets.PacketSyncStats(stats.serializeNBT()),
                    player
            );
        });
        Advancement advancement = player.server.getAdvancements()
                .getAdvancement(new ResourceLocation("blacksouls", "first_talk_noden"));
        if (advancement != null) {
            net.minecraft.advancements.AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
            java.util.List<String> completedCriteria = new java.util.ArrayList<>();
            progress.getCompletedCriteria().forEach(completedCriteria::add);
            for (String criterion : completedCriteria) {
                player.getAdvancements().revoke(advancement, criterion);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount == 1 && this.getHealth() < this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
        if (this.isSitting()) {
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            this.yBodyRot = this.getYRot();
        }
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {

            if (player.isCrouching()) {
                if (!this.level().isClientSide) {
                    this.setSitting(!this.isSitting());
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            double d0 = player.getX() - this.getX();
            double d1 = player.getZ() - this.getZ();
            float yaw = (float)(net.minecraft.util.Mth.atan2(d1, d0) * (180F / Math.PI)) - 90F;

            this.setYRot(yaw);
            this.setYHeadRot(yaw);
            this.yBodyRot = yaw;
            this.getLookControl().setLookAt(player, 30.0F, 30.0F);

            if (!this.level().isClientSide) {
                int talkCount = this.entityData.get(DATA_TALK_COUNT);
                String nameKey = "entity.blacksouls.noden.name";
                String avatarId = "white_rabbit_noden";
                String[] dialogues;

                if (talkCount == 0) {
                    dialogues = new String[]{"dialogue.blacksouls.noden.first_1", "dialogue.blacksouls.noden.first_2", "dialogue.blacksouls.noden.first_3"};
                } else if (talkCount == 1) {
                    dialogues = new String[]{"dialogue.blacksouls.noden.second_1", "dialogue.blacksouls.noden.second_2", "dialogue.blacksouls.noden.second_3", "dialogue.blacksouls.noden.second_4"};
                } else {
                    dialogues = new String[]{"dialogue.blacksouls.noden.later_1"};
                }

                boolean isLaterDialogue = (talkCount >= 2);
                int covLevel = player.getCapability(com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats.CAPABILITY)
                        .map(stats -> stats.unlockedCovenants.contains("noden") ? stats.nodenCovenantLevel : -1)
                        .orElse(-1);

                if (player instanceof ServerPlayer serverPlayer) {
                    NetworkHandler.sendToPlayer(
                            new PacketOpenDialogue(nameKey, avatarId, dialogues, isLaterDialogue, this.getId(), covLevel),
                            serverPlayer
                    );
                    if (talkCount == 0) {
                        Advancement advancement = serverPlayer.server.getAdvancements()
                                .getAdvancement(new ResourceLocation("blacksouls", "first_talk_noden"));
                        if (advancement != null) {
                            serverPlayer.getAdvancements().award(advancement, "talked");
                        }
                    }
                }

                if (talkCount < 2) {
                    this.entityData.set(DATA_TALK_COUNT, talkCount + 1);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TalkCount", this.entityData.get(DATA_TALK_COUNT));
        tag.putBoolean("IsSitting", this.isSitting());
    }


    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_TALK_COUNT, tag.getInt("TalkCount"));
        this.setSitting(tag.getBoolean("IsSitting"));
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) { return false; }
    @Override
    public boolean isPushable() { return false; }
    @Override
    public boolean shouldShowName() { return true; }
}

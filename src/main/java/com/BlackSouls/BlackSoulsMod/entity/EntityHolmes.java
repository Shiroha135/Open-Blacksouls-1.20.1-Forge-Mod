package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketOpenDialogue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 路德维希市街的福尔摩斯（原事件 Map002 / EV059）。
 * 三段式剧情：初见警告 → 受伤赠药 → 常态对话（询问爱丽丝 / 杀害 / 离开）。
 */
public final class EntityHolmes extends PathfinderMob implements DialogueResettable {
    private boolean introduced;
    private boolean woundedHandled;

    public EntityHolmes(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCustomName(Component.translatable("entity.blacksouls.holmes.name"));
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

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0.0D, this.onGround() ? 0.0D : this.getDeltaMovement().y, 0.0D);
        this.yBodyRot = this.getYRot();
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            facePlayer(player);
            if (!this.introduced) {
                open(serverPlayer, HolmesDialogue.introductionKeys(), false, HolmesDialogue.Mode.INTRO);
            } else if (!this.woundedHandled) {
                open(serverPlayer, HolmesDialogue.woundedKeys(), false, HolmesDialogue.Mode.WOUNDED);
            } else {
                open(serverPlayer, HolmesDialogue.mainKeys(), true, HolmesDialogue.Mode.MAIN);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    private void open(ServerPlayer player, String[] keys, boolean showOptions, HolmesDialogue.Mode mode) {
        NetworkHandler.sendToPlayer(new PacketOpenDialogue(
                "entity.blacksouls.holmes.name",
                "holmes",
                keys,
                showOptions,
                this.getId(),
                -1,
                false,
                -1,
                false,
                CheshireDialogue.Mode.NONE,
                mode
        ), player);
    }

    private void facePlayer(Player player) {
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0F / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;
    }

    /** 玩家读完某段对话（客户端回包）：推进剧情 / 发放输血药。 */
    public boolean completeStage(ServerPlayer player, HolmesDialogue.Mode stage) {
        if (this.distanceToSqr(player) > 64.0D) {
            return false;
        }
        switch (stage) {
            case INTRO -> {
                if (this.introduced) {
                    return false;
                }
                this.introduced = true;
            }
            case WOUNDED -> {
                if (!this.introduced || this.woundedHandled) {
                    return false;
                }
                this.woundedHandled = true;
                giveItem(player, new ItemStack(BlackSouls.BLOOD_VIAL.get(), 3));
                player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.inventoryMenu.broadcastChanges();
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** 常态「杀害」选项：抹除这个 NPC（与其它对话 NPC 一致）。 */
    public void killByPlayer(ServerPlayer player) {
        this.level().playSound(null, this.blockPosition(), BlackSouls.GUCHA004A_EVENT.get(),
                SoundSource.NEUTRAL, 1.0F, 1.0F);
        this.discard();
    }

    private static void giveItem(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public void resetDialogue(ServerPlayer player) {
        this.introduced = false;
        this.woundedHandled = false;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HolmesIntroduced", this.introduced);
        tag.putBoolean("HolmesWoundedHandled", this.woundedHandled);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.introduced = tag.getBoolean("HolmesIntroduced");
        this.woundedHandled = tag.getBoolean("HolmesWoundedHandled");
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

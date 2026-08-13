package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketOpenDialogue;
import com.BlackSouls.BlackSoulsMod.util.PlayerStatService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class EntityRabbitKnight extends EntityOriginalDatabaseEnemy
        implements DialogueResettable {
    private boolean introduced;
    private boolean killBattleArmed;

    public EntityRabbitKnight(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setProfileId(217);
        setCustomName(Component.translatable("entity.blacksouls.rabbit_knight.name"));
        setCustomNameVisible(true);
        setPersistenceRequired();
        setNoAi(true);
        setInvulnerable(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (!TurnBattleManager.isInBattle(this)) {
            setDeltaMovement(Vec3.ZERO);
            setNoAi(true);
            setInvulnerable(true);
        }
    }

    @Override
    protected @NotNull InteractionResult mobInteract(
            @NotNull Player player, @NotNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || TurnBattleManager.isInBattle(this)) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            boolean firstMeeting = !introduced;
            introduced = true;
            NetworkHandler.sendToPlayer(new PacketOpenDialogue(
                    "entity.blacksouls.rabbit_knight.name",
                    "rabbit_knight",
                    firstMeeting ? RabbitKnightDialogue.introductionKeys()
                            : RabbitKnightDialogue.repeatKeys(),
                    !firstMeeting,
                    getId(),
                    -1,
                    false,
                    -1,
                    false,
                    firstMeeting ? CheshireDialogue.Mode.NONE
                            : CheshireDialogue.Mode.RABBIT_KNIGHT
            ), serverPlayer);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public boolean startKillBattle(ServerPlayer player) {
        if (distanceToSqr(player) > 64.0D || TurnBattleManager.isInBattle(this)) {
            return false;
        }
        killBattleArmed = true;
        setInvulnerable(false);
        TurnBattleManager.tryStart(player, this);
        if (!TurnBattleManager.isInBattle(this)) {
            killBattleArmed = false;
            setInvulnerable(true);
            return false;
        }
        return true;
    }

    public void finishKnightBattle(ServerPlayer player, boolean victory) {
        killBattleArmed = false;
        if (victory) {
            PlayerStatService.addSen(player, -5);
            return;
        }
        setNoAi(true);
        setInvulnerable(true);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return TurnBattleManager.isInBattle(this) && super.hurt(source, amount);
    }

    @Override
    public boolean canStartTurnBattle() {
        return killBattleArmed;
    }

    @Override
    public void resetDialogue(ServerPlayer player) {
        introduced = false;
        killBattleArmed = false;
        setTarget(null);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        setNoAi(true);
        setInvulnerable(true);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("RabbitKnightIntroduced", introduced);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setProfileId(217);
        introduced = tag.getBoolean("RabbitKnightIntroduced");
        killBattleArmed = false;
        setNoAi(true);
        setInvulnerable(true);
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

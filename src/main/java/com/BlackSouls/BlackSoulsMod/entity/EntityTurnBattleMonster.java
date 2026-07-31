package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.BSConfig;
import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public abstract class EntityTurnBattleMonster extends Monster {
    private int battleCooldown;

    protected EntityTurnBattleMonster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.battleCooldown > 0) {
            this.battleCooldown--;
            this.setTarget(null);
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            if (this.battleCooldown == 0 && !TurnBattleManager.isInBattle(this)) {
                this.setNoAi(false);
            }
        }
        if (this.battleCooldown == 0
                && BSConfig.COMBAT_MODE.get() == BSConfig.CombatMode.BLACK_SOULS_TURN_BASED
                && !TurnBattleManager.isInBattle(this)) {
            this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(0.35D),
                            player -> player.isAlive() && !player.isSpectator())
                    .stream().findFirst().ifPresent(player -> TurnBattleManager.tryStart(player, this));
        }
    }

    @Override
    public void playerTouch(Player player) {
        super.playerTouch(player);
        if (!this.level().isClientSide() && this.battleCooldown == 0 && player instanceof ServerPlayer serverPlayer) {
            TurnBattleManager.tryStart(serverPlayer, this);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (this.battleCooldown > 0) {
            this.setTarget(null);
            return false;
        }
        if (target instanceof ServerPlayer player
                && BSConfig.COMBAT_MODE.get() == BSConfig.CombatMode.BLACK_SOULS_TURN_BASED) {
            TurnBattleManager.tryStart(player, this);
            return false;
        }
        return super.doHurtTarget(target);
    }

    public void setBattleCooldown(int ticks) {
        this.battleCooldown = Math.max(this.battleCooldown, ticks);
        if (this.battleCooldown > 0) {
            this.setTarget(null);
            this.getNavigation().stop();
            this.setNoAi(true);
        }
    }

    public abstract ResourceLocation getTurnBattleTexture();

    public abstract int getTurnBattleTextureWidth();

    public abstract int getTurnBattleTextureHeight();

    public abstract double getTurnBattleAttack();

    public abstract double getTurnBattleDefense();

    public double getTurnBattleMagicAttack() {
        return getTurnBattleAttack();
    }

    public double getTurnBattleMagicDefense() {
        return getTurnBattleDefense();
    }

    public abstract double getTurnBattleAgility();

    public double getTurnBattleLuck() {
        return 10.0D;
    }

    public double getTurnBattleMana() {
        return 0.0D;
    }

    public double getTurnBattleMaxMana() {
        return getTurnBattleMana();
    }

    public abstract long getTurnBattleSoulReward();

    public abstract String getTurnBattleAttackText();

    public int getTurnBattleAttackAnimationId() {
        return 1;
    }

    public int getTurnBattleAttackRepeats() {
        return 1;
    }

    public double getTurnBattleHealth() {
        return getHealth();
    }

    public double getTurnBattleMaxHealth() {
        return getMaxHealth();
    }

    public void setTurnBattleHealth(double health) {
        setHealth((float) Math.max(0.0D, Math.min(getMaxHealth(), health)));
    }

    public boolean isTurnBattleDefeated() {
        return getTurnBattleHealth() <= 0.0D || !isAlive();
    }

    public List<ItemStack> rollTurnBattleDrops(RandomSource random) {
        return List.of();
    }
}

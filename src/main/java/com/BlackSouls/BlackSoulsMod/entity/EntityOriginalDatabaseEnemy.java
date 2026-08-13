package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalEnemyData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalEnemyPhaseData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EntityOriginalDatabaseEnemy extends EntityTurnBattleMonster {
    private static final EntityDataAccessor<Integer> PROFILE_ID =
            SynchedEntityData.defineId(EntityOriginalDatabaseEnemy.class, EntityDataSerializers.INT);
    private double turnBattleHealth;
    private double turnBattleDifficultyMultiplier = 1.0D;

    public EntityOriginalDatabaseEnemy(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setProfileId(1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1024.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.LUCK, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PROFILE_ID, 1);
    }

    public int getProfileId() {
        return this.entityData.get(PROFILE_ID);
    }

    public void setProfileId(int profileId) {
        this.entityData.set(PROFILE_ID, BSOriginalEnemyData.get(profileId).id());
        applyProfile(true);
    }

    public BSOriginalEnemyData.Entry getProfile() {
        return BSOriginalEnemyData.get(getProfileId());
    }

    private void applyProfile(boolean resetHealth) {
        BSOriginalEnemyData.Entry profile = getProfile();
        setAttributeBase(Attributes.MAX_HEALTH, Math.max(1.0D, Math.min(1024.0D, profile.health())));
        setAttributeBase(Attributes.ATTACK_DAMAGE, Math.max(1.0D, Math.min(2048.0D, profile.attack())));
        setAttributeBase(Attributes.ARMOR, Math.max(0.0D, Math.min(30.0D, profile.defense())));
        setAttributeBase(Attributes.MOVEMENT_SPEED, profile.movementSpeed());
        setAttributeBase(Attributes.LUCK, Math.max(-1024.0D, Math.min(1024.0D, profile.luck())));
        double scaledMaxHealth = profile.health() * this.turnBattleDifficultyMultiplier;
        if (resetHealth) {
            this.turnBattleHealth = scaledMaxHealth;
        } else {
            this.turnBattleHealth = Math.max(0.0D,
                    Math.min(scaledMaxHealth, this.turnBattleHealth));
        }
        super.setHealth((float) Math.max(1.0D,
                Math.min(getMaxHealth(), this.turnBattleHealth)));
        refreshDimensions();
    }

    private void setAttributeBase(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (PROFILE_ID.equals(accessor)) {
            applyProfile(false);
        }
    }

    @Override
    public EntityDimensions getDimensions(@NotNull Pose pose) {
        BSOriginalEnemyData.Entry profile = BSOriginalEnemyData.get(
                this.entityData == null ? 1 : getProfileId());
        float height = Math.max(0.75F, Math.min(1.95F, profile.worldRenderHeight()));
        float width = Math.max(0.45F, Math.min(0.85F, height * profile.aspectRatio() * 0.35F));
        return EntityDimensions.scalable(width, height);
    }

    @Override
    public @NotNull Component getName() {
        return hasCustomName() ? getCustomName() : Component.literal(getProfile().name());
    }

    @Override
    public ResourceLocation getTurnBattleTexture() {
        return getProfile().texture();
    }

    @Override
    public int getTurnBattleTextureWidth() {
        return getProfile().textureWidth();
    }

    @Override
    public int getTurnBattleTextureHeight() {
        return getProfile().textureHeight();
    }

    @Override
    public double getTurnBattleAttack() {
        return getProfile().attack();
    }

    @Override
    public double getTurnBattleDefense() {
        return getProfile().defense();
    }

    @Override
    public double getTurnBattleMagicAttack() {
        return getProfile().magicAttack();
    }

    @Override
    public double getTurnBattleMagicDefense() {
        return getProfile().magicDefense();
    }

    @Override
    public double getTurnBattleAgility() {
        return getProfile().agility();
    }

    @Override
    public double getTurnBattleLuck() {
        return getProfile().luck();
    }

    @Override
    public double getTurnBattleMana() {
        return getProfile().mp();
    }

    @Override
    public double getTurnBattleMaxMana() {
        return getProfile().mp();
    }

    @Override
    public long getTurnBattleSoulReward() {
        return getProfile().souls();
    }

    @Override
    public String getTurnBattleAttackText() {
        return getProfile().attackText();
    }

    @Override
    public int getTurnBattleAttackAnimationId() {
        return getProfile().attackAnimationId();
    }

    @Override
    public int getTurnBattleAttackRepeats() {
        return Math.max(1, getProfile().attackRepeats());
    }

    public BSOriginalEnemyData.Action selectTurnBattleAction(RandomSource random, int turn,
                                                              Set<Integer> activeStates) {
        List<BSOriginalEnemyData.Action> valid = getProfile().actions().stream()
                .filter(BSOriginalEnemyData.Action::selectable)
                .filter(action -> isActionConditionMet(action, turn, activeStates))
                .toList();
        if (valid.isEmpty()) {
            valid = getProfile().actions().stream()
                    .filter(BSOriginalEnemyData.Action::selectable)
                    .filter(action -> action.conditionType() == 0)
                    .toList();
        }
        if (valid.isEmpty()) {
            return null;
        }
        int maximumRating = valid.stream().mapToInt(BSOriginalEnemyData.Action::rating).max().orElse(5);
        int ratingFloor = maximumRating - 3;
        int totalWeight = valid.stream().mapToInt(action -> Math.max(0, action.rating() - ratingFloor)).sum();
        if (totalWeight <= 0) {
            return valid.get(random.nextInt(valid.size()));
        }
        int roll = random.nextInt(totalWeight);
        for (BSOriginalEnemyData.Action action : valid) {
            roll -= Math.max(0, action.rating() - ratingFloor);
            if (roll < 0) {
                return action;
            }
        }
        return valid.get(valid.size() - 1);
    }

    private boolean isActionConditionMet(BSOriginalEnemyData.Action action, int turn,
                                         Set<Integer> activeStates) {
        return switch (action.conditionType()) {
            case 0 -> true;
            case 1 -> {
                int firstTurn = Math.max(1, (int) action.conditionParam1());
                int interval = Math.max(0, (int) action.conditionParam2());
                yield turn == firstTurn || interval > 0 && turn > firstTurn
                        && (turn - firstTurn) % interval == 0;
            }
            case 2 -> {
                double healthRate = getTurnBattleHealth() / Math.max(1.0D, getTurnBattleMaxHealth());
                yield healthRate >= action.conditionParam1() && healthRate <= action.conditionParam2();
            }
            case 3 -> {
                double manaRate = getTurnBattleMana() / Math.max(1.0D, getTurnBattleMaxMana());
                yield manaRate >= action.conditionParam1() && manaRate <= action.conditionParam2();
            }
            case 4, 6 -> {
                int stateId = resolveTurnBattleActionConditionState(action);
                yield stateId > 0 && activeStates.contains(stateId);
            }
            default -> false;
        };
    }

    public int resolveTurnBattleActionConditionState(BSOriginalEnemyData.Action action) {
        if (action.conditionType() == 4) {
            return (int) action.conditionParam1();
        }
        if (getProfileId() == 184
                && action.skillId() == 53
                && action.conditionType() == 6
                && (int) action.conditionParam1() == 24) {
            return 32;
        }
        return 0;
    }

    @Override
    public double getTurnBattleHealth() {
        return this.turnBattleHealth;
    }

    @Override
    public double getTurnBattleMaxHealth() {
        return getProfile().health() * this.turnBattleDifficultyMultiplier;
    }

    public void setTurnBattleDifficultyMultiplier(double multiplier) {
        double oldMaxHealth = getTurnBattleMaxHealth();
        double healthRatio = oldMaxHealth <= 0.0D ? 1.0D
                : this.turnBattleHealth / oldMaxHealth;
        this.turnBattleDifficultyMultiplier = Math.max(1.0D, multiplier);
        this.turnBattleHealth = getTurnBattleMaxHealth()
                * Math.max(0.0D, Math.min(1.0D, healthRatio));
        super.setHealth((float) Math.max(1.0D,
                Math.min(getMaxHealth(), this.turnBattleHealth)));
    }

    @Override
    public void setTurnBattleHealth(double health) {
        this.turnBattleHealth = Math.max(0.0D, Math.min(getTurnBattleMaxHealth(), health));
        boolean keepVirtualEnemyAlive = TurnBattleManager.isInBattle(this);
        super.setHealth(this.turnBattleHealth <= 0.0D && !keepVirtualEnemyAlive ? 0.0F
                : (float) Math.max(1.0D, Math.min(getMaxHealth(), this.turnBattleHealth)));
    }

    public BSOriginalEnemyPhaseData.Transition tryAdvanceTurnBattlePhase() {
        BSOriginalEnemyPhaseData.Transition transition =
                BSOriginalEnemyPhaseData.get(getProfileId());
        if (transition == null) {
            return null;
        }
        double ratio = getTurnBattleHealth() * 100.0D
                / Math.max(1.0D, getTurnBattleMaxHealth());
        if (ratio > transition.thresholdPercent()) {
            return null;
        }
        double previousHealth = getTurnBattleHealth();
        this.entityData.set(PROFILE_ID, transition.to());
        applyProfile(false);
        double nextHealth;
        if (transition.recoverAll()) {
            nextHealth = getTurnBattleMaxHealth();
        } else if (previousHealth > 0.0D) {
            nextHealth = Math.min(previousHealth, getTurnBattleMaxHealth());
        } else {
            nextHealth = Math.max(1.0D, getTurnBattleMaxHealth()
                    * Math.max(0.01D, transition.thresholdPercent() / 100.0D));
        }
        setTurnBattleHealth(nextHealth);
        return transition;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (amount <= 0.0F || this.turnBattleHealth <= 0.0D) {
            return false;
        }
        float safeAmount = Math.min(amount, Math.max(0.001F, getHealth() - 1.0F));
        boolean accepted = super.hurt(source, safeAmount);
        if (!accepted) {
            return false;
        }
        setTurnBattleHealth(this.turnBattleHealth - amount);
        return true;
    }

    @Override
    public List<ItemStack> rollTurnBattleDrops(RandomSource random) {
        List<ItemStack> result = new ArrayList<>();
        for (BSOriginalEnemyData.Drop drop : getProfile().drops()) {
            if (random.nextInt(Math.max(1, drop.denominator())) != 0) {
                continue;
            }
            Item item = ForgeRegistries.ITEMS.getValue(
                    new ResourceLocation(BlackSouls.MODID, drop.item()));
            if (item != null && item != BuiltInRegistries.ITEM.get(
                    new ResourceLocation("minecraft", "air"))) {
                result.add(new ItemStack(item));
            }
        }
        return result;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("OriginalEnemyId", getProfileId());
        tag.putDouble("OriginalBattleHealth", this.turnBattleHealth);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setProfileId(tag.contains("OriginalEnemyId") ? tag.getInt("OriginalEnemyId") : 1);
        if (tag.contains("OriginalBattleHealth")) {
            setTurnBattleHealth(tag.getDouble("OriginalBattleHealth"));
        }
    }
}

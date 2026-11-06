package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.combat.EnemyActionResources;
import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalCombatData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalEnemyData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalEnemyPhaseData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalSkillData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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

@SuppressWarnings("removal")
public class EntityOriginalDatabaseEnemy extends EntityTurnBattleMonster {
    private static final double MAX_SUPPORTED_HEALTH = Integer.MAX_VALUE;
    private static final EntityDataAccessor<Integer> PROFILE_ID =
            SynchedEntityData.defineId(EntityOriginalDatabaseEnemy.class, EntityDataSerializers.INT);
    private double turnBattleHealth;
    private double turnBattleDifficultyMultiplier = 1.0D;
    private double turnBattleHealthRate = 1.0D;
    private double turnBattleManaRate = 1.0D;
    private final EnemyActionResources actionResources = new EnemyActionResources();

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

    public void transformTurnBattleProfile(int profileId) {
        if (getProfileId() != profileId) {
            this.entityData.set(PROFILE_ID, BSOriginalEnemyData.get(profileId).id());
        }
    }

    private void applyProfile(boolean resetHealth) {
        BSOriginalEnemyData.Entry profile = getProfile();
        double scaledMaxHealth = getScaledMaxHealth(profile);
        setAttributeBase(Attributes.MAX_HEALTH, scaledMaxHealth);
        setAttributeBase(Attributes.ATTACK_DAMAGE, Math.max(1.0D, Math.min(2048.0D, profile.attack())));
        setAttributeBase(Attributes.ARMOR, Math.max(0.0D, Math.min(30.0D, profile.defense())));
        setAttributeBase(Attributes.MOVEMENT_SPEED, profile.movementSpeed());
        setAttributeBase(Attributes.LUCK, Math.max(-1024.0D, Math.min(1024.0D, profile.luck())));
        if (resetHealth) {
            this.turnBattleHealth = scaledMaxHealth;
            this.actionResources.reset(getTurnBattleMaxMana());
        } else {
            this.turnBattleHealth = Math.max(0.0D,
                    Math.min(scaledMaxHealth, this.turnBattleHealth));
            this.actionResources.setMana(this.actionResources.mana(), getTurnBattleMaxMana());
        }
        syncVanillaHealth(false);
        refreshDimensions();
    }

    private double getScaledMaxHealth(BSOriginalEnemyData.Entry profile) {
        return Math.max(1.0D, Math.min(MAX_SUPPORTED_HEALTH,
                profile.health() * this.turnBattleDifficultyMultiplier * this.turnBattleHealthRate));
    }

    public void setTurnBattleParameterRates(double healthRate, double manaRate) {
        this.turnBattleHealthRate = Math.max(0.000001D, healthRate);
        this.turnBattleManaRate = Math.max(0.0D, manaRate);
        setAttributeBase(Attributes.MAX_HEALTH, getTurnBattleMaxHealth());
        setTurnBattleHealth(getTurnBattleHealth());
        setTurnBattleMana(getTurnBattleMana());
    }

    public void setTurnBattleParameterRatesPreservingResources(double healthRate, double manaRate) {
        this.turnBattleHealthRate = Math.max(0.000001D, healthRate);
        this.turnBattleManaRate = Math.max(0.0D, manaRate);
        setAttributeBase(Attributes.MAX_HEALTH, getTurnBattleMaxHealth());
        syncVanillaHealth(TurnBattleManager.isInBattle(this));
    }

    private void syncVanillaHealth(boolean keepAlive) {
        super.setHealth(this.turnBattleHealth <= 0.0D && !keepAlive ? 0.0F
                : (float) Math.max(1.0D, Math.min(getMaxHealth(), this.turnBattleHealth)));
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
        return this.actionResources.mana();
    }

    @Override
    public double getTurnBattleMaxMana() {
        return getProfile().mp() * this.turnBattleManaRate;
    }

    @Override
    public void setTurnBattleMana(double mana) {
        this.actionResources.setMana(mana, getTurnBattleMaxMana());
    }

    public double getTurnBattleTp() {
        return this.actionResources.tp();
    }

    public void setTurnBattleTp(double tp) {
        this.actionResources.setTp(tp);
    }

    public boolean canUseTurnBattleAction(BSOriginalEnemyData.Action action) {
        return canUseTurnBattleAction(action, getTurnBattleManaCostRate());
    }

    public boolean canUseTurnBattleAction(BSOriginalEnemyData.Action action, double manaCostRate) {
        return action != null && this.actionResources.canUse(BSOriginalSkillData.get(action.skillId()),
                manaCostRate, getTurnBattleHealth(), getTurnBattleMaxHealth());
    }

    public void useTurnBattleAction(BSOriginalEnemyData.Action action) {
        useTurnBattleAction(action, getTurnBattleManaCostRate());
    }

    public void useTurnBattleAction(BSOriginalEnemyData.Action action, double manaCostRate) {
        if (action == null) {
            return;
        }
        BSOriginalSkillData.Entry skill = BSOriginalSkillData.get(action.skillId());
        if (skill != null) {
            double healthCost = EnemyActionResources.healthCost(skill, getTurnBattleHealth(), getTurnBattleMaxHealth());
            this.actionResources.use(skill, manaCostRate);
            if (healthCost > 0.0D) {
                setTurnBattleHealth(getTurnBattleHealth() - healthCost);
            }
        }
    }

    public void advanceTurnBattleActionResources() {
        this.actionResources.advanceRound();
    }

    public void resetTurnBattleActionResources() {
        this.actionResources.clearCooldowns();
        this.actionResources.setTp(0.0D);
    }

    public void recoverTurnBattleResources() {
        setTurnBattleMana(getTurnBattleMaxMana());
    }

    public int getTurnBattleSkillCooldown(int skillId) {
        return this.actionResources.cooldown(skillId);
    }

    @Override
    public int rollTurnBattleActionCount(RandomSource random) {
        return EnemyActionResources.rollActionCount(random,
                BSOriginalCombatData.enemy(getProfileId()).features(), getProfile().actionCount());
    }

    private double getTurnBattleManaCostRate() {
        double rate = 1.0D;
        for (BSOriginalCombatData.Feature feature : BSOriginalCombatData.enemy(getProfileId()).features()) {
            if (feature.code() == 23 && feature.dataId() == 4) {
                rate *= feature.value();
            }
        }
        return rate;
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
        return selectTurnBattleAction(random, turn, activeStates, getTurnBattleManaCostRate());
    }

    public BSOriginalEnemyData.Action selectTurnBattleAction(RandomSource random, int turn,
                                                              Set<Integer> activeStates,
                                                              double manaCostRate) {
        return selectTurnBattleAction(random, turn, activeStates, manaCostRate, ignored -> false, ignored -> true);
    }

    public BSOriginalEnemyData.Action selectTurnBattleAction(RandomSource random, int turn,
                                                              Set<Integer> activeStates, double manaCostRate,
                                                              java.util.function.IntPredicate switches,
                                                              java.util.function.Predicate<BSOriginalEnemyData.Action> available) {
        List<BSOriginalEnemyData.Action> valid = getProfile().actions().stream()
                .filter(BSOriginalEnemyData.Action::selectable)
                .filter(action -> action.conditionType() == 6
                        ? switches.test((int) action.conditionParam1()) || isActionConditionMet(action, turn, activeStates)
                        : isActionConditionMet(action, turn, activeStates))
                .filter(available)
                .filter(action -> canUseTurnBattleAction(action, manaCostRate))
                .toList();
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
        return getScaledMaxHealth(getProfile());
    }

    public void setTurnBattleDifficultyMultiplier(double multiplier) {
        double oldMaxHealth = getTurnBattleMaxHealth();
        double healthRatio = oldMaxHealth <= 0.0D ? 1.0D
                : this.turnBattleHealth / oldMaxHealth;
        this.turnBattleDifficultyMultiplier = Math.max(1.0D, multiplier);
        double scaledMaxHealth = getTurnBattleMaxHealth();
        setAttributeBase(Attributes.MAX_HEALTH, scaledMaxHealth);
        this.turnBattleHealth = scaledMaxHealth
                * Math.max(0.0D, Math.min(1.0D, healthRatio));
        syncVanillaHealth(TurnBattleManager.isInBattle(this));
    }

    @Override
    public void setTurnBattleHealth(double health) {
        this.turnBattleHealth = Math.max(0.0D, Math.min(getTurnBattleMaxHealth(), health));
        syncVanillaHealth(TurnBattleManager.isInBattle(this));
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
            recoverTurnBattleResources();
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
        return rollTurnBattleDrops(random, 1);
    }

    @Override
    public List<ItemStack> rollTurnBattleDrops(RandomSource random, int rate) {
        List<ItemStack> result = new ArrayList<>();
        for (BSOriginalEnemyData.Drop drop : getProfile().drops()) {
            if (random.nextInt(Math.max(1, drop.denominator())) >= Math.max(1, rate)) {
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
        tag.putDouble("OriginalBattleDifficultyMultiplier", this.turnBattleDifficultyMultiplier);
        tag.put("OriginalBattleResources", this.actionResources.save());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setProfileId(tag.contains("OriginalEnemyId") ? tag.getInt("OriginalEnemyId") : 1);
        if (tag.contains("OriginalBattleDifficultyMultiplier")) {
            setTurnBattleDifficultyMultiplier(tag.getDouble("OriginalBattleDifficultyMultiplier"));
        }
        if (tag.contains("OriginalBattleHealth")) {
            setTurnBattleHealth(tag.getDouble("OriginalBattleHealth"));
        }
        if (tag.contains("OriginalBattleResources", Tag.TAG_COMPOUND)) {
            this.actionResources.load(tag.getCompound("OriginalBattleResources"), getTurnBattleMaxMana());
        }
    }
}

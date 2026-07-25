package com.BlackSouls.BlackSoulsMod.capability;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

public class BSPlayerStats {
    public static final Capability<BSPlayerStats> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private static final int MAX_LEVEL = 999;
    private static final long[] EXP_THRESHOLDS = createExpThresholds();

    public static final double HARD_CAP_HP = 1099998;
    public static final double HARD_CAP_MP = 109998;
    public static final double HARD_CAP_OTHER = 109998;

    public boolean hasVisitedLibrary = false;
    public boolean whiteBearIntroduced = false;
    public boolean whiteBearFreeSoulsClaimed = false;
    public int whiteBearProgress = 0;

    public double hp = 638, mp = 100, maxMp = 100;
    public double attack = 16, defense = 16, magicAttack = 10, magicDefense = 10, luck = 10, speed = 40;
    public double critRate = 5.0, bonusCritRate = 0.0, evasion = 0;
    public int level = 1;
    public long currentExp = 0, maxExp = 31;
    public double bonusHp = 0, bonusMp = 0, bonusAtk = 0, bonusDef = 0, bonusMatk = 0, bonusMdef = 0, bonusLuc = 0, bonusSpeed = 0;

    public double mpCostRate = 1.0;
    public double extraActionRate = 0.0;
    public double currentActionPoints = 1.0;

    public int sen = 100;

    public double burnRate = 0.0;
    public double hpRegenRate = 0.0;
    public double instantDeathRate = 0.0;
    public double stunRate = 0.0;
    public double mpRegenRate = 0.0;
    public double physicalDamageRate = 1.0;
    public double magicDamageRate = 1.0;
    public double poisonResistRate = 0.0;
    public double severePoisonResistRate = 0.0;
    public double bleedResistRate = 0.0;
    public double sleepResistRate = 0.0;
    public double fearRate = 0.0;
    public double fearResistRate = 0.0;
    public double targetingRate = 1.0;
    public List<String> weaponEnchantments = new ArrayList<>();

    public long souls = 0;
    public long lastTradeTime = 0;
    public long lostSouls = 0;
    public double lostX = 0, lostY = 0, lostZ = 0;
    public String lostDim = "";
    public long purgeRefreshCycle = -1L;
    public long purgeRefreshDay = -1L;
    public int purgeRefreshesUsedToday = 0;
    public int purgeMobKills = 0;
    public int purgeOreBreaks = 0;
    public int purgeTrashEarned = 0;
    public List<PurgeCommissionTask> purgeTasks = new ArrayList<>();

    public String activeCovenant = "", skillZ = "", skillX = "", skillC = "", skillV = "";
    public int nodenCovenantLevel = 0;
    public List<String> unlockedCovenants = new ArrayList<>();
    public List<String> unlockedSkills = new ArrayList<>();
    public List<BonfireEntry> unlockedBonfires = new ArrayList<>();

    public long vorpalLastTime = 0;
    public int vorpalComboStage = 0;

    public float getMana() {
        return (float) this.mp;
    }

    public float getMaxMana() {
        return (float) this.maxMp;
    }

    public double getCurrentActionPoints() {
        return this.currentActionPoints;
    }

    public double getMaxActionPoints() {
        return Math.max(1.0, 1.0 + this.extraActionRate);
    }

    public boolean consumeActionPoints(double amount) {
        if (amount <= 0.0) {
            return true;
        }
        if (this.currentActionPoints + 1.0E-6 >= amount) {
            this.currentActionPoints -= amount;
            if (this.currentActionPoints < 1.0E-4) {
                this.currentActionPoints = 0.0;
            }
            return true;
        }
        return false;
    }

    public void restoreActionPoints(double amount) {
        if (amount <= 0.0) {
            return;
        }
        this.currentActionPoints += amount;
    }

    public void restoreActionPoints(double amount, double maxActionPoints) {
        if (amount <= 0.0) {
            return;
        }
        this.currentActionPoints = Math.min(maxActionPoints, this.currentActionPoints + amount);
    }

    public void clampActionPoints() {
        this.currentActionPoints = Math.max(0.0, this.currentActionPoints);
    }

    public void clampActionPoints(double maxActionPoints) {
        this.currentActionPoints = Math.max(0.0, Math.min(this.currentActionPoints, maxActionPoints));
    }

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Level", level);
        nbt.putLong("Exp", currentExp);
        nbt.putDouble("MP", mp);
        nbt.putDouble("CurrentActionPoints", currentActionPoints);
        nbt.putDouble("BonusHP", bonusHp);
        nbt.putDouble("BonusMP", bonusMp);
        nbt.putDouble("BonusATK", bonusAtk);
        nbt.putDouble("BonusDEF", bonusDef);
        nbt.putDouble("BonusMATK", bonusMatk);
        nbt.putDouble("BonusMDEF", bonusMdef);
        nbt.putDouble("BonusLUC", bonusLuc);
        nbt.putDouble("BonusSPEED", bonusSpeed);
        nbt.putInt("Sen", sen);
        nbt.putBoolean("HasVisitedLibrary", this.hasVisitedLibrary);
        nbt.putBoolean("WhiteBearIntroduced", this.whiteBearIntroduced);
        nbt.putBoolean("WhiteBearFreeSoulsClaimed", this.whiteBearFreeSoulsClaimed);
        nbt.putInt("WhiteBearProgress", this.whiteBearProgress);
        nbt.putDouble("BurnRate", burnRate);
        nbt.putDouble("InstantDeathRate", instantDeathRate);
        nbt.putDouble("StunRate", stunRate);
        nbt.putDouble("MpRegenRate", mpRegenRate);
        nbt.putString("SkillZ", skillZ);
        nbt.putString("SkillX", skillX);
        nbt.putString("SkillC", skillC);
        nbt.putString("SkillV", skillV);
        nbt.putString("ActiveCovenant", activeCovenant);
        nbt.putInt("NodenCovenantLevel", nodenCovenantLevel);
        nbt.putLong("VorpalLastTime", vorpalLastTime);
        nbt.putInt("VorpalComboStage", vorpalComboStage);
        nbt.putLong("Souls", this.souls);
        nbt.putLong("LostSouls", this.lostSouls);
        nbt.putDouble("LostX", this.lostX);
        nbt.putDouble("LostY", this.lostY);
        nbt.putDouble("LostZ", this.lostZ);
        nbt.putString("LostDim", this.lostDim);
        nbt.putLong("PurgeRefreshCycle", this.purgeRefreshCycle);
        nbt.putLong("PurgeRefreshDay", this.purgeRefreshDay);
        nbt.putInt("PurgeRefreshesUsedToday", this.purgeRefreshesUsedToday);
        nbt.putInt("PurgeMobKills", this.purgeMobKills);
        nbt.putInt("PurgeOreBreaks", this.purgeOreBreaks);
        nbt.putInt("PurgeTrashEarned", this.purgeTrashEarned);

        ListTag purgeTaskList = new ListTag();
        for (PurgeCommissionTask task : this.purgeTasks) {
            purgeTaskList.add(task.save());
        }
        nbt.put("PurgeTasks", purgeTaskList);

        ListTag listBonfires = new ListTag();
        for (BonfireEntry entry : unlockedBonfires) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dim", entry.pos.dimension().location().toString());
            tag.putInt("X", entry.pos.pos().getX());
            tag.putInt("Y", entry.pos.pos().getY());
            tag.putInt("Z", entry.pos.pos().getZ());
            tag.putString("Name", entry.name);
            tag.putString("Desc", entry.description);
            listBonfires.add(tag);
        }
        nbt.put("UnlockedBonfires", listBonfires);

        ListTag listCov = new ListTag();
        for (String s : unlockedCovenants) {
            listCov.add(StringTag.valueOf(s));
        }
        nbt.put("UnlockedCovenants", listCov);

        ListTag listSkill = new ListTag();
        for (String s : unlockedSkills) {
            listSkill.add(StringTag.valueOf(s));
        }
        nbt.put("UnlockedSkills", listSkill);

        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        this.level = nbt.getInt("Level");
        this.currentExp = nbt.getLong("Exp");
        this.mp = nbt.getDouble("MP");
        this.currentActionPoints = nbt.contains("CurrentActionPoints") ? nbt.getDouble("CurrentActionPoints") : 1.0;
        this.bonusHp = nbt.getDouble("BonusHP");
        this.bonusMp = nbt.getDouble("BonusMP");
        this.bonusAtk = nbt.getDouble("BonusATK");
        this.bonusDef = nbt.getDouble("BonusDEF");
        this.bonusMatk = nbt.getDouble("BonusMATK");
        this.bonusMdef = nbt.getDouble("BonusMDEF");
        this.bonusLuc = nbt.getDouble("BonusLUC");
        this.bonusSpeed = nbt.getDouble("BonusSPEED");
        this.sen = nbt.getInt("Sen");
        this.hasVisitedLibrary = nbt.getBoolean("HasVisitedLibrary");
        this.whiteBearIntroduced = nbt.getBoolean("WhiteBearIntroduced");
        this.whiteBearFreeSoulsClaimed = nbt.getBoolean("WhiteBearFreeSoulsClaimed");
        this.whiteBearProgress = Math.max(0, Math.min(12, nbt.getInt("WhiteBearProgress")));
        this.burnRate = nbt.getDouble("BurnRate");
        this.instantDeathRate = nbt.getDouble("InstantDeathRate");
        this.stunRate = nbt.getDouble("StunRate");
        this.mpRegenRate = nbt.getDouble("MpRegenRate");
        this.skillZ = nbt.getString("SkillZ");
        this.skillX = nbt.getString("SkillX");
        this.skillC = nbt.getString("SkillC");
        this.skillV = nbt.getString("SkillV");
        this.activeCovenant = nbt.getString("ActiveCovenant");
        this.nodenCovenantLevel = nbt.getInt("NodenCovenantLevel");
        this.souls = nbt.getLong("Souls");
        this.lostSouls = nbt.getLong("LostSouls");
        this.lostX = nbt.getDouble("LostX");
        this.lostY = nbt.getDouble("LostY");
        this.lostZ = nbt.getDouble("LostZ");
        this.lostDim = nbt.getString("LostDim");
        this.purgeRefreshCycle = nbt.contains("PurgeRefreshCycle") ? nbt.getLong("PurgeRefreshCycle") : -1L;
        this.purgeRefreshDay = nbt.contains("PurgeRefreshDay") ? nbt.getLong("PurgeRefreshDay") : -1L;
        this.purgeRefreshesUsedToday = nbt.getInt("PurgeRefreshesUsedToday");
        this.purgeMobKills = nbt.getInt("PurgeMobKills");
        this.purgeOreBreaks = nbt.getInt("PurgeOreBreaks");
        this.purgeTrashEarned = nbt.getInt("PurgeTrashEarned");
        this.purgeTasks.clear();
        if (nbt.contains("PurgeTasks", Tag.TAG_LIST)) {
            ListTag purgeTaskList = nbt.getList("PurgeTasks", Tag.TAG_COMPOUND);
            for (int i = 0; i < purgeTaskList.size(); i++) {
                this.purgeTasks.add(PurgeCommissionTask.load(purgeTaskList.getCompound(i)));
            }
        }
        this.vorpalLastTime = nbt.getLong("VorpalLastTime");
        this.vorpalComboStage = nbt.getInt("VorpalComboStage");

        this.unlockedBonfires.clear();
        if (nbt.contains("UnlockedBonfires")) {
            ListTag listBonfires = nbt.getList("UnlockedBonfires", Tag.TAG_COMPOUND);
            for (int i = 0; i < listBonfires.size(); i++) {
                CompoundTag tag = listBonfires.getCompound(i);
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim =
                        net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DIMENSION,
                                new net.minecraft.resources.ResourceLocation(tag.getString("Dim"))
                        );
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
                this.unlockedBonfires.add(new BonfireEntry(
                        net.minecraft.core.GlobalPos.of(dim, pos),
                        tag.getString("Name"),
                        tag.getString("Desc")
                ));
            }
        }

        this.unlockedCovenants.clear();
        ListTag listCov = nbt.getList("UnlockedCovenants", Tag.TAG_STRING);
        for (int i = 0; i < listCov.size(); i++) {
            this.unlockedCovenants.add(listCov.getString(i));
        }

        this.unlockedSkills.clear();
        ListTag listSkill = nbt.getList("UnlockedSkills", Tag.TAG_STRING);
        for (int i = 0; i < listSkill.size(); i++) {
            this.unlockedSkills.add(listSkill.getString(i));
        }

        recalculateStats();
        clampActionPoints();
    }

    public void addPermanentStat(String type, double amount) {
        switch (type) {
            case "HP": bonusHp += amount; break;
            case "MP": bonusMp += amount; break;
            case "ATK": bonusAtk += amount; break;
            case "DEF": bonusDef += amount; break;
            case "MATK": bonusMatk += amount; break;
            case "MDEF": bonusMdef += amount; break;
            case "LUC": bonusLuc += amount; break;
            case "SPEED": bonusSpeed += amount; break;
            default: break;
        }
        recalculateStats();
    }

    public void addExp(long amount) {
        if (this.level >= MAX_LEVEL) {
            return;
        }
        this.currentExp += amount;
        while (this.level < MAX_LEVEL && this.currentExp >= getExpToReachLevel(this.level + 1)) {
            this.level++;
        }
        recalculateStats();
    }

    public boolean consumeMP(float amount) {
        if (this.mp >= amount) {
            this.mp -= amount;
            return true;
        }
        return false;
    }

    public void restoreMP(double amount) {
        this.mp = Math.min(this.maxMp, this.mp + amount);
    }

    public void recalculateStats() {
        int l = Math.min(MAX_LEVEL, this.level);

        double baseHp = getRMStat(l, 638, 6680, 61);
        double baseMp = getRMStat(l, 100, 205, 1);
        double baseAtk = getRMStat(l, 16, 180, 1);
        double baseDef = getRMStat(l, 16, 122, 1);
        double baseMatk = getRMStat(l, 10, 160, 1);
        double baseMdef = getRMStat(l, 10, 112, 1);
        double baseLuc = getRMStat(l, 10, 248, 2);
        double baseSpeed = getRMStat(l, 40, 318, 2);

        double finalHp = baseHp + this.bonusHp;

        if ("noden".equals(this.activeCovenant)) {
            double hpMultiplier = 1.10;
            if (this.nodenCovenantLevel == 1) {
                hpMultiplier = 1.25;
            } else if (this.nodenCovenantLevel == 2) {
                hpMultiplier = 1.35;
            } else if (this.nodenCovenantLevel >= 3) {
                hpMultiplier = 1.50;
            }
            finalHp *= hpMultiplier;
        }

        this.hp = Math.min(HARD_CAP_HP, finalHp);
        this.maxMp = Math.min(HARD_CAP_MP, baseMp + this.bonusMp);
        this.attack = Math.min(HARD_CAP_OTHER, baseAtk + this.bonusAtk);
        this.defense = Math.min(HARD_CAP_OTHER, baseDef + this.bonusDef);
        this.magicAttack = Math.min(HARD_CAP_OTHER, baseMatk + this.bonusMatk);
        this.magicDefense = Math.min(HARD_CAP_OTHER, baseMdef + this.bonusMdef);
        this.luck = Math.min(HARD_CAP_OTHER, baseLuc + this.bonusLuc);
        this.speed = Math.min(HARD_CAP_OTHER, baseSpeed + this.bonusSpeed);

        this.critRate = Math.min(100.0, 5.0 + this.bonusCritRate);
        this.evasion = Math.min(100.0, 0.0 + this.evasion);
        this.maxExp = getExpToReachLevel(this.level + 1);
    }

    private double getRMStat(int level, double val1, double val99, double delta99) {
        if (level <= 99) {
            return Math.round(val1 + (val99 - val1) * (level - 1) / 98.0);
        }
        double limitBreakBonus = Math.floor(delta99 * (level - 99) * 1.05);
        return val99 + limitBreakBonus;
    }

    public static class PurgeCommissionTask {
        public String category;
        public String targetId;
        public int required;
        public int progress;
        public String rewardItemId;
        public int rewardCount;
        public boolean rewarded;

        public PurgeCommissionTask(String category, String targetId, int required, int progress, String rewardItemId, int rewardCount, boolean rewarded) {
            this.category = category;
            this.targetId = targetId;
            this.required = required;
            this.progress = progress;
            this.rewardItemId = rewardItemId;
            this.rewardCount = rewardCount;
            this.rewarded = rewarded;
        }

        public boolean isComplete() {
            return this.progress >= this.required;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Category", this.category);
            tag.putString("TargetId", this.targetId);
            tag.putInt("Required", this.required);
            tag.putInt("Progress", this.progress);
            tag.putString("RewardItemId", this.rewardItemId);
            tag.putInt("RewardCount", this.rewardCount);
            tag.putBoolean("Rewarded", this.rewarded);
            return tag;
        }

        public static PurgeCommissionTask load(CompoundTag tag) {
            return new PurgeCommissionTask(
                    tag.getString("Category"),
                    tag.getString("TargetId"),
                    tag.getInt("Required"),
                    tag.getInt("Progress"),
                    tag.getString("RewardItemId"),
                    tag.getInt("RewardCount"),
                    tag.getBoolean("Rewarded")
            );
        }
    }

    public long getExpToReachLevel(int targetLevel) {
        if (targetLevel <= 1) {
            return 0;
        }

        if (targetLevel < EXP_THRESHOLDS.length) {
            return EXP_THRESHOLDS[targetLevel];
        }

        return calculateExpThreshold(targetLevel);
    }

    private static long[] createExpThresholds() {
        long[] thresholds = new long[MAX_LEVEL + 2];
        for (int level = 2; level < thresholds.length; level++) {
            thresholds[level] = calculateExpThreshold(level);
        }
        return thresholds;
    }

    private static long calculateExpThreshold(int targetLevel) {
        double basis = 30.0;
        double extra = 0.0;
        double accA = 10.0;
        double accB = 10.0;
        double lv = targetLevel;
        double val = basis * Math.pow(lv - 1, 0.9 + accA / 250.0) * lv * (lv + 1) /
                (6.0 + Math.pow(lv, 2) / 50.0 / accB) + (lv - 1) * extra;

        return Math.round(val);
    }

    public static class Provider implements net.minecraftforge.common.capabilities.ICapabilitySerializable<CompoundTag> {
        private final BSPlayerStats instance = new BSPlayerStats();
        private final LazyOptional<BSPlayerStats> optional = LazyOptional.of(() -> instance);

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return instance.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            instance.deserializeNBT(nbt);
        }
    }
}

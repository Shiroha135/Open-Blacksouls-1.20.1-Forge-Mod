package yuki135.blacksouls.combat;

import yuki135.blacksouls.util.BSOriginalCombatData;
import yuki135.blacksouls.util.BSOriginalSkillData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EnemyActionResources {
    private static final Pattern MAX_HP_COST = Pattern.compile("<消費最大(?:ＨＰ|HP)\\s*(\\d+)%\\s*>");
    private static final Pattern HP_RATE_COST = Pattern.compile("<消費(?:ＨＰ|HP)\\s*(\\d+)%\\s*>");
    private static final Pattern HP_COST = Pattern.compile("<消費(?:ＨＰ|HP)\\s*(\\d+)\\s*>");
    private final Map<Integer, Integer> cooldowns = new HashMap<>();
    private double mana;
    private double tp;

    public double mana() {
        return mana;
    }

    public double tp() {
        return tp;
    }

    public void setMana(double value, double maximum) {
        mana = finiteClamp(value, maximum);
    }

    public void setTp(double value) {
        tp = finiteClamp(value, 100.0D);
    }

    public void reset(double maximumMana) {
        setMana(maximumMana, maximumMana);
        tp = 0.0D;
        cooldowns.clear();
    }

    public boolean canUse(BSOriginalSkillData.Entry skill, double manaCostRate,
                          double health, double maxHealth) {
        return skill != null && cooldown(skill.id()) <= 0
                && mana >= manaCost(skill, manaCostRate)
                && tp >= Math.max(0, skill.tpCost())
                && health > healthCost(skill, health, maxHealth);
    }

    public void use(BSOriginalSkillData.Entry skill, double manaCostRate) {
        if (skill == null) {
            return;
        }
        mana = Math.max(0.0D, mana - manaCost(skill, manaCostRate));
        tp = Math.max(0.0D, tp - Math.max(0, skill.tpCost()));
        if (skill.cooldown() > 0) {
            cooldowns.put(skill.id(), (int) Math.min(Integer.MAX_VALUE, (long) skill.cooldown() + 1L));
        }
    }

    public int cooldown(int skillId) {
        return cooldowns.getOrDefault(skillId, 0);
    }

    public void advanceRound() {
        cooldowns.replaceAll((skillId, remaining) -> remaining - 1);
        cooldowns.values().removeIf(remaining -> remaining <= 0);
    }

    public void clearCooldowns() {
        cooldowns.clear();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Mana", mana);
        tag.putDouble("Tp", tp);
        CompoundTag savedCooldowns = new CompoundTag();
        cooldowns.forEach((skillId, remaining) -> savedCooldowns.putInt(Integer.toString(skillId), remaining));
        tag.put("Cooldowns", savedCooldowns);
        return tag;
    }

    public void load(CompoundTag tag, double maximumMana) {
        reset(maximumMana);
        if (tag.contains("Mana", Tag.TAG_ANY_NUMERIC)) {
            setMana(tag.getDouble("Mana"), maximumMana);
        }
        if (tag.contains("Tp", Tag.TAG_ANY_NUMERIC)) {
            setTp(tag.getDouble("Tp"));
        }
        CompoundTag savedCooldowns = tag.getCompound("Cooldowns");
        for (String key : savedCooldowns.getAllKeys()) {
            try {
                int skillId = Integer.parseInt(key);
                int remaining = savedCooldowns.getInt(key);
                if (skillId > 0 && remaining > 0) {
                    cooldowns.put(skillId, remaining);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public static int rollActionCount(RandomSource random, List<BSOriginalCombatData.Feature> features,
                                      int minimumActions) {
        int actions = 1;
        for (BSOriginalCombatData.Feature feature : features) {
            if (feature.code() == 61 && random.nextDouble() < feature.value()) {
                actions++;
            }
        }
        return Math.max(Math.max(1, minimumActions), actions);
    }

    public static double healthCost(BSOriginalSkillData.Entry skill, double health, double maxHealth) {
        int maxRate = noteCost(MAX_HP_COST, skill.note());
        if (maxRate > 0) {
            return Math.max(1.0D, Math.floor(Math.max(0.0D, maxHealth) * maxRate / 100.0D));
        }
        int currentRate = noteCost(HP_RATE_COST, skill.note());
        if (currentRate > 0) {
            return Math.max(1.0D, Math.floor(Math.max(0.0D, health) * currentRate / 100.0D));
        }
        return noteCost(HP_COST, skill.note());
    }

    private static double manaCost(BSOriginalSkillData.Entry skill, double rate) {
        double safeRate = Double.isFinite(rate) ? Math.max(0.0D, rate) : 1.0D;
        return Math.floor(Math.max(0, skill.mpCost()) * safeRate);
    }

    private static int noteCost(Pattern pattern, String note) {
        Matcher matcher = pattern.matcher(note);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static double finiteClamp(double value, double maximum) {
        double limit = Double.isFinite(maximum) ? Math.max(0.0D, maximum) : 0.0D;
        return Double.isFinite(value) ? Math.max(0.0D, Math.min(limit, value)) : 0.0D;
    }
}

package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.entity.EntityMeatWall;
import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import com.BlackSouls.BlackSoulsMod.util.BSAttributeManager;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SkillOriginalMagic extends AbstractSkill {
    public enum Profile {
        SOUL_VOLLEY("soul_volley", 28, 400, 295, 2, true, "soul_arrow.png"),
        DISPEL("dispel", 5, 200, 46, 1, true, "reinforce.png"),
        SEE_THROUGH_ATTACK("see_through_attack", 2, 200, -1, 1, true, "weapon.png"),
        POISON("poison", 4, 200, 299, 1, true, "reinforce.png"),
        POISON_II("poison_ii", 9, 200, 300, 1, true, "reinforce.png"),
        HYPNOSIS("hypnosis", 6, 200, 54, 1, true, "invisible.png"),
        CURE("cure", 3, 200, 40, 1, false, "soul_arrow.png"),
        MAGIC_BLESSING("magic_blessing", 9, 200, 43, 1, false, "reinforce.png"),
        RAMPAGE("rampage", 15, 200, 5, 1, true, "weapon.png"),
        FULL_BLESSING("full_blessing", 30, 200, 44, 1, false, "reinforce.png"),
        RESURRECTION("resurrection", 15, 200, 42, 1, false, "requiem.png"),
        MANA_ABSORPTION("mana_absorption", 10, 200, 49, 1, true, "reinforce.png"),
        ERASE("erase", 5, 200, 41, 1, false, "soul_arrow.png"),
        KINGS_COMMAND("kings_command", 12, 200, 43, 2, false, "knights_glory.png"),
        FIRE("fire", 6, 200, 58, 1, true, "hellfire_blade.png"),
        DROWNING_BUBBLE("drowning_bubble", 5, 200, 69, 1, true, "reinforce.png"),
        DARK_SIDE_OF_MOON("dark_side_of_moon", 20, 300, 49, 1, true, "moonlight.png"),
        FREEZING_MAGIC_BULLET("freezing_magic_bullet", 20, 300, 61, 2, true, "soul_arrow.png"),
        HELLFIRE("hellfire", 30, 400, 98, 2, true, "hellfire_blade.png"),
        DESTRUCTION_STORM("destruction_storm", 100, 800, 103, 2, true, "storm_ruler.png"),
        INNER_POTENTIAL("inner_potential", 5, 200, 466, 2, false, "smoldering_frenzy.png"),
        GREAT_SOUL_ARROW("great_soul_arrow", 50, 700, 124, 2, true, "soul_arrow.png"),
        VERDANT_POWER("verdant_power", 30, 2000, 38, 2, false, "soul_arrow.png"),
        ROCK_BODY("rock_body", 15, 600, 288, 2, false, "shield_slam.png"),
        DARK_ORB("dark_orb", 6, 200, 253, 2, true, "darkness.png"),
        DARK_DANCE("dark_dance", 28, 400, 296, 2, true, "darkness.png"),
        DARK_SWARM("dark_swarm", 12, 200, 265, 2, true, "darkness.png"),
        DIVINE_THUNDER("divine_thunder", 20, 400, 65, 2, true, "heaven_shattering_thunder.png"),
        DIVINE_BEAST_THUNDER("divine_beast_thunder", 20, 800, 100, 2, true, "heaven_shattering_thunder.png"),
        METEOR_SWARM("meteor_swarm", 200, 2000, 106, 2, true, "soul_collapse.png"),
        FULL_CURSE("full_curse", 20, 200, 46, 1, true, "darkness.png"),
        GREAT_SOUL_ARROW_VOLLEY("great_soul_arrow_volley", 200, 2000, 124, 2, true, "soul_arrow.png"),
        FATAL_GUARD("fatal_guard", 10, 200, 81, 1, false, "shield_slam.png"),
        GHOST_FIRE("ghost_fire", 15, 500, 266, 2, true, "hellfire_blade.png"),
        PHALANX("phalanx", 13, 200, 162, 2, false, "shield_slam.png"),
        ABSOLUTE_HIT("absolute_hit", 0, 2000, 33, 2, false, "aim.png"),
        CHAOS_EXPLOSION("chaos_explosion", 50, 800, 394, 2, true, "hellfire_blade.png"),
        CRITICAL_STRIKE("critical_strike", 10, 200, -1, 1, true, "weapon.png"),
        SOUL_SHIELD("soul_shield", 25, 800, 396, 2, false, "shield_slam.png"),
        DENSE_SPIROCHETE("dense_spirochete", 20, 200, 499, 1, true, "reinforce.png"),
        SUMMON_MEAT_WALL("summon_meat_wall", 0, 300, 118, 1, false, "shield_slam.png"),
        TORN_GRUDGE("torn_grudge", 1, 800, 510, 2, true, "twilight_of_grudge.png"),
        PIERCING_ICICLE("piercing_icicle", 54, 1000, 511, 2, true, "soul_arrow.png"),
        RAIN_OF_RUIN("rain_of_ruin", 25, 600, 512, 2, true, "arrow_rain.png"),
        GLOOMY_SWAMP("gloomy_swamp", 26, 300, 513, 2, true, "darkness.png"),
        ACID_RAIN("acid_rain", 18, 400, 514, 2, true, "arrow_rain.png"),
        ROYAL_TEA("royal_tea", 50, 500, 44, 2, false, "reinforce.png"),
        GODSPEED_DANCE("godspeed_dance", 20, 300, 354, 2, false, "dodo_run.png"),
        KATARINA_WHEEL("katarina_wheel", 30, 600, 126, 2, true, "weapon.png"),
        PALADIN_BANNER("paladin_banner", 50, 2000, 95, 2, false, "knights_glory.png"),
        BLACK_WAVE("black_wave", 20, 5000, 173, 2, true, "darkness.png"),
        BLACK_SLASH("black_slash", 25, 200, 192, 1, true, "weapon.png"),
        AWAKENING("awakening", 10, 2000, 81, 2, false, "smoldering_frenzy.png"),
        SERPENT_EMBRACE("serpent_embrace", 44, 800, 531, 2, true, "darkness.png"),
        SOUL_STREAM("soul_stream", 120, 1500, 534, 2, true, "soul_arrow.png");

        private final String path;
        private final float manaCost;
        private final int cooldownTicks;
        private final int animationId;
        private final int loreLines;
        private final boolean offensive;
        private final String icon;

        Profile(String path, float manaCost, int cooldownTicks, int animationId, int loreLines, boolean offensive, String icon) {
            this.path = path;
            this.manaCost = manaCost;
            this.cooldownTicks = cooldownTicks;
            this.animationId = animationId;
            this.loreLines = loreLines;
            this.offensive = offensive;
            this.icon = icon;
        }

        public String getSkillId() {
            return "bs2_skill_" + path;
        }

        public String getBookId() {
            return "skill_book_" + path;
        }

        public String getTranslationKey() {
            return "skill.blacksouls." + getSkillId() + ".name";
        }

        public int getLoreLines() {
            return loreLines;
        }
    }

    private enum Element {
        NONE, FIRE, ICE, THUNDER, LIGHT, DARK
    }

    private final Profile profile;

    public SkillOriginalMagic(Profile profile) {
        this.profile = profile;
    }

    @Override
    public String getSkillId() {
        return profile.getSkillId();
    }

    @Override
    public float getManaCost() {
        return profile.manaCost;
    }

    @Override
    public int getBaseCooldownTicks() {
        return profile.cooldownTicks;
    }

    @Override
    public String getTranslationKey() {
        return profile.getTranslationKey();
    }

    @Override
    public ChatFormatting getTextColor() {
        return ChatFormatting.LIGHT_PURPLE;
    }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/original/" + profile.path + ".png");
    }

    @Override
    public boolean isUnlockedForGUI(Player player) {
        return SkillUtils.hasLearnedSkill(player, getSkillId());
    }

    @Override
    public boolean canCast(ServerPlayer player, BSPlayerStats stats) {
        if (!super.canCast(player, stats)) {
            return false;
        }
        if (profile.offensive && getEnemies(player, 14.0D).isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.blacksouls.skill.no_target").withStyle(ChatFormatting.GRAY));
            return false;
        }
        return true;
    }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        player.sendSystemMessage(Component.translatable(
                "message.blacksouls.skill.original_magic.use",
                player.getName().getString(),
                Component.translatable(getTranslationKey())
        ).withStyle(ChatFormatting.WHITE));

        switch (profile) {
            case DISPEL -> castDispel(player);
            case POISON -> castStatusOnEnemies(player, BlackSouls.BUFF_POISON.get(), 1200, 0.50D);
            case POISON_II -> castStatusOnEnemies(player, BlackSouls.BUFF_SEVERE_POISON.get(), 1200, 0.50D);
            case HYPNOSIS -> castStatusOnEnemies(player, BlackSouls.BUFF_SLEEP.get(), 300, 0.50D);
            case CURE -> castCure(player, false);
            case MAGIC_BLESSING -> castMagicBlessing(player);
            case FULL_BLESSING -> castFullBlessing(player);
            case RESURRECTION -> castResurrection(player);
            case MANA_ABSORPTION -> castManaAbsorption(player, stats);
            case ERASE -> castCure(player, true);
            case KINGS_COMMAND -> castKingsCommand(player);
            case INNER_POTENTIAL -> castSimpleBuff(player, BlackSouls.BUFF_INNER_POTENTIAL.get(), 600);
            case VERDANT_POWER -> castVerdantPower(player);
            case ROCK_BODY -> castRockBody(player);
            case FATAL_GUARD -> castAlliedBuff(player, BlackSouls.BUFF_DAGGER_GUARD.get(), 600);
            case PHALANX -> castPhalanx(player);
            case ABSOLUTE_HIT -> castSimpleBuff(player, BlackSouls.BUFF_AIM.get(), 600);
            case SOUL_SHIELD -> castSoulShield(player);
            case SUMMON_MEAT_WALL -> castMeatWall(player);
            case FULL_CURSE -> castFullCurse(player);
            case ROYAL_TEA -> castRoyalTea(player);
            case GODSPEED_DANCE -> castAlliedBuff(player, BlackSouls.BUFF_DAGGER_EVASION.get(), 600);
            case PALADIN_BANNER -> castPaladinBanner(player);
            case AWAKENING -> castSimpleBuff(player, BlackSouls.BUFF_AWAKENING.get(), 600);
            default -> castAttack(player, stats);
        }
    }

    private void castAttack(ServerPlayer player, BSPlayerStats stats) {
        List<LivingEntity> enemies = getEnemies(player, 14.0D);
        if (enemies.isEmpty()) {
            return;
        }
        switch (profile) {
            case SOUL_VOLLEY, PIERCING_ICICLE, BLACK_SLASH -> scheduleSingle(player, stats, selectSingle(player, enemies), 4);
            case DENSE_SPIROCHETE -> scheduleSingle(player, stats, selectSingle(player, enemies), 3);
            case DIVINE_BEAST_THUNDER, SOUL_STREAM -> scheduleAll(player, stats, enemies, profile == Profile.DIVINE_BEAST_THUNDER ? 3 : 4);
            case RAIN_OF_RUIN -> scheduleAll(player, stats, enemies, 2);
            case DARK_DANCE, GREAT_SOUL_ARROW_VOLLEY -> scheduleRandom(player, stats, enemies, 4);
            case DESTRUCTION_STORM -> scheduleRandom(player, stats, enemies, 6);
            case KATARINA_WHEEL -> scheduleRandom(player, stats, enemies, 8);
            case RAMPAGE, FREEZING_MAGIC_BULLET -> {
                List<LivingEntity> limited = enemies.subList(0, Math.min(3, enemies.size()));
                scheduleAll(player, stats, limited, 1);
            }
            case HELLFIRE, DARK_SWARM, METEOR_SWARM, GHOST_FIRE, CHAOS_EXPLOSION,
                    ACID_RAIN, BLACK_WAVE -> scheduleAll(player, stats, enemies, 1);
            default -> scheduleSingle(player, stats, selectSingle(player, enemies), 1);
        }
    }

    private void scheduleSingle(ServerPlayer player, BSPlayerStats stats, LivingEntity target, int hits) {
        for (int hit = 0; hit < hits; hit++) {
            int delay = hit * 3;
            player.server.tell(new TickTask(delay, () -> hitTarget(player, stats, target)));
        }
    }

    private void scheduleAll(ServerPlayer player, BSPlayerStats stats, List<LivingEntity> targets, int hits) {
        for (int hit = 0; hit < hits; hit++) {
            int delay = hit * 3;
            player.server.tell(new TickTask(delay, () -> {
                for (LivingEntity target : targets) {
                    hitTarget(player, stats, target);
                }
            }));
        }
    }

    private void scheduleRandom(ServerPlayer player, BSPlayerStats stats, List<LivingEntity> targets, int hits) {
        for (int hit = 0; hit < hits; hit++) {
            int delay = hit * 3;
            player.server.tell(new TickTask(delay, () -> {
                List<LivingEntity> living = targets.stream().filter(LivingEntity::isAlive).toList();
                if (!living.isEmpty()) {
                    hitTarget(player, stats, living.get(player.getRandom().nextInt(living.size())));
                }
            }));
        }
    }

    private void hitTarget(ServerPlayer player, BSPlayerStats stats, LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return;
        }
        double raw = calculateRawDamage(stats, target);
        Element element = getElement();
        if (element != Element.NONE) {
            raw *= BSAttributeManager.getBestMultiplier(target, List.of(getAttribute(element)));
        }
        double variance = switch (profile) {
            case DESTRUCTION_STORM, METEOR_SWARM -> 0.40D;
            case DENSE_SPIROCHETE, GLOOMY_SWAMP, ACID_RAIN, BLACK_WAVE -> 0.0D;
            default -> 0.20D;
        };
        if (variance > 0.0D) {
            raw *= 1.0D - variance + player.getRandom().nextDouble() * variance * 2.0D;
        }
        float damage = (float) Math.max(1.0D, raw);
        if (profile == Profile.CRITICAL_STRIKE) {
            damage *= 3.0F;
        } else if (canCritical()) {
            damage = StatEventHandler.rollSkillCrit(player, damage);
        }

        float drain = isDrainSkill() ? Math.min(damage, target.getHealth()) : 0.0F;
        boolean damaged = StatEventHandler.hurtWithSkillDamage(player, target, damage, isSureHit(), 0.0D);
        if (damaged) {
            if (drain > 0.0F) {
                player.heal(drain);
            }
            applyOnHitEffects(target);
        }
        playAnimation(target);
        playImpactSound(target, element);
    }

    private double calculateRawDamage(BSPlayerStats stats, LivingEntity target) {
        double matk = stats.magicAttack;
        double atk = stats.attack;
        double mdef = StatEventHandler.getRpgMagicDefense(target);
        double def = StatEventHandler.getRpgPhysicalDefense(target);
        return switch (profile) {
            case SOUL_VOLLEY, DARK_ORB, DARK_DANCE, SOUL_STREAM -> matk * 5.0D - mdef * 2.0D;
            case SEE_THROUGH_ATTACK, RAMPAGE, CRITICAL_STRIKE -> atk * 4.0D - def * 2.0D;
            case FIRE, DROWNING_BUBBLE, DIVINE_THUNDER, DIVINE_BEAST_THUNDER -> 300.0D + matk * 4.0D - mdef * 2.0D;
            case DARK_SIDE_OF_MOON, HELLFIRE -> 500.0D + matk * 4.0D - mdef * 2.0D;
            case FREEZING_MAGIC_BULLET -> 200.0D + matk * 4.0D - mdef * 2.0D;
            case DESTRUCTION_STORM -> 300.0D + matk * 2.0D;
            case GREAT_SOUL_ARROW, GREAT_SOUL_ARROW_VOLLEY -> matk * 8.0D - mdef * 2.0D;
            case DARK_SWARM -> matk * 6.0D - mdef * 2.0D;
            case METEOR_SWARM -> 3000.0D + matk * 10.0D;
            case GHOST_FIRE -> matk * 4.0D - mdef * 2.0D + target.getMaxHealth() * 0.05D;
            case CHAOS_EXPLOSION -> 1000.0D + target.getHealth() * 0.25D + matk * 4.0D - mdef * 2.0D;
            case DENSE_SPIROCHETE -> 1000.0D;
            case TORN_GRUDGE -> (matk * 4.0D - mdef * 2.0D) * Math.min(stats.maxMp / Math.max(1.0D, stats.mp), 10.0D);
            case PIERCING_ICICLE -> matk * 3.0D;
            case RAIN_OF_RUIN -> (matk * 4.0D - mdef * 2.0D) * Math.min(target.getMaxHealth() / Math.max(1.0F, target.getHealth()), 3.0D);
            case GLOOMY_SWAMP -> target.getMaxHealth() * 0.03D;
            case ACID_RAIN -> target.getMaxHealth() * 0.02D;
            case KATARINA_WHEEL -> matk * 5.0D - mdef * 2.0D;
            case BLACK_WAVE -> target.getMaxHealth() * 0.15D;
            case BLACK_SLASH -> 1000.0D + atk * 6.0D - def * 2.0D;
            case SERPENT_EMBRACE -> {
                double value = matk * 4.0D - mdef * 2.0D;
                if (target.hasEffect(BlackSouls.BUFF_POISON.get()) || target.hasEffect(BlackSouls.BUFF_SEVERE_POISON.get())) {
                    value *= 1.5D;
                }
                yield value;
            }
            default -> 1.0D;
        };
    }

    private boolean canCritical() {
        return switch (profile) {
            case DENSE_SPIROCHETE, GLOOMY_SWAMP, ACID_RAIN, BLACK_WAVE, BLACK_SLASH -> false;
            default -> true;
        };
    }

    private boolean isSureHit() {
        return switch (profile) {
            case RAMPAGE, CRITICAL_STRIKE -> false;
            default -> true;
        };
    }

    private boolean isDrainSkill() {
        return profile == Profile.DARK_SIDE_OF_MOON || profile == Profile.KATARINA_WHEEL;
    }

    private Element getElement() {
        return switch (profile) {
            case SOUL_VOLLEY, GREAT_SOUL_ARROW, GREAT_SOUL_ARROW_VOLLEY, SOUL_STREAM -> Element.LIGHT;
            case FIRE, HELLFIRE, GHOST_FIRE, CHAOS_EXPLOSION -> Element.FIRE;
            case FREEZING_MAGIC_BULLET, PIERCING_ICICLE -> Element.ICE;
            case DIVINE_THUNDER, DIVINE_BEAST_THUNDER -> Element.THUNDER;
            case DARK_ORB, DARK_DANCE, DARK_SWARM, RAIN_OF_RUIN, BLACK_WAVE -> Element.DARK;
            default -> Element.NONE;
        };
    }

    private String getAttribute(Element element) {
        return switch (element) {
            case FIRE -> BSAttributeManager.FIRE;
            case ICE -> BSAttributeManager.ICE;
            case THUNDER -> BSAttributeManager.THUNDER;
            case LIGHT -> BSAttributeManager.LIGHT;
            case DARK -> BSAttributeManager.DARK;
            default -> "";
        };
    }

    private void applyOnHitEffects(LivingEntity target) {
        switch (profile) {
            case FIRE, HELLFIRE, CHAOS_EXPLOSION -> target.addEffect(new MobEffectInstance(BlackSouls.BUFF_BURN.get(), 400, 0));
            case DROWNING_BUBBLE -> target.addEffect(new MobEffectInstance(BlackSouls.BUFF_FEAR.get(), 300, 0));
            case FREEZING_MAGIC_BULLET -> {
                StatEventHandler.applySpeedDown(target, 600);
                if (target.getRandom().nextDouble() < 0.30D) {
                    target.addEffect(new MobEffectInstance(BlackSouls.BUFF_FROSTBITE.get(), 600, 0));
                }
            }
            case DIVINE_THUNDER, DIVINE_BEAST_THUNDER -> target.addEffect(new MobEffectInstance(BlackSouls.BUFF_STUN.get(), 60, 0));
            case DENSE_SPIROCHETE, BLACK_WAVE -> target.addEffect(new MobEffectInstance(BlackSouls.BUFF_FRAGILE.get(), 600, 0));
            case KATARINA_WHEEL, ACID_RAIN -> target.addEffect(new MobEffectInstance(BlackSouls.BUFF_BLEEDING.get(), 600, 0));
            case GLOOMY_SWAMP -> {
                applyAllStatsDown(target, 600);
                target.removeEffect(BlackSouls.BUFF_DAGGER_EVASION.get());
            }
            case SERPENT_EMBRACE -> {
                target.addEffect(new MobEffectInstance(BlackSouls.BUFF_POISON.get(), 1200, 0));
                target.addEffect(new MobEffectInstance(BlackSouls.BUFF_SEVERE_POISON.get(), 600, 0));
            }
            default -> {
            }
        }
    }

    private void castDispel(ServerPlayer player) {
        for (LivingEntity target : getEnemies(player, 14.0D)) {
            for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
                if (effect.getEffect().getCategory() == MobEffectCategory.BENEFICIAL) {
                    target.removeEffect(effect.getEffect());
                }
            }
            playAnimation(target);
        }
        playCastSound(player);
    }

    private void castStatusOnEnemies(ServerPlayer player, net.minecraft.world.effect.MobEffect effect, int duration, double chance) {
        for (LivingEntity target : getEnemies(player, 14.0D)) {
            if (target.getRandom().nextDouble() < chance) {
                target.addEffect(new MobEffectInstance(effect, duration, 0));
            }
            playAnimation(target);
        }
        playCastSound(player);
    }

    private void castCure(ServerPlayer player, boolean all) {
        List<LivingEntity> targets = all ? getAllies(player, 12.0D) : List.of(selectAlly(player));
        for (LivingEntity target : targets) {
            clearHarmfulEffects(target);
            playAnimation(target);
        }
        playCastSound(player);
    }

    private void castMagicBlessing(ServerPlayer player) {
        StatEventHandler.applyMagicAttackUp(player, 600);
        StatEventHandler.applyMagicAttackUp(player, 600);
        playAnimation(player);
        playCastSound(player);
    }

    private void castFullBlessing(ServerPlayer player) {
        for (LivingEntity ally : getAllies(player, 12.0D)) {
            applyAllStatsUp(ally, 600);
            playAnimation(ally);
        }
        playCastSound(player);
    }

    private void castResurrection(ServerPlayer player) {
        LivingEntity ally = selectAlly(player);
        ally.heal(ally.getMaxHealth() * 0.50F);
        ally.addEffect(new MobEffectInstance(BlackSouls.BUFF_REQUIEM.get(), 200, 0));
        playAnimation(ally);
        playCastSound(player);
    }

    private void castManaAbsorption(ServerPlayer player, BSPlayerStats stats) {
        LivingEntity target = selectSingle(player, getEnemies(player, 14.0D));
        double amount = Math.max(1.0D, 20.0D + stats.magicAttack - StatEventHandler.getRpgMagicDefense(target) * 0.5D);
        if (target instanceof ServerPlayer targetPlayer) {
            targetPlayer.getCapability(BSPlayerStats.CAPABILITY).ifPresent(targetStats -> {
                targetStats.mp = Math.max(0.0D, targetStats.mp - amount);
                StatEventHandler.syncToClient(targetPlayer);
            });
        }
        stats.restoreMP(amount);
        StatEventHandler.syncToClient(player);
        playAnimation(target);
        playImpactSound(target, Element.DARK);
    }

    private void castKingsCommand(ServerPlayer player) {
        for (LivingEntity ally : getAllies(player, 12.0D)) {
            StatEventHandler.applyAttackUp(ally, 600);
            StatEventHandler.applyAttackUp(ally, 600);
            StatEventHandler.applyDefenseUp(ally, 600);
            StatEventHandler.applyDefenseUp(ally, 600);
            ally.addEffect(new MobEffectInstance(BlackSouls.BUFF_STRUGGLE.get(), 600, 0));
            ally.addEffect(new MobEffectInstance(BlackSouls.BUFF_AIM.get(), 600, 0));
            playAnimation(ally);
        }
        playCastSound(player);
    }

    private void castFullCurse(ServerPlayer player) {
        for (LivingEntity target : getEnemies(player, 14.0D)) {
            applyAllStatsDown(target, 600);
            playAnimation(target);
        }
        playCastSound(player);
    }

    private void castSimpleBuff(ServerPlayer player, net.minecraft.world.effect.MobEffect effect, int duration) {
        player.addEffect(new MobEffectInstance(effect, duration, 0));
        playAnimation(player);
        playCastSound(player);
    }

    private void castVerdantPower(ServerPlayer player) {
        player.heal(player.getMaxHealth() * 0.50F);
        clearHarmfulEffects(player);
        playAnimation(player);
        playCastSound(player);
    }

    private void castRockBody(ServerPlayer player) {
        StatEventHandler.applyDefenseUp(player, 600);
        StatEventHandler.applyDefenseUp(player, 600);
        StatEventHandler.applySpeedDown(player, 600);
        player.addEffect(new MobEffectInstance(BlackSouls.BUFF_KNIGHTS_GLORY.get(), 600, 0));
        playAnimation(player);
        playCastSound(player);
    }

    private void castAlliedBuff(ServerPlayer player, net.minecraft.world.effect.MobEffect effect, int duration) {
        for (LivingEntity ally : getAllies(player, 12.0D)) {
            ally.addEffect(new MobEffectInstance(effect, duration, 0));
            playAnimation(ally);
        }
        playCastSound(player);
    }

    private void castPhalanx(ServerPlayer player) {
        StatEventHandler.applyAttackUp(player, 600);
        StatEventHandler.applyAttackUp(player, 600);
        StatEventHandler.applyDefenseUp(player, 600);
        StatEventHandler.applyDefenseUp(player, 600);
        StatEventHandler.applySpeedDown(player, 600);
        playAnimation(player);
        playCastSound(player);
    }

    private void castSoulShield(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(BlackSouls.BUFF_ECLIPSE.get(), 600, 0));
        player.addEffect(new MobEffectInstance(BlackSouls.BUFF_DAGGER_GUARD.get(), 600, 0));
        playAnimation(player);
        playCastSound(player);
    }

    private void castMeatWall(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        EntityMeatWall wall = BSEntityRegistry.MEAT_WALL.get().create(level);
        if (wall == null) {
            return;
        }
        Vec3 spawn = player.position().add(player.getLookAngle().normalize().scale(2.0D));
        wall.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), 0.0F);
        wall.setOwner(player);
        wall.setLifetimeTicks(600);
        if (wall.getAttribute(Attributes.MAX_HEALTH) != null) {
            wall.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(100.0D, player.getMaxHealth() * 2.0D));
        }
        wall.setHealth(wall.getMaxHealth());
        level.addFreshEntity(wall);
        playAnimation(wall);
        playCastSound(player);
    }

    private void castRoyalTea(ServerPlayer player) {
        LivingEntity ally = selectAlly(player);
        ally.addEffect(new MobEffectInstance(BlackSouls.BUFF_HAKI.get(), 500, 0));
        ally.addEffect(new MobEffectInstance(BlackSouls.BUFF_FIRE_POWER.get(), 500, 0));
        ally.addEffect(new MobEffectInstance(BlackSouls.BUFF_ICE_POWER.get(), 500, 0));
        ally.addEffect(new MobEffectInstance(BlackSouls.BUFF_THUNDER_POWER.get(), 500, 0));
        ally.addEffect(new MobEffectInstance(BlackSouls.BUFF_STRUGGLE.get(), 500, 1));
        playAnimation(ally);
        playCastSound(player);
    }

    private void castPaladinBanner(ServerPlayer player) {
        for (LivingEntity ally : getAllies(player, 12.0D)) {
            ally.addEffect(new MobEffectInstance(BlackSouls.BUFF_ECLIPSE.get(), 600, 0));
            ally.addEffect(new MobEffectInstance(BlackSouls.BUFF_NATURAL_RECOVERY.get(), 600, 0));
            playAnimation(ally);
        }
        playCastSound(player);
    }

    private void applyAllStatsUp(LivingEntity entity, int duration) {
        StatEventHandler.applyAttackUp(entity, duration);
        StatEventHandler.applyDefenseUp(entity, duration);
        StatEventHandler.applyMagicAttackUp(entity, duration);
        StatEventHandler.applyMagicDefenseUp(entity, duration);
        StatEventHandler.applyLuckUp(entity, duration);
        StatEventHandler.applySpeedUp(entity, duration);
    }

    private void applyAllStatsDown(LivingEntity entity, int duration) {
        StatEventHandler.applyAttackDown(entity, duration);
        StatEventHandler.applyDefenseDown(entity, duration);
        StatEventHandler.applyMagicAttackDown(entity, duration);
        StatEventHandler.applyMagicDefenseDown(entity, duration);
        StatEventHandler.applyLuckDown(entity, duration);
        StatEventHandler.applySpeedDown(entity, duration);
    }

    private void clearHarmfulEffects(LivingEntity target) {
        for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                target.removeEffect(effect.getEffect());
            }
        }
    }

    private List<LivingEntity> getEnemies(ServerPlayer player, double range) {
        List<LivingEntity> enemies = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                target -> target != player && target.isAlive() && !target.isSpectator()
                        && !player.isAlliedTo(target)
                        && !(target instanceof EntityMeatWall wall && wall.isOwnedBy(player))
        );
        enemies.sort(Comparator.comparingDouble(player::distanceToSqr));
        return enemies;
    }

    private List<LivingEntity> getAllies(ServerPlayer player, double range) {
        List<LivingEntity> allies = new ArrayList<>();
        allies.add(player);
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range))) {
            if (entity == player) {
                continue;
            }
            if (entity instanceof Player
                    || entity instanceof TamableAnimal tamable && tamable.isOwnedBy(player)
                    || entity instanceof EntityMeatWall wall && wall.isOwnedBy(player)) {
                allies.add(entity);
            }
        }
        return allies;
    }

    private LivingEntity selectSingle(ServerPlayer player, List<LivingEntity> enemies) {
        LivingEntity aimed = findAimedLiving(player, 14.0D);
        if (aimed != null && enemies.contains(aimed)) {
            return aimed;
        }
        return enemies.get(0);
    }

    private LivingEntity selectAlly(ServerPlayer player) {
        LivingEntity aimed = findAimedLiving(player, 12.0D);
        if (aimed instanceof Player
                || aimed instanceof TamableAnimal tamable && tamable.isOwnedBy(player)
                || aimed instanceof EntityMeatWall wall && wall.isOwnedBy(player)) {
            return aimed;
        }
        return player;
    }

    private LivingEntity findAimedLiving(Player player, double range) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        LivingEntity selected = null;
        double closest = Double.MAX_VALUE;
        List<Entity> entities = player.level().getEntities(player, player.getBoundingBox().inflate(range));
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) {
                continue;
            }
            AABB bounds = living.getBoundingBox().inflate(0.5D);
            Optional<Vec3> hit = bounds.clip(eye, end);
            if (hit.isPresent()) {
                double distance = eye.distanceToSqr(hit.get());
                if (distance < closest) {
                    closest = distance;
                    selected = living;
                }
            }
        }
        return selected;
    }

    private void playAnimation(LivingEntity target) {
        if (profile.animationId <= 0 || target == null || target.isRemoved()) {
            return;
        }
        NetworkHandler.sendToAllAround(new PacketPlayAnim(
                profile.animationId,
                target.getX(),
                target.getY() + target.getBbHeight() / 2.0F,
                target.getZ()
        ), target);
    }

    private void playCastSound(LivingEntity source) {
        source.level().playSound(null, source.getX(), source.getY(), source.getZ(),
                BlackSouls.MAGIC4_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private void playImpactSound(LivingEntity target, Element element) {
        SoundEvent sound = switch (element) {
            case FIRE -> BlackSouls.FIRE3_EVENT.get();
            case ICE -> BlackSouls.ICE7_EVENT.get();
            case THUNDER -> BlackSouls.THUNDER7_EVENT.get();
            case LIGHT -> BlackSouls.ICE2_EVENT.get();
            case DARK -> BlackSouls.DARKNESS5_EVENT.get();
            default -> BlackSouls.MAGIC4_EVENT.get();
        };
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, 0.9F, 1.0F);
    }
}

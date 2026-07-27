package com.BlackSouls.BlackSoulsMod.combat;

import com.BlackSouls.BlackSoulsMod.BSConfig;
import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalDatabaseEnemy;
import com.BlackSouls.BlackSoulsMod.entity.EntityTurnBattleMonster;
import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.item.consumables.ItemThrownBladeBase;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundTurnBattlePacket;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketSyncStats;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundTurnBattleActionPacket;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalItemData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalBattleProfileData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalEnemyData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalEnemyPhaseData;
import com.BlackSouls.BlackSoulsMod.util.DifficultyManager;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import com.BlackSouls.BlackSoulsMod.util.skill.AbstractSkill;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillRegistry;
import com.BlackSouls.BlackSoulsMod.util.skill.WeaponSkill;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class TurnBattleManager {
    private static final int ESCAPE_GRACE_TICKS = 160;
    private static final int GRAN_STAGE_PROFILE = 570;
    private static final int GRAN_FINAL_STAGE_PROFILE = 577;
    private static final int GRAN_FRAGMENT_FIRST_PROFILE = 580;
    private static final int GRAN_FRAGMENT_LAST_PROFILE = 586;
    private static final Set<String> NON_DAMAGE_SKILLS = Set.of(
            "bs2_skill_absolute_hit", "bs2_skill_aim", "bs2_skill_awakening",
            "bs2_skill_berserker_roar", "bs2_skill_blood_trail",
            "bs2_skill_bullet_load", "bs2_skill_chakra", "bs2_skill_chrono_clock",
            "bs2_skill_counter", "bs2_skill_cure",
            "bs2_skill_delicious_turtle_soup", "bs2_skill_dispel",
            "bs2_skill_dodo_run", "bs2_skill_eclipse", "bs2_skill_erase",
            "bs2_skill_fatal_guard", "bs2_skill_full_blessing", "bs2_skill_full_curse",
            "bs2_skill_godspeed_dance", "bs2_skill_grit", "bs2_skill_gunpowder_replenish",
            "bs2_skill_haki", "bs2_skill_hasso", "bs2_skill_hypnosis",
            "bs2_skill_inner_potential", "bs2_skill_invisible_body", "bs2_skill_juggling_evasion",
            "bs2_skill_kings_command", "bs2_skill_knights_glory", "bs2_skill_mad_bird_call",
            "bs2_skill_magic_blessing", "bs2_skill_mana_absorption", "bs2_skill_mana_burn",
            "bs2_skill_mana_recovery", "bs2_skill_mental_focus", "bs2_skill_mind_eye",
            "bs2_skill_paladin_banner", "bs2_skill_peerless_challenge", "bs2_skill_phalanx",
            "bs2_skill_poison", "bs2_skill_poison_ii", "bs2_skill_quick_reload",
            "bs2_skill_rage", "bs2_skill_reinforce", "bs2_skill_requiem",
            "bs2_skill_resurrection", "bs2_skill_rock_body", "bs2_skill_royal_tea",
            "bs2_skill_self_harm", "bs2_skill_shadowless", "bs2_skill_slaughter_begins",
            "bs2_skill_smoldering_frenzy", "bs2_skill_soul_light", "bs2_skill_soul_shield",
            "bs2_skill_strong_crush", "bs2_skill_struggle", "bs2_skill_summon_meat_wall",
            "bs2_skill_verdant_power"
    );
    private static final Set<String> ALL_TARGET_SKILLS = Set.of(
            "bs2_skill_divine_beast_thunder", "bs2_skill_soul_stream",
            "bs2_skill_rain_of_ruin", "bs2_skill_dark_dance",
            "bs2_skill_great_soul_arrow_volley", "bs2_skill_destruction_storm",
            "bs2_skill_katarina_wheel", "bs2_skill_rampage",
            "bs2_skill_freezing_magic_bullet", "bs2_skill_hellfire",
            "bs2_skill_dark_swarm", "bs2_skill_meteor_swarm",
            "bs2_skill_ghost_fire", "bs2_skill_chaos_explosion",
            "bs2_skill_acid_rain", "bs2_skill_black_wave",
            "bs2_skill_blood_edge", "bs2_skill_lion_whirlwind",
            "bs2_skill_moonlight_break", "bs2_skill_storm_overlord",
            "bs2_skill_tempest_rend", "bs2_skill_true_soul_harvest",
            "bs2_skill_tsubame_gaeshi"
    );
    private static final Map<UUID, Session> PLAYER_SESSIONS = new HashMap<>();
    private static final Map<UUID, UUID> ENEMY_SESSIONS = new HashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL_BATTLE_ITEM_DAMAGE =
            ThreadLocal.withInitial(() -> false);

    private TurnBattleManager() {
    }

    public static void tryStart(ServerPlayer player, EntityTurnBattleMonster enemy) {
        if (BSConfig.COMBAT_MODE.get() != BSConfig.CombatMode.BLACK_SOULS_TURN_BASED
                || player.isCreative() || player.isSpectator() || !player.isAlive()
                || enemy.isTurnBattleDefeated()
                || PLAYER_SESSIONS.containsKey(player.getUUID()) || ENEMY_SESSIONS.containsKey(enemy.getUUID())) {
            return;
        }
        int profileId = enemy instanceof EntityOriginalDatabaseEnemy originalEnemy
                ? originalEnemy.getProfileId() : -1;
        Session session = new Session(enemy.getUUID(), player.position(), enemy.position(), profileId);
        PLAYER_SESSIONS.put(player.getUUID(), session);
        configureInitialEnemies(player, enemy, session);
        sendState(player, true,
                Component.literal(enemy.getDisplayName().getString() + "出现了！"),
                true, ClientboundTurnBattlePacket.Outcome.NONE, 0L);
    }

    public static boolean isInBattle(Entity entity) {
        return PLAYER_SESSIONS.containsKey(entity.getUUID()) || ENEMY_SESSIONS.containsKey(entity.getUUID());
    }

    public static void handleAction(ServerPlayer player, ServerboundTurnBattleActionPacket.Action action,
                                    int selection, int targetIndex) {
        Session session = PLAYER_SESSIONS.get(player.getUUID());
        EntityTurnBattleMonster rootEnemy = getRootEnemy(player, session);
        if (session == null || rootEnemy == null || session.resolving) {
            return;
        }
        session.resolving = true;
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) {
            finish(player, session, ClientboundTurnBattlePacket.Outcome.ESCAPED,
                    Component.literal("战斗中断。"), false, 0L);
            return;
        }
        if (session.remainingPlayerActions <= 0) {
            session.remainingPlayerActions = rollPlayerActionCount(player, stats);
        }

        Component playerMessage;
        boolean consumeTurn = true;
        boolean guard = false;
        List<ClientboundTurnBattlePacket.DamageHit> playerHits = List.of();
        switch (action) {
            case ATTACK -> {
                EntityTurnBattleMonster target = getTargetEnemy(player, session, targetIndex);
                if (target == null) {
                    session.resolving = false;
                    sendState(player, true, Component.literal("请选择攻击目标。"), true,
                            ClientboundTurnBattlePacket.Outcome.NONE, 0L);
                    return;
                }
                ActionResult result = performAttack(player, target, stats);
                playerMessage = result.message;
                playerHits = result.hits;
            }
            case SKILL -> {
                ActionResult result = performSkill(player, stats, session, selection, targetIndex);
                playerMessage = result.message;
                consumeTurn = result.consumeTurn;
                playerHits = result.hits;
            }
            case ITEM -> {
                EntityTurnBattleMonster target = firstAliveEnemy(player, session);
                ActionResult result = target == null
                        ? new ActionResult(Component.literal("没有可用的目标。"), false)
                        : performItem(player, target, stats, selection);
                playerMessage = result.message;
                consumeTurn = result.consumeTurn;
            }
            case GUARD -> {
                guard = true;
                playerMessage = Component.literal(player.getDisplayName().getString() + "采取了防御姿态！");
            }
            case ESCAPE -> {
                double chance = Mth.clamp(0.50D + (stats.speed - 40.0D) / 200.0D, 0.10D, 0.95D);
                if (player.getRandom().nextDouble() < chance) {
                    finish(player, session, ClientboundTurnBattlePacket.Outcome.ESCAPED,
                            Component.literal(player.getDisplayName().getString() + "成功逃走了！"), false, 0L);
                    return;
                }
                playerMessage = Component.literal(player.getDisplayName().getString() + "没能逃走！");
            }
            case WEAPON_CHANGE -> {
                ActionResult result = changeWeapon(player, selection);
                playerMessage = result.message;
                consumeTurn = result.consumeTurn;
            }
            default -> {
                session.resolving = false;
                return;
            }
        }

        session.playerHits.clear();
        session.playerHits.addAll(playerHits);

        if (!consumeTurn) {
            session.resolving = false;
            sendState(player, true, playerMessage, true,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            return;
        }
        session.remainingPlayerActions = Math.max(0, session.remainingPlayerActions - 1);
        session.guardQueued |= guard;
        updateGranStage(player, session);
        if (canAdvanceEnemyPhase(player, rootEnemy, session)) {
            advanceSkillCooldowns(session);
            StatEventHandler.syncToClient(player);
            session.awaitingPhasePresentation = true;
            session.presentationDeadline = player.serverLevel().getGameTime() + 200L;
            sendState(player, true, playerMessage, false,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            return;
        }
        if (allEnemiesDefeated(player, session)) {
            long reward = rewardVictory(player, stats, session);
            finish(player, session, ClientboundTurnBattlePacket.Outcome.VICTORY,
                    Component.literal(playerMessage.getString() + "\n"
                            + "敌群被打倒了！"),
                    true, reward);
            return;
        }

        if (session.remainingPlayerActions > 0) {
            session.resolving = false;
            StatEventHandler.syncToClient(player);
            sendState(player, true, playerMessage, true,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            return;
        }

        Component enemyMessage = performEnemyGroupTurn(player, stats, session.guardQueued, session);
        session.guardQueued = false;
        advanceSkillCooldowns(session);
        StatEventHandler.syncToClient(player);
        Component turnMessage = Component.literal(playerMessage.getString() + "\n" + enemyMessage.getString());
        if (player.getHealth() <= 0.0F || !player.isAlive()) {
            finish(player, session, ClientboundTurnBattlePacket.Outcome.DEFEAT,
                    turnMessage, false, 0L);
            return;
        }
        session.remainingPlayerActions = rollPlayerActionCount(player, stats);
        session.resolving = false;
        sendState(player, true, turnMessage, true,
                ClientboundTurnBattlePacket.Outcome.NONE, 0L);
    }

    private static ActionResult performAttack(ServerPlayer player, EntityTurnBattleMonster enemy, BSPlayerStats stats) {
        String actor = player.getDisplayName().getString();
        String target = enemy.getDisplayName().getString();
        if (player.getRandom().nextDouble() >= 0.90D) {
            return new ActionResult(Component.literal(actor + "的攻击！\n但是没有命中" + target + "！"), true);
        }
        double enemyDefense = DifficultyManager.scaleManagedStat(
                enemy.level(), enemy.getTurnBattleDefense());
        double damage = varied(player, Math.max(
                1.0D, stats.attack * 4.0D - enemyDefense * 2.0D));
        boolean critical = player.getRandom().nextDouble() < (stats.critRate + stats.bonusCritRate) / 100.0D;
        if (critical) {
            damage *= 3.0D;
        }
        int dealt = Math.max(1, (int) Math.round(damage));
        enemy.setTurnBattleHealth(enemy.getTurnBattleHealth() - dealt);
        StringBuilder message = new StringBuilder(actor).append("的攻击！\n");
        if (critical) {
            message.append("会心一击！\n");
        }
        message.append("对").append(target).append("造成了 ").append(dealt).append(" 点伤害！");
        return new ActionResult(Component.literal(message.toString()), true,
                List.of(new ClientboundTurnBattlePacket.DamageHit(
                        enemy.getId(), dealt, critical, 0)));
    }

    public static boolean skillRequiresTarget(AbstractSkill skill) {
        return skill != null && !NON_DAMAGE_SKILLS.contains(skill.getSkillId())
                && !ALL_TARGET_SKILLS.contains(skill.getSkillId());
    }

    public static boolean skillTargetsAll(AbstractSkill skill) {
        return skill != null && ALL_TARGET_SKILLS.contains(skill.getSkillId());
    }

    private static ActionResult performSkill(ServerPlayer player, BSPlayerStats stats,
                                             Session session, int selection, int targetIndex) {
        if (selection < 0) {
            return new ActionResult(Component.literal("这个技能当前不可用。"), false);
        }
        AbstractSkill skill = SkillRegistry.SKILLS.values().stream().skip(selection).findFirst().orElse(null);
        if (skill == null || !skill.isUnlockedForGUI(player)) {
            return new ActionResult(Component.literal("这个技能当前不可用。"), false);
        }
        boolean infiniteCooldown = SkillUtils.hasInfiniteCooldownAccessory(player);
        int remainingCooldown = infiniteCooldown ? 0 : session.skillCooldowns.getOrDefault(skill.getSkillId(), 0);
        if (remainingCooldown > 0) {
            return new ActionResult(Component.literal("技能正在冷却（CD" + remainingCooldown + "）。"), false);
        }
        if (!skill.canCastInTurnBattle(player, stats)) {
            return new ActionResult(Component.literal("这个技能当前不可用。"), false);
        }
        EntityTurnBattleMonster selectedTarget = null;
        if (skillRequiresTarget(skill)) {
            selectedTarget = getTargetEnemy(player, session, targetIndex);
            if (selectedTarget == null) {
                return new ActionResult(Component.literal("请选择技能目标。"), false);
            }
        }
        skill.consumeForTurnBattle(player, stats);
        int cooldownRounds = skill.getTurnCooldownRounds();
        if (!infiniteCooldown && cooldownRounds > 0) {
            session.skillCooldowns.put(skill.getSkillId(), cooldownRounds);
        }
        String skillName = Component.translatable(skill.getTranslationKey()).getString();
        String actor = player.getDisplayName().getString();
        if (NON_DAMAGE_SKILLS.contains(skill.getSkillId())) {
            Map<UUID, Double> healthBeforeEffect = new HashMap<>();
            for (EntityTurnBattleMonster enemy : getEnemies(player, session)) {
                healthBeforeEffect.put(enemy.getUUID(), enemy.getTurnBattleHealth());
            }
            skill.execute(player, stats);
            for (EntityTurnBattleMonster enemy : getEnemies(player, session)) {
                Double health = healthBeforeEffect.get(enemy.getUUID());
                if (health != null) {
                    enemy.setTurnBattleHealth(health);
                }
            }
            StatEventHandler.syncToClient(player);
            return new ActionResult(Component.literal(actor + "使用了" + skillName + "！"), true);
        }
        List<EntityTurnBattleMonster> targets;
        if (skillTargetsAll(skill)) {
            targets = getEnemies(player, session).stream()
                    .filter(enemy -> isTargetableEnemy(player, session, enemy)).toList();
        } else {
            targets = List.of(selectedTarget);
        }
        if (targets.isEmpty()) {
            return new ActionResult(Component.literal("没有可用的目标。"), false);
        }
        int hitCount = turnHitCount(player, skill);
        List<ClientboundTurnBattlePacket.DamageHit> hits = new ArrayList<>();
        Map<UUID, Integer> totalDamage = new HashMap<>();
        StringBuilder message = new StringBuilder(actor).append("使用了").append(skillName).append("！");
        for (int wave = 0; wave < hitCount; wave++) {
            for (EntityTurnBattleMonster enemy : targets) {
                double enemyDefense = DifficultyManager.scaleManagedStat(
                        enemy.level(), skill instanceof WeaponSkill
                                ? enemy.getTurnBattleDefense()
                                : enemy.getTurnBattleMagicDefense());
                double baseDamage = skill instanceof WeaponSkill
                        ? stats.attack * 4.0D - enemyDefense * 2.0D
                        : stats.magicAttack * 5.0D - enemyDefense * 2.0D;
                double damage = varied(player, Math.max(1.0D, baseDamage));
                int dealt = Math.max(1, (int) Math.round(damage));
                enemy.setTurnBattleHealth(enemy.getTurnBattleHealth() - dealt);
                hits.add(new ClientboundTurnBattlePacket.DamageHit(
                        enemy.getId(), dealt, false, wave));
                totalDamage.merge(enemy.getUUID(), dealt, Integer::sum);
            }
        }
        for (EntityTurnBattleMonster enemy : targets) {
            message.append("\n对").append(enemy.getDisplayName().getString())
                    .append("造成了 ").append(totalDamage.getOrDefault(enemy.getUUID(), 0))
                    .append(" 点伤害！");
        }
        StatEventHandler.syncToClient(player);
        return new ActionResult(Component.literal(message.toString()), true, hits);
    }

    private static int turnHitCount(ServerPlayer player, AbstractSkill skill) {
        return switch (skill.getSkillId()) {
            case "bs2_skill_arrow_rain", "bs2_skill_divine_beast_thunder",
                    "bs2_skill_forward_slash",
                    "bs2_skill_moonlight_blade", "bs2_skill_overhead_barrage",
                    "bs2_skill_ultimate_triple_slash" -> 3;
            case "bs2_skill_black_slash", "bs2_skill_great_soul_arrow_volley",
                    "bs2_skill_piercing_icicle", "bs2_skill_soul_stream",
                    "bs2_skill_soul_volley", "bs2_skill_storm_overlord",
                    "bs2_skill_zenith_blade" -> 4;
            case "bs2_skill_blood_edge", "bs2_skill_cross_slash",
                    "bs2_skill_dark_dance", "bs2_skill_double_collision",
                    "bs2_skill_moonlight_break", "bs2_skill_rain_of_ruin",
                    "bs2_skill_tempest_rend", "bs2_skill_tsubame_gaeshi",
                    "bs2_skill_vorpal_slash" -> 2;
            case "bs2_skill_corpse_dragon_awe", "bs2_skill_mental_break" -> 5;
            case "bs2_skill_destruction_storm", "bs2_skill_true_soul_harvest" -> 6;
            case "bs2_skill_hundred_fists" -> 9;
            case "bs2_skill_katarina_wheel" -> 2;
            case "bs2_skill_gale_sixfold_thrust" -> player.getRandom().nextBoolean() ? 4 : 2;
            case "bs2_skill_lion_whirlwind" -> {
                int volleys = 1;
                while (volleys < 8 && player.getRandom().nextBoolean()) {
                    volleys++;
                }
                yield volleys * 3;
            }
            case "bs2_skill_blood_trial", "bs2_skill_blessing_of_pain" ->
                    player.hasEffect(BlackSouls.BUFF_SLAUGHTER_MODE.get()) ? 4 : 1;
            default -> 1;
        };
    }

    private static boolean canAdvanceEnemyPhase(ServerPlayer player,
                                                EntityTurnBattleMonster rootEnemy,
                                                Session session) {
        if (!(rootEnemy instanceof EntityOriginalDatabaseEnemy)) {
            return false;
        }
        BSOriginalEnemyPhaseData.Transition transition =
                BSOriginalEnemyPhaseData.get(session.battleProfileId);
        if (transition == null) {
            return false;
        }
        List<EntityTurnBattleMonster> enemies = getEnemies(player, session);
        EntityTurnBattleMonster controller = enemies.stream()
                .filter(enemy -> enemy instanceof EntityOriginalDatabaseEnemy original
                        && original.getProfileId() == session.battleProfileId)
                .findFirst().orElse(rootEnemy);
        boolean groupPhase = enemies.size() > 1;
        double ratio = controller.getTurnBattleHealth() * 100.0D
                / Math.max(1.0D, controller.getTurnBattleMaxHealth());
        return groupPhase && transition.thresholdPercent() <= 1.0D
                && !usesControllerPhaseTransition(session.battleProfileId)
                ? enemies.stream().allMatch(EntityTurnBattleMonster::isTurnBattleDefeated)
                : ratio <= transition.thresholdPercent();
    }

    public static void handlePresentationComplete(ServerPlayer player) {
        Session session = PLAYER_SESSIONS.get(player.getUUID());
        EntityTurnBattleMonster rootEnemy = getRootEnemy(player, session);
        if (session == null || rootEnemy == null || !session.resolving
                || !session.awaitingPhasePresentation) {
            return;
        }
        session.awaitingPhasePresentation = false;
        session.presentationDeadline = 0L;
        PhaseAdvance phaseAdvance = advanceEnemyPhase(player, rootEnemy, session);
        session.resolving = false;
        if (phaseAdvance == null) {
            sendState(player, true, Component.empty(), true,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            return;
        }
        session.phaseChanged = true;
        session.remainingPlayerActions = rollPlayerActionCount(player,
                player.getCapability(BSPlayerStats.CAPABILITY).orElse(null));
        session.guardQueued = false;
        StatEventHandler.syncToClient(player);
        sendState(player, true, Component.empty(), true,
                ClientboundTurnBattlePacket.Outcome.NONE, 0L);
    }

    private static PhaseAdvance advanceEnemyPhase(ServerPlayer player,
                                                  EntityTurnBattleMonster rootEnemy,
                                                  Session session) {
        if (!(rootEnemy instanceof EntityOriginalDatabaseEnemy)) {
            return null;
        }
        BSOriginalEnemyPhaseData.Transition transition =
                BSOriginalEnemyPhaseData.get(session.battleProfileId);
        if (transition == null) {
            return null;
        }
        List<EntityTurnBattleMonster> enemies = getEnemies(player, session);
        EntityTurnBattleMonster controller = enemies.stream()
                .filter(enemy -> enemy instanceof EntityOriginalDatabaseEnemy original
                        && original.getProfileId() == session.battleProfileId)
                .findFirst().orElse(rootEnemy);
        boolean groupPhase = enemies.size() > 1;
        double ratio = controller.getTurnBattleHealth() * 100.0D
                / Math.max(1.0D, controller.getTurnBattleMaxHealth());
        boolean shouldAdvance = groupPhase && transition.thresholdPercent() <= 1.0D
                && !usesControllerPhaseTransition(session.battleProfileId)
                ? enemies.stream().allMatch(EntityTurnBattleMonster::isTurnBattleDefeated)
                : ratio <= transition.thresholdPercent();
        if (!shouldAdvance) {
            return null;
        }
        String initialName = controller.getDisplayName().getString();
        double previousHealth = controller.getTurnBattleHealth();
        rebuildEnemyGroup(player, rootEnemy, session, transition.to());
        EntityTurnBattleMonster nextController = getEnemies(player, session).stream()
                .filter(enemy -> enemy instanceof EntityOriginalDatabaseEnemy original
                        && original.getProfileId() == transition.to())
                .findFirst().orElse(rootEnemy);
        if (!transition.recoverAll()) {
            double nextHealth = previousHealth > 0.0D
                    ? Math.min(previousHealth, nextController.getTurnBattleMaxHealth())
                    : Math.max(1.0D, nextController.getTurnBattleMaxHealth()
                    * Math.max(0.01D, transition.thresholdPercent() / 100.0D));
            nextController.setTurnBattleHealth(nextHealth);
        }
        session.enemyTurn = 1;
        session.lastEnemyAnimationId = nextController.getTurnBattleAttackAnimationId();
        session.actingEnemyIndex = Math.max(0, session.enemyIds.indexOf(nextController.getUUID()));
        String finalName = nextController.getDisplayName().getString();
        String message = initialName.equals(finalName)
                ? finalName + "的形态发生了变化！"
                : initialName + "变为了" + finalName + "！";
        return new PhaseAdvance(message);
    }

    private static boolean usesControllerPhaseTransition(int profileId) {
        return profileId == 561 || profileId == 568;
    }

    private static void configureInitialEnemies(ServerPlayer player,
                                                EntityTurnBattleMonster rootEnemy,
                                                Session session) {
        if (rootEnemy instanceof EntityOriginalDatabaseEnemy) {
            rebuildEnemyGroup(player, rootEnemy, session, session.battleProfileId);
        } else {
            session.enemyIds.add(rootEnemy.getUUID());
            session.enemyAnchors.put(rootEnemy.getUUID(), rootEnemy.position());
            session.enemyStates.put(rootEnemy.getUUID(), new HashSet<>());
            prepareEnemy(player, rootEnemy, session, false);
        }
    }

    private static void rebuildEnemyGroup(ServerPlayer player,
                                          EntityTurnBattleMonster rootEnemy,
                                          Session session, int profileId) {
        for (UUID enemyId : List.copyOf(session.enemyIds)) {
            ENEMY_SESSIONS.remove(enemyId);
            if (!enemyId.equals(session.rootEnemyId)) {
                Entity old = player.serverLevel().getEntity(enemyId);
                if (old != null) {
                    old.discard();
                }
            }
        }
        session.enemyIds.clear();
        session.enemyAnchors.clear();
        session.enemyStates.clear();
        session.enemyTurns.clear();
        session.battleProfileId = profileId;
        BSOriginalBattleProfileData.Entry battle = BSOriginalBattleProfileData.get(profileId);
        List<BSOriginalBattleProfileData.Member> members = battle.members().isEmpty()
                ? List.of(new BSOriginalBattleProfileData.Member(profileId, List.of()))
                : battle.members();
        int rootIndex = 0;
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).profileId() == profileId) {
                rootIndex = i;
                break;
            }
        }
        for (int i = 0; i < members.size(); i++) {
            BSOriginalBattleProfileData.Member member = members.get(i);
            EntityOriginalDatabaseEnemy enemy;
            boolean virtual;
            if (i == rootIndex) {
                enemy = (EntityOriginalDatabaseEnemy) rootEnemy;
                virtual = false;
            } else {
                enemy = BSEntityRegistry.ORIGINAL_ENEMY.get().create(player.serverLevel());
                if (enemy == null) {
                    continue;
                }
                enemy.moveTo(session.enemyAnchor.x, session.enemyAnchor.y,
                        session.enemyAnchor.z, rootEnemy.getYRot(), rootEnemy.getXRot());
                player.serverLevel().addFreshEntity(enemy);
                virtual = true;
            }
            enemy.setProfileId(member.profileId());
            enemy.setTurnBattleDifficultyMultiplier(
                    DifficultyManager.getCurrentTotalMultiplierForLevel(player.serverLevel()));
            enemy.setTurnBattleHealth(enemy.getTurnBattleMaxHealth());
            Set<Integer> states = new HashSet<>(
                    BSOriginalEnemyData.get(member.profileId()).initialStates());
            states.addAll(member.states());
            session.enemyIds.add(enemy.getUUID());
            session.enemyAnchors.put(enemy.getUUID(), enemy.position());
            session.enemyStates.put(enemy.getUUID(), states);
            session.enemyTurns.put(enemy.getUUID(), 1);
            prepareEnemy(player, enemy, session, virtual);
        }
    }

    private static void prepareEnemy(ServerPlayer player, EntityTurnBattleMonster enemy,
                                     Session session, boolean virtual) {
        ENEMY_SESSIONS.put(enemy.getUUID(), player.getUUID());
        enemy.setNoAi(true);
        enemy.setInvulnerable(true);
        enemy.setDeltaMovement(Vec3.ZERO);
        enemy.setInvisible(virtual);
        enemy.setSilent(virtual);
    }

    private static void advanceSkillCooldowns(Session session) {
        session.skillCooldowns.replaceAll((skillId, rounds) -> Math.max(0, rounds - 1));
        session.skillCooldowns.values().removeIf(rounds -> rounds <= 0);
    }

    private static ActionResult performItem(ServerPlayer player, EntityTurnBattleMonster enemy,
                                            BSPlayerStats stats, int slot) {
        if (slot < 0 || slot >= player.getInventory().getContainerSize()) {
            return new ActionResult(Component.literal("请选择要使用的道具。"), false);
        }
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.isEmpty()) {
            return new ActionResult(Component.literal("这个栏位没有道具。"), false);
        }
        BSOriginalItemData.Entry itemData = BSOriginalItemData.get(stack.getItem());
        if (itemData == null || !itemData.canUseInBattle()) {
            return new ActionResult(Component.literal("这个道具不适合在战斗中使用。"), false);
        }
        ItemStack display = stack.copy();
        boolean targetEnemy = itemData.scope() == 1 || itemData.scope() == 2;
        boolean used = executeBattleItem(player, enemy, slot, stack, targetEnemy);
        if (!used) {
            return new ActionResult(Component.literal("这个道具不适合在战斗中使用。"), false);
        }
        StatEventHandler.syncToClient(player);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new ActionResult(Component.literal(player.getDisplayName().getString() + "使用了"
                + display.getHoverName().getString() + "！"), true);
    }

    private static boolean executeBattleItem(ServerPlayer player, EntityTurnBattleMonster enemy,
                                             int slot, ItemStack stack, boolean targetEnemy) {
        boolean wasInvulnerable = enemy.isInvulnerable();
        enemy.setInvulnerable(false);
        INTERNAL_BATTLE_ITEM_DAMAGE.set(true);
        try {
            if (targetEnemy && stack.getItem() instanceof ItemThrownBladeBase thrownBlade) {
                return thrownBlade.useInTurnBattle(player, stack, enemy);
            }
            if (targetEnemy) {
                InteractionResult targetResult = stack.getItem().interactLivingEntity(
                        stack, player, enemy, InteractionHand.MAIN_HAND);
                if (targetResult.consumesAction()) {
                    return true;
                }
            }
            InteractionResultHolder<ItemStack> result = useInventoryItem(player, slot, stack);
            return result.getResult().consumesAction();
        } finally {
            INTERNAL_BATTLE_ITEM_DAMAGE.set(false);
            enemy.setInvulnerable(wasInvulnerable);
        }
    }

    private static InteractionResultHolder<ItemStack> useInventoryItem(ServerPlayer player, int slot,
                                                                       ItemStack stack) {
        int selected = player.getInventory().selected;
        if (slot == selected) {
            InteractionResultHolder<ItemStack> result =
                    stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            player.getInventory().setItem(slot, result.getObject());
            return result;
        }

        ItemStack held = player.getInventory().getItem(selected);
        player.getInventory().setItem(selected, stack);
        player.getInventory().setItem(slot, held);
        InteractionResultHolder<ItemStack> result =
                stack.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        player.getInventory().setItem(slot, result.getObject());
        player.getInventory().setItem(selected, held);
        return result;
    }

    private static ActionResult changeWeapon(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= player.getInventory().getContainerSize()) {
            return new ActionResult(Component.literal("请选择要装备的武器。"), false);
        }
        int selected = player.getInventory().selected;
        ItemStack chosen = player.getInventory().getItem(slot);
        if (chosen.isEmpty() || !isBattleWeapon(chosen)) {
            return new ActionResult(Component.literal("这个栏位没有可装备的武器。"), false);
        }
        if (slot != selected) {
            ItemStack held = player.getInventory().getItem(selected);
            player.getInventory().setItem(selected, chosen);
            player.getInventory().setItem(slot, held);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return new ActionResult(Component.empty(), false);
    }

    public static boolean isBattleWeapon(ItemStack stack) {
        Package itemPackage = stack.getItem().getClass().getPackage();
        return itemPackage != null && itemPackage.getName().contains(".item.weapon");
    }

    public static boolean isBattleItem(ItemStack stack) {
        BSOriginalItemData.Entry entry = BSOriginalItemData.get(stack.getItem());
        return entry != null && entry.canUseInBattle();
    }

    private static Component performEnemyGroupTurn(ServerPlayer player, BSPlayerStats stats,
                                                   boolean guard, Session session) {
        StringBuilder message = new StringBuilder();
        List<EntityTurnBattleMonster> enemies = getEnemies(player, session);
        for (int i = 0; i < enemies.size(); i++) {
            EntityTurnBattleMonster enemy = enemies.get(i);
            if (enemy.isTurnBattleDefeated() || isDormantGranStage(enemy) || !player.isAlive()) {
                continue;
            }
            session.actingEnemyIndex = i;
            Component result = performEnemyTurn(player, enemy, stats, guard, session);
            if (!result.getString().isEmpty()) {
                if (!message.isEmpty()) {
                    message.append("\n");
                }
                message.append(result.getString());
            }
        }
        return Component.literal(message.toString());
    }

    private static Component performEnemyTurn(ServerPlayer player, EntityTurnBattleMonster battleEnemy,
                                              BSPlayerStats stats, boolean guard, Session session) {
        String enemy = battleEnemy.getDisplayName().getString();
        Set<Integer> activeStates = session.enemyStates.computeIfAbsent(
                battleEnemy.getUUID(), ignored -> new HashSet<>());
        int turn = session.enemyTurns.getOrDefault(battleEnemy.getUUID(), 1);
        BSOriginalEnemyData.Action action = battleEnemy instanceof EntityOriginalDatabaseEnemy originalEnemy
                ? originalEnemy.selectTurnBattleAction(player.getRandom(), turn, activeStates)
                : null;
        session.enemyTurns.put(battleEnemy.getUUID(), turn + 1);
        session.enemyTurn++;
        if (action == null) {
            session.lastEnemyAnimationId = battleEnemy.getTurnBattleAttackAnimationId();
            String attackText = battleEnemy.getTurnBattleAttackText();
            if (player.getRandom().nextDouble() >= Mth.clamp(
                    0.95D - stats.evasion / 100.0D, 0.05D, 0.99D)) {
                return Component.literal(enemy + attackText + "！\n但是攻击落空了！");
            }
            double enemyAttack = DifficultyManager.scaleManagedStat(
                    battleEnemy.level(), battleEnemy.getTurnBattleAttack());
            double damage = varied(player, Math.max(
                    1.0D, enemyAttack * 4.0D - stats.defense * 2.0D));
            damage *= Math.max(1, battleEnemy.getTurnBattleAttackRepeats());
            boolean critical = player.getRandom().nextDouble() < 0.05D;
            if (critical) {
                damage *= 3.0D;
            }
            if (guard) {
                damage *= 0.5D;
            }
            damage *= Math.max(0.0D, stats.physicalDamageRate);
            int dealt = safeDamageInt(damage);
            player.setHealth(Math.max(0.0F, player.getHealth() - dealt));
            return Component.literal(enemy + attackText + "！\n"
                    + (critical ? "会心一击！\n" : "")
                    + player.getDisplayName().getString() + "受到了 " + dealt + " 点伤害！");
        }

        session.lastEnemyAnimationId = action.animationId() > 0
                ? action.animationId() : battleEnemy.getTurnBattleAttackAnimationId();
        String actionHeader = enemy + action.text() + "！";
        applyEnemyStateEffects(action, activeStates, player);
        if (action.conditionType() == 4) {
            activeStates.remove((int) action.conditionParam1());
        }

        double difficulty = DifficultyManager.getCurrentTotalMultiplierForLevel(battleEnemy.level());
        TurnBattleFormulaEvaluator.Context context = new TurnBattleFormulaEvaluator.Context(
                battleEnemy.getTurnBattleAttack() * difficulty,
                battleEnemy.getTurnBattleDefense() * difficulty,
                battleEnemy.getTurnBattleMagicAttack() * difficulty,
                battleEnemy.getTurnBattleMagicDefense() * difficulty,
                battleEnemy.getTurnBattleAgility() * difficulty,
                battleEnemy.getTurnBattleLuck() * difficulty,
                battleEnemy.getTurnBattleHealth(),
                battleEnemy.getTurnBattleMaxHealth(),
                battleEnemy.getTurnBattleMana(),
                battleEnemy.getTurnBattleMaxMana(),
                stats.attack,
                stats.defense,
                stats.magicAttack,
                stats.magicDefense,
                stats.speed,
                stats.luck,
                player.getHealth(),
                Math.max(player.getMaxHealth(), stats.hp),
                stats.mp,
                stats.maxMp
        );
        double baseValue = Math.max(0.0D,
                TurnBattleFormulaEvaluator.evaluate(action.formula(), context));

        if (action.damageType() == 3) {
            double healed = repeatedValue(player, baseValue, action.repeats(), action.variance());
            double before = battleEnemy.getTurnBattleHealth();
            battleEnemy.setTurnBattleHealth(before + healed);
            int restored = safeDamageInt(battleEnemy.getTurnBattleHealth() - before);
            return Component.literal(actionHeader + "\n" + enemy + "恢复了 " + restored + " 点生命！");
        }
        if (action.damageType() == 4) {
            return Component.literal(actionHeader);
        }
        if (action.damageType() != 1 && action.damageType() != 2
                && action.damageType() != 5 && action.damageType() != 6) {
            return Component.literal(actionHeader);
        }

        int landedHits = 0;
        boolean critical = false;
        double total = 0.0D;
        int repeats = Math.max(1, action.repeats());
        for (int hit = 0; hit < repeats; hit++) {
            double hitChance = Mth.clamp(action.successRate() / 100.0D
                    - (action.hitType() == 1 ? stats.evasion / 100.0D : 0.0D), 0.0D, 1.0D);
            if (player.getRandom().nextDouble() >= hitChance) {
                continue;
            }
            landedHits++;
            double value = varied(player, baseValue, action.variance());
            if (action.critical() && player.getRandom().nextDouble() < 0.05D) {
                value *= 3.0D;
                critical = true;
            }
            if (guard && (action.damageType() == 1 || action.damageType() == 5)) {
                value *= 0.5D;
            }
            total += Math.max(0.0D, value);
        }
        if (landedHits == 0) {
            return Component.literal(actionHeader + "\n但是攻击落空了！");
        }
        if (action.damageType() == 2 || action.damageType() == 6) {
            int lostMana = Math.min(safeDamageInt(total), safeDamageInt(stats.mp));
            stats.mp = Math.max(0.0D, stats.mp - lostMana);
            return Component.literal(actionHeader + "\n"
                    + player.getDisplayName().getString() + "失去了 " + lostMana + " MP！");
        }

        boolean magicLike = action.hitType() == 2
                || action.formula().contains("a.mat") && !action.formula().contains("a.atk");
        total *= Math.max(0.0D, magicLike ? stats.magicDamageRate : stats.physicalDamageRate);
        int dealt = safeDamageInt(total);
        player.setHealth(Math.max(0.0F, player.getHealth() - dealt));
        if (action.damageType() == 5 && dealt > 0) {
            battleEnemy.setTurnBattleHealth(battleEnemy.getTurnBattleHealth() + dealt);
        }
        return Component.literal(actionHeader + "\n"
                + (critical ? "会心一击！\n" : "")
                + player.getDisplayName().getString() + "受到了 " + dealt + " 点伤害！");
    }

    private static double varied(ServerPlayer player, double value) {
        return value * (0.8D + player.getRandom().nextDouble() * 0.4D);
    }

    private static double varied(ServerPlayer player, double value, int variance) {
        double spread = Mth.clamp(variance, 0, 100) / 100.0D;
        return value * (1.0D - spread + player.getRandom().nextDouble() * spread * 2.0D);
    }

    private static double repeatedValue(ServerPlayer player, double value, int repeats, int variance) {
        double total = 0.0D;
        for (int hit = 0; hit < Math.max(1, repeats); hit++) {
            total += Math.max(0.0D, varied(player, value, variance));
        }
        return total;
    }

    private static int safeDamageInt(double value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, Math.round(value)));
    }

    private static void applyEnemyStateEffects(BSOriginalEnemyData.Action action, Set<Integer> activeStates,
                                               ServerPlayer player) {
        if (action.scope() != 11) {
            return;
        }
        for (BSOriginalEnemyData.StateEffect effect : action.stateEffects()) {
            if (effect.stateId() <= 0 || player.getRandom().nextDouble() >= effect.chance()) {
                continue;
            }
            if (effect.code() == 21) {
                activeStates.add(effect.stateId());
            } else if (effect.code() == 22) {
                activeStates.remove(effect.stateId());
            }
        }
    }

    private static long rewardVictory(ServerPlayer player, BSPlayerStats stats,
                                      Session session) {
        long soulReward = 0L;
        for (EntityTurnBattleMonster enemy : getEnemies(player, session)) {
            soulReward += DifficultyManager.scaleManagedSoulReward(
                    enemy.level(), enemy.getTurnBattleSoulReward());
            for (ItemStack drop : enemy.rollTurnBattleDrops(player.getRandom())) {
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
            }
        }
        stats.souls += soulReward;
        if (player.getRandom().nextInt(5) == 0) {
            ItemStack drop = new ItemStack(BlackSouls.SOUL_WHITE.get());
            if (!player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
        }
        NetworkHandler.sendToPlayer(new PacketSyncStats(stats.serializeNBT()), player);
        return soulReward;
    }

    private static void finish(ServerPlayer player, Session session,
                               ClientboundTurnBattlePacket.Outcome outcome, Component message,
                               boolean removeEnemy, long soulReward) {
        sendState(player, false, message, false, outcome, soulReward);
        EntityTurnBattleMonster rootEnemy = getRootEnemy(player, session);
        for (EntityTurnBattleMonster enemy : getEnemies(player, session)) {
            enemy.setInvulnerable(false);
            enemy.setDeltaMovement(Vec3.ZERO);
            ENEMY_SESSIONS.remove(enemy.getUUID());
            if (removeEnemy || !enemy.getUUID().equals(session.rootEnemyId)) {
                enemy.discard();
            }
        }
        if (!removeEnemy && rootEnemy != null) {
            rootEnemy.setInvisible(false);
            rootEnemy.setSilent(false);
            rootEnemy.teleportTo(session.enemyAnchor.x, session.enemyAnchor.y, session.enemyAnchor.z);
            rootEnemy.setBattleCooldown(ESCAPE_GRACE_TICKS);
        }
        PLAYER_SESSIONS.remove(player.getUUID());
    }

    private static void sendState(ServerPlayer player, boolean active,
                                  Component message, boolean canAct,
                                  ClientboundTurnBattlePacket.Outcome outcome, long soulReward) {
        Session session = PLAYER_SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        List<ClientboundTurnBattlePacket.EnemySnapshot> snapshots = new ArrayList<>();
        for (EntityTurnBattleMonster enemy : getEnemies(player, session)) {
            int profileId = enemy instanceof EntityOriginalDatabaseEnemy originalEnemy
                    ? originalEnemy.getProfileId() : -1;
            List<Integer> states = session.enemyStates.containsKey(enemy.getUUID())
                    ? session.enemyStates.get(enemy.getUUID()).stream().sorted().toList()
                    : List.of();
            snapshots.add(new ClientboundTurnBattlePacket.EnemySnapshot(
                    enemy.getId(), enemy.getDisplayName(),
                    (float) enemy.getTurnBattleHealth(),
                    (float) enemy.getTurnBattleMaxHealth(),
                    profileId, states));
        }
        EntityTurnBattleMonster root = getRootEnemy(player, session);
        int rootEntityId = root == null ? -1 : root.getId();
        NetworkHandler.sendToPlayer(new ClientboundTurnBattlePacket(
                active, rootEntityId, session.battleProfileId, snapshots,
                session.actingEnemyIndex, session.lastEnemyAnimationId,
                session.phaseChanged, session.awaitingPhasePresentation,
                session.playerHits, message, canAct, outcome, soulReward,
                session.skillCooldowns), player);
        session.phaseChanged = false;
        session.playerHits.clear();
    }

    private static EntityTurnBattleMonster getRootEnemy(ServerPlayer player, Session session) {
        if (session == null) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(session.rootEnemyId);
        return entity instanceof EntityTurnBattleMonster enemy ? enemy : null;
    }

    private static List<EntityTurnBattleMonster> getEnemies(ServerPlayer player, Session session) {
        if (session == null) {
            return List.of();
        }
        List<EntityTurnBattleMonster> enemies = new ArrayList<>();
        for (UUID enemyId : session.enemyIds) {
            Entity entity = player.serverLevel().getEntity(enemyId);
            if (entity instanceof EntityTurnBattleMonster enemy) {
                enemies.add(enemy);
            }
        }
        return enemies;
    }

    private static EntityTurnBattleMonster getTargetEnemy(ServerPlayer player,
                                                          Session session, int index) {
        List<EntityTurnBattleMonster> enemies = getEnemies(player, session);
        if (index < 0 || index >= enemies.size()) {
            return null;
        }
        EntityTurnBattleMonster enemy = enemies.get(index);
        return isTargetableEnemy(player, session, enemy) ? enemy : null;
    }

    private static EntityTurnBattleMonster firstAliveEnemy(ServerPlayer player, Session session) {
        return getEnemies(player, session).stream()
                .filter(enemy -> isTargetableEnemy(player, session, enemy))
                .findFirst().orElse(null);
    }

    private static boolean isTargetableEnemy(ServerPlayer player, Session session,
                                             EntityTurnBattleMonster enemy) {
        return !enemy.isTurnBattleDefeated() && !isLockedGranStage(player, session, enemy);
    }

    private static boolean isLockedGranStage(ServerPlayer player, Session session,
                                             EntityTurnBattleMonster enemy) {
        if (session == null || session.battleProfileId != GRAN_STAGE_PROFILE
                || !(enemy instanceof EntityOriginalDatabaseEnemy original)
                || original.getProfileId() < GRAN_STAGE_PROFILE
                || original.getProfileId() > GRAN_FINAL_STAGE_PROFILE) {
            return false;
        }
        return getEnemies(player, session).stream().anyMatch(candidate ->
                candidate instanceof EntityOriginalDatabaseEnemy fragment
                        && fragment.getProfileId() >= GRAN_FRAGMENT_FIRST_PROFILE
                        && fragment.getProfileId() <= GRAN_FRAGMENT_LAST_PROFILE
                        && !fragment.isTurnBattleDefeated());
    }

    private static boolean isDormantGranStage(EntityTurnBattleMonster enemy) {
        return enemy instanceof EntityOriginalDatabaseEnemy original
                && original.getProfileId() >= GRAN_STAGE_PROFILE
                && original.getProfileId() < GRAN_FINAL_STAGE_PROFILE;
    }

    private static void updateGranStage(ServerPlayer player, Session session) {
        if (session == null || session.battleProfileId != GRAN_STAGE_PROFILE) {
            return;
        }
        List<EntityTurnBattleMonster> enemies = getEnemies(player, session);
        int defeatedFragments = 0;
        EntityOriginalDatabaseEnemy stage = null;
        for (EntityTurnBattleMonster enemy : enemies) {
            if (!(enemy instanceof EntityOriginalDatabaseEnemy original)) {
                continue;
            }
            int profileId = original.getProfileId();
            if (profileId >= GRAN_STAGE_PROFILE && profileId <= GRAN_FINAL_STAGE_PROFILE) {
                stage = original;
            } else if (profileId >= GRAN_FRAGMENT_FIRST_PROFILE
                    && profileId <= GRAN_FRAGMENT_LAST_PROFILE
                    && original.isTurnBattleDefeated()) {
                defeatedFragments++;
            }
        }
        int nextProfile = GRAN_STAGE_PROFILE + Math.min(7, defeatedFragments);
        if (stage == null || stage.getProfileId() == nextProfile) {
            return;
        }
        stage.setProfileId(nextProfile);
        stage.setTurnBattleHealth(stage.getTurnBattleMaxHealth());
        session.enemyStates.put(stage.getUUID(), new HashSet<>(
                BSOriginalEnemyData.get(nextProfile).initialStates()));
        session.enemyStates.get(stage.getUUID()).add(10);
        session.enemyStates.get(stage.getUUID()).add(221);
        session.enemyTurns.put(stage.getUUID(), 1);
        session.actingEnemyIndex = Math.max(0, session.enemyIds.indexOf(stage.getUUID()));
    }

    private static int rollPlayerActionCount(ServerPlayer player, BSPlayerStats stats) {
        double actionCount = SkillUtils.getActionCount(player, stats);
        int result = Math.max(1, (int) Math.floor(actionCount));
        double fractional = actionCount - Math.floor(actionCount);
        if (fractional > 0.0D && player.getRandom().nextDouble() < fractional) {
            result++;
        }
        return result;
    }

    private static boolean allEnemiesDefeated(ServerPlayer player, Session session) {
        List<EntityTurnBattleMonster> enemies = getEnemies(player, session);
        return !enemies.isEmpty()
                && enemies.stream().allMatch(EntityTurnBattleMonster::isTurnBattleDefeated);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        Session session = PLAYER_SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (session.awaitingPhasePresentation
                && session.presentationDeadline > 0L
                && player.serverLevel().getGameTime() >= session.presentationDeadline) {
            handlePresentationComplete(player);
            return;
        }
        EntityTurnBattleMonster rootEnemy = getRootEnemy(player, session);
        if (rootEnemy == null || !player.isAlive()) {
            if (rootEnemy != null) {
                finish(player, session, ClientboundTurnBattlePacket.Outcome.DEFEAT,
                        Component.literal(player.getDisplayName().getString() + "倒下了……"), false, 0L);
            } else {
                PLAYER_SESSIONS.remove(player.getUUID());
                session.enemyIds.forEach(ENEMY_SESSIONS::remove);
            }
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        if (player.position().distanceToSqr(session.playerAnchor) > 0.01D) {
            player.teleportTo(session.playerAnchor.x, session.playerAnchor.y, session.playerAnchor.z);
        }
        for (EntityTurnBattleMonster enemy : getEnemies(player, session)) {
            enemy.setDeltaMovement(Vec3.ZERO);
            Vec3 anchor = session.enemyAnchors.getOrDefault(enemy.getUUID(), session.enemyAnchor);
            if (enemy.position().distanceToSqr(anchor) > 0.01D) {
                enemy.teleportTo(anchor.x, anchor.y, anchor.z);
            }
        }
        if ((player.tickCount & 7) == 0) {
            for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class,
                    player.getBoundingBox().inflate(64.0D),
                    candidate -> !isInBattle(candidate) && candidate.getTarget() == player)) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof ServerPlayer player
                && PLAYER_SESSIONS.containsKey(player.getUUID())
                && !isInBattle(event.getEntity())) {
            event.setNewTarget(null);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (INTERNAL_BATTLE_ITEM_DAMAGE.get()) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (BSConfig.COMBAT_MODE.get() == BSConfig.CombatMode.BLACK_SOULS_TURN_BASED
                && event.getEntity() instanceof EntityTurnBattleMonster enemy
                && source instanceof ServerPlayer player) {
            event.setCanceled(true);
            tryStart(player, enemy);
            return;
        }
        if (isInBattle(event.getEntity())
                || source != null && isInBattle(source)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Session session = PLAYER_SESSIONS.remove(event.getEntity().getUUID());
        if (session != null) {
            for (UUID enemyId : session.enemyIds) {
                ENEMY_SESSIONS.remove(enemyId);
                if (!enemyId.equals(session.rootEnemyId)
                        && event.getEntity() instanceof ServerPlayer player) {
                    Entity enemy = player.serverLevel().getEntity(enemyId);
                    if (enemy != null) {
                        enemy.discard();
                    }
                }
            }
        }
    }

    private record ActionResult(Component message, boolean consumeTurn,
                                List<ClientboundTurnBattlePacket.DamageHit> hits) {
        private ActionResult(Component message, boolean consumeTurn) {
            this(message, consumeTurn, List.of());
        }
    }

    private record PhaseAdvance(String message) {
    }

    private static final class Session {
        private final UUID rootEnemyId;
        private final Vec3 playerAnchor;
        private final Vec3 enemyAnchor;
        private final List<UUID> enemyIds = new ArrayList<>();
        private final Map<UUID, Vec3> enemyAnchors = new HashMap<>();
        private final Map<String, Integer> skillCooldowns = new HashMap<>();
        private final Map<UUID, Set<Integer>> enemyStates = new HashMap<>();
        private final Map<UUID, Integer> enemyTurns = new HashMap<>();
        private int battleProfileId;
        private int enemyTurn = 1;
        private int lastEnemyAnimationId = 1;
        private int actingEnemyIndex;
        private boolean phaseChanged;
        private boolean resolving;
        private boolean awaitingPhasePresentation;
        private boolean guardQueued;
        private int remainingPlayerActions;
        private long presentationDeadline;
        private final List<ClientboundTurnBattlePacket.DamageHit> playerHits = new ArrayList<>();

        private Session(UUID rootEnemyId, Vec3 playerAnchor, Vec3 enemyAnchor,
                        int battleProfileId) {
            this.rootEnemyId = rootEnemyId;
            this.playerAnchor = playerAnchor;
            this.enemyAnchor = enemyAnchor;
            this.battleProfileId = battleProfileId;
        }
    }
}

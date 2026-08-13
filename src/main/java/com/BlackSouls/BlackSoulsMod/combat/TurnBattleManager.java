package com.BlackSouls.BlackSoulsMod.combat;

import com.BlackSouls.BlackSoulsMod.BSConfig;
import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalDatabaseEnemy;
import com.BlackSouls.BlackSoulsMod.handler.SceneSpawnerBossHandler;
import com.BlackSouls.BlackSoulsMod.entity.EntityCorpseEatingRabbit;
import com.BlackSouls.BlackSoulsMod.entity.EntityRabbitKnight;
import com.BlackSouls.BlackSoulsMod.entity.RabbitKnightDialogue;
import com.BlackSouls.BlackSoulsMod.entity.EntityTurnBattleMonster;
import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import com.BlackSouls.BlackSoulsMod.handler.SkillEventHandler;
import com.BlackSouls.BlackSoulsMod.handler.StatEventHandler;
import com.BlackSouls.BlackSoulsMod.item.consumables.ItemThrownBladeBase;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundTurnBattlePacket;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketSyncStats;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundTurnBattleActionPacket;
import com.BlackSouls.BlackSoulsMod.party.PartyManager;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalItemData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalBattleProfileData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalEnemyData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalEnemyPhaseData;
import com.BlackSouls.BlackSoulsMod.util.BSAttributeManager;
import com.BlackSouls.BlackSoulsMod.util.DifficultyManager;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import com.BlackSouls.BlackSoulsMod.util.skill.AbstractSkill;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillRegistry;
import com.BlackSouls.BlackSoulsMod.util.skill.WeaponSkill;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
    private static final int EFFECT_TICKS_PER_ACTION = 200;
    private static final int GRAN_STAGE_PROFILE = 570;
    private static final int GRAN_FINAL_STAGE_PROFILE = 577;
    private static final int GRAN_FRAGMENT_FIRST_PROFILE = 580;
    private static final int GRAN_FRAGMENT_LAST_PROFILE = 586;
    private static final Set<String> BREAK_SKILLS = Set.of(
            "bs2_skill_crush", "bs2_skill_weapon_break", "bs2_skill_armor_break"
    );
    private static final Set<Integer> BREAK_IMMUNITY_STATES = Set.of(33, 135, 161);
    private static final Set<Integer> INTERRUPTIBLE_CHARGE_STATES = Set.of(32, 35, 50, 51);
    private static final int STUN_SKIP_ACTION = -1;
    private static final int BREAK_SKIP_ACTION = -2;
    private static final int ATK_DOWN_STATE = 1001;
    private static final int ATK_DOWN_2_STATE = 1002;
    private static final int DEF_DOWN_STATE = 1003;
    private static final int DEF_DOWN_2_STATE = 1004;
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
    private static final Set<String> SINGLE_ALLY_SKILLS = Set.of(
            "bs2_skill_cure", "bs2_skill_resurrection", "bs2_skill_royal_tea",
            "bs2_skill_delicious_turtle_soup"
    );
    private static final Set<String> ALL_ALLY_SKILLS = Set.of(
            "bs2_skill_soul_light", "bs2_skill_full_blessing", "bs2_skill_erase",
            "bs2_skill_kings_command", "bs2_skill_godspeed_dance", "bs2_skill_paladin_banner"
    );
    private static final Map<UUID, Session> PLAYER_SESSIONS = new HashMap<>();
    private static final Map<UUID, UUID> ENEMY_SESSIONS = new HashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL_BATTLE_ITEM_DAMAGE =
            ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> ADVANCING_BATTLE_EFFECTS =
            ThreadLocal.withInitial(() -> false);

    private TurnBattleManager() {
    }

    public static void tryStart(ServerPlayer player, EntityTurnBattleMonster enemy) {
        if (BSConfig.COMBAT_MODE.get() != BSConfig.CombatMode.BLACK_SOULS_TURN_BASED
                || player.isCreative() || player.isSpectator() || !player.isAlive()
                || enemy.isTurnBattleDefeated() || !enemy.canStartTurnBattle()
                || PLAYER_SESSIONS.containsKey(player.getUUID()) || ENEMY_SESSIONS.containsKey(enemy.getUUID())) {
            return;
        }
        int profileId = enemy instanceof EntityOriginalDatabaseEnemy originalEnemy
                ? originalEnemy.getProfileId() : -1;
        Session session = new Session(enemy.getUUID(), player.position(), enemy.position(), profileId);
        List<ServerPlayer> members = PartyManager.onlineMembers(player).stream()
                .filter(member -> member.serverLevel() == player.serverLevel())
                .filter(member -> !PLAYER_SESSIONS.containsKey(member.getUUID()))
                .toList();
        if (members.isEmpty()) members = List.of(player);
        for (ServerPlayer member : members) {
            PLAYER_SESSIONS.put(member.getUUID(), session);
            session.partyMembers.add(member.getUUID());
            session.playerAnchors.put(member.getUUID(), member.position());
            member.fallDistance = 0.0F;
        }
        configureInitialEnemies(player, enemy, session);
        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats ->
                session.remainingPlayerActions = rollPlayerActionCount(player, stats));
        session.openingPending = BSOriginalBattleProfileData.get(profileId).preemptiveSkillId() > 0;
        session.resolving = session.openingPending;
        session.presentationDriver = player.getUUID();
        broadcastState(session, true, battleIntro(session.battleProfileId, enemy),
                !session.openingPending, ClientboundTurnBattlePacket.Outcome.NONE, 0L);
    }

    private static Component battleIntro(int profileId, EntityTurnBattleMonster enemy) {
        List<String> pages = BSOriginalBattleProfileData.get(profileId).introPages();
        return pages.isEmpty()
                ? Component.literal(enemy.getDisplayName().getString() + "出现了！")
                : Component.literal(String.join("\f", pages));
    }

    public static boolean isInBattle(Entity entity) {
        return PLAYER_SESSIONS.containsKey(entity.getUUID()) || ENEMY_SESSIONS.containsKey(entity.getUUID());
    }

    public static boolean isDowned(ServerPlayer player) {
        Session session = PLAYER_SESSIONS.get(player.getUUID());
        return session != null && session.downedMembers.contains(player.getUUID());
    }

    public static boolean shouldFreezeEffectTick(LivingEntity entity) {
        return !entity.level().isClientSide()
                && !ADVANCING_BATTLE_EFFECTS.get() && isInBattle(entity);
    }

    public static void handleAction(ServerPlayer player, ServerboundTurnBattleActionPacket.Action action,
                                    int selection, int targetIndex) {
        Session session = PLAYER_SESSIONS.get(player.getUUID());
        if (session == null) {
            BlackSouls.LOGGER.warn("Turn battle action {} from {} has no active session",
                    action, player.getGameProfile().getName());
            sendBrokenBattleEnd(player, Component.literal("战斗会话已经失效。"));
            return;
        }
        if (session.downedMembers.contains(player.getUUID())) {
            sendState(player, true, Component.literal("你已倒下，正在观战并等待队友复活……"), false,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            return;
        }
        if (session.partyMembers.size() > 1 && !session.executingPartyBatch
                && action != ServerboundTurnBattleActionPacket.Action.WEAPON_CHANGE) {
            if (session.pendingPartyActions.containsKey(player.getUUID())) {
                sendState(player, true, Component.literal("你已经选择了本回合行动，正在等待其他队员……"), false,
                        ClientboundTurnBattlePacket.Outcome.NONE, 0L);
                return;
            }
            session.pendingPartyActions.put(player.getUUID(), new PendingPartyAction(player.getUUID(), action, selection, targetIndex));
            sendState(player, true, Component.literal("行动已选择，正在等待其他队员……"), false,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            if (!allPartyActionsReady(player, session)) return;
            session.executingPartyBatch = true;
            session.remainingPlayerActions = session.pendingPartyActions.size();
            session.partyActionQueue.clear();
            session.partyActionQueue.addAll(session.pendingPartyActions.values());
            session.pendingPartyActions.clear();
            executeNextPartyAction(player.server, session);
            return;
        }
        EntityTurnBattleMonster rootEnemy = getRootEnemy(player, session);
        if (rootEnemy == null) {
            BlackSouls.LOGGER.warn("Turn battle root {} is missing for player {}",
                    session.rootEnemyId, player.getGameProfile().getName());
            finish(player, session, ClientboundTurnBattlePacket.Outcome.ESCAPED,
                    Component.literal("战斗目标已经消失。"), false, 0L);
            return;
        }
        if (session.resolving) {
            BlackSouls.LOGGER.warn(
                    "Turn battle action {} arrived while resolving for {}: opening={}, awaiting={}, enemySequence={}, queue={}",
                    action, player.getGameProfile().getName(), session.openingPending,
                    session.awaitingPhasePresentation, session.enemySequenceActive,
                    session.enemyActionQueue.size());
            if (session.openingPending) {
                handlePresentationComplete(player);
                return;
            }
            if (session.awaitingPhasePresentation) {
                sendState(player, true, Component.literal("战斗行动正在结算……"), false,
                        ClientboundTurnBattlePacket.Outcome.NONE, 0L);
                return;
            }
            if (session.enemySequenceActive) {
                BSPlayerStats pendingStats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
                if (pendingStats == null) {
                    finish(player, session, ClientboundTurnBattlePacket.Outcome.ESCAPED,
                            Component.literal("战斗中断。"), false, 0L);
                } else {
                    presentNextEnemyAction(player, pendingStats, session);
                }
                return;
            }
            session.resolving = false;
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
        if (player.hasEffect(BlackSouls.BUFF_STUN.get())) {
            session.remainingPlayerActions = Math.max(0, session.remainingPlayerActions - 1);
            advanceSkillCooldowns(player, session);
            StatEventHandler.syncToClient(player);
            Component stunned = Component.literal(player.getDisplayName().getString() + "因眩晕无法行动！");
            if (session.executingPartyBatch && session.remainingPlayerActions > 0) {
                broadcastState(session, true, stunned, false,
                        ClientboundTurnBattlePacket.Outcome.NONE, 0L);
                session.resolving = false;
                executeNextPartyAction(player.server, session);
            } else {
                session.executingPartyBatch = false;
                beginEnemySequence(player, stats, session, stunned);
            }
            return;
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
                ActionResult result = performAttack(player, target, stats, session);
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
                        : performItem(player, target, stats, session, selection);
                playerMessage = result.message;
                consumeTurn = result.consumeTurn;
            }
            case GUARD -> {
                TurnBattleDomainData.Domain domain = TurnBattleDomainData.get(session.battleProfileId);
                if (domain != null && domain.guardDisabled()) {
                    session.resolving = false;
                    sendState(player, true, Component.literal("领域封锁了防御。"), true,
                            ClientboundTurnBattlePacket.Outcome.NONE, 0L);
                    return;
                }
                guard = true;
                playerMessage = Component.literal(player.getDisplayName().getString() + "采取了防御姿态！");
            }
            case ESCAPE -> {
                double chance = Mth.clamp(0.50D + (stats.speed - 40.0D) / 200.0D, 0.10D, 0.95D);
                if (player.getRandom().nextDouble() < chance) {
                    if (session.partyMembers.size() > 1 && !PartyManager.isLeader(player)) {
                        session.remainingPlayerActions = Math.max(0, session.remainingPlayerActions - 1);
                        removePartyCombatant(player, session,
                                Component.literal(player.getDisplayName().getString() + "成功逃走了！"));
                        if (session.remainingPlayerActions > 0) {
                            executeNextPartyAction(player.server, session);
                        } else {
                            beginEnemySequenceForParty(player.server, session,
                                    Component.literal(player.getDisplayName().getString() + "成功逃走了！"));
                        }
                    } else {
                        finish(player, session, ClientboundTurnBattlePacket.Outcome.ESCAPED,
                                Component.literal(player.getDisplayName().getString() + "成功逃走了！"), false, 0L);
                    }
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
            if (session.executingPartyBatch) {
                session.executingPartyBatch = false;
                session.partyActionQueue.clear();
                session.pendingPartyActions.clear();
                broadcastStateExcept(session, player.getUUID(), true,
                        Component.literal(player.getDisplayName().getString() + "需要重新选择行动。"),
                        true, ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            }
            return;
        }
        session.remainingPlayerActions = Math.max(0, session.remainingPlayerActions - 1);
        session.guardQueued |= guard;
        updateGranStage(player, session);
        if (canAdvanceEnemyPhase(player, rootEnemy, session)) {
            advanceSkillCooldowns(player, session);
            StatEventHandler.syncToClient(player);
            session.awaitingPhasePresentation = true;
            session.presentationDeadline = player.serverLevel().getGameTime() + 200L;
            session.presentationDriver = player.getUUID();
            sendState(player, true, playerMessage, false,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            broadcastStateExcept(session, player.getUUID(), true, playerMessage, false,
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
            if (session.executingPartyBatch) {
                broadcastState(session, true, playerMessage, false,
                        ClientboundTurnBattlePacket.Outcome.NONE, 0L);
                executeNextPartyAction(player.server, session);
            } else {
                sendState(player, true, playerMessage, true,
                        ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            }
            return;
        }

        advanceSkillCooldowns(player, session);
        StatEventHandler.syncToClient(player);
        if (action == ServerboundTurnBattleActionPacket.Action.ITEM) {
            session.pendingEnemySequenceAfterPlayerAction = true;
            session.pendingEnemySequencePrefix = playerMessage.getString();
            session.awaitingPhasePresentation = true;
            session.presentationDeadline = player.serverLevel().getGameTime() + 200L;
            session.presentationDriver = player.getUUID();
            sendState(player, true, playerMessage, false,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            broadcastStateExcept(session, player.getUUID(), true, playerMessage, false,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            return;
        }
        session.executingPartyBatch = false;
        beginEnemySequence(player, stats, session, playerMessage);
    }

    private static boolean allPartyActionsReady(ServerPlayer player, Session session) {
        for (UUID id : List.copyOf(session.partyMembers)) {
            ServerPlayer member = player.server.getPlayerList().getPlayer(id);
            if (member == null) {
                session.partyMembers.remove(id);
                PLAYER_SESSIONS.remove(id);
                continue;
            }
            if (session.downedMembers.contains(id)) continue;
            if (!session.pendingPartyActions.containsKey(id)) return false;
        }
        return !session.pendingPartyActions.isEmpty();
    }

    private static void executeNextPartyAction(net.minecraft.server.MinecraftServer server, Session session) {
        PendingPartyAction pending = session.partyActionQueue.pollFirst();
        if (pending == null) {
            session.executingPartyBatch = false;
            return;
        }
        ServerPlayer actor = server.getPlayerList().getPlayer(pending.playerId());
        if (actor == null || !session.partyMembers.contains(pending.playerId())
                || session.downedMembers.contains(pending.playerId())) {
            session.remainingPlayerActions = Math.max(0, session.remainingPlayerActions - 1);
            executeNextPartyAction(server, session);
            return;
        }
        session.resolving = false;
        session.presentationDriver = actor.getUUID();
        handleAction(actor, pending.action(), pending.selection(), pending.targetIndex());
    }

    private static void removePartyCombatant(ServerPlayer player, Session session, Component message) {
        session.partyMembers.remove(player.getUUID());
        session.downedMembers.remove(player.getUUID());
        session.playerAnchors.remove(player.getUUID());
        PLAYER_SESSIONS.remove(player.getUUID());
        player.fallDistance = 0.0F;
        player.setDeltaMovement(Vec3.ZERO);
        NetworkHandler.sendToPlayer(new ClientboundTurnBattlePacket(
                false, -1, -1, List.of(), 0, 1, false, false,
                List.of(), List.of(), message, false, ClientboundTurnBattlePacket.Outcome.ESCAPED,
                0L, List.of(), Map.of()), player);
        broadcastState(session, true, message, false, ClientboundTurnBattlePacket.Outcome.NONE, 0L);
    }

    private static void beginEnemySequenceForParty(net.minecraft.server.MinecraftServer server,
                                                   Session session, Component prefix) {
        session.executingPartyBatch = false;
        for (UUID id : session.partyMembers) {
            ServerPlayer member = server.getPlayerList().getPlayer(id);
            if (member == null) continue;
            BSPlayerStats stats = member.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            if (stats != null) {
                beginEnemySequence(member, stats, session, prefix);
                return;
            }
        }
    }

    private static ActionResult performAttack(ServerPlayer player, EntityTurnBattleMonster enemy,
                                              BSPlayerStats stats, Session session) {
        String actor = player.getDisplayName().getString();
        String target = enemy.getDisplayName().getString();
        TurnBattleDomainData.Domain domain = TurnBattleDomainData.get(session.battleProfileId);
        if (domain != null && domain.normalAttackDisabled()) {
            return new ActionResult(Component.literal("领域封锁了通常攻击。"), false);
        }
        if (player.getRandom().nextDouble() >= 0.90D) {
            return new ActionResult(Component.literal(actor + "的攻击！\n但是没有命中" + target + "！"), true);
        }
        double enemyDefense = DifficultyManager.scaleManagedStat(
                enemy.level(), enemy.getTurnBattleDefense()
                        * StatEventHandler.getDefenseShiftMultiplier(enemy));
        int dualSwordAura = getDualSwordAura(player);
        int hitCount = player.getMainHandItem().is(BlackSouls.DIVINE_ANGEL_DUAL_SWORDS.get())
                ? dualSwordAura + 1 : 1;
        List<ClientboundTurnBattlePacket.DamageHit> hits = new ArrayList<>();
        int totalDamage = 0;
        boolean critical = false;
        for (int hit = 0; hit < hitCount; hit++) {
            double multiplier = hit == 0 ? 4.0D : 3.0D;
            double damage = varied(player, Math.max(
                    1.0D, stats.attack * domainAttackRate(session)
                            * multiplier - enemyDefense * 2.0D));
            boolean hitCritical = player.getRandom().nextDouble()
                    < (stats.critRate + stats.bonusCritRate) / 100.0D;
            if (hitCritical) {
                damage *= 3.0D;
                critical = true;
            }
            damage *= turnBattlePhysicalAttributeMultiplier(player, enemy, stats);
            damage *= turnBattleDamageMultiplier(enemy);
            int dealt = Math.max(1, (int) Math.round(damage));
            enemy.setTurnBattleHealth(enemy.getTurnBattleHealth() - dealt);
            totalDamage += dealt;
            hits.add(new ClientboundTurnBattlePacket.DamageHit(
                    enemy.getId(), dealt, hitCritical, hit));
        }
        if (player.getMainHandItem().is(BlackSouls.DIVINE_ANGEL_DUAL_SWORDS.get())) {
            int nextAura = Math.min(7, dualSwordAura + 1);
            player.addEffect(new MobEffectInstance(
                    BlackSouls.BUFF_DUAL_SWORD_AURA.get(), 400, nextAura - 1,
                    false, false, true));
            StatEventHandler.applyStats(player);
            StatEventHandler.syncToClient(player);
        }
        boolean stunned = !enemy.isTurnBattleDefeated()
                && applyTurnBattleWeaponStun(player, enemy, session);
        StringBuilder message = new StringBuilder(actor).append("的攻击！\n");
        if (critical) {
            message.append("会心一击！\n");
        }
        message.append("对").append(target).append("造成了 ").append(totalDamage).append(" 点伤害！");
        if (stunned) {
            message.append("\n").append(target).append("陷入了眩晕！");
        }
        return new ActionResult(Component.literal(message.toString()), true, hits);
    }

    private static int getDualSwordAura(ServerPlayer player) {
        if (!BlackSouls.BUFF_DUAL_SWORD_AURA.isPresent()) {
            return 0;
        }
        MobEffectInstance effect = player.getEffect(BlackSouls.BUFF_DUAL_SWORD_AURA.get());
        return effect == null ? 0 : Math.min(7, effect.getAmplifier() + 1);
    }

    public static boolean skillRequiresTarget(AbstractSkill skill) {
        return skill != null && skill.isUsableInTurnBattle()
                && (skillTargetsSingleAlly(skill) || skill.requiresTurnBattleTarget()
                || (!isNonDamageSkill(skill)
                && !ALL_TARGET_SKILLS.contains(skill.getSkillId())));
    }

    public static boolean skillTargetsSingleAlly(AbstractSkill skill) {
        return skill != null && SINGLE_ALLY_SKILLS.contains(skill.getSkillId());
    }

    public static boolean skillTargetsAllAllies(AbstractSkill skill) {
        return skill != null && ALL_ALLY_SKILLS.contains(skill.getSkillId());
    }

    public static boolean skillTargetsDownedAlly(AbstractSkill skill) {
        return skill != null && "bs2_skill_resurrection".equals(skill.getSkillId());
    }

    public static boolean skillTargetsAll(AbstractSkill skill) {
        return skill != null && ALL_TARGET_SKILLS.contains(skill.getSkillId());
    }

    private static boolean isNonDamageSkill(AbstractSkill skill) {
        return skill != null && (skill.isTurnBattleNonDamage()
                || NON_DAMAGE_SKILLS.contains(skill.getSkillId()));
    }

    private static ActionResult performSkill(ServerPlayer player, BSPlayerStats stats,
                                             Session session, int selection, int targetIndex) {
        if (player.hasEffect(BlackSouls.BUFF_SILENCE.get())) {
            return new ActionResult(Component.literal("沉默状态下无法使用技·魔法。"), false);
        }
        if (selection < 0) {
            return new ActionResult(Component.literal("这个技能当前不可用。"), false);
        }
        AbstractSkill skill = SkillRegistry.SKILLS.values().stream().skip(selection).findFirst().orElse(null);
        if (skill == null || !skill.isUsableInTurnBattle() || !skill.isUnlockedForGUI(player)) {
            return new ActionResult(Component.literal("这个技能当前不可用。"), false);
        }
        TurnBattleDomainData.Domain domain = TurnBattleDomainData.get(session.battleProfileId);
        if (domain != null && domain.skillDisabled()) {
            return new ActionResult(Component.literal("领域封锁了技·魔法。"), false);
        }
        if (domain != null && domain.buffDisabled()
                && isNonDamageSkill(skill)) {
            return new ActionResult(Component.literal("领域封锁了强化。"), false);
        }
        boolean infiniteCooldown = SkillUtils.hasInfiniteCooldownAccessory(player);
        Map<String, Integer> skillCooldowns = skillCooldowns(player, session);
        int remainingCooldown = infiniteCooldown ? 0 : skillCooldowns.getOrDefault(skill.getSkillId(), 0);
        if (remainingCooldown > 0) {
            return new ActionResult(Component.literal("技能正在冷却（CD" + remainingCooldown + "）。"), false);
        }
        if (!skill.canCastInTurnBattle(player, stats)) {
            return new ActionResult(Component.literal("这个技能当前不可用。"), false);
        }
        EntityTurnBattleMonster selectedTarget = null;
        ServerPlayer selectedAlly = null;
        if (skillTargetsSingleAlly(skill)) {
            selectedAlly = getTargetAlly(player, session, targetIndex,
                    skillTargetsDownedAlly(skill));
            if (selectedAlly == null) {
                return new ActionResult(Component.literal(skillTargetsDownedAlly(skill)
                        ? "请选择倒下的友方目标。" : "请选择友方目标。"), false);
            }
        } else if (skillRequiresTarget(skill)) {
            selectedTarget = getTargetEnemy(player, session, targetIndex);
            if (selectedTarget == null) {
                return new ActionResult(Component.literal("请选择技能目标。"), false);
            }
        }
        skill.consumeForTurnBattle(player, stats);
        int cooldownRounds = skill.getTurnCooldownRounds();
        if (!infiniteCooldown && cooldownRounds > 0) {
            skillCooldowns.put(skill.getSkillId(), cooldownRounds);
        }
        String skillName = Component.translatable(skill.getTranslationKey()).getString();
        String actor = player.getDisplayName().getString();
        if ("bs2_skill_resurrection".equals(skill.getSkillId())) {
            ServerPlayer revived = selectedAlly;
            if (revived != null) {
                int revivedHealth = Math.max(1, Math.round(revived.getMaxHealth() * 0.5F));
                revived.setHealth(revivedHealth);
                revived.invulnerableTime = 0;
                revived.hurtMarked = true;
                revived.addEffect(new MobEffectInstance(BlackSouls.BUFF_REQUIEM.get(), 200, 0));
                session.downedMembers.remove(revived.getUUID());
                StatEventHandler.syncToClient(revived);
                PartyManager.refresh(revived);
                return new ActionResult(Component.literal(actor + "使用了" + skillName + "！\n"
                        + revived.getDisplayName().getString() + "复活了！"), true);
            }
        }
        if (isNonDamageSkill(skill)) {
            Map<UUID, Double> healthBeforeEffect = new HashMap<>();
            for (EntityTurnBattleMonster enemy : getEnemies(player, session)) {
                healthBeforeEffect.put(enemy.getUUID(), enemy.getTurnBattleHealth());
            }
            if (skillTargetsAllAllies(skill)) {
                for (ServerPlayer ally : activePartyMembers(player, session)) {
                    skill.executeInTurnBattle(player, stats, ally);
                }
            } else {
                skill.executeInTurnBattle(player, stats,
                        selectedAlly == null ? selectedTarget : selectedAlly);
            }
            boolean interrupted = applyTurnBattleSkillEffects(skill, selectedTarget, session);
            for (EntityTurnBattleMonster enemy : getEnemies(player, session)) {
                Double health = healthBeforeEffect.get(enemy.getUUID());
                if (health != null) {
                    enemy.setTurnBattleHealth(health);
                }
            }
            StatEventHandler.syncToClient(player);
            return new ActionResult(Component.literal(actor + "使用了" + skillName + "！"
                    + interruptedText(selectedTarget, interrupted)), true);
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
                                * StatEventHandler.getDefenseShiftMultiplier(enemy)
                                : enemy.getTurnBattleMagicDefense());
                double baseDamage = skill instanceof WeaponSkill
                        ? stats.attack * domainAttackRate(session) * 4.0D - enemyDefense * 2.0D
                        : stats.magicAttack * domainMagicRate(session) * 5.0D - enemyDefense * 2.0D;
                double damage = varied(player, Math.max(1.0D, baseDamage));
                if (skill instanceof WeaponSkill) {
                    damage *= turnBattlePhysicalAttributeMultiplier(player, enemy, stats);
                }
                damage *= turnBattleDamageMultiplier(enemy);
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
            boolean interrupted = applyTurnBattleSkillEffects(skill, enemy, session);
            message.append(interruptedText(enemy, interrupted));
        }
        StatEventHandler.syncToClient(player);
        return new ActionResult(Component.literal(message.toString()), true, hits);
    }

    private static boolean applyTurnBattleSkillEffects(AbstractSkill skill,
                                                       EntityTurnBattleMonster target,
                                                       Session session) {
        if (skill == null || target == null || !BREAK_SKILLS.contains(skill.getSkillId())) {
            return false;
        }
        Set<Integer> states = session.enemyStates.computeIfAbsent(
                target.getUUID(), ignored -> new HashSet<>());
        if ("bs2_skill_weapon_break".equals(skill.getSkillId())) {
            StatEventHandler.applyAttackDown(target, 1000);
        } else if ("bs2_skill_armor_break".equals(skill.getSkillId())) {
            StatEventHandler.applyDefenseDown(target, 1000);
        }
        if (states.stream().anyMatch(BREAK_IMMUNITY_STATES::contains)) {
            return false;
        }
        boolean interrupted = states.removeIf(INTERRUPTIBLE_CHARGE_STATES::contains);
        if (interrupted) {
            target.addEffect(new MobEffectInstance(BlackSouls.BUFF_DEFENSELESS.get(), 400, 0));
            states.add(8);
            session.enemyBreakRounds.put(target.getUUID(), 2);
            session.enemyActionSkips.put(target.getUUID(),
                    new EnemyActionSkip(BREAK_SKIP_ACTION, 2));
        }
        return interrupted;
    }

    private static boolean applyTurnBattleWeaponStun(ServerPlayer player,
                                                     EntityTurnBattleMonster target,
                                                     Session session) {
        boolean wasStunned = target.hasEffect(BlackSouls.BUFF_STUN.get());
        StatEventHandler.applyPlayerOnHitStatusEffects(player, target);
        if (!target.hasEffect(BlackSouls.BUFF_STUN.get())) {
            return false;
        }
        Set<Integer> states = session.enemyStates.computeIfAbsent(
                target.getUUID(), ignored -> new HashSet<>());
        if (states.contains(25)) {
            target.removeEffect(BlackSouls.BUFF_STUN.get());
            return false;
        }
        states.add(13);
        session.enemyActionSkips.put(target.getUUID(),
                new EnemyActionSkip(STUN_SKIP_ACTION, 1));
        return !wasStunned;
    }

    private static String interruptedText(EntityTurnBattleMonster target, boolean interrupted) {
        return interrupted && target != null
                ? "\n" + target.getDisplayName().getString() + "的蓄力被打断了！"
                : "";
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
        if (session == null || rootEnemy == null || !session.resolving) {
            return;
        }
        if (session.presentationDriver != null && !session.presentationDriver.equals(player.getUUID())) {
            return;
        }
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) {
            finish(player, session, ClientboundTurnBattlePacket.Outcome.ESCAPED,
                    Component.literal("战斗中断。"), false, 0L);
            return;
        }
        if (session.openingPending) {
            beginOpeningSequence(player, stats, session);
            return;
        }
        if (session.counterVictoryPending && session.awaitingPhasePresentation) {
            session.counterVictoryPending = false;
            session.awaitingPhasePresentation = false;
            session.presentationDeadline = 0L;
            long reward = rewardVictory(player, stats, session);
            finish(player, session, ClientboundTurnBattlePacket.Outcome.VICTORY,
                    Component.literal("敌群被打倒了！"), true, reward);
            return;
        }
        if (session.pendingEnemySequenceAfterPlayerAction && session.awaitingPhasePresentation) {
            String prefix = session.pendingEnemySequencePrefix;
            session.pendingEnemySequenceAfterPlayerAction = false;
            session.pendingEnemySequencePrefix = "";
            session.awaitingPhasePresentation = false;
            session.presentationDeadline = 0L;
            beginEnemySequence(player, stats, session, Component.literal(prefix));
            return;
        }
        if (session.enemySequenceActive && session.awaitingPhasePresentation) {
            clearPendingEnemyStun(player, session);
            session.awaitingPhasePresentation = false;
            session.presentationDeadline = 0L;
            presentNextEnemyAction(player, stats, session);
            return;
        }
        if (!session.awaitingPhasePresentation) {
            return;
        }
        session.awaitingPhasePresentation = false;
        session.presentationDeadline = 0L;
        PhaseAdvance phaseAdvance = advanceEnemyPhase(player, rootEnemy, session);
        if (phaseAdvance == null) {
            session.resolving = false;
            if (session.executingPartyBatch && !session.partyActionQueue.isEmpty()) {
                executeNextPartyAction(player.server, session);
            } else {
                broadcastState(session, true, Component.empty(), true,
                        ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            }
            return;
        }
        session.phaseChanged = true;
        session.remainingPlayerActions = rollPlayerActionCount(player, stats);
        session.guardQueued = false;
        StatEventHandler.syncToClient(player);
        if (session.executingPartyBatch && !session.partyActionQueue.isEmpty()) {
            session.resolving = false;
            executeNextPartyAction(player.server, session);
            return;
        }
        if (phaseAdvance.fromProfileId() == 185 && phaseAdvance.toProfileId() == 184) {
            EntityTurnBattleMonster skullBeast = getEnemies(player, session).stream()
                    .filter(enemy -> enemy instanceof EntityOriginalDatabaseEnemy original
                            && original.getProfileId() == 184)
                    .findFirst().orElse(null);
            if (skullBeast != null) {
                beginForcedEnemyAction(player, stats, session, skullBeast, 52,
                        phaseAdvance.message());
                return;
            }
        }
        session.resolving = false;
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
        return new PhaseAdvance(message, transition.from(), transition.to());
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
        session.enemyActionSkips.clear();
        session.enemyBreakRounds.clear();
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

    private static Map<String, Integer> skillCooldowns(ServerPlayer player, Session session) {
        return session.playerSkillCooldowns.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
    }

    private static void advanceSkillCooldowns(ServerPlayer player, Session session) {
        Map<String, Integer> cooldowns = skillCooldowns(player, session);
        cooldowns.replaceAll((skillId, rounds) -> Math.max(0, rounds - 1));
        cooldowns.values().removeIf(rounds -> rounds <= 0);
    }

    private static ActionResult performItem(ServerPlayer player, EntityTurnBattleMonster enemy,
                                            BSPlayerStats stats, Session session, int slot) {
        TurnBattleDomainData.Domain domain = TurnBattleDomainData.get(session.battleProfileId);
        if (domain != null && domain.itemDisabled()) {
            return new ActionResult(Component.literal("领域封锁了道具使用。"), false);
        }
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

    private static void beginEnemySequence(ServerPlayer player, BSPlayerStats stats,
                                           Session session, Component prefix) {
        session.enemyActionQueue.clear();
        session.roundEffectSnapshots.clear();
        session.roundEffectSnapshots.putAll(captureBattleEffects(player));
        session.enemySequenceCountsAsRound = true;
        List<EntityTurnBattleMonster> enemies = getEnemies(player, session);
        for (EntityTurnBattleMonster enemy : enemies) {
            if (enemy.isTurnBattleDefeated() || isDormantGranStage(enemy)) {
                continue;
            }
            EnemyActionSkip skip = session.enemyActionSkips.get(enemy.getUUID());
            if (skip != null) {
                if (skip.remainingRounds() <= 1) {
                    session.enemyActionSkips.remove(enemy.getUUID());
                } else {
                    session.enemyActionSkips.put(enemy.getUUID(),
                            new EnemyActionSkip(skip.actionId(), skip.remainingRounds() - 1));
                }
                session.enemyActionQueue.addLast(new PendingEnemyAction(
                        enemy.getUUID(), skip.actionId(), true));
                advanceEnemyBreakRound(enemy, session);
                continue;
            }
            int actionCount = enemy instanceof EntityOriginalDatabaseEnemy originalEnemy
                    ? originalEnemy.getProfile().actionCount() : 1;
            if (session.battleProfileId == 567) {
                actionCount += Math.min(4, Math.max(0, session.enemyTurn - 1));
            }
            for (int actionIndex = 0; actionIndex < actionCount; actionIndex++) {
                session.enemyActionQueue.addLast(new PendingEnemyAction(
                        enemy.getUUID(), 0, actionIndex == actionCount - 1));
            }
            advanceEnemyBreakRound(enemy, session);
        }
        session.enemySequencePrefix = prefix.getString();
        session.enemySequenceGuard = session.guardQueued;
        session.guardQueued = false;
        session.enemySequenceActive = true;
        presentNextEnemyAction(player, stats, session);
    }

    private static void beginForcedEnemyAction(ServerPlayer player, BSPlayerStats stats,
                                               Session session, EntityTurnBattleMonster enemy,
                                               int skillId, String prefix) {
        session.enemyActionQueue.clear();
        session.roundEffectSnapshots.clear();
        session.enemySequenceCountsAsRound = false;
        session.enemyActionQueue.addLast(new PendingEnemyAction(enemy.getUUID(), skillId, false));
        session.enemySequencePrefix = prefix == null ? "" : prefix;
        session.enemySequenceGuard = false;
        session.enemySequenceActive = true;
        presentNextEnemyAction(player, stats, session);
    }

    private static void advanceEnemyBreakRound(EntityTurnBattleMonster enemy, Session session) {
        Integer rounds = session.enemyBreakRounds.get(enemy.getUUID());
        if (rounds == null) {
            return;
        }
        if (rounds > 1) {
            session.enemyBreakRounds.put(enemy.getUUID(), rounds - 1);
            return;
        }
        session.enemyBreakRounds.remove(enemy.getUUID());
        session.enemyStates.computeIfAbsent(enemy.getUUID(), ignored -> new HashSet<>()).remove(8);
        enemy.removeEffect(BlackSouls.BUFF_DEFENSELESS.get());
    }

    private static void beginOpeningSequence(ServerPlayer player, BSPlayerStats stats,
                                             Session session) {
        int skillId = BSOriginalBattleProfileData.get(session.battleProfileId).preemptiveSkillId();
        EntityTurnBattleMonster enemy = getEnemies(player, session).stream()
                .filter(candidate -> !candidate.isTurnBattleDefeated() && !isDormantGranStage(candidate))
                .findFirst().orElse(null);
        session.openingPending = false;
        if (skillId <= 0 || enemy == null) {
            session.resolving = false;
            sendState(player, true, Component.empty(), true,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            return;
        }
        session.enemyActionQueue.clear();
        session.roundEffectSnapshots.clear();
        session.enemySequenceCountsAsRound = false;
        session.enemyActionQueue.addLast(new PendingEnemyAction(enemy.getUUID(), skillId, false));
        session.enemySequencePrefix = "";
        session.enemySequenceGuard = false;
        session.enemySequenceActive = true;
        presentNextEnemyAction(player, stats, session);
    }

    private static void presentNextEnemyAction(ServerPlayer player, BSPlayerStats stats,
                                               Session session) {
        ServerPlayer targetPlayer = chooseEnemyTarget(player, session);
        if (targetPlayer == null) {
            defeatParty(player, session, Component.literal("队伍已经无法继续战斗。"));
            return;
        }
        player = targetPlayer;
        stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) {
            removeDefeatedCombatant(player, session);
            executeNextEnemyActionForParty(session);
            return;
        }
        session.presentationDriver = player.getUUID();
        while (!session.enemyActionQueue.isEmpty()) {
            PendingEnemyAction pending = session.enemyActionQueue.removeFirst();
            Entity entity = player.serverLevel().getEntity(pending.enemyId());
            if (!(entity instanceof EntityTurnBattleMonster battleEnemy)
                    || battleEnemy.isTurnBattleDefeated() || isDormantGranStage(battleEnemy)) {
                continue;
            }
            session.actingEnemyIndex = Math.max(0, session.enemyIds.indexOf(battleEnemy.getUUID()));
            int playerHitCountBefore = session.playerHits.size();
            EnemyActionResult result = performEnemyAction(player, battleEnemy, stats,
                    session.enemySequenceGuard, session, pending.skillId());
            boolean countered = session.playerHits.size() > playerHitCountBefore;
            boolean counterPhase = false;
            if (countered) {
                updateGranStage(player, session);
                EntityTurnBattleMonster rootEnemy = getRootEnemy(player, session);
                counterPhase = rootEnemy != null && canAdvanceEnemyPhase(player, rootEnemy, session);
            }
            if (countered && (counterPhase || allEnemiesDefeated(player, session))) {
                session.enemyActionQueue.clear();
                session.enemySequenceActive = false;
                session.enemySequenceCountsAsRound = false;
                session.roundEffectSnapshots.clear();
                session.counterVictoryPending = !counterPhase && allEnemiesDefeated(player, session);
            } else if (result.followUpSkillId() > 0) {
                session.enemyActionQueue.addFirst(new PendingEnemyAction(
                        pending.enemyId(), result.followUpSkillId(), pending.advanceTurnAfter()));
            } else if (pending.advanceTurnAfter()) {
                int turn = session.enemyTurns.getOrDefault(pending.enemyId(), 1);
                session.enemyTurns.put(pending.enemyId(), turn + 1);
            }
            String text = result.message().getString();
            if (!session.enemySequencePrefix.isEmpty()) {
                text = text.isEmpty() ? session.enemySequencePrefix
                        : session.enemySequencePrefix + "\n" + text;
                session.enemySequencePrefix = "";
            }
            StatEventHandler.syncToClient(player);
            if (player.getHealth() <= 0.0F || !player.isAlive()) {
                if (session.partyMembers.size() <= 1) {
                    finish(player, session, ClientboundTurnBattlePacket.Outcome.DEFEAT,
                            Component.literal(text), false, 0L);
                    return;
                }
                stabilizeDownedPlayer(player, session);
            }
            session.awaitingPhasePresentation = true;
            session.presentationDeadline = player.serverLevel().getGameTime() + 200L;
            sendState(player, true, Component.literal(text), false,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            broadcastStateExcept(session, player.getUUID(), true, Component.literal(text), false,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            return;
        }
        session.enemySequenceActive = false;
        session.awaitingPhasePresentation = false;
        session.presentationDeadline = 0L;
        if (session.enemySequenceCountsAsRound) {
            session.enemyTurn++;
            restoreTurnBattleHealth(player, stats);
            restoreTurnBattleMana(stats);
            advanceBattleEffects(player, session.roundEffectSnapshots);
        }
        session.enemySequenceCountsAsRound = false;
        session.roundEffectSnapshots.clear();
        if (player.getHealth() <= 0.0F || !player.isAlive()) {
            Component down = Component.literal("状态效果使" + player.getDisplayName().getString() + "倒下了！");
            if (session.partyMembers.size() <= 1) {
                finish(player, session, ClientboundTurnBattlePacket.Outcome.DEFEAT, down, false, 0L);
                return;
            }
            stabilizeDownedPlayer(player, session);
            if (chooseEnemyTarget(player, session) == null) {
                defeatParty(player, session, Component.literal("队伍已经无法继续战斗。"));
                return;
            }
        }
        session.remainingPlayerActions = rollPlayerActionCount(player, stats);
        session.pendingPartyActions.clear();
        session.partyActionQueue.clear();
        session.presentationDriver = null;
        StatEventHandler.syncToClient(player);
        session.resolving = false;
        broadcastState(session, true, Component.empty(), true,
                ClientboundTurnBattlePacket.Outcome.NONE, 0L);
    }

    private static ServerPlayer chooseEnemyTarget(ServerPlayer fallback, Session session) {
        List<ServerPlayer> alive = new ArrayList<>();
        for (UUID id : session.partyMembers) {
            ServerPlayer member = fallback.server.getPlayerList().getPlayer(id);
            if (member != null && member.isAlive() && member.getHealth() > 0.0F
                    && !session.downedMembers.contains(id)) alive.add(member);
        }
        if (alive.isEmpty()) return null;
        return alive.get(fallback.getRandom().nextInt(alive.size()));
    }

    private static void removeDefeatedCombatant(ServerPlayer player, Session session) {
        session.partyMembers.remove(player.getUUID());
        session.downedMembers.remove(player.getUUID());
        session.playerAnchors.remove(player.getUUID());
        PLAYER_SESSIONS.remove(player.getUUID());
    }

    private static void executeNextEnemyActionForParty(Session session) {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || session.partyMembers.isEmpty()) return;
        ServerPlayer member = server.getPlayerList().getPlayer(session.partyMembers.iterator().next());
        if (member == null) return;
        BSPlayerStats stats = member.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats != null) presentNextEnemyAction(member, stats, session);
    }

    private static EnemyActionResult performEnemyAction(ServerPlayer player,
                                                        EntityTurnBattleMonster battleEnemy,
                                                        BSPlayerStats stats, boolean guard,
                                                        Session session, int forcedSkillId) {
        String enemy = battleEnemy.getDisplayName().getString();
        Set<Integer> activeStates = session.enemyStates.computeIfAbsent(
                battleEnemy.getUUID(), ignored -> new HashSet<>());
        if (forcedSkillId == STUN_SKIP_ACTION) {
            session.pendingEnemyStunClear = battleEnemy.getUUID();
            session.lastEnemyAnimationId = 0;
            return new EnemyActionResult(Component.literal(
                    battleEnemy.getDisplayName().getString() + "因眩晕无法行动！"), 0);
        }
        if (forcedSkillId == BREAK_SKIP_ACTION) {
            session.lastEnemyAnimationId = 0;
            return new EnemyActionResult(Component.literal(
                    battleEnemy.getDisplayName().getString() + "因被击溃而无法行动！"), 0);
        }
        TurnBattleDomainData.Domain domain = TurnBattleDomainData.get(session.battleProfileId);
        double defenseRate = domainDefenseRate(session);
        double magicDefenseRate = domainMagicDefenseRate(session);
        double effectiveEvasion = domain != null && session.battleProfileId == 570
                ? 0.0D : stats.evasion;
        int turn = session.enemyTurns.getOrDefault(battleEnemy.getUUID(), 1);
        BSOriginalEnemyData.Action action = null;
        EntityOriginalDatabaseEnemy originalEnemy = battleEnemy instanceof EntityOriginalDatabaseEnemy original
                ? original : null;
        if (originalEnemy != null) {
            action = forcedSkillId > 0
                    ? originalEnemy.getProfile().findAction(forcedSkillId)
                    : originalEnemy.selectTurnBattleAction(player.getRandom(), turn, activeStates);
        }
        if (action == null) {
            session.lastEnemyAnimationId = battleEnemy.getTurnBattleAttackAnimationId();
            String attackText = battleEnemy.getTurnBattleAttackText();
            if (player.getRandom().nextDouble() >= Mth.clamp(
                    0.95D - effectiveEvasion / 100.0D, 0.05D, 0.99D)) {
                return new EnemyActionResult(
                        Component.literal(enemy + attackText + "！\n但是攻击落空了！"), 0);
            }
            double enemyAttack = DifficultyManager.scaleManagedStat(
                    battleEnemy.level(), battleEnemy.getTurnBattleAttack()
                            * StatEventHandler.getAttackShiftMultiplier(battleEnemy));
            boolean anyCritical = false;
            boolean counterCritical = false;
            int counterDamage = 0;
            int counterWave = session.playerHits.size();
            int repeats = Math.max(1, battleEnemy.getTurnBattleAttackRepeats());
            for (int hit = 0; hit < repeats && player.isAlive(); hit++) {
                double damage = varied(player, Math.max(
                        1.0D, enemyAttack * 4.0D - stats.defense * defenseRate * 2.0D));
                boolean critical = player.getRandom().nextDouble() < 0.05D;
                if (critical) {
                    damage *= 3.0D;
                    anyCritical = true;
                }
                if (guard) {
                    damage *= 0.5D;
                }
                damage *= Math.max(0.0D, stats.physicalDamageRate);
                applyIncomingHit(session, player, safeDamageInt(damage), critical);
                CounterHit counter = tryTurnBattleCounter(player, battleEnemy, stats, session, counterWave);
                if (counter != null) {
                    counterDamage += counter.damage();
                    counterCritical |= counter.critical();
                    counterWave++;
                    if (battleEnemy.isTurnBattleDefeated()) {
                        break;
                    }
                }
            }
            return new EnemyActionResult(Component.literal(enemy + attackText + "！"
                    + (anyCritical ? "\n会心一击！" : "")
                    + counterText(player, battleEnemy, counterDamage, counterCritical)), 0);
        }

        session.lastEnemyAnimationId = action.animationId();
        String actionHeader = enemy + action.text() + "！";
        applyEnemySelfStateEffects(action, activeStates, player);
        int conditionState = originalEnemy == null
                ? 0 : originalEnemy.resolveTurnBattleActionConditionState(action);
        if (conditionState > 0) {
            activeStates.remove(conditionState);
        }

        double difficulty = DifficultyManager.getCurrentTotalMultiplierForLevel(battleEnemy.level());
        boolean roldEffect = activeStates.contains(164);
        double roldMultiplier = roldEffect ? 2.0D : 1.0D;
        TurnBattleFormulaEvaluator.Context context = new TurnBattleFormulaEvaluator.Context(
                battleEnemy.getTurnBattleAttack() * difficulty * roldMultiplier
                        * StatEventHandler.getAttackShiftMultiplier(battleEnemy),
                battleEnemy.getTurnBattleDefense() * difficulty
                        * StatEventHandler.getDefenseShiftMultiplier(battleEnemy),
                battleEnemy.getTurnBattleMagicAttack() * difficulty * roldMultiplier,
                battleEnemy.getTurnBattleMagicDefense() * difficulty,
                battleEnemy.getTurnBattleAgility() * difficulty,
                battleEnemy.getTurnBattleLuck() * difficulty,
                battleEnemy.getTurnBattleHealth(),
                battleEnemy.getTurnBattleMaxHealth(),
                battleEnemy.getTurnBattleMana(),
                battleEnemy.getTurnBattleMaxMana(),
                stats.attack,
                stats.defense * defenseRate,
                stats.magicAttack,
                stats.magicDefense * magicDefenseRate,
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
            return new EnemyActionResult(Component.literal(
                    actionHeader + "\n" + enemy + "恢复了 " + restored + " 点生命！"),
                    action.followUpSkillId());
        }
        if (action.damageType() == 4) {
            applyEnemyTargetStateEffects(action, player, 1);
            CounterHit counter = action.scope() == 11 || action.hitType() != 1
                    ? null : tryTurnBattleCounter(player, battleEnemy, stats, session, 0);
            if (counter != null) {
                return new EnemyActionResult(Component.literal(actionHeader
                        + counterText(player, battleEnemy, counter.damage(), counter.critical())),
                        battleEnemy.isTurnBattleDefeated() ? 0 : action.followUpSkillId());
            }
            return new EnemyActionResult(Component.literal(actionHeader), action.followUpSkillId());
        }
        if (action.damageType() != 1 && action.damageType() != 2
                && action.damageType() != 5 && action.damageType() != 6) {
            applyEnemyTargetStateEffects(action, player, 1);
            CounterHit counter = action.scope() == 11 || action.hitType() != 1
                    ? null : tryTurnBattleCounter(player, battleEnemy, stats, session, 0);
            if (counter != null) {
                return new EnemyActionResult(Component.literal(actionHeader
                        + counterText(player, battleEnemy, counter.damage(), counter.critical())),
                        battleEnemy.isTurnBattleDefeated() ? 0 : action.followUpSkillId());
            }
            return new EnemyActionResult(Component.literal(actionHeader), action.followUpSkillId());
        }

        int landedHits = 0;
        List<HitValue> hitValues = new ArrayList<>();
        int targetRepeats = action.scope() >= 3 && action.scope() <= 6
                ? action.scope() - 2 : 1;
        int repeats = Math.max(1, action.repeats()) * targetRepeats;
        for (int hit = 0; hit < repeats; hit++) {
            double actionEvasion = action.hitType() == 1 ? effectiveEvasion
                    : action.hitType() == 2 ? stats.magicEvasion : 0.0D;
            double hitChance = roldEffect && action.hitType() == 1 ? 1.0D
                    : Mth.clamp(action.successRate() / 100.0D
                    - actionEvasion / 100.0D, 0.0D, 1.0D);
            if (player.getRandom().nextDouble() >= hitChance) {
                continue;
            }
            landedHits++;
            double value = varied(player, baseValue, action.variance());
            boolean hitCritical = action.critical()
                    && (roldEffect || player.getRandom().nextDouble() < 0.05D);
            if (hitCritical) {
                value *= 3.0D;
            }
            if (guard && (action.damageType() == 1 || action.damageType() == 5)) {
                value *= 0.5D;
            }
            value = Math.max(0.0D, value);
            hitValues.add(new HitValue(value, hitCritical));
        }
        if (landedHits == 0) {
            return new EnemyActionResult(Component.literal(
                    actionHeader + "\n但是攻击落空了！"), action.followUpSkillId());
        }
        double hpRateDamage = originalHpRateDamage(action, player, stats);
        if (hpRateDamage > 0.0D) {
            for (int index = 0; index < hitValues.size(); index++) {
                HitValue hit = hitValues.get(index);
                hitValues.set(index, new HitValue(hit.damage() + hpRateDamage, hit.critical()));
            }
        }
        int counterDamage = 0;
        boolean counterCritical = false;
        if (action.damageType() == 2 || action.damageType() == 6) {
            int lostMana = 0;
            for (HitValue hit : hitValues) {
                int hitMana = Math.min(safeDamageInt(hit.damage()), safeDamageInt(stats.mp));
                stats.mp = Math.max(0.0D, stats.mp - hitMana);
                lostMana += hitMana;
                applyEnemyTargetStateEffects(action, player, 1);
                CounterHit counter = action.hitType() == 1
                        ? tryTurnBattleCounter(player, battleEnemy, stats, session, session.playerHits.size())
                        : null;
                if (counter != null) {
                    counterDamage += counter.damage();
                    counterCritical |= counter.critical();
                    if (battleEnemy.isTurnBattleDefeated()) {
                        break;
                    }
                }
            }
            int followUpSkillId = battleEnemy.isTurnBattleDefeated() ? 0 : action.followUpSkillId();
            return new EnemyActionResult(Component.literal(actionHeader + "\n"
                    + player.getDisplayName().getString() + "失去了 " + lostMana + " MP！"
                    + counterText(player, battleEnemy, counterDamage, counterCritical)), followUpSkillId);
        }

        boolean magicLike = action.hitType() == 2
                || action.formula().contains("a.mat") && !action.formula().contains("a.atk");
        double damageRate = Math.max(0.0D,
                magicLike ? stats.magicDamageRate : stats.physicalDamageRate);
        boolean processedCritical = false;
        for (HitValue hit : hitValues) {
            int hitDamage = safeDamageInt(hit.damage() * damageRate);
            processedCritical |= hit.critical();
            applyIncomingHit(session, player, hitDamage, hit.critical());
            if (action.damageType() == 5 && hitDamage > 0) {
                battleEnemy.setTurnBattleHealth(battleEnemy.getTurnBattleHealth() + hitDamage);
            }
            applyEnemyTargetStateEffects(action, player, 1);
            CounterHit counter = action.hitType() == 1
                    ? tryTurnBattleCounter(player, battleEnemy, stats, session, session.playerHits.size())
                    : null;
            if (counter != null) {
                counterDamage += counter.damage();
                counterCritical |= counter.critical();
            }
            if (!player.isAlive()) {
                break;
            }
            if (battleEnemy.isTurnBattleDefeated()) {
                break;
            }
        }
        int followUpSkillId = battleEnemy.isTurnBattleDefeated() ? 0 : action.followUpSkillId();
        return new EnemyActionResult(Component.literal(actionHeader
                + (processedCritical ? "\n会心一击！" : "")
                + counterText(player, battleEnemy, counterDamage, counterCritical)), followUpSkillId);
    }

    private static CounterHit tryTurnBattleCounter(ServerPlayer player,
                                                   EntityTurnBattleMonster enemy,
                                                   BSPlayerStats stats, Session session,
                                                   int wave) {
        if (!player.isAlive()) {
            return null;
        }
        double counterRate = StatEventHandler.getWeaponCounterRate(player);
        if (counterRate <= 0.0D
                || counterRate < 100.0D && player.getRandom().nextDouble() * 100.0D >= counterRate) {
            return null;
        }
        double enemyDefense = DifficultyManager.scaleManagedStat(
                enemy.level(), enemy.getTurnBattleDefense()
                        * StatEventHandler.getDefenseShiftMultiplier(enemy));
        double damage = varied(player, Math.max(1.0D,
                stats.attack * domainAttackRate(session) * 4.0D - enemyDefense * 2.0D));
        boolean critical = player.getRandom().nextDouble()
                < (stats.critRate + stats.bonusCritRate) / 100.0D;
        if (critical) {
            damage *= 3.0D;
        }
        damage *= turnBattlePhysicalAttributeMultiplier(player, enemy, stats);
        damage *= turnBattleDamageMultiplier(enemy);
        int dealt = Math.max(1, safeDamageInt(damage));
        enemy.setTurnBattleHealth(enemy.getTurnBattleHealth() - dealt);
        session.playerHits.add(new ClientboundTurnBattlePacket.DamageHit(
                enemy.getId(), dealt, critical, wave));
        return new CounterHit(dealt, critical);
    }

    private static double turnBattleDamageMultiplier(LivingEntity target) {
        MobEffectInstance defenseless = target.getEffect(BlackSouls.BUFF_DEFENSELESS.get());
        return defenseless == null ? 1.0D : defenseless.getAmplifier() > 0 ? 3.0D : 2.0D;
    }

    private static double turnBattlePhysicalAttributeMultiplier(
            ServerPlayer player, LivingEntity target, BSPlayerStats stats) {
        return BSAttributeManager.getBestMultiplier(
                target, StatEventHandler.buildPlayerAttackAttributes(player, stats));
    }

    private static void clearPendingEnemyStun(ServerPlayer player, Session session) {
        UUID enemyId = session.pendingEnemyStunClear;
        if (enemyId == null) {
            return;
        }
        session.pendingEnemyStunClear = null;
        session.enemyStates.computeIfAbsent(enemyId, ignored -> new HashSet<>()).remove(13);
        Entity entity = player.serverLevel().getEntity(enemyId);
        if (entity instanceof LivingEntity living) {
            living.removeEffect(BlackSouls.BUFF_STUN.get());
        }
    }

    private static String counterText(ServerPlayer player, EntityTurnBattleMonster enemy,
                                      int damage, boolean critical) {
        if (damage <= 0) {
            return "";
        }
        return "\n" + player.getDisplayName().getString() + "的反击！"
                + (critical ? "\n会心一击！" : "")
                + "\n对" + enemy.getDisplayName().getString() + "造成了 " + damage + " 点伤害！";
    }

    private static void applyIncomingHit(Session session, ServerPlayer player,
                                         int damage, boolean critical) {
        SkillEventHandler.TurnBattleDamageResult result =
                SkillEventHandler.applyTurnBattleDamageDetailed(
                        player, damage, session.partyMembers.size() > 1);
        session.incomingHits.add(new ClientboundTurnBattlePacket.IncomingHit(
                result.damage(), critical, result.knockedDown(),
                result.revived(), result.reviveHealth()));
        if (result.knockedDown() && !result.revived() && session.partyMembers.size() > 1) {
            stabilizeDownedPlayer(player, session);
        }
    }

    private static void stabilizeDownedPlayer(ServerPlayer player, Session session) {
        boolean newlyDowned = session.downedMembers.add(player.getUUID());
        player.setHealth(1.0F);
        player.invulnerableTime = 0;
        player.hurtMarked = true;
        if (newlyDowned) PartyManager.refresh(player);
    }

    private static ServerPlayer firstDownedMember(ServerPlayer player, Session session) {
        for (UUID id : session.partyMembers) {
            if (!session.downedMembers.contains(id)) continue;
            ServerPlayer member = player.server.getPlayerList().getPlayer(id);
            if (member != null) return member;
        }
        return null;
    }

    private static ServerPlayer getTargetAlly(ServerPlayer player, Session session,
                                               int index, boolean downed) {
        UUID id = PartyManager.onlineMemberIdAt(player, index);
        if (id == null || !session.partyMembers.contains(id)) return null;
        if (session.downedMembers.contains(id) != downed) return null;
        return player.server.getPlayerList().getPlayer(id);
    }

    private static List<ServerPlayer> activePartyMembers(ServerPlayer player, Session session) {
        List<ServerPlayer> members = new ArrayList<>();
        for (UUID id : session.partyMembers) {
            if (session.downedMembers.contains(id)) continue;
            ServerPlayer member = player.server.getPlayerList().getPlayer(id);
            if (member != null) members.add(member);
        }
        return members;
    }

    private static void defeatParty(ServerPlayer player, Session session, Component message) {
        List<ServerPlayer> members = session.partyMembers.stream()
                .map(id -> player.server.getPlayerList().getPlayer(id))
                .filter(java.util.Objects::nonNull)
                .toList();
        finish(player, session, ClientboundTurnBattlePacket.Outcome.DEFEAT, message, false, 0L);
        for (ServerPlayer member : members) {
            member.setHealth(0.0F);
            member.hurtMarked = true;
        }
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

    private static void restoreTurnBattleMana(BSPlayerStats stats) {
        if (stats == null || stats.maxMp <= 0.0D || stats.mpRegenRate <= 0.0D) {
            return;
        }
        double restored = Math.floor(stats.maxMp * stats.mpRegenRate);
        if (restored > 0.0D) {
            stats.mp = Math.min(stats.maxMp, stats.mp + restored);
        }
    }

    private static void restoreTurnBattleHealth(ServerPlayer player, BSPlayerStats stats) {
        if (stats == null || stats.hpRegenRate == 0.0D || !player.isAlive()) {
            return;
        }
        long amount = (long) (player.getMaxHealth() * stats.hpRegenRate);
        if (amount > 0L && player.getHealth() < player.getMaxHealth()) {
            player.heal((float) amount);
        } else if (amount < 0L && player.getHealth() > 1.0F) {
            player.setHealth(Math.max(1.0F, player.getHealth() + amount));
        }
    }

    private static double domainAttackRate(Session session) {
        TurnBattleDomainData.Domain domain = TurnBattleDomainData.get(session.battleProfileId);
        if (domain == null) {
            return 1.0D;
        }
        if (session.battleProfileId == 561) {
            int elapsedRounds = Math.max(0, session.enemyTurn - 1);
            return Math.max(0.0D, 1.0D - Math.min(5, elapsedRounds) * 0.20D);
        }
        return domain.attackRate();
    }

    private static double domainMagicRate(Session session) {
        TurnBattleDomainData.Domain domain = TurnBattleDomainData.get(session.battleProfileId);
        if (domain == null) {
            return 1.0D;
        }
        if (session.battleProfileId == 561) {
            int elapsedRounds = Math.max(0, session.enemyTurn - 11);
            return Math.max(0.0D, 1.0D - Math.min(5, elapsedRounds) * 0.20D);
        }
        return domain.magicRate();
    }

    private static double domainDefenseRate(Session session) {
        TurnBattleDomainData.Domain domain = TurnBattleDomainData.get(session.battleProfileId);
        if (domain == null) {
            return 1.0D;
        }
        if (session.battleProfileId == 561) {
            int elapsedRounds = Math.max(0, session.enemyTurn - 6);
            return Math.max(0.0D, 1.0D - Math.min(5, elapsedRounds) * 0.20D);
        }
        if (session.battleProfileId == 558 || session.battleProfileId == 564) {
            return Math.max(0.0D, 1.0D - session.enemyTurn * 0.10D);
        }
        return domain.defenseRate();
    }

    private static double domainMagicDefenseRate(Session session) {
        TurnBattleDomainData.Domain domain = TurnBattleDomainData.get(session.battleProfileId);
        if (domain == null) {
            return 1.0D;
        }
        if (session.battleProfileId == 561) {
            int elapsedRounds = Math.max(0, session.enemyTurn - 16);
            return Math.max(0.0D, 1.0D - Math.min(5, elapsedRounds) * 0.20D);
        }
        return domain.magicDefenseRate();
    }

    private static double originalHpRateDamage(BSOriginalEnemyData.Action action,
                                               ServerPlayer player, BSPlayerStats stats) {
        double maxHealth = Math.max(player.getMaxHealth(), stats.hp);
        double damage = 0.0D;
        for (BSOriginalEnemyData.StateEffect effect : action.stateEffects()) {
            if (effect.code() == 11 && effect.chance() < 0.0D) {
                damage += maxHealth * -effect.chance();
            }
        }
        return damage;
    }

    private static void applyEnemySelfStateEffects(BSOriginalEnemyData.Action action,
                                                   Set<Integer> activeStates,
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

    private static void applyEnemyTargetStateEffects(BSOriginalEnemyData.Action action,
                                                     ServerPlayer player, int landedHits) {
        if (action.scope() == 11 || landedHits <= 0) {
            return;
        }
        boolean changed = false;
        for (BSOriginalEnemyData.StateEffect effect : action.stateEffects()) {
            if (effect.code() != 21 || effect.stateId() <= 0) {
                continue;
            }
            double chance = 1.0D - Math.pow(1.0D - Mth.clamp(effect.chance(), 0.0D, 1.0D), landedHits);
            if (player.getRandom().nextDouble() >= chance) {
                continue;
            }
            MobEffect mobEffect = originalPlayerStateEffect(effect.stateId());
            if (mobEffect == null) {
                continue;
            }
            player.addEffect(new MobEffectInstance(mobEffect,
                    originalPlayerStateDuration(effect.stateId()),
                    originalPlayerStateAmplifier(effect.stateId()), false, true, true));
            changed = true;
        }
        if (changed) {
            StatEventHandler.applyStats(player);
            StatEventHandler.syncToClient(player);
        }
    }

    private static MobEffect originalPlayerStateEffect(int stateId) {
        return switch (stateId) {
            case 2 -> BlackSouls.BUFF_POISON.get();
            case 3 -> BlackSouls.BUFF_SEVERE_POISON.get();
            case 5 -> BlackSouls.BUFF_OILY.get();
            case 6 -> BlackSouls.BUFF_SLEEP.get();
            case 7, 8 -> BlackSouls.BUFF_DEFENSELESS.get();
            case 13 -> BlackSouls.BUFF_STUN.get();
            case 17 -> BlackSouls.BUFF_SILENCE.get();
            case 26 -> BlackSouls.BUFF_BLEEDING.get();
            case 28 -> BlackSouls.BUFF_MADNESS.get();
            case 29 -> BlackSouls.BUFF_BURN.get();
            case 39 -> BlackSouls.BUFF_JUGGLING_EVASION.get();
            case 55 -> BlackSouls.BUFF_WEAKNESS.get();
            case 56 -> BlackSouls.BUFF_FEAR.get();
            case 61 -> BlackSouls.BUFF_FRAGILE.get();
            case 162 -> BlackSouls.BUFF_FROSTBITE.get();
            case 163 -> BlackSouls.BUFF_LACERATION.get();
            case 165 -> BlackSouls.BUFF_SEVERED_LEG.get();
            default -> null;
        };
    }

    private static int originalPlayerStateDuration(int stateId) {
        return switch (stateId) {
            case 2, 3, 55 -> 2000;
            case 5, 6, 26, 39, 61, 162 -> 800;
            case 7, 8, 17, 28, 29 -> 400;
            case 13 -> 200;
            case 56 -> 1000;
            case 163 -> 800;
            case 165 -> 600;
            default -> 400;
        };
    }

    private static int originalPlayerStateAmplifier(int stateId) {
        return stateId == 7 ? 1 : 0;
    }

    private static long rewardVictory(ServerPlayer player, BSPlayerStats stats,
                                      Session session) {
        long soulReward = 0L;
        session.rewardItems.clear();
        for (EntityTurnBattleMonster enemy : getEnemies(player, session)) {
            soulReward += DifficultyManager.scaleManagedSoulReward(
                    enemy.level(), enemy.getTurnBattleSoulReward());
            for (ItemStack drop : enemy.rollTurnBattleDrops(player.getRandom())) {
                mergeRewardItem(session.rewardItems, drop);
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
            }
        }
        stats.souls += soulReward;
        if (player.getRandom().nextInt(5) == 0) {
            ItemStack drop = new ItemStack(BlackSouls.SOUL_WHITE.get());
            mergeRewardItem(session.rewardItems, drop);
            if (!player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
        }
        NetworkHandler.sendToPlayer(new PacketSyncStats(stats.serializeNBT()), player);
        return soulReward;
    }

    private static void mergeRewardItem(List<ItemStack> rewards, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        for (ItemStack reward : rewards) {
            if (ItemStack.isSameItemSameTags(reward, stack)) {
                reward.grow(stack.getCount());
                return;
            }
        }
        rewards.add(stack.copy());
    }

    private static void finish(ServerPlayer player, Session session,
                               ClientboundTurnBattlePacket.Outcome outcome, Component message,
                               boolean removeEnemy, long soulReward) {
        EntityTurnBattleMonster rootEnemy = getRootEnemy(player, session);
        if (outcome == ClientboundTurnBattlePacket.Outcome.VICTORY
                && rootEnemy instanceof EntityRabbitKnight) {
            message = Component.literal(message.getString() + "\n" + RabbitKnightDialogue.deathLine());
        }
        player.fallDistance = 0.0F;
        player.setDeltaMovement(Vec3.ZERO);
        sendState(player, false, message, false, outcome, soulReward);
        if (outcome == ClientboundTurnBattlePacket.Outcome.VICTORY && rootEnemy != null) {
            SceneSpawnerBossHandler.markDefeated(rootEnemy, player);
        }
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
        if (rootEnemy instanceof EntityCorpseEatingRabbit rabbit) {
            rabbit.finishDialogueRabbitBattle(player,
                    outcome == ClientboundTurnBattlePacket.Outcome.VICTORY);
        }
        if (rootEnemy instanceof EntityRabbitKnight rabbitKnight) {
            rabbitKnight.finishKnightBattle(player,
                    outcome == ClientboundTurnBattlePacket.Outcome.VICTORY);
        }
        for (UUID memberId : List.copyOf(session.partyMembers)) {
            ServerPlayer member = player.server.getPlayerList().getPlayer(memberId);
            if (member != null && member != player) {
                member.fallDistance = 0.0F;
                member.setDeltaMovement(Vec3.ZERO);
                sendState(member, false, message, false, outcome, 0L);
            }
            PLAYER_SESSIONS.remove(memberId);
        }
        session.downedMembers.clear();
        session.partyMembers.clear();
    }

    private static void sendState(ServerPlayer player, boolean active,
                                  Component message, boolean canAct,
                                  ClientboundTurnBattlePacket.Outcome outcome, long soulReward) {
        sendState(player, active, message, canAct, outcome, soulReward, true, true, true);
    }

    private static void sendState(ServerPlayer player, boolean active,
                                  Component message, boolean canAct,
                                  ClientboundTurnBattlePacket.Outcome outcome, long soulReward,
                                  boolean includePlayerHits, boolean includeIncomingHits,
                                  boolean clearTransientHits) {
        Session session = PLAYER_SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        List<ClientboundTurnBattlePacket.EnemySnapshot> snapshots = new ArrayList<>();
        for (EntityTurnBattleMonster enemy : getEnemies(player, session)) {
            int profileId = enemy instanceof EntityOriginalDatabaseEnemy originalEnemy
                    ? originalEnemy.getProfileId() : -1;
            Set<Integer> visibleStates = new HashSet<>(
                    session.enemyStates.getOrDefault(enemy.getUUID(), Set.of()));
            if (enemy.hasEffect(BlackSouls.BUFF_ATK_DOWN_2.get())) {
                visibleStates.add(ATK_DOWN_2_STATE);
            } else if (enemy.hasEffect(BlackSouls.BUFF_ATK_DOWN.get())) {
                visibleStates.add(ATK_DOWN_STATE);
            }
            if (enemy.hasEffect(BlackSouls.BUFF_DEF_DOWN_2.get())) {
                visibleStates.add(DEF_DOWN_2_STATE);
            } else if (enemy.hasEffect(BlackSouls.BUFF_DEF_DOWN.get())) {
                visibleStates.add(DEF_DOWN_STATE);
            }
            List<Integer> states = visibleStates.stream().sorted().toList();
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
                includePlayerHits ? session.playerHits : List.of(),
                includeIncomingHits ? session.incomingHits : List.of(),
                message, canAct && !session.downedMembers.contains(player.getUUID()), outcome, soulReward,
                session.rewardItems, skillCooldowns(player, session)), player);
        if (clearTransientHits) clearTransientState(session);
    }

    private static void clearTransientState(Session session) {
        session.phaseChanged = false;
        session.playerHits.clear();
        session.incomingHits.clear();
    }

    private static void sendBrokenBattleEnd(ServerPlayer player, Component message) {
        NetworkHandler.sendToPlayer(new ClientboundTurnBattlePacket(
                false, -1, -1, List.of(), 0, 1,
                false, false, List.of(), List.of(), message, false,
                ClientboundTurnBattlePacket.Outcome.ESCAPED, 0L,
                List.of(), Map.of()), player);
    }

    private static void broadcastState(Session session, boolean active, Component message,
                                       boolean canAct, ClientboundTurnBattlePacket.Outcome outcome,
                                       long soulReward) {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (UUID id : List.copyOf(session.partyMembers)) {
            ServerPlayer member = server.getPlayerList().getPlayer(id);
            if (member != null) sendState(member, active, message,
                    canAct && !session.downedMembers.contains(id), outcome, soulReward,
                    true, id.equals(session.presentationDriver), false);
        }
        clearTransientState(session);
    }

    private static void broadcastStateExcept(Session session, UUID excluded, boolean active,
                                             Component message, boolean canAct,
                                             ClientboundTurnBattlePacket.Outcome outcome, long soulReward) {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (UUID id : List.copyOf(session.partyMembers)) {
            if (id.equals(excluded)) continue;
            ServerPlayer member = server.getPlayerList().getPlayer(id);
            if (member != null) sendState(member, active, message,
                    canAct && !session.downedMembers.contains(id), outcome, soulReward,
                    true, false, false);
        }
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
        session.phaseChanged = true;
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
                if (session.partyMembers.size() > 1) {
                    stabilizeDownedPlayer(player, session);
                    if (chooseEnemyTarget(player, session) == null) {
                        defeatParty(player, session, Component.literal("队伍已经无法继续战斗。"));
                    }
                } else {
                    finish(player, session, ClientboundTurnBattlePacket.Outcome.DEFEAT,
                            Component.literal(player.getDisplayName().getString() + "倒下了……"), false, 0L);
                }
            } else {
                PLAYER_SESSIONS.remove(player.getUUID());
                session.enemyIds.forEach(ENEMY_SESSIONS::remove);
            }
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        Vec3 playerAnchor = session.playerAnchors.getOrDefault(player.getUUID(), session.playerAnchor);
        if (player.position().distanceToSqr(playerAnchor) > 0.01D) {
            player.teleportTo(playerAnchor.x, playerAnchor.y, playerAnchor.z);
            player.fallDistance = 0.0F;
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
            session.partyMembers.remove(event.getEntity().getUUID());
            session.downedMembers.remove(event.getEntity().getUUID());
            session.playerAnchors.remove(event.getEntity().getUUID());
            session.pendingPartyActions.remove(event.getEntity().getUUID());
            session.partyActionQueue.removeIf(action -> action.playerId().equals(event.getEntity().getUUID()));
            if (!session.partyMembers.isEmpty()) {
                if (event.getEntity().getUUID().equals(session.presentationDriver)) {
                    session.presentationDriver = session.partyMembers.iterator().next();
                }
                return;
            }
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

    private static Map<MobEffect, EffectSnapshot> captureBattleEffects(ServerPlayer player) {
        Map<MobEffect, EffectSnapshot> snapshots = new HashMap<>();
        for (MobEffectInstance instance : player.getActiveEffects()) {
            snapshots.put(instance.getEffect(), new EffectSnapshot(
                    instance, instance.getDuration(), instance.getAmplifier()));
        }
        return snapshots;
    }

    private static void advanceBattleEffects(ServerPlayer player,
                                              Map<MobEffect, EffectSnapshot> snapshots) {
        ADVANCING_BATTLE_EFFECTS.set(true);
        boolean previousInternalDamage = INTERNAL_BATTLE_ITEM_DAMAGE.get();
        INTERNAL_BATTLE_ITEM_DAMAGE.set(true);
        try {
            MobEffect requiem = BlackSouls.BUFF_REQUIEM.isPresent()
                    ? BlackSouls.BUFF_REQUIEM.get() : null;
            for (Map.Entry<MobEffect, EffectSnapshot> entry : snapshots.entrySet()) {
                if (entry.getKey() != requiem) {
                    advanceBattleEffect(player, entry.getKey(), entry.getValue());
                }
            }
            if (requiem != null && snapshots.containsKey(requiem)) {
                advanceBattleEffect(player, requiem, snapshots.get(requiem));
            }
        } finally {
            INTERNAL_BATTLE_ITEM_DAMAGE.set(previousInternalDamage);
            ADVANCING_BATTLE_EFFECTS.set(false);
        }
    }

    private static void advanceBattleEffect(ServerPlayer player, MobEffect effect,
                                            EffectSnapshot snapshot) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current != snapshot.instance
                || current.getDuration() != snapshot.duration
                || current.getAmplifier() != snapshot.amplifier
                || current.isInfiniteDuration()) {
            return;
        }
        boolean active = true;
        for (int tick = 0; tick < EFFECT_TICKS_PER_ACTION && active; tick++) {
            active = current.tick(player, () -> {
            });
        }
        if (!active) {
            player.removeEffect(effect);
        } else {
            player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), current));
        }
    }

    private record ActionResult(Component message, boolean consumeTurn,
                                List<ClientboundTurnBattlePacket.DamageHit> hits) {
        private ActionResult(Component message, boolean consumeTurn) {
            this(message, consumeTurn, List.of());
        }
    }

    private record PhaseAdvance(String message, int fromProfileId, int toProfileId) {
    }

    private record EffectSnapshot(MobEffectInstance instance, int duration, int amplifier) {
    }

    private record EnemyActionSkip(int actionId, int remainingRounds) {
    }

    private record PendingEnemyAction(UUID enemyId, int skillId, boolean advanceTurnAfter) {
    }

    private record PendingPartyAction(UUID playerId, ServerboundTurnBattleActionPacket.Action action,
                                      int selection, int targetIndex) {
    }

    private record EnemyActionResult(Component message, int followUpSkillId) {
    }

    private record HitValue(double damage, boolean critical) {
    }

    private record CounterHit(int damage, boolean critical) {
    }

    private static final class Session {
        private final UUID rootEnemyId;
        private final Vec3 playerAnchor;
        private final Set<UUID> partyMembers = new java.util.LinkedHashSet<>();
        private final Set<UUID> downedMembers = new HashSet<>();
        private final Map<UUID, Vec3> playerAnchors = new HashMap<>();
        private final Map<UUID, PendingPartyAction> pendingPartyActions = new java.util.LinkedHashMap<>();
        private final Deque<PendingPartyAction> partyActionQueue = new ArrayDeque<>();
        private final Vec3 enemyAnchor;
        private final List<UUID> enemyIds = new ArrayList<>();
        private final Map<UUID, Vec3> enemyAnchors = new HashMap<>();
        private final Map<UUID, Map<String, Integer>> playerSkillCooldowns = new HashMap<>();
        private final Map<UUID, Set<Integer>> enemyStates = new HashMap<>();
        private final Map<UUID, Integer> enemyTurns = new HashMap<>();
        private final Map<UUID, EnemyActionSkip> enemyActionSkips = new HashMap<>();
        private final Map<UUID, Integer> enemyBreakRounds = new HashMap<>();
        private final Map<MobEffect, EffectSnapshot> roundEffectSnapshots = new HashMap<>();
        private final Deque<PendingEnemyAction> enemyActionQueue = new ArrayDeque<>();
        private final List<ItemStack> rewardItems = new ArrayList<>();
        private int battleProfileId;
        private int enemyTurn = 1;
        private int lastEnemyAnimationId = 1;
        private int actingEnemyIndex;
        private boolean phaseChanged;
        private boolean resolving;
        private boolean awaitingPhasePresentation;
        private boolean openingPending;
        private boolean enemySequenceActive;
        private boolean enemySequenceCountsAsRound;
        private boolean counterVictoryPending;
        private boolean enemySequenceGuard;
        private String enemySequencePrefix = "";
        private boolean guardQueued;
        private boolean pendingEnemySequenceAfterPlayerAction;
        private boolean executingPartyBatch;
        private String pendingEnemySequencePrefix = "";
        private UUID pendingEnemyStunClear;
        private UUID presentationDriver;
        private int remainingPlayerActions;
        private long presentationDeadline;
        private final List<ClientboundTurnBattlePacket.DamageHit> playerHits = new ArrayList<>();
        private final List<ClientboundTurnBattlePacket.IncomingHit> incomingHits = new ArrayList<>();

        private Session(UUID rootEnemyId, Vec3 playerAnchor, Vec3 enemyAnchor,
                        int battleProfileId) {
            this.rootEnemyId = rootEnemyId;
            this.playerAnchor = playerAnchor;
            this.enemyAnchor = enemyAnchor;
            this.battleProfileId = battleProfileId;
        }
    }
}

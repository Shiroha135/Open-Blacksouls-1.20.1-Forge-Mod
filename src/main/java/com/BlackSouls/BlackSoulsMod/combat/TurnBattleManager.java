package com.BlackSouls.BlackSoulsMod.combat;

import com.BlackSouls.BlackSoulsMod.BSConfig;
import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.capability.OriginalNpcProgress;
import com.BlackSouls.BlackSoulsMod.story.OriginalNpcInterpreter;
import com.BlackSouls.BlackSoulsMod.story.OriginalBattleMusic;
import com.BlackSouls.BlackSoulsMod.story.OriginalBattleMusicService;
import com.BlackSouls.BlackSoulsMod.story.OriginalNpcPresentationService;
import com.BlackSouls.BlackSoulsMod.util.OriginalActorBridge;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalNpcData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalDatabaseEnemy;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalTurnBattleEnemy;
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
import com.BlackSouls.BlackSoulsMod.util.BSOriginalSkillData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalReactionData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalTroopData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalCombatData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalEnemyPhaseData;
import com.BlackSouls.BlackSoulsMod.util.BSAttributeManager;
import com.BlackSouls.BlackSoulsMod.util.DifficultyManager;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import com.BlackSouls.BlackSoulsMod.util.skill.AbstractSkill;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillRegistry;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillOriginalMagic;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;
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
    private static final Set<Integer> RUNNER_TROOPS = Set.of(35, 191, 343, 629);
    private static final int ESCAPE_GRACE_TICKS = 160;
    private static final int EFFECT_TICKS_PER_ACTION = 200;
    private static final Pattern COUNTER_SKILL = Pattern.compile("<\\s*反撃スキル\\s*(\\d+)\\s*>");
    private static final UUID ORIGINAL_HEALTH_RATE = UUID.fromString("1fa07013-8433-4f33-8d0c-5d8fc62dfe06");
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
            "bs2_skill_bullet_load", "bs2_skill_chakra",
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
        int originalTroop = switch (profileId) {
            case 88 -> 35;
            case 303 -> 343;
            case 594 -> 629;
            default -> 0;
        };
        if (originalTroop > 0) {
            tryStartOriginalTroop(player, enemy, originalTroop, false, false, null, false);
            return;
        }
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
        initializeOriginalEncounter(player, session, false);
        player.getCapability(BSPlayerStats.CAPABILITY).ifPresent(stats ->
                session.remainingPlayerActions = rollPlayerActionCount(player, stats));
        session.openingPending = BSOriginalBattleProfileData.get(profileId).preemptiveSkillId() > 0;
        session.resolving = session.openingPending || session.surprisePending;
        session.presentationDriver = player.getUUID();
        broadcastState(session, true, battleIntro(session.battleProfileId, enemy),
                !session.resolving, ClientboundTurnBattlePacket.Outcome.NONE, 0L);
    }

    public static boolean tryStartOriginalTroop(ServerPlayer player, EntityTurnBattleMonster rootEnemy,
                                                 int troopId, boolean canEscape, boolean canLose,
                                                 Consumer<ClientboundTurnBattlePacket.Outcome> onFinished) {
        return tryStartOriginalTroop(player, rootEnemy, troopId, canEscape, canLose, onFinished, true);
    }

    public static boolean tryStartOriginalTroop(ServerPlayer player, EntityTurnBattleMonster rootEnemy,
                                                 int troopId, boolean canEscape, boolean canLose,
                                                 Consumer<ClientboundTurnBattlePacket.Outcome> onFinished,
                                                 boolean scripted) {
        return tryStartOriginalTroop(player, rootEnemy, troopId, canEscape, canLose, onFinished, scripted, 0);
    }

    public static boolean tryStartOriginalTroop(ServerPlayer player, EntityTurnBattleMonster rootEnemy,
                                                 int troopId, boolean canEscape, boolean canLose,
                                                 Consumer<ClientboundTurnBattlePacket.Outcome> onFinished,
                                                 boolean scripted, int openingCondition) {
        if (openingCondition < 0 || openingCondition > 2 || !scripted && openingCondition != 0) return false;
        BSOriginalTroopData.Entry troop = BSOriginalTroopData.get(troopId);
        if (troop == null || troop.members().isEmpty() || troop.members().stream().allMatch(BSOriginalTroopData.Member::hidden)) {
            BlackSouls.LOGGER.warn("Rejected original battle troop {}: missing or no visible members", troopId);
            return false;
        }
        List<Integer> missing = troop.members().stream().map(BSOriginalTroopData.Member::enemyId)
                .filter(id -> BSOriginalEnemyData.values().stream().noneMatch(enemy -> enemy.id() == id))
                .distinct().toList();
        if (!missing.isEmpty()) {
            BlackSouls.LOGGER.warn("Rejected original battle troop {}: enemy definitions missing {}", troopId, missing);
            player.sendSystemMessage(Component.literal("原作战斗尚缺少敌人定义：" + missing));
            return false;
        }
        if (!(rootEnemy instanceof EntityOriginalDatabaseEnemy original)
                || original.getProfileId() != troop.members().stream().filter(member -> !member.hidden())
                .findFirst().orElseThrow().enemyId()
                || rootEnemy.level() != player.level() || rootEnemy.isRemoved() || rootEnemy.isTurnBattleDefeated()
                || !player.isAlive() || player.isSpectator()
                || PLAYER_SESSIONS.containsKey(player.getUUID()) || ENEMY_SESSIONS.containsKey(rootEnemy.getUUID())) {
            return false;
        }
        Session session = new Session(rootEnemy.getUUID(), player.position(), rootEnemy.position(), original.getProfileId());
        session.originalTroopId = troopId;
        session.battleMusic = scripted ? OriginalBattleMusicService.current(player) : OriginalBattleMusic.UNSET;
        session.canEscape = canEscape;
        session.canLose = canLose;
        session.onFinished = onFinished;
        List<ServerPlayer> members = PartyManager.onlineMembers(player).stream()
                .filter(member -> member.serverLevel() == player.serverLevel())
                .filter(member -> !PLAYER_SESSIONS.containsKey(member.getUUID())).toList();
        if (members.isEmpty()) {
            members = List.of(player);
        }
        for (ServerPlayer member : members) {
            PLAYER_SESSIONS.put(member.getUUID(), session);
            session.partyMembers.add(member.getUUID());
            session.playerAnchors.put(member.getUUID(), member.position());
            member.fallDistance = 0.0F;
        }
        try {
            configureOriginalTroop(player, original, session, troop);
        } catch (RuntimeException exception) {
            session.partyMembers.forEach(PLAYER_SESSIONS::remove);
            for (EntityTurnBattleMonster enemy : getAllEnemies(player, session)) {
                ENEMY_SESSIONS.remove(enemy.getUUID());
                enemy.discard();
            }
            BlackSouls.LOGGER.error("Failed to create original battle troop {}", troopId, exception);
            return false;
        }
        initializeOriginalEncounter(player, session, scripted);
        if (scripted) {
            session.preemptive = openingCondition == 1;
            session.surprisePending = openingCondition == 2;
        }
        session.remainingPlayerActions = rollPlayerActionCount(player, battlePlayerStats(player));
        session.presentationDriver = player.getUUID();
        if (RUNNER_TROOPS.contains(troopId)) {
            session.troopContext = new TroopEventContext(player, session);
            session.troopRunner = new OriginalTroopEventRunner(troop, session.troopContext);
            runTroopEvents(player, session, () -> {
                session.resolving = session.surprisePending;
                broadcastState(session, true, Component.empty(), !session.resolving,
                        ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            }, false);
        } else {
            long pendingPages = troop.pages().stream()
                    .filter(page -> page.commands().stream().anyMatch(command -> command.code() != 0)).count();
            if (pendingPages > 0) {
                BlackSouls.LOGGER.warn("Original troop {} retains {} battle-event pages pending the generic troop runner", troopId, pendingPages);
            }
            session.openingPending = BSOriginalBattleProfileData.get(session.battleProfileId).preemptiveSkillId() > 0;
            session.resolving = session.openingPending || session.surprisePending;
            broadcastState(session, true, battleIntro(session.battleProfileId, rootEnemy), !session.resolving,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
        }
        return true;
    }

    private static void runTroopEvents(ServerPlayer player, Session session, Runnable resume, boolean turnEnding) {
        if (session.finished) return;
        if (session.troopRunner == null) {
            resume.run();
            return;
        }
        if (!session.troopRunner.hasPending() && allEnemiesDefeated(player, session)) {
            finishTroopVictory(player, session);
            return;
        }
        session.resolving = true;
        session.presentationDriver = player.getUUID();
        session.troopResume = resume;
        session.troopRunner.setTurnEnding(turnEnding);
        presentTroopStep(player, session, session.troopRunner.pump());
    }

    private static void presentTroopStep(ServerPlayer player, Session session, OriginalTroopEventRunner.Step step) {
        if (session.finished) return;
        if (step.kind() == OriginalTroopEventRunner.Kind.IDLE) {
            Runnable resume = session.troopResume;
            session.troopResume = null;
            if (!session.troopContext.animations.isEmpty()) {
                session.troopPresentation = new OriginalTroopPresentation(session.troopToken, 0L, 5,
                        "", List.of(), 0, -1, session.troopContext.animations);
                session.troopContext.animations.clear();
                broadcastState(session, true, Component.empty(), false,
                        ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            }
            session.troopPresentation = null;
            session.troopWaitUntil = 0L;
            session.troopTextUntil = 0L;
            session.troopRunner.setTurnEnding(false);
            if (allEnemiesDefeated(player, session)) {
                finishTroopVictory(player, session);
            } else if (resume != null) {
                resume.run();
            }
            return;
        }
        OriginalNpcInterpreter.Prompt prompt = step.prompt();
        int kind = switch (step.kind()) {
            case TEXT -> 1;
            case CHOICE -> 2;
            case WAIT -> 3;
            default -> 4;
        };
        String text = prompt == null ? "" : prompt.text();
        int textWait = OriginalTroopPresentation.textWaitTicks(text);
        int automaticTicks = kind == 1 && text.contains("\\^") ? textWait : -1;
        session.troopTextUntil = player.serverLevel().getGameTime() + textWait;
        session.troopWaitUntil = kind == 3
                ? player.serverLevel().getGameTime() + Math.max(1, prompt == null ? 1 : prompt.waitTicks()) : 0L;
        if (kind == 4) {
            text = "原作战斗事件尚未支持：Troop" + session.originalTroopId
                    + " / 第" + (step.pageIndex() + 1) + "页 / " + step.reason();
            BlackSouls.LOGGER.warn("Blocked original troop {} page {}: {}",
                    session.originalTroopId, step.pageIndex(), step.reason());
        }
        session.troopPresentation = new OriginalTroopPresentation(session.troopToken,
                prompt == null ? 0L : prompt.revision(), kind, session.troopContext.speaker,
                prompt == null ? List.of() : prompt.choices(), textWait, automaticTicks,
                session.troopContext.animations);
        session.troopContext.animations.clear();
        broadcastState(session, true, Component.literal(OriginalTroopPresentation.visibleText(text)), false,
                ClientboundTurnBattlePacket.Outcome.NONE, 0L);
    }

    public static void handleTroopPresentation(ServerPlayer player, UUID token, long revision, int selection) {
        Session session = PLAYER_SESSIONS.get(player.getUUID());
        if (session == null || session.finished || session.troopResume == null
                || session.troopRunner == null || session.troopPresentation == null
                || !session.troopToken.equals(token)
                || !player.getUUID().equals(session.presentationDriver)
                || revision != session.troopPresentation.revision()
                || session.troopPresentation.kind() > 2) return;
        if (player.serverLevel().getGameTime() < session.troopTextUntil) {
            if (session.troopPresentation.kind() == 1 && session.troopPresentation.automaticTicks() >= 0) {
                session.troopAutomaticAcknowledged = true;
            } else {
                OriginalTroopPresentation current = session.troopPresentation;
                int remaining = (int) Math.min(Integer.MAX_VALUE,
                        session.troopTextUntil - player.serverLevel().getGameTime());
                session.troopPresentation = new OriginalTroopPresentation(current.token(), current.revision(),
                        current.kind(), current.speaker(), current.choices(), remaining,
                        current.automaticTicks(), List.of());
                OriginalNpcInterpreter.Prompt prompt = session.troopRunner.pump().prompt();
                sendState(player, true, Component.literal(OriginalTroopPresentation.visibleText(prompt.text())),
                        false, ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            }
            return;
        }
        session.troopAutomaticAcknowledged = false;
        OriginalTroopEventRunner.Step step = session.troopRunner.acknowledge(revision, selection);
        presentTroopStep(player, session, step);
    }

    private static void finishTroopVictory(ServerPlayer player, Session session) {
        BSPlayerStats stats = battlePlayerStats(player);
        long reward = rewardVictory(player, stats, session);
        session.troopPresentation = null;
        session.troopResume = null;
        session.troopAfterPresentation = null;
        finish(player, session, ClientboundTurnBattlePacket.Outcome.VICTORY,
                Component.literal("敌群被打倒了！"), true, reward);
    }

    private static final class TroopEventContext implements OriginalTroopEventRunner.Context {
        private final ServerPlayer player;
        private final Session session;
        private final OriginalNpcProgress progress;
        private String speaker = "";
        private final List<OriginalTroopPresentation.Animation> animations = new ArrayList<>();

        private TroopEventContext(ServerPlayer player, Session session) {
            this.player = player;
            this.session = session;
            this.progress = OriginalNpcProgress.get(player.server);
        }

        private EntityTurnBattleMonster enemy(int slot) {
            if (slot < 0 || slot >= session.originalTroopMembers.size()) {
                throw unsupported("enemy_slot:" + slot);
            }
            Entity entity = player.serverLevel().getEntity(session.originalTroopMembers.get(slot));
            if (!(entity instanceof EntityTurnBattleMonster enemy)) throw unsupported("missing_enemy:" + slot);
            return enemy;
        }

        private List<EntityTurnBattleMonster> enemies(int slot) {
            return slot == -1 ? getAllEnemies(player, session) : List.of(enemy(slot));
        }

        @Override
        public double enemyHpPercent(int originalSlot) {
            EntityTurnBattleMonster enemy = enemy(originalSlot);
            return enemy.getTurnBattleHealth() * 100.0D / Math.max(1.0D, enemy.getTurnBattleMaxHealth());
        }

        @Override
        public Double actorHpPercent(int originalActorId) {
            Double value = OriginalActorBridge.hpPercent(player, originalActorId);
            if (value == null) throw unsupported("unmapped_actor:" + originalActorId);
            return value;
        }

        @Override
        public boolean switchValue(int id) {
            return com.BlackSouls.BlackSoulsMod.story.OriginalSenService.isThresholdSwitch(id)
                    ? OriginalNpcProgress.readSwitch(player, id) : progress.switchValue(id);
        }

        @Override
        public void setSwitch(int id, boolean value) {
            if (com.BlackSouls.BlackSoulsMod.story.OriginalSenService.isThresholdSwitch(id)) OriginalNpcProgress.writeSwitch(player, id, value);
            else progress.setSwitch(id, value);
        }

        @Override
        public long variable(int id) { return OriginalNpcProgress.readVariable(player, id); }

        @Override
        public void setVariable(int id, long value) { OriginalNpcProgress.writeVariable(player, id, value); }

        @Override
        public void afterVariableOperation(int id, int operation, long operand, long before, long after) {
            if (id == 38) com.BlackSouls.BlackSoulsMod.story.OriginalSenService.afterVariableOperation(player, operation, operand, before, after);
        }

        @Override
        public List<BSOriginalNpcData.Command> commonEvent(int id) { return BSOriginalNpcData.commonEvent(id); }

        @Override
        public boolean hasActor(int actorId) { return OriginalActorBridge.isPlayerActor(player, actorId); }

        @Override
        public boolean actorCondition(int actorId, int condition, JsonElement value) {
            if (condition == 0) return hasActor(actorId);
            if (!hasActor(actorId)) throw unsupported("unbound_actor:" + actorId);
            return switch (condition) {
                case 0 -> true;
                case 4, 5 -> OriginalActorBridge.equipped(player, actorId, condition == 4 ? 1 : 2, value.getAsInt());
                case 6 -> session.originalState.hasState(player, value.getAsInt());
                default -> throw unsupported("actor_condition:" + condition);
            };
        }

        @Override
        public boolean enemyCondition(int slot, int condition, JsonElement operand) {
            EntityTurnBattleMonster enemy = enemy(slot);
            return switch (condition) {
                case 0 -> !session.hiddenEnemyIds.contains(enemy.getUUID()) && battleAlive(enemy, session);
                case 1 -> session.originalState.hasState(enemy, operand.getAsInt());
                default -> throw unsupported("enemy_condition:" + condition);
            };
        }

        @Override
        public long gameData(int type, int first, int second) {
            if (type == 3) return OriginalActorBridge.gameData(player, first, second);
            if (type == 4) {
                EntityTurnBattleMonster enemy = enemy(first);
                return switch (second) {
                    case 0 -> (long) enemy.getTurnBattleHealth();
                    case 1 -> (long) enemy.getTurnBattleMana();
                    case 2 -> (long) enemy.getTurnBattleMaxHealth();
                    case 3 -> (long) enemy.getTurnBattleMaxMana();
                    default -> throw unsupported("enemy_game_data:" + second);
                };
            }
            throw unsupported("game_data:" + type + ":" + first + ":" + second);
        }

        @Override
        public long random(int first, int last) {
            if (last < first || (long) last - first >= Integer.MAX_VALUE) throw unsupported("random_range");
            return (long) first + player.getRandom().nextInt(last - first + 1);
        }

        @Override
        public void comment(String text) {
            if (text.startsWith("NW名前指定 ")) speaker = text.substring("NW名前指定 ".length()).strip();
        }

        @Override
        public void sound(JsonObject sound) {
            EntityTurnBattleMonster root = getRootEnemy(player, session);
            for (UUID memberId : session.partyMembers) {
                ServerPlayer member = player.server.getPlayerList().getPlayer(memberId);
                if (member != null) NetworkHandler.sendToPlayer(
                        OriginalNpcPresentationService.createPacket(member, root, 250, List.of(sound)), member);
            }
        }

        @Override
        public int battleCommand(int command, List<JsonElement> parameters) {
            int slot = parameters.get(0).getAsInt();
            if (command == 335) {
                for (EntityTurnBattleMonster enemy : enemies(slot)) {
                    appearOriginalTroopEnemy(player, session.originalTroopMembers.indexOf(enemy.getUUID()));
                }
                return 0;
            }
            if (command == 337) {
                int animation = parameters.get(1).getAsInt();
                if (animation <= 0) throw unsupported("enemy_animation:" + animation);
                for (EntityTurnBattleMonster enemy : enemies(slot)) {
                    if (battleAlive(enemy, session) && !session.hiddenEnemyIds.contains(enemy.getUUID())) {
                        animations.add(new OriginalTroopPresentation.Animation(
                                session.originalTroopMembers.indexOf(enemy.getUUID()), animation));
                    }
                }
                return 0;
            }
            if (command == 331 || command == 332) {
                int operation = parameters.get(1).getAsInt();
                int operandType = parameters.get(2).getAsInt();
                if (operation < 0 || operation > 1 || operandType < 0 || operandType > 1) {
                    throw unsupported("enemy_resource_operand");
                }
                long operand = operandType == 0 ? parameters.get(3).getAsLong()
                        : variable(parameters.get(3).getAsInt());
                double amount = operation == 0 ? operand : -(double) operand;
                for (EntityTurnBattleMonster enemy : enemies(slot)) {
                    if (command == 332) {
                        enemy.setTurnBattleMana(enemy.getTurnBattleMana() + amount);
                    } else if (battleAlive(enemy, session)) {
                        boolean allowDeath = parameters.get(4).getAsBoolean();
                        double hp = enemy.getTurnBattleHealth() + amount;
                        enemy.setTurnBattleHealth(allowDeath ? hp : Math.max(1.0D, hp));
                        if (enemy.isTurnBattleDefeated()) {
                            session.enemyStates.computeIfAbsent(enemy.getUUID(), ignored -> new HashSet<>()).add(1);
                        }
                    }
                }
                return 0;
            }
            if (command == 333) {
                int operation = parameters.get(1).getAsInt();
                int state = parameters.get(2).getAsInt();
                if (operation < 0 || operation > 1 || BSOriginalCombatData.state(state) == null) {
                    throw unsupported("enemy_state_operand:" + state);
                }
                for (EntityTurnBattleMonster enemy : enemies(slot)) {
                    if (operation == 0) {
                        if (session.originalState.addStateDirect(enemy, state, player.getRandom()) && state == 1) {
                            enemy.setTurnBattleHealth(0.0D);
                        }
                    } else {
                        session.originalState.removeState(enemy, state);
                        if (state == 1 && enemy.isTurnBattleDefeated()) enemy.setTurnBattleHealth(1.0D);
                    }
                    refreshOriginalParameters(enemy, session);
                }
                return 0;
            }
            if (command == 336) {
                int profileId = parameters.get(1).getAsInt();
                if (BSOriginalEnemyData.values().stream().noneMatch(value -> value.id() == profileId)) {
                    throw unsupported("enemy_transform:" + profileId);
                }
                for (EntityTurnBattleMonster enemy : enemies(slot)) {
                    if (!(enemy instanceof EntityOriginalDatabaseEnemy original)) throw unsupported("enemy_transform_type");
                    original.transformTurnBattleProfile(profileId);
                    refreshOriginalParameters(original, session);
                }
                return 0;
            }
            throw unsupported("battle_command:" + command);
        }

        private UnsupportedOperationException unsupported(String reason) {
            return new UnsupportedOperationException(reason);
        }

        @Override
        public boolean selfSwitch(String key) { throw unsupported("troop_self_switch"); }

        @Override
        public void setSelfSwitch(String key, boolean value) { throw unsupported("troop_self_switch"); }

        @Override
        public long gold() { return battlePlayerStats(player).souls; }

        @Override
        public void changeGold(long amount) { throw unsupported("troop_gold_change"); }

        @Override
        public int itemCount(int kind, int id) { throw unsupported("troop_item_count"); }

        @Override
        public boolean changeItem(int kind, int id, long amount) { throw unsupported("troop_item_change"); }

        @Override
        public boolean scriptCondition(String expression) { throw unsupported("script_condition:" + expression); }

        @Override
        public void eraseEvent() { throw unsupported("troop_erase_event"); }

        @Override
        public void animation(int id) { throw unsupported("troop_character_animation"); }

        @Override
        public void recoverAll(int type, int id) { throw unsupported("troop_actor_recover"); }

        @Override
        public boolean canTrade(OriginalNpcInterpreter.Goods goods) { return false; }

        @Override
        public void buy(OriginalNpcInterpreter.Goods goods) { throw unsupported("troop_shop"); }
    }

    public static boolean appearOriginalTroopEnemy(ServerPlayer player, int memberIndex) {
        Session session = PLAYER_SESSIONS.get(player.getUUID());
        if (session == null || session.finished || session.originalTroopId <= 0
                || memberIndex < 0 || memberIndex >= session.originalTroopMembers.size()) {
            return false;
        }
        UUID id = session.originalTroopMembers.get(memberIndex);
        Entity entity = player.serverLevel().getEntity(id);
        if (!(entity instanceof EntityTurnBattleMonster enemy) || !session.hiddenEnemyIds.remove(id)) {
            return false;
        }
        enemy.setInvisible(!id.equals(session.rootEnemyId));
        refreshOriginalParameters(enemy, session);
        if (!session.resolving) {
            broadcastState(session, true, Component.literal(enemy.getDisplayName().getString() + "出现了！"),
                    true, ClientboundTurnBattlePacket.Outcome.NONE, 0L);
        }
        return true;
    }

    public static EntityTurnBattleMonster originalTroopMember(ServerPlayer player, int memberIndex) {
        Session session = PLAYER_SESSIONS.get(player.getUUID());
        if (session == null || session.finished || session.originalTroopId <= 0
                || memberIndex < 0 || memberIndex >= session.originalTroopMembers.size()) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(session.originalTroopMembers.get(memberIndex));
        return entity instanceof EntityTurnBattleMonster enemy ? enemy : null;
    }

    public static boolean abortOriginalTroop(ServerPlayer player) {
        Session session = PLAYER_SESSIONS.get(player.getUUID());
        if (session == null || session.finished || session.originalTroopId <= 0) {
            return false;
        }
        finish(player, session, ClientboundTurnBattlePacket.Outcome.NONE,
                Component.literal("原作战斗已中止。"), false, 0L);
        return true;
    }

    private static ClientboundTurnBattlePacket.Outcome interruptionOutcome(Session session) {
        return session.originalTroopId > 0 ? ClientboundTurnBattlePacket.Outcome.NONE
                : ClientboundTurnBattlePacket.Outcome.ESCAPED;
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

    public static double originalElementRate(LivingEntity entity, int elementId) {
        Session session = entity instanceof Player ? PLAYER_SESSIONS.get(entity.getUUID())
                : PLAYER_SESSIONS.get(ENEMY_SESSIONS.get(entity.getUUID()));
        if (session == null && entity instanceof EntityTurnBattleMonster) {
            session = PLAYER_SESSIONS.values().stream()
                    .filter(candidate -> candidate.enemyIds.contains(entity.getUUID())).findFirst().orElse(null);
        }
        if (session != null) {
            return session.originalState.elementRate(entity, elementId);
        }
        int profileId = entity instanceof EntityOriginalDatabaseEnemy original ? original.getProfileId()
                : entity instanceof EntityOriginalTurnBattleEnemy legacy ? legacy.getOriginalBattleProfileId() : 0;
        double rate = 1.0D;
        rate *= StatEventHandler.getOriginalSpiritRate(entity, 11, elementId);
        for (BSOriginalCombatData.Feature feature : BSOriginalCombatData.enemy(profileId).features()) {
            if (feature.code() == 11 && feature.dataId() == elementId) {
                rate *= feature.value();
            }
        }
        return rate;
    }

    public static boolean shouldFreezeEffectTick(LivingEntity entity) {
        return !entity.level().isClientSide()
                && !ADVANCING_BATTLE_EFFECTS.get() && isInBattle(entity);
    }

    public static boolean isAdvancingBattleEffects() {
        return ADVANCING_BATTLE_EFFECTS.get();
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
        if (session.troopResume != null || session.troopAfterPresentation != null) return;
        if (action == ServerboundTurnBattleActionPacket.Action.ESCAPE && !session.canEscape) {
            sendState(player, true, Component.literal("这场战斗无法逃走。"), !session.resolving,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
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
            finish(player, session, interruptionOutcome(session),
                    Component.literal("战斗目标已经消失。"), false, 0L);
            return;
        }
        if (session.resolving) {
            BlackSouls.LOGGER.warn(
                    "Turn battle action {} arrived while resolving for {}: opening={}, awaiting={}, enemySequence={}, queue={}",
                    action, player.getGameProfile().getName(), session.openingPending,
                    session.awaitingPhasePresentation, session.enemySequenceActive,
                    session.enemyActionQueue.size());
            if (session.openingPending || session.surprisePending) {
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
                    finish(player, session, interruptionOutcome(session),
                            Component.literal("战斗中断。"), false, 0L);
                } else {
                    presentNextEnemyAction(player, pendingStats, session);
                }
                return;
            }
            session.resolving = false;
        }
        if (session.troopRunner != null && (!session.troopPlayerBatchOpen
                || !player.getUUID().equals(session.troopPlayerBatchOwner))
                && action != ServerboundTurnBattleActionPacket.Action.WEAPON_CHANGE) {
            session.troopPlayerBatchOpen = true;
            session.troopPlayerBatchOwner = player.getUUID();
            session.troopRunner.increaseTurn();
            runTroopEvents(player, session, () -> {
                session.resolving = false;
                handleAction(player, action, selection, targetIndex);
            }, false);
            return;
        }
        session.resolving = true;
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) {
            finish(player, session, interruptionOutcome(session),
                    Component.literal("战斗中断。"), false, 0L);
            return;
        }
        if (session.remainingPlayerActions <= 0) {
            session.remainingPlayerActions = rollPlayerActionCount(player, stats);
        }
        if (player.hasEffect(BlackSouls.BUFF_STUN.get()) || player.hasEffect(BlackSouls.BUFF_SLEEP.get())
                || session.originalState.restriction(player) >= 4) {
            session.originalState.advanceAction(player);
            session.remainingPlayerActions = Math.max(0, session.remainingPlayerActions - 1);
            advanceSkillCooldowns(player, session);
            StatEventHandler.syncToClient(player);
            Component stunned = Component.literal(player.getDisplayName().getString() + "无法行动！");
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
                if (session.preemptive || player.getRandom().nextDouble() < session.escapeRatio) {
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
                session.escapeRatio += 0.10D;
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
        session.originalState.advanceAction(player);
        session.guardQueued |= guard;
        if (guard) {
            session.guardingPlayers.add(player.getUUID());
        } else {
            session.guardingPlayers.remove(player.getUUID());
        }
        if (session.troopRunner != null && battleAlive(player, session) && !allEnemiesDefeated(player, session)) {
            session.troopAfterPresentation = () -> runTroopEvents(player, session, () -> {
                if (session.remainingPlayerActions <= 0) session.troopPlayerBatchOpen = false;
                completePlayerAction(player, stats, session, action, Component.empty());
            }, session.remainingPlayerActions <= 0 || session.executingPartyBatch);
            session.awaitingPhasePresentation = true;
            session.presentationDeadline = player.serverLevel().getGameTime() + 200L;
            session.presentationDriver = player.getUUID();
            broadcastState(session, true, playerMessage, false, ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            return;
        }
        completePlayerAction(player, stats, session, action, playerMessage);
    }

    private static void completePlayerAction(ServerPlayer player, BSPlayerStats stats, Session session,
                                              ServerboundTurnBattleActionPacket.Action action,
                                              Component playerMessage) {
        EntityTurnBattleMonster rootEnemy = getRootEnemy(player, session);
        if (!battleAlive(player, session)) {
            ServerPlayer survivor = chooseEnemyTarget(player, session);
            if (survivor == null) {
                defeatParty(player, session, playerMessage);
                return;
            }
            if (session.executingPartyBatch && session.remainingPlayerActions > 0) {
                broadcastState(session, true, playerMessage, false,
                        ClientboundTurnBattlePacket.Outcome.NONE, 0L);
                session.resolving = false;
                executeNextPartyAction(player.server, session);
            } else {
                session.executingPartyBatch = false;
                beginEnemySequence(survivor, battlePlayerStats(survivor), session, playerMessage);
            }
            return;
        }
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
        double enemyDefense = battleParameter(enemy, 3, player, session);
        int dualSwordAura = getDualSwordAura(player);
        int weaponHits = player.getMainHandItem().is(BlackSouls.DIVINE_ANGEL_DUAL_SWORDS.get())
                ? dualSwordAura + 1 : 1;
        int hitCount = weaponHits + (int) Math.max(0.0D, session.originalState.features(player).stream()
                .filter(feature -> feature.code() == 34).mapToDouble(BSOriginalCombatData.Feature::value).sum());
        List<ClientboundTurnBattlePacket.DamageHit> hits = new ArrayList<>();
        int totalDamage = 0;
        boolean critical = false;
        StringBuilder reactions = new StringBuilder();
        for (int hit = 0; hit < hitCount; hit++) {
            if (!battleAlive(player, session) || !battleAlive(enemy, session)) {
                break;
            }
            boolean counter = rollTurnBattleCounter(player, player, enemy, 1,
                    BSOriginalReactionData.metadata(1), session);
            double chance = Mth.clamp(originalHitRate(player, session), 0.0D, 1.0D)
                    * (1.0D - session.originalState.xparam(enemy, 1));
            if (player.getRandom().nextDouble() >= Mth.clamp(chance, 0.0D, 1.0D)) {
                if (counter) reactions.append(executeTurnBattleCounter(player, enemy, player, session));
                continue;
            }
            double multiplier = hit == 0 || hit >= weaponHits ? 4.0D : 3.0D;
            double damage = varied(player, Math.max(
                    1.0D, battleParameter(player, 2, player, session)
                            * multiplier - enemyDefense * 2.0D));
            boolean hitCritical = player.getRandom().nextDouble()
                    < (stats.critRate + stats.bonusCritRate + session.originalState.xparam(player, 2) * 100.0D)
                    / 100.0D * (1.0D - session.originalState.xparam(enemy, 3));
            if (hitCritical) {
                damage *= 3.0D;
                critical = true;
            }
            damage *= turnBattlePhysicalAttributeMultiplier(player, enemy, stats);
            damage *= turnBattleDamageMultiplier(enemy, session) * session.originalState.sparam(enemy, 6);
            int dealt = Math.max(1, (int) Math.round(damage));
            enemy.setTurnBattleHealth(enemy.getTurnBattleHealth() - dealt);
            totalDamage += dealt;
            hits.add(new ClientboundTurnBattlePacket.DamageHit(
                    enemy.getId(), dealt, hitCritical, hit));
            session.originalState.onDamage(enemy, player.getRandom());
            refreshOriginalParameters(enemy, session);
            if (counter) {
                reactions.append(executeTurnBattleCounter(player, enemy, player, session));
            }
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
        message.append(reactions);
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
                || skill instanceof SkillOriginalMagic magic && magic.getTurnBattleSourceSkillId() == 50
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
            if (skill instanceof SkillOriginalMagic magic && magic.getTurnBattleHitType() == 2) {
                BSOriginalEnemyData.Action originalAction = BSOriginalReactionData.action(magic.getTurnBattleSourceSkillId());
                if (originalAction == null) {
                    throw new IllegalStateException("Missing original magic action " + magic.getTurnBattleSourceSkillId());
                }
                List<LivingEntity> targets = originalAction.scope() == 1 ? List.of(selectedTarget)
                        : new ArrayList<>(getEnemies(player, session).stream()
                        .filter(enemy -> isTargetableEnemy(player, session, enemy)).toList());
                EnemyActionResult result = executeOriginalSkillTargets(player, player, originalAction,
                        targets, false, session, true);
                StatEventHandler.syncToClient(player);
                return new ActionResult(Component.literal(actor + "使用了" + skillName + "！\n"
                        + result.message().getString()), true, List.copyOf(session.playerHits));
            }
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
        SkillOriginalMagic originalMagic = skill instanceof SkillOriginalMagic magic ? magic : null;
        int reactionHitType = playerSkillHitType(skill);
        BSOriginalSkillData.Entry reactionMetadata = playerReactionMetadata(skill);
        int hitCount = originalMagic == null ? turnHitCount(player, skill) : originalMagic.getTurnBattleHitCount();
        List<ClientboundTurnBattlePacket.DamageHit> hits = new ArrayList<>();
        Map<UUID, Integer> totalDamage = new HashMap<>();
        StringBuilder message = new StringBuilder(actor).append("使用了").append(skillName).append("！");
        for (int wave = 0; wave < hitCount; wave++) {
            if (!battleAlive(player, session)) {
                break;
            }
            List<EntityTurnBattleMonster> waveTargets = targets.stream()
                    .filter(enemy -> !enemy.isTurnBattleDefeated()).toList();
            if (waveTargets.isEmpty()) {
                break;
            }
            if (originalMagic != null) {
                if (originalMagic.selectsRandomTurnBattleTargets()) {
                    waveTargets = List.of(waveTargets.get(player.getRandom().nextInt(waveTargets.size())));
                } else {
                    waveTargets = waveTargets.stream().limit(originalMagic.getTurnBattleTargetLimit()).toList();
                }
            }
            for (EntityTurnBattleMonster enemy : waveTargets) {
                if (!battleAlive(player, session)) {
                    break;
                }
                boolean counter = rollTurnBattleCounter(player, player, enemy, reactionHitType, reactionMetadata, session);
                boolean reflected = !counter && rollTurnBattleReflection(player, enemy, reactionHitType, session);
                LivingEntity hitTarget = reflected ? player : enemy;
                if (reflected) {
                    message.append("\n").append(enemy.getDisplayName().getString()).append("的魔法反射！");
                }
                double defense = battleParameter(hitTarget, 3, player, session);
                double magicDefense = battleParameter(hitTarget, 5, player, session);
                double damage;
                boolean critical = false;
                boolean physical = skill instanceof WeaponSkill
                        || originalMagic != null && originalMagic.isPhysicalTurnBattleAttack();
                if (originalMagic != null) {
                    SkillOriginalMagic.TurnBattleHit hit = originalMagic.rollTurnBattleHit(
                            player, stats, hitTarget, battleParameter(player, 2, player, session),
                            battleParameter(player, 4, player, session), defense, magicDefense);
                    damage = hit.damage();
                    critical = hit.critical();
                } else {
                    double baseDamage = physical
                            ? battleParameter(player, 2, player, session) * 4.0D - defense * 2.0D
                            : battleParameter(player, 4, player, session) * 5.0D - magicDefense * 2.0D;
                    damage = varied(player, Math.max(1.0D, baseDamage));
                }
                if (physical && reactionHitType != 0) {
                    damage *= turnBattlePhysicalAttributeMultiplier(player, hitTarget, stats);
                }
                damage *= turnBattleDamageMultiplier(hitTarget, session);
                if (reactionHitType == 1 || reactionHitType == 2) {
                    damage *= session.originalState.sparam(hitTarget, reactionHitType == 1 ? 6 : 7);
                    if (hitTarget instanceof ServerPlayer targetPlayer) {
                        damage *= Math.max(0.0D, reactionHitType == 1 ? battlePlayerStats(targetPlayer).physicalDamageRate
                                : battlePlayerStats(targetPlayer).magicDamageRate);
                    }
                }
                int dealt = Math.max(1, safeDamageInt(damage));
                double healthBeforeHit = battleHealth(hitTarget);
                if (hitTarget instanceof ServerPlayer targetPlayer) {
                    applyIncomingHit(session, targetPlayer, dealt, critical);
                } else {
                    enemy.setTurnBattleHealth(healthBeforeHit - dealt);
                }
                if (originalMagic != null) {
                    boolean wasStunned = hitTarget.hasEffect(BlackSouls.BUFF_STUN.get());
                    originalMagic.applyTurnBattleHitEffects(player, hitTarget, Math.min(healthBeforeHit, dealt));
                    if (!reflected && !enemy.isTurnBattleDefeated()) {
                        applyTurnBattleStun(enemy, session, wasStunned);
                    }
                }
                session.originalState.onDamage(hitTarget, player.getRandom());
                refreshOriginalParameters(hitTarget, session);
                if (!reflected) {
                    hits.add(new ClientboundTurnBattlePacket.DamageHit(
                            enemy.getId(), dealt, critical, wave));
                    totalDamage.merge(enemy.getUUID(), dealt,
                            (previous, next) -> (int) Math.min(Integer.MAX_VALUE, (long) previous + next));
                }
                if (counter) {
                    message.append(executeTurnBattleCounter(player, enemy, player, session));
                }
            }
        }
        for (EntityTurnBattleMonster enemy : targets) {
            if (!totalDamage.containsKey(enemy.getUUID())) {
                continue;
            }
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
        return applyTurnBattleStun(target, session, wasStunned);
    }

    private static boolean applyTurnBattleStun(EntityTurnBattleMonster target,
                                               Session session, boolean wasStunned) {
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
        if (rootEnemy == null || session.troopRunner != null
                || session.escapedEnemyIds.contains(rootEnemy.getUUID())) {
            return false;
        }
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
        if (session == null || rootEnemy == null || !session.resolving || session.troopResume != null) {
            return;
        }
        if (session.presentationDriver != null && !session.presentationDriver.equals(player.getUUID())) {
            return;
        }
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) {
            finish(player, session, interruptionOutcome(session),
                    Component.literal("战斗中断。"), false, 0L);
            return;
        }
        if (session.troopAfterPresentation != null) {
            Runnable continuation = session.troopAfterPresentation;
            session.troopAfterPresentation = null;
            session.awaitingPhasePresentation = false;
            session.presentationDeadline = 0L;
            continuation.run();
            return;
        }
        if (session.openingPending) {
            beginOpeningSequence(player, stats, session);
            return;
        }
        if (session.surprisePending) {
            session.surprisePending = false;
            beginEnemySequence(player, stats, session, Component.literal("遭到了敌人的突然袭击！"));
            return;
        }
        if (session.troopRunner != null && session.counterVictoryPending && session.awaitingPhasePresentation) {
            session.counterVictoryPending = false;
            session.awaitingPhasePresentation = false;
            session.presentationDeadline = 0L;
            runTroopEvents(player, session, () -> presentNextEnemyAction(player, stats, session), true);
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
            if (session.troopRunner != null) {
                runTroopEvents(player, session, () -> presentNextEnemyAction(player, stats, session),
                        session.troopEnemyBatchEnding);
            } else {
                presentNextEnemyAction(player, stats, session);
            }
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
        if (transition.recoverAll()) {
            nextController.setTurnBattleHealth(nextController.getTurnBattleMaxHealth());
            if (nextController instanceof EntityOriginalDatabaseEnemy original) {
                original.recoverTurnBattleResources();
            }
        } else {
            double nextHealth = previousHealth > 0.0D
                    ? Math.min(previousHealth, nextController.getTurnBattleMaxHealth())
                    : Math.max(1.0D, nextController.getTurnBattleMaxHealth()
                    * Math.max(0.01D, transition.thresholdPercent() / 100.0D));
            nextController.setTurnBattleHealth(nextHealth);
        }
        session.enemyTurn = 1;
        session.lastEnemyAnimationId = nextController.getTurnBattleAttackAnimationId();
        session.actingEnemyIndex = visibleEnemyIndex(player, session, nextController);
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

    private static void configureOriginalTroop(ServerPlayer player, EntityOriginalDatabaseEnemy root,
                                                Session session, BSOriginalTroopData.Entry troop) {
        BSOriginalBattleProfileData.Entry specialized = BSOriginalBattleProfileData.get(session.battleProfileId);
        int rootIndex = 0;
        while (troop.members().get(rootIndex).hidden()) {
            rootIndex++;
        }
        for (int index = 0; index < troop.members().size(); index++) {
            BSOriginalTroopData.Member member = troop.members().get(index);
            EntityOriginalDatabaseEnemy enemy = index == rootIndex ? root : BSEntityRegistry.ORIGINAL_ENEMY.get().create(player.serverLevel());
            if (enemy == null) {
                throw new IllegalStateException("Failed to create original troop member " + index);
            }
            if (index != rootIndex) {
                enemy.setProfileId(member.enemyId());
                enemy.moveTo(session.enemyAnchor.x, session.enemyAnchor.y, session.enemyAnchor.z, root.getYRot(), root.getXRot());
                player.serverLevel().addFreshEntity(enemy);
            }
            enemy.setTurnBattleDifficultyMultiplier(DifficultyManager.getCurrentTotalMultiplierForLevel(player.serverLevel()));
            Set<Integer> states = new HashSet<>(BSOriginalEnemyData.get(member.enemyId()).initialStates());
            if (!RUNNER_TROOPS.contains(troop.id()) && specialized.troopId() == troop.id() && index < specialized.members().size()
                    && specialized.members().get(index).profileId() == member.enemyId()) {
                states.addAll(specialized.members().get(index).states());
            }
            session.enemyIds.add(enemy.getUUID());
            session.originalTroopMembers.add(enemy.getUUID());
            session.enemyAnchors.put(enemy.getUUID(), enemy.position());
            session.enemyStates.put(enemy.getUUID(), states);
            if (member.hidden()) {
                session.hiddenEnemyIds.add(enemy.getUUID());
            }
            session.originalState.initializeStates(enemy, player.getRandom());
            session.enemyTurns.put(enemy.getUUID(), 1);
            prepareEnemy(player, enemy, session, index != rootIndex || member.hidden());
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
        session.hiddenEnemyIds.clear();
        session.originalTroopMembers.clear();
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
            if (virtual) {
                enemy.setProfileId(member.profileId());
            } else {
                enemy.transformTurnBattleProfile(member.profileId());
            }
            enemy.setTurnBattleDifficultyMultiplier(
                    DifficultyManager.getCurrentTotalMultiplierForLevel(player.serverLevel()));
            if (virtual) {
                enemy.setTurnBattleHealth(enemy.getTurnBattleMaxHealth());
            }
            Set<Integer> states = new HashSet<>(
                    BSOriginalEnemyData.get(member.profileId()).initialStates());
            states.addAll(member.states());
            session.enemyIds.add(enemy.getUUID());
            if (session.originalTroopId > 0) {
                session.originalTroopMembers.add(enemy.getUUID());
            }
            session.enemyAnchors.put(enemy.getUUID(), enemy.position());
            session.enemyStates.put(enemy.getUUID(), states);
            session.originalState.initializeStates(enemy, player.getRandom());
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
        if (session.preemptive) {
            session.preemptive = false;
            session.enemySequenceCountsAsRound = true;
            session.roundEffectSnapshots.clear();
            presentNextEnemyAction(player, stats, session);
            return;
        }
        session.enemyActionQueue.clear();
        session.roundEffectSnapshots.clear();
        for (UUID memberId : session.partyMembers) {
            ServerPlayer member = player.server.getPlayerList().getPlayer(memberId);
            if (member != null && !session.downedMembers.contains(memberId)) {
                session.roundEffectSnapshots.put(memberId, captureBattleEffects(member));
            }
        }
        session.enemySequenceCountsAsRound = true;
        List<EntityTurnBattleMonster> enemies = getEnemies(player, session);
        for (EntityTurnBattleMonster enemy : enemies) {
            if (enemy.isTurnBattleDefeated() || isDormantGranStage(enemy)) {
                continue;
            }
            session.roundEffectSnapshots.put(enemy.getUUID(), captureBattleEffects(enemy));
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
                    ? EnemyActionResources.rollActionCount(player.getRandom(), session.originalState.features(enemy),
                    originalEnemy.getProfile().actionCount()) : enemy.rollTurnBattleActionCount(player.getRandom());
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
                    || session.hiddenEnemyIds.contains(battleEnemy.getUUID())
                    || battleEnemy.isTurnBattleDefeated() || isDormantGranStage(battleEnemy)) {
                continue;
            }
            if (session.troopRunner != null && !pending.enemyId().equals(session.troopEnemyBatch)) {
                session.troopEnemyBatch = pending.enemyId();
                session.troopRunner.increaseTurn();
                session.enemyActionQueue.addFirst(pending);
                ServerPlayer actorPlayer = player;
                BSPlayerStats actorStats = stats;
                runTroopEvents(player, session, () -> presentNextEnemyAction(actorPlayer, actorStats, session), false);
                return;
            }
            session.actingEnemyIndex = visibleEnemyIndex(player, session, battleEnemy);
            int playerHitCountBefore = session.playerHits.size();
            EnemyActionResult result = performEnemyAction(player, battleEnemy, stats,
                    session.enemySequenceGuard, session, pending.skillId());
            session.troopEnemyBatchEnding = pending.advanceTurnAfter() && result.followUpSkillId() <= 0;
            if (session.troopEnemyBatchEnding) session.troopEnemyBatch = null;
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
            broadcastState(session, true, Component.literal(text), false,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            return;
        }
        session.enemySequenceActive = false;
        session.awaitingPhasePresentation = false;
        session.presentationDeadline = 0L;
        if (session.enemySequenceCountsAsRound) {
            session.enemyTurn++;
            for (UUID memberId : session.partyMembers) {
                ServerPlayer member = player.server.getPlayerList().getPlayer(memberId);
                if (member == null || session.downedMembers.contains(memberId)) {
                    continue;
                }
                BSPlayerStats memberStats = member.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
                if (memberStats != null) {
                    restoreTurnBattleMana(member, memberStats, session);
                }
                advanceBattleEffects(member, session.roundEffectSnapshots.getOrDefault(memberId, Map.of()), session);
                session.originalState.advanceRound(member);
                refreshOriginalParameters(member, session);
                if ((member.getHealth() <= 0.0F || !member.isAlive()) && session.partyMembers.size() > 1) {
                    stabilizeDownedPlayer(member, session);
                }
                StatEventHandler.syncToClient(member);
            }
            for (EntityTurnBattleMonster enemy : getEnemies(player, session)) {
                if (!enemy.isTurnBattleDefeated() && !isDormantGranStage(enemy)) {
                    advanceBattleEffects(enemy, session.roundEffectSnapshots.getOrDefault(enemy.getUUID(), Map.of()), session);
                    double regenerated = enemy.getTurnBattleMaxHealth() * session.originalState.xparam(enemy, 7);
                    enemy.setTurnBattleHealth(enemy.getTurnBattleHealth() + (long) regenerated);
                    enemy.setTurnBattleMana(enemy.getTurnBattleMana() + (long) (enemy.getTurnBattleMaxMana()
                            * session.originalState.xparam(enemy, 8)));
                    session.originalState.advanceRound(enemy);
                    refreshOriginalParameters(enemy, session);
                }
                if (enemy instanceof EntityOriginalDatabaseEnemy original) {
                    original.advanceTurnBattleActionResources();
                }
            }
            session.headSearch.updateForms(getEnemies(player, session));
        }
        session.enemySequenceCountsAsRound = false;
        session.roundEffectSnapshots.clear();
        session.guardingPlayers.clear();
        ServerPlayer survivor = player.isAlive() && player.getHealth() > 0.0F
                && !session.downedMembers.contains(player.getUUID()) ? player : chooseEnemyTarget(player, session);
        if (survivor == null) {
            defeatParty(player, session, Component.literal("状态效果使队伍无法继续战斗。"));
            return;
        }
        player = survivor;
        stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        if (stats == null) {
            removeDefeatedCombatant(player, session);
            executeNextEnemyActionForParty(session);
            return;
        }
        if (session.troopRunner != null) {
            ServerPlayer survivingPlayer = player;
            BSPlayerStats survivingStats = stats;
            session.troopPlayerBatchOpen = false;
            runTroopEvents(player, session,
                    () -> finishEnemyRound(survivingPlayer, survivingStats, session), true);
            return;
        }
        finishEnemyRound(player, stats, session);
    }

    private static void finishEnemyRound(ServerPlayer player, BSPlayerStats stats, Session session) {
        session.pendingPartyActions.clear();
        session.partyActionQueue.clear();
        updateGranStage(player, session);
        EntityTurnBattleMonster rootEnemy = getRootEnemy(player, session);
        if (rootEnemy != null && canAdvanceEnemyPhase(player, rootEnemy, session)) {
            session.awaitingPhasePresentation = true;
            session.presentationDeadline = player.serverLevel().getGameTime() + 200L;
            session.presentationDriver = player.getUUID();
            broadcastState(session, true, Component.literal("状态效果改变了战况。"), false,
                    ClientboundTurnBattlePacket.Outcome.NONE, 0L);
            return;
        }
        if (allEnemiesDefeated(player, session)) {
            long reward = rewardVictory(player, stats, session);
            finish(player, session, ClientboundTurnBattlePacket.Outcome.VICTORY,
                    Component.literal("敌群被状态效果打倒了！"), true, reward);
            return;
        }
        session.remainingPlayerActions = rollPlayerActionCount(player, stats);
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
        boolean searching = forcedSkillId >= 0 && session.headSearch.blocksAction(battleEnemy);
        boolean restricted = forcedSkillId == 0 && session.originalState.restriction(battleEnemy) >= 4;
        BSOriginalEnemyData.Action action = null;
        if (!searching && !restricted && forcedSkillId >= 0 && battleEnemy instanceof EntityOriginalDatabaseEnemy original) {
            Set<Integer> states = session.enemyStates.computeIfAbsent(battleEnemy.getUUID(), ignored -> new HashSet<>());
            action = forcedSkillId > 0 ? original.getProfile().findAction(forcedSkillId)
                    : original.selectTurnBattleAction(player.getRandom(),
                    session.enemyTurns.getOrDefault(battleEnemy.getUUID(), 1), states,
                    session.originalState.sparam(battleEnemy, 4), session.commonEvents::switchValue,
                    candidate -> !session.originalState.hasFeature(battleEnemy, 44, candidate.skillId())
                            && !session.originalState.hasFeature(battleEnemy, 42,
                            BSOriginalSkillData.get(candidate.skillId()).skillTypeId())
                            && !(battleEnemy.hasEffect(BlackSouls.BUFF_SILENCE.get())
                            && BSOriginalSkillData.get(candidate.skillId()).skillTypeId() == 1)
                            && !(session.originalState.hasFeature(battleEnemy, 62, 0) && candidate.skillId() == 1));
        } else if (!searching && !restricted && forcedSkillId >= 0
                && battleEnemy instanceof EntityOriginalTurnBattleEnemy legacy
                && legacy.getProfile() == EntityOriginalTurnBattleEnemy.Profile.WEREWOLF) {
            action = BSOriginalEnemyData.get(legacy.getOriginalBattleProfileId())
                    .findAction(forcedSkillId > 0 ? forcedSkillId : 57);
        }
        EnemyActionResult result;
        if (searching) {
            session.lastEnemyAnimationId = 0;
            result = new EnemyActionResult(session.headSearch.actionMessage(battleEnemy), 0);
        } else if (restricted || forcedSkillId == 0 && action == null
                && battleEnemy instanceof EntityOriginalDatabaseEnemy) {
            session.lastEnemyAnimationId = 0;
            result = new EnemyActionResult(Component.literal(battleEnemy.getDisplayName().getString() + "无法行动！"), 0);
        } else {
            if (action != null && battleEnemy instanceof EntityOriginalDatabaseEnemy original) {
                original.useTurnBattleAction(action, session.originalState.sparam(battleEnemy, 4));
            }
            result = resolveEnemyAction(player, battleEnemy, stats, guard, session, forcedSkillId, action);
        }
        Component stateEnd = session.originalState.advanceAction(battleEnemy);
        refreshOriginalParameters(battleEnemy, session);
        Component recovered = session.headSearch.onActionEnd(battleEnemy);
        if (action != null) {
            session.headSearch.onSkillUsed(action.skillId(), getAllEnemies(player, session), player.getRandom());
            applyEnemyCommonEvents(player, action, session, result.selectedPlayerTargets());
        }
        if (!stateEnd.getString().isEmpty()) {
            result = new EnemyActionResult(Component.literal(result.message().getString() + "\n" + stateEnd.getString()),
                    result.followUpSkillId(), result.selectedPlayerTargets());
        }
        if (searching && !recovered.getString().isEmpty()) {
            return new EnemyActionResult(recovered, 0);
        }
        return recovered.getString().isEmpty() ? result
                : new EnemyActionResult(Component.literal(result.message().getString() + "\n" + recovered.getString()),
                result.followUpSkillId(), result.selectedPlayerTargets());
    }

    private static EnemyActionResult resolveEnemyAction(ServerPlayer player,
                                                        EntityTurnBattleMonster battleEnemy,
                                                        BSPlayerStats stats, boolean guard,
                                                        Session session, int forcedSkillId,
                                                        BSOriginalEnemyData.Action action) {
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
        EntityOriginalDatabaseEnemy originalEnemy = battleEnemy instanceof EntityOriginalDatabaseEnemy original
                ? original : null;
        if (action == null) {
            session.lastEnemyAnimationId = battleEnemy.getTurnBattleAttackAnimationId();
            String attackText = battleEnemy.getTurnBattleAttackText();
            if (player.getRandom().nextDouble() >= Mth.clamp(
                    0.95D * (1.0D - effectiveEvasion / 100.0D
                            - session.originalState.xparam(player, 1)), 0.0D, 1.0D)) {
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
                    damage /= 2.0D * Math.max(0.000001D, session.originalState.sparam(player, 1));
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

        return executeOriginalEnemySkill(player, battleEnemy, action, guard, session);
    }

    private static EnemyActionResult executeOriginalEnemySkill(ServerPlayer observer,
                                                               EntityTurnBattleMonster caster,
                                                               BSOriginalEnemyData.Action action,
                                                               boolean guard, Session session) {
        session.lastEnemyAnimationId = action.animationId();
        List<LivingEntity> allies = new ArrayList<>(getEnemies(observer, session));
        List<LivingEntity> opponents = new ArrayList<>(activePartyMembers(observer, session));
        if (opponents.isEmpty() && observer.isAlive() && !session.downedMembers.contains(observer.getUUID())) {
            opponents.add(observer);
        }
        List<LivingEntity> targets = EnemyTargetSelector.select(action.scope(), caster, allies, opponents,
                entity -> battleAlive(entity, session), entity -> battleTargetRate(entity, session), observer.getRandom());
        List<UUID> selectedPlayers = targets.stream().filter(ServerPlayer.class::isInstance)
                .map(LivingEntity::getUUID).distinct().toList();
        EnemyActionResult result = executeOriginalSkillTargets(observer, caster, action, targets, guard, session, true);
        return new EnemyActionResult(result.message(), result.followUpSkillId(), selectedPlayers);
    }

    private static EnemyActionResult executeOriginalSkillTargets(ServerPlayer observer, LivingEntity caster,
                                                                 BSOriginalEnemyData.Action action,
                                                                 List<LivingEntity> targets, boolean guard,
                                                                 Session session, boolean allowReactions) {
        BSOriginalSkillData.Entry metadata = BSOriginalReactionData.metadata(action.skillId());
        StringBuilder message = new StringBuilder(caster.getDisplayName().getString()).append(action.text()).append("！");
        int landed = 0;
        for (LivingEntity selectedTarget : targets) {
            for (int hit = 0; hit < Math.max(1, action.repeats()); hit++) {
                LivingEntity target = selectedTarget;
                if ((action.scope() != 9 && action.scope() != 10 && !battleAlive(target, session))
                        || !battleAlive(caster, session)) {
                    break;
                }
                boolean counter = allowReactions && rollTurnBattleCounter(observer, caster, selectedTarget,
                        action.hitType(), metadata, session);
                boolean reflected = allowReactions && !counter && rollTurnBattleReflection(observer,
                        selectedTarget, action.hitType(), session);
                if (reflected) {
                    target = caster;
                    message.append("\n").append(selectedTarget.getDisplayName().getString()).append("的魔法反射！");
                }
                double evasion = target instanceof ServerPlayer player
                        ? battlePlayerStats(player).evasion / 100.0D : 0.0D;
                double magicEvasion = target instanceof ServerPlayer player
                        ? battlePlayerStats(player).magicEvasion / 100.0D : 0.0D;
                evasion += session.originalState.xparam(target, 1);
                magicEvasion += session.originalState.xparam(target, 4);
                double hitRate = action.hitType() == 1 ? originalHitRate(caster, session) : 1.0D;
                double avoid = action.hitType() == 1 ? evasion : action.hitType() == 2 ? magicEvasion : 0.0D;
                double chance = Mth.clamp(action.successRate() / 100.0D * hitRate, 0.0D, 1.0D) * (1.0D - avoid);
                if (observer.getRandom().nextDouble() >= Mth.clamp(chance, 0.0D, 1.0D)) {
                    if (counter) {
                        message.append(executeTurnBattleCounter(observer, selectedTarget, caster, session));
                    }
                    continue;
                }
                landed++;
                double healthAtHitStart = battleHealth(target);
                double manaAtHitStart = battleMana(target);
                if (action.damageType() != 0) {
                    double value = Math.max(0.0D, TurnBattleFormulaEvaluator.evaluate(action.formula(),
                            originalFormulaContext(caster, target, observer, session)));
                    value *= originalElementRate(caster, target, action.elementId(), session);
                    if (action.hitType() == 1 || action.hitType() == 2) {
                        double rate = session.originalState.sparam(target, action.hitType() == 1 ? 6 : 7);
                        if (target instanceof ServerPlayer player) {
                            BSPlayerStats targetStats = battlePlayerStats(player);
                            rate *= action.hitType() == 1 ? targetStats.physicalDamageRate : targetStats.magicDamageRate;
                        }
                        value *= Math.max(0.0D, rate) * turnBattleDamageMultiplier(target, session);
                        if (target.hasEffect(BlackSouls.BUFF_FRAGILE.get())
                                && !session.originalState.hasState(target, 61)) {
                            value *= 1.5D;
                        }
                    }
                    if (action.damageType() == 3 || action.damageType() == 4) {
                        value *= session.originalState.sparam(target, 2);
                    }
                    double criticalRate = Math.max(0.0D, session.originalState.xparam(caster, 2)
                            + (caster instanceof ServerPlayer player ? battlePlayerStats(player).critRate / 100.0D : 0.0D))
                            * Math.max(0.0D, 1.0D - session.originalState.xparam(target, 3));
                    boolean critical = action.critical() && observer.getRandom().nextDouble() < criticalRate;
                    if (critical) {
                        value *= 3.0D;
                        message.append("\n会心一击！");
                    }
                    value = originalVariance(observer, value, action.variance());
                    boolean guarding = target instanceof ServerPlayer
                            ? session.guardingPlayers.contains(target.getUUID())
                            || guard && session.partyMembers.size() <= 1
                            : session.originalState.hasFeature(target, 62, 1);
                    double ignoreGuard = metadata == null ? 0.0D : metadata.guardIgnoreRate();
                    if (value > 0.0D && action.damageType() != 3 && action.damageType() != 4 && guarding
                            && (ignoreGuard <= 0.0D || observer.getRandom().nextDouble() >= ignoreGuard)) {
                        value /= 2.0D * Math.max(0.000001D, session.originalState.sparam(target, 1));
                    }
                    int amount = safeDamageInt(Math.floor(value));
                    double before = battleHealth(target);
                    switch (action.damageType()) {
                        case 1, 5 -> {
                            originalDamage(observer, target, amount, critical, session);
                            if (action.damageType() == 5) {
                                setBattleHealth(caster, battleHealth(caster)
                                        + Math.min(before, Math.max(0.0D, amount)), session);
                            }
                        }
                        case 2, 6 -> {
                            double lost = Math.min(battleMana(target), amount);
                            setBattleMana(target, battleMana(target) - lost);
                            if (action.damageType() == 6) {
                                setBattleMana(caster, battleMana(caster) + lost);
                            }
                        }
                        case 3 -> {
                            setBattleHealth(target, before + amount, session);
                        }
                        case 4 -> setBattleMana(target, battleMana(target) + amount);
                        default -> {
                        }
                    }
                }
                applyOriginalEffects(observer, caster, target, action, metadata, session, reflected);
                double restored = battleHealth(target) - healthAtHitStart;
                if (restored > 0.0D) {
                    message.append("\n").append(target.getDisplayName().getString()).append("恢复了 ")
                            .append(safeDamageInt(restored)).append(" 点生命！");
                }
                double manaChange = battleMana(target) - manaAtHitStart;
                if (manaChange != 0.0D) {
                    message.append("\n").append(target.getDisplayName().getString())
                            .append(manaChange > 0.0D ? "恢复了 " : "失去了 ")
                            .append(safeDamageInt(Math.abs(manaChange))).append(" MP！");
                }
                if (counter) {
                    message.append(executeTurnBattleCounter(observer, selectedTarget, caster, session));
                }
                if (target instanceof ServerPlayer player) {
                    StatEventHandler.syncToClient(player);
                }
            }
        }
        if (!targets.isEmpty() && landed == 0) {
            message.append("\n但是攻击落空了！");
        }
        return new EnemyActionResult(Component.literal(message.toString()),
                !battleAlive(caster, session) ? 0 : action.followUpSkillId());
    }

    private static boolean rollTurnBattleCounter(ServerPlayer observer, LivingEntity attacker,
                                                  LivingEntity defender, int hitType,
                                                  BSOriginalSkillData.Entry metadata, Session session) {
        if (!battleAlive(defender, session)
                || attacker instanceof Player == defender instanceof Player
                || metadata != null && metadata.note().contains("<反撃不可>")) {
            return false;
        }
        if (hitType != 1 && (metadata == null || !metadata.note().contains("<反撃可能>"))) {
            return false;
        }
        double rate = session.originalState.xparam(defender, 6);
        if (defender instanceof ServerPlayer player) {
            rate += StatEventHandler.getWeaponCounterRate(player) / 100.0D;
        }
        return rate > 0.0D && observer.getRandom().nextDouble() < Mth.clamp(rate, 0.0D, 1.0D);
    }

    private static boolean rollTurnBattleReflection(ServerPlayer observer, LivingEntity defender,
                                                     int hitType, Session session) {
        if (hitType != 2 || !battleAlive(defender, session)) {
            return false;
        }
        double rate = session.originalState.xparam(defender, 5);
        return rate > 0.0D && observer.getRandom().nextDouble() < Mth.clamp(rate, 0.0D, 1.0D);
    }

    private static String executeTurnBattleCounter(ServerPlayer observer, LivingEntity defender,
                                                    LivingEntity attacker, Session session) {
        if (!battleAlive(defender, session) || !battleAlive(attacker, session)) {
            return "";
        }
        int skillId = 1;
        if (defender instanceof EntityOriginalDatabaseEnemy enemy) {
            skillId = counterSkillId(BSOriginalCombatData.enemy(enemy.getProfileId()).note(), skillId);
        } else if (defender instanceof EntityOriginalTurnBattleEnemy enemy && enemy.getOriginalBattleProfileId() > 0) {
            skillId = counterSkillId(BSOriginalCombatData.enemy(enemy.getOriginalBattleProfileId()).note(), skillId);
        }
        for (int stateId : session.enemyStates.getOrDefault(defender.getUUID(), Set.of())) {
            BSOriginalCombatData.State state = BSOriginalCombatData.state(stateId);
            if (state != null) {
                skillId = counterSkillId(state.note(), skillId);
            }
        }
        BSOriginalEnemyData.Action counter = BSOriginalReactionData.action(skillId);
        if (counter == null) {
            throw new IllegalStateException("Missing original counter skill " + skillId);
        }
        if (defender instanceof EntityTurnBattleMonster enemy) {
            session.actingEnemyIndex = visibleEnemyIndex(observer, session, enemy);
            session.lastEnemyAnimationId = counter.animationId() < 0
                    ? enemy.getTurnBattleAttackAnimationId() : counter.animationId();
        }
        EnemyActionResult result = executeOriginalSkillTargets(observer, defender, counter,
                List.of(attacker), false, session, false);
        return "\n" + defender.getDisplayName().getString() + "的反击！\n" + result.message().getString();
    }

    private static int counterSkillId(String note, int current) {
        Matcher matcher = COUNTER_SKILL.matcher(note);
        while (matcher.find()) {
            current = Math.max(current, Integer.parseInt(matcher.group(1)));
        }
        return current;
    }

    private static int playerSkillHitType(AbstractSkill skill) {
        if (skill instanceof SkillOriginalMagic magic) {
            return magic.getTurnBattleHitType();
        }
        return BSOriginalReactionData.playerHitType(skill.getSkillId(), skill instanceof WeaponSkill ? 1 : 0);
    }

    private static BSOriginalSkillData.Entry playerReactionMetadata(AbstractSkill skill) {
        return skill instanceof SkillOriginalMagic magic
                ? BSOriginalReactionData.metadata(magic.getTurnBattleSourceSkillId())
                : BSOriginalReactionData.playerMetadata(skill.getSkillId());
    }

    private static void applyOriginalEffects(ServerPlayer observer, LivingEntity caster,
                                             LivingEntity target, BSOriginalEnemyData.Action action,
                                             BSOriginalSkillData.Entry metadata, Session session, boolean reflected) {
        List<BSOriginalSkillData.Effect> effects = metadata == null ? action.stateEffects().stream()
                .map(effect -> new BSOriginalSkillData.Effect(effect.code(), effect.stateId(), effect.chance(), 0.0D))
                .toList() : metadata.effects();
        for (BSOriginalSkillData.Effect effect : effects) {
            switch (effect.code()) {
                case 11 -> {
                    double amount = (battleMaximumHealth(target) * effect.value1() + effect.value2())
                            * session.originalState.sparam(target, 2);
                    if (amount < 0.0D) {
                        originalDamage(observer, target, safeDamageInt(Math.floor(-amount)), false, session);
                    } else {
                        setBattleHealth(target, battleHealth(target) + Math.floor(amount), session);
                    }
                }
                case 12 -> {
                    double amount = (battleMaximumMana(target) * effect.value1() + effect.value2())
                            * session.originalState.sparam(target, 2);
                    setBattleMana(target, battleMana(target) + (long) amount);
                }
                case 21 -> {
                    if (effect.dataId() == 0) {
                        Map<Integer, Double> attackStates = new HashMap<>();
                        for (BSOriginalCombatData.Feature feature : session.originalState.features(caster)) {
                            if (feature.code() == 32) {
                                attackStates.merge(feature.dataId(), feature.value(), Double::sum);
                            }
                        }
                        attackStates.forEach((stateId, rate) -> addOriginalState(observer, caster, target, stateId,
                                effect.value1() * rate, action.hitType(), metadata, session));
                    } else {
                        addOriginalState(observer, caster, target, effect.dataId(), effect.value1(),
                                action.hitType(), metadata, session, reflected);
                    }
                }
                case 22 -> {
                    if (observer.getRandom().nextDouble() < effect.value1()) {
                        session.originalState.removeState(target, effect.dataId());
                        MobEffect mapped = originalPlayerStateEffect(effect.dataId());
                        if (mapped != null) {
                            target.removeEffect(mapped);
                        }
                    }
                }
                case 31 -> session.originalState.addBuff(target, effect.dataId(), (int) effect.value1(), false);
                case 32 -> {
                    boolean forced = metadata != null && metadata.note().contains("<強制付与>");
                    double chance = forced || metadata != null && metadata.note().contains("有効度無視弱体付与")
                            ? 1.0D : session.originalState.debuffRate(target, effect.dataId());
                    if (!forced && (metadata == null || !metadata.note().contains("運無視弱体付与"))) {
                        chance *= originalLuckRate(caster, target, observer, session);
                    }
                    if (observer.getRandom().nextDouble() < chance) {
                        session.originalState.addBuff(target, effect.dataId(), (int) effect.value1(), true);
                    }
                }
                case 33 -> session.originalState.removeBuff(target, effect.dataId(), false);
                case 34 -> session.originalState.removeBuff(target, effect.dataId(), true);
                case 41 -> {
                    if (effect.dataId() == 0 && target instanceof EntityTurnBattleMonster enemy) {
                        session.escapedEnemyIds.add(enemy.getUUID());
                        enemy.setTurnBattleHealth(0.0D);
                    }
                }
                default -> {
                }
            }
            refreshOriginalParameters(target, session);
        }
    }

    private static void addOriginalState(ServerPlayer observer, LivingEntity caster, LivingEntity target,
                                          int stateId, double probability, int hitType,
                                          BSOriginalSkillData.Entry metadata, Session session) {
        addOriginalState(observer, caster, target, stateId, probability, hitType, metadata, session, false);
    }

    private static void addOriginalState(ServerPlayer observer, LivingEntity caster, LivingEntity target,
                                          int stateId, double probability, int hitType,
                                          BSOriginalSkillData.Entry metadata, Session session, boolean reflected) {
        boolean opposite = caster instanceof EntityTurnBattleMonster != target instanceof EntityTurnBattleMonster
                || reflected && caster == target;
        double chance = probability;
        boolean forced = metadata != null && metadata.note().contains("<強制付与>");
        if (opposite) {
            if (!forced && (metadata == null || !metadata.note().contains("有効度無視ステート付与"))) {
                chance *= session.originalState.stateRate(target, stateId);
            }
            if (!forced && (metadata == null || !metadata.note().contains("運無視ステート付与"))) {
                chance *= originalLuckRate(caster, target, observer, session);
            }
        }
        if (target instanceof ServerPlayer player) {
            BSPlayerStats stats = battlePlayerStats(player);
            double resistance = switch (stateId) {
                case 2 -> stats.poisonResistRate;
                case 3 -> stats.severePoisonResistRate;
                case 6 -> stats.sleepResistRate;
                case 26 -> stats.bleedResistRate;
                case 56 -> stats.fearResistRate;
                default -> 0.0D;
            };
            if (!forced && (metadata == null || !metadata.note().contains("有効度無視ステート付与"))) {
                chance *= Math.max(0.0D, 1.0D - resistance);
            }
        }
        if (session.originalState.hasFeature(target, 14, stateId)
                || observer.getRandom().nextDouble() >= chance) {
            return;
        }
        if (stateId == 1) {
            originalDamage(observer, target, safeDamageInt(battleHealth(target)), false, session);
            return;
        }
        MobEffect mapped = originalPlayerStateEffect(stateId);
        if (mapped != null && target instanceof ServerPlayer) {
            BSOriginalCombatData.State state = BSOriginalCombatData.state(stateId);
            int turns = state == null ? 2 : state.minTurns()
                    + observer.getRandom().nextInt(Math.max(1, state.maxTurns() - state.minTurns() + 1));
            target.addEffect(new MobEffectInstance(mapped,
                    state != null && state.autoRemovalTiming() == 0 ? -1 : Math.max(1, turns) * EFFECT_TICKS_PER_ACTION,
                    originalPlayerStateAmplifier(stateId), false, true, true));
        } else {
            session.originalState.addStateDirect(target, stateId, observer.getRandom());
        }
    }

    private static void applyEnemyCommonEvents(ServerPlayer observer, BSOriginalEnemyData.Action action, Session session,
                                               List<UUID> selectedPlayerTargets) {
        BSOriginalSkillData.Entry skill = BSOriginalSkillData.get(action.skillId());
        if (skill == null) {
            return;
        }
        List<EntityTurnBattleMonster> enemies = getAllEnemies(observer, session);
        for (BSOriginalSkillData.Effect effect : skill.effects()) {
            if (effect.code() != 44 || effect.dataId() == 44) {
                continue;
            }
            if ((effect.dataId() == 72 || effect.dataId() == 344) && (session.partyMembers.stream()
                    .map(id -> observer.server.getPlayerList().getPlayer(id))
                    .noneMatch(member -> member != null && battleAlive(member, session))
                    || allEnemiesDefeated(observer, session))) {
                continue;
            }
            List<ServerPlayer> affectedPlayers = selectedPlayerTargets.stream()
                    .map(id -> observer.server.getPlayerList().getPlayer(id))
                    .filter(member -> member != null && session.partyMembers.contains(member.getUUID())
                            && member.serverLevel() == observer.serverLevel() && !member.isSpectator()).toList();
            session.commonEvents.execute(effect.dataId(), new EnemyCommonEvents.Context() {
                @Override
                public void setInformationEnabled(boolean enabled) {
                    OriginalNpcProgress.get(observer.server).setSwitch(22, enabled);
                }

                @Override
                public void operatePlayerVariable(int variableId, int operation, long operand) {
                    if (variableId != 38 || operation != 0 || operand != 100) {
                        throw new UnsupportedOperationException("enemy_sen_assignment");
                    }
                    for (ServerPlayer member : affectedPlayers) {
                        long before = OriginalNpcProgress.readVariable(member, variableId);
                        long value = com.BlackSouls.BlackSoulsMod.story.OriginalVariableOperation.apply(before, operation, operand);
                        OriginalNpcProgress.writeVariable(member, variableId, value);
                        com.BlackSouls.BlackSoulsMod.story.OriginalSenService.afterVariableOperation(member, operation, operand,
                                before, OriginalNpcProgress.readVariable(member, variableId));
                    }
                }

                @Override
                public boolean hasEnemyState(int enemyIndex, int stateId) {
                    return enemyIndex >= 0 && enemyIndex < enemies.size()
                            && session.originalState.hasState(enemies.get(enemyIndex), stateId);
                }

                @Override
                public void changeEnemyState(int enemyIndex, boolean remove, int stateId) {
                    List<EntityTurnBattleMonster> targets = enemyIndex == -1 ? enemies
                            : enemyIndex >= 0 && enemyIndex < enemies.size() ? List.of(enemies.get(enemyIndex)) : List.of();
                    for (EntityTurnBattleMonster target : targets) {
                        if (remove) {
                            session.originalState.removeState(target, stateId);
                        } else if (!session.originalState.hasFeature(target, 14, stateId)) {
                            if (stateId == 1) {
                                target.setTurnBattleHealth(0.0D);
                            } else {
                                session.originalState.addStateDirect(target, stateId, observer.getRandom());
                            }
                        }
                        refreshOriginalParameters(target, session);
                    }
                }

                @Override
                public void changeEnemyHealth(int enemyIndex, long amount, boolean allowDeath) {
                    List<EntityTurnBattleMonster> targets = enemyIndex == -1 ? enemies
                            : enemyIndex >= 0 && enemyIndex < enemies.size() ? List.of(enemies.get(enemyIndex)) : List.of();
                    for (EntityTurnBattleMonster target : targets) {
                        if (target.isTurnBattleDefeated()) continue;
                        double hp = target.getTurnBattleHealth() + amount;
                        target.setTurnBattleHealth(allowDeath ? hp : Math.max(1.0D, hp));
                        if (target.isTurnBattleDefeated()) {
                            session.enemyStates.computeIfAbsent(target.getUUID(), ignored -> new HashSet<>()).add(1);
                        }
                    }
                }
            });
        }
    }

    private static double originalLuckRate(LivingEntity caster, LivingEntity target, ServerPlayer observer, Session session) {
        return Math.max(1.0D + (battleParameter(caster, 7, observer, session)
                - battleParameter(target, 7, observer, session)) * 0.001D, 0.0D);
    }

    private static TurnBattleFormulaEvaluator.Context originalFormulaContext(LivingEntity caster, LivingEntity target,
                                                                             ServerPlayer observer, Session session) {
        return new TurnBattleFormulaEvaluator.Context(
                battleParameter(caster, 2, observer, session), battleParameter(caster, 3, observer, session),
                battleParameter(caster, 4, observer, session), battleParameter(caster, 5, observer, session),
                battleParameter(caster, 6, observer, session), battleParameter(caster, 7, observer, session),
                battleHealth(caster), battleMaximumHealth(caster), battleMana(caster), battleMaximumMana(caster),
                battleParameter(target, 2, observer, session), battleParameter(target, 3, observer, session),
                battleParameter(target, 4, observer, session), battleParameter(target, 5, observer, session),
                battleParameter(target, 6, observer, session), battleParameter(target, 7, observer, session),
                battleHealth(target), battleMaximumHealth(target), battleMana(target), battleMaximumMana(target),
                stateId -> session.originalState.hasState(target, stateId)
                        || originalPlayerStateEffect(stateId) != null && target.hasEffect(originalPlayerStateEffect(stateId)));
    }

    private static double battleParameter(LivingEntity entity, int parameter, ServerPlayer observer, Session session) {
        double value;
        if (entity instanceof ServerPlayer player) {
            BSPlayerStats stats = battlePlayerStats(player);
            value = switch (parameter) {
                case 0 -> player.getMaxHealth();
                case 1 -> stats.maxMp;
                case 2 -> stats.attack * domainAttackRate(session);
                case 3 -> stats.defense * domainDefenseRate(session);
                case 4 -> stats.magicAttack * domainMagicRate(session);
                case 5 -> stats.magicDefense * domainMagicDefenseRate(session);
                case 6 -> stats.speed;
                case 7 -> stats.luck;
                default -> 0.0D;
            };
        } else if (entity instanceof EntityTurnBattleMonster enemy) {
            value = switch (parameter) {
                case 0 -> enemy.getTurnBattleMaxHealth();
                case 1 -> enemy.getTurnBattleMaxMana();
                case 2 -> enemy.getTurnBattleAttack() * StatEventHandler.getAttackShiftMultiplier(enemy);
                case 3 -> enemy.getTurnBattleDefense() * StatEventHandler.getDefenseShiftMultiplier(enemy);
                case 4 -> enemy.getTurnBattleMagicAttack();
                case 5 -> enemy.getTurnBattleMagicDefense();
                case 6 -> enemy.getTurnBattleAgility();
                case 7 -> enemy.getTurnBattleLuck();
                default -> 0.0D;
            };
            if (parameter >= 2) {
                value = DifficultyManager.scaleManagedStat(enemy.level(), value);
            }
        } else {
            value = 0.0D;
        }
        return Math.max(parameter == 1 ? 0.0D : 1.0D,
                Math.floor(value * (parameter <= 1 ? 1.0D : session.originalState.parameterRate(entity, parameter))));
    }

    private static void refreshOriginalParameters(LivingEntity entity, Session session) {
        double healthRate = session.originalState.parameterRate(entity, 0);
        double manaRate = session.originalState.parameterRate(entity, 1);
        if (entity instanceof EntityOriginalDatabaseEnemy original) {
            original.setTurnBattleParameterRates(healthRate, manaRate);
        } else if (entity instanceof ServerPlayer player) {
            net.minecraft.world.entity.ai.attributes.AttributeInstance attribute = entity.getAttribute(
                    net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (attribute != null) {
                attribute.removeModifier(ORIGINAL_HEALTH_RATE);
                if (healthRate != 1.0D) {
                    attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            ORIGINAL_HEALTH_RATE, "Original battle maximum health", healthRate - 1.0D,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                entity.setHealth(Math.min(entity.getHealth(), entity.getMaxHealth()));
            }
            BSPlayerStats stats = battlePlayerStats(player);
            Double applied = session.playerAppliedMaxMana.get(player.getUUID());
            double base = applied != null && Math.abs(stats.maxMp - applied) < 0.000001D
                    ? session.playerBaseMaxMana.getOrDefault(player.getUUID(), stats.maxMp) : stats.maxMp;
            stats.maxMp = Math.max(0.0D, Math.floor(base * manaRate));
            stats.mp = Math.min(stats.mp, stats.maxMp);
            if (manaRate == 1.0D) {
                session.playerBaseMaxMana.remove(player.getUUID());
                session.playerAppliedMaxMana.remove(player.getUUID());
            } else {
                session.playerBaseMaxMana.put(player.getUUID(), base);
                session.playerAppliedMaxMana.put(player.getUUID(), stats.maxMp);
            }
        }
    }

    private static double originalElementRate(LivingEntity caster, LivingEntity target, int elementId, Session session) {
        if (elementId < 0) {
            if (caster instanceof ServerPlayer player) {
                return BSAttributeManager.getBestMultiplier(target,
                        StatEventHandler.buildPlayerAttackAttributes(player, battlePlayerStats(player)))
                        * session.originalState.elementRate(target, 1);
            }
            return session.originalState.features(caster).stream().filter(feature -> feature.code() == 31)
                    .mapToDouble(feature -> originalElementRate(caster, target, feature.dataId(), session))
                    .max().orElse(1.0D);
        }
        double rate = session.originalState.elementRate(target, elementId);
        if (target instanceof ServerPlayer) {
            String attribute = switch (elementId) {
                case 1 -> BSAttributeManager.PHYSICAL;
                case 3 -> BSAttributeManager.FIRE;
                case 4 -> BSAttributeManager.ICE;
                case 5 -> BSAttributeManager.THUNDER;
                case 9 -> BSAttributeManager.LIGHT;
                case 10 -> BSAttributeManager.DARK;
                default -> BSAttributeManager.TYPELESS;
            };
            rate *= BSAttributeManager.getResistance(target, attribute);
        }
        return rate;
    }

    private static double originalVariance(ServerPlayer player, double value, int variance) {
        int amplitude = (int) Math.min(Integer.MAX_VALUE - 1L, Math.floor(Math.abs(value) * variance / 100.0D));
        return value + player.getRandom().nextInt(amplitude + 1) + player.getRandom().nextInt(amplitude + 1) - amplitude;
    }

    private static BSPlayerStats battlePlayerStats(ServerPlayer player) {
        return player.getCapability(BSPlayerStats.CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("Missing turn battle player stats"));
    }

    private static boolean battleAlive(LivingEntity entity, Session session) {
        return entity instanceof EntityTurnBattleMonster enemy ? !enemy.isTurnBattleDefeated()
                && !session.escapedEnemyIds.contains(enemy.getUUID())
                : entity.isAlive() && entity.getHealth() > 0.0F && !session.downedMembers.contains(entity.getUUID());
    }

    private static double battleTargetRate(LivingEntity entity, Session session) {
        double rate = session.originalState.sparam(entity, 0);
        return entity instanceof ServerPlayer player ? rate * battlePlayerStats(player).targetingRate : rate;
    }

    private static double battleHealth(LivingEntity entity) {
        return entity instanceof EntityTurnBattleMonster enemy ? enemy.getTurnBattleHealth() : entity.getHealth();
    }

    private static double battleMaximumHealth(LivingEntity entity) {
        return entity instanceof EntityTurnBattleMonster enemy ? enemy.getTurnBattleMaxHealth() : entity.getMaxHealth();
    }

    private static double battleMana(LivingEntity entity) {
        return entity instanceof EntityTurnBattleMonster enemy ? enemy.getTurnBattleMana()
                : entity instanceof ServerPlayer player ? battlePlayerStats(player).mp : 0.0D;
    }

    private static double battleMaximumMana(LivingEntity entity) {
        return entity instanceof EntityTurnBattleMonster enemy ? enemy.getTurnBattleMaxMana()
                : entity instanceof ServerPlayer player ? battlePlayerStats(player).maxMp : 0.0D;
    }

    private static void setBattleMana(LivingEntity entity, double mana) {
        if (entity instanceof EntityTurnBattleMonster enemy) {
            enemy.setTurnBattleMana(mana);
        } else if (entity instanceof ServerPlayer player) {
            BSPlayerStats stats = battlePlayerStats(player);
            stats.mp = Math.max(0.0D, Math.min(stats.maxMp, mana));
        }
    }

    private static void setBattleHealth(LivingEntity entity, double health, Session session) {
        if (entity instanceof EntityTurnBattleMonster enemy) {
            enemy.setTurnBattleHealth(health);
        } else {
            entity.setHealth((float) Math.max(0.0D, Math.min(entity.getMaxHealth(), health)));
            if (health > 0.0D) {
                session.downedMembers.remove(entity.getUUID());
            }
        }
    }

    private static void originalDamage(ServerPlayer observer, LivingEntity target, int damage,
                                        boolean critical, Session session) {
        if (target instanceof ServerPlayer player) {
            applyIncomingHit(session, player, damage, critical);
        } else if (target instanceof EntityTurnBattleMonster enemy) {
            enemy.setTurnBattleHealth(enemy.getTurnBattleHealth() - damage);
            session.playerHits.add(new ClientboundTurnBattlePacket.DamageHit(enemy.getId(), damage, critical, 0));
        }
        if (damage > 0) {
            session.originalState.onDamage(target, observer.getRandom());
            refreshOriginalParameters(target, session);
        }
    }

    private static double enemyActionHitChance(BSOriginalEnemyData.Action action,
                                               double evasion, double magicEvasion, boolean roldEffect) {
        if (roldEffect && action.hitType() == 1) {
            return 1.0D;
        }
        double actionEvasion = action.hitType() == 1 ? evasion
                : action.hitType() == 2 ? magicEvasion : 0.0D;
        return Mth.clamp(action.successRate() / 100.0D, 0.0D, 1.0D)
                * (1.0D - Mth.clamp(actionEvasion / 100.0D, 0.0D, 1.0D));
    }

    private static CounterHit tryTurnBattleCounter(ServerPlayer player,
                                                   EntityTurnBattleMonster enemy,
                                                   BSPlayerStats stats, Session session,
                                                   int wave) {
        if (!player.isAlive()) {
            return null;
        }
        double counterRate = StatEventHandler.getWeaponCounterRate(player) + session.originalState.xparam(player, 6) * 100.0D;
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
        damage *= turnBattleDamageMultiplier(enemy, session) * session.originalState.sparam(enemy, 6);
        int dealt = Math.max(1, safeDamageInt(damage));
        enemy.setTurnBattleHealth(enemy.getTurnBattleHealth() - dealt);
        session.playerHits.add(new ClientboundTurnBattlePacket.DamageHit(
                enemy.getId(), dealt, critical, wave));
        return new CounterHit(dealt, critical);
    }

    private static double turnBattleDamageMultiplier(LivingEntity target, Session session) {
        if (session.originalState.hasState(target, 7) || session.originalState.hasState(target, 8)) {
            return 1.0D;
        }
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
                        player, damage, session.partyMembers.size() > 1 || session.canLose);
        ClientboundTurnBattlePacket.IncomingHit incoming = new ClientboundTurnBattlePacket.IncomingHit(
                result.damage(), critical, result.knockedDown(),
                result.revived(), result.reviveHealth());
        session.incomingHits.add(incoming);
        session.incomingHitsByPlayer.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>()).add(incoming);
        if (result.knockedDown() && !result.revived() && (session.partyMembers.size() > 1 || session.canLose)) {
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
        if (!session.canLose) {
            for (ServerPlayer member : members) {
                member.setHealth(0.0F);
                member.hurtMarked = true;
            }
        }
        finish(player, session, ClientboundTurnBattlePacket.Outcome.DEFEAT, message, false, 0L);
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

    private static void restoreTurnBattleMana(ServerPlayer player, BSPlayerStats stats, Session session) {
        double rate = (stats == null ? 0.0D : stats.mpRegenRate) + session.originalState.xparam(player, 8);
        if (stats == null || stats.maxMp <= 0.0D || rate <= 0.0D) {
            return;
        }
        double restored = Math.floor(stats.maxMp * rate);
        if (restored > 0.0D) {
            stats.mp = Math.min(stats.maxMp, stats.mp + restored);
        }
    }

    private static void restoreTurnBattleHealth(ServerPlayer player, BSPlayerStats stats,
                                                double damageRate, Session session) {
        if (!player.isAlive() || session.downedMembers.contains(player.getUUID())) {
            return;
        }
        double recoveryRate = (stats == null ? 0.0D : stats.hpRegenRate)
                + session.originalState.xparam(player, 7);
        double netRate = recoveryRate - damageRate * StatEventHandler.getPercentageDamageMultiplier(player);
        long amount = (long) (player.getMaxHealth() * netRate);
        if (amount > 0L && player.getHealth() < player.getMaxHealth()) {
            player.heal((float) amount);
        } else if (amount < 0L) {
            SkillEventHandler.TurnBattleDamageResult result = SkillEventHandler.applyTurnBattleDamageDetailed(
                    player, safeDamageInt(-(double) amount), session.partyMembers.size() > 1 || session.canLose);
            if (result.knockedDown() && !result.revived() && (session.partyMembers.size() > 1 || session.canLose)) {
                stabilizeDownedPlayer(player, session);
            }
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
            if ((effect.code() != 21 && effect.code() != 22) || effect.stateId() <= 0) {
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
            if (effect.code() == 22) {
                changed |= player.removeEffect(mobEffect);
            } else {
                changed |= player.addEffect(new MobEffectInstance(mobEffect,
                        originalPlayerStateDuration(effect.stateId()),
                        originalPlayerStateAmplifier(effect.stateId()), false, true, true));
            }
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
        int goldRate = originalPartyAbility(player, session, 4) ? 2 : 1;
        int dropRate = originalPartyAbility(player, session, 5) ? 2 : 1;
        session.rewardItems.clear();
        for (EntityTurnBattleMonster enemy : getEnemies(player, session)) {
            if (session.escapedEnemyIds.contains(enemy.getUUID())) {
                continue;
            }
            soulReward += DifficultyManager.scaleManagedSoulReward(
                    enemy.level(), enemy.getTurnBattleSoulReward()) * goldRate;
            for (ItemStack drop : enemy.rollTurnBattleDrops(player.getRandom(), dropRate)) {
                mergeRewardItem(session.rewardItems, drop);
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
            }
        }
        stats.souls += soulReward;
        boolean anyDefeated = getEnemies(player, session).stream()
                .anyMatch(enemy -> !session.escapedEnemyIds.contains(enemy.getUUID()));
        if (anyDefeated && player.getRandom().nextInt(5) == 0) {
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
        if (session.finished) {
            return;
        }
        session.finished = true;
        if (session.canLose && outcome == ClientboundTurnBattlePacket.Outcome.DEFEAT) {
            for (UUID id : session.partyMembers) {
                ServerPlayer member = id.equals(player.getUUID()) ? player : player.server.getPlayerList().getPlayer(id);
                if (member != null && (session.downedMembers.contains(id) || !member.isAlive())) {
                    member.setHealth(1.0F);
                    member.hurtMarked = true;
                }
            }
        }
        session.headSearch.clear(getAllEnemies(player, session));
        List<LivingEntity> combatants = new ArrayList<>(getAllEnemies(player, session));
        for (UUID memberId : session.partyMembers) {
            ServerPlayer member = player.server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                combatants.add(member);
            }
        }
        session.originalState.clear(combatants);
        for (LivingEntity entity : combatants) {
            refreshOriginalParameters(entity, session);
            if (entity instanceof ServerPlayer member) {
                StatEventHandler.syncToClient(member);
            }
            if (entity instanceof EntityOriginalDatabaseEnemy original) {
                original.resetTurnBattleActionResources();
                original.setTurnBattleParameterRates(1.0D, 1.0D);
            }
        }
        EntityTurnBattleMonster rootEnemy = getRootEnemy(player, session);
        if (outcome == ClientboundTurnBattlePacket.Outcome.VICTORY
                && rootEnemy instanceof EntityRabbitKnight) {
            message = Component.literal(message.getString() + "\n" + RabbitKnightDialogue.deathLine());
        }
        player.fallDistance = 0.0F;
        player.setDeltaMovement(Vec3.ZERO);
        sendState(player, false, message, false, outcome, soulReward, true, true, false);
        if (outcome == ClientboundTurnBattlePacket.Outcome.VICTORY && rootEnemy != null
                && !session.escapedEnemyIds.contains(rootEnemy.getUUID())
                && !session.hiddenEnemyIds.contains(rootEnemy.getUUID())) {
            SceneSpawnerBossHandler.markDefeated(rootEnemy, player);
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
                sendState(member, false, message, false, outcome, 0L, true, true, false);
            }
            PLAYER_SESSIONS.remove(memberId);
        }
        for (EntityTurnBattleMonster enemy : getAllEnemies(player, session)) {
            enemy.setInvulnerable(false);
            enemy.setDeltaMovement(Vec3.ZERO);
            ENEMY_SESSIONS.remove(enemy.getUUID());
            if (removeEnemy || session.originalTroopId > 0 || !enemy.getUUID().equals(session.rootEnemyId)) {
                enemy.discard();
            }
        }
        if (!removeEnemy && session.originalTroopId <= 0 && rootEnemy != null) {
            rootEnemy.setInvisible(false);
            rootEnemy.setSilent(false);
            rootEnemy.teleportTo(session.enemyAnchor.x, session.enemyAnchor.y, session.enemyAnchor.z);
            rootEnemy.setBattleCooldown(ESCAPE_GRACE_TICKS);
        }
        session.downedMembers.clear();
        session.partyMembers.clear();
        clearTransientState(session);
        if (session.onFinished != null) {
            session.onFinished.accept(outcome);
        }
    }

    private static void sendState(ServerPlayer player, boolean active,
                                  Component message, boolean canAct,
                                  ClientboundTurnBattlePacket.Outcome outcome, long soulReward) {
        Session session = PLAYER_SESSIONS.get(player.getUUID());
        sendState(player, active, message, canAct, outcome, soulReward, true, true,
                session == null || session.partyMembers.size() <= 1);
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
                    ? originalEnemy.getProfileId() : enemy instanceof EntityOriginalTurnBattleEnemy legacy
                    && legacy.getOriginalBattleProfileId() > 0 ? legacy.getOriginalBattleProfileId() : -1;
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
                session.incomingHitsByPlayer.getOrDefault(player.getUUID(), List.of()),
                message, canAct && !session.downedMembers.contains(player.getUUID()), outcome, soulReward,
                session.rewardItems, skillCooldowns(player, session), session.canEscape,
                session.originalTroopId, getEnemies(player, session).stream()
                .map(enemy -> session.originalTroopMembers.indexOf(enemy.getUUID())).toList())
                .withTroopPresentation(session.troopPresentation).withBattleMusic(session.battleMusic), player);
        if (clearTransientHits) clearTransientState(session);
    }

    private static void clearTransientState(Session session) {
        session.phaseChanged = false;
        session.playerHits.clear();
        session.incomingHits.clear();
        session.incomingHitsByPlayer.clear();
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
        clearTransientState(session);
    }

    private static EntityTurnBattleMonster getRootEnemy(ServerPlayer player, Session session) {
        if (session == null) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(session.rootEnemyId);
        return entity instanceof EntityTurnBattleMonster enemy ? enemy : null;
    }

    private static List<EntityTurnBattleMonster> getEnemies(ServerPlayer player, Session session) {
        List<EntityTurnBattleMonster> visible = getAllEnemies(player, session).stream()
                .filter(enemy -> !session.hiddenEnemyIds.contains(enemy.getUUID())).toList();
        if (session != null && session.originalTroopId > 0) {
            return visible.stream().sorted(java.util.Comparator.comparing(
                    enemy -> !enemy.getUUID().equals(session.rootEnemyId))).toList();
        }
        return visible;
    }

    private static List<EntityTurnBattleMonster> getAllEnemies(ServerPlayer player, Session session) {
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

    private static int visibleEnemyIndex(ServerPlayer player, Session session, EntityTurnBattleMonster enemy) {
        return Math.max(0, getEnemies(player, session).indexOf(enemy));
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
        session.actingEnemyIndex = visibleEnemyIndex(player, session, stage);
        session.phaseChanged = true;
    }

    private static int rollPlayerActionCount(ServerPlayer player, BSPlayerStats stats) {
        double actionCount = SkillUtils.getActionCount(player, stats);
        int result = Math.max(1, (int) Math.floor(actionCount));
        double fractional = actionCount - Math.floor(actionCount);
        if (fractional > 0.0D && player.getRandom().nextDouble() < fractional) {
            result++;
        }
        Session session = PLAYER_SESSIONS.get(player.getUUID());
        var spirit = stats == null ? null : stats.getOriginalSpirit();
        var features = session != null ? session.originalState.features(player)
                : spirit == null ? List.<BSOriginalCombatData.Feature>of() : spirit.features();
        for (var feature : features) {
            if (feature.code() == 61 && player.getRandom().nextDouble() < feature.value()) result++;
        }
        return result;
    }

    private static double originalHitRate(LivingEntity entity, Session session) {
        boolean originalProfile = entity instanceof EntityOriginalDatabaseEnemy
                || entity instanceof EntityOriginalTurnBattleEnemy legacy && legacy.getOriginalBattleProfileId() > 0;
        return (originalProfile ? 0.0D : entity instanceof Player ? 1.0D : 0.95D) + session.originalState.xparam(entity, 0);
    }

    private static boolean originalPartyAbility(ServerPlayer player, Session session, int ability) {
        for (UUID memberId : session.partyMembers) {
            ServerPlayer member = player.server.getPlayerList().getPlayer(memberId);
            if (member != null && session.originalState.hasFeature(member, 64, ability)) return true;
        }
        return false;
    }

    private static void initializeOriginalEncounter(ServerPlayer player, Session session, boolean scripted) {
        double partyAgility = session.partyMembers.stream().map(player.server.getPlayerList()::getPlayer)
                .filter(member -> member != null && member.isAlive())
                .mapToDouble(member -> battleParameter(member, 6, player, session)).average().orElse(1.0D);
        double troopAgility = getEnemies(player, session).stream().filter(enemy -> !enemy.isTurnBattleDefeated())
                .mapToDouble(enemy -> battleParameter(enemy, 6, player, session)).average().orElse(1.0D);
        double escapeBonus = session.partyMembers.stream().map(player.server.getPlayerList()::getPlayer)
                .filter(java.util.Objects::nonNull).mapToDouble(member -> {
                    var spirit = battlePlayerStats(member).getOriginalSpirit();
                    return spirit == null ? 0.0D : spirit.escapeBonus();
                }).sum();
        double escapePenalty = getAllEnemies(player, session).stream().mapToDouble(enemy -> {
            int profile = enemy instanceof EntityOriginalDatabaseEnemy original ? original.getProfileId()
                    : enemy instanceof EntityOriginalTurnBattleEnemy legacy ? legacy.getOriginalBattleProfileId() : 0;
            var matcher = java.util.regex.Pattern.compile("<逃走成功率減少[:：](\\S+)>")
                    .matcher(BSOriginalCombatData.enemy(profile).note());
            return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0.0D;
        }).sum();
        session.escapeRatio = Math.max(0.20D, Mth.clamp(1.5D - Math.floor(troopAgility)
                / Math.max(1.0D, Math.floor(partyAgility)), 0.40D, 1.0D) + (escapeBonus - escapePenalty) * 0.01D);
        if (!scripted) {
            double preemptiveRate = (partyAgility >= troopAgility ? 0.05D : 0.03D)
                    * (originalPartyAbility(player, session, 3) ? 4.0D : 1.0D);
            session.preemptive = player.getRandom().nextDouble() < preemptiveRate;
            session.surprisePending = player.getRandom().nextDouble()
                    < (partyAgility >= troopAgility ? 0.03D : 0.05D) && !session.preemptive
                    && !originalPartyAbility(player, session, 2);
        }
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
        if (session.troopResume != null && session.troopAutomaticAcknowledged
                && session.troopPresentation != null && player.getUUID().equals(session.presentationDriver)
                && player.serverLevel().getGameTime() >= session.troopTextUntil) {
            handleTroopPresentation(player, session.troopToken, session.troopPresentation.revision(), -1);
            return;
        }
        if (session.troopResume != null && session.troopWaitUntil > 0L
                && player.getUUID().equals(session.presentationDriver)
                && player.serverLevel().getGameTime() >= session.troopWaitUntil) {
            session.troopWaitUntil = 0L;
            presentTroopStep(player, session, session.troopRunner.resumeWait());
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
            } else if (session.originalTroopId > 0) {
                finish(player, session, ClientboundTurnBattlePacket.Outcome.NONE,
                        Component.literal("原作战斗目标已失效。"), false, 0L);
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
    public static void onOriginalEffectApplicable(net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof EntityOriginalDatabaseEnemy enemy) || enemy.level().isClientSide()) {
            return;
        }
        Session session = PLAYER_SESSIONS.get(ENEMY_SESSIONS.get(enemy.getUUID()));
        OriginalBattleState states = session == null ? new OriginalBattleState(new HashMap<>()) : session.originalState;
        for (int stateId : List.of(2, 3, 5, 6, 7, 8, 13, 17, 26, 28, 29, 39, 55, 56, 61, 162, 163, 165)) {
            if (stateId == 7 && event.getEffectInstance().getAmplifier() == 0
                    || stateId == 8 && event.getEffectInstance().getAmplifier() > 0) {
                continue;
            }
            if (originalPlayerStateEffect(stateId) == event.getEffectInstance().getEffect()
                    && (states.hasFeature(enemy, 14, stateId) || states.stateRate(enemy, stateId) <= 0.0D)) {
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Session session = PLAYER_SESSIONS.remove(event.getEntity().getUUID());
        if (session != null) {
            if (session.originalTroopId > 0 && event.getEntity() instanceof ServerPlayer player) {
                session.originalState.clear(List.of(player));
                refreshOriginalParameters(player, session);
                finish(player, session, ClientboundTurnBattlePacket.Outcome.NONE,
                        Component.literal("战斗成员已断开连接。"), false, 0L);
                return;
            }
            if (event.getEntity() instanceof ServerPlayer player) {
                session.originalState.clear(List.of(player));
                refreshOriginalParameters(player, session);
            }
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
            if (event.getEntity() instanceof ServerPlayer player) {
                session.headSearch.clear(getAllEnemies(player, session));
                session.originalState.clear(getAllEnemies(player, session));
                for (EntityTurnBattleMonster enemy : getAllEnemies(player, session)) {
                    enemy.setInvulnerable(false);
                    if (enemy instanceof EntityOriginalDatabaseEnemy original) {
                        original.resetTurnBattleActionResources();
                        original.setTurnBattleParameterRates(1.0D, 1.0D);
                    }
                }
            }
            for (UUID enemyId : session.enemyIds) {
                ENEMY_SESSIONS.remove(enemyId);
                if ((!enemyId.equals(session.rootEnemyId) || session.originalTroopId > 0)
                        && event.getEntity() instanceof ServerPlayer player) {
                    Entity enemy = player.serverLevel().getEntity(enemyId);
                    if (enemy != null) {
                        enemy.discard();
                    }
                }
            }
            if (!session.finished) {
                session.finished = true;
                if (session.onFinished != null) {
                    session.onFinished.accept(ClientboundTurnBattlePacket.Outcome.NONE);
                }
            }
        }
    }

    private static Map<MobEffect, EffectSnapshot> captureBattleEffects(LivingEntity entity) {
        Map<MobEffect, EffectSnapshot> snapshots = new HashMap<>();
        for (MobEffectInstance instance : entity.getActiveEffects()) {
            snapshots.put(instance.getEffect(), new EffectSnapshot(
                    instance, instance.getDuration(), instance.getAmplifier()));
        }
        return snapshots;
    }

    private static void advanceBattleEffects(LivingEntity entity,
                                              Map<MobEffect, EffectSnapshot> snapshots, Session session) {
        boolean previousAdvancing = ADVANCING_BATTLE_EFFECTS.get();
        ADVANCING_BATTLE_EFFECTS.set(true);
        boolean previousInternalDamage = INTERNAL_BATTLE_ITEM_DAMAGE.get();
        INTERNAL_BATTLE_ITEM_DAMAGE.set(true);
        try {
            double damageRate = 0.0D;
            for (Map.Entry<MobEffect, EffectSnapshot> entry : snapshots.entrySet()) {
                if (matchesBattleEffect(entity.getEffect(entry.getKey()), entry.getValue())) {
                    damageRate += originalBattleStatusDamageRate(entry.getKey());
                }
            }
            applyBattleStatusDamage(entity, damageRate, session);
            MobEffect requiem = BlackSouls.BUFF_REQUIEM.isPresent()
                    ? BlackSouls.BUFF_REQUIEM.get() : null;
            for (Map.Entry<MobEffect, EffectSnapshot> entry : snapshots.entrySet()) {
                if (entry.getKey() != requiem) {
                    advanceBattleEffect(entity, entry.getKey(), entry.getValue(), session);
                }
            }
            if (requiem != null && snapshots.containsKey(requiem)) {
                advanceBattleEffect(entity, requiem, snapshots.get(requiem), session);
            }
        } finally {
            INTERNAL_BATTLE_ITEM_DAMAGE.set(previousInternalDamage);
            ADVANCING_BATTLE_EFFECTS.set(previousAdvancing);
        }
    }

    private static void advanceBattleEffect(LivingEntity entity, MobEffect effect,
                                            EffectSnapshot snapshot, Session session) {
        MobEffectInstance current = entity.getEffect(effect);
        if (!matchesBattleEffect(current, snapshot)) {
            return;
        }
        if (current.isInfiniteDuration()) {
            return;
        }
        boolean active = true;
        for (int tick = 0; tick < EFFECT_TICKS_PER_ACTION && active; tick++) {
            active = current.tick(entity, () -> {
            });
        }
        if (!active) {
            entity.removeEffect(effect);
        } else if (entity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), current));
        }
    }

    private static boolean matchesBattleEffect(MobEffectInstance current, EffectSnapshot snapshot) {
        return current != null && current == snapshot.instance
                && current.getDuration() == snapshot.duration
                && current.getAmplifier() == snapshot.amplifier;
    }

    private static void applyBattleStatusDamage(LivingEntity entity, double damageRate, Session session) {
        if (entity instanceof ServerPlayer player) {
            restoreTurnBattleHealth(player, player.getCapability(BSPlayerStats.CAPABILITY).orElse(null),
                    damageRate, session);
            return;
        }
        if (damageRate <= 0.0D) {
            return;
        }
        double maxHealth = entity instanceof EntityTurnBattleMonster enemy
                ? enemy.getTurnBattleMaxHealth() : entity.getMaxHealth();
        int damage = safeDamageInt(Math.floor(maxHealth * damageRate
                * StatEventHandler.getPercentageDamageMultiplier(entity)));
        if (entity instanceof EntityTurnBattleMonster enemy) {
            enemy.setTurnBattleHealth(enemy.getTurnBattleHealth() - damage);
        }
    }

    private static double originalBattleStatusDamageRate(MobEffect effect) {
        if (effect == BlackSouls.BUFF_POISON.get()) {
            return 0.02D;
        }
        if (effect == BlackSouls.BUFF_SEVERE_POISON.get() || effect == BlackSouls.BUFF_BLEEDING.get()) {
            return 0.03D;
        }
        return effect == BlackSouls.BUFF_BURN.get() ? 0.05D : 0.0D;
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

    private record EnemyActionResult(Component message, int followUpSkillId, List<UUID> selectedPlayerTargets) {
        private EnemyActionResult {
            selectedPlayerTargets = List.copyOf(selectedPlayerTargets);
        }

        private EnemyActionResult(Component message, int followUpSkillId) {
            this(message, followUpSkillId, List.of());
        }
    }

    private record HitValue(double damage, boolean critical) {
    }

    private record CounterHit(int damage, boolean critical) {
    }

    private static final class Session {
        private final UUID rootEnemyId;
        private int originalTroopId;
        private OriginalBattleMusic battleMusic = OriginalBattleMusic.UNSET;
        private final UUID troopToken = UUID.randomUUID();
        private OriginalTroopEventRunner troopRunner;
        private TroopEventContext troopContext;
        private OriginalTroopPresentation troopPresentation;
        private Runnable troopResume;
        private Runnable troopAfterPresentation;
        private long troopWaitUntil;
        private long troopTextUntil;
        private boolean troopAutomaticAcknowledged;
        private boolean troopPlayerBatchOpen;
        private UUID troopPlayerBatchOwner;
        private UUID troopEnemyBatch;
        private boolean troopEnemyBatchEnding;
        private boolean canEscape = true;
        private boolean canLose;
        private boolean finished;
        private Consumer<ClientboundTurnBattlePacket.Outcome> onFinished;
        private final List<UUID> originalTroopMembers = new ArrayList<>();
        private final Set<UUID> hiddenEnemyIds = new HashSet<>();
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
        private final EnemyHeadSearch headSearch = new EnemyHeadSearch(enemyStates);
        private final OriginalBattleState originalState = new OriginalBattleState(enemyStates);
        private final EnemyCommonEvents commonEvents = new EnemyCommonEvents();
        private final Set<UUID> escapedEnemyIds = new HashSet<>();
        private final Set<UUID> guardingPlayers = new HashSet<>();
        private final Map<UUID, Double> playerBaseMaxMana = new HashMap<>();
        private final Map<UUID, Double> playerAppliedMaxMana = new HashMap<>();
        private final Map<UUID, Integer> enemyTurns = new HashMap<>();
        private final Map<UUID, EnemyActionSkip> enemyActionSkips = new HashMap<>();
        private final Map<UUID, Integer> enemyBreakRounds = new HashMap<>();
        private final Map<UUID, Map<MobEffect, EffectSnapshot>> roundEffectSnapshots = new HashMap<>();
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
        private boolean preemptive;
        private boolean surprisePending;
        private double escapeRatio;
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
        private final Map<UUID, List<ClientboundTurnBattlePacket.IncomingHit>> incomingHitsByPlayer = new HashMap<>();

        private Session(UUID rootEnemyId, Vec3 playerAnchor, Vec3 enemyAnchor,
                        int battleProfileId) {
            this.rootEnemyId = rootEnemyId;
            this.playerAnchor = playerAnchor;
            this.enemyAnchor = enemyAnchor;
            this.battleProfileId = battleProfileId;
        }
    }
}

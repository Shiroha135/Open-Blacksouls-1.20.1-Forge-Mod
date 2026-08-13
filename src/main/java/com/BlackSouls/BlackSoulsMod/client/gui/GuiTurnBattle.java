package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.client.TurnBattleAudioGate;
import com.BlackSouls.BlackSoulsMod.client.ClientSceneState;
import com.BlackSouls.BlackSoulsMod.client.render.AnimationRegistry;
import com.BlackSouls.BlackSoulsMod.client.render.BSAvatarRenderer;
import com.BlackSouls.BlackSoulsMod.client.render.BattleScreenVFXRenderer;
import com.BlackSouls.BlackSoulsMod.client.render.BattleTransitionRenderer;
import com.BlackSouls.BlackSoulsMod.client.render.FadedBannerRenderer;
import com.BlackSouls.BlackSoulsMod.client.render.TextBannerRenderer;
import com.BlackSouls.BlackSoulsMod.client.render.TurnBattleVfxResolver;
import com.BlackSouls.BlackSoulsMod.client.render.VFXAnimation;
import com.BlackSouls.BlackSoulsMod.client.render.VFXSoundTiming;
import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import com.BlackSouls.BlackSoulsMod.combat.TurnBattleDomainData;
import com.BlackSouls.BlackSoulsMod.client.ClientSkillInfo;
import com.BlackSouls.BlackSoulsMod.client.ClientPartyState;
import com.BlackSouls.BlackSoulsMod.entity.EntityTurnBattleMonster;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundTurnBattlePacket;
import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundPartyStatePacket;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundTurnBattleActionPacket;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundTurnBattlePresentationPacket;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalBattleProfileData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalEnemyData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalEnemyPhaseData;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalStateData;
import com.BlackSouls.BlackSoulsMod.util.SkillUtils;
import com.BlackSouls.BlackSoulsMod.util.skill.AbstractSkill;
import com.BlackSouls.BlackSoulsMod.util.skill.SkillRegistry;
import com.BlackSouls.BlackSoulsMod.util.skill.WeaponSkill;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import top.theillusivec4.curios.api.CuriosApi;

public class GuiTurnBattle extends Screen {
    private static final ResourceLocation RABBIT_TEXTURE =
            new ResourceLocation(BlackSouls.MODID, "textures/entity/corpse_eating_rabbit.png");
    private static final ResourceLocation BATTLE_START_TEXTURE =
            new ResourceLocation(BlackSouls.MODID, "textures/gui/battle/battle_start.png");
    private static final ResourceLocation BATTLE_START1_TEXTURE =
            new ResourceLocation(BlackSouls.MODID, "textures/gui/battle/battle_start1.png");
    private static final ResourceLocation BATTLE_MIST_TEXTURE =
            new ResourceLocation(BlackSouls.MODID, "textures/gui/battle/mist.png");
    private static final ResourceLocation[] CURTAIN_TEXTURES = java.util.stream.IntStream.range(0, 9)
            .mapToObj(index -> new ResourceLocation(BlackSouls.MODID,
                    "textures/gui/battle/curtain/curtain_" + index + ".png"))
            .toArray(ResourceLocation[]::new);
    private static final float MAP_TRANSITION_FRAMES = 35.0F;
    private static final float BATTLE_REVEAL_FRAMES = 10.0F;
    private static final long VFX_FRAME_DURATION_MILLIS = 66L;
    private static final float ENEMY_MAX_AP = 4000.0F;
    private static final int ENEMY_START_WAIT_TICKS = 10;
    private static final Component[] MAIN_OPTIONS = {
            Component.literal("攻击"),
            Component.literal("技·魔法"),
            Component.literal("防御"),
            Component.literal("道具"),
            Component.literal("逃走"),
            Component.literal("武器变更")
    };

    private final int entityId;
    private int battleProfileId;
    private final List<EnemyVisual> enemies = new ArrayList<>();
    private int actingEnemyIndex;
    private int targetSelection;
    private int lastTargetIndex;
    private ServerboundTurnBattleActionPacket.Action pendingTargetAction;
    private int pendingTargetValue;
    private View targetReturnView = View.COMMAND;
    private ResourceLocation battleBgm;
    private float battleBgmVolume = 1.0F;
    private float battleBgmPitch = 1.0F;
    private ResourceLocation battleback1;
    private ResourceLocation battleback2;
    private int battleback1Width = 544;
    private int battleback1Height = 416;
    private int battleback2Width = 544;
    private int battleback2Height = 416;
    private Component enemyName;
    private float enemyHealth;
    private float enemyMaxHealth;
    private int enemyProfileId;
    private Component message;
    private final List<String> introPages = new ArrayList<>();
    private int introPageIndex;
    private boolean canAct;
    private boolean pendingCanAct;
    private View view = View.INTRO;
    private ClientboundTurnBattlePacket.Outcome outcome = ClientboundTurnBattlePacket.Outcome.NONE;
    private int selection;
    private int menuScrollRow;
    private int rememberedSkillSelection;
    private int rememberedSkillScrollRow;
    private int rememberedSkillValue = -1;
    private int sequenceTicks;
    private float enemyAlpha = 1.0F;
    private int phaseTransitionTicks;
    private float displayEnemyHealth;
    private float displayPlayerHealth = -1.0F;
    private float displayPlayerMaxHealth = 1.0F;
    private double displayMp = -1.0D;
    private double displayMaxMp = 1.0D;
    private double displayAp = -1.0D;
    private double displayMaxAp = 1.0D;
    private long soulReward;
    private final List<ItemStack> rewardItems = new ArrayList<>();
    private SimpleSoundInstance battleMusic;
    private boolean encounterSoundPlayed;
    private long encounterStartedAt = -1L;
    private int battleMusicDelay = 16;
    private int effectAge;
    private int enemyHitTicks;
    private int playerHitTicks;
    private int enemyDamageNumber;
    private int playerDamageNumber;
    private long enemyDamagePopupStartedAt = -1L;
    private long playerDamagePopupStartedAt = -1L;
    private boolean enemyDamageCritical;
    private boolean playerUsedNormalAttack;
    private boolean enemyWasDamaged;
    private boolean enemyActed;
    private boolean enemyAttacked;
    private boolean playerWasDamaged;
    private int holdEnemyGaugeTicks;
    private int holdPlayerGaugeTicks;
    private int enemyImpactAge = 5;
    private float enemyActionPoints;
    private int enemyActionWaitTicks;
    private boolean enemyActionInitialized;
    private ResourceLocation enemyTexture = RABBIT_TEXTURE;
    private int enemyTextureWidth = 127;
    private int enemyTextureHeight = 121;
    private double enemyAgility = 40.0D;
    private int enemyAnimationId = 1;
    private final Map<String, Integer> skillCooldowns = new HashMap<>();
    private final Map<String, ResourceLocation> turnSkillIcons = new HashMap<>();
    private TurnBattleVfxResolver.Cue pendingPlayerVfx = TurnBattleVfxResolver.Cue.NONE;
    private boolean pendingPlayerTargetsAll;
    private int pendingPlayerVfxStartAge = 2;
    private boolean pendingCounterSound;
    private int pendingEnemyEvasionSound;
    private final List<ActiveBattleVfx> activeBattleVfx = new ArrayList<>();
    private final List<ScheduledDamageHit> scheduledEnemyHits = new ArrayList<>();
    private final List<DamagePopup> enemyDamagePopups = new ArrayList<>();
    private final List<PlayerDamagePopup> playerDamagePopups = new ArrayList<>();
    private final List<ScheduledIncomingHit> scheduledIncomingHits = new ArrayList<>();
    private final List<ScheduledRevival> scheduledRevivals = new ArrayList<>();
    private boolean awaitingPresentation;
    private int enemyAttackAge = 27;
    private boolean incomingSequenceActive;
    private boolean playerDown;
    private String activeEnemyActionHeader = "";
    private int curtainTransitionTicks;
    private PendingCurtainPhase pendingCurtainPhase;
    private boolean curtainRestoresInput;
    private long domainBannerStartedAt = -1L;
    private boolean domainBannerSoundPlayed;
    private final java.util.Set<String> shownDomainTitles = new java.util.HashSet<>();
    private final long mistStartedAt = System.currentTimeMillis();
    private int granFinalPresentationTicks;
    private int granFinalPresentationSoundStep = -1;

    public GuiTurnBattle(int entityId, int battleProfileId,
                         List<ClientboundTurnBattlePacket.EnemySnapshot> enemies,
                         Component message, boolean canAct,
                         Map<String, Integer> skillCooldowns, int enemyAnimationId) {
        super(Component.translatable("gui.blacksouls.turn_battle.title"));
        this.entityId = entityId;
        this.battleProfileId = battleProfileId;
        replaceEnemies(enemies, true);
        this.message = message;
        setIntroPages(message);
        this.canAct = canAct;
        this.skillCooldowns.putAll(skillCooldowns);
        this.enemyAnimationId = enemyAnimationId;
        updateBattleEnvironment(false);
    }

    @Override
    protected void init() {
        TurnBattleAudioGate.enter(this);
        captureEnemyVisual();
        initializeDisplayedStats();
        initializeEnemyActionGauge();
        if (this.encounterStartedAt < 0L) {
            this.encounterStartedAt = System.currentTimeMillis();
        }
        if (!this.encounterSoundPlayed) {
            this.encounterSoundPlayed = true;
            playUiSound(BlackSouls.TURN_BATTLE_START_EVENT.get(), 1.0F);
        }
    }

    public boolean matches(int entityId) {
        return this.entityId == entityId;
    }

    private void replaceEnemies(List<ClientboundTurnBattlePacket.EnemySnapshot> snapshots,
                                boolean resetVisuals) {
        Map<Integer, EnemyVisual> previous = new HashMap<>();
        for (EnemyVisual enemy : this.enemies) {
            previous.put(enemy.entityId, enemy);
        }
        this.enemies.clear();
        for (ClientboundTurnBattlePacket.EnemySnapshot snapshot : snapshots) {
            EnemyVisual old = previous.get(snapshot.entityId());
            float displayed = resetVisuals || old == null
                    ? snapshot.health() : old.displayHealth;
            float alpha = resetVisuals || old == null ? 1.0F : old.displayAlpha;
            int profileId = snapshot.profileId();
            if (old != null && old.profileId >= 570 && old.profileId < 577
                    && profileId > old.profileId && profileId <= 577) {
                profileId = old.profileId + 1;
            }
            EnemyVisual visual = new EnemyVisual(snapshot.entityId(), snapshot.name(),
                    snapshot.health(), snapshot.maxHealth(), profileId,
                    snapshot.states(), displayed, alpha);
            visual.targetProfileId = snapshot.profileId();
            visual.profileMorphTicks = profileId == snapshot.profileId() ? 0 : 6;
            this.enemies.add(visual);
        }
        this.targetSelection = firstLivingEnemyIndex();
        this.lastTargetIndex = Math.max(0, Math.min(
                Math.max(0, this.enemies.size() - 1), this.lastTargetIndex));
        syncPrimaryEnemy();
    }

    private void syncPrimaryEnemy() {
        if (this.enemies.isEmpty()) {
            this.enemyName = Component.literal("敌人");
            this.enemyHealth = 0.0F;
            this.enemyMaxHealth = 1.0F;
            this.enemyProfileId = -1;
            return;
        }
        int index = Math.max(0, Math.min(this.enemies.size() - 1, this.actingEnemyIndex));
        EnemyVisual primary = this.enemies.get(index);
        this.enemyName = primary.name;
        this.enemyHealth = primary.health;
        this.displayEnemyHealth = primary.displayHealth;
        this.enemyMaxHealth = primary.maxHealth;
        this.enemyProfileId = primary.profileId;
    }

    private int firstLivingEnemyIndex() {
        for (int i = 0; i < this.enemies.size(); i++) {
            if (isLivingTarget(i)) {
                return i;
            }
        }
        return 0;
    }

    private void updateBattleEnvironment(boolean restartMusic) {
        BSOriginalBattleProfileData.Entry battle =
                BSOriginalBattleProfileData.get(this.battleProfileId);
        ResourceLocation nextBgm = battle.bgm();
        boolean musicChanged = !java.util.Objects.equals(this.battleBgm, nextBgm);
        this.battleBgm = nextBgm;
        this.battleBgmVolume = battle.bgmVolume();
        this.battleBgmPitch = battle.bgmPitch();
        this.battleback1 = battle.battleback1();
        this.battleback2 = battle.battleback2();
        this.battleback1Width = battle.battleback1Width();
        this.battleback1Height = battle.battleback1Height();
        this.battleback2Width = battle.battleback2Width();
        this.battleback2Height = battle.battleback2Height();
        ResourceLocation sceneBackground = ClientSceneState.getBattleBackground();
        if (sceneBackground != null) {
            this.battleback1 = sceneBackground;
            this.battleback1Width = 640;
            this.battleback1Height = 480;
            this.battleback2 = null;
        }
        if (restartMusic && musicChanged && this.battleMusic != null) {
            stopBattleMusic();
            startBattleMusic();
        }
    }

    public void applyState(boolean active, int battleProfileId,
                           List<ClientboundTurnBattlePacket.EnemySnapshot> enemies,
                           int actingEnemyIndex, boolean phaseChanged,
                           boolean awaitingPresentation,
                           List<ClientboundTurnBattlePacket.DamageHit> playerHits,
                           List<ClientboundTurnBattlePacket.IncomingHit> incomingHits,
                           Component message, boolean canAct,
                           ClientboundTurnBattlePacket.Outcome outcome, long soulReward,
                           List<ItemStack> rewardItems,
                           Map<String, Integer> skillCooldowns, int enemyAnimationId) {
        int previousBattleProfileId = this.battleProfileId;
        boolean granFinalEntrance = phaseChanged
                && previousBattleProfileId == 579 && battleProfileId == 570;
        boolean granCurtainTransition = phaseChanged
                && isGranCurtainTransition(this.battleProfileId, battleProfileId);
        if (phaseChanged && previousBattleProfileId == 185 && battleProfileId == 184) {
            playUiSound(BlackSouls.MONSTER4_EVENT.get(), 0.5F, 1.0F);
        }
        Map<Integer, Float> previousHealth = new HashMap<>();
        for (EnemyVisual enemy : this.enemies) {
            previousHealth.put(enemy.entityId, enemy.health);
        }
        if (granCurtainTransition) {
            this.pendingCurtainPhase = new PendingCurtainPhase(
                    battleProfileId, List.copyOf(enemies), actingEnemyIndex, enemyAnimationId);
        } else {
            boolean environmentChanged = this.battleProfileId != battleProfileId;
            this.battleProfileId = battleProfileId;
            this.actingEnemyIndex = Math.max(0, Math.min(
                    Math.max(0, enemies.size() - 1), actingEnemyIndex));
            replaceEnemies(enemies, phaseChanged);
            updateBattleEnvironment(environmentChanged);
            this.enemyAnimationId = enemyAnimationId;
        }
        if (granFinalEntrance) {
            stopBattleMusic();
            this.battleMusicDelay = 65;
            this.granFinalPresentationTicks = 65;
            this.granFinalPresentationSoundStep = -1;
        }
        if (!playerHits.isEmpty()) {
            Map<Integer, Integer> totalDamage = new HashMap<>();
            for (ClientboundTurnBattlePacket.DamageHit hit : playerHits) {
                totalDamage.merge(hit.targetEntityId(), hit.damage(), Integer::sum);
            }
            for (EnemyVisual enemy : this.enemies) {
                enemy.finalHealth = enemy.health;
                Float oldHealth = previousHealth.get(enemy.entityId);
                if (oldHealth != null) {
                    enemy.health = oldHealth;
                } else {
                    enemy.health = Math.min(enemy.maxHealth,
                            enemy.finalHealth + totalDamage.getOrDefault(enemy.entityId, 0));
                    enemy.displayHealth = enemy.health;
                }
            }
            syncPrimaryEnemy();
        }
        this.message = message;
        this.pendingCanAct = canAct;
        this.canAct = false;
        this.outcome = outcome;
        this.soulReward = soulReward;
        this.rewardItems.clear();
        rewardItems.stream().map(ItemStack::copy).forEach(this.rewardItems::add);
        this.skillCooldowns.clear();
        this.skillCooldowns.putAll(skillCooldowns);
        this.awaitingPresentation = awaitingPresentation;
        if (!granCurtainTransition) {
            captureEnemyVisual();
        }
        if (phaseChanged) {
            if (granCurtainTransition) {
                this.enemyAlpha = 1.0F;
                this.phaseTransitionTicks = 0;
                this.curtainTransitionTicks = 54;
                this.curtainRestoresInput = true;
                scheduleDomainBanner(battleProfileId, granFinalEntrance ? 3600L : 2700L);
            } else {
                this.enemyAlpha = 0.0F;
                this.phaseTransitionTicks = 24;
                this.curtainTransitionTicks = 0;
                scheduleDomainBanner(battleProfileId, granFinalEntrance ? 3600L : 1200L);
            }
            this.enemyActionPoints = 0.0F;
            this.enemyActionWaitTicks = ENEMY_START_WAIT_TICKS;
        }
        if (outcome == ClientboundTurnBattlePacket.Outcome.NONE
                && canAct && message.getString().isEmpty()) {
            this.canAct = !granCurtainTransition;
            this.pendingCanAct = true;
            this.view = View.COMMAND;
            this.selection = 0;
            this.menuScrollRow = 0;
            this.effectAge = 0;
            this.sequenceTicks = 0;
            this.pendingPlayerVfx = TurnBattleVfxResolver.Cue.NONE;
            this.pendingPlayerTargetsAll = false;
            this.pendingPlayerVfxStartAge = 2;
            this.pendingCounterSound = false;
            this.pendingEnemyEvasionSound = 0;
            return;
        }
        this.view = View.MESSAGE;
        this.selection = 0;
        String report = message.getString();
        String playerName = this.minecraft != null && this.minecraft.player != null
                ? this.minecraft.player.getDisplayName().getString() : "";
        String targetName = this.enemyName.getString();
        this.effectAge = 0;
        boolean playerCountered = !playerName.isEmpty()
                && (report.contains(playerName + "的反击！")
                || report.contains(playerName + "发动了反击！"));
        this.playerUsedNormalAttack = !playerName.isEmpty()
                && (report.startsWith(playerName + "的攻击！") || playerCountered);
        boolean playerUsedSkill = !playerName.isEmpty() && report.startsWith(playerName + "使用了");
        this.pendingPlayerVfxStartAge = 2;
        this.pendingCounterSound = playerCountered;
        if (playerCountered && this.minecraft != null && this.minecraft.player != null) {
            this.pendingPlayerVfx = TurnBattleVfxResolver.resolveWeapon(
                    this.minecraft.player, this.minecraft.player.getMainHandItem());
            this.pendingPlayerTargetsAll = false;
        }
        if (!this.playerUsedNormalAttack && !playerUsedSkill) {
            this.pendingPlayerVfx = TurnBattleVfxResolver.Cue.NONE;
        }
        this.enemyWasDamaged = !playerHits.isEmpty() || phaseChanged || this.enemies.stream()
                .anyMatch(enemy -> report.contains("对" + enemy.name.getString() + "造成了"));
        this.playerWasDamaged = !incomingHits.isEmpty()
                || !playerName.isEmpty() && report.contains(playerName + "受到了");
        this.enemyActed = awaitingPresentation && !phaseChanged
                && !report.contains("无法行动") && report.lines()
                .anyMatch(line -> this.enemies.stream()
                        .anyMatch(enemy -> line.startsWith(enemy.name.getString())));
        this.enemyAttacked = this.enemyActed
                && (this.playerWasDamaged || report.contains("但是攻击落空了"));
        this.pendingEnemyEvasionSound = report.contains("但是攻击落空了")
                ? resolveEnemyEvasionSound(report) : 0;
        this.enemyDamageNumber = extractDamage(report, "造成了\\s*(\\d+)\\s*点伤害");
        this.playerDamageNumber = extractDamage(report, "受到了\\s*(\\d+)\\s*点伤害");
        this.enemyDamageCritical = report.contains("会心一击");
        int incomingEndAge;
        if (playerCountered) {
            this.enemyAttackAge = 27;
            incomingEndAge = scheduleIncomingHits(incomingHits, this.enemyAttackAge + 6);
            this.pendingPlayerVfxStartAge = Math.max(this.enemyAttackAge + 8, incomingEndAge + 2);
            int counterImpactAge = this.pendingPlayerVfxStartAge
                    + Math.max(3, playerImpactAge(this.pendingPlayerVfx) - 2);
            this.enemyImpactAge = scheduleDamageHitsFrom(playerHits, counterImpactAge);
        } else {
            int animationImpactAge = this.enemyWasDamaged ? playerImpactAge(this.pendingPlayerVfx) : 5;
            this.enemyImpactAge = scheduleDamageHits(playerHits, animationImpactAge);
            this.enemyAttackAge = this.enemyAttacked
                    ? Math.max(27, this.enemyImpactAge + (this.enemyWasDamaged ? 10 : 0)) : 27;
            incomingEndAge = scheduleIncomingHits(incomingHits, this.enemyAttackAge + 6);
        }
        if (playerHits.isEmpty() && this.enemyWasDamaged && this.enemyDamageNumber > 0) {
            this.scheduledEnemyHits.add(new ScheduledDamageHit(
                    this.lastTargetIndex, this.enemyDamageNumber,
                    this.enemyDamageCritical, this.enemyImpactAge, true, 0));
        }
        int presentationEnd = Math.max(62, this.enemyImpactAge + 22);
        if (this.enemyActed) {
            presentationEnd = Math.max(presentationEnd,
                    Math.max(this.enemyAttackAge + 30, incomingEndAge + 20));
        }
        if (outcome == ClientboundTurnBattlePacket.Outcome.VICTORY) {
            presentationEnd = Math.max(presentationEnd, this.enemyImpactAge + 28);
        }
        for (EnemyVisual enemy : this.enemies) {
            if (enemy.health > 0.0F && enemy.finalHealth <= 0.0F && isBossCollapse(enemy)) {
                presentationEnd = Math.max(presentationEnd,
                        this.enemyImpactAge + bossCollapseDuration(enemy) + 8);
            }
        }
        this.sequenceTicks = presentationEnd;
        this.holdEnemyGaugeTicks = 0;
        this.holdPlayerGaugeTicks = this.playerWasDamaged ? presentationEnd + 2 : 0;
        this.scheduledEnemyHits.sort(java.util.Comparator.comparingInt(ScheduledDamageHit::triggerAge));
        this.enemyDamagePopups.clear();
        this.enemyDamagePopupStartedAt = -1L;
        this.playerDamagePopupStartedAt = -1L;
        this.playerDamagePopups.clear();
        this.activeEnemyActionHeader = report.lines()
                .filter(line -> this.enemies.stream()
                        .anyMatch(enemy -> line.startsWith(enemy.name.getString())))
                .reduce((first, second) -> second)
                .orElseGet(() -> report.lines().findFirst().orElse(report));
    }

    @Override
    public void tick() {
        updateDisplayedStats();
        updateEnemyActionGauge();
        tickGranFinalPresentation();
        if (this.phaseTransitionTicks > 0
                && this.outcome == ClientboundTurnBattlePacket.Outcome.NONE) {
            this.phaseTransitionTicks--;
            this.enemyAlpha = 1.0F - this.phaseTransitionTicks / 24.0F;
        }
        if (this.curtainTransitionTicks > 0) {
            this.curtainTransitionTicks--;
            if (this.curtainTransitionTicks == 27) {
                applyPendingCurtainPhase();
            }
            if (this.curtainTransitionTicks == 0 && this.curtainRestoresInput) {
                this.curtainRestoresInput = false;
                this.canAct = this.pendingCanAct;
                if (this.canAct) {
                    this.view = View.COMMAND;
                    this.selection = 0;
                    this.menuScrollRow = 0;
                }
            }
        }
        if (!this.domainBannerSoundPlayed && this.domainBannerStartedAt >= 0L
                && System.currentTimeMillis() >= this.domainBannerStartedAt
                && TurnBattleDomainData.get(this.battleProfileId) != null) {
            this.domainBannerSoundPlayed = true;
            playUiSound(BlackSouls.TURN_BATTLE_DOMAIN_EVENT.get(), 1.0F);
        }
        if (this.battleMusicDelay > 0 && --this.battleMusicDelay == 0
                && this.outcome == ClientboundTurnBattlePacket.Outcome.NONE) {
            startBattleMusic();
        }
        if (this.enemyHitTicks > 0) {
            this.enemyHitTicks--;
        }
        if (this.playerHitTicks > 0) {
            this.playerHitTicks--;
        }
        if (this.view == View.MESSAGE) {
            this.effectAge++;
            if (this.effectAge == this.pendingPlayerVfxStartAge && this.pendingCounterSound) {
                playUiSound(BlackSouls.EVASION1_EVENT.get(), 1.0F);
                this.pendingCounterSound = false;
            }
            if (this.effectAge == this.pendingPlayerVfxStartAge && this.pendingPlayerVfx.valid()) {
                if (this.pendingPlayerTargetsAll) {
                    boolean playSounds = true;
                    for (int i = 0; i < this.enemies.size(); i++) {
                        if (this.enemies.get(i).displayHealth > 0.0F) {
                            queueBattleVfx(this.pendingPlayerVfx, playSounds, i);
                            playSounds = false;
                        }
                    }
                } else {
                    queueBattleVfx(this.pendingPlayerVfx, true);
                }
                this.pendingPlayerVfx = TurnBattleVfxResolver.Cue.NONE;
                this.pendingPlayerTargetsAll = false;
            }
            triggerScheduledDamageHits();
            if (this.effectAge == this.enemyAttackAge && this.enemyActed) {
                VFXAnimation animation = AnimationRegistry.ANIMATIONS.get(this.enemyAnimationId);
                if (animation == null || animation.soundTimings.isEmpty()) {
                    playUiSound(BlackSouls.TURN_ENEMY_ATTACK_EVENT.get(), 1.0F);
                }
                queueBattleVfx(new TurnBattleVfxResolver.Cue(
                        this.enemyAnimationId,
                        resolveEnemyAnimationTarget(this.message.getString())), true,
                        this.actingEnemyIndex);
            }
            triggerIncomingHits();
            triggerScheduledRevivals();
            if (this.effectAge == this.enemyAttackAge + 6 && this.enemyActed
                    && !this.incomingSequenceActive) {
                this.enemyActionPoints = 0.0F;
                this.enemyActionWaitTicks = 0;
                if (this.pendingEnemyEvasionSound == 2) {
                    playUiSound(BlackSouls.EVASION2_EVENT.get(), 1.0F, 0.8F);
                } else if (this.pendingEnemyEvasionSound == 1) {
                    playUiSound(BlackSouls.EVASION1_EVENT.get(), 1.0F, 0.8F);
                }
                this.pendingEnemyEvasionSound = 0;
                if (this.playerWasDamaged) {
                    playUiSound(BlackSouls.TURN_PLAYER_DAMAGE_EVENT.get(), 1.0F);
                    this.playerHitTicks = 10;
                    this.playerDamagePopupStartedAt = System.currentTimeMillis();
                }
            }
            tickBattleVfxSounds();
        }
        if (this.view != View.MESSAGE || this.sequenceTicks <= 0) {
            return;
        }
        this.sequenceTicks--;
        if (this.outcome == ClientboundTurnBattlePacket.Outcome.VICTORY
                && !hasActiveBossCollapse() && this.sequenceTicks < 24) {
            this.enemyAlpha = Math.max(0.0F, this.sequenceTicks / 24.0F);
        }
        if (this.sequenceTicks > 0) {
            return;
        }
        if (this.awaitingPresentation) {
            this.awaitingPresentation = false;
            NetworkHandler.sendToServer(new ServerboundTurnBattlePresentationPacket());
            return;
        }
        if (this.outcome == ClientboundTurnBattlePacket.Outcome.NONE) {
            this.view = View.COMMAND;
            this.canAct = this.pendingCanAct;
        } else {
            this.view = View.RESULT;
            stopBattleMusic();
            if (this.outcome == ClientboundTurnBattlePacket.Outcome.VICTORY
                    && !TextBannerRenderer.isWaitingForCenteredBanner()) {
                playUiSound(BlackSouls.TURN_BATTLE_VICTORY_EVENT.get(), 1.0F);
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (drawEncounterTransition(graphics)) {
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        if (this.view == View.WEAPONS) {
            graphics.fill(0, 0, this.width, this.height, 0xFF000000);
            drawWeaponChangeScreen(graphics, mouseX, mouseY);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        graphics.pose().pushPose();
        int shake = this.playerHitTicks > 0 ? ((this.playerHitTicks & 1) == 0 ? 2 : -2) : 0;
        if (granFinalPresentationAge() >= 45 && this.granFinalPresentationTicks > 0) {
            int power = 4 + (granFinalPresentationAge() & 1) * 3;
            shake += (granFinalPresentationAge() & 2) == 0 ? power : -power;
        }
        if (shake != 0) {
            graphics.pose().translate(shake, 0.0F, 0.0F);
        }
        drawBattleBackdrop(graphics);
        drawEnemy(graphics);
        drawBattleEffects(graphics);
        drawCurtainTransition(graphics);
        if (this.view == View.INTRO) {
            drawIntro(graphics);
        } else {
            drawEnemyHealth(graphics);
            if (this.view == View.MESSAGE) {
                drawBattleMessage(graphics);
                drawStatusWindow(graphics);
            } else if (this.view == View.RESULT) {
                drawResult(graphics);
            } else {
                drawStatusWindow(graphics);
                drawCommandWindow(graphics, mouseX, mouseY);
            }
        }
        drawDomainBanner(graphics);
        drawGranFinalFlash(graphics);
        graphics.pose().popPose();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private boolean drawEncounterTransition(GuiGraphics graphics) {
        if (this.encounterStartedAt < 0L) {
            return false;
        }
        float frame = (System.currentTimeMillis() - this.encounterStartedAt) * 60.0F / 1000.0F;
        if (frame < MAP_TRANSITION_FRAMES) {
            BattleTransitionRenderer.drawMask(graphics, BATTLE_START1_TEXTURE, this.width, this.height,
                    frame / MAP_TRANSITION_FRAMES);
            return true;
        }
        if (frame < MAP_TRANSITION_FRAMES + BATTLE_REVEAL_FRAMES) {
            drawBattleBackdrop(graphics);
            drawEnemy(graphics);
            float reveal = (frame - MAP_TRANSITION_FRAMES) / BATTLE_REVEAL_FRAMES;
            BattleTransitionRenderer.drawMask(graphics, BATTLE_START_TEXTURE, this.width, this.height,
                    1.0F - reveal);
            return true;
        }
        return false;
    }

    private boolean encounterTransitionComplete() {
        if (this.encounterStartedAt < 0L) {
            return true;
        }
        float frame = (System.currentTimeMillis() - this.encounterStartedAt) * 60.0F / 1000.0F;
        return frame >= MAP_TRANSITION_FRAMES + BATTLE_REVEAL_FRAMES;
    }

    private void drawBattleBackdrop(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);
        RenderSystem.enableBlend();
        if (this.battleback1 != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(this.battleback1, 0, 0, this.width, this.height,
                    0.0F, 0.0F, this.battleback1Width,
                    this.battleback1Height, this.battleback1Width,
                    this.battleback1Height);
        }
        if (this.battleback2 != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(this.battleback2, 0, 0, this.width, this.height,
                    0.0F, 0.0F, this.battleback2Width,
                    this.battleback2Height, this.battleback2Width,
                    this.battleback2Height);
        }
        drawBattleMist(graphics);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawBattleMist(GuiGraphics graphics) {
        int sceneHeight = Math.max(1, this.height - statusHeight());
        double elapsedFrames = (System.currentTimeMillis() - this.mistStartedAt) * 0.18D;
        float scaleX = this.width / 544.0F;
        float scaleY = sceneHeight / 416.0F;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        for (int index = 0; index < 10; index++) {
            int initialZ = Math.floorMod(mistHash(index, 0), 570);
            double progressed = initialZ + elapsedFrames;
            int cycle;
            float z;
            if (progressed < 600.0D) {
                cycle = 0;
                z = (float) progressed;
            } else {
                double afterReset = progressed - 600.0D;
                cycle = 1 + (int) Math.floor(afterReset / 580.0D);
                z = 20.0F + (float) (afterReset % 580.0D);
            }
            int baseX = Math.floorMod(mistHash(index, cycle), 272) + 136;
            float x = ((baseX - 272.0F) * z / 128.0F + baseX) * scaleX;
            float y = (z / 4.0F + 160.0F) * scaleY;
            float zoom = z * 0.003F + 0.25F;
            int drawWidth = Math.max(1, Math.round(256.0F * zoom * scaleX));
            int drawHeight = Math.max(1, Math.round(128.0F * zoom * scaleY));
            int drawX = Math.round(x - drawWidth * 0.5F);
            int drawY = Math.round(y - drawHeight * 0.5F);
            float opacity = Math.min(255.0F,
                    z >= 536.0F ? Math.max(0.0F, (600.0F - z) * 4.0F) : z) / 255.0F;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
            if (Math.floorMod(mistHash(index, cycle + 97), 3) == 0) {
                graphics.pose().pushPose();
                graphics.pose().translate(drawX + drawWidth, 0.0F, 0.0F);
                graphics.pose().scale(-1.0F, 1.0F, 1.0F);
                graphics.blit(BATTLE_MIST_TEXTURE, 0, drawY, drawWidth, drawHeight,
                        0.0F, 0.0F, 256, 128, 256, 128);
                graphics.pose().popPose();
            } else {
                graphics.blit(BATTLE_MIST_TEXTURE, drawX, drawY, drawWidth, drawHeight,
                        0.0F, 0.0F, 256, 128, 256, 128);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
    }

    private static int mistHash(int index, int cycle) {
        int value = index * 0x6D2B79F5 + cycle * 0x1B873593 + 0x5F356495;
        value ^= value >>> 16;
        value *= 0x7FEB352D;
        value ^= value >>> 15;
        return value;
    }

    private int granFinalPresentationAge() {
        return this.granFinalPresentationTicks <= 0 ? 65
                : 65 - this.granFinalPresentationTicks;
    }

    private void tickGranFinalPresentation() {
        if (this.granFinalPresentationTicks <= 0) {
            return;
        }
        int age = granFinalPresentationAge();
        if (age >= 0 && this.granFinalPresentationSoundStep < 0) {
            playUiSound(BlackSouls.GUCHA004A_EVENT.get(), 1.0F);
            playUiSound(BlackSouls.GRAN_BLOOD_SPLATTER_01_EVENT.get(), 0.95F);
            this.granFinalPresentationSoundStep = 0;
        }
        if (age >= 10 && this.granFinalPresentationSoundStep < 1) {
            playUiSound(BlackSouls.GUCHA004A_EVENT.get(), 1.0F);
            playUiSound(BlackSouls.GRAN_BONE_BREAK_EVENT.get(), 0.60F);
            this.granFinalPresentationSoundStep = 1;
        }
        if (age >= 20 && this.granFinalPresentationSoundStep < 2) {
            playUiSound(BlackSouls.GRAN_BLOOD_SPLATTER_02_EVENT.get(), 0.95F);
            playUiSound(BlackSouls.GRAN_BONE_EVENT.get(), 0.70F);
            this.granFinalPresentationSoundStep = 2;
        }
        if (age >= 30 && this.granFinalPresentationSoundStep < 3) {
            playUiSound(BlackSouls.GRAN_FLESH_CRUSH_EVENT.get(), 1.0F);
            this.granFinalPresentationSoundStep = 3;
        }
        if (age >= 45 && this.granFinalPresentationSoundStep < 4) {
            playUiSound(BlackSouls.GRAN_HALLUCINATION_EVENT.get(), 1.30F, 0.80F);
            this.granFinalPresentationSoundStep = 4;
        }
        this.granFinalPresentationTicks--;
    }

    private void drawGranFinalFlash(GuiGraphics graphics) {
        if (this.granFinalPresentationTicks <= 0) {
            return;
        }
        int age = granFinalPresentationAge();
        int start;
        int end;
        if (age < 10) {
            start = 0;
            end = 10;
        } else if (age < 20) {
            start = 10;
            end = 20;
        } else if (age < 30) {
            start = 20;
            end = 30;
        } else if (age < 45) {
            start = 30;
            end = 45;
        } else {
            return;
        }
        float fade = (end - age) / (float) Math.max(1, end - start);
        int alpha = Math.max(0, Math.min(210, Math.round(210.0F * fade)));
        graphics.fill(0, 0, this.width, this.height, alpha << 24 | 0x00FF0000);
    }

    private void drawCurtainTransition(GuiGraphics graphics) {
        if (this.curtainTransitionTicks <= 0) {
            return;
        }
        int elapsed = 54 - this.curtainTransitionTicks;
        int frame = elapsed < 24 ? 1 + elapsed / 3
                : elapsed < 30 ? 0
                : Math.max(1, 8 - (elapsed - 30) / 3);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(CURTAIN_TEXTURES[frame], 0, 0, this.width, this.height,
                0.0F, 0.0F, 640, 480, 640, 480);
        RenderSystem.disableBlend();
    }

    private static boolean isGranCurtainTransition(int fromProfileId, int toProfileId) {
        return fromProfileId != toProfileId
                && isGranCurtainProfile(fromProfileId) && isGranCurtainProfile(toProfileId);
    }

    private static boolean isGranCurtainProfile(int profileId) {
        return switch (profileId) {
            case 560, 561, 564, 566, 567, 568, 579, 570 -> true;
            default -> false;
        };
    }

    private void applyPendingCurtainPhase() {
        PendingCurtainPhase pending = this.pendingCurtainPhase;
        if (pending == null) {
            return;
        }
        boolean environmentChanged = this.battleProfileId != pending.battleProfileId();
        this.battleProfileId = pending.battleProfileId();
        this.actingEnemyIndex = Math.max(0, Math.min(
                Math.max(0, pending.enemies().size() - 1), pending.actingEnemyIndex()));
        replaceEnemies(pending.enemies(), true);
        updateBattleEnvironment(environmentChanged);
        this.enemyAnimationId = pending.enemyAnimationId();
        captureEnemyVisual();
        this.enemyAlpha = 1.0F;
        this.phaseTransitionTicks = 0;
        this.pendingCurtainPhase = null;
    }

    private void drawDomainBanner(GuiGraphics graphics) {
        TurnBattleDomainData.Domain domain = TurnBattleDomainData.get(this.battleProfileId);
        if (domain == null || this.domainBannerStartedAt < 0L) {
            return;
        }
        long elapsed = System.currentTimeMillis() - this.domainBannerStartedAt;
        if (elapsed < 0L || elapsed >= 5200L) {
            return;
        }
        float alpha = elapsed < 500L ? elapsed / 500.0F
                : elapsed > 4500L ? (5200L - elapsed) / 700.0F : 1.0F;
        int lineCount = 1 + domain.lines().size();
        int contentHeight = 24 + lineCount * (this.font.lineHeight + 4);
        int bannerHeight = Math.max(116, contentHeight * 2);
        int top = Math.max(18, (this.height - statusHeight() - bannerHeight) / 2);
        FadedBannerRenderer.draw(graphics, 0, top, this.width,
                top + bannerHeight, alpha);
        int alphaByte = Math.max(0, Math.min(255, Math.round(255.0F * alpha)));
        int titleColor = alphaByte << 24 | 0x00FF7D3F;
        int descriptionColor = alphaByte << 24 | 0x00E86E55;
        int textHeight = this.font.lineHeight + 6
                + domain.lines().size() * (this.font.lineHeight + 4);
        int textX = 16;
        int y = top + (bannerHeight - textHeight) / 2;
        graphics.drawString(this.font, domain.title(), textX, y, titleColor, false);
        y += this.font.lineHeight + 6;
        for (String line : domain.lines()) {
            graphics.drawString(this.font, line, textX, y, descriptionColor, false);
            y += this.font.lineHeight + 4;
        }
    }

    private void scheduleDomainBanner(int profileId, long delayMillis) {
        TurnBattleDomainData.Domain domain = TurnBattleDomainData.get(profileId);
        if (domain == null || !this.shownDomainTitles.add(domain.title())) {
            return;
        }
        this.domainBannerStartedAt = System.currentTimeMillis() + delayMillis;
        this.domainBannerSoundPlayed = false;
    }

    private void drawEnemy(GuiGraphics graphics) {
        if (this.enemyAlpha <= 0.0F) {
            return;
        }
        List<EnemyGeometry> geometries = enemyGeometries();
        for (int i = 0; i < geometries.size(); i++) {
            EnemyGeometry geometry = geometries.get(i);
            EnemyVisual enemy = this.enemies.get(i);
            float alpha = Math.min(this.enemyAlpha, enemy.displayAlpha);
            if (alpha <= 0.01F) {
                continue;
            }
            if (isSkullHunterCharged(enemy)) {
                drawSkullHunterChargeAfterimages(graphics, geometry, alpha);
            }
            boolean bossCollapse = isBossCollapse(enemy) && enemy.health <= 0.0F;
            int collapseOffsetY = bossCollapse
                    ? Math.round(geometry.height * 0.48F * bossCollapseProgress(enemy)) : 0;
            int shake = i == this.lastTargetIndex && this.enemyHitTicks > 0
                    ? ((this.enemyHitTicks & 1) == 0 ? 3 : -3)
                    : bossCollapse ? ((enemy.collapseTicks & 1) == 0 ? 2 : -2) : 0;
            RenderSystem.enableBlend();
            if (bossCollapse) {
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE);
            }
            float greenBlue = i == this.lastTargetIndex && this.enemyHitTicks > 0
                    ? 0.28F : bossCollapse ? 1.0F : alpha < 1.0F ? 0.28F : 1.0F;
            RenderSystem.setShaderColor(1.0F, greenBlue, greenBlue, alpha);
            graphics.blit(geometry.texture, geometry.x + shake, geometry.y + collapseOffsetY,
                    geometry.width, geometry.height, 0.0F, 0.0F,
                    geometry.textureWidth, geometry.textureHeight,
                    geometry.textureWidth, geometry.textureHeight);
            if (bossCollapse) {
                RenderSystem.defaultBlendFunc();
            }
            if (this.view == View.TARGETS && i == this.targetSelection
                    && enemy.health > 0.0F) {
                float pulse = 0.20F + 0.18F * (0.5F + 0.5F
                        * (float) Math.sin(System.currentTimeMillis() / 95.0D));
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, pulse);
                graphics.blit(geometry.texture, geometry.x, geometry.y,
                        geometry.width, geometry.height, 0.0F, 0.0F,
                        geometry.textureWidth, geometry.textureHeight,
                        geometry.textureWidth, geometry.textureHeight);
                RenderSystem.defaultBlendFunc();
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    private boolean isSkullHunterCharged(EnemyVisual enemy) {
        return enemy.states.contains(32)
                && (enemy.profileId == 184 || enemy.profileId == 328 || enemy.profileId == 357);
    }

    private void drawSkullHunterChargeAfterimages(GuiGraphics graphics,
                                                    EnemyGeometry geometry, float enemyOpacity) {
        float cycle = Math.floorMod(System.currentTimeMillis(), 1050L) / 1050.0F;
        int centerX = geometry.x + geometry.width / 2;
        int bottomY = geometry.y + geometry.height;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        for (int layer = 0; layer < 3; layer++) {
            float progress = (cycle + layer / 3.0F) % 1.0F;
            float scale = 1.03F + progress * 0.24F;
            float alpha = enemyOpacity * (1.0F - progress) * 0.28F;
            int width = Math.max(1, Math.round(geometry.width * scale));
            int height = Math.max(1, Math.round(geometry.height * scale));
            int x = centerX - width / 2;
            int y = bottomY - height - Math.round(progress * geometry.height * 0.04F);
            RenderSystem.setShaderColor(0.88F, 0.94F, 1.0F, alpha);
            graphics.blit(geometry.texture, x, y, width, height,
                    0.0F, 0.0F, geometry.textureWidth, geometry.textureHeight,
                    geometry.textureWidth, geometry.textureHeight);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawEnemyHealth(GuiGraphics graphics) {
        if (this.enemyAlpha <= 0.0F) {
            return;
        }
        int statusTop = this.height - statusHeight();
        List<EnemyGeometry> geometries = enemyGeometries();
        for (int i = 0; i < geometries.size(); i++) {
            EnemyGeometry geometry = geometries.get(i);
            EnemyVisual enemy = this.enemies.get(i);
            if (enemy.displayAlpha <= 0.01F || isProtectedGranStage(i)) {
                continue;
            }
            int barWidth = Math.max(46, Math.min(150, geometry.cellWidth - 18));
            int barX = geometry.cellX + (geometry.cellWidth - barWidth) / 2;
            int actionY = Math.max(42, statusTop - 48);
            int barY = Math.max(54, statusTop - 27);
            graphics.fill(barX, actionY, barX + barWidth, actionY + 5, 0x80333333);
            int actionFill = Math.round(barWidth * Math.max(0.0F,
                    Math.min(1.0F, this.enemyActionPoints / ENEMY_MAX_AP)));
            graphics.fill(barX, actionY, barX + actionFill, actionY + 5, 0x8000C800);
            graphics.fill(barX, barY, barX + barWidth, barY + 6, 0xFF250909);
            float ratio = enemy.maxHealth <= 0.0F ? 0.0F
                    : enemy.displayHealth / enemy.maxHealth;
            graphics.fill(barX, barY,
                    barX + Math.round(barWidth * Math.max(0.0F, ratio)),
                    barY + 6, 0xFFE12D24);
            drawOriginalStateIcons(graphics, enemy.states, barX,
                    actionY - 18, barWidth, true);
        }
    }

    private void drawIntro(GuiGraphics graphics) {
        int h = Math.min(120, Math.max(84, this.height / 4));
        int y = this.height - h;
        BSGuiUtils.drawRMWindow(graphics, 0, y, this.width, h);
        int lineY = y + 15;
        String page = this.introPages.isEmpty()
                ? this.enemyName.getString() + "出现了！"
                : this.introPages.get(Math.min(this.introPageIndex, this.introPages.size() - 1));
        for (String rawLine : page.split("\n", -1)) {
            List<FormattedCharSequence> wrapped = this.font.split(
                    Component.literal(rawLine), Math.max(1, this.width - 28));
            if (wrapped.isEmpty()) {
                lineY += this.font.lineHeight + 3;
                continue;
            }
            for (FormattedCharSequence line : wrapped) {
                graphics.drawString(this.font, line, 14, lineY, 0xFFFFFFFF, false);
                lineY += this.font.lineHeight + 3;
            }
        }
        drawContinueArrow(graphics, this.width / 2, this.height - 13);
    }

    private void setIntroPages(Component intro) {
        this.introPages.clear();
        for (String page : intro.getString().split("\f", -1)) {
            if (!page.isBlank()) {
                this.introPages.add(page);
            }
        }
        this.introPageIndex = 0;
    }

    private void advanceIntro() {
        if (this.introPageIndex + 1 < this.introPages.size()) {
            this.introPageIndex++;
            return;
        }
        if (BSOriginalBattleProfileData.get(this.battleProfileId).preemptiveSkillId() > 0) {
            this.view = View.MESSAGE;
            this.canAct = false;
            this.pendingCanAct = false;
            this.message = Component.empty();
            NetworkHandler.sendToServer(new ServerboundTurnBattlePresentationPacket());
            return;
        }
        scheduleDomainBanner(this.battleProfileId, 0L);
        this.view = View.COMMAND;
        this.selection = 0;
    }

    private void drawBattleMessage(GuiGraphics graphics) {
        int y = 18;
        for (String line : this.message.getString().split("\\n", -1)) {
            graphics.drawString(this.font, line, 16, y, 0xFFFFFFFF, false);
            y += this.font.lineHeight + 3;
        }
    }

    private void drawResult(GuiGraphics graphics) {
        int itemLineHeight = this.font.lineHeight + 4;
        int desiredHeight = 58 + Math.max(1, this.rewardItems.size()) * itemLineHeight;
        int h = Math.min(this.height - 20, Math.max(84, desiredHeight));
        int y = this.height - h;
        BSGuiUtils.drawRMWindow(graphics, 0, y, this.width, h);
        String playerName = this.minecraft != null && this.minecraft.player != null
                ? this.minecraft.player.getDisplayName().getString() : "玩家";
        if (this.outcome == ClientboundTurnBattlePacket.Outcome.VICTORY) {
            graphics.drawString(this.font, playerName + "胜利！", 14, y + 14, 0xFFFFFFFF, false);
            graphics.drawString(this.font, "获得了 " + this.soulReward + "S 魂！", 14,
                    y + 14 + this.font.lineHeight + 4, 0xFFFFFFFF, false);
            int itemY = y + 14 + (this.font.lineHeight + 4) * 2 + 3;
            for (ItemStack stack : this.rewardItems) {
                if (itemY + this.font.lineHeight > this.height - 14) {
                    break;
                }
                String line = "获得了" + stack.getHoverName().getString() + "。";
                graphics.drawString(this.font,
                        this.font.plainSubstrByWidth(line, Math.max(20, this.width - 28)),
                        14, itemY, 0xFFFFFFFF, false);
                itemY += itemLineHeight;
            }
        } else {
            int lineY = y + 14;
            for (String line : this.message.getString().split("\\n", -1)) {
                graphics.drawString(this.font, line, 14, lineY, 0xFFFFFFFF, false);
                lineY += this.font.lineHeight + 4;
            }
        }
        drawContinueArrow(graphics, this.width / 2, this.height - 13);
    }

    private void drawStatusWindow(GuiGraphics graphics) {
        int h = statusHeight();
        int y = this.height - h;
        BSGuiUtils.drawRMWindow(graphics, 0, y, this.width, h);
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        List<ClientboundPartyStatePacket.Member> party = ClientPartyState.getMembers();
        if (party.size() > 1) {
            for (int i = 0; i < Math.min(4, party.size()); i++) {
                drawPartyBattleStatus(graphics, party.get(i), y + 9 + i * 30);
            }
            return;
        }
        String name = this.minecraft.player.getDisplayName().getString();
        int hp = Math.max(0, Math.round(this.displayPlayerHealth));
        int maxHp = Math.max(1, Math.round(this.displayPlayerMaxHealth));
        int mp = Math.max(0, (int) Math.round(this.displayMp));
        int maxMp = Math.max(1, (int) Math.round(this.displayMaxMp));
        int apPercent = (int) Math.round(100.0D * this.displayAp / Math.max(0.0001D, this.displayMaxAp));
        int rowY = y + 11;
        int nameEnd = Math.min(118, Math.max(72, this.width / 5));
        int hpX = Math.max(nameEnd + 42, Math.round(this.width * 0.40F));
        int usableWidth = Math.max(120, this.width - hpX - 22);
        int hpWidth = Math.max(60, Math.round(usableWidth * 0.42F));
        int mpWidth = Math.max(38, Math.round(usableWidth * 0.25F));
        int hpEnd = hpX + hpWidth;
        int mpX = hpEnd + 6;
        int mpEnd = mpX + mpWidth;
        int apX = mpEnd + 6;
        int apEnd = this.width - 10;
        int nameColor = this.playerDown ? 0xFFFF4444
                : this.displayPlayerHealth / Math.max(1.0F, this.displayPlayerMaxHealth) <= 0.25F
                ? 0xFFFFFF55 : 0xFFFFFFFF;
        drawBattlePortrait(graphics, 10, rowY,
                nameEnd - 16, rowY + this.font.lineHeight);
        graphics.drawString(this.font, name, 10, rowY, nameColor, false);
        if (this.view == View.TARGETS) {
            drawTargetStatusWindow(graphics, y, nameEnd);
            return;
        }
        int effectX = 10 + this.font.width(name) + 6;
        if (effectX < hpX - 6) {
            if (this.playerDown) {
                drawOriginalStateIcons(graphics, List.of(1), effectX, rowY - 3,
                        Math.min(16, hpX - effectX - 6), false);
                effectX += 17;
            }
            drawEffectIcons(graphics, this.minecraft.player, effectX, rowY - 3,
                    hpX - effectX - 6, false);
        }
        drawThinGauge(graphics, hpX, hpEnd, rowY, "HP", hp + " / " + maxHp,
                this.displayPlayerHealth / Math.max(1.0F, this.displayPlayerMaxHealth), 0xFFE02A2A);
        drawThinGauge(graphics, mpX, mpEnd, rowY, "MP", String.valueOf(mp),
                this.displayMp / Math.max(1.0D, this.displayMaxMp), 0xFF287FD8);
        drawThinGauge(graphics, apX, apEnd, rowY, "AP", apPercent + "%",
                this.displayAp / Math.max(0.0001D, this.displayMaxAp), 0xFF36C84A);
    }

    private void drawPartyBattleStatus(GuiGraphics graphics, ClientboundPartyStatePacket.Member member, int rowY) {
        boolean local = this.minecraft != null && this.minecraft.player != null
                && member.id().equals(this.minecraft.player.getUUID());
        int portraitX = 10;
        int portraitW = 28;
        graphics.enableScissor(portraitX, rowY, portraitX + portraitW, rowY + 22);
        RenderSystem.enableBlend();
        String avatar = local ? ClientSkillInfo.getAvatar() : member.avatar();
        BSAvatarRenderer.draw(graphics, BSAvatarRenderer.getTexture(avatar), avatar, portraitX, rowY - 9, 34);
        RenderSystem.disableBlend();
        graphics.disableScissor();
        int nameX = 43;
        graphics.drawString(this.font, member.name(), nameX, rowY + 6,
                member.downed() ? 0xFFFF4444 : 0xFFFFFFFF, false);
        int hpX = Math.max(130, Math.round(this.width * 0.36F));
        int usable = Math.max(160, this.width - hpX - 16);
        int hpEnd = hpX + Math.round(usable * 0.45F);
        int mpX = hpEnd + 6;
        int mpEnd = mpX + Math.round(usable * 0.27F);
        int apX = mpEnd + 6;
        int apEnd = this.width - 10;
        float health = member.downed() ? 0.0F : local ? this.displayPlayerHealth : member.health();
        float maxHealth = local ? this.displayPlayerMaxHealth : member.maxHealth();
        double mp = local ? this.displayMp : member.mp();
        double maxMp = local ? this.displayMaxMp : member.maxMp();
        double ap = local ? this.displayAp : member.ap();
        double maxAp = local ? this.displayMaxAp : member.maxAp();
        drawThinGauge(graphics, hpX, hpEnd, rowY + 6, "HP",
                Math.max(0, Math.round(health)) + " / " + Math.max(1, Math.round(maxHealth)),
                health / Math.max(1.0F, maxHealth), 0xFFE02A2A);
        drawThinGauge(graphics, mpX, mpEnd, rowY + 6, "MP", String.valueOf(Math.max(0, (int)Math.round(mp))),
                mp / Math.max(1.0D, maxMp), 0xFF287FD8);
        drawThinGauge(graphics, apX, apEnd, rowY + 6, "AP",
                (int)Math.round(100.0D * ap / Math.max(0.0001D, maxAp)) + "%",
                ap / Math.max(0.0001D, maxAp), 0xFF36C84A);
    }

    private void drawBattlePortrait(GuiGraphics graphics, int x, int top,
                                    int width, int bottom) {
        if (width <= 4 || bottom <= top) {
            return;
        }
        String avatar = ClientSkillInfo.getAvatar();
        if (avatar == null || avatar.isBlank()) {
            avatar = "knight_face";
        }
        int size = Math.max(width, bottom - top + 34);
        int drawY = top - Math.max(8, (size - (bottom - top)) / 2);
        graphics.enableScissor(x, top, x + width, bottom);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.playerDown ? 0.55F : 1.0F);
        BSAvatarRenderer.draw(graphics, BSAvatarRenderer.getTexture(avatar), avatar,
                x, drawY, size);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        graphics.disableScissor();
    }

    private void drawTargetStatusWindow(GuiGraphics graphics, int statusY, int nameEnd) {
        TargetListGeometry geometry = targetListGeometry(statusY, nameEnd);
        BSGuiUtils.drawRMWindow(graphics, geometry.x, geometry.y,
                geometry.width, geometry.height);
        int selectedRow = livingTargetRow(this.targetSelection);
        if (selectedRow < this.menuScrollRow) {
            this.menuScrollRow = selectedRow;
        } else if (selectedRow >= this.menuScrollRow + geometry.visibleRows) {
            this.menuScrollRow = selectedRow - geometry.visibleRows + 1;
        }
        List<Integer> targets = livingTargetIndices();
        int maxScroll = Math.max(0, targets.size() - geometry.visibleRows);
        this.menuScrollRow = Math.max(0, Math.min(this.menuScrollRow, maxScroll));
        for (int row = 0; row < geometry.visibleRows
                && this.menuScrollRow + row < targets.size(); row++) {
            int enemyIndex = targets.get(this.menuScrollRow + row);
            EnemyVisual enemy = this.enemies.get(enemyIndex);
            int rowY = geometry.y + 11 + row * geometry.rowHeight;
            if (enemyIndex == this.targetSelection) {
                int pulse = 0x70 + (int) (0x18
                        * (0.5D + 0.5D * Math.sin(System.currentTimeMillis() / 95.0D)));
                graphics.fill(geometry.x + 8, rowY - 2,
                        geometry.x + geometry.width - 8,
                        rowY + 13, pulse << 24 | 0x00705563);
            }
            graphics.drawString(this.font, enemy.name,
                    geometry.x + 12, rowY, 0xFFFFFFFF, false);
        }
    }

    private TargetListGeometry targetListGeometry(int statusY, int nameEnd) {
        int x = Math.max(nameEnd + 4, Math.min(140, this.width / 4));
        int width = Math.min(Math.max(150, this.width / 3),
                Math.max(80, this.width - x - 8));
        int height = statusHeight();
        int rowHeight = 18;
        int visibleRows = Math.max(1, (height - 18) / rowHeight);
        return new TargetListGeometry(x, statusY, width, height,
                rowHeight, visibleRows);
    }

    private List<Integer> livingTargetIndices() {
        List<Integer> targets = new ArrayList<>();
        for (int i = 0; i < this.enemies.size(); i++) {
            if (isLivingTarget(i)) {
                targets.add(i);
            }
        }
        return targets;
    }

    private boolean isLivingTarget(int index) {
        return index >= 0 && index < this.enemies.size()
                && this.enemies.get(index).health > 0.0F
                && !isProtectedGranStage(index);
    }

    private boolean isProtectedGranStage(int index) {
        if (this.battleProfileId != 570 || index < 0 || index >= this.enemies.size()) {
            return false;
        }
        int profileId = this.enemies.get(index).profileId;
        if (profileId < 570 || profileId > 577) {
            return false;
        }
        return this.enemies.stream().anyMatch(enemy -> enemy.profileId >= 580
                && enemy.profileId <= 586 && enemy.health > 0.0F);
    }

    private int livingTargetRow(int enemyIndex) {
        List<Integer> targets = livingTargetIndices();
        int row = targets.indexOf(enemyIndex);
        return Math.max(0, row);
    }

    private void moveTargetSelection(int offset) {
        List<Integer> targets = livingTargetIndices();
        if (targets.isEmpty()) {
            return;
        }
        int row = targets.indexOf(this.targetSelection);
        row = row < 0 ? 0 : Math.floorMod(row + offset, targets.size());
        int next = targets.get(row);
        if (next != this.targetSelection) {
            this.targetSelection = next;
            playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
        }
    }

    private void drawEffectIcons(GuiGraphics graphics, LivingEntity entity, int x, int y,
                                 int availableWidth, boolean centered) {
        List<MobEffectInstance> effects = entity.getActiveEffects().stream()
                .filter(effect -> {
                    ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
                    return id != null && BlackSouls.MODID.equals(id.getNamespace());
                })
                .toList();
        if (effects.isEmpty() || availableWidth < 12 || this.minecraft == null) {
            return;
        }
        int iconSize = 16;
        int gap = 1;
        int maxIcons = Math.max(1, (availableWidth + gap) / (iconSize + gap));
        int count = Math.min(maxIcons, effects.size());
        int totalWidth = count * iconSize + Math.max(0, count - 1) * gap;
        int drawX = centered ? x + (availableWidth - totalWidth) / 2 : x;
        int index = 0;
        for (MobEffectInstance effect : effects) {
            if (index >= count) {
                break;
            }
            TextureAtlasSprite sprite = this.minecraft.getMobEffectTextures().get(effect.getEffect());
            int iconX = drawX + index * (iconSize + gap);
            graphics.fill(iconX - 1, y - 1, iconX + iconSize + 1, y + iconSize + 1, 0xA0000000);
            graphics.blit(iconX, y, 0, iconSize, iconSize, sprite);
            int remainingTurns = (effect.getDuration() + 199) / 200;
            if (remainingTurns > 0 && remainingTurns <= 99) {
                String turns = String.valueOf(remainingTurns);
                graphics.drawString(this.font, turns,
                        iconX + iconSize - this.font.width(turns), y - 2,
                        0xFFFFFF55, false);
            }
            if (effect.getAmplifier() > 0) {
                String level = String.valueOf(effect.getAmplifier() + 1);
                graphics.drawString(this.font, level,
                        iconX + iconSize - this.font.width(level), y + iconSize - this.font.lineHeight,
                        0xFFFFFF55, false);
            }
            index++;
        }
    }

    private void drawOriginalStateIcons(GuiGraphics graphics, List<Integer> states,
                                        int x, int y, int availableWidth,
                                        boolean centered) {
        List<BSOriginalStateData.Entry> visible = states.stream()
                .map(BSOriginalStateData::get)
                .filter(java.util.Objects::nonNull)
                .filter(state -> state.iconIndex() > 0)
                .toList();
        if (visible.isEmpty() || availableWidth < 12) {
            return;
        }
        ResourceLocation iconSet = new ResourceLocation(
                BlackSouls.MODID, "textures/gui/battle/icon_set.png");
        int iconSize = 16;
        int gap = 1;
        int maxIcons = Math.max(1, (availableWidth + gap) / (iconSize + gap));
        int count = Math.min(maxIcons, visible.size());
        int totalWidth = count * iconSize + Math.max(0, count - 1) * gap;
        int drawX = centered ? x + (availableWidth - totalWidth) / 2 : x;
        RenderSystem.enableBlend();
        for (int index = 0; index < count; index++) {
            int iconIndex = visible.get(index).iconIndex();
            int sourceX = iconIndex % 16 * 24;
            int sourceY = iconIndex / 16 * 24;
            int iconX = drawX + index * (iconSize + gap);
            graphics.fill(iconX - 1, y - 1,
                    iconX + iconSize + 1, y + iconSize + 1, 0xA0000000);
            graphics.blit(iconSet, iconX, y, iconSize, iconSize,
                    sourceX, sourceY, 24, 24, 384, 1300);
        }
        RenderSystem.disableBlend();
    }

    private List<EnemyGeometry> enemyGeometries() {
        if (this.enemies.isEmpty()) {
            return List.of();
        }
        int count = this.enemies.size();
        int margin = Math.max(10, this.width / 45);
        int availableWidth = Math.max(1, this.width - margin * 2);
        if (this.battleProfileId == 570 && count >= 8) {
            return granStageGeometries(margin, availableWidth);
        }
        int cellWidth = Math.max(1, availableWidth / count);
        int statusTop = this.height - statusHeight();
        int spriteBottom = Math.max(76, statusTop - 72);
        int maxHeight = Math.min(count == 1 ? 128 : 112,
                Math.max(52, statusTop - 116));
        List<EnemyGeometry> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            EnemyVisual enemy = this.enemies.get(index);
            ResourceLocation texture = RABBIT_TEXTURE;
            int textureWidth = 127;
            int textureHeight = 121;
            if (enemy.profileId > 0) {
                BSOriginalEnemyData.Entry profile =
                        BSOriginalEnemyData.get(enemy.profileId);
                texture = profile.texture();
                textureWidth = Math.max(1, profile.textureWidth());
                textureHeight = Math.max(1, profile.textureHeight());
            }
            int spriteHeight = enemy.profileId == 579
                    ? Math.min(Math.max(maxHeight, 220), Math.max(52, statusTop - 70))
                    : maxHeight;
            int spriteWidth = Math.max(1, Math.round(spriteHeight
                    * (float) textureWidth / textureHeight));
            int maxWidth = Math.max(34, cellWidth - 20);
            if (spriteWidth > maxWidth) {
                spriteHeight = Math.max(1,
                        Math.round(spriteHeight * (float) maxWidth / spriteWidth));
                spriteWidth = maxWidth;
            }
            int cellX = margin + index * cellWidth;
            int spriteX = cellX + (cellWidth - spriteWidth) / 2;
            int spriteY = spriteBottom - spriteHeight;
            result.add(new EnemyGeometry(cellX, cellWidth, spriteX, spriteY,
                    spriteWidth, spriteHeight, texture, textureWidth, textureHeight));
        }
        return result;
    }

    private List<EnemyGeometry> granStageGeometries(int margin, int availableWidth) {
        int statusTop = this.height - statusHeight();
        int stageBottom = Math.max(88, statusTop - 76);
        int fragmentBottom = Math.max(82, statusTop - 70);
        int fragmentCount = this.enemies.size() - 1;
        int fragmentCellWidth = Math.max(1, availableWidth / Math.max(1, fragmentCount));
        List<EnemyGeometry> result = new ArrayList<>(this.enemies.size());
        result.add(createEnemyGeometry(0, margin, availableWidth, stageBottom,
                Math.min(176, Math.max(96, statusTop - 112)),
                Math.max(80, availableWidth - 36)));
        for (int index = 1; index < this.enemies.size(); index++) {
            int cellX = margin + (index - 1) * fragmentCellWidth;
            result.add(createEnemyGeometry(index, cellX, fragmentCellWidth,
                    fragmentBottom, Math.min(76, Math.max(46, statusTop - 150)),
                    Math.max(28, fragmentCellWidth - 12)));
        }
        return result;
    }

    private EnemyGeometry createEnemyGeometry(int index, int cellX, int cellWidth,
                                              int spriteBottom, int maxHeight,
                                              int maxWidth) {
        EnemyVisual enemy = this.enemies.get(index);
        ResourceLocation texture = RABBIT_TEXTURE;
        int textureWidth = 127;
        int textureHeight = 121;
        if (enemy.profileId > 0) {
            BSOriginalEnemyData.Entry profile = BSOriginalEnemyData.get(enemy.profileId);
            texture = profile.texture();
            textureWidth = Math.max(1, profile.textureWidth());
            textureHeight = Math.max(1, profile.textureHeight());
        }
        int spriteHeight = maxHeight;
        int spriteWidth = Math.max(1, Math.round(spriteHeight
                * (float) textureWidth / textureHeight));
        if (spriteWidth > maxWidth) {
            spriteHeight = Math.max(1, Math.round(spriteHeight
                    * (float) maxWidth / spriteWidth));
            spriteWidth = maxWidth;
        }
        int spriteX = cellX + (cellWidth - spriteWidth) / 2;
        int spriteY = spriteBottom - spriteHeight;
        return new EnemyGeometry(cellX, cellWidth, spriteX, spriteY,
                spriteWidth, spriteHeight, texture, textureWidth, textureHeight);
    }

    private void drawThinGauge(GuiGraphics graphics, int x, int end, int y, String label,
                               String value, double ratio, int color) {
        int labelWidth = this.font.width(label);
        int barX = x;
        int barBottom = y + this.font.lineHeight;
        int barHeight = Math.max(1, this.font.lineHeight / 2);
        int barY = barBottom - barHeight;
        graphics.fill(barX, barY, end, barBottom, 0xFF270B0B);
        int fillEnd = barX + (int) Math.round((end - barX) * Math.max(0.0D, Math.min(1.0D, ratio)));
        graphics.fill(barX, barY, fillEnd, barBottom, color);
        graphics.drawString(this.font, label, x, y, 0xFF7FB8FF, false);
        int valueX = Math.min(end - this.font.width(value), x + labelWidth + 5);
        graphics.drawString(this.font, value, Math.max(x + labelWidth + 2, valueX), y, 0xFFFFFFFF, false);
    }

    private void drawBattleEffects(GuiGraphics graphics) {
        int statusTop = this.height - statusHeight();
        float canvasScale = Math.max(0.45F,
                Math.min(this.width / 544.0F, Math.max(1, statusTop) / 416.0F));
        List<EnemyGeometry> geometries = enemyGeometries();
        int playerCenterX = this.width / 2;
        int playerCenterY = Math.max(42, statusTop - 34);

        Iterator<ActiveBattleVfx> iterator = this.activeBattleVfx.iterator();
        while (iterator.hasNext()) {
            ActiveBattleVfx active = iterator.next();
            boolean enemyTarget = active.target == TurnBattleVfxResolver.Target.ENEMY;
            int effectTarget = Math.max(0, Math.min(
                    Math.max(0, geometries.size() - 1), active.targetIndex));
            int enemyCenterX = geometries.isEmpty() ? this.width / 2
                    : geometries.get(effectTarget).x
                    + geometries.get(effectTarget).width / 2;
            int enemyCenterY = geometries.isEmpty()
                    ? enemySpriteY() + enemySpriteHeight() / 2
                    : geometries.get(effectTarget).y
                    + geometries.get(effectTarget).height / 2;
            boolean alive = BattleScreenVFXRenderer.render(graphics, active.animationId, active.startedAt,
                    enemyTarget ? enemyCenterX : playerCenterX,
                    enemyTarget ? enemyCenterY : playerCenterY,
                    canvasScale);
            if (!alive) {
                iterator.remove();
            }
        }

        Iterator<DamagePopup> popupIterator = this.enemyDamagePopups.iterator();
        while (popupIterator.hasNext()) {
            DamagePopup popup = popupIterator.next();
            float frame = (System.currentTimeMillis() - popup.startedAt) * 60.0F / 1000.0F;
            if (frame >= 30.0F) {
                popupIterator.remove();
                continue;
            }
            int popupTarget = Math.max(0, Math.min(
                    Math.max(0, geometries.size() - 1), popup.targetIndex));
            int enemyCenterX = geometries.isEmpty() ? this.width / 2
                    : geometries.get(popupTarget).x + geometries.get(popupTarget).width / 2;
            int enemyCenterY = geometries.isEmpty()
                    ? enemySpriteY() + enemySpriteHeight() / 2
                    : geometries.get(popupTarget).y + geometries.get(popupTarget).height / 2;
            int laneOffset = (popup.wave % 3 - 1) * 9;
            drawDamagePopup(graphics, popup.damage, popup.startedAt,
                    enemyCenterX + laneOffset, enemyCenterY, popup.critical);
        }
        Iterator<PlayerDamagePopup> playerPopupIterator = this.playerDamagePopups.iterator();
        while (playerPopupIterator.hasNext()) {
            PlayerDamagePopup popup = playerPopupIterator.next();
            float frame = (System.currentTimeMillis() - popup.startedAt()) * 60.0F / 1000.0F;
            if (frame >= 30.0F) {
                playerPopupIterator.remove();
                continue;
            }
            int laneOffset = (popup.wave() % 3 - 1) * 9;
            drawDamagePopup(graphics, popup.damage(), popup.startedAt(),
                    playerCenterX + laneOffset, playerCenterY - 16, popup.critical());
        }
        if (!this.incomingSequenceActive) {
            drawDamagePopup(graphics, this.playerDamageNumber, this.playerDamagePopupStartedAt,
                    playerCenterX, playerCenterY - 16, false);
        }
    }

    private void drawDamagePopup(GuiGraphics graphics, int damage, long startedAt,
                                 int centerX, int baseY, boolean critical) {
        if (damage <= 0 || startedAt < 0L) {
            return;
        }
        float frame = (System.currentTimeMillis() - startedAt) * 60.0F / 1000.0F;
        if (frame < 0.0F || frame >= 30.0F) {
            return;
        }
        float offset;
        if (frame < 2.0F) {
            offset = -4.0F * frame;
        } else if (frame < 4.0F) {
            offset = -8.0F - 2.0F * (frame - 2.0F);
        } else if (frame < 6.0F) {
            offset = -12.0F + 2.0F * (frame - 4.0F);
        } else if (frame < 12.0F) {
            offset = -8.0F + 4.0F * (frame - 6.0F);
        } else {
            offset = 16.0F;
        }
        float opacity = Math.max(0.0F, Math.min(1.0F, (26.0F - frame) / 8.0F));
        int alpha = Math.round(opacity * 255.0F);
        int color = alpha << 24 | (critical ? 0x00FF5555 : 0x00FFFFFF);
        String text = Integer.toString(damage);

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, baseY + offset, 280.0F);
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        int x = -this.font.width(text) / 2;
        graphics.drawString(this.font, text, x, 0, color, false);
        graphics.pose().popPose();
    }

    private void queueBattleVfx(TurnBattleVfxResolver.Cue cue, boolean playSounds) {
        queueBattleVfx(cue, playSounds, this.lastTargetIndex);
    }

    private void queueBattleVfx(TurnBattleVfxResolver.Cue cue, boolean playSounds,
                                int targetIndex) {
        if (cue != null && cue.valid()) {
            this.activeBattleVfx.add(new ActiveBattleVfx(
                    cue.animationId(), cue.target(), targetIndex,
                    System.currentTimeMillis(), playSounds));
        }
    }

    private void tickBattleVfxSounds() {
        long now = System.currentTimeMillis();
        for (ActiveBattleVfx active : this.activeBattleVfx) {
            if (!active.playSounds) {
                continue;
            }
            VFXAnimation animation = AnimationRegistry.ANIMATIONS.get(active.animationId);
            if (animation == null) {
                continue;
            }
            int frame = (int) ((now - active.startedAt) / VFX_FRAME_DURATION_MILLIS);
            while (active.nextSoundIndex < animation.soundTimings.size()) {
                VFXSoundTiming timing = animation.soundTimings.get(active.nextSoundIndex);
                if (timing.frame() > frame) {
                    break;
                }
                SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(timing.sound());
                if (sound != null) {
                    playUiSound(sound, timing.pitch(), timing.volume());
                }
                active.nextSoundIndex++;
            }
        }
    }

    private int playerImpactAge(TurnBattleVfxResolver.Cue cue) {
        if (cue == null || !cue.valid()) {
            return 5;
        }
        VFXAnimation animation = AnimationRegistry.ANIMATIONS.get(cue.animationId());
        if (animation == null || animation.frames.isEmpty()) {
            return 5;
        }
        int animationTicks = (int) Math.ceil(
                animation.frames.size() * VFX_FRAME_DURATION_MILLIS / 50.0D);
        return Math.max(5, 2 + animationTicks);
    }

    private int scheduleDamageHits(List<ClientboundTurnBattlePacket.DamageHit> hits,
                                   int animationImpactAge) {
        this.scheduledEnemyHits.clear();
        if (hits.isEmpty()) {
            return animationImpactAge;
        }
        int maxWave = hits.stream().mapToInt(ClientboundTurnBattlePacket.DamageHit::wave)
                .max().orElse(0);
        int firstAge = maxWave == 0 ? animationImpactAge : 5;
        int interval = maxWave == 0 ? 0
                : Math.max(3, (animationImpactAge - firstAge) / maxWave);
        Map<Integer, Integer> lastWaveByTarget = new HashMap<>();
        for (ClientboundTurnBattlePacket.DamageHit hit : hits) {
            lastWaveByTarget.merge(hit.targetEntityId(), hit.wave(), Math::max);
        }
        int lastAge = firstAge;
        for (ClientboundTurnBattlePacket.DamageHit hit : hits) {
            int targetIndex = enemyIndexByEntityId(hit.targetEntityId());
            if (targetIndex < 0) {
                continue;
            }
            int triggerAge = firstAge + hit.wave() * interval;
            boolean finalHit = hit.wave() >= lastWaveByTarget.getOrDefault(
                    hit.targetEntityId(), hit.wave());
            this.scheduledEnemyHits.add(new ScheduledDamageHit(targetIndex,
                    hit.damage(), hit.critical(), triggerAge, finalHit, hit.wave()));
            lastAge = Math.max(lastAge, triggerAge);
        }
        return Math.max(animationImpactAge, lastAge);
    }

    private int scheduleDamageHitsFrom(List<ClientboundTurnBattlePacket.DamageHit> hits,
                                       int firstAge) {
        this.scheduledEnemyHits.clear();
        if (hits.isEmpty()) {
            return firstAge;
        }
        Map<Integer, Integer> lastWaveByTarget = new HashMap<>();
        for (ClientboundTurnBattlePacket.DamageHit hit : hits) {
            lastWaveByTarget.merge(hit.targetEntityId(), hit.wave(), Math::max);
        }
        int lastAge = firstAge;
        for (ClientboundTurnBattlePacket.DamageHit hit : hits) {
            int targetIndex = enemyIndexByEntityId(hit.targetEntityId());
            if (targetIndex < 0) {
                continue;
            }
            int triggerAge = firstAge + Math.max(0, hit.wave()) * 5;
            boolean finalHit = hit.wave() >= lastWaveByTarget.getOrDefault(
                    hit.targetEntityId(), hit.wave());
            this.scheduledEnemyHits.add(new ScheduledDamageHit(targetIndex,
                    hit.damage(), hit.critical(), triggerAge, finalHit, hit.wave()));
            lastAge = Math.max(lastAge, triggerAge);
        }
        return lastAge;
    }

    private int scheduleIncomingHits(List<ClientboundTurnBattlePacket.IncomingHit> hits,
                                     int firstAge) {
        this.scheduledIncomingHits.clear();
        this.scheduledRevivals.clear();
        this.incomingSequenceActive = !hits.isEmpty();
        int age = firstAge;
        int wave = 0;
        for (ClientboundTurnBattlePacket.IncomingHit hit : hits) {
            this.scheduledIncomingHits.add(new ScheduledIncomingHit(hit, age, wave++));
            if (hit.knockedDown() && hit.revived()) {
                int reviveAge = age + 10;
                this.scheduledRevivals.add(new ScheduledRevival(
                        reviveAge, Math.max(1, hit.reviveHealth())));
                age = reviveAge + 6;
            } else {
                age += 7;
            }
        }
        return hits.isEmpty() ? 0 : age;
    }

    private void triggerIncomingHits() {
        Iterator<ScheduledIncomingHit> iterator = this.scheduledIncomingHits.iterator();
        while (iterator.hasNext()) {
            ScheduledIncomingHit scheduled = iterator.next();
            if (scheduled.triggerAge() > this.effectAge) {
                break;
            }
            iterator.remove();
            ClientboundTurnBattlePacket.IncomingHit hit = scheduled.hit();
            this.enemyActionPoints = 0.0F;
            this.enemyActionWaitTicks = 0;
            this.displayPlayerHealth = hit.knockedDown()
                    ? 0.0F : Math.max(0.0F, this.displayPlayerHealth - hit.damage());
            this.playerHitTicks = 10;
            this.playerDamagePopups.add(new PlayerDamagePopup(
                    hit.damage(), hit.critical(), System.currentTimeMillis(), scheduled.wave()));
            playUiSound(BlackSouls.TURN_PLAYER_DAMAGE_EVENT.get(), 1.0F);
            String playerName = this.minecraft != null && this.minecraft.player != null
                    ? this.minecraft.player.getDisplayName().getString() : "玩家";
            StringBuilder text = new StringBuilder(this.activeEnemyActionHeader);
            if (hit.critical()) {
                text.append("\n会心一击！");
            }
            text.append("\n").append(playerName).append("受到了 ")
                    .append(hit.damage()).append(" 点伤害！");
            if (hit.knockedDown()) {
                this.playerDown = true;
                text.append("\n").append(playerName).append("倒下了！");
                playUiSound(BlackSouls.PLAYER_DEATH_EVENT.get(), 1.0F);
            }
            this.message = Component.literal(text.toString());
        }
    }

    private void triggerScheduledRevivals() {
        Iterator<ScheduledRevival> iterator = this.scheduledRevivals.iterator();
        while (iterator.hasNext()) {
            ScheduledRevival revival = iterator.next();
            if (revival.triggerAge() > this.effectAge) {
                break;
            }
            iterator.remove();
            this.playerDown = false;
            this.displayPlayerHealth = revival.health();
            String playerName = this.minecraft != null && this.minecraft.player != null
                    ? this.minecraft.player.getDisplayName().getString() : "玩家";
            this.message = Component.literal(this.activeEnemyActionHeader
                    + "\n" + playerName + "复活了！");
        }
    }

    private int enemyIndexByEntityId(int entityId) {
        for (int i = 0; i < this.enemies.size(); i++) {
            if (this.enemies.get(i).entityId == entityId) {
                return i;
            }
        }
        return -1;
    }

    private void triggerScheduledDamageHits() {
        boolean soundPlayed = false;
        Iterator<ScheduledDamageHit> iterator = this.scheduledEnemyHits.iterator();
        while (iterator.hasNext()) {
            ScheduledDamageHit hit = iterator.next();
            if (hit.triggerAge > this.effectAge) {
                break;
            }
            iterator.remove();
            if (hit.targetIndex < 0 || hit.targetIndex >= this.enemies.size()) {
                continue;
            }
            EnemyVisual enemy = this.enemies.get(hit.targetIndex);
            enemy.health = Math.max(enemy.finalHealth, enemy.health - hit.damage);
            if (hit.finalHit) {
                enemy.health = enemy.finalHealth;
            }
            this.lastTargetIndex = hit.targetIndex;
            this.enemyHitTicks = 6;
            long now = System.currentTimeMillis();
            this.enemyDamagePopups.add(new DamagePopup(hit.targetIndex,
                    hit.damage, hit.critical, now, hit.wave));
            if (!soundPlayed) {
                playUiSound(BlackSouls.TURN_ENEMY_DAMAGE_EVENT.get(), 1.0F);
                soundPlayed = true;
            }
        }
        syncPrimaryEnemy();
    }

    public void queueExternalAnimation(int animationId, double x, double y, double z) {
        TurnBattleVfxResolver.Target target = TurnBattleVfxResolver.Target.ENEMY;
        if (this.minecraft != null && this.minecraft.player != null && this.minecraft.level != null) {
            net.minecraft.world.entity.Entity enemy = this.minecraft.level.getEntity(this.entityId);
            double playerDistance = this.minecraft.player.distanceToSqr(x, y, z);
            double enemyDistance = enemy == null ? Double.MAX_VALUE : enemy.distanceToSqr(x, y, z);
            target = playerDistance < enemyDistance
                    ? TurnBattleVfxResolver.Target.PLAYER
                    : TurnBattleVfxResolver.Target.ENEMY;
        }
        queueBattleVfx(new TurnBattleVfxResolver.Cue(animationId, target), true);
    }

    private int enemySpriteHeight() {
        int statusTop = this.height - statusHeight();
        return Math.min(128, Math.max(68, statusTop / 3));
    }

    private int enemySpriteY() {
        return Math.max(34, this.height - statusHeight() - enemySpriteHeight() - 74);
    }

    private int statusHeight() {
        int members = Math.max(1, Math.min(4, ClientPartyState.getMembers().size()));
        return members > 1 ? 18 + members * 30 : Math.min(120, Math.max(84, this.height / 4));
    }

    private void drawCommandWindow(GuiGraphics graphics, int mouseX, int mouseY) {
        List<MenuEntry> entries = currentEntries();
        if (this.view == View.WEAPONS) {
            drawWeaponChangeScreen(graphics, mouseX, mouseY);
            return;
        }
        if (isGridMenu()) {
            drawGridMenu(graphics, mouseX, mouseY, entries);
            return;
        }
        MenuGeometry geometry = menuGeometry(entries.size());
        BSGuiUtils.drawRMWindow(graphics, 0, geometry.y, geometry.width, geometry.height);
        int activeSelection = this.view == View.TARGETS
                ? (this.pendingTargetAction == ServerboundTurnBattleActionPacket.Action.SKILL ? 1 : 0)
                : this.selection;
        for (int visible = 0; visible < geometry.visibleRows
                && geometry.first + visible < entries.size(); visible++) {
            int index = geometry.first + visible;
            MenuEntry entry = entries.get(index);
            int rowY = geometry.y + 11 + visible * 18;
            boolean hovered = this.view != View.TARGETS
                    && mouseX >= 8 && mouseX < geometry.width - 8
                    && mouseY >= rowY - 2 && mouseY < rowY + 14;
            if (index == activeSelection || hovered) {
                graphics.fill(8, rowY - 2, geometry.width - 8, rowY + 13,
                        index == activeSelection ? 0x88705563 : 0x55444444);
            }
            graphics.drawString(this.font, entry.label, 12, rowY,
                    entry.enabled ? 0xFFFFFFFF : 0xFF777777, false);
        }
    }

    private void drawGridMenu(GuiGraphics graphics, int mouseX, int mouseY, List<MenuEntry> entries) {
        GridGeometry geometry = gridGeometry(entries.size());
        BSGuiUtils.drawRMWindow(graphics, 0, 0, this.width, geometry.descriptionHeight);
        BSGuiUtils.drawRMWindow(graphics, 0, geometry.listY, this.width, geometry.listHeight);
        drawSelectedDescription(graphics, entries, geometry);

        int firstIndex = geometry.firstRow * 2;
        int lastIndex = Math.min(entries.size(), firstIndex + geometry.visibleRows * 2);
        for (int index = firstIndex; index < lastIndex; index++) {
            MenuEntry entry = entries.get(index);
            int column = index & 1;
            int row = index / 2 - geometry.firstRow;
            int cellX = column * geometry.columnWidth;
            int rowY = geometry.listY + 11 + row * geometry.rowHeight;
            int left = cellX + 8;
            int right = cellX + geometry.columnWidth - 8;
            boolean hovered = mouseX >= left && mouseX < right
                    && mouseY >= rowY - 2 && mouseY < rowY + 18;
            if (index == this.selection || hovered) {
                graphics.fill(left, rowY - 2, right, rowY + 18,
                        index == this.selection ? 0x88705563 : 0x55444444);
            }
            if (entry.item != null && !entry.item.isEmpty()) {
                graphics.renderItem(entry.item, left + 2, rowY);
            } else if (entry.icon != null) {
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                graphics.blit(entry.icon, left + 2, rowY, 16, 16,
                        0.0F, 0.0F, 24, 24, 24, 24);
                RenderSystem.disableBlend();
            }
            int textX = left + 22;
            int textY = rowY + 4;
            int amountWidth = entry.amount.isEmpty() ? 0 : this.font.width(entry.amount);
            int labelEnd = right - (amountWidth == 0 ? 4 : amountWidth + 12);
            graphics.enableScissor(textX, rowY, Math.max(textX + 1, labelEnd), rowY + 17);
            graphics.drawString(this.font, entry.label, textX, textY,
                    entry.enabled ? 0xFFFFFFFF : 0xFF777777, false);
            graphics.disableScissor();
            if (!entry.amount.isEmpty()) {
                int amountColor = this.view == View.SKILLS
                        ? (entry.amount.startsWith("CD") ? 0xFFCC99FF : 0xFF55FFFF)
                        : 0xFFFFFFFF;
                graphics.drawString(this.font, entry.amount, right - amountWidth - 4, textY,
                        amountColor, false);
            }
        }
    }

    private void drawWeaponChangeScreen(GuiGraphics graphics, int mouseX, int mouseY) {
        List<MenuEntry> entries = inventoryEntries(true);
        WeaponGeometry geometry = weaponGeometry(entries.size());
        BSGuiUtils.drawRMWindow(graphics, 0, 0, this.width, geometry.descriptionHeight);
        BSGuiUtils.drawRMWindow(graphics, 0, geometry.bodyY, geometry.statsWidth, geometry.bodyHeight);
        BSGuiUtils.drawRMWindow(graphics, geometry.equipmentX, geometry.bodyY,
                this.width - geometry.equipmentX, geometry.bodyHeight);
        BSGuiUtils.drawRMWindow(graphics, 0, geometry.listY, this.width, geometry.listHeight);
        drawWeaponDescription(graphics, entries, geometry);
        drawWeaponStats(graphics, geometry);
        drawEquipmentOverview(graphics, geometry);
        drawWeaponInventory(graphics, mouseX, mouseY, entries, geometry);
    }

    private void drawWeaponDescription(GuiGraphics graphics, List<MenuEntry> entries, WeaponGeometry geometry) {
        if (entries.isEmpty() || this.selection < 0 || this.selection >= entries.size()) {
            return;
        }
        MenuEntry entry = entries.get(this.selection);
        List<Component> description = entry.description.isEmpty()
                ? List.of(entry.label)
                : entry.description;
        int y = 11;
        int bottom = geometry.descriptionHeight - 9;
        for (Component line : description) {
            for (FormattedCharSequence wrapped : this.font.split(line, Math.max(40, this.width - 24))) {
                if (y + this.font.lineHeight > bottom) {
                    return;
                }
                graphics.drawString(this.font, wrapped, 12, y, 0xFFFFFFFF, false);
                y += this.font.lineHeight + 2;
            }
        }
    }

    private void drawWeaponStats(GuiGraphics graphics, WeaponGeometry geometry) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        BSPlayerStats stats = this.minecraft.player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        int x = 14;
        int y = geometry.bodyY + 13;
        graphics.drawString(this.font, this.minecraft.player.getDisplayName(), x, y, 0xFFFFFFFF, false);
        String[] labels = {"攻击力", "防御力", "魔力", "魔防御", "速度", "运"};
        double[] values = stats == null
                ? new double[]{0, 0, 0, 0, 0, 0}
                : new double[]{stats.attack, stats.defense, stats.magicAttack,
                stats.magicDefense, stats.speed, stats.luck};
        int rowHeight = Math.max(13, Math.min(18, (geometry.bodyHeight - 35) / labels.length));
        int valueRight = geometry.statsWidth - 18;
        for (int i = 0; i < labels.length; i++) {
            int rowY = y + 19 + i * rowHeight;
            graphics.drawString(this.font, labels[i], x, rowY, 0xFF75B8FF, false);
            String value = formatStat(values[i]);
            int valueX = Math.max(x + 54, valueRight - this.font.width(value) - 12);
            graphics.drawString(this.font, value, valueX, rowY, 0xFFFFFFFF, false);
            graphics.drawString(this.font, "→", valueRight - 7, rowY, 0xFFAAAAAA, false);
        }
    }

    private void drawEquipmentOverview(GuiGraphics graphics, WeaponGeometry geometry) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        int left = geometry.equipmentX + 9;
        int right = this.width - 9;
        int tabY = geometry.bodyY + 10;
        String[] tabs = {"装备变更", "最强装备", "全部卸下"};
        int tabWidth = Math.max(1, (right - left) / tabs.length);
        for (int i = 0; i < tabs.length; i++) {
            int tabLeft = left + i * tabWidth;
            int tabRight = i == tabs.length - 1 ? right : tabLeft + tabWidth;
            if (i == 0) {
                graphics.fill(tabLeft, tabY - 2, tabRight, tabY + this.font.lineHeight + 3, 0x88705563);
            }
            graphics.drawCenteredString(this.font, tabs[i], (tabLeft + tabRight) / 2,
                    tabY, i == 0 ? 0xFFFFFFFF : 0xFFDDDDDD);
        }

        ItemStack head = firstCurio("head");
        if (head.isEmpty()) {
            head = this.minecraft.player.getInventory().armor.get(3);
        }
        ItemStack body = firstCurio("body");
        if (body.isEmpty()) {
            body = this.minecraft.player.getInventory().armor.get(2);
        }
        ItemStack[] equipment = {
                this.minecraft.player.getMainHandItem(),
                this.minecraft.player.getOffhandItem(),
                head,
                body,
                firstCurio("ring")
        };
        String[] labels = {"武器", "盾", "头", "身体", "戒指"};
        int rowsTop = tabY + this.font.lineHeight + 13;
        int rowHeight = Math.max(18, (geometry.bodyY + geometry.bodyHeight - 10 - rowsTop) / labels.length);
        for (int i = 0; i < labels.length; i++) {
            int rowY = rowsTop + i * rowHeight;
            if (i == 0) {
                graphics.fill(left, rowY - 2, right, rowY + Math.min(19, rowHeight - 1), 0x66705563);
            }
            graphics.drawString(this.font, labels[i], left + 2, rowY + 3, 0xFF75B8FF, false);
            ItemStack stack = equipment[i];
            if (!stack.isEmpty()) {
                int itemX = left + Math.min(72, Math.max(42, (right - left) / 5));
                graphics.renderItem(stack, itemX, rowY);
                graphics.drawString(this.font, stack.getHoverName(), itemX + 20, rowY + 4,
                        0xFFFFFFFF, false);
            }
        }
    }

    private void drawWeaponInventory(GuiGraphics graphics, int mouseX, int mouseY,
                                     List<MenuEntry> entries, WeaponGeometry geometry) {
        int firstIndex = geometry.firstRow * 2;
        int lastIndex = Math.min(entries.size(), firstIndex + geometry.visibleRows * 2);
        for (int index = firstIndex; index < lastIndex; index++) {
            MenuEntry entry = entries.get(index);
            int column = index & 1;
            int row = index / 2 - geometry.firstRow;
            int cellX = column * geometry.columnWidth;
            int rowY = geometry.listY + 10 + row * geometry.rowHeight;
            int left = cellX + 8;
            int right = cellX + geometry.columnWidth - 8;
            boolean hovered = mouseX >= left && mouseX < right
                    && mouseY >= rowY - 2 && mouseY < rowY + 18;
            if (index == this.selection || hovered) {
                graphics.fill(left, rowY - 2, right, rowY + 18,
                        index == this.selection ? 0x88705563 : 0x55444444);
            }
            if (entry.item != null && !entry.item.isEmpty()) {
                graphics.renderItem(entry.item, left + 2, rowY);
            }
            int textX = left + 22;
            int textY = rowY + 4;
            int amountWidth = this.font.width(entry.amount);
            int labelEnd = right - amountWidth - 12;
            graphics.enableScissor(textX, rowY, Math.max(textX + 1, labelEnd), rowY + 17);
            graphics.drawString(this.font, entry.label, textX, textY,
                    entry.enabled ? 0xFFFFFFFF : 0xFF777777, false);
            graphics.disableScissor();
            if (!entry.amount.isEmpty()) {
                graphics.drawString(this.font, entry.amount, right - amountWidth - 4,
                        textY, 0xFFFFFFFF, false);
            }
        }
    }

    private ItemStack firstCurio(String slot) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack[] result = {ItemStack.EMPTY};
        CuriosApi.getCuriosInventory(this.minecraft.player).ifPresent(handler ->
                handler.getStacksHandler(slot).ifPresent(stacks -> {
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        ItemStack stack = stacks.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            result[0] = stack;
                            break;
                        }
                    }
                }));
        return result[0];
    }

    private static String formatStat(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format("%.1f", value);
    }

    private void drawSelectedDescription(GuiGraphics graphics, List<MenuEntry> entries, GridGeometry geometry) {
        if (entries.isEmpty() || this.selection < 0 || this.selection >= entries.size()) {
            return;
        }
        MenuEntry entry = entries.get(this.selection);
        List<Component> description = entry.description.isEmpty()
                ? List.of(entry.label)
                : entry.description;
        int y = 11;
        int bottom = geometry.descriptionHeight - 9;
        for (Component line : description) {
            for (FormattedCharSequence wrapped : this.font.split(line, Math.max(40, this.width - 24))) {
                if (y + this.font.lineHeight > bottom) {
                    return;
                }
                graphics.drawString(this.font, wrapped, 12, y, 0xFFFFFFFF, false);
                y += this.font.lineHeight + 2;
            }
        }
    }

    private int firstVisible(int total, int visible) {
        if (total <= visible) {
            return 0;
        }
        return Math.max(0, Math.min(this.selection - visible / 2, total - visible));
    }

    private MenuGeometry menuGeometry(int totalEntries) {
        int visibleRows = Math.min(8, Math.max(1, totalEntries));
        int width = this.view == View.COMMAND || this.view == View.TARGETS
                ? 142 : Math.min(270, Math.max(180, this.width / 3));
        int height = visibleRows * 18 + 20;
        int gap = Math.min(64, Math.max(28, this.height / 8));
        int y = Math.max(4, this.height - statusHeight() - gap - height);
        return new MenuGeometry(width, height, y, visibleRows, firstVisible(totalEntries, visibleRows));
    }

    private GridGeometry gridGeometry(int totalEntries) {
        int descriptionHeight = Math.min(74, Math.max(54, this.height / 7));
        int listY = descriptionHeight + 3;
        int listHeight = Math.max(48, this.height - statusHeight() - listY);
        int rowHeight = 22;
        int visibleRows = Math.max(1, (listHeight - 18) / rowHeight);
        int totalRows = Math.max(1, (totalEntries + 1) / 2);
        int maxScroll = Math.max(0, totalRows - visibleRows);
        this.menuScrollRow = Math.max(0, Math.min(this.menuScrollRow, maxScroll));
        return new GridGeometry(descriptionHeight, listY, listHeight, this.width / 2,
                rowHeight, visibleRows, this.menuScrollRow, maxScroll);
    }

    private WeaponGeometry weaponGeometry(int totalEntries) {
        int descriptionHeight = Math.min(76, Math.max(56, this.height * 3 / 20));
        int bodyY = descriptionHeight + 3;
        int bodyHeight = Math.min(190, Math.max(132, this.height * 2 / 5));
        int minimumListHeight = 92;
        if (bodyY + bodyHeight + 3 + minimumListHeight > this.height) {
            bodyHeight = Math.max(112, this.height - bodyY - 3 - minimumListHeight);
        }
        int statsWidth = Math.min(220, Math.max(170, this.width / 3));
        int equipmentX = statsWidth + 3;
        int listY = bodyY + bodyHeight + 3;
        int listHeight = Math.max(minimumListHeight, this.height - listY);
        int rowHeight = 21;
        int visibleRows = Math.max(1, (listHeight - 18) / rowHeight);
        int totalRows = Math.max(1, (totalEntries + 1) / 2);
        int maxScroll = Math.max(0, totalRows - visibleRows);
        this.menuScrollRow = Math.max(0, Math.min(this.menuScrollRow, maxScroll));
        return new WeaponGeometry(descriptionHeight, bodyY, bodyHeight, statsWidth, equipmentX,
                listY, listHeight, this.width / 2, rowHeight, visibleRows,
                this.menuScrollRow, maxScroll);
    }

    private void ensureGridSelectionVisible(int totalEntries) {
        if (!isGridMenu() || totalEntries <= 0) {
            return;
        }
        int visibleRows;
        int maxScroll;
        if (this.view == View.WEAPONS) {
            WeaponGeometry geometry = weaponGeometry(totalEntries);
            visibleRows = geometry.visibleRows;
            maxScroll = geometry.maxScroll;
        } else {
            GridGeometry geometry = gridGeometry(totalEntries);
            visibleRows = geometry.visibleRows;
            maxScroll = geometry.maxScroll;
        }
        int row = this.selection / 2;
        if (row < this.menuScrollRow) {
            this.menuScrollRow = row;
        } else if (row >= this.menuScrollRow + visibleRows) {
            this.menuScrollRow = row - visibleRows + 1;
        }
        this.menuScrollRow = Math.max(0, Math.min(this.menuScrollRow, maxScroll));
    }

    private List<MenuEntry> currentEntries() {
        return switch (this.view) {
            case COMMAND, TARGETS -> {
                List<MenuEntry> entries = new ArrayList<>();
                for (Component option : MAIN_OPTIONS) {
                    entries.add(MenuEntry.simple(option, -1, true));
                }
                yield entries;
            }
            case SKILLS -> skillEntries();
            case ITEMS -> inventoryEntries(false);
            case WEAPONS -> inventoryEntries(true);
            default -> List.of();
        };
    }

    private List<MenuEntry> skillEntries() {
        List<MenuEntry> weaponSkills = new ArrayList<>();
        List<MenuEntry> learnedSkills = new ArrayList<>();
        if (this.minecraft == null || this.minecraft.player == null) {
            return learnedSkills;
        }
        int registryIndex = 0;
        for (AbstractSkill skill : SkillRegistry.SKILLS.values()) {
            if (skill.isUsableInTurnBattle() && skill.isUnlockedForGUI(this.minecraft.player)) {
                int manaCost = Math.round(skill.getManaCost());
                int cooldown = this.skillCooldowns.getOrDefault(skill.getSkillId(), 0);
                MenuEntry entry = new MenuEntry(
                        Component.translatable(skill.getTranslationKey()),
                        registryIndex,
                        cooldown <= 0,
                        resolveTurnSkillIcon(skill),
                        ItemStack.EMPTY,
                        cooldown > 0 ? "CD" + cooldown : manaCost > 0 ? String.valueOf(manaCost) : "",
                        skillDescription(skill)
                );
                if (skill instanceof WeaponSkill) {
                    weaponSkills.add(entry);
                } else {
                    learnedSkills.add(entry);
                }
            }
            registryIndex++;
        }
        weaponSkills.addAll(learnedSkills);
        if (weaponSkills.isEmpty()) {
            weaponSkills.add(MenuEntry.simple(Component.literal("没有可用的技能"), -1, false));
        }
        return weaponSkills;
    }

    private ResourceLocation resolveTurnSkillIcon(AbstractSkill skill) {
        return this.turnSkillIcons.computeIfAbsent(skill.getSkillId(), skillId -> {
            String name = skillId.startsWith("bs2_skill_")
                    ? skillId.substring("bs2_skill_".length()) : skillId;
            ResourceLocation originalIcon = new ResourceLocation(
                    BlackSouls.MODID, "textures/gui/skills/turn/" + name + ".png");
            if (this.minecraft != null
                    && this.minecraft.getResourceManager().getResource(originalIcon).isPresent()) {
                return originalIcon;
            }
            return skill.getIcon();
        });
    }

    private List<Component> skillDescription(AbstractSkill skill) {
        String descriptionKey = "skill.blacksouls." + skill.getSkillId() + ".description";
        if (!I18n.exists(descriptionKey)) {
            return List.of(Component.translatable(skill.getTranslationKey()));
        }
        List<Component> result = new ArrayList<>();
        for (String line : Component.translatable(descriptionKey).getString().split("\\R", -1)) {
            if (!line.isEmpty()) {
                result.add(Component.literal(line));
            }
        }
        return result;
    }

    private List<MenuEntry> inventoryEntries(boolean weapons) {
        List<MenuEntry> result = new ArrayList<>();
        if (this.minecraft == null || this.minecraft.player == null) {
            return result;
        }
        for (int slot = 0; slot < this.minecraft.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = this.minecraft.player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            boolean valid = weapons ? TurnBattleManager.isBattleWeapon(stack) : TurnBattleManager.isBattleItem(stack);
            if (valid) {
                if (weapons) {
                    result.add(new MenuEntry(
                            stack.getHoverName().copy(),
                            slot,
                            true,
                            null,
                            stack.copy(),
                            ": " + stack.getCount(),
                            itemDescription(stack)
                    ));
                } else {
                    result.add(new MenuEntry(
                            stack.getHoverName().copy(),
                            slot,
                            true,
                            null,
                            stack.copy(),
                            ": " + stack.getCount(),
                            itemDescription(stack)
                    ));
                }
            }
        }
        if (result.isEmpty()) {
            result.add(MenuEntry.simple(
                    Component.literal(weapons ? "没有可更换的武器" : "没有可用道具"), -1, false));
        }
        return result;
    }

    private List<Component> itemDescription(ItemStack stack) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return List.of();
        }
        List<Component> tooltip = stack.getTooltipLines(this.minecraft.player, TooltipFlag.Default.NORMAL);
        if (tooltip.size() <= 1) {
            return List.of(stack.getHoverName());
        }
        return tooltip.subList(1, tooltip.size()).stream()
                .filter(line -> !(line.getContents() instanceof TranslatableContents translated)
                        || !translated.getKey().startsWith("tooltip.blacksouls.item_category")
                        && !translated.getKey().equals("tooltip.blacksouls.item_price"))
                .toList();
    }

    private void drawContinueArrow(GuiGraphics graphics, int centerX, int y) {
        int offset = (int) ((System.currentTimeMillis() / 180L) % 2L);
        int top = y + offset;
        graphics.fill(centerX - 4, top, centerX + 5, top + 2, 0xFFFFFFFF);
        graphics.fill(centerX - 3, top + 2, centerX + 4, top + 4, 0xFFFFFFFF);
        graphics.fill(centerX - 1, top + 4, centerX + 2, top + 6, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!encounterTransitionComplete()) {
            return true;
        }
        if (button != 0) {
            return true;
        }
        if (this.view == View.INTRO) {
            playUiSound(BlackSouls.SWORD1_EVENT.get(), 1.0F);
            advanceIntro();
            return true;
        }
        if (this.view == View.RESULT) {
            playUiSound(BlackSouls.SWORD1_EVENT.get(), 1.0F);
            closeBattleScreen();
            return true;
        }
        if (!isMenuView() || !this.canAct) {
            return true;
        }
        if (this.view == View.TARGETS) {
            TargetListGeometry geometry = targetListGeometry(
                    this.height - statusHeight(),
                    Math.min(118, Math.max(72, this.width / 5)));
            List<Integer> targets = livingTargetIndices();
            int listTop = geometry.y + 9;
            int listBottom = listTop + geometry.visibleRows * geometry.rowHeight;
            if (mouseX >= geometry.x + 8
                    && mouseX < geometry.x + geometry.width - 8
                    && mouseY >= listTop && mouseY < listBottom) {
                int row = this.menuScrollRow
                        + (int) ((mouseY - listTop) / geometry.rowHeight);
                if (row >= 0 && row < targets.size()) {
                    int clicked = targets.get(row);
                    if (clicked != this.targetSelection) {
                        this.targetSelection = clicked;
                        playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
                    }
                    activateSelection();
                }
            }
            return true;
        }
        List<MenuEntry> entries = currentEntries();
        if (isGridMenu()) {
            if (this.view == View.WEAPONS) {
                WeaponGeometry geometry = weaponGeometry(entries.size());
                int listTop = geometry.listY + 8;
                int listBottom = listTop + geometry.visibleRows * geometry.rowHeight;
                if (mouseX >= 8 && mouseX < this.width - 8
                        && mouseY >= listTop && mouseY < listBottom) {
                    int column = mouseX < geometry.columnWidth ? 0 : 1;
                    int row = (int) ((mouseY - listTop) / geometry.rowHeight);
                    int clicked = (geometry.firstRow + row) * 2 + column;
                    if (clicked >= 0 && clicked < entries.size()) {
                        if (this.selection != clicked) {
                            this.selection = clicked;
                            playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
                        }
                        activateSelection();
                    }
                }
                return true;
            }
            GridGeometry geometry = gridGeometry(entries.size());
            int listTop = geometry.listY + 9;
            int listBottom = listTop + geometry.visibleRows * geometry.rowHeight;
            if (mouseX >= 8 && mouseX < this.width - 8
                    && mouseY >= listTop && mouseY < listBottom) {
                int column = mouseX < geometry.columnWidth ? 0 : 1;
                int row = (int) ((mouseY - listTop) / geometry.rowHeight);
                int clicked = (geometry.firstRow + row) * 2 + column;
                if (clicked >= 0 && clicked < entries.size()) {
                    if (this.selection != clicked) {
                        this.selection = clicked;
                        playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
                    }
                    activateSelection();
                }
            }
            return true;
        }
        MenuGeometry geometry = menuGeometry(entries.size());
        if (mouseX >= 8 && mouseX < geometry.width - 8
                && mouseY >= geometry.y + 9
                && mouseY < geometry.y + 9 + geometry.visibleRows * 18) {
            int clicked = geometry.first + (int) ((mouseY - geometry.y - 9) / 18);
            if (clicked >= 0 && clicked < entries.size()) {
                if (this.selection != clicked) {
                    this.selection = clicked;
                    playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
                }
                activateSelection();
            }
        }
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!encounterTransitionComplete()) {
            return;
        }
        if (!isMenuView() || !this.canAct) {
            return;
        }
        if (this.view == View.TARGETS) {
            TargetListGeometry geometry = targetListGeometry(
                    this.height - statusHeight(),
                    Math.min(118, Math.max(72, this.width / 5)));
            List<Integer> targets = livingTargetIndices();
            int listTop = geometry.y + 9;
            int listBottom = listTop + geometry.visibleRows * geometry.rowHeight;
            if (mouseX < geometry.x + 8
                    || mouseX >= geometry.x + geometry.width - 8
                    || mouseY < listTop || mouseY >= listBottom) {
                return;
            }
            int row = this.menuScrollRow
                    + (int) ((mouseY - listTop) / geometry.rowHeight);
            if (row >= 0 && row < targets.size()) {
                int hovered = targets.get(row);
                if (hovered != this.targetSelection) {
                    this.targetSelection = hovered;
                    playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
                }
            }
            return;
        }
        List<MenuEntry> entries = currentEntries();
        if (isGridMenu()) {
            if (this.view == View.WEAPONS) {
                WeaponGeometry geometry = weaponGeometry(entries.size());
                int listTop = geometry.listY + 8;
                int listBottom = listTop + geometry.visibleRows * geometry.rowHeight;
                if (mouseX < 8 || mouseX >= this.width - 8
                        || mouseY < listTop || mouseY >= listBottom) {
                    return;
                }
                int column = mouseX < geometry.columnWidth ? 0 : 1;
                int row = (int) ((mouseY - listTop) / geometry.rowHeight);
                int hovered = (geometry.firstRow + row) * 2 + column;
                if (hovered >= 0 && hovered < entries.size() && hovered != this.selection) {
                    this.selection = hovered;
                    playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
                }
                return;
            }
            GridGeometry geometry = gridGeometry(entries.size());
            int listTop = geometry.listY + 9;
            int listBottom = listTop + geometry.visibleRows * geometry.rowHeight;
            if (mouseX < 8 || mouseX >= this.width - 8
                    || mouseY < listTop || mouseY >= listBottom) {
                return;
            }
            int column = mouseX < geometry.columnWidth ? 0 : 1;
            int row = (int) ((mouseY - listTop) / geometry.rowHeight);
            int hovered = (geometry.firstRow + row) * 2 + column;
            if (hovered >= 0 && hovered < entries.size() && hovered != this.selection) {
                this.selection = hovered;
                playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
            }
            return;
        }
        MenuGeometry geometry = menuGeometry(entries.size());
        if (mouseX < 8 || mouseX >= geometry.width - 8
                || mouseY < geometry.y + 9
                || mouseY >= geometry.y + 9 + geometry.visibleRows * 18) {
            return;
        }
        int hovered = geometry.first + (int) ((mouseY - geometry.y - 9) / 18);
        if (hovered >= 0 && hovered < entries.size() && hovered != this.selection) {
            this.selection = hovered;
            playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!encounterTransitionComplete()) {
            return true;
        }
        if (this.view == View.INTRO) {
            if (isConfirm(keyCode)) {
                playUiSound(BlackSouls.SWORD1_EVENT.get(), 1.0F);
                advanceIntro();
            }
            return true;
        }
        if (this.view == View.RESULT) {
            if (isConfirm(keyCode) || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                playUiSound(keyCode == GLFW.GLFW_KEY_ESCAPE
                        ? BlackSouls.SWORD3_EVENT.get() : BlackSouls.SWORD1_EVENT.get(), 1.0F);
                closeBattleScreen();
            }
            return true;
        }
        if (this.view == View.TARGETS) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                playUiSound(BlackSouls.SWORD3_EVENT.get(), 1.0F);
                this.view = this.targetReturnView;
                if (this.targetReturnView == View.SKILLS) {
                    this.selection = this.rememberedSkillSelection;
                    this.menuScrollRow = this.rememberedSkillScrollRow;
                    ensureGridSelectionVisible(currentEntries().size());
                } else {
                    this.menuScrollRow = 0;
                }
            } else if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_W
                    || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
                moveTargetSelection(-1);
            } else if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_S
                    || keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
                moveTargetSelection(1);
            } else if (isConfirm(keyCode)) {
                activateSelection();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.view != View.COMMAND && isMenuView()) {
            playUiSound(BlackSouls.SWORD3_EVENT.get(), 1.0F);
            this.view = View.COMMAND;
            this.selection = 0;
            return true;
        }
        if (!isMenuView() || !this.canAct) {
            return true;
        }
        int count = Math.max(1, currentEntries().size());
        if (isGridMenu()) {
            if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) {
                if ((this.selection & 1) == 1) {
                    this.selection--;
                    playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
                }
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_D) {
                if ((this.selection & 1) == 0 && this.selection + 1 < count) {
                    this.selection++;
                    playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
                }
            } else if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_W) {
                int next = this.selection - 2;
                if (next >= 0) {
                    this.selection = next;
                    playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
                }
            } else if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_S) {
                int next = this.selection + 2;
                if (next < count) {
                    this.selection = next;
                    playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
                }
            } else if (isConfirm(keyCode)) {
                activateSelection();
            }
            ensureGridSelectionVisible(count);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_W) {
            this.selection = Math.floorMod(this.selection - 1, count);
            playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
        } else if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_S) {
            this.selection = Math.floorMod(this.selection + 1, count);
            playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
        } else if (isConfirm(keyCode)) {
            activateSelection();
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!encounterTransitionComplete() || !this.canAct || delta == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        if (this.view == View.TARGETS) {
            moveTargetSelection(delta > 0.0D ? -1 : 1);
            return true;
        }
        if (!isGridMenu()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        int count = currentEntries().size();
        if (count <= 1) {
            return true;
        }
        int next = this.selection + (delta > 0.0D ? -2 : 2);
        next = Math.max(0, Math.min(count - 1, next));
        if (next != this.selection) {
            this.selection = next;
            ensureGridSelectionVisible(count);
            playUiSound(BlackSouls.CURSOR1_EVENT.get(), 1.0F);
        }
        return true;
    }

    private boolean isConfirm(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
    }

    private boolean isMenuView() {
        return this.view == View.COMMAND || this.view == View.SKILLS
                || this.view == View.ITEMS || this.view == View.WEAPONS
                || this.view == View.TARGETS;
    }

    private boolean isGridMenu() {
        return this.view == View.SKILLS || this.view == View.ITEMS || this.view == View.WEAPONS;
    }

    private void activateSelection() {
        if (this.view == View.TARGETS) {
            if (this.pendingTargetAction != null
                    && this.targetSelection >= 0
                    && this.targetSelection < this.enemies.size()
                    && isLivingTarget(this.targetSelection)) {
                playUiSound(BlackSouls.SWORD1_EVENT.get(), 1.0F);
                send(this.pendingTargetAction, this.pendingTargetValue,
                        this.targetSelection);
            }
            return;
        }
        List<MenuEntry> entries = currentEntries();
        if (this.selection < 0 || this.selection >= entries.size() || !entries.get(this.selection).enabled) {
            return;
        }
        playUiSound(BlackSouls.SWORD1_EVENT.get(), 1.0F);
        if (this.view == View.SKILLS) {
            int skillIndex = entries.get(this.selection).value;
            this.rememberedSkillSelection = this.selection;
            this.rememberedSkillScrollRow = this.menuScrollRow;
            this.rememberedSkillValue = skillIndex;
            AbstractSkill skill = SkillRegistry.SKILLS.values().stream()
                    .skip(Math.max(0, skillIndex)).findFirst().orElse(null);
            if (TurnBattleManager.skillRequiresTarget(skill)) {
                openTargetSelection(ServerboundTurnBattleActionPacket.Action.SKILL,
                        skillIndex, View.SKILLS);
            } else {
                send(ServerboundTurnBattleActionPacket.Action.SKILL, skillIndex);
            }
            return;
        }
        if (this.view == View.ITEMS) {
            send(ServerboundTurnBattleActionPacket.Action.ITEM, entries.get(this.selection).value);
            return;
        }
        if (this.view == View.WEAPONS) {
            send(ServerboundTurnBattleActionPacket.Action.WEAPON_CHANGE, entries.get(this.selection).value);
            return;
        }
        switch (this.selection) {
            case 0 -> openTargetSelection(
                    ServerboundTurnBattleActionPacket.Action.ATTACK, 0, View.COMMAND);
            case 1 -> openMenu(View.SKILLS);
            case 2 -> send(ServerboundTurnBattleActionPacket.Action.GUARD, 0);
            case 3 -> openMenu(View.ITEMS);
            case 4 -> send(ServerboundTurnBattleActionPacket.Action.ESCAPE, 0);
            case 5 -> openMenu(View.WEAPONS);
            default -> {
            }
        }
    }

    private void openMenu(View menu) {
        this.view = menu;
        if (menu == View.SKILLS) {
            List<MenuEntry> entries = skillEntries();
            int rememberedIndex = -1;
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).value == this.rememberedSkillValue) {
                    rememberedIndex = i;
                    break;
                }
            }
            this.selection = rememberedIndex >= 0 ? rememberedIndex
                    : Math.max(0, Math.min(entries.size() - 1, this.rememberedSkillSelection));
            this.menuScrollRow = this.rememberedSkillScrollRow;
            ensureGridSelectionVisible(entries.size());
        } else {
            this.selection = 0;
            this.menuScrollRow = 0;
        }
    }

    private void openTargetSelection(ServerboundTurnBattleActionPacket.Action action,
                                     int value, View returnView) {
        List<Integer> targets = livingTargetIndices();
        if (targets.isEmpty()) {
            return;
        }
        this.pendingTargetAction = action;
        this.pendingTargetValue = value;
        this.targetReturnView = returnView;
        if (!targets.contains(this.targetSelection)) {
            this.targetSelection = targets.get(0);
        }
        this.view = View.TARGETS;
        this.menuScrollRow = 0;
    }

    private void send(ServerboundTurnBattleActionPacket.Action action, int value) {
        send(action, value, firstLivingEnemyIndex());
    }

    private void send(ServerboundTurnBattleActionPacket.Action action, int value, int target) {
        this.pendingPlayerVfx = TurnBattleVfxResolver.Cue.NONE;
        this.pendingPlayerTargetsAll = false;
        this.pendingPlayerVfxStartAge = 2;
        this.pendingCounterSound = false;
        if (this.minecraft != null && this.minecraft.player != null) {
            if (action == ServerboundTurnBattleActionPacket.Action.ATTACK) {
                this.pendingPlayerVfx = TurnBattleVfxResolver.resolveWeapon(
                        this.minecraft.player, this.minecraft.player.getMainHandItem());
            } else if (action == ServerboundTurnBattleActionPacket.Action.SKILL) {
                AbstractSkill skill = SkillRegistry.SKILLS.values().stream()
                        .skip(Math.max(0, value))
                        .findFirst()
                        .orElse(null);
                this.pendingPlayerVfx = TurnBattleVfxResolver.resolveSkill(skill);
                this.pendingPlayerTargetsAll = TurnBattleManager.skillTargetsAll(skill);
            }
        }
        this.lastTargetIndex = Math.max(0, Math.min(
                Math.max(0, this.enemies.size() - 1), target));
        this.canAct = false;
        this.view = View.MESSAGE;
        this.message = Component.literal("……");
        NetworkHandler.sendToServer(new ServerboundTurnBattleActionPacket(
                action, value, target));
    }

    private void initializeDisplayedStats() {
        if (this.minecraft == null || this.minecraft.player == null || this.displayPlayerHealth >= 0.0F) {
            return;
        }
        BSPlayerStats stats = this.minecraft.player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        this.displayPlayerHealth = this.minecraft.player.getHealth();
        this.displayPlayerMaxHealth = this.minecraft.player.getMaxHealth();
        this.displayMp = stats == null ? 0.0D : stats.mp;
        this.displayMaxMp = stats == null ? 1.0D : Math.max(1.0D, stats.maxMp);
        this.displayAp = SkillUtils.getCurrentActionPoints(this.minecraft.player);
        this.displayMaxAp = Math.max(0.0001D, SkillUtils.getMaxActionPoints(this.minecraft.player));
    }

    private void initializeEnemyActionGauge() {
        if (this.enemyActionInitialized) {
            return;
        }
        this.enemyActionInitialized = true;
        float startRate = 0.30F;
        if (this.minecraft != null && this.minecraft.player != null) {
            startRate += this.minecraft.player.getRandom().nextFloat() * 0.40F;
        }
        this.enemyActionPoints = ENEMY_MAX_AP * startRate;
        this.enemyActionWaitTicks = ENEMY_START_WAIT_TICKS;
    }

    private void updateEnemyActionGauge() {
        captureEnemyVisual();
        initializeEnemyActionGauge();
        if (this.outcome != ClientboundTurnBattlePacket.Outcome.NONE) {
            return;
        }
        if (this.enemyActionWaitTicks > 0) {
            this.enemyActionWaitTicks--;
            return;
        }
        this.enemyActionPoints = Math.min(
                ENEMY_MAX_AP, this.enemyActionPoints + enemyActionPointsPerTick());
    }

    private void captureEnemyVisual() {
        if (this.enemyProfileId > 0) {
            BSOriginalEnemyData.Entry profile = BSOriginalEnemyData.get(this.enemyProfileId);
            this.enemyTexture = profile.texture();
            this.enemyTextureWidth = Math.max(1, profile.textureWidth());
            this.enemyTextureHeight = Math.max(1, profile.textureHeight());
            this.enemyAgility = profile.agility();
            return;
        }
        EntityTurnBattleMonster enemy = getBattleEnemy();
        if (enemy != null) {
            this.enemyTexture = enemy.getTurnBattleTexture();
            this.enemyTextureWidth = Math.max(1, enemy.getTurnBattleTextureWidth());
            this.enemyTextureHeight = Math.max(1, enemy.getTurnBattleTextureHeight());
            this.enemyAgility = enemy.getTurnBattleAgility();
        }
    }

    private EntityTurnBattleMonster getBattleEnemy() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        return this.minecraft.level.getEntity(this.entityId) instanceof EntityTurnBattleMonster enemy
                ? enemy : null;
    }

    private float enemyActionPointsPerTick() {
        return (float) Math.max(15.0D, (this.enemyAgility + 10.0D) * 3.0D);
    }

    private void updateDisplayedStats() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        initializeDisplayedStats();
        BSPlayerStats stats = this.minecraft.player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
        for (EnemyVisual enemy : this.enemies) {
            if (enemy.health > 0.0F) {
                enemy.collapseTicks = 0;
                enemy.collapseSoundStep = -1;
                enemy.displayAlpha = approach(enemy.displayAlpha, 1.0F, 0.22F);
            } else if (isBossCollapse(enemy)) {
                if (enemy.collapseTicks == 0) {
                    playUiSound(BlackSouls.TURN_BOSS_COLLAPSE_START_EVENT.get(), 0.5F, 0.8F);
                    enemy.collapseSoundStep = 0;
                }
                enemy.collapseTicks++;
                int soundStep = enemy.collapseTicks / 7;
                if (soundStep > enemy.collapseSoundStep && enemy.collapseTicks > 1
                        && enemy.collapseTicks < bossCollapseDuration(enemy)) {
                    enemy.collapseSoundStep = soundStep;
                    playUiSound(BlackSouls.TURN_BOSS_COLLAPSE_LOOP_EVENT.get(), 1.0F, 0.8F);
                }
                enemy.displayAlpha = Math.max(0.0F, 1.0F - bossCollapseProgress(enemy));
            } else {
                enemy.displayAlpha = approach(enemy.displayAlpha, 0.0F, 0.22F);
            }
            if (enemy.profileId < enemy.targetProfileId) {
                if (enemy.profileMorphTicks > 0) {
                    enemy.profileMorphTicks--;
                } else {
                    enemy.profileId++;
                    enemy.profileMorphTicks = enemy.profileId < enemy.targetProfileId ? 6 : 0;
                }
            }
        }
        if (this.holdEnemyGaugeTicks > 0) {
            this.holdEnemyGaugeTicks--;
        } else {
            for (EnemyVisual enemy : this.enemies) {
                enemy.displayHealth = approach(
                        enemy.displayHealth, enemy.health, 0.30F);
            }
            syncPrimaryEnemy();
        }
        ClientPartyState.getMembers().stream()
                .filter(member -> member.id().equals(this.minecraft.player.getUUID()))
                .findFirst()
                .ifPresent(member -> this.playerDown = member.downed());
        if (this.holdPlayerGaugeTicks > 0) {
            this.holdPlayerGaugeTicks--;
        } else {
            this.displayPlayerHealth = approach(this.displayPlayerHealth,
                    this.playerDown ? 0.0F : this.minecraft.player.getHealth(), 0.30F);
        }
        this.displayPlayerMaxHealth = approach(this.displayPlayerMaxHealth, this.minecraft.player.getMaxHealth(), 0.18F);
        this.displayMp = approach(this.displayMp, stats == null ? 0.0D : stats.mp, 0.14D);
        this.displayMaxMp = approach(this.displayMaxMp, stats == null ? 1.0D : Math.max(1.0D, stats.maxMp), 0.18D);
        this.displayAp = approach(this.displayAp, SkillUtils.getCurrentActionPoints(this.minecraft.player), 0.14D);
        this.displayMaxAp = approach(this.displayMaxAp,
                Math.max(0.0001D, SkillUtils.getMaxActionPoints(this.minecraft.player)), 0.18D);
    }

    private static float approach(float current, float target, float factor) {
        float next = current + (target - current) * factor;
        return Math.abs(next - target) < 0.05F ? target : next;
    }

    private static double approach(double current, double target, double factor) {
        double next = current + (target - current) * factor;
        return Math.abs(next - target) < 0.01D ? target : next;
    }

    private static int extractDamage(String text, String expression) {
        Matcher matcher = Pattern.compile(expression).matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private int resolveEnemyEvasionSound(String report) {
        if (this.enemies.isEmpty()) {
            return 1;
        }
        int index = Math.max(0, Math.min(this.enemies.size() - 1, this.actingEnemyIndex));
        BSOriginalEnemyData.Entry profile = BSOriginalEnemyData.get(this.enemies.get(index).profileId);
        BSOriginalEnemyData.Action fallback = null;
        for (BSOriginalEnemyData.Action action : profile.actions()) {
            if (action.animationId() != this.enemyAnimationId) {
                continue;
            }
            if (fallback == null) {
                fallback = action;
            }
            if ((!action.name().isBlank() && report.contains(action.name()))
                    || (!action.text().isBlank() && report.contains(action.text()))) {
                return action.hitType() == 2 ? 2 : 1;
            }
        }
        return fallback != null && fallback.hitType() == 2 ? 2 : 1;
    }

    private TurnBattleVfxResolver.Target resolveEnemyAnimationTarget(String report) {
        if (this.enemies.isEmpty()) {
            return TurnBattleVfxResolver.Target.PLAYER;
        }
        int index = Math.max(0, Math.min(this.enemies.size() - 1, this.actingEnemyIndex));
        BSOriginalEnemyData.Entry profile = BSOriginalEnemyData.get(this.enemies.get(index).profileId);
        for (BSOriginalEnemyData.Action action : profile.actions()) {
            if (action.animationId() == this.enemyAnimationId
                    && ((!action.name().isBlank() && report.contains(action.name()))
                    || (!action.text().isBlank() && report.contains(action.text())))) {
                return action.scope() == 11
                        ? TurnBattleVfxResolver.Target.ENEMY
                        : TurnBattleVfxResolver.Target.PLAYER;
            }
        }
        return TurnBattleVfxResolver.Target.PLAYER;
    }

    private boolean isBossCollapse(EnemyVisual enemy) {
        return enemy.profileId > 0
                && BSOriginalEnemyData.get(enemy.profileId).collapseType() == 1
                && !BSOriginalEnemyPhaseData.hasNext(enemy.profileId);
    }

    private boolean hasActiveBossCollapse() {
        return this.enemies.stream().anyMatch(enemy -> enemy.health <= 0.0F
                && enemy.displayAlpha > 0.01F && isBossCollapse(enemy));
    }

    private int bossCollapseDuration(EnemyVisual enemy) {
        if (enemy.profileId <= 0) {
            return 80;
        }
        return Math.max(80, Math.min(220,
                BSOriginalEnemyData.get(enemy.profileId).textureHeight() / 3));
    }

    private float bossCollapseProgress(EnemyVisual enemy) {
        return Math.min(1.0F, enemy.collapseTicks
                / (float) Math.max(1, bossCollapseDuration(enemy)));
    }

    private void startBattleMusic() {
        if (this.battleMusic == null && this.minecraft != null
                && this.outcome == ClientboundTurnBattlePacket.Outcome.NONE) {
            ResourceLocation event = this.battleBgm;
            if (event == null) {
                event = ForgeRegistries.SOUND_EVENTS.getKey(
                        BlackSouls.TURN_BATTLE_BGM_EVENT.get());
            }
            if (event == null) {
                return;
            }
            this.battleMusic = new SimpleSoundInstance(
                    event, SoundSource.MUSIC,
                    this.battleBgmVolume, this.battleBgmPitch,
                    RandomSource.create(), true, 0,
                    SoundInstance.Attenuation.NONE,
                    0.0D, 0.0D, 0.0D, true);
            TurnBattleAudioGate.play(this.battleMusic);
        }
    }

    private void playUiSound(SoundEvent sound, float pitch) {
        playUiSound(sound, pitch, 1.0F);
    }

    private void playUiSound(SoundEvent sound, float pitch, float volume) {
        if (this.minecraft != null) {
            TurnBattleAudioGate.play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }

    private void closeBattleScreen() {
        stopBattleMusic();
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    private void stopBattleMusic() {
        if (this.minecraft != null && this.battleMusic != null) {
            this.minecraft.getSoundManager().stop(this.battleMusic);
            this.battleMusic = null;
        }
    }

    @Override
    public void removed() {
        stopBattleMusic();
        TurnBattleAudioGate.leave(this);
        super.removed();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum View {
        INTRO,
        COMMAND,
        SKILLS,
        ITEMS,
        WEAPONS,
        TARGETS,
        MESSAGE,
        RESULT
    }

    private record MenuEntry(Component label, int value, boolean enabled, ResourceLocation icon,
                             ItemStack item, String amount, List<Component> description) {
        private static MenuEntry simple(Component label, int value, boolean enabled) {
            return new MenuEntry(label, value, enabled, null, ItemStack.EMPTY, "", List.of());
        }
    }

    private record MenuGeometry(int width, int height, int y, int visibleRows, int first) {
    }

    private record GridGeometry(int descriptionHeight, int listY, int listHeight, int columnWidth,
                                int rowHeight, int visibleRows, int firstRow, int maxScroll) {
    }

    private record WeaponGeometry(int descriptionHeight, int bodyY, int bodyHeight, int statsWidth,
                                  int equipmentX, int listY, int listHeight, int columnWidth,
                                  int rowHeight, int visibleRows, int firstRow, int maxScroll) {
    }

    private record TargetListGeometry(int x, int y, int width, int height,
                                       int rowHeight, int visibleRows) {
    }

    private record PendingCurtainPhase(int battleProfileId,
                                       List<ClientboundTurnBattlePacket.EnemySnapshot> enemies,
                                       int actingEnemyIndex, int enemyAnimationId) {
    }

    private record EnemyGeometry(int cellX, int cellWidth, int x, int y,
                                 int width, int height, ResourceLocation texture,
                                 int textureWidth, int textureHeight) {
    }

    private static final class EnemyVisual {
        private final int entityId;
        private final Component name;
        private float health;
        private float finalHealth;
        private final float maxHealth;
        private int profileId;
        private int targetProfileId;
        private int profileMorphTicks;
        private final List<Integer> states;
        private float displayHealth;
        private float displayAlpha;
        private int collapseTicks;
        private int collapseSoundStep = -1;

        private EnemyVisual(int entityId, Component name, float health,
                            float maxHealth, int profileId, List<Integer> states,
                            float displayHealth, float displayAlpha) {
            this.entityId = entityId;
            this.name = name;
            this.health = health;
            this.finalHealth = health;
            this.maxHealth = maxHealth;
            this.profileId = profileId;
            this.targetProfileId = profileId;
            this.states = List.copyOf(states);
            this.displayHealth = displayHealth;
            this.displayAlpha = displayAlpha;
        }
    }

    private record ScheduledDamageHit(int targetIndex, int damage, boolean critical,
                                      int triggerAge, boolean finalHit, int wave) {
    }

    private record ScheduledIncomingHit(ClientboundTurnBattlePacket.IncomingHit hit,
                                        int triggerAge, int wave) {
    }

    private record ScheduledRevival(int triggerAge, int health) {
    }

    private record PlayerDamagePopup(int damage, boolean critical, long startedAt, int wave) {
    }

    private record DamagePopup(int targetIndex, int damage, boolean critical,
                               long startedAt, int wave) {
    }

    private static final class ActiveBattleVfx {
        private final int animationId;
        private final TurnBattleVfxResolver.Target target;
        private final int targetIndex;
        private final long startedAt;
        private final boolean playSounds;
        private int nextSoundIndex;

        private ActiveBattleVfx(int animationId, TurnBattleVfxResolver.Target target,
                                int targetIndex,
                                long startedAt, boolean playSounds) {
            this.animationId = animationId;
            this.target = target;
            this.targetIndex = targetIndex;
            this.startedAt = startedAt;
            this.playSounds = playSounds;
        }
    }
}

package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketPlayAnim;
import com.BlackSouls.BlackSoulsMod.util.BSMobStatManager;
import com.BlackSouls.BlackSoulsMod.util.DifficultyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("removal")
public class EntityHellPrince extends Monster implements InstantDeathImmuneEntity {

    private boolean hasSpokenIntro = false;
    private boolean hasCastOpeningCombo = false;
    private boolean openingComboStarted = false;

    private boolean castingJabaraSweep = false;
    private int jabaraSweepTick = 0;
    private int jabaraSweepHitIndex = 0;

    private static final String TAG_OPENING_COMBO_DAMAGE = "bs2_hell_prince_opening_combo_damage";

    private int introTick = 0;
    private int openingComboTick = 0;

    private int bgmLoopTimer = 0;
    private static final int BGM_DURATION_TICKS = 3280;

    public EntityHellPrince(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 50000;
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        super.remove(reason);

        if (!this.level().isClientSide()) {
            for (Player player : this.level().players()) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.connection.send(new ClientboundStopSoundPacket(
                            new ResourceLocation(BlackSouls.MODID, "hell_prince_bgm"),
                            SoundSource.HOSTILE
                    ));
                }
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.LUCK, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    public static boolean isOpeningComboDamage(DamageSource source) {
        return source.getEntity() instanceof EntityHellPrince prince
                && prince.getPersistentData().getBoolean(TAG_OPENING_COMBO_DAMAGE);
    }

    private void dealOpeningComboDamage(LivingEntity target, float damage) {
        this.getPersistentData().putBoolean(TAG_OPENING_COMBO_DAMAGE, true);

        try {
            target.invulnerableTime = 0;

            
            
            target.hurt(this.damageSources().indirectMagic(this, this), damage);
        } finally {
            this.getPersistentData().remove(TAG_OPENING_COMBO_DAMAGE);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.45D, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 15.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HasSpokenIntro", this.hasSpokenIntro);
        tag.putBoolean("HasCastOpeningCombo", this.hasCastOpeningCombo);
        tag.putBoolean("OpeningComboStarted", this.openingComboStarted);
        tag.putInt("OpeningComboTick", this.openingComboTick);
        tag.putInt("BgmLoopTimer", this.bgmLoopTimer);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.hasSpokenIntro = tag.getBoolean("HasSpokenIntro");
        this.hasCastOpeningCombo = tag.getBoolean("HasCastOpeningCombo");
        this.openingComboStarted = tag.getBoolean("OpeningComboStarted");
        this.openingComboTick = tag.getInt("OpeningComboTick");
        this.bgmLoopTimer = tag.getInt("BgmLoopTimer");
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            antiCheeseTick();
            handleIntroAndBgm();
            handleOpeningCombo();
            handleJabaraSweepTick();
        }
    }

    private void handleIntroAndBgm() {
        if (!this.hasSpokenIntro) {
            this.introTick++;

            if (this.introTick == 1) {
                broadcastMessage(Component.translatable("chat.blacksouls.hell_prince.intro1"));
                this.playSound(BlackSouls.HELL_PRINCE_BGM_EVENT.get(), 20.0F, 1.0F);

            } else if (this.introTick == 70) {
                broadcastMessage(Component.translatable("chat.blacksouls.hell_prince.intro2"));
                this.hasSpokenIntro = true;

                if (!this.hasCastOpeningCombo) {
                    this.openingComboStarted = true;
                    this.openingComboTick = 0;
                }
            }
        } else {
            this.bgmLoopTimer++;

            if (this.bgmLoopTimer >= BGM_DURATION_TICKS) {
                this.playSound(BlackSouls.HELL_PRINCE_BGM_EVENT.get(), 20.0F, 1.0F);
                this.bgmLoopTimer = 0;
            }
        }
    }

    private void handleOpeningCombo() {
        if (!this.openingComboStarted || this.hasCastOpeningCombo) {
            return;
        }

        this.openingComboTick++;

        if (openingComboTick == 1) {
            castTerrifyingSurgery(); 
        }

        if (openingComboTick == 40) {
            castAcidSplash(); 
        }

        if (openingComboTick == 80) {
            LivingEntity target = getComboTarget();
            if (target != null) {
                castLegCut(target);
            }
        }

        if (openingComboTick == 120) {
            castGunBreakSingle(3.0F, 217, 20); 
        }

        if (openingComboTick == 150) {
            castGunBreakSingle(4.0F, 246, 20); 
        }

        if (openingComboTick == 180) {
            castGunBreakSingle(5.0F, 247, 50); 
        }

        if (openingComboTick == 220) {
            castGunBreakAll(10.0F, 385, 20); 
        }

        if (openingComboTick == 260) {
            castJabaraSwordSweep();
        }

        if (this.openingComboTick >= 420) {
            this.hasCastOpeningCombo = true;
            this.openingComboStarted = false;
        }

    }

    private void castTerrifyingSurgery() {
        List<Player> targets = pickPlayersAllowRepeat(2, 64.0D);

        if (targets.isEmpty()) {
            return;
        }

        this.swing(InteractionHand.MAIN_HAND, true);
        broadcastMessage(Component.translatable("message.blacksouls.hell_prince.terrifying_surgery.start"));

        for (Player target : targets) {
            dealOpeningComboDamage(target, 10000.0F);

            target.sendSystemMessage(Component.translatable(
                    "message.blacksouls.hell_prince.damage",
                    target.getName().getString(),
                    10000
            ).withStyle(ChatFormatting.WHITE));

            BlackSouls.BUFF_FEAR.ifPresent(effect ->
                    target.addEffect(new MobEffectInstance(effect, 100, 0))
            );
        }
    }

    private void castAcidSplash() {
        List<Player> targets = pickPlayersAllowRepeat(3, 64.0D);
        if (targets.isEmpty()) return;

        this.swing(InteractionHand.MAIN_HAND, true);
        broadcastMessage(Component.translatable("message.blacksouls.hell_prince.acid_splash.start"));

        float atk = getBossAttack();

        for (Player target : targets) {
            playAnim(target, 292);

            this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    BlackSouls.ACID_EVENT.get(), SoundSource.HOSTILE, 1.0F, 1.0F);

            float damage = atk * 2.0F;

            dealOpeningComboDamage(target, damage);

            target.sendSystemMessage(Component.translatable(
                    "message.blacksouls.hell_prince.damage",
                    target.getName().getString(),
                    (int) damage
            ).withStyle(ChatFormatting.WHITE));
        }
    }

    private void castLegCut(LivingEntity target) {
        this.swing(InteractionHand.MAIN_HAND, true);
        broadcastMessage(Component.translatable("message.blacksouls.hell_prince.leg_cut.start"));

        playAnim(target, 7);

        float atk = getBossAttack();
        float def = getTargetDefense(target);

        float damage = atk * 10.0F - def * 0.5F;
        if (damage < 1.0F) {
            damage = 1.0F;
        }

        damage = applyVariance(damage, 20);

        dealOpeningComboDamage(target, damage);

        BlackSouls.BUFF_SEVERED_LEG.ifPresent(effect ->
                target.addEffect(new MobEffectInstance(effect, 600, 0, false, true, true))
        );

        if (target instanceof Player player) {
            player.sendSystemMessage(Component.translatable(
                    "message.blacksouls.hell_prince.damage",
                    player.getName().getString(),
                    (int) damage
            ).withStyle(ChatFormatting.WHITE));
        }
    }

    private List<Player> pickPlayersAllowRepeat(int count, double range) {
        List<Player> players = this.level().getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(range),
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative()
        );

        List<Player> result = new ArrayList<>();

        if (players.isEmpty()) {
            return result;
        }

        Collections.shuffle(players);

        for (int i = 0; i < count; i++) {
            result.add(players.get(i % players.size()));
        }

        return result;
    }

    private LivingEntity findNearestPlayerTarget(double range) {
        List<Player> players = this.level().getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(range),
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative()
        );

        Player closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Player player : players) {
            double dist = this.distanceToSqr(player);
            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }

        return closest;
    }

    private void playAnim(LivingEntity target, int animId) {
        PacketPlayAnim animPacket = new PacketPlayAnim(
                animId,
                target.getX(),
                target.getY() + target.getBbHeight() / 2.0F,
                target.getZ()
        );
        NetworkHandler.sendToAllAround(animPacket, target);
    }

    private void broadcastMessage(Component message) {
        List<Player> players = this.level().getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(64.0D)
        );

        for (Player player : players) {
            player.sendSystemMessage(message.copy().withStyle(ChatFormatting.WHITE));
        }

        this.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.6F);
    }

    private void antiCheeseTick() {
        if (this.isPassenger()) {
            this.stopRiding();
        }

        clearNearbyVehicles();

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            double distSqr = this.distanceToSqr(target);

            if (distSqr > 9.0D) {
                this.getNavigation().moveTo(target, 1.45D);
            }

            if (this.tickCount % 10 == 0) {
                breakPathBlocks();
            }
        }

        if (this.tickCount % 20 == 0) {
            breakTrapBlocks();
        }
    }

    @Override
    public boolean startRiding(@NotNull Entity vehicle, boolean force) {
        return false;
    }

    @Override
    protected boolean canRide(@NotNull Entity vehicle) {
        return false;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        if (result && !this.level().isClientSide()) {
            Entity attacker = source.getEntity();

            if (attacker instanceof LivingEntity living) {
                this.setTarget(living);
                this.getNavigation().moveTo(living, 1.45D);
            }

            clearNearbyVehicles();
            breakTrapBlocks();
            breakPathBlocks();
        }

        return result;
    }

    private void clearNearbyVehicles() {
        this.level().getEntitiesOfClass(Boat.class, this.getBoundingBox().inflate(2.0D))
                .forEach(Entity::discard);

        this.level().getEntitiesOfClass(AbstractMinecart.class, this.getBoundingBox().inflate(2.0D))
                .forEach(Entity::discard);
    }

    private void breakTrapBlocks() {
        BlockPos center = this.blockPosition();

        breakBlocksInArea(
                center.offset(-1, 0, -1),
                center.offset(1, 3, 1)
        );
    }

    private void breakPathBlocks() {
        LivingEntity target = this.getTarget();

        if (target == null || !target.isAlive()) {
            return;
        }

        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();

        int stepX = Math.abs(dx) > 0.2D ? (dx > 0 ? 1 : -1) : 0;
        int stepZ = Math.abs(dz) > 0.2D ? (dz > 0 ? 1 : -1) : 0;

        BlockPos front = this.blockPosition().offset(stepX, 0, stepZ);

        breakBlocksInArea(
                front.offset(-1, 0, -1),
                front.offset(1, 3, 1)
        );
    }

    private void breakBlocksInArea(BlockPos from, BlockPos to) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            BlockState state = this.level().getBlockState(pos);

            if (shouldNotBreakBlock(state, pos)) {
                continue;
            }

            this.level().destroyBlock(pos, true, this);
        }
    }

    private LivingEntity getComboTarget() {
        LivingEntity target = this.getTarget();

        if (target != null && target.isAlive()) {
            return target;
        }

        return findNearestPlayerTarget(64.0D);
    }

    private void castGunBreakSingle(float multiplier, int animId, int variance) {
        List<Player> targets = pickPlayersAllowRepeat(1, 64.0D);
        if (targets.isEmpty()) return;

        Player target = targets.get(0);

        this.swing(InteractionHand.MAIN_HAND, true);

        broadcastMessage(Component.translatable("message.blacksouls.hell_prince.gun_break.start"));

        
        broadcastMessageStyled(
                Component.translatable("message.blacksouls.hell_prince.critical_hit"),
                ChatFormatting.DARK_RED
        );

        applyGunBreakHit(target, multiplier, animId, variance);
    }

    private void broadcastMessageStyled(Component message, ChatFormatting color) {
        List<Player> players = this.level().getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(64.0D)
        );

        for (Player player : players) {
            player.sendSystemMessage(message.copy().withStyle(color));
        }

        this.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.6F);
    }

    private void castGunBreakAll(float multiplier, int animId, int variance) {
        List<Player> targets = this.level().getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(64.0D),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
        );

        if (targets.isEmpty()) return;

        this.swing(InteractionHand.MAIN_HAND, true);

        broadcastMessage(Component.translatable("message.blacksouls.hell_prince.gun_break.start"));

        broadcastMessageStyled(
                Component.translatable("message.blacksouls.hell_prince.critical_hit"),
                ChatFormatting.DARK_RED
        );

        for (Player target : targets) {
            applyGunBreakHit(target, multiplier, animId, variance);
        }
    }

    private void applyGunBreakHit(LivingEntity target, float multiplier, int animId, int variance) {
        playAnim(target, animId);

        this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5F, 0.8F);

        float atk = getBossAttack();
        float def = getTargetDefense(target);

        float damage = atk * multiplier - def * 0.5F;
        if (damage < 1.0F) damage = 1.0F;
        damage = applyVariance(damage, variance);
        
        damage *= 3.0F;

        dealOpeningComboDamage(target, damage);

        if (target instanceof Player player) {
            player.sendSystemMessage(Component.translatable(
                    "message.blacksouls.hell_prince.damage",
                    player.getName().getString(),
                    (int) damage
            ).withStyle(ChatFormatting.WHITE));
        }
    }

    private void castJabaraSwordSweep() {
        this.swing(InteractionHand.MAIN_HAND, true);
        broadcastMessage(Component.translatable("message.blacksouls.hell_prince.jabara_sword.start"));

        this.castingJabaraSweep = true;
        this.jabaraSweepTick = 0;
        this.jabaraSweepHitIndex = 0;
    }

    private void handleJabaraSweepTick() {
        if (!this.castingJabaraSweep) {
            return;
        }

        this.jabaraSweepTick++;

        if (this.jabaraSweepTick % 10 != 0) {
            return;
        }

        List<Player> targets = this.level().getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(64.0D),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
        );

        if (!targets.isEmpty()) {
            for (Player target : targets) {
                playAnim(target, 550);

                BlackSouls.BUFF_FROSTBITE.ifPresent(effect ->
                        target.addEffect(new MobEffectInstance(effect, 100, 0, false, true, true))
                );

                float atk = getBossAttack();
                float def = getTargetDefense(target);

                float damage = atk * 10.0F - def * 0.5F;
                if (damage < 1.0F) {
                    damage = 1.0F;
                }

                damage = applyVariance(damage, 20);

                dealOpeningComboDamage(target, damage);

                target.sendSystemMessage(Component.translatable(
                        "message.blacksouls.hell_prince.damage",
                        target.getName().getString(),
                        (int) damage
                ).withStyle(ChatFormatting.WHITE));
            }
        }

        this.jabaraSweepHitIndex++;

        if (this.jabaraSweepHitIndex >= 14) {
            this.castingJabaraSweep = false;
            this.jabaraSweepTick = 0;
            this.jabaraSweepHitIndex = 0;
        }
    }

    private float getTargetDefense(LivingEntity target) {
        float defense;

        if (target instanceof Player player) {
            defense = player.getCapability(BSPlayerStats.CAPABILITY)
                    .map(stats -> (float) stats.defense)
                    .orElse((float) target.getArmorValue());
        } else if (BSMobStatManager.hasManagedStats(target)) {
            defense = (float) DifficultyManager.scaleManagedStat(
                    target.level(),
                    BSMobStatManager.getStats(target).defense
            );
        } else {
            defense = target.getArmorValue();
        }

        if (BlackSouls.BUFF_SEVERED_LEG.isPresent()
                && target.hasEffect(BlackSouls.BUFF_SEVERED_LEG.get())) {
            defense *= 0.01F;
        }

        return defense;
    }

    private float getBossAttack() {
        if (BSMobStatManager.hasManagedStats(this)) {
            return (float) DifficultyManager.scaleManagedStat(
                    this.level(),
                    BSMobStatManager.getStats(this).attack
            );
        }
        return (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private float applyVariance(float damage, int variancePercent) {
        if (variancePercent <= 0) {
            return damage;
        }

        float min = 1.0F - variancePercent / 100.0F;
        float max = 1.0F + variancePercent / 100.0F;
        float factor = min + this.random.nextFloat() * (max - min);

        return damage * factor;
    }

    private boolean shouldNotBreakBlock(BlockState state, BlockPos pos) {
        return state.isAir()
                || state.getDestroySpeed(this.level(), pos) < 0
                || state.is(Blocks.BEDROCK)
                || state.is(Blocks.BARRIER)
                || state.is(Blocks.COMMAND_BLOCK)
                || state.is(Blocks.CHAIN_COMMAND_BLOCK)
                || state.is(Blocks.REPEATING_COMMAND_BLOCK)
                || state.is(Blocks.STRUCTURE_BLOCK)
                || state.is(Blocks.JIGSAW);
    }
}
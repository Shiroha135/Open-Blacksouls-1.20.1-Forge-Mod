package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketOpenDialogue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class EntityCheshireCat extends PathfinderMob implements DialogueResettable {
    private static final String CURRENT_SCENE_TAG = "Blacksouls2CurrentScene";
    private static final String LEGACY_ASKED_ALICE_TAG = "BlacksoulsCheshireAskedAlice";
    private static final String STORY_STAGE_TAG = "BlacksoulsCheshireRabbitHoleStage";
    private static final int STAGE_INTRO = 0;
    private static final int STAGE_WAITING = 1;
    private static final int STAGE_GIFT = 2;
    private static final int STAGE_AFTER_GIFT = 3;
    private static final Vec3 LEGACY_RABBIT_HOLE_POSITION = new Vec3(79.5D, -25.0D, 170.5D);
    private String sceneId = "";
    private int vanishTicks;
    private boolean awaitingGiftReturn;
    private UUID waitingPlayer;
    private double waitingStartX;
    private double waitingStartZ;

    public EntityCheshireCat(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setCustomName(Component.translatable("entity.blacksouls.cheshire_cat.name"));
        setPersistenceRequired();
        setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1024.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public static void recoverLegacyRabbitHoleEntity(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(LEGACY_ASKED_ALICE_TAG) || player.tickCount % 20 != 0
                || !CheshireDialogue.isRabbitHole(data.getString(CURRENT_SCENE_TAG))
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos target = BlockPos.containing(LEGACY_RABBIT_HOLE_POSITION);
        if (!level.hasChunkAt(target)) {
            return;
        }
        boolean present = !level.getEntitiesOfClass(EntityCheshireCat.class,
                AABB.ofSize(LEGACY_RABBIT_HOLE_POSITION, 4.0D, 4.0D, 4.0D)).isEmpty();
        if (!present) {
            EntityCheshireCat cheshire = BSEntityRegistry.CHESHIRE_CAT.get().create(level);
            if (cheshire == null) {
                return;
            }
            cheshire.moveTo(LEGACY_RABBIT_HOLE_POSITION.x, LEGACY_RABBIT_HOLE_POSITION.y,
                    LEGACY_RABBIT_HOLE_POSITION.z, 0.0F, 0.0F);
            cheshire.sceneId = "map_051";
            if (!level.addFreshEntity(cheshire)) {
                return;
            }
        }
        data.remove(LEGACY_ASKED_ALICE_TAG);
        data.putInt(STORY_STAGE_TAG, STAGE_GIFT);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0.0D, onGround() ? 0.0D : getDeltaMovement().y, 0.0D);
        if (!level().isClientSide) {
            if (awaitingGiftReturn) {
                checkGiftReturn();
            } else if (vanishTicks > 0 && --vanishTicks == 0) {
                setInvisible(false);
                setSilent(false);
            }
        }
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || vanishTicks > 0) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            bindScene(serverPlayer);
            boolean rabbitHole = CheshireDialogue.isRabbitHole(sceneId);
            CheshireDialogue.Mode mode = CheshireDialogue.Mode.NONE;
            String[] dialogueKeys = CheshireDialogue.keysFor(sceneId);
            if (rabbitHole) {
                int stage = storyStage(serverPlayer);
                if (stage == STAGE_WAITING) {
                    return InteractionResult.CONSUME;
                }
                if (stage == STAGE_GIFT) {
                    mode = CheshireDialogue.Mode.GIFT;
                    dialogueKeys = CheshireDialogue.rabbitHoleGiftKeys();
                } else if (stage >= STAGE_AFTER_GIFT) {
                    mode = CheshireDialogue.Mode.AFTER_GIFT;
                    dialogueKeys = CheshireDialogue.rabbitHoleAfterGiftKeys();
                } else {
                    mode = CheshireDialogue.Mode.INTRO;
                }
            }
            level().playSound(null, blockPosition(), BlackSouls.CHESHIRE_CAT3_EVENT.get(),
                    SoundSource.NEUTRAL, 1.0F, 1.0F);
            NetworkHandler.sendToPlayer(new PacketOpenDialogue(
                    "entity.blacksouls.cheshire_cat.name",
                    "cheshire_cat",
                    dialogueKeys,
                    rabbitHole,
                    getId(),
                    -1,
                    false,
                    -1,
                    false,
                    mode
            ), serverPlayer);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void finishAliceQuestion(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(LEGACY_ASKED_ALICE_TAG);
        data.putInt(STORY_STAGE_TAG, STAGE_WAITING);
        level().playSound(null, blockPosition(), BlackSouls.CHESHIRE_CAT2_EVENT.get(),
                SoundSource.NEUTRAL, 1.0F, 1.0F);
        waitingPlayer = player.getUUID();
        waitingStartX = player.getX();
        waitingStartZ = player.getZ();
        awaitingGiftReturn = true;
        vanishTicks = 0;
        setInvisible(true);
        setSilent(true);
    }

    public boolean giveRabbitHoleGift(ServerPlayer player, int primaryGift, int secondaryGift) {
        if (!CheshireDialogue.isRabbitHole(sceneId) || storyStage(player) != STAGE_GIFT
                || primaryGift < 0 || primaryGift > 3 || secondaryGift < 0 || secondaryGift > 1) {
            return false;
        }
        ItemStack primary = switch (primaryGift) {
            case 0 -> new ItemStack(BlackSouls.RING_LIFE.get());
            case 1 -> new ItemStack(BlackSouls.MASTER_KEY.get());
            case 2 -> new ItemStack(BlackSouls.HOMEWARD_BONE_DUST.get());
            default -> new ItemStack(BlackSouls.MAIDENSFRAGRANCE.get());
        };
        ItemStack secondary = secondaryGift == 0
                ? new ItemStack(BlackSouls.INVISIBLE_PEPPER.get())
                : new ItemStack(BlackSouls.CANDY.get());
        giveItem(player, primary);
        giveItem(player, secondary);
        player.getPersistentData().putInt(STORY_STAGE_TAG, STAGE_AFTER_GIFT);
        player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    public void vanishFromThreat(ServerPlayer player) {
        level().playSound(null, blockPosition(), BlackSouls.CHESHIRE_CAT2_EVENT.get(),
                SoundSource.NEUTRAL, 1.0F, 1.0F);
        Vec3 away = player.position().subtract(position());
        if (away.horizontalDistanceSqr() > 0.0001D) {
            Vec3 push = away.normalize().scale(0.45D);
            player.push(push.x, 0.08D, push.z);
            player.hurtMarked = true;
        }
        vanishTicks = 60;
        setInvisible(true);
        setSilent(true);
    }

    private void bindScene(ServerPlayer player) {
        if (!sceneId.isBlank()) {
            return;
        }
        sceneId = CheshireDialogue.normalizeSceneId(
                player.getPersistentData().getString(CURRENT_SCENE_TAG));
    }

    private void checkGiftReturn() {
        if (waitingPlayer == null || level().getServer() == null) {
            return;
        }
        ServerPlayer player = level().getServer().getPlayerList().getPlayer(waitingPlayer);
        if (player == null || player.level() != level()) {
            return;
        }
        double dx = player.getX() - waitingStartX;
        double dz = player.getZ() - waitingStartZ;
        if (dx * dx + dz * dz < 4.0D) {
            return;
        }
        player.getPersistentData().putInt(STORY_STAGE_TAG, STAGE_GIFT);
        awaitingGiftReturn = false;
        waitingPlayer = null;
        setInvisible(false);
        setSilent(false);
    }

    private int storyStage(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(STORY_STAGE_TAG, Tag.TAG_INT)) {
            return data.getInt(STORY_STAGE_TAG);
        }
        if (data.getBoolean(LEGACY_ASKED_ALICE_TAG)) {
            data.remove(LEGACY_ASKED_ALICE_TAG);
            data.putInt(STORY_STAGE_TAG, STAGE_GIFT);
            return STAGE_GIFT;
        }
        return STAGE_INTRO;
    }

    private static void giveItem(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public void resetDialogue(ServerPlayer player) {
        player.getPersistentData().remove(LEGACY_ASKED_ALICE_TAG);
        player.getPersistentData().remove(STORY_STAGE_TAG);
        vanishTicks = 0;
        awaitingGiftReturn = false;
        waitingPlayer = null;
        setInvisible(false);
        setSilent(false);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("CheshireSceneId", sceneId);
        tag.putBoolean("CheshireAwaitingGiftReturn", awaitingGiftReturn);
        if (waitingPlayer != null) {
            tag.putUUID("CheshireWaitingPlayer", waitingPlayer);
            tag.putDouble("CheshireWaitingStartX", waitingStartX);
            tag.putDouble("CheshireWaitingStartZ", waitingStartZ);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        sceneId = CheshireDialogue.normalizeSceneId(tag.getString("CheshireSceneId"));
        vanishTicks = 0;
        awaitingGiftReturn = tag.getBoolean("CheshireAwaitingGiftReturn");
        waitingPlayer = tag.hasUUID("CheshireWaitingPlayer") ? tag.getUUID("CheshireWaitingPlayer") : null;
        waitingStartX = tag.getDouble("CheshireWaitingStartX");
        waitingStartZ = tag.getDouble("CheshireWaitingStartZ");
        setInvisible(awaitingGiftReturn);
        setSilent(awaitingGiftReturn);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }
}

package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.shiroha.mmdskin.render.entity.CustomEntityRenderStateProvider;
import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketOpenDialogue;
import com.BlackSouls.BlackSoulsMod.story.OriginalNpcCharacterBindings;
import com.BlackSouls.BlackSoulsMod.story.OriginalNpcInterpreter;
import com.BlackSouls.BlackSoulsMod.story.OriginalNpcService;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalNpcData;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcLogicData;
import com.google.gson.JsonElement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public final class EntityCheshireCat extends PathfinderMob implements DialogueResettable, CustomEntityRenderStateProvider {
    private static final EntityDataAccessor<Boolean> ORIGINAL_SOURCE_MANAGED = SynchedEntityData.defineId(
            EntityCheshireCat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ORIGINAL_ROUTE_PAGE = SynchedEntityData.defineId(
            EntityCheshireCat.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> ORIGINAL_ROUTE_STATE = SynchedEntityData.defineId(
            EntityCheshireCat.class, EntityDataSerializers.COMPOUND_TAG);
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
    private String forcedRouteTicket = "";

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

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ORIGINAL_SOURCE_MANAGED, false);
        entityData.define(ORIGINAL_ROUTE_PAGE, -2);
        entityData.define(ORIGINAL_ROUTE_STATE, initialOriginalRouteState());
    }

    public boolean isOriginalSourceManaged() {
        return entityData.get(ORIGINAL_SOURCE_MANAGED);
    }

    public int getSpriteOpacity() {
        return entityData.get(ORIGINAL_ROUTE_STATE).getInt("Opacity");
    }

    @Override
    public float getMmdRenderOpacity() {
        return getSpriteOpacity() / 255.0F;
    }

    @Override
    public float getMmdBodyYaw(float resolvedYaw) {
        return isOriginalSourceManaged() ? 180.0F : resolvedYaw;
    }

    public CompoundTag originalRouteState() {
        return entityData.get(ORIGINAL_ROUTE_STATE).copy();
    }

    public String forcedRouteTicket() {
        return forcedRouteTicket;
    }

    public void beginForcedRoute(String ticket) {
        forcedRouteTicket = ticket;
    }

    public void endForcedRoute(String ticket) {
        if (forcedRouteTicket.equals(ticket)) forcedRouteTicket = "";
    }

    public void refreshOriginalRoutePage() {
        if (level().isClientSide()) return;
        if (!isOriginalSourceManaged()) {
            if (!OriginalNpcCharacterBindings.matches(this, OriginalNpcCharacterBindings.CHESHIRE_RABBIT_HOLE)) return;
            entityData.set(ORIGINAL_SOURCE_MANAGED, true);
            awaitingGiftReturn = false;
            vanishTicks = 0;
            waitingPlayer = null;
            waitingStartX = 0.0D;
            waitingStartZ = 0.0D;
        }
        BSOriginalNpcData.Entry definition = OriginalNpcLogicData.get(OriginalNpcCharacterBindings.CHESHIRE_RABBIT_HOLE);
        BSOriginalNpcData.Page page = OriginalNpcInterpreter.selectPage(definition,
                new OriginalNpcService.EventContext(null, this, OriginalNpcCharacterBindings.CHESHIRE_RABBIT_HOLE));
        int index = page == null ? -1 : definition.pages().indexOf(page);
        if (entityData.get(ORIGINAL_ROUTE_PAGE) != index) {
            CompoundTag state = originalRouteState();
            state.putString("CharacterName", page == null ? "" : page.graphic().get("character_name").getAsString());
            state.putInt("PriorityType", page == null ? 0 : page.movement().get("priority_type").getAsInt());
            if (page == null) {
                state.putBoolean("Through", true);
            } else {
                forcedRouteTicket = "";
                state.putBoolean("Through", page.movement().get("through").getAsBoolean());
            }
            entityData.set(ORIGINAL_ROUTE_PAGE, index);
            entityData.set(ORIGINAL_ROUTE_STATE, state);
        }
        updateOriginalRouteVisibility();
    }

    public void applyOriginalMoveCommand(int code, List<JsonElement> parameters) {
        int opacity = 0;
        if (code == 42) {
            if (parameters.size() != 1 || !parameters.get(0).isJsonPrimitive()
                    || !parameters.get(0).getAsJsonPrimitive().isNumber()) {
                throw new UnsupportedOperationException("bound_character_route_opacity");
            }
            opacity = parameters.get(0).getAsBigDecimal().intValueExact();
            if (opacity < 0 || opacity > 255) throw new UnsupportedOperationException("bound_character_route_opacity");
        } else if ((code != 37 && code != 39 && code != 40) || !parameters.isEmpty()) {
            throw new UnsupportedOperationException("bound_character_route_command:" + code);
        }
        refreshOriginalRoutePage();
        if (!isOriginalSourceManaged()) throw new UnsupportedOperationException("bound_character_unmanaged");
        CompoundTag state = originalRouteState();
        if (code == 37) state.putBoolean("Through", true);
        else if (code == 39 || code == 40) state.putBoolean("Transparent", code == 39);
        else state.putInt("Opacity", opacity);
        entityData.set(ORIGINAL_ROUTE_STATE, state);
        updateOriginalRouteVisibility();
    }

    private static CompoundTag initialOriginalRouteState() {
        CompoundTag state = new CompoundTag();
        state.putInt("Opacity", 255);
        state.putBoolean("Transparent", false);
        state.putBoolean("Through", false);
        state.putString("CharacterName", "$チェシャ");
        state.putInt("PriorityType", 1);
        return state;
    }

    private void updateOriginalRouteVisibility() {
        CompoundTag state = entityData.get(ORIGINAL_ROUTE_STATE);
        boolean hidden = state.getBoolean("Transparent") || state.getString("CharacterName").isBlank();
        setInvisible(hidden);
        setSilent(hidden);
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
        List<EntityCheshireCat> presentCats = level.getEntitiesOfClass(EntityCheshireCat.class,
                AABB.ofSize(LEGACY_RABBIT_HOLE_POSITION, 4.0D, 4.0D, 4.0D));
        for (EntityCheshireCat cat : presentCats) {
            cat.refreshOriginalRoutePage();
            if (cat.isOriginalSourceManaged()) return;
        }
        boolean present = !presentCats.isEmpty();
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
            cheshire.refreshOriginalRoutePage();
            if (cheshire.isOriginalSourceManaged()) return;
        }
        data.remove(LEGACY_ASKED_ALICE_TAG);
        data.putInt(STORY_STAGE_TAG, Math.max(data.getInt(STORY_STAGE_TAG), STAGE_GIFT));
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0.0D, onGround() ? 0.0D : getDeltaMovement().y, 0.0D);
        if (!level().isClientSide) {
            refreshOriginalRoutePage();
            if (!isOriginalSourceManaged()) {
                if (awaitingGiftReturn) {
                    checkGiftReturn();
                } else if (vanishTicks > 0) {
                    vanishTicks--;
                }
                refreshVisibility();
            }
        }
    }

    public String getSceneId() {
        return sceneId;
    }

    public void refreshVisibility() {
        refreshOriginalRoutePage();
        if (isOriginalSourceManaged()) {
            updateOriginalRouteVisibility();
            return;
        }
        boolean hidden = awaitingGiftReturn || vanishTicks > 0 || !OriginalNpcCharacterBindings.cheshireSourceVisible(this);
        setInvisible(hidden);
        setSilent(hidden);
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return super.mobInteract(player, hand);
        refreshOriginalRoutePage();
        if (isOriginalSourceManaged()) {
            if (!level().isClientSide() && player instanceof ServerPlayer serverPlayer && !isInvisible()) {
                OriginalNpcService.interactCheshire(serverPlayer, this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        if (vanishTicks > 0 || awaitingGiftReturn
                || !OriginalNpcCharacterBindings.cheshireSourceVisible(this)) {
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
        if (isOriginalSourceManaged()) return;
        refreshOriginalRoutePage();
        if (isOriginalSourceManaged()) return;
        if (!CheshireDialogue.isRabbitHole(sceneId) || storyStage(player) != STAGE_INTRO
                || awaitingGiftReturn || vanishTicks > 0 || !OriginalNpcCharacterBindings.cheshireSourceVisible(this)) {
            return;
        }
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
        refreshVisibility();
    }

    public boolean giveRabbitHoleGift(ServerPlayer player, int primaryGift, int secondaryGift) {
        if (isOriginalSourceManaged()) return false;
        refreshOriginalRoutePage();
        if (isOriginalSourceManaged()) return false;
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
        if (isOriginalSourceManaged()) return;
        refreshOriginalRoutePage();
        if (isOriginalSourceManaged()) return;
        level().playSound(null, blockPosition(), BlackSouls.CHESHIRE_CAT2_EVENT.get(),
                SoundSource.NEUTRAL, 1.0F, 1.0F);
        Vec3 away = player.position().subtract(position());
        if (away.horizontalDistanceSqr() > 0.0001D) {
            Vec3 push = away.normalize().scale(0.45D);
            player.push(push.x, 0.08D, push.z);
            player.hurtMarked = true;
        }
        vanishTicks = 60;
        refreshVisibility();
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
        refreshVisibility();
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
        if (isOriginalSourceManaged()) return;
        refreshOriginalRoutePage();
        if (isOriginalSourceManaged()) return;
        player.getPersistentData().remove(LEGACY_ASKED_ALICE_TAG);
        player.getPersistentData().remove(STORY_STAGE_TAG);
        vanishTicks = 0;
        awaitingGiftReturn = false;
        waitingPlayer = null;
        refreshVisibility();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("CheshireSceneId", sceneId);
        tag.putBoolean("CheshireOriginalSourceManaged", isOriginalSourceManaged());
        tag.putInt("CheshireOriginalRoutePage", entityData.get(ORIGINAL_ROUTE_PAGE));
        tag.put("CheshireOriginalRouteState", originalRouteState());
        tag.putString("CheshireOriginalRouteTicket", forcedRouteTicket);
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
        entityData.set(ORIGINAL_SOURCE_MANAGED, tag.getBoolean("CheshireOriginalSourceManaged"));
        entityData.set(ORIGINAL_ROUTE_PAGE, tag.contains("CheshireOriginalRoutePage", Tag.TAG_INT)
                ? tag.getInt("CheshireOriginalRoutePage") : -2);
        entityData.set(ORIGINAL_ROUTE_STATE, tag.contains("CheshireOriginalRouteState", Tag.TAG_COMPOUND)
                ? tag.getCompound("CheshireOriginalRouteState").copy() : initialOriginalRouteState());
        forcedRouteTicket = tag.getString("CheshireOriginalRouteTicket");
        vanishTicks = 0;
        awaitingGiftReturn = tag.getBoolean("CheshireAwaitingGiftReturn");
        waitingPlayer = tag.hasUUID("CheshireWaitingPlayer") ? tag.getUUID("CheshireWaitingPlayer") : null;
        waitingStartX = tag.getDouble("CheshireWaitingStartX");
        waitingStartZ = tag.getDouble("CheshireWaitingStartZ");
        if (isOriginalSourceManaged()) {
            awaitingGiftReturn = false;
            waitingPlayer = null;
            updateOriginalRouteVisibility();
        } else {
            refreshVisibility();
        }
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
    public boolean canBeCollidedWith() {
        if (!isOriginalSourceManaged()) return super.canBeCollidedWith();
        CompoundTag state = entityData.get(ORIGINAL_ROUTE_STATE);
        return isAlive() && state.getInt("PriorityType") == 1 && !state.getBoolean("Through");
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return isOriginalSourceManaged() ? canBeCollidedWith() && !isPassengerOfSameVehicle(entity)
                : super.canCollideWith(entity);
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

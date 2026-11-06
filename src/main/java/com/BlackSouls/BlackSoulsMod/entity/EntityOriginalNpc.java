package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.story.OriginalNpcService;
import com.shiroha.mmdskin.render.entity.CustomEntityRenderStateProvider;
import com.BlackSouls.BlackSoulsMod.story.OriginalNpcMovementService;
import com.BlackSouls.BlackSoulsMod.story.OriginalNpcAutonomousRoute;
import com.BlackSouls.BlackSoulsMod.story.OriginalPoolCrowdMovementService;
import com.BlackSouls.BlackSoulsMod.story.OriginalPoolCrowdSurface;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalNpcData;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcTextures;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public class EntityOriginalNpc extends PathfinderMob implements CustomEntityRenderStateProvider {
    private static final EntityDataAccessor<String> NPC_ID = SynchedEntityData.defineId(
            EntityOriginalNpc.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> PAGE = SynchedEntityData.defineId(
            EntityOriginalNpc.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> ROUTE_STATE = SynchedEntityData.defineId(
            EntityOriginalNpc.class, EntityDataSerializers.COMPOUND_TAG);
    private boolean erased;
    private String forcedRouteTicket = "";
    private double routeAnimationCount;
    private int routeStopCount;
    private OriginalNpcAutonomousRoute autonomousRoute = new OriginalNpcAutonomousRoute();

    public EntityOriginalNpc(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 1024.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(NPC_ID, "");
        entityData.define(PAGE, -1);
        entityData.define(ROUTE_STATE, initialRouteState());
    }

    public String getNpcId() {
        return entityData.get(NPC_ID);
    }

    public void setNpcId(String id) {
        OriginalNpcMovementService.clearAutonomousMotion(this);
        applyOriginalPoolSurfaceSupport(false);
        autonomousRoute = new OriginalNpcAutonomousRoute();
        entityData.set(NPC_ID, BSOriginalNpcData.getEvent(id) == null ? "" : id);
        entityData.set(PAGE, -2);
        entityData.set(ROUTE_STATE, initialRouteState());
        forcedRouteTicket = "";
        if (!level().isClientSide()) {
            OriginalNpcService.refreshPage(this, null);
        }
    }

    public void setPageIndex(int index) {
        if (entityData.get(PAGE) != index) {
            entityData.set(PAGE, index);
            applyPageMovement();
        }
        updateRouteVisibility();
    }

    public void eraseForCurrentVisit() {
        erased = true;
        setInvisible(true);
    }

    public boolean isErasedForCurrentVisit() {
        return erased;
    }

    public void resetVisit() {
        erased = false;
    }

    public BSOriginalNpcData.Page currentPage() {
        BSOriginalNpcData.Entry entry = BSOriginalNpcData.getEvent(getNpcId());
        int index = entityData.get(PAGE);
        return entry == null || index < 0 || index >= entry.pages().size() ? null : entry.pages().get(index);
    }

    private JsonObject graphic() {
        BSOriginalNpcData.Page page = currentPage();
        if (page != null) {
            return page.graphic();
        }
        JsonObject empty = new JsonObject();
        empty.addProperty("character_name", "");
        return empty;
    }

    public ResourceLocation getCharacterTexture() {
        return OriginalNpcTextures.texture(OriginalNpcTextures.character(getRouteCharacterName()));
    }

    public int getCharacterIndex() {
        return entityData.get(ROUTE_STATE).getInt("CharacterIndex");
    }

    public boolean isSingleCharacterSheet() {
        JsonObject texture = OriginalNpcTextures.character(getRouteCharacterName());
        return texture != null && texture.get("single").getAsBoolean();
    }

    public int getSpriteDirection() {
        return entityData.get(ROUTE_STATE).getInt("Direction");
    }

    public CompoundTag originalMovementState() {
        return entityData.get(ROUTE_STATE).copy();
    }

    public int getSpritePattern() {
        return entityData.get(ROUTE_STATE).getInt("Pattern");
    }

    public int getSpriteOpacity() {
        return entityData.get(ROUTE_STATE).getInt("Opacity");
    }

    @Override
    public float getMmdRenderOpacity() {
        return getSpriteOpacity() / 255.0F;
    }

    @Override
    public float getMmdBodyYaw(float resolvedYaw) {
        BSOriginalNpcData.Entry definition = BSOriginalNpcData.getEvent(getNpcId());
        int base = getSpriteDirection();
        if (definition != null) {
            for (BSOriginalNpcData.Page page : definition.pages()) {
                if (!page.graphic().get("character_name").getAsString().isBlank()) {
                    base = page.graphic().has("direction") ? page.graphic().get("direction").getAsInt() : 2;
                    break;
                }
            }
        }
        return net.minecraft.util.Mth.wrapDegrees(getYRot() + 90.0F
                * (mmdDirectionQuarter(getSpriteDirection()) - mmdDirectionQuarter(base)));
    }

    private static int mmdDirectionQuarter(int direction) {
        return switch (direction) {
            case 4 -> 1;
            case 8 -> 2;
            case 6 -> 3;
            default -> 0;
        };
    }

    public String getRouteCharacterName() {
        return entityData.get(ROUTE_STATE).getString("CharacterName");
    }

    public int getRouteTileId() {
        return entityData.get(ROUTE_STATE).getInt("TileId");
    }

    public CompoundTag originalRouteState() {
        return entityData.get(ROUTE_STATE).copy();
    }

    public int getOriginalBushDepth() {
        return entityData.get(ROUTE_STATE).getInt("OriginalBushDepth");
    }

    public int getOriginalShiftY() {
        return entityData.get(ROUTE_STATE).getInt("OriginalShiftY");
    }

    public void setOriginalBushState(int depth, int shift) {
        if (depth != 0 && depth != 8 || shift != 0 && shift != 4) throw new IllegalArgumentException("original_bush_state");
        CompoundTag current = entityData.get(ROUTE_STATE);
        if (current.getInt("OriginalBushDepth") == depth && current.getInt("OriginalShiftY") == shift) return;
        CompoundTag state = current.copy();
        state.putInt("OriginalBushDepth", depth);
        state.putInt("OriginalShiftY", shift);
        entityData.set(ROUTE_STATE, state);
    }

    public void applyOriginalPoolSurfaceSupport(boolean supported) {
        CompoundTag current = entityData.get(ROUTE_STATE);
        boolean captured = current.getBoolean("PoolSurfaceGravityCaptured");
        if (supported) {
            if (!captured || !current.getBoolean("PoolSurfaceWasActive")) {
                CompoundTag state = current.copy();
                if (!captured) state.putBoolean("PoolSurfaceOriginalNoGravity", isNoGravity());
                state.putBoolean("PoolSurfaceGravityCaptured", true);
                state.putBoolean("PoolSurfaceWasActive", true);
                entityData.set(ROUTE_STATE, state);
            }
            setNoGravity(true);
            setDeltaMovement(0.0D, 0.0D, 0.0D);
            fallDistance = 0;
        } else if (captured) {
            setNoGravity(current.getBoolean("PoolSurfaceOriginalNoGravity"));
            CompoundTag state = current.copy();
            state.remove("PoolSurfaceGravityCaptured");
            state.remove("PoolSurfaceOriginalNoGravity");
            entityData.set(ROUTE_STATE, state);
        }
    }

    public void resetOriginalVisitState() {
        String ticket = forcedRouteTicket;
        forcedRouteTicket = "";
        OriginalNpcMovementService.clearAutonomousMotion(this);
        OriginalNpcMovementService.cancelForVisitReset(this, ticket);
        applyOriginalPoolSurfaceSupport(false);
        autonomousRoute = new OriginalNpcAutonomousRoute();
        erased = false;
        routeAnimationCount = 0;
        routeStopCount = 0;
        entityData.set(ROUTE_STATE, initialRouteState());
        entityData.set(PAGE, -2);
        OriginalNpcMovementService.refreshAutonomousPage(this);
        OriginalPoolCrowdMovementService.refreshBush(this);
    }

    public String forcedRouteTicket() {
        return forcedRouteTicket;
    }

    public void beginForcedRoute(String ticket) {
        if (hasOriginalAutonomy()) autonomousRoute.beforeForcedRoute();
        forcedRouteTicket = ticket;
    }

    public void endForcedRoute(String ticket) {
        if (forcedRouteTicket.equals(ticket)) {
            forcedRouteTicket = "";
            if (hasOriginalAutonomy()) autonomousRoute.afterForcedRoute();
        }
    }

    public boolean hasOriginalAutonomy() {
        return getNpcId().equals("map037_ev053") || OriginalPoolCrowdSurface.isCrowd(getNpcId());
    }

    public OriginalNpcAutonomousRoute originalAutonomousRoute() {
        return autonomousRoute;
    }

    public void lockOriginalMovement(ServerPlayer player) {
        if (!hasOriginalAutonomy() || !autonomousRoute.lock(getSpriteDirection())) return;
        boolean crowd = OriginalPoolCrowdSurface.isCrowd(getNpcId());
        var anchor = crowd ? OriginalNpcMovementService.logicalPosition(this) : position();
        double playerX = crowd ? player.blockPosition().getX() + 0.5D : player.getX();
        double playerZ = crowd ? player.blockPosition().getZ() + 0.5D : player.getZ();
        double dx = playerX - anchor.x, dz = playerZ - anchor.z;
        if ((autonomousRoute.symbol() == null || !autonomousRoute.symbol().enabled()) && (dx != 0 || dz != 0)) {
            int direction = Math.abs(dx) > Math.abs(dz) ? dx > 0 ? 4 : 6 : dz > 0 ? 8 : 2;
            applyOriginalMoveCommand(direction == 2 ? 16 : direction == 4 ? 17 : direction == 6 ? 18 : 19, List.of());
        }
    }

    public void unlockOriginalMovement() {
        if (!hasOriginalAutonomy()) return;
        int direction = autonomousRoute.unlock();
        if (direction != 0) applyOriginalMoveCommand(direction == 2 ? 16 : direction == 4 ? 17 : direction == 6 ? 18 : 19, List.of());
    }

    private static CompoundTag initialRouteState() {
        CompoundTag state = new CompoundTag();
        state.putString("CharacterName", "");
        state.putInt("TileId", 0);
        state.putInt("CharacterIndex", 0);
        state.putInt("Direction", 2);
        state.putInt("OriginalDirection", 2);
        state.putInt("Pattern", 1);
        state.putInt("OriginalPattern", 1);
        state.putInt("MoveSpeed", 4);
        state.putInt("MoveFrequency", 6);
        state.putBoolean("WalkAnimation", true);
        state.putBoolean("StepAnimation", false);
        state.putBoolean("DirectionFixed", false);
        state.putBoolean("Through", false);
        state.putBoolean("Transparent", false);
        state.putInt("Opacity", 255);
        state.putInt("Blend", 0);
        state.putInt("OriginalBushDepth", 0);
        state.putInt("OriginalShiftY", 0);
        return state;
    }

    private void applyPageMovement() {
        CompoundTag state = entityData.get(ROUTE_STATE).copy();
        JsonObject graphic = graphic();
        state.putString("CharacterName", graphic.get("character_name").getAsString());
        state.putInt("TileId", graphic.has("tile_id") ? graphic.get("tile_id").getAsInt() : 0);
        state.putInt("CharacterIndex", graphic.has("character_index") ? graphic.get("character_index").getAsInt() : 0);
        BSOriginalNpcData.Page page = currentPage();
        if (page == null) {
            state.putBoolean("Through", true);
            if (hasOriginalAutonomy()) forcedRouteTicket = "";
        } else {
            forcedRouteTicket = "";
            int direction = graphic.has("direction") ? graphic.get("direction").getAsInt() : 2;
            int pattern = graphic.has("pattern") ? graphic.get("pattern").getAsInt() : 1;
            if (state.getInt("OriginalDirection") != direction) {
                state.putInt("Direction", direction);
                state.putInt("OriginalDirection", direction);
                if (hasOriginalAutonomy()) autonomousRoute.clearPrelockDirection();
            }
            if (state.getInt("OriginalPattern") != pattern) {
                state.putInt("Pattern", pattern);
                state.putInt("OriginalPattern", pattern);
            }
            JsonObject movement = page.movement();
            state.putInt("MoveSpeed", movement.has("move_speed") ? movement.get("move_speed").getAsInt() : 4);
            state.putInt("MoveFrequency", movement.has("move_frequency") ? movement.get("move_frequency").getAsInt() : 6);
            state.putBoolean("WalkAnimation", !movement.has("walk_anime") || movement.get("walk_anime").getAsBoolean());
            state.putBoolean("StepAnimation", movement.has("step_anime") && movement.get("step_anime").getAsBoolean());
            state.putBoolean("DirectionFixed", movement.has("direction_fix") && movement.get("direction_fix").getAsBoolean());
            state.putBoolean("Through", movement.has("through") && movement.get("through").getAsBoolean());
        }
        entityData.set(ROUTE_STATE, state);
        if (hasOriginalAutonomy()) autonomousRoute.configure(page == null ? new JsonObject() : page.movement());
        if (OriginalPoolCrowdSurface.isCrowd(getNpcId())) OriginalPoolCrowdMovementService.refreshBush(this);
    }

    public void applyOriginalMoveCommand(int code, List<JsonElement> parameters) {
        CompoundTag state = entityData.get(ROUTE_STATE).copy();
        switch (code) {
            case 16, 17, 18, 19 -> setRouteDirection(state, switch (code) {
                case 16 -> 2;
                case 17 -> 4;
                case 18 -> 6;
                default -> 8;
            });
            case 20, 21, 22, 23, 24 -> {
                int turn = code == 23 ? (getRandom().nextBoolean() ? 20 : 21) : code;
                int direction = state.getInt("Direction");
                int next = turn == 22 ? 10 - direction : turn == 24 ? 2 + getRandom().nextInt(4) * 2
                        : turn == 20 ? switch (direction) { case 2 -> 4; case 4 -> 8; case 6 -> 2; default -> 6; }
                        : switch (direction) { case 2 -> 6; case 4 -> 2; case 6 -> 8; default -> 4; };
                setRouteDirection(state, next);
            }
            case 29 -> state.putInt("MoveSpeed", parameters.get(0).getAsInt());
            case 30 -> state.putInt("MoveFrequency", parameters.get(0).getAsInt());
            case 31, 32 -> state.putBoolean("WalkAnimation", code == 31);
            case 33, 34 -> state.putBoolean("StepAnimation", code == 33);
            case 35, 36 -> state.putBoolean("DirectionFixed", code == 35);
            case 37, 38 -> state.putBoolean("Through", code == 37);
            case 39, 40 -> state.putBoolean("Transparent", code == 39);
            case 41 -> {
                state.putString("CharacterName", parameters.get(0).getAsString());
                state.putInt("TileId", 0);
                state.putInt("CharacterIndex", parameters.get(1).getAsInt());
                state.putInt("OriginalPattern", 1);
            }
            case 42 -> state.putInt("Opacity", parameters.get(0).getAsInt());
            case 43 -> state.putInt("Blend", parameters.get(0).getAsInt());
            default -> throw new UnsupportedOperationException("npc_route_command:" + code);
        }
        entityData.set(ROUTE_STATE, state);
        updateRouteVisibility();
    }

    private void setRouteDirection(CompoundTag state, int direction) {
        if (!state.getBoolean("DirectionFixed")) state.putInt("Direction", direction);
        routeStopCount = 0;
        if (hasOriginalAutonomy()) autonomousRoute.resetStopCount();
    }

    public void advanceOriginalAnimationFrame() {
        advanceOriginalAnimationFrame(false);
    }

    private void advanceOriginalAnimationFrame(boolean moving) {
        CompoundTag state = entityData.get(ROUTE_STATE);
        if (moving && state.getBoolean("WalkAnimation")) routeAnimationCount += 1.5D;
        else if (state.getBoolean("StepAnimation") || state.getInt("Pattern") != state.getInt("OriginalPattern")) {
            routeAnimationCount++;
        }
        if (routeAnimationCount > 18 - state.getInt("MoveSpeed") * 2) {
            state = state.copy();
            state.putInt("Pattern", !state.getBoolean("StepAnimation") && routeStopCount > 0
                    ? state.getInt("OriginalPattern") : (state.getInt("Pattern") + 1) % 4);
            entityData.set(ROUTE_STATE, state);
            routeAnimationCount = 0;
        }
        if (routeStopCount < Integer.MAX_VALUE) routeStopCount++;
    }

    public void advanceOriginalAutonomousAnimationFrame(int stopCount) {
        routeStopCount = stopCount;
        advanceOriginalAnimationFrame();
        routeStopCount = stopCount;
    }

    public void advanceOriginalAutonomousAnimationFrame(int stopCount, boolean moving) {
        routeStopCount = stopCount;
        advanceOriginalAnimationFrame(moving);
        routeStopCount = stopCount;
    }

    public void advanceOriginalForcedAnimationFrame(boolean moving) {
        if (!hasOriginalAutonomy()) {
            advanceOriginalAnimationFrame();
            return;
        }
        if (OriginalPoolCrowdSurface.isCrowd(getNpcId())) advanceOriginalAutonomousAnimationFrame(autonomousRoute.stopCount(),
                OriginalNpcMovementService.isOriginalWalking(this));
        else advanceOriginalAutonomousAnimationFrame(autonomousRoute.stopCount());
        if (!moving) autonomousRoute.advanceStopFrame();
        routeStopCount = autonomousRoute.stopCount();
    }

    private void updateRouteVisibility() {
        setInvisible(erased || com.BlackSouls.BlackSoulsMod.util.OriginalWorldEventData.isNativeBlock(getNpcId())
                || entityData.get(ROUTE_STATE).getBoolean("Transparent") || getRouteCharacterName().isBlank()
                && com.BlackSouls.BlackSoulsMod.util.OriginalNpcTileGraphics.get(getNpcId(), getRouteTileId()) == null);
    }

    @Override
    public Component getName() {
        BSOriginalNpcData.Entry entry = BSOriginalNpcData.getEvent(getNpcId());
        return hasCustomName() ? getCustomName() : Component.literal(entry == null
                || com.BlackSouls.BlackSoulsMod.util.OriginalWorldEventData.isNativeBlock(getNpcId()) ? "" : entry.name());
    }

    @Override
    public void tick() {
        if (com.BlackSouls.BlackSoulsMod.util.OriginalWorldEventData.isNativeBlock(getNpcId())) {
            noPhysics = true;
            setNoGravity(true);
        }
        if (!level().isClientSide()) OriginalPoolCrowdMovementService.beforeEntityTick(this);
        super.tick();
        setDeltaMovement(0.0D, onGround() ? 0.0D : getDeltaMovement().y, 0.0D);
        if (!level().isClientSide() && forcedRouteTicket.isEmpty()) {
            if (!OriginalNpcMovementService.tickAutonomous(this)) {
                for (int frame = 0; frame < 3; frame++) advanceOriginalAnimationFrame();
            } else {
                routeStopCount = autonomousRoute.stopCount();
            }
        }
        if (!level().isClientSide() && tickCount % 20 == 0) {
            if (!OriginalNpcMovementService.refreshPageWhileForcing(this)) {
                Player nearby = level().getNearestPlayer(this, 32.0D);
                OriginalNpcService.refreshPage(this, nearby instanceof ServerPlayer player ? player : null);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (!hasOriginalAutonomy() || currentPage() == null || currentPage().trigger() != 2) OriginalNpcService.interact(serverPlayer, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("OriginalNpcId", getNpcId());
        tag.putInt("OriginalNpcPage", entityData.get(PAGE));
        tag.put("OriginalNpcRouteState", entityData.get(ROUTE_STATE).copy());
        tag.putString("OriginalNpcRouteTicket", forcedRouteTicket);
        tag.putDouble("OriginalNpcAnimeCount", routeAnimationCount);
        tag.putInt("OriginalNpcStopCount", routeStopCount);
        if (hasOriginalAutonomy()) tag.putByteArray("OriginalNpcAutonomousState", autonomousRoute.snapshot().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setNpcId(tag.getString("OriginalNpcId"));
        if (tag.contains("OriginalNpcRouteState")) {
            entityData.set(PAGE, tag.getInt("OriginalNpcPage"));
            CompoundTag state = tag.getCompound("OriginalNpcRouteState").copy();
            if (!state.contains("TileId")) {
                JsonObject graphic = graphic();
                int tileId = graphic.has("tile_id") ? graphic.get("tile_id").getAsInt() : 0;
                boolean unchanged = state.getString("CharacterName").equals(graphic.get("character_name").getAsString())
                        && state.getInt("CharacterIndex") == (graphic.has("character_index") ? graphic.get("character_index").getAsInt() : 0)
                        && tag.getString("OriginalNpcRouteTicket").isEmpty();
                state.putInt("TileId", unchanged && com.BlackSouls.BlackSoulsMod.util.OriginalNpcTileGraphics.get(getNpcId(), tileId) != null ? tileId : 0);
            }
            if (!state.contains("TileId", net.minecraft.nbt.Tag.TAG_INT) || state.getInt("TileId") < 0) {
                throw new IllegalArgumentException("Invalid original NPC route tile graphic");
            }
            entityData.set(ROUTE_STATE, state);
            forcedRouteTicket = tag.getString("OriginalNpcRouteTicket");
            routeAnimationCount = tag.getDouble("OriginalNpcAnimeCount");
            routeStopCount = tag.getInt("OriginalNpcStopCount");
            if (hasOriginalAutonomy()) {
                autonomousRoute.configure(currentPage() == null ? new JsonObject() : currentPage().movement());
                if (tag.contains("OriginalNpcAutonomousState", net.minecraft.nbt.Tag.TAG_BYTE_ARRAY)) {
                    autonomousRoute.restore(com.google.gson.JsonParser.parseString(new String(tag.getByteArray("OriginalNpcAutonomousState"),
                            java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
                    OriginalNpcMovementService.validateAutonomousState(this);
                }
            }
            if (OriginalPoolCrowdSurface.isCrowd(getNpcId())
                    && !tag.contains("OriginalNpcAutonomousState", net.minecraft.nbt.Tag.TAG_BYTE_ARRAY)
                    && (!state.contains("OriginalBushDepth", net.minecraft.nbt.Tag.TAG_INT)
                    || !state.contains("OriginalShiftY", net.minecraft.nbt.Tag.TAG_INT))) {
                OriginalPoolCrowdMovementService.refreshBush(this);
            }
            updateRouteVisibility();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !com.BlackSouls.BlackSoulsMod.util.OriginalWorldEventData.isNativeBlock(getNpcId()) && super.isPickable();
    }

    @Override
    public boolean canBeCollidedWith() {
        BSOriginalNpcData.Page page = currentPage();
        if (erased || !isAlive() || page == null || com.BlackSouls.BlackSoulsMod.util.OriginalWorldEventData.isNativeBlock(getNpcId())
                || entityData.get(ROUTE_STATE).getBoolean("Through")) return false;
        JsonObject movement = page.movement();
        return !movement.has("priority_type") || movement.get("priority_type").getAsInt() == 1;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return canBeCollidedWith() && !isPassengerOfSameVehicle(entity);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}

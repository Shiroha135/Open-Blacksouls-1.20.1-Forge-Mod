package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import com.BlackSouls.BlackSoulsMod.story.OriginalNpcService;
import com.BlackSouls.BlackSoulsMod.story.OriginalNpcInterpreter;
import com.BlackSouls.BlackSoulsMod.story.OriginalRabbitKnightBinding;
import com.BlackSouls.BlackSoulsMod.util.BSOriginalNpcData;
import com.google.gson.JsonElement;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import com.shiroha.mmdskin.render.entity.CustomEntityRenderStateProvider;
import java.util.List;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketOpenDialogue;
import com.BlackSouls.BlackSoulsMod.util.PlayerStatService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class EntityRabbitKnight extends EntityOriginalDatabaseEnemy
        implements DialogueResettable, CustomEntityRenderStateProvider {
    private static final EntityDataAccessor<Boolean> SOURCE_MANAGED = SynchedEntityData.defineId(EntityRabbitKnight.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SOURCE_PAGE = SynchedEntityData.defineId(EntityRabbitKnight.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> SOURCE_ROUTE = SynchedEntityData.defineId(EntityRabbitKnight.class, EntityDataSerializers.COMPOUND_TAG);
    private String forcedRouteTicket = "";
    private boolean introduced;
    private boolean killBattleArmed;

    public EntityRabbitKnight(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setProfileId(217);
        setCustomName(Component.translatable("entity.blacksouls.rabbit_knight.name"));
        setCustomNameVisible(true);
        setPersistenceRequired();
        setNoAi(true);
        setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SOURCE_MANAGED, false);
        entityData.define(SOURCE_PAGE, -2);
        entityData.define(SOURCE_ROUTE, initialSourceRoute());
    }

    public boolean isOriginalSourceManaged() {
        return entityData.get(SOURCE_MANAGED);
    }

    public CompoundTag originalRouteState() {
        return entityData.get(SOURCE_ROUTE).copy();
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

    public int getSpriteOpacity() {
        return isOriginalSourceManaged() ? entityData.get(SOURCE_ROUTE).getInt("Opacity") : 255;
    }

    @Override
    public float getMmdRenderOpacity() {
        return getSpriteOpacity() / 255.0F;
    }

    @Override
    public float getMmdBodyYaw(float resolvedYaw) {
        return isOriginalSourceManaged() ? 270.0F : resolvedYaw;
    }

    public void refreshOriginalSourcePage() {
        if (level().isClientSide()) return;
        if (isOriginalSourceManaged() && level() instanceof net.minecraft.server.level.ServerLevel sourceLevel
                && com.BlackSouls.BlackSoulsMod.compat.scene.OriginalNpcPlacementData.get(sourceLevel)
                .entityId(OriginalRabbitKnightBinding.SOURCE_ID) == null) OriginalRabbitKnightBinding.claim(this);
        if (!isOriginalSourceManaged()) {
            if (!OriginalRabbitKnightBinding.claim(this)) return;
            entityData.set(SOURCE_MANAGED, true);
            killBattleArmed = false;
            setCustomNameVisible(false);
        }
        var definition = BSOriginalNpcData.get(OriginalRabbitKnightBinding.SOURCE_ID);
        var page = OriginalNpcInterpreter.selectPage(definition,
                new OriginalNpcService.EventContext(null, this, OriginalRabbitKnightBinding.SOURCE_ID));
        int index = page == null ? -1 : definition.pages().indexOf(page);
        if (entityData.get(SOURCE_PAGE) != index) {
            var state = originalRouteState();
            var graphic = page == null ? null : page.graphic();
            state.putString("CharacterName", graphic == null ? "" : graphic.get("character_name").getAsString());
            state.putInt("CharacterIndex", graphic == null ? 0 : graphic.get("character_index").getAsInt());
            state.putInt("Direction", graphic == null ? 2 : graphic.get("direction").getAsInt());
            state.putInt("Pattern", graphic == null ? 1 : graphic.get("pattern").getAsInt());
            state.putInt("PriorityType", page == null ? 0 : page.movement().get("priority_type").getAsInt());
            state.putBoolean("Through", page == null || page.movement().get("through").getAsBoolean());
            if (page != null) forcedRouteTicket = "";
            entityData.set(SOURCE_PAGE, index);
            entityData.set(SOURCE_ROUTE, state);
        }
        updateSourceVisibility();
    }

    public void applyOriginalMoveCommand(int code, List<JsonElement> parameters) {
        if (code != 37 && code != 39 && code != 40 && code != 42) throw new UnsupportedOperationException("rabbit_route_command:" + code);
        if (code == 42 ? parameters.size() != 1 || !parameters.get(0).isJsonPrimitive()
                || !parameters.get(0).getAsJsonPrimitive().isNumber() : !parameters.isEmpty()) {
            throw new UnsupportedOperationException("rabbit_route_parameters");
        }
        int opacity = code == 42 ? parameters.get(0).getAsBigDecimal().intValueExact() : 0;
        if (opacity < 0 || opacity > 255) throw new UnsupportedOperationException("rabbit_route_opacity");
        refreshOriginalSourcePage();
        if (!isOriginalSourceManaged()) throw new UnsupportedOperationException("rabbit_source_unmanaged");
        var state = originalRouteState();
        if (code == 37) state.putBoolean("Through", true);
        else if (code == 39 || code == 40) state.putBoolean("Transparent", code == 39);
        else state.putInt("Opacity", opacity);
        entityData.set(SOURCE_ROUTE, state);
        updateSourceVisibility();
    }

    private static CompoundTag initialSourceRoute() {
        var state = new CompoundTag();
        state.putString("CharacterName", "$うさぎ騎士");
        state.putInt("CharacterIndex", 0);
        state.putInt("Direction", 4);
        state.putInt("Pattern", 1);
        state.putInt("PriorityType", 1);
        state.putInt("Opacity", 255);
        return state;
    }

    private void updateSourceVisibility() {
        var state = entityData.get(SOURCE_ROUTE);
        setInvisible(state.getBoolean("Transparent") || state.getString("CharacterName").isBlank());
        setCustomNameVisible(false);
    }

    @Override
    public boolean canBeCollidedWith() {
        if (!isOriginalSourceManaged()) return super.canBeCollidedWith();
        var state = entityData.get(SOURCE_ROUTE);
        return isAlive() && state.getInt("PriorityType") == 1 && !state.getBoolean("Through");
    }

    @Override
    public boolean canCollideWith(net.minecraft.world.entity.Entity entity) {
        return isOriginalSourceManaged() ? canBeCollidedWith() && !isPassengerOfSameVehicle(entity) : super.canCollideWith(entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 20 == 0) refreshOriginalSourcePage();
        if (!TurnBattleManager.isInBattle(this)) {
            setDeltaMovement(Vec3.ZERO);
            setNoAi(true);
            setInvulnerable(true);
        }
    }

    @Override
    protected @NotNull InteractionResult mobInteract(
            @NotNull Player player, @NotNull InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !level().isClientSide()) refreshOriginalSourcePage();
        if (isOriginalSourceManaged()) {
            if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) OriginalNpcService.interactRabbitKnight(serverPlayer, this);
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        if (hand != InteractionHand.MAIN_HAND || TurnBattleManager.isInBattle(this)) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            boolean firstMeeting = !introduced;
            introduced = true;
            NetworkHandler.sendToPlayer(new PacketOpenDialogue(
                    "entity.blacksouls.rabbit_knight.name",
                    "rabbit_knight",
                    firstMeeting ? RabbitKnightDialogue.introductionKeys()
                            : RabbitKnightDialogue.repeatKeys(),
                    !firstMeeting,
                    getId(),
                    -1,
                    false,
                    -1,
                    false,
                    firstMeeting ? CheshireDialogue.Mode.NONE
                            : CheshireDialogue.Mode.RABBIT_KNIGHT
            ), serverPlayer);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public boolean startKillBattle(ServerPlayer player) {
        refreshOriginalSourcePage();
        if (isOriginalSourceManaged() || distanceToSqr(player) > 64.0D || TurnBattleManager.isInBattle(this)) {
            return false;
        }
        killBattleArmed = true;
        setInvulnerable(false);
        TurnBattleManager.tryStart(player, this);
        if (!TurnBattleManager.isInBattle(this)) {
            killBattleArmed = false;
            setInvulnerable(true);
            return false;
        }
        return true;
    }

    public void finishKnightBattle(ServerPlayer player, boolean victory) {
        killBattleArmed = false;
        if (victory) {
            PlayerStatService.addSen(player, -5);
            return;
        }
        setNoAi(true);
        setInvulnerable(true);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return TurnBattleManager.isInBattle(this) && super.hurt(source, amount);
    }

    @Override
    public boolean canStartTurnBattle() {
        return !isOriginalSourceManaged() && killBattleArmed;
    }

    @Override
    public void resetDialogue(ServerPlayer player) {
        refreshOriginalSourcePage();
        if (isOriginalSourceManaged()) return;
        introduced = false;
        killBattleArmed = false;
        setTarget(null);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        setNoAi(true);
        setInvulnerable(true);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("RabbitKnightIntroduced", introduced);
        tag.putBoolean("RabbitOriginalSourceManaged", isOriginalSourceManaged());
        tag.putInt("RabbitOriginalPage", entityData.get(SOURCE_PAGE));
        tag.put("RabbitOriginalRoute", originalRouteState());
        tag.putString("RabbitOriginalRouteTicket", forcedRouteTicket);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setProfileId(217);
        introduced = tag.getBoolean("RabbitKnightIntroduced");
        entityData.set(SOURCE_MANAGED, tag.getBoolean("RabbitOriginalSourceManaged"));
        entityData.set(SOURCE_PAGE, tag.contains("RabbitOriginalPage", Tag.TAG_INT) ? tag.getInt("RabbitOriginalPage") : -2);
        entityData.set(SOURCE_ROUTE, tag.contains("RabbitOriginalRoute", Tag.TAG_COMPOUND) ? tag.getCompound("RabbitOriginalRoute").copy() : initialSourceRoute());
        forcedRouteTicket = tag.getString("RabbitOriginalRouteTicket");
        if (isOriginalSourceManaged()) updateSourceVisibility();
        killBattleArmed = false;
        setNoAi(true);
        setInvulnerable(true);
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
        return !isOriginalSourceManaged();
    }
}

package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import com.BlackSouls.BlackSoulsMod.handler.BSEntityRegistry;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.PacketOpenDialogue;
import com.BlackSouls.BlackSoulsMod.util.PlayerStatService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.UUID;

public class EntityRabbitHoleNpc extends Rabbit implements DialogueResettable {
    private Role role = Role.EV052;
    private boolean foodEaten;
    private UUID battleEnemy;

    public EntityRabbitHoleNpc(EntityType<? extends Rabbit> type, Level level) {
        super(type, level);
        this.setVariant(Variant.WHITE);
        this.setCustomName(Component.translatable("entity.blacksouls.rabbit_hole_npc.name"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Rabbit.createAttributes();
    }

    public void setRole(Role role) {
        this.role = role == null ? Role.EV052 : role;
    }

    public Role getRole() {
        return this.role;
    }

    public void setFoodEaten(boolean foodEaten) {
        this.foodEaten = foodEaten;
    }

    public boolean isFoodEaten() {
        return this.foodEaten;
    }

    public boolean startKillBattle(ServerPlayer player) {
        if (!(this.level() instanceof ServerLevel level) || this.battleEnemy != null
                || this.distanceToSqr(player) > 64.0D) {
            return false;
        }
        EntityCorpseEatingRabbit enemy = BSEntityRegistry.CORPSE_EATING_RABBIT.get().create(level);
        if (enemy == null) {
            return false;
        }
        enemy.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
        enemy.bindDialogueRabbit(this.getUUID());
        level.addFreshEntity(enemy);
        TurnBattleManager.tryStart(player, enemy);
        if (!TurnBattleManager.isInBattle(enemy)) {
            enemy.discard();
            return false;
        }
        this.battleEnemy = enemy.getUUID();
        this.setInvisible(true);
        this.setSilent(true);
        return true;
    }

    public void finishKillBattle(ServerPlayer player, boolean victory) {
        this.battleEnemy = null;
        if (victory) {
            PlayerStatService.addSen(player, -5);
            this.discard();
            return;
        }
        this.setInvisible(false);
        this.setSilent(false);
    }

    @Override
    public void tick() {
        super.tick();
        this.setVariant(Variant.WHITE);
        this.setDeltaMovement(0.0D, this.onGround() ? 0.0D : this.getDeltaMovement().y, 0.0D);
        this.yBodyRot = this.getYRot();
        if (!this.level().isClientSide && this.battleEnemy != null && this.tickCount % 10 == 0
                && this.level() instanceof ServerLevel level) {
            Entity enemy = level.getEntity(this.battleEnemy);
            if (enemy == null || !TurnBattleManager.isInBattle(enemy)) {
                if (enemy instanceof EntityCorpseEatingRabbit) {
                    enemy.discard();
                }
                this.battleEnemy = null;
                this.setInvisible(false);
                this.setSilent(false);
            }
        }
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || this.battleEnemy != null) {
            return super.mobInteract(player, hand);
        }
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0F / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.yBodyRot = yaw;
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            int sen = player.getCapability(BSPlayerStats.CAPABILITY).map(stats -> stats.sen).orElse(100);
            boolean lowSen = sen <= 30;
            NetworkHandler.sendToPlayer(
                    new PacketOpenDialogue(
                            lowSen
                                    ? "entity.blacksouls.rabbit_hole_npc.low_sen_name"
                                    : "entity.blacksouls.rabbit_hole_npc.name",
                            "",
                            RabbitHoleDialogue.keys(this.role, lowSen, this.foodEaten),
                            true,
                            this.getId(),
                            -1,
                            true
                    ),
                    serverPlayer
            );
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void resetDialogue(ServerPlayer player) {
        if (this.battleEnemy != null && this.level() instanceof ServerLevel level) {
            Entity enemy = level.getEntity(this.battleEnemy);
            if (enemy != null && !TurnBattleManager.isInBattle(enemy)) {
                enemy.discard();
            }
        }
        this.battleEnemy = null;
        this.foodEaten = false;
        this.setInvisible(false);
        this.setSilent(false);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("RabbitHoleRole", this.role.id());
        tag.putBoolean("RabbitFoodEaten", this.foodEaten);
        if (this.battleEnemy != null) {
            tag.putUUID("RabbitBattleEnemy", this.battleEnemy);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.role = Role.fromId(tag.getString("RabbitHoleRole"));
        this.foodEaten = tag.getBoolean("RabbitFoodEaten");
        this.battleEnemy = tag.hasUUID("RabbitBattleEnemy") ? tag.getUUID("RabbitBattleEnemy") : null;
        this.setVariant(Variant.WHITE);
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

    public enum Role {
        EV052,
        EV011,
        EV012;

        public String id() {
            return this.name();
        }

        public static Role fromId(String id) {
            if (id == null || id.isBlank()) {
                return EV052;
            }
            try {
                return valueOf(id.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return EV052;
            }
        }
    }
}

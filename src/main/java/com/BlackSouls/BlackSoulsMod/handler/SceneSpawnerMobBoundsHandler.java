package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.combat.TurnBattleManager;
import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SceneSpawnerMobBoundsHandler {
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) {
            return;
        }
        CompoundTag data = mob.getPersistentData();
        if (!data.contains(SceneSpawnerBounds.ORIGIN_X_TAG)
                || !data.contains(SceneSpawnerBounds.ORIGIN_Z_TAG)
                || !data.contains(SceneSpawnerBounds.RANGE_X_TAG)
                || !data.contains(SceneSpawnerBounds.RANGE_Z_TAG)) {
            return;
        }
        if (TurnBattleManager.isInBattle(mob)) {
            return;
        }
        if (!data.contains(SceneSpawnerBounds.ORIGINAL_NO_AI_TAG)) {
            data.putBoolean(SceneSpawnerBounds.ORIGINAL_NO_AI_TAG, mob.isNoAi());
        }
        boolean originalNoAi = data.getBoolean(SceneSpawnerBounds.ORIGINAL_NO_AI_TAG);

        int originX = data.getInt(SceneSpawnerBounds.ORIGIN_X_TAG);
        int originY = data.getInt(SceneSpawnerBounds.ORIGIN_Y_TAG);
        int originZ = data.getInt(SceneSpawnerBounds.ORIGIN_Z_TAG);
        int rangeX = Mth.clamp(data.getInt(SceneSpawnerBounds.RANGE_X_TAG), 1, SceneSpawnerBounds.MAX_RANGE);
        int rangeZ = Mth.clamp(data.getInt(SceneSpawnerBounds.RANGE_Z_TAG), 1, SceneSpawnerBounds.MAX_RANGE);
        double centerX = originX + 0.5D;
        double centerZ = originZ + 0.5D;
        double minX = centerX - rangeX * 0.5D;
        double maxX = centerX + rangeX * 0.5D;
        double minZ = centerZ - rangeZ * 0.5D;
        double maxZ = centerZ + rangeZ * 0.5D;

        LivingEntity target = mob.getTarget();
        if (target != null && (!target.isAlive()
                || !mob.canAttack(target)
                || !blacksouls$inside(target.getX(), target.getZ(), minX, maxX, minZ, maxZ))) {
            mob.setTarget(null);
            mob.setAggressive(false);
            mob.getNavigation().stop();
            target = null;
        }

        if (!blacksouls$inside(mob.getX(), mob.getZ(), minX, maxX, minZ, maxZ)) {
            double insetX = Math.min(rangeX * 0.5D - 0.05D, Math.max(0.05D, mob.getBbWidth() * 0.5D));
            double insetZ = Math.min(rangeZ * 0.5D - 0.05D, Math.max(0.05D, mob.getBbWidth() * 0.5D));
            double returnX = Mth.clamp(mob.getX(), minX + insetX, maxX - insetX);
            double returnZ = Mth.clamp(mob.getZ(), minZ + insetZ, maxZ - insetZ);
            mob.setTarget(null);
            mob.setAggressive(false);
            data.remove(SceneSpawnerBounds.IDLE_LOCK_TAG);
            if (!originalNoAi) {
                mob.setNoAi(false);
                mob.getNavigation().moveTo(returnX, originY, returnZ, 1.0D);
            } else {
                mob.setPos(returnX, mob.getY(), returnZ);
            }
            return;
        }

        if (target == null && mob.tickCount % 5 == 0) {
            AABB searchArea = new AABB(
                    minX,
                    mob.level().getMinBuildHeight(),
                    minZ,
                    maxX,
                    mob.level().getMaxBuildHeight(),
                    maxZ
            );
            Player nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            double followRange = Math.max(1.0D, mob.getAttributeValue(Attributes.FOLLOW_RANGE));
            double followRangeSqr = followRange * followRange;
            for (Player player : mob.level().getEntitiesOfClass(Player.class, searchArea,
                    player -> player.isAlive() && !player.isSpectator()
                            && !player.getAbilities().instabuild
                            && mob.canAttack(player)
                            && mob.distanceToSqr(player) <= followRangeSqr
                            && mob.hasLineOfSight(player))) {
                double distance = mob.distanceToSqr(player);
                if (distance < nearestDistance) {
                    nearest = player;
                    nearestDistance = distance;
                }
            }
            if (nearest != null) {
                data.remove(SceneSpawnerBounds.IDLE_LOCK_TAG);
                if (!originalNoAi) {
                    mob.setNoAi(false);
                }
                mob.setTarget(nearest);
                mob.setAggressive(true);
                mob.getLookControl().setLookAt(nearest, 30.0F, 30.0F);
                mob.getNavigation().moveTo(nearest, 1.0D);
                target = nearest;
            }
        }

        if (target == null) {
            mob.setTarget(null);
            mob.setAggressive(false);
            mob.getNavigation().stop();
            Vec3 movement = mob.getDeltaMovement();
            mob.setDeltaMovement(0.0D, movement.y, 0.0D);
            if (!originalNoAi) {
                data.putBoolean(SceneSpawnerBounds.IDLE_LOCK_TAG, true);
                if (!mob.isNoAi()) {
                    mob.setNoAi(true);
                }
            }
            return;
        }

        data.remove(SceneSpawnerBounds.IDLE_LOCK_TAG);
        if (!originalNoAi && mob.isNoAi()) {
            mob.setNoAi(false);
        }
        if (target != null && mob.tickCount % 10 == 0 && mob.getNavigation().isDone()) {
            mob.setAggressive(true);
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            mob.getNavigation().moveTo(target, 1.0D);
        }

        BlockPos navigationTarget = mob.getNavigation().getTargetPos();
        if (navigationTarget != null && !blacksouls$inside(navigationTarget.getX() + 0.5D,
                navigationTarget.getZ() + 0.5D, minX, maxX, minZ, maxZ)) {
            mob.getNavigation().stop();
        }

        Vec3 movement = mob.getDeltaMovement();
        if (!blacksouls$inside(mob.getX() + movement.x, mob.getZ() + movement.z,
                minX, maxX, minZ, maxZ)) {
            mob.setDeltaMovement(0.0D, movement.y, 0.0D);
            mob.getNavigation().stop();
        }
    }

    private static boolean blacksouls$inside(double x, double z,
                                               double minX, double maxX, double minZ, double maxZ) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    private SceneSpawnerMobBoundsHandler() {
    }
}

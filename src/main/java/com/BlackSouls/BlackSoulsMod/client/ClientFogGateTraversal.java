package com.BlackSouls.BlackSoulsMod.client;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class ClientFogGateTraversal {
    private static final double SPEED = 0.115D;
    private static final double TARGET_EPSILON = 0.02D;
    private static WalkState active;

    public static void start(Direction direction, double targetX, double targetZ) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        stop();
        active = new WalkState(
                player.getUUID(),
                direction,
                targetX,
                targetZ,
                player.noPhysics,
                player.isNoGravity()
        );
        player.noPhysics = true;
        player.setNoGravity(true);
        applyMovement(player, active);
    }

    public static void stop() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        WalkState state = active;
        active = null;
        if (player == null || state == null || !state.playerId.equals(player.getUUID())) {
            return;
        }
        player.noPhysics = state.previousNoPhysics;
        player.setNoGravity(state.previousNoGravity);
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(0.0D, movement.y, 0.0D);
        clearInput(player.input);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (active == null || !(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        if (!active.playerId.equals(player.getUUID())) {
            stop();
            return;
        }
        forceInput(event.getInput());
        applyMovement(player, active);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active == null) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !active.playerId.equals(player.getUUID())) {
            stop();
            return;
        }
        double remaining = remainingDistance(player, active);
        if (remaining <= TARGET_EPSILON) {
            stop();
            return;
        }
        forceInput(player.input);
        applyMovement(player, active);
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stop();
    }

    private static void applyMovement(LocalPlayer player, WalkState state) {
        Direction direction = state.direction;
        float yaw = direction.toYRot();
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        player.setSprinting(false);
        player.noPhysics = true;
        player.setNoGravity(true);
        player.fallDistance = 0.0F;
        double speed = Math.min(SPEED, Math.max(0.0D, remainingDistance(player, state)));
        player.setDeltaMovement(
                direction.getStepX() * speed,
                0.0D,
                direction.getStepZ() * speed
        );
    }

    private static double remainingDistance(LocalPlayer player, WalkState state) {
        return state.direction.getStepX() * (state.targetX - player.getX())
                + state.direction.getStepZ() * (state.targetZ - player.getZ());
    }

    private static void forceInput(Input input) {
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 1.0F;
        input.up = true;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    private static void clearInput(Input input) {
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    private record WalkState(
            java.util.UUID playerId,
            Direction direction,
            double targetX,
            double targetZ,
            boolean previousNoPhysics,
            boolean previousNoGravity
    ) {
    }

    private ClientFogGateTraversal() {
    }
}

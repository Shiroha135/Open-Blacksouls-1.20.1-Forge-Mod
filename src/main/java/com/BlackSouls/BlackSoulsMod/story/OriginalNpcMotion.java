package com.BlackSouls.BlackSoulsMod.story;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Set;

public final class OriginalNpcMotion {
    private static final double WORLD_LIMIT = 30000000.0D;
    private static final Set<String> SNAPSHOT_FIELDS = Set.of("version", "jump", "startX", "startY", "startZ",
            "targetX", "targetY", "targetZ", "moveSpeed", "sourceOffsetX", "sourceOffsetY", "elapsedFrames",
            "jumpPeak", "jumpCount", "x", "z", "complete");
    private final double startX;
    private final double startY;
    private final double startZ;
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final int moveSpeed;
    private final boolean jump;
    private final int sourceOffsetX;
    private final int sourceOffsetY;
    private final int jumpPeak;
    private final long totalFrames;
    private double x;
    private double z;
    private long elapsedFrames;
    private int jumpCount;

    private OriginalNpcMotion(double x, double y, double z, double targetX, double targetY, double targetZ,
                              int moveSpeed, boolean jump, int sourceOffsetX, int sourceOffsetY) {
        position(x);
        position(y);
        position(z);
        position(targetX);
        position(targetY);
        position(targetZ);
        require(moveSpeed >= 1 && moveSpeed <= (jump ? 6 : 7), "move_speed");
        require(Math.abs((long) sourceOffsetX) <= 1000000L && Math.abs((long) sourceOffsetY) <= 1000000L,
                "source_offset");
        this.startX = this.x = x;
        this.startY = y;
        this.startZ = this.z = z;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.moveSpeed = moveSpeed;
        this.jump = jump;
        this.sourceOffsetX = sourceOffsetX;
        this.sourceOffsetY = sourceOffsetY;
        if (jump) {
            double sourceDistance = Math.hypot(sourceOffsetX, sourceOffsetY);
            double targetDistance = Math.hypot(targetX - x, targetZ - z);
            require(close(targetDistance, sourceDistance, x, z, targetX, targetZ, 1L), "jump_target_distance");
            jumpPeak = 10 + (int) Math.round(sourceDistance) - moveSpeed;
            jumpCount = Math.multiplyExact(jumpPeak, 2);
            totalFrames = jumpCount;
        } else {
            require(sourceOffsetX == 0 && sourceOffsetY == 0, "walk_offset");
            require(x != targetX || z != targetZ || y == targetY, "vertical_walk");
            jumpPeak = 0;
            jumpCount = 0;
            totalFrames = (long) Math.ceil(Math.max(Math.abs(targetX - x), Math.abs(targetZ - z)) / distancePerFrame());
        }
    }

    public static OriginalNpcMotion walk(double x, double y, double z, double targetX, double targetY,
                                         double targetZ, int moveSpeed) {
        return new OriginalNpcMotion(x, y, z, targetX, targetY, targetZ, moveSpeed, false, 0, 0);
    }

    public static OriginalNpcMotion jump(double x, double y, double z, double targetX, double targetY,
                                         double targetZ, int moveSpeed, int sourceOffsetX, int sourceOffsetY) {
        return new OriginalNpcMotion(x, y, z, targetX, targetY, targetZ, moveSpeed, true, sourceOffsetX, sourceOffsetY);
    }

    public void advanceFrame() {
        if (complete()) return;
        elapsedFrames = Math.addExact(elapsedFrames, 1L);
        if (jump) {
            jumpCount--;
            x = (x * jumpCount + targetX) / (jumpCount + 1.0D);
            z = (z * jumpCount + targetZ) / (jumpCount + 1.0D);
            if (jumpCount == 0) {
                x = targetX;
                z = targetZ;
            }
        } else {
            x = approach(x, targetX, distancePerFrame());
            z = approach(z, targetZ, distancePerFrame());
        }
    }

    public double x() {
        return x;
    }

    public double y() {
        double progress;
        if (jump) {
            progress = elapsedFrames / (double) totalFrames;
        } else {
            double distance = Math.max(Math.abs(targetX - startX), Math.abs(targetZ - startZ));
            double remaining = Math.max(Math.abs(targetX - x), Math.abs(targetZ - z));
            progress = distance == 0.0D ? 1.0D : Math.max(0.0D, Math.min(1.0D, 1.0D - remaining / distance));
        }
        double ground = startY + (targetY - startY) * progress;
        long offset = Math.abs((long) jumpCount - jumpPeak);
        long pixels = jump ? ((long) jumpPeak * jumpPeak - offset * offset) / 2L : 0L;
        return ground + pixels / 32.0D;
    }

    public double z() {
        return z;
    }

    public boolean complete() {
        return jump ? jumpCount == 0 : x == targetX && z == targetZ;
    }

    public JsonObject snapshot() {
        JsonObject value = new JsonObject();
        value.addProperty("version", 1);
        value.addProperty("jump", jump);
        value.addProperty("startX", startX);
        value.addProperty("startY", startY);
        value.addProperty("startZ", startZ);
        value.addProperty("targetX", targetX);
        value.addProperty("targetY", targetY);
        value.addProperty("targetZ", targetZ);
        value.addProperty("moveSpeed", moveSpeed);
        value.addProperty("sourceOffsetX", sourceOffsetX);
        value.addProperty("sourceOffsetY", sourceOffsetY);
        value.addProperty("elapsedFrames", elapsedFrames);
        value.addProperty("jumpPeak", jumpPeak);
        value.addProperty("jumpCount", jumpCount);
        value.addProperty("x", x);
        value.addProperty("z", z);
        value.addProperty("complete", complete());
        return value;
    }

    public static OriginalNpcMotion restore(JsonObject saved) {
        require(saved != null && saved.keySet().equals(SNAPSHOT_FIELDS), "snapshot_fields");
        require(integer(saved.get("version")) == 1, "snapshot_version");
        boolean jump = bool(saved.get("jump"));
        OriginalNpcMotion motion = new OriginalNpcMotion(number(saved.get("startX")), number(saved.get("startY")),
                number(saved.get("startZ")), number(saved.get("targetX")), number(saved.get("targetY")),
                number(saved.get("targetZ")), integer(saved.get("moveSpeed")), jump,
                integer(saved.get("sourceOffsetX")), integer(saved.get("sourceOffsetY")));
        motion.elapsedFrames = whole(saved.get("elapsedFrames"));
        require(motion.elapsedFrames >= 0L && motion.elapsedFrames <= motion.totalFrames, "snapshot_elapsed");
        require(integer(saved.get("jumpPeak")) == motion.jumpPeak, "snapshot_jump_peak");
        motion.jumpCount = integer(saved.get("jumpCount"));
        require(motion.jumpCount == (jump ? motion.totalFrames - motion.elapsedFrames : 0L), "snapshot_jump_count");
        motion.x = number(saved.get("x"));
        motion.z = number(saved.get("z"));
        position(motion.x);
        position(motion.z);
        double expectedX;
        double expectedZ;
        if (jump) {
            double progress = motion.elapsedFrames / (double) motion.totalFrames;
            expectedX = motion.startX + (motion.targetX - motion.startX) * progress;
            expectedZ = motion.startZ + (motion.targetZ - motion.startZ) * progress;
        } else {
            expectedX = approach(motion.startX, motion.targetX, motion.distancePerFrame() * motion.elapsedFrames);
            expectedZ = approach(motion.startZ, motion.targetZ, motion.distancePerFrame() * motion.elapsedFrames);
        }
        long precisionFrames = jump ? Math.max(1L, motion.elapsedFrames) : 1L;
        require(close(motion.x, expectedX, motion.startX, motion.targetX, motion.startZ, motion.targetZ, precisionFrames)
                && close(motion.z, expectedZ, motion.startX, motion.targetX, motion.startZ, motion.targetZ, precisionFrames), "snapshot_position");
        require(inRange(motion.x, motion.startX, motion.targetX, precisionFrames)
                && inRange(motion.z, motion.startZ, motion.targetZ, precisionFrames), "snapshot_bounds");
        boolean complete = bool(saved.get("complete"));
        require(complete == motion.complete() && complete == (motion.elapsedFrames == motion.totalFrames), "snapshot_complete");
        require(!complete || motion.x == motion.targetX && motion.z == motion.targetZ, "snapshot_landing");
        require(motion.elapsedFrames != 0L || motion.x == motion.startX && motion.z == motion.startZ, "snapshot_origin");
        return motion;
    }

    private double distancePerFrame() {
        return Math.scalb(1.0D, moveSpeed) / 256.0D;
    }

    private static double approach(double value, double target, double distance) {
        return target < value ? Math.max(value - distance, target) : target > value ? Math.min(value + distance, target) : value;
    }

    private static boolean inRange(double value, double first, double second, long frames) {
        double minimum = Math.min(first, second), maximum = Math.max(first, second);
        return (value >= minimum || close(value, minimum, first, second, first, second, frames))
                && (value <= maximum || close(value, maximum, first, second, first, second, frames));
    }

    private static boolean close(double actual, double expected, double first, double second, double third,
                                 double fourth, long frames) {
        double magnitude = Math.max(Math.max(Math.abs(first), Math.abs(second)), Math.max(Math.abs(third), Math.abs(fourth)));
        double tolerance = Math.max(1.0E-10D, Math.ulp(magnitude) * 16.0D * frames);
        return Math.abs(actual - expected) <= tolerance;
    }

    private static double number(JsonElement value) {
        require(value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), "number");
        double number = value.getAsDouble();
        require(Double.isFinite(number), "finite_number");
        return number;
    }

    private static long whole(JsonElement value) {
        require(value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), "integer");
        try {
            return value.getAsBigDecimal().longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException("original_motion_integer", exception);
        }
    }

    private static int integer(JsonElement value) {
        return Math.toIntExact(whole(value));
    }

    private static boolean bool(JsonElement value) {
        require(value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean(), "boolean");
        return value.getAsBoolean();
    }

    private static void position(double value) {
        require(Double.isFinite(value) && Math.abs(value) <= WORLD_LIMIT, "world_position");
    }

    private static void require(boolean valid, String reason) {
        if (!valid) throw new IllegalArgumentException("original_motion_" + reason);
    }
}

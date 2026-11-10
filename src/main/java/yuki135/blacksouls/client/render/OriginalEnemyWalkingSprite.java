package yuki135.blacksouls.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import yuki135.blacksouls.entity.EntityCorpseEatingRabbit;
import yuki135.blacksouls.entity.EntityOriginalDatabaseEnemy;
import yuki135.blacksouls.entity.EntityOriginalTurnBattleEnemy;
import yuki135.blacksouls.entity.EntityHellPrince;
import yuki135.blacksouls.util.OriginalEnemyWalkData;

public final class OriginalEnemyWalkingSprite {
    private static final int[] CYCLE = {1, 2, 1, 0};
    private static final java.util.Map<LivingEntity, Walk> WALKING = new java.util.WeakHashMap<>();
    private static final class Walk {
        int tick;
        double x;
        double z;
        double distance;
        double traveled;
        Walk(LivingEntity entity) { tick = entity.tickCount; x = entity.getX(); z = entity.getZ(); }
    }
    private OriginalEnemyWalkingSprite() { }

    public static OriginalEnemyWalkData.Sprite resolve(Monster entity) {
        if (entity instanceof EntityOriginalDatabaseEnemy original) return OriginalEnemyWalkData.get(original.getProfileId(), original.getWalkingMapId());
        if (entity instanceof EntityCorpseEatingRabbit) return OriginalEnemyWalkData.get(6, 0);
        if (entity instanceof EntityHellPrince) return OriginalEnemyWalkData.get(554, 0);
        if (entity instanceof EntityOriginalTurnBattleEnemy original) {
            int id = switch (original.getProfile()) {
                case HEADLESS_UNDEAD -> 1;
                case CORRUPT_DOG -> 11;
                case WEREWOLF -> original.isHeadSearching() ? 7 : 5;
            };
            return OriginalEnemyWalkData.get(id, 0);
        }
        return null;
    }

    public static int pattern(LivingEntity entity, OriginalEnemyWalkData.Sprite sprite, float partialTicks) {
        if (!sprite.walkAnime() && !sprite.stepAnime()) return sprite.pattern();
        var walk = WALKING.computeIfAbsent(entity, Walk::new);
        if (walk.tick != entity.tickCount) {
            walk.distance = Math.hypot(entity.getX() - walk.x, entity.getZ() - walk.z);
            if (walk.distance > 2D) walk.distance = 0;
            walk.traveled += walk.distance;
            walk.tick = entity.tickCount;
            walk.x = entity.getX();
            walk.z = entity.getZ();
        }
        if (walk.distance < 0.00001D) return sprite.stepAnime() ? CYCLE[(entity.tickCount / 5) % 4] : 1;
        return CYCLE[Math.floorMod((int) Math.floor((walk.traveled + walk.distance * partialTicks) / 0.25D), 4)];
    }

    public static void render(LivingEntity entity, OriginalEnemyWalkData.Sprite sprite, float partialTicks,
                              Vec3 camera, Quaternionf rotation, PoseStack poses, MultiBufferSource buffers, int light) {
        if (entity.isInvisible()) return;
        float yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        int direction = OriginalCharacterSprite.facingRow(yaw, entity.getPosition(partialTicks), camera);
        if (sprite.directionFix()) direction = sprite.direction() == 4 ? 1 : sprite.direction() == 6 ? 2 : sprite.direction() == 8 ? 3 : 0;
        if ((sprite.framesMask() >> (direction * 3) & 7) == 0) {
            direction = sprite.direction() == 4 ? 1 : sprite.direction() == 6 ? 2 : sprite.direction() == 8 ? 3 : 0;
            if ((sprite.framesMask() >> (direction * 3) & 7) == 0) direction = Integer.numberOfTrailingZeros(sprite.framesMask()) / 3;
        }
        int character = sprite.columns() == 3 ? 0 : sprite.characterIndex();
        int pattern = pattern(entity, sprite, partialTicks);
        int frames = sprite.framesMask() >> (direction * 3) & 7;
        if ((frames & 1 << pattern) == 0) pattern = Integer.numberOfTrailingZeros(frames);
        int column = character % 4 * 3 + pattern;
        int row = character / 4 * 4 + direction;
        float u0 = column * sprite.frameWidth() / (float) sprite.textureWidth();
        float u1 = (column + 1) * sprite.frameWidth() / (float) sprite.textureWidth();
        float v0 = row * sprite.frameHeight() / (float) sprite.textureHeight();
        float v1 = (row + 1) * sprite.frameHeight() / (float) sprite.textureHeight();
        float height = sprite.frameHeight() / 32.0F * 1.9F;
        float halfWidth = sprite.frameWidth() / 32.0F * 0.95F;
        poses.pushPose();
        OriginalCharacterSprite.faceCamera(poses, rotation);
        var output = buffers.getBuffer(RenderType.entityCutoutNoCull(sprite.texture()));
        var pose = poses.last();
        float[] xs = {-halfWidth, halfWidth, halfWidth, -halfWidth};
        float[] ys = {0, 0, height, height};
        float[] us = {u0, u1, u1, u0};
        float[] vs = {v1, v1, v0, v0};
        for (int index = 0; index < 4; index++) output.vertex(pose.pose(), xs[index], ys[index], 0)
                .color(255, 255, 255, 255).uv(us[index], vs[index]).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(pose.normal(), 0, 0, 1).endVertex();
        poses.popPose();
    }
}

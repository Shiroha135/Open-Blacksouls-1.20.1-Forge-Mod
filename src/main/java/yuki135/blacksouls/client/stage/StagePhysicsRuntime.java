package yuki135.blacksouls.client.stage;

import yuki135.blacksouls.BlackSouls;
import yuki135.mmdskin.NativeFunc;
import yuki135.mmdskin.config.ModelConfigManager;
import yuki135.mmdskin.model.runtime.ManagedModel;
import yuki135.mmdskin.render.entity.CustomEntityModelOffsetProvider;
import yuki135.mmdskin.render.entity.CustomEntityPoseProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StagePhysicsRuntime {
    public static final int MAX_BOXES = 192;
    private static final Map<Long, Scene> SCENES = new HashMap<>();
    private static Boolean supported;

    private static final class Scene {
        private Level level;
        private BlockPos scanCenter;
        private long scanTick = Long.MIN_VALUE;
        private List<AABB> boxes = List.of();
        private Vec3 lastRoot;
        private float lastYaw;
        private float lastScale;
        private boolean active;
        private boolean dirty;
    }

    public static boolean available() {
        if (supported == null) {
            try {
                NativeFunc.GetInst();
                supported = StageNativeBridge.GetApiVersion() == 1;
            } catch (LinkageError error) {
                supported = false;
                BlackSouls.LOGGER.warn("Stage scene physics requires the BlackSouls stage native runtime", error);
            }
        }
        return supported;
    }

    public static boolean ownsRoot(long handle, Entity entity) {
        Scene scene = SCENES.get(handle);
        return entity instanceof CustomEntityPoseProvider && scene != null && scene.active;
    }

    public static void release(long handle) {
        SCENES.remove(handle);
    }

    public static Vec3 root(Entity entity, float partialTick) {
        Vec3 origin = new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()));
        if (entity instanceof CustomEntityModelOffsetProvider provider) origin = origin.add(provider.getMmdModelOffset());
        return entity instanceof yuki135.mmdskin.render.entity.CustomEntityRenderStateProvider provider
                ? origin.add(provider.getMmdPlacementOffset()) : origin;
    }

    public static void prepare(Entity entity, ManagedModel model, float yawDegrees, float partialTick) {
        if (!(entity instanceof CustomEntityPoseProvider) || !available()) return;
        long handle = model.modelInstance().getModelHandle();
        Scene scene = SCENES.computeIfAbsent(handle, ignored -> new Scene());
        Vec3 root = root(entity, partialTick);
        float scale = 0.09F * model.renderProperties().modelScale() * ModelConfigManager.getConfig(model.modelName()).modelScale;
        if (entity instanceof LivingEntity living && living.isBaby()) scale *= 0.5F;
        if (!Float.isFinite(scale) || scale < 0.001F || scale > 10.0F) return;
        float yaw = yawDegrees * Mth.DEG_TO_RAD;
        BlockPos center = BlockPos.containing(root);
        long tick = entity.level().getGameTime();
        if (scene.level != entity.level() || !center.equals(scene.scanCenter) || scene.scanTick == Long.MIN_VALUE
                || tick - scene.scanTick >= 5 || tick < scene.scanTick) {
            scene.boxes = collectBoxes(entity, root);
            scene.level = entity.level();
            scene.scanCenter = center;
            scene.scanTick = tick;
            scene.dirty = true;
        }
        boolean reset = scene.lastRoot == null || scene.lastRoot.distanceToSqr(root) > 4.0D || scene.lastScale != scale;
        if (!scene.dirty && !reset && root.equals(scene.lastRoot) && yaw == scene.lastYaw) return;
        float[] boxes = relativeBoxes(scene.boxes, root);
        scene.active = StageNativeBridge.SyncScene(handle, root.x, root.y, root.z, yaw, scale, boxes, boxes.length / 6, reset);
        if (scene.active) {
            scene.lastRoot = root;
            scene.lastYaw = yaw;
            scene.lastScale = scale;
            scene.dirty = false;
        }
    }

    public static List<AABB> collectBoxes(Entity entity, Vec3 root) {
        Level level = entity.level();
        AABB bounds = new AABB(root.add(-1.75D, -1.75D, -1.75D), root.add(1.75D, 2.75D, 1.75D));
        List<AABB> boxes = new ArrayList<>();
        CollisionContext context = CollisionContext.of(entity);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) {
            if (!level.hasChunkAt(pos)) continue;
            var state = level.getBlockState(pos);
            if (state.isAir()) continue;
            for (AABB box : state.getCollisionShape(level, pos, context).toAabbs()) {
                AABB world = box.move(pos);
                if (world.intersects(bounds) && world.getXsize() > 0.00001D && world.getYsize() > 0.00001D && world.getZsize() > 0.00001D) {
                    boxes.add(world);
                }
            }
        }
        boxes.sort(Comparator.comparingDouble(box -> box.getCenter().distanceToSqr(root)));
        return List.copyOf(boxes.subList(0, Math.min(MAX_BOXES, boxes.size())));
    }

    public static float[] relativeBoxes(List<AABB> boxes, Vec3 root) {
        int count = Math.min(MAX_BOXES, boxes.size());
        float[] result = new float[count * 6];
        for (int i = 0; i < count; i++) {
            AABB box = boxes.get(i);
            int index = i * 6;
            result[index] = (float) (box.minX - root.x);
            result[index + 1] = (float) (box.minY - root.y);
            result[index + 2] = (float) (box.minZ - root.z);
            result[index + 3] = (float) (box.maxX - root.x);
            result[index + 4] = (float) (box.maxY - root.y);
            result[index + 5] = (float) (box.maxZ - root.z);
        }
        return result;
    }

    private StagePhysicsRuntime() {
    }
}

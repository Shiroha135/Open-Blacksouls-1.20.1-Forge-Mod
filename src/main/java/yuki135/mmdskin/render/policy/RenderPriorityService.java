package yuki135.mmdskin.render.policy;

import yuki135.mmdskin.config.UIConstants;
import yuki135.mmdskin.render.pipeline.RenderPerformanceProfiler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** 文件职责：基于可见性与距离预算决定渲染更新优先级。 */
public final class RenderPriorityService {
    private static final RenderPriorityService INSTANCE = new RenderPriorityService();
    private final RenderPerformanceConfig config = ConfigManagerRenderPerformanceConfig.get();

    private final ConcurrentMap<Long, Long> lastAnimationUpdateFrameByModel = new ConcurrentHashMap<>();
    private final Set<UUID> prioritizedVisibleEntities = new HashSet<>();
    private final Set<UUID> prioritizedPhysicsEntities = new HashSet<>();

    private long currentFrameKey = Long.MIN_VALUE;
    private long currentFrameIndex = 0L;
    private int visibleModelsThisFrame = 0;
    private int physicsModelsThisFrame = 0;

    private RenderPriorityService() {
    }

    public static RenderPriorityService get() {
        return INSTANCE;
    }

    public synchronized void beginWorldFrame() {
        long nextFrameKey = computeFrameKey();
        if (nextFrameKey == currentFrameKey) {
            return;
        }

        if (currentFrameKey != Long.MIN_VALUE) {
            RenderPerformanceProfiler.get().completeFrame(visibleModelsThisFrame, physicsModelsThisFrame);
        }

        currentFrameKey = nextFrameKey;
        currentFrameIndex++;
        rebuildPrioritySets();
    }

    public boolean shouldUpdateAnimation(long modelHandle, double distanceSq, boolean localPlayer) {
        if (localPlayer) {
            lastAnimationUpdateFrameByModel.put(modelHandle, currentFrameIndex);
            return true;
        }

        int updateInterval = resolveAnimationUpdateInterval(distanceSq);
        if (updateInterval <= 1) {
            lastAnimationUpdateFrameByModel.put(modelHandle, currentFrameIndex);
            return true;
        }

        Long lastFrame = lastAnimationUpdateFrameByModel.get(modelHandle);
        if (lastFrame == null || currentFrameIndex - lastFrame >= updateInterval) {
            lastAnimationUpdateFrameByModel.put(modelHandle, currentFrameIndex);
            return true;
        }

        return false;
    }

    public synchronized boolean shouldEnablePhysics(Entity entity, boolean localPlayer) {
        beginWorldFrame();
        if (!config.isPhysicsEnabled()) {
            return false;
        }
        if (localPlayer) {
            return true;
        }
        return entity != null && prioritizedPhysicsEntities.contains(entity.getUUID());
    }

    public double distanceSqToCamera(Entity entity, boolean localPlayer) {
        if (entity == null || localPlayer) {
            return 0.0d;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Entity cameraEntity = minecraft.getCameraEntity();
        if (cameraEntity == null) {
            return 0.0d;
        }

        return entity.distanceToSqr(cameraEntity);
    }

    private int resolveAnimationUpdateInterval(double distanceSq) {
        double mediumDistance = config.getAnimationLodMediumDistance();
        double farDistance = config.getAnimationLodFarDistance();
        double mediumSq = mediumDistance * mediumDistance;
        double farSq = farDistance * farDistance;

        if (distanceSq <= mediumSq) {
            return 1;
        }
        if (distanceSq <= farSq) {
            return config.getAnimationLodMediumUpdateInterval();
        }
        return config.getAnimationLodFarUpdateInterval();
    }

    private long computeFrameKey() {
        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level != null ? minecraft.level.getGameTime() : 0L;
        long frameTimeBits = Float.floatToRawIntBits(minecraft.getFrameTime()) & 0xffffffffL;
        return (gameTime << 32) ^ frameTimeBits;
    }

    private void rebuildPrioritySets() {
        prioritizedVisibleEntities.clear();
        prioritizedPhysicsEntities.clear();
        visibleModelsThisFrame = 0;
        physicsModelsThisFrame = 0;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        List<PrioritizedEntity> candidates = new ArrayList<>();

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof yuki135.mmdskin.render.entity.CustomEntityPoseProvider
                    && !entity.isInvisible() && entity.isAlive()) {
                candidates.add(new PrioritizedEntity(entity, distanceSqToCamera(entity, false), false));
            }
        }

        candidates.sort(Comparator
                .comparing(PrioritizedEntity::localPlayer).reversed()
                .thenComparingDouble(PrioritizedEntity::distanceSq));

        int visibleCap = config.getMaxVisibleModelsPerFrame();
        int physicsCap = config.getMaxPhysicsModelsPerFrame();
        double physicsDistance = config.getPhysicsLodMaxDistance();
        double physicsDistanceSq = physicsDistance * physicsDistance;

        for (PrioritizedEntity candidate : candidates) {
            Entity entity = candidate.entity();
            UUID uuid = entity.getUUID();
            if (candidate.localPlayer() || visibleCap <= 0 || prioritizedVisibleEntities.size() < visibleCap) {
                prioritizedVisibleEntities.add(uuid);
            }

            if (!config.isPhysicsEnabled()) {
                continue;
            }
            if (candidate.localPlayer()) {
                prioritizedPhysicsEntities.add(uuid);
                continue;
            }
            if (physicsDistance > 0.0d && candidate.distanceSq() > physicsDistanceSq) {
                continue;
            }
            if (physicsCap <= 0 || prioritizedPhysicsEntities.size() < physicsCap) {
                prioritizedPhysicsEntities.add(uuid);
            }
        }

        visibleModelsThisFrame = prioritizedVisibleEntities.size();
        physicsModelsThisFrame = prioritizedPhysicsEntities.size();
    }

    private record PrioritizedEntity(Entity entity, double distanceSq, boolean localPlayer) {
    }
}

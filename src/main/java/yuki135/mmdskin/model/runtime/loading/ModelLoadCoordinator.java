package yuki135.mmdskin.model.runtime.loading;

import yuki135.mmdskin.asset.catalog.ModelCatalogEntry;
import yuki135.mmdskin.model.port.ModelRuntimeAccessPort;
import yuki135.mmdskin.model.runtime.ManagedModel;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 文件职责：协调模型异步加载、失败退避与预解码纹理预热。 */
public final class ModelLoadCoordinator {

    private static final Logger logger = LogManager.getLogger();

    private static final long FAILED_RETRY_INTERVAL_MS = 10_000L;
    private static final long COMPLETED_IDLE_TTL_MS = 60_000L;

    private final ModelRuntimeAccessPort runtimeAccessPort;

    public static final class AsyncLoadResult {
        public final long modelHandle;
        public final ModelCatalogEntry modelInfo;
        public final String modelName;
        private final AtomicBoolean nativeHandleConsumed = new AtomicBoolean();

        public boolean consumeNativeHandle() {
            return nativeHandleConsumed.compareAndSet(false, true);
        }

        AsyncLoadResult(long modelHandle, ModelCatalogEntry modelInfo, String modelName) {
            this.modelHandle = modelHandle;
            this.modelInfo = modelInfo;
            this.modelName = modelName;
        }
    }

    private static final class PendingLoad {
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean completionClaimed = new AtomicBoolean();
        private FutureTask<AsyncLoadResult> future;
        private AsyncLoadResult readyResult;
        private long completedAt = -1L;

        synchronized boolean publish(AsyncLoadResult result) {
            if (cancelled.get()) return false;
            readyResult = result;
            return true;
        }

        synchronized AsyncLoadResult takeReadyResult() {
            AsyncLoadResult result = readyResult;
            readyResult = null;
            return result;
        }

        synchronized AsyncLoadResult claimReadyResult() {
            return cancelled.get() ? null : takeReadyResult();
        }

        synchronized void markCompleted(long now) {
            completedAt = now;
        }

        synchronized boolean expired(long now) {
            return completedAt >= 0L && now >= completedAt && now - completedAt >= COMPLETED_IDLE_TTL_MS;
        }

        Future<AsyncLoadResult> future() {
            return future;
        }

        void attach(FutureTask<AsyncLoadResult> future) {
            this.future = future;
        }

        boolean claimCompletion() {
            return completionClaimed.compareAndSet(false, true);
        }

        boolean isCancelled() {
            return cancelled.get();
        }

        synchronized void cancel() {
            cancelled.set(true);
            FutureTask<AsyncLoadResult> currentFuture = future;
            if (currentFuture != null) {
                currentFuture.cancel(true);
            }
        }
    }

    private final ExecutorService loadingExecutor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "MMD-ModelLoader");
        thread.setDaemon(true);
        return thread;
    });

    private final ConcurrentHashMap<String, PendingLoad> pendingLoads = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> failedLoads = new ConcurrentHashMap<>();
    private final Set<String> missingModels = ConcurrentHashMap.newKeySet();

    public ModelLoadCoordinator(ModelRuntimeAccessPort runtimeAccessPort) {
        if (runtimeAccessPort == null) {
            throw new IllegalArgumentException("runtimeAccessPort cannot be null");
        }
        this.runtimeAccessPort = runtimeAccessPort;
    }

    public ManagedModel resolveOrQueue(String fullCacheKey,
                                       String modelName,
                                       Function<AsyncLoadResult, ManagedModel> finalizer) {
        PendingLoad pendingLoad = pendingLoads.get(fullCacheKey);
        if (pendingLoad != null) {
            Future<AsyncLoadResult> future = pendingLoad.future();
            if (!future.isDone()) {
                return null;
            }

            return finalizeCompletedLoad(fullCacheKey, pendingLoad, finalizer);
        }

        Long failedTime = failedLoads.get(fullCacheKey);
        if (failedTime != null && (System.currentTimeMillis() - failedTime) < FAILED_RETRY_INTERVAL_MS) {
            return null;
        }
        failedLoads.remove(fullCacheKey);

        ModelCatalogEntry modelInfo = ModelCatalogEntry.findByFolderName(modelName);
        if (modelInfo == null) {
            if (missingModels.add(modelName)) {
                logger.warn("模型本地不存在，跳过加载: {}", modelName);
            }
            return null;
        }

        startBackgroundLoad(fullCacheKey, modelInfo, modelName);
        return null;
    }

    public boolean isPending(String fullCacheKey) {
        return pendingLoads.containsKey(fullCacheKey);
    }

    public boolean hasPendingLoads() {
        return !pendingLoads.isEmpty();
    }

    public int getPendingLoadCount() {
        return pendingLoads.size();
    }

    public void removeMatching(Predicate<String> keyMatcher, Consumer<AsyncLoadResult> resultCleaner) {
        pendingLoads.forEach((key, pendingLoad) -> {
            if (keyMatcher.test(key) && pendingLoads.remove(key, pendingLoad)) {
                cleanupPendingLoad(pendingLoad, resultCleaner);
            }
        });
        failedLoads.entrySet().removeIf(entry -> keyMatcher.test(entry.getKey()));
    }

    public void cancelAll(Consumer<AsyncLoadResult> resultCleaner) {
        if (pendingLoads.isEmpty()) {
            failedLoads.clear();
            return;
        }

        pendingLoads.forEach((key, pendingLoad) -> {
            if (pendingLoads.remove(key, pendingLoad)) {
                cleanupPendingLoad(pendingLoad, resultCleaner);
            }
        });
        failedLoads.clear();
    }

    public void cleanupExpiredCompleted(long now) {
        pendingLoads.forEach((key, pendingLoad) -> {
            if (pendingLoad.expired(now) && pendingLoads.remove(key, pendingLoad)) {
                cleanupPendingLoad(pendingLoad, this::cleanupLoadedResult);
            }
        });
    }

    private void startBackgroundLoad(String fullCacheKey, ModelCatalogEntry modelInfo, String modelName) {
        long startTime = System.currentTimeMillis();

        PendingLoad pendingLoad = new PendingLoad();
        FutureTask<AsyncLoadResult> future = new FutureTask<>(
                () -> loadModelHandle(fullCacheKey, modelInfo, modelName, startTime, pendingLoad));
        pendingLoad.attach(future);
        if (pendingLoads.putIfAbsent(fullCacheKey, pendingLoad) != null) {
            return;
        }

        logger.info("[异步加载] 开始后台加载模型 {} ({})", modelName, modelInfo.getModelFileName());
        try {
            loadingExecutor.execute(future);
        } catch (RuntimeException e) {
            pendingLoads.remove(fullCacheKey, pendingLoad);
            markFailed(fullCacheKey);
            logger.error("[异步加载] 提交后台任务失败: {}", modelName, e);
        }
    }

    private AsyncLoadResult loadModelHandle(String fullCacheKey,
                                            ModelCatalogEntry modelInfo,
                                            String modelName,
                                            long startTime,
                                            PendingLoad pendingLoad) {
        AsyncLoadResult result = null;
        boolean published = false;
        try {
            long handle = loadNativeModel(modelInfo);
            long elapsed = System.currentTimeMillis() - startTime;
            if (handle == 0) {
                logger.error("[异步加载] 后台加载失败 ({}ms): {}", elapsed, modelName);
                return null;
            }
            result = new AsyncLoadResult(handle, modelInfo, modelName);
            if (shouldDiscardLoadedHandle(fullCacheKey, pendingLoad)) return null;
            logger.info("[异步加载] 模型解析完成 ({}ms)，开始预解码纹理: {}", elapsed, modelName);
            preloadModelTextures(handle, modelInfo.getFolderPath(), pendingLoad);
            if (shouldDiscardLoadedHandle(fullCacheKey, pendingLoad)) return null;
            if (!pendingLoad.publish(result)) return null;
            published = true;
            long totalElapsed = System.currentTimeMillis() - startTime;
            logger.info("[异步加载] 后台加载全部完成 ({}ms): {}", totalElapsed, modelName);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            logger.error("[异步加载] 后台加载异常 ({}ms): {}", elapsed, modelName, e);
            return null;
        } finally {
            try {
                if (!published) cleanupLoadedResult(result);
                if (pendingLoad.isCancelled()) {
                    cleanupLoadedResult(pendingLoad.takeReadyResult());
                    clearPreloadedTextures();
                }
            } finally {
                pendingLoad.markCompleted(System.currentTimeMillis());
            }
        }
    }

    private long loadNativeModel(ModelCatalogEntry modelInfo) {
        return runtimeAccessPort.loadPmxModel(modelInfo.getModelFilePath(), modelInfo.getFolderPath(), 3);
    }

    private void preloadModelTextures(long modelHandle, String modelDir, PendingLoad pendingLoad) {
        try {
            if (pendingLoad.isCancelled()) return;
            int materialCount = runtimeAccessPort.getMaterialCount(modelHandle);
            for (int i = 0; i < materialCount; i++) {
                if (pendingLoad.isCancelled()) return;
                String texturePath = runtimeAccessPort.getMaterialTexturePath(modelHandle, i);
                if (texturePath == null || texturePath.isEmpty()) {
                    continue;
                }
                if (pendingLoad.isCancelled()) return;
                runtimeAccessPort.preloadTexture(texturePath);
            }
            if (!pendingLoad.isCancelled()) runtimeAccessPort.preloadTexture(modelDir + "/lightMap.png");
        } catch (Exception e) {
            logger.warn("[异步加载] 纹理预解码部分失败（不影响后续加载）", e);
        }
    }

    private ManagedModel finalizeCompletedLoad(String fullCacheKey,
                                               PendingLoad pendingLoad,
                                               Function<AsyncLoadResult, ManagedModel> finalizer) {
        Future<AsyncLoadResult> future = pendingLoad.future();
        if (!pendingLoad.claimCompletion()) return null;
        AsyncLoadResult claimed = null;
        try {
            AsyncLoadResult result = future.get();
            if (result == null || result.modelHandle == 0L) {
                if (!pendingLoad.isCancelled()) markFailed(fullCacheKey);
                return null;
            }
            claimed = pendingLoad.claimReadyResult();
            if (claimed == null) return null;
            ManagedModel model = finalizer.apply(claimed);
            if (model == null) {
                cleanupLoadedResult(claimed);
                claimed = null;
                if (!pendingLoad.isCancelled()) markFailed(fullCacheKey);
                return null;
            }
            claimed.consumeNativeHandle();
            claimed = null;
            if (pendingLoad.isCancelled()) {
                disposeManagedModel(model);
                return null;
            }
            return model;
        } catch (CancellationException ignored) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (!pendingLoad.isCancelled()) {
                logger.warn("获取后台加载结果时线程被中断: {}", fullCacheKey, e);
                markFailed(fullCacheKey);
            }
            return null;
        } catch (Exception e) {
            logger.error("获取后台加载结果失败: {}", fullCacheKey, e);
            if (!pendingLoad.isCancelled()) markFailed(fullCacheKey);
            return null;
        } finally {
            try {
                cleanupLoadedResult(claimed);
                cleanupLoadedResult(pendingLoad.takeReadyResult());
            } finally {
                pendingLoads.remove(fullCacheKey, pendingLoad);
            }
        }
    }

    private void cleanupPendingLoad(PendingLoad pendingLoad, Consumer<AsyncLoadResult> resultCleaner) {
        pendingLoad.cancel();
        AsyncLoadResult ready = pendingLoad.takeReadyResult();
        if (ready != null) {
            try {
                resultCleaner.accept(ready);
            } catch (Exception exception) {
                logger.warn("取消后的模型清理失败: {}", ready.modelName, exception);
                cleanupLoadedResult(ready);
            }
        }
    }

    private boolean shouldDiscardLoadedHandle(String fullCacheKey, PendingLoad pendingLoad) {
        return pendingLoad.isCancelled()
                || pendingLoads.get(fullCacheKey) != pendingLoad
                || Thread.currentThread().isInterrupted();
    }

    private void cleanupLoadedResult(AsyncLoadResult result) {
        if (result != null && result.modelHandle != 0L && result.consumeNativeHandle()) {
            try {
                runtimeAccessPort.deleteModel(result.modelHandle);
            } catch (Exception exception) {
                logger.warn("异步模型句柄清理失败: {}", result.modelName, exception);
            } finally {
                clearPreloadedTextures();
            }
        }
    }

    private void clearPreloadedTextures() {
        try {
            runtimeAccessPort.clearPreloadedTextures();
        } catch (Exception exception) {
            logger.warn("清理异步纹理预读失败", exception);
        }
    }

    private void disposeManagedModel(ManagedModel model) {
        try {
            model.dispose();
        } catch (Exception e) {
            logger.warn("[异步加载] 取消后的模型实例清理失败: {}", model.modelName(), e);
        }
    }

    private void markFailed(String fullCacheKey) {
        failedLoads.put(fullCacheKey, System.currentTimeMillis());
    }
}

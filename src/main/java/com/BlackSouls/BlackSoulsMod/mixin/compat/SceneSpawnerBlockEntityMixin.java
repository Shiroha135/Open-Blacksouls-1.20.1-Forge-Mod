package com.BlackSouls.BlackSoulsMod.mixin.compat;

import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBounds;
import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBossData;
import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBossState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.blockentity.SceneSpawnerBlockEntity", remap = false)
public abstract class SceneSpawnerBlockEntityMixin implements SceneSpawnerBounds, SceneSpawnerBossState {
    @Unique
    private static final String BLACKSOULS_RANGE_X_NBT = "BlackSoulsRangeX";
    @Unique
    private static final String BLACKSOULS_RANGE_Z_NBT = "BlackSoulsRangeZ";
    @Unique
    private static final String BLACKSOULS_BOSS_MODE_NBT = "BlackSoulsBossMode";

    @Shadow(remap = false)
    private String sceneId;

    @Shadow(remap = false)
    public abstract void resetForCurrentScene(ServerLevel level);

    @Unique
    private int blacksouls$rangeX = DEFAULT_RANGE;
    @Unique
    private int blacksouls$rangeZ = DEFAULT_RANGE;
    @Unique
    private boolean blacksouls$bossMode;

    @Override
    public int blacksouls$getRangeX() {
        return blacksouls$rangeX;
    }

    @Override
    public int blacksouls$getRangeZ() {
        return blacksouls$rangeZ;
    }

    @Override
    public void blacksouls$setBounds(int rangeX, int rangeZ) {
        int normalizedX = Math.max(1, Math.min(MAX_RANGE, rangeX));
        int normalizedZ = Math.max(1, Math.min(MAX_RANGE, rangeZ));
        if (normalizedX == blacksouls$rangeX && normalizedZ == blacksouls$rangeZ) {
            return;
        }
        blacksouls$rangeX = normalizedX;
        blacksouls$rangeZ = normalizedZ;
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        blockEntity.setChanged();
        Level level = blockEntity.getLevel();
        if (level != null) {
            level.sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(),
                    blockEntity.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean blacksouls$isBossMode() {
        return blacksouls$bossMode;
    }

    @Override
    public String blacksouls$getSceneId() {
        return sceneId;
    }

    @Override
    public void blacksouls$setBossMode(boolean bossMode) {
        if (blacksouls$bossMode == bossMode) {
            return;
        }
        blacksouls$bossMode = bossMode;
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        if (blockEntity.getLevel() instanceof ServerLevel serverLevel) {
            resetForCurrentScene(serverLevel);
        } else {
            blockEntity.setChanged();
        }
        Level level = blockEntity.getLevel();
        if (level != null) {
            level.sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(),
                    blockEntity.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Inject(method = {"load", "m_142466_"}, at = @At("TAIL"), remap = false)
    private void blacksouls$loadBounds(CompoundTag tag, CallbackInfo callback) {
        blacksouls$rangeX = tag.contains(BLACKSOULS_RANGE_X_NBT, Tag.TAG_INT)
                ? Math.max(1, Math.min(MAX_RANGE, tag.getInt(BLACKSOULS_RANGE_X_NBT)))
                : DEFAULT_RANGE;
        blacksouls$rangeZ = tag.contains(BLACKSOULS_RANGE_Z_NBT, Tag.TAG_INT)
                ? Math.max(1, Math.min(MAX_RANGE, tag.getInt(BLACKSOULS_RANGE_Z_NBT)))
                : DEFAULT_RANGE;
        blacksouls$bossMode = tag.getBoolean(BLACKSOULS_BOSS_MODE_NBT);
    }

    @Inject(method = {"saveAdditional", "m_183515_"}, at = @At("TAIL"), remap = false)
    private void blacksouls$saveBounds(CompoundTag tag, CallbackInfo callback) {
        tag.putInt(BLACKSOULS_RANGE_X_NBT, blacksouls$rangeX);
        tag.putInt(BLACKSOULS_RANGE_Z_NBT, blacksouls$rangeZ);
        tag.putBoolean(BLACKSOULS_BOSS_MODE_NBT, blacksouls$bossMode);
    }

    @Inject(method = "resetForCurrentScene", at = @At("HEAD"), remap = false)
    private void blacksouls$clearBossDefeat(ServerLevel level, CallbackInfo callback) {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        SceneSpawnerBossData.get(level.getServer()).clear(
                SceneSpawnerBossData.spawnerKey(level.dimension(), blockEntity.getBlockPos()));
    }

    @Inject(method = "configure", at = @At("HEAD"), remap = false)
    private void blacksouls$clearBossDefeatForSceneChange(String newSceneId, float yaw,
                                                           boolean enabled, CallbackInfo callback) {
        String normalized = newSceneId == null ? "" : newSceneId.trim();
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64);
        }
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        if (!sceneId.equals(normalized) && blockEntity.getLevel() instanceof ServerLevel serverLevel) {
            SceneSpawnerBossData.get(serverLevel.getServer()).clear(
                    SceneSpawnerBossData.spawnerKey(serverLevel.dimension(), blockEntity.getBlockPos()));
        }
    }
}

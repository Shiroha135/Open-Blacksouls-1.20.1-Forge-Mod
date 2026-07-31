package com.BlackSouls.BlackSoulsMod.mixin.compat;

import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.blockentity.SceneSpawnerBlockEntity", remap = false)
public abstract class SceneSpawnerBlockEntityMixin implements SceneSpawnerBounds {
    @Unique
    private static final String BLACKSOULS_RANGE_X_NBT = "BlackSoulsRangeX";
    @Unique
    private static final String BLACKSOULS_RANGE_Z_NBT = "BlackSoulsRangeZ";

    @Unique
    private int blacksouls$rangeX = DEFAULT_RANGE;
    @Unique
    private int blacksouls$rangeZ = DEFAULT_RANGE;

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

    @Inject(method = "m_142466_", at = @At("TAIL"), remap = false)
    private void blacksouls$loadBounds(CompoundTag tag, CallbackInfo callback) {
        blacksouls$rangeX = tag.contains(BLACKSOULS_RANGE_X_NBT, Tag.TAG_INT)
                ? Math.max(1, Math.min(MAX_RANGE, tag.getInt(BLACKSOULS_RANGE_X_NBT)))
                : DEFAULT_RANGE;
        blacksouls$rangeZ = tag.contains(BLACKSOULS_RANGE_Z_NBT, Tag.TAG_INT)
                ? Math.max(1, Math.min(MAX_RANGE, tag.getInt(BLACKSOULS_RANGE_Z_NBT)))
                : DEFAULT_RANGE;
    }

    @Inject(method = "m_183515_", at = @At("TAIL"), remap = false)
    private void blacksouls$saveBounds(CompoundTag tag, CallbackInfo callback) {
        tag.putInt(BLACKSOULS_RANGE_X_NBT, blacksouls$rangeX);
        tag.putInt(BLACKSOULS_RANGE_Z_NBT, blacksouls$rangeZ);
    }
}

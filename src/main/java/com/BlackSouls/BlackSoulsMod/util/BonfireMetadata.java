package com.BlackSouls.BlackSoulsMod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BonfireMetadata {
    public static final String DEFAULT_NAME = "gui.blacksouls.bonfire.unnamed";
    private static final String NAME_KEY = "BlackSoulsBonfireName";
    private static final String DESCRIPTION_KEY = "BlackSoulsBonfireDescription";

    public static Data read(Level level, BlockPos pos) {
        if (HokoniwaDestination.isHokoniwa(level.dimension())
                && pos.equals(HokoniwaDestination.FIRST_BONFIRE_POS)) {
            return new Data(
                    "gui.blacksouls.bonfire.library_name",
                    "gui.blacksouls.bonfire.library_desc"
            );
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return new Data(DEFAULT_NAME, "");
        }
        CompoundTag persistentData = blockEntity.getPersistentData();
        String name = persistentData.getString(NAME_KEY);
        String description = persistentData.getString(DESCRIPTION_KEY);
        return new Data(name.isBlank() ? DEFAULT_NAME : name, description);
    }

    public static boolean write(Level level, BlockPos pos, String name, String description) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return false;
        }
        CompoundTag persistentData = blockEntity.getPersistentData();
        persistentData.putString(NAME_KEY, name.isBlank() ? DEFAULT_NAME : name);
        persistentData.putString(DESCRIPTION_KEY, description);
        blockEntity.setChanged();
        if (!level.isClientSide()) {
            level.getChunkAt(pos).setUnsaved(true);
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    public static boolean isSupported(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) != null;
    }

    public record Data(String name, String description) {
    }

    private BonfireMetadata() {
    }
}

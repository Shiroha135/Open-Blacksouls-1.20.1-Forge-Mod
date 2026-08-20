package com.BlackSouls.BlackSoulsMod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

@SuppressWarnings("removal")
public final class LibraryDestination {
    public static final ResourceLocation ID = new ResourceLocation("blacksouls", "library");
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ID);
    public static final double X = -48.5D;
    public static final double Y = -43.0D;
    public static final double Z = 33.5D;
    public static final float YAW = 0.0F;
    public static final double STORY_START_X = 59.5D;
    public static final double STORY_START_Y = -16.0D;
    public static final double STORY_START_Z = 157.5D;
    public static final BlockPos FIRST_BONFIRE_POS = new BlockPos(-49, -43, 47);

    public static boolean isLandingSafe(ServerLevel level) {
        BlockPos feet = BlockPos.containing(X, Y, Z);
        BlockPos floor = feet.below();
        return !level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    private LibraryDestination() {
    }
}

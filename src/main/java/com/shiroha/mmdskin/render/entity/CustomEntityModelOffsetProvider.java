package com.shiroha.mmdskin.render.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public interface CustomEntityModelOffsetProvider {
    double MAX_OFFSET = 4.0D;

    Vec3 getMmdModelOffset();

    void setMmdModelOffset(Vec3 offset);

    static boolean isValidOffset(Vec3 offset) {
        return offset != null && Double.isFinite(offset.x) && Double.isFinite(offset.y) && Double.isFinite(offset.z)
                && Math.abs(offset.x) <= MAX_OFFSET && Math.abs(offset.y) <= MAX_OFFSET && Math.abs(offset.z) <= MAX_OFFSET;
    }

    default void saveMmdModelOffset(CompoundTag tag) {
        Vec3 offset = getMmdModelOffset();
        CompoundTag value = new CompoundTag();
        value.putDouble("X", offset.x);
        value.putDouble("Y", offset.y);
        value.putDouble("Z", offset.z);
        tag.put("MmdModelOffset", value);
    }

    default void loadMmdModelOffset(CompoundTag tag) {
        CompoundTag value = tag.getCompound("MmdModelOffset");
        Vec3 offset = new Vec3(value.getDouble("X"), value.getDouble("Y"), value.getDouble("Z"));
        setMmdModelOffset(isValidOffset(offset) ? offset : Vec3.ZERO);
    }
}

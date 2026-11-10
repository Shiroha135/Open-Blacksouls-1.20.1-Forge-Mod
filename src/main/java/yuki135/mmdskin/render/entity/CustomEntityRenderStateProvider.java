package yuki135.mmdskin.render.entity;

public interface CustomEntityRenderStateProvider {
    default boolean isMmdPhysicsEnabled() {
        return true;
    }

    default net.minecraft.world.phys.Vec3 getMmdPlacementOffset() {
        return net.minecraft.world.phys.Vec3.ZERO;
    }

    default float getMmdRenderOpacity() {
        return 1.0F;
    }

    default float getMmdBodyYaw(float resolvedYaw) {
        return resolvedYaw;
    }
}

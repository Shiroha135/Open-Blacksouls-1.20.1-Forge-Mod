package yuki135.mmdskin.render.entity;

public interface CustomEntityRenderStateProvider {
    default float getMmdRenderOpacity() {
        return 1.0F;
    }

    default float getMmdBodyYaw(float resolvedYaw) {
        return resolvedYaw;
    }
}

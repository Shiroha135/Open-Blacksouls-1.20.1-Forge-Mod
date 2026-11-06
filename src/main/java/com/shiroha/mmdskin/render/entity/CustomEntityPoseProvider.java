package com.shiroha.mmdskin.render.entity;

public interface CustomEntityPoseProvider extends CustomEntityModelOffsetProvider {
    boolean isSitting();

    void setSitting(boolean sitting);
}

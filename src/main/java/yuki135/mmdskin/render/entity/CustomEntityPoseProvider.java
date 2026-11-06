package yuki135.mmdskin.render.entity;

public interface CustomEntityPoseProvider extends CustomEntityModelOffsetProvider {
    boolean isSitting();

    void setSitting(boolean sitting);
}

/* 文件职责：定义模型运行时相关 native 能力边界。 */
package yuki135.mmdskin.bridge.runtime;

/** 文件职责：定义模型运行时相关的 native 能力边界。 */
public interface NativeModelPort {

    boolean setLayerBoneMask(long modelHandle, int layer, String rootBoneName);

    boolean setLayerBoneExclude(long modelHandle, int layer, String rootBoneName);

    long getModelMemoryUsage(long modelHandle);


    int getMaterialCount(long modelHandle);

    void setMaterialVisible(long modelHandle, int materialIndex, boolean visible);

    void setAllMaterialsVisible(long modelHandle, boolean visible);

    void deleteModel(long modelHandle);
}

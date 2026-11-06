package yuki135.mmdskin.model.runtime;

import java.util.Objects;
import net.minecraft.world.entity.Entity;

/** 文件职责：为模型仓储提供强类型请求键，替代旧的字符串拼接缓存键。 */
public record ModelRequestKey(ModelSubjectKind subjectKind, String subjectId, String modelName) {

    public ModelRequestKey {
        subjectKind = Objects.requireNonNull(subjectKind, "subjectKind");
        subjectId = normalize(subjectId);
        modelName = normalize(modelName);
    }

    public static ModelRequestKey mob(Entity entity, String modelName) {
        return new ModelRequestKey(ModelSubjectKind.MOB, entitySubjectId(entity), modelName);
    }

    public String cacheKey() {
        return subjectKind.name() + ":" + subjectId + ":" + modelName;
    }

    private static String entitySubjectId(Entity entity) {
        if (entity == null) {
            return "unknown";
        }
        String uuid = entity.getStringUUID();
        if (uuid != null && !uuid.isBlank()) {
            return uuid;
        }
        return entity.getName().getString();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value;
    }
}

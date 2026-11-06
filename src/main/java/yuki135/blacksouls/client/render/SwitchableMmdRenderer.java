package yuki135.blacksouls.client.render;

import yuki135.blacksouls.BSConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import yuki135.mmdskin.model.runtime.ManagedModel;
import yuki135.mmdskin.model.runtime.ModelRequestKey;
import yuki135.mmdskin.render.bootstrap.ClientRenderRuntime;
import yuki135.mmdskin.render.entity.MmdSkinRenderer;
import yuki135.mmdskin.render.entity.CustomEntityRenderStateProvider;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class SwitchableMmdRenderer<T extends Entity> extends EntityRenderer<T> {

    private final String modelName;
    private final EntityRenderer<T> mmdRenderer;
    private final EntityRenderer<T> fallbackRenderer;
    private final Map<T, Boolean> renderChoices = new WeakHashMap<>();

    public SwitchableMmdRenderer(EntityRendererProvider.Context context, String modelName, EntityRendererProvider<T> fallbackFactory) {
        this(context, modelName, fallbackFactory, 0.5F);
    }

    public SwitchableMmdRenderer(EntityRendererProvider.Context context, String modelName,
                                 EntityRendererProvider<T> fallbackFactory, float shadowRadius) {
        super(context);
        this.modelName = modelName.replace(':', '.');
        this.mmdRenderer = new MmdSkinRenderer<>(context, modelName);
        this.fallbackRenderer = fallbackFactory.create(context);
        this.shadowRadius = shadowRadius;
    }

    @Override
    public void render(T entity, float entityYaw, float tickDelta, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        float opacity = opacity(entity);
        this.shadowStrength = entity.isInvisible() ? 0.0F : opacity;
        if (entity.isInvisible() || opacity <= 0.0F) {
            renderChoices.remove(entity);
            return;
        }
        if (useMmd(entity, opacity)) {
            this.mmdRenderer.render(entity, entityYaw, tickDelta, matrixStack, buffer, packedLight);
        } else {
            this.fallbackRenderer.render(entity, entityYaw, tickDelta, matrixStack, buffer, packedLight);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        if (!entity.isInvisible() && opacity(entity) > 0.0F && useMmd(entity, opacity(entity))) {
            return this.mmdRenderer.getTextureLocation(entity);
        }
        return this.fallbackRenderer.getTextureLocation(entity);
    }

    private boolean useMmd(T entity, float opacity) {
        if (!BSConfig.ENABLE_MMD_MODELS.get()) {
            renderChoices.remove(entity);
            return false;
        }
        Boolean chosen = renderChoices.get(entity);
        if (opacity < 1.0F && chosen != null) return chosen;
        boolean ready = isMmdReady(entity);
        renderChoices.put(entity, ready);
        return ready;
    }

    private static float opacity(Entity entity) {
        float opacity = entity instanceof CustomEntityRenderStateProvider provider ? provider.getMmdRenderOpacity() : 1.0F;
        return Float.isFinite(opacity) ? Math.max(0.0F, Math.min(1.0F, opacity)) : 0.0F;
    }

    private boolean isMmdReady(T entity) {
        ManagedModel model = ClientRenderRuntime.get().modelRepository()
                .acquire(ModelRequestKey.mob(entity, this.modelName));
        return model != null && model.modelInstance() != null;
    }
}

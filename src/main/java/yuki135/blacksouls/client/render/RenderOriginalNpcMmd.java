package yuki135.blacksouls.client.render;

import yuki135.blacksouls.BSConfig;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.util.OriginalNpcMmdBindings;
import com.mojang.blaze3d.vertex.PoseStack;
import yuki135.mmdskin.asset.catalog.ModelCatalogEntry;
import yuki135.mmdskin.model.runtime.ManagedModel;
import yuki135.mmdskin.model.runtime.ModelRequestKey;
import yuki135.mmdskin.render.bootstrap.ClientRenderRuntime;
import yuki135.mmdskin.render.entity.MmdSkinRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

public final class RenderOriginalNpcMmd extends EntityRenderer<EntityOriginalNpc> {
    private final EntityRendererProvider.Context context;
    private final RenderOriginalNpc fallback;
    private final Map<String, MmdSkinRenderer<EntityOriginalNpc>> renderers = new HashMap<>();
    private final Map<EntityOriginalNpc, BindingChoice> choices = new WeakHashMap<>();

    public RenderOriginalNpcMmd(EntityRendererProvider.Context context) {
        super(context);
        this.context = context;
        fallback = new RenderOriginalNpc(context);
        shadowRadius = .3F;
    }

    @Override
    public void render(EntityOriginalNpc entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        int opacity = Math.max(0, Math.min(255, entity.getSpriteOpacity()));
        String character = entity.getRouteCharacterName();
        shadowStrength = entity.isInvisible() || character.isBlank() ? 0.0F : opacity / 255.0F;
        if (entity.isInvisible() || character.isBlank() || opacity == 0) {
            choices.remove(entity);
            return;
        }
        String model = OriginalNpcMmdBindings.modelFor(entity.getNpcId(), character, entity.getCharacterIndex());
        boolean enabled = BSConfig.ENABLE_MMD_MODELS.get();
        if (!enabled || model == null) {
            choices.remove(entity);
            fallback.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }
        BindingChoice state = choices.computeIfAbsent(entity, ignored -> new BindingChoice());
        Choice choice = state.choose(entity.getNpcId(), character, entity.getCharacterIndex(), model,
                true, true, opacity, () -> isReady(entity, model));
        if (choice == Choice.MMD) {
            renderer(model).render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        } else if (choice == Choice.FALLBACK) {
            fallback.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntityOriginalNpc entity) {
        if (entity.isInvisible() || entity.getRouteCharacterName().isBlank() || entity.getSpriteOpacity() <= 0
                || !BSConfig.ENABLE_MMD_MODELS.get()) {
            choices.remove(entity);
            return fallback.getTextureLocation(entity);
        }
        String model = OriginalNpcMmdBindings.modelFor(entity.getNpcId(), entity.getRouteCharacterName(), entity.getCharacterIndex());
        BindingChoice state = choices.get(entity);
        if (state != null) {
            if (!state.matches(entity.getNpcId(), entity.getRouteCharacterName(), entity.getCharacterIndex(), model)) choices.remove(entity);
            else if (state.choice == Choice.MMD) return renderer(model).getTextureLocation(entity);
        }
        return fallback.getTextureLocation(entity);
    }

    private MmdSkinRenderer<EntityOriginalNpc> renderer(String model) {
        return renderers.computeIfAbsent(model, key -> new MmdSkinRenderer<>(context, key));
    }

    private static boolean isReady(EntityOriginalNpc entity, String model) {
        if (ModelCatalogEntry.findByFolderName(model) == null) return false;
        ManagedModel managed = ClientRenderRuntime.get().modelRepository().acquire(ModelRequestKey.mob(entity, model));
        return managed != null && managed.modelInstance() != null;
    }

    enum Choice {
        NONE, FALLBACK, MMD
    }

    static final class BindingChoice {
        private String npcId;
        private String character;
        private int index = -1;
        private String model;
        private Choice choice = Choice.NONE;

        Choice choose(String npcId, String character, int index, String model, boolean enabled, boolean visible,
                      int opacity, BooleanSupplier ready) {
            if (!visible || character == null || character.isBlank() || opacity <= 0) {
                clear();
                return Choice.NONE;
            }
            if (!enabled || model == null || model.isBlank()) {
                clear();
                return Choice.FALLBACK;
            }
            if (opacity < 255 && matches(npcId, character, index, model)) return choice;
            this.npcId = npcId;
            this.character = character;
            this.index = index;
            this.model = model;
            choice = ready.getAsBoolean() ? Choice.MMD : Choice.FALLBACK;
            return choice;
        }

        private boolean matches(String npcId, String character, int index, String model) {
            return choice != Choice.NONE && Objects.equals(this.npcId, npcId) && Objects.equals(this.character, character)
                    && this.index == index && Objects.equals(this.model, model);
        }

        private void clear() {
            npcId = null;
            character = null;
            model = null;
            index = -1;
            choice = Choice.NONE;
        }
    }
}

package yuki135.blacksouls.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import yuki135.blacksouls.entity.EntityRabbitHoleNpc;
import yuki135.blacksouls.util.BSOriginalNpcData;
import yuki135.blacksouls.util.OriginalEnemyWalkData;
import yuki135.blacksouls.util.OriginalNpcTextures;
import java.util.EnumMap;
import java.util.Map;

public final class RenderRabbitHoleNpc extends EntityRenderer<EntityRabbitHoleNpc> {
    private static final Map<EntityRabbitHoleNpc.Role, OriginalEnemyWalkData.Sprite> SPRITES = load();

    public RenderRabbitHoleNpc(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.3F;
    }

    public static OriginalEnemyWalkData.Sprite sprite(EntityRabbitHoleNpc entity) {
        return SPRITES.get(entity.getRole());
    }

    private static Map<EntityRabbitHoleNpc.Role, OriginalEnemyWalkData.Sprite> load() {
        var result = new EnumMap<EntityRabbitHoleNpc.Role, OriginalEnemyWalkData.Sprite>(EntityRabbitHoleNpc.Role.class);
        for (var role : EntityRabbitHoleNpc.Role.values()) {
            String source = switch (role) {
                case EV009 -> "map052_ev009";
                case EV030 -> "map053_ev030";
                default -> "map051_" + role.id().toLowerCase(java.util.Locale.ROOT);
            };
            var definition = BSOriginalNpcData.get(source);
            if (definition == null) throw new IllegalStateException("Missing original rabbit source " + source);
            var page = definition.pages().get(0);
            var graphic = page.graphic();
            var sheet = OriginalNpcTextures.character(graphic.get("character_name").getAsString());
            if (sheet == null) throw new IllegalStateException("Missing original rabbit walking sheet " + source);
            boolean single = sheet.get("single").getAsBoolean();
            var movement = page.movement();
            result.put(role, new OriginalEnemyWalkData.Sprite(OriginalNpcTextures.texture(sheet),
                    sheet.get("width").getAsInt(), sheet.get("height").getAsInt(),
                    single ? 0 : graphic.get("character_index").getAsInt(), single ? 3 : 12, single ? 4 : 8,
                    sheet.get("frameWidth").getAsInt(), sheet.get("frameHeight").getAsInt(),
                    movement.has("step_anime") && movement.get("step_anime").getAsBoolean(),
                    movement.has("direction_fix") && movement.get("direction_fix").getAsBoolean(),
                    graphic.get("direction").getAsInt(), 4095,
                    !movement.has("walk_anime") || movement.get("walk_anime").getAsBoolean(),
                    Math.max(0, Math.min(2, graphic.get("pattern").getAsInt()))));
        }
        return Map.copyOf(result);
    }

    @Override
    public void render(EntityRabbitHoleNpc entity, float yaw, float partialTicks, PoseStack poses,
                       MultiBufferSource buffers, int light) {
        if (entity.isInvisible()) return;
        OriginalEnemyWalkingSprite.render(entity, sprite(entity), partialTicks, entityRenderDispatcher.camera.getPosition(),
                entityRenderDispatcher.cameraOrientation(), poses, buffers, light);
        super.render(entity, yaw, partialTicks, poses, buffers, light);
    }

    @Override
    protected void renderNameTag(EntityRabbitHoleNpc entity, Component name, PoseStack poses,
                                 MultiBufferSource buffers, int light) {
        poses.pushPose();
        poses.translate(0, sprite(entity).frameHeight() / 32.0F * 1.9F - entity.getBbHeight(), 0);
        super.renderNameTag(entity, name, poses, buffers, light);
        poses.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityRabbitHoleNpc entity) { return sprite(entity).texture(); }
}

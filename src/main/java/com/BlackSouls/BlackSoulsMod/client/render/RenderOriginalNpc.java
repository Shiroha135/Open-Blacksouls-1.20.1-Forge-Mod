package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.EntityOriginalNpc;
import com.BlackSouls.BlackSoulsMod.util.OriginalNpcTileGraphics;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class RenderOriginalNpc extends EntityRenderer<EntityOriginalNpc> {
    private static final Map<ResourceLocation, SheetSize> SHEET_SIZES = loadSheetSizes();

    public RenderOriginalNpc(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.3F;
    }

    @Override
    public void render(EntityOriginalNpc entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        var tile = OriginalNpcTileGraphics.get(entity.getNpcId(), entity.getRouteTileId());
        ResourceLocation texture = tile != null ? tile.texture() : entity.getRouteTileId() == 0 ? entity.getCharacterTexture() : null;
        int opacity = Math.max(0, Math.min(255, entity.getSpriteOpacity()));
        if (entity.isInvisible() || texture == null || opacity == 0) {
            return;
        }
        boolean single = entity.isSingleCharacterSheet();
        int columns = single ? 3 : 12;
        int rows = single ? 4 : 8;
        int character = single ? 0 : Math.max(0, Math.min(7, entity.getCharacterIndex()));
        int pattern = entity.getSpritePattern();
        pattern = pattern >= 0 && pattern < 3 ? pattern : 1;
        int direction = entity.getSpriteDirection();
        int directionRow = direction == 4 ? 1 : direction == 6 ? 2 : direction == 8 ? 3 : 0;
        int column = character % 4 * 3 + pattern;
        int row = character / 4 * 4 + directionRow;
        SheetSize size = SHEET_SIZES.getOrDefault(texture,
                single ? new SheetSize(96, 128) : new SheetSize(384, 256));
        int frameWidth = Math.max(1, size.width() / columns);
        int frameHeight = Math.max(1, size.height() / rows);
        float u0 = tile != null ? 0 : column * frameWidth / (float) size.width();
        float u1 = tile != null ? 1 : (column + 1) * frameWidth / (float) size.width();
        float v0 = tile != null ? 0 : row * frameHeight / (float) size.height();
        float v1 = tile != null ? 1 : (row + 1) * frameHeight / (float) size.height();
        boolean crowd = tile == null && OriginalPoolCrowdSprite.applies(entity.getNpcId());
        float height = tile != null ? tile.blockHeight() : crowd ? frameHeight / 32.0F : 1.9F;
        float halfWidth = tile != null ? tile.blockWidth() * 0.5F : height * frameWidth / frameHeight * 0.5F;
        float bottom = crowd ? entity.getOriginalShiftY() / 32.0F : 0.0F;
        int bushPixels = crowd ? Math.max(0, Math.min(frameHeight, entity.getOriginalBushDepth())) : 0;

        poseStack.pushPose();
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        PoseStack.Pose pose = poseStack.last();
        if (bushPixels > 0) {
            float split = bottom + bushPixels / 32.0F;
            float splitV = v1 - (v1 - v0) * bushPixels / frameHeight;
            quad(consumer, pose, packedLight, OriginalPoolCrowdSprite.bushOpacity(opacity),
                    halfWidth, bottom, split, u0, u1, splitV, v1);
            if (bushPixels < frameHeight) {
                quad(consumer, pose, packedLight, opacity, halfWidth, split, bottom + height, u0, u1, v0, splitV);
            }
        } else {
            quad(consumer, pose, packedLight, opacity, halfWidth, bottom, bottom + height, u0, u1, v0, v1);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static Map<ResourceLocation, SheetSize> loadSheetSizes() {
        Map<ResourceLocation, SheetSize> sizes = new HashMap<>();
        try (InputStream stream = RenderOriginalNpc.class.getResourceAsStream(
                "/data/blacksouls/original_npc_textures.json")) {
            if (stream == null) {
                return Map.of();
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("characters").entrySet()) {
                JsonObject sheet = entry.getValue().getAsJsonObject();
                ResourceLocation texture = ResourceLocation.tryParse(sheet.get("texture").getAsString());
                int width = sheet.get("width").getAsInt();
                int height = sheet.get("height").getAsInt();
                if (texture != null && width > 0 && height > 0) {
                    sizes.put(texture, new SheetSize(width, height));
                }
            }
        } catch (Exception exception) {
            BlackSouls.LOGGER.error("Failed to load original NPC sprite dimensions", exception);
        }
        return Map.copyOf(sizes);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, int light, int opacity,
                             float halfWidth, float bottom, float top, float u0, float u1, float v0, float v1) {
        vertex(consumer, pose.pose(), pose.normal(), light, opacity, -halfWidth, bottom, u0, v1);
        vertex(consumer, pose.pose(), pose.normal(), light, opacity, halfWidth, bottom, u1, v1);
        vertex(consumer, pose.pose(), pose.normal(), light, opacity, halfWidth, top, u1, v0);
        vertex(consumer, pose.pose(), pose.normal(), light, opacity, -halfWidth, top, u0, v0);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                               int light, int opacity, float x, float y, float u, float v) {
        consumer.vertex(pose, x, y, 0.0F)
                .color(255, 255, 255, opacity)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityOriginalNpc entity) {
        var tile = OriginalNpcTileGraphics.get(entity.getNpcId(), entity.getRouteTileId());
        ResourceLocation texture = tile != null ? tile.texture() : entity.getRouteTileId() == 0 ? entity.getCharacterTexture() : null;
        return texture == null ? MissingTextureAtlasSprite.getLocation() : texture;
    }

    private record SheetSize(int width, int height) {
    }
}

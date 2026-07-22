package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.entity.EntityMeatWall;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class RenderMeatWall extends MobRenderer<EntityMeatWall, RenderMeatWall.MeatWallModel> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(BlackSouls.MODID, "meat_wall"), "main");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BlackSouls.MODID, "textures/item/consumable/meat.png");

    public RenderMeatWall(EntityRendererProvider.Context context) {
        super(context, new MeatWallModel(context.bakeLayer(LAYER_LOCATION)), 0.8F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityMeatWall entity) {
        return TEXTURE;
    }

    public static class MeatWallModel extends EntityModel<EntityMeatWall> {
        private final ModelPart root;
        private final ModelPart body;

        public MeatWallModel(ModelPart root) {
            this.root = root;
            this.body = root.getChild("body");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            CubeListBuilder cubes = CubeListBuilder.create();
            int[][] rows = {
                    {-8, -4, 0, 4},
                    {-12, -8, -4, 0, 4, 8},
                    {-12, -8, -4, 0, 4, 8},
                    {-12, -8, -4, 0, 4, 8},
                    {-8, -4, 0, 4},
                    {-8, -4, 0, 4},
                    {-4, 0}
            };
            for (int row = 0; row < rows.length; row++) {
                int y = -4 - row * 4;
                for (int x : rows[row]) {
                    cubes.texOffs(0, 0).addBox(x, y, -4.0F, 4.0F, 4.0F, 4.0F);
                    cubes.texOffs(0, 0).addBox(x, y, 0.0F, 4.0F, 4.0F, 4.0F);
                }
            }
            cubes.texOffs(0, 0).addBox(-16.0F, -16.0F, -2.0F, 4.0F, 4.0F, 4.0F);
            cubes.texOffs(0, 0).addBox(12.0F, -20.0F, -2.0F, 4.0F, 4.0F, 4.0F);
            cubes.texOffs(0, 0).addBox(-4.0F, -32.0F, -2.0F, 4.0F, 4.0F, 4.0F);
            cubes.texOffs(0, 0).addBox(0.0F, -32.0F, -2.0F, 4.0F, 4.0F, 4.0F);
            root.addOrReplaceChild("body", cubes, PartPose.offset(0.0F, 24.0F, 0.0F));
            return LayerDefinition.create(mesh, 16, 16);
        }

        @Override
        public void setupAnim(@NotNull EntityMeatWall entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
            this.body.yRot = Mth.sin(ageInTicks * 0.05F) * 0.04F;
            this.body.zRot = Mth.sin(ageInTicks * 0.035F) * 0.025F;
        }

        @Override
        public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer,
                                   int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
            this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }
}

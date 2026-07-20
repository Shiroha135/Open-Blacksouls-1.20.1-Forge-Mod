package com.BlackSouls.BlackSoulsMod.client.render;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.capability.BSPlayerStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.BlackSouls.BlackSoulsMod.client.ClientSkillInfo;

public class BSAvatarRenderer {

    private static final int FACE_SHEET_WIDTH = 384;
    private static final int FACE_SHEET_HEIGHT = 192;
    private static final int FACE_CELL_SIZE = 96;
    private static final int FACE_SHEET_COLS = 4;
    private static final int FACE_MZ_SHEET_WIDTH = 576;
    private static final int FACE_MZ_SHEET_HEIGHT = 288;
    private static final int FACE_MZ_CELL_SIZE = 144;
    private static String cachedTextureAvatar;
    private static ResourceLocation cachedTexture;

    private static final java.util.Map<String, int[]> EXPRESSION_MAP = new java.util.HashMap<>();

    static {
        
        EXPRESSION_MAP.put("guine_sheet", new int[]{0, 1, 7, 2});
        EXPRESSION_MAP.put("guine_crest_sheet", new int[]{0, 1, 7, 2});
        EXPRESSION_MAP.put("guine_king_sheet", new int[]{0, 1, 7, 2});
        EXPRESSION_MAP.put("guine_prisoner_sheet", new int[]{0, 1, 7, 2});
        EXPRESSION_MAP.put("georuise_sheet", new int[]{0, 1, 7, 2});
        EXPRESSION_MAP.put("georuise_2_sheet", new int[]{0, 1, 7, 2});
        EXPRESSION_MAP.put("samidare_nin_sheet", new int[]{0, 1, 7, 2});
        EXPRESSION_MAP.put("samidare_spider_sheet", new int[]{0, 1, 7, 2});
        EXPRESSION_MAP.put("stiara_sheet", new int[]{0, 1, 7, 2});
        EXPRESSION_MAP.put("stiara_2_sheet", new int[]{0, 1, 7, 2});
        EXPRESSION_MAP.put("stiara_3_sheet", new int[]{0, 1, 7, 2});
        EXPRESSION_MAP.put("stiara_4_sheet", new int[]{0, 1, 7, 2});
        EXPRESSION_MAP.put("senpai_sheet", new int[]{0, 1, 7, 2});

    }


    public static void draw(GuiGraphics guiGraphics, ResourceLocation tex, String avatarId, int x, int y, int size) {
        if (avatarId.endsWith("_mz_sheet")) {
            int logicalExpression = getExpression();
            int expression = remapExpression(avatarId, logicalExpression);

            int u = (expression % FACE_SHEET_COLS) * FACE_MZ_CELL_SIZE;
            int v = (expression / FACE_SHEET_COLS) * FACE_MZ_CELL_SIZE;

            guiGraphics.blit(
                    tex,
                    x, y,
                    size, size,
                    u, v,
                    FACE_MZ_CELL_SIZE, FACE_MZ_CELL_SIZE,
                    FACE_MZ_SHEET_WIDTH, FACE_MZ_SHEET_HEIGHT
            );
        } else if (avatarId.endsWith("_sheet")) {
            int logicalExpression = getExpression();
            int expression = remapExpression(avatarId, logicalExpression);

            int u = (expression % FACE_SHEET_COLS) * FACE_CELL_SIZE;
            int v = (expression / FACE_SHEET_COLS) * FACE_CELL_SIZE;

            guiGraphics.blit(
                    tex,
                    x, y,
                    size, size,
                    u, v,
                    FACE_CELL_SIZE, FACE_CELL_SIZE,
                    FACE_SHEET_WIDTH, FACE_SHEET_HEIGHT
            );
        } else {
            guiGraphics.blit(
                    tex,
                    x, y,
                    size, size,
                    0, 0,
                    96, 96,
                    96, 96
            );
        }
    }

    public static ResourceLocation getTexture(String avatarId) {
        if (!avatarId.equals(cachedTextureAvatar)) {
            ResourceLocation texture = new ResourceLocation(BlackSouls.MODID, "textures/gui/avatars/" + avatarId + ".png");
            cachedTextureAvatar = avatarId;
            cachedTexture = texture;
        }
        return cachedTexture;
    }

    private static int remapExpression(String avatarId, int logicalExpression) {
        int[] map = EXPRESSION_MAP.get(avatarId);

        if (map == null) {
            return logicalExpression;
        }

        if (logicalExpression < 0 || logicalExpression >= map.length) {
            return 0;
        }

        return map[logicalExpression];
    }

    private static int getExpression() {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;

        float hp = mc.player.getHealth();
        float maxHp = mc.player.getMaxHealth();

        BSPlayerStats stats = mc.player.getCapability(BSPlayerStats.CAPABILITY).resolve().orElse(null);

        float hpRatio = hp / maxHp;
        float mpRatio = 1.0f;

        if (stats != null && stats.maxMp > 0) {
            mpRatio = (float) (stats.mp / stats.maxMp);
        }

        
        if (hpRatio <= 0.25f) {
            return 2;
        }

        
        if (hpRatio <= 0.5f) {
            return 1;
        }

        
        if (mpRatio <= 0.2f) {
            return 3;
        }

        
        return 0;
    }
}

package com.BlackSouls.BlackSoulsMod.client.render;

final class OriginalPoolCrowdSprite {
    private OriginalPoolCrowdSprite() {
    }

    static boolean applies(String npcId) {
        if (npcId == null) return false;
        return switch (npcId) {
            case "map037_ev003", "map037_ev004", "map037_ev005", "map037_ev006", "map037_ev007",
                    "map037_ev008", "map037_ev009", "map037_ev010", "map037_ev011", "map037_ev054" -> true;
            default -> false;
        };
    }

    static int bushOpacity(int opacity) {
        return opacity * 128 / 255;
    }
}

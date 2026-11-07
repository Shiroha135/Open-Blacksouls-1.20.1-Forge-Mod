package yuki135.blacksouls.story;

import com.google.gson.JsonObject;

public final class OriginalNpcSymbolState {
    private boolean enabled;
    private boolean forming;

    public static boolean recognizes(JsonObject movement) {
        if (!movement.has("move_type") || movement.get("move_type").getAsInt() != 3 || !movement.has("move_route")) return false;
        JsonObject route = movement.getAsJsonObject("move_route");
        if (!route.has("repeat") || !route.get("repeat").getAsBoolean() || route.get("wait").getAsBoolean()
                || route.get("skippable").getAsBoolean()) return false;
        var list = route.getAsJsonArray("list");
        if (list == null || list.size() != 2) return false;
        var command = list.get(0).getAsJsonObject();
        var end = list.get(1).getAsJsonObject();
        return command.get("code").getAsInt() == 45 && command.getAsJsonArray("parameters").size() == 1
                && command.getAsJsonArray("parameters").get(0).isJsonPrimitive()
                && command.getAsJsonArray("parameters").get(0).getAsJsonPrimitive().isString()
                && command.getAsJsonArray("parameters").get(0).getAsString().equals("enable_symbol_encount(1)")
                && end.get("code").getAsInt() == 0 && end.getAsJsonArray("parameters").isEmpty();
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean forming() {
        return forming;
    }

    public void beforeFrame(Context context) {
        if (!enabled || !active(context)) return;
        boolean reaction = distance(context) <= (forming ? 5 : context.symbolDashMoving() ? 4 : 3);
        if (!forming && reaction) {
            context.symbolRate(4, 5);
            context.symbolAlert();
        } else if (forming && !reaction) {
            context.symbolRate(0, 0);
        }
        forming = distance(context) <= (forming ? 5 : context.symbolDashMoving() ? 4 : 3);
    }

    public void move(Context context) {
        if (!enabled) {
            enabled = true;
            return;
        }
        if (active(context) && forming) {
            context.symbolRate(4, 5);
            if (distance(context) < 20) {
                switch (context.symbolRandom(6)) {
                    case 0, 1, 2, 3 -> toward(context);
                    case 4 -> context.symbolStep(2 + context.symbolRandom(4) * 2, false);
                    case 5 -> context.symbolStep(context.symbolDirection());
                    default -> throw new IllegalStateException("Invalid original symbol random value");
                }
            } else {
                context.symbolStep(2 + context.symbolRandom(4) * 2, false);
            }
        } else {
            context.symbolRate(0, 0);
        }
    }

    private boolean active(Context context) {
        if (context.symbolInterpreterRunning()) return false;
        if (context.symbolErased() || context.symbolStealth()) {
            forming = false;
            return false;
        }
        return context.symbolHasPlayer();
    }

    private static int distance(Context context) {
        return Math.abs(context.symbolX() - context.symbolPlayerX()) + Math.abs(context.symbolY() - context.symbolPlayerY());
    }

    private static void toward(Context context) {
        int sx = context.symbolX() - context.symbolPlayerX(), sy = context.symbolY() - context.symbolPlayerY();
        if (Math.abs(sx) > Math.abs(sy)) {
            if (!context.symbolStep(sx > 0 ? 4 : 6) && sy != 0) context.symbolStep(sy > 0 ? 8 : 2);
        } else {
            if (!context.symbolStep(sy > 0 ? 8 : 2) && sx != 0) context.symbolStep(sx > 0 ? 4 : 6);
        }
    }

    public static int contactCondition(int direction, int playerDirection, double deltaX, double deltaY) {
        if (direction != playerDirection) return 0;
        int position;
        if (deltaX == 0.0D) position = deltaY > 0.0D ? 1 : 0;
        else if (deltaY == 0.0D) position = deltaX > 0.0D ? 2 : 3;
        else return 0;
        return switch (direction) {
            case 2 -> position == 0 ? 1 : 2;
            case 4 -> position == 2 ? 1 : 2;
            case 6 -> position == 3 ? 1 : 2;
            case 8 -> position == 1 ? 1 : 2;
            default -> throw new IllegalArgumentException("Invalid original symbol direction");
        };
    }

    public JsonObject snapshot() {
        JsonObject state = new JsonObject();
        state.addProperty("profile", 1);
        state.addProperty("enabled", enabled);
        state.addProperty("forming", forming);
        return state;
    }

    public void restore(JsonObject state) {
        if (!state.keySet().equals(java.util.Set.of("profile", "enabled", "forming"))
                || !state.get("profile").isJsonPrimitive() || !state.get("profile").getAsJsonPrimitive().isNumber()
                || state.get("profile").getAsBigDecimal().intValueExact() != 1
                || !state.get("enabled").isJsonPrimitive() || !state.get("enabled").getAsJsonPrimitive().isBoolean()
                || !state.get("forming").isJsonPrimitive() || !state.get("forming").getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Invalid original symbol state");
        }
        enabled = state.get("enabled").getAsBoolean();
        forming = state.get("forming").getAsBoolean();
        if (forming && !enabled) throw new IllegalArgumentException("Inactive original symbol cannot be forming");
    }

    public interface Context {
        default boolean symbolHasPlayer() { throw new UnsupportedOperationException("symbol_player"); }
        default boolean symbolDashMoving() { throw new UnsupportedOperationException("symbol_dash"); }
        default boolean symbolInterpreterRunning() { throw new UnsupportedOperationException("symbol_interpreter"); }
        default boolean symbolErased() { throw new UnsupportedOperationException("symbol_erased"); }
        default boolean symbolStealth() { throw new UnsupportedOperationException("symbol_stealth"); }
        default boolean symbolBattlePaused() { return false; }
        default int symbolX() { throw new UnsupportedOperationException("symbol_x"); }
        default int symbolY() { throw new UnsupportedOperationException("symbol_y"); }
        default int symbolPlayerX() { throw new UnsupportedOperationException("symbol_player_x"); }
        default int symbolPlayerY() { throw new UnsupportedOperationException("symbol_player_y"); }
        default int symbolDirection() { throw new UnsupportedOperationException("symbol_direction"); }
        default int symbolRandom(int bound) { throw new UnsupportedOperationException("symbol_random"); }
        default void symbolRate(int speed, int frequency) { throw new UnsupportedOperationException("symbol_rate"); }
        default void symbolAlert() { throw new UnsupportedOperationException("symbol_alert"); }
        default boolean symbolStep(int direction) { throw new UnsupportedOperationException("symbol_step"); }
        default boolean symbolStep(int direction, boolean turnOnFailure) { return symbolStep(direction); }
    }
}

package yuki135.blacksouls.story;

import com.google.gson.JsonObject;

public final class OriginalNpcPictureState {
    private final int number;
    private String name = "";
    private int origin;
    private double x;
    private double y;
    private double zoomX = 100.0D;
    private double zoomY = 100.0D;
    private double opacity = 255.0D;
    private int blendType = 1;
    private double targetX;
    private double targetY;
    private double targetZoomX = 100.0D;
    private double targetZoomY = 100.0D;
    private double targetOpacity = 255.0D;
    private int duration;

    public OriginalNpcPictureState(int number) {
        if (number < 1 || number > 100) throw new IllegalArgumentException("Invalid original picture number");
        this.number = number;
    }

    public void show(String name, int origin, double x, double y, double zoomX, double zoomY, double opacity, int blendType) {
        if (name == null) throw new IllegalArgumentException("Missing original picture name");
        validate(origin, x, y, zoomX, zoomY, opacity, blendType, 0);
        this.name = name;
        this.origin = origin;
        this.x = x;
        this.y = y;
        this.zoomX = zoomX;
        this.zoomY = zoomY;
        this.opacity = opacity;
        this.blendType = blendType;
        targetX = x;
        targetY = y;
        targetZoomX = zoomX;
        targetZoomY = zoomY;
        targetOpacity = opacity;
        duration = 0;
    }

    public void move(int origin, double x, double y, double zoomX, double zoomY, double opacity,
                     int blendType, int duration) {
        validate(origin, x, y, zoomX, zoomY, opacity, blendType, duration);
        this.origin = origin;
        this.blendType = blendType;
        targetX = x;
        targetY = y;
        targetZoomX = zoomX;
        targetZoomY = zoomY;
        targetOpacity = opacity;
        this.duration = duration;
    }

    public void erase() {
        name = "";
        origin = 0;
    }

    public void advanceFrames(int frames) {
        if (frames < 0 || frames > 360000) throw new IllegalArgumentException("Invalid original picture frame count");
        for (int frame = 0; frame < frames && duration > 0; frame++) {
            int remaining = duration;
            x = (x * (remaining - 1) + targetX) / remaining;
            y = (y * (remaining - 1) + targetY) / remaining;
            zoomX = (zoomX * (remaining - 1) + targetZoomX) / remaining;
            zoomY = (zoomY * (remaining - 1) + targetZoomY) / remaining;
            opacity = (opacity * (remaining - 1) + targetOpacity) / remaining;
            duration--;
        }
    }

    public int number() { return number; }
    public String name() { return name; }
    public int origin() { return origin; }
    public double x() { return x; }
    public double y() { return y; }
    public double zoomX() { return zoomX; }
    public double zoomY() { return zoomY; }
    public double opacity() { return opacity; }
    public int blendType() { return blendType; }
    public int duration() { return duration; }

    public JsonObject snapshot() {
        JsonObject result = new JsonObject();
        result.addProperty("version", 1);
        result.addProperty("number", number);
        result.addProperty("name", name);
        result.addProperty("origin", origin);
        result.addProperty("x", x);
        result.addProperty("y", y);
        result.addProperty("zoomX", zoomX);
        result.addProperty("zoomY", zoomY);
        result.addProperty("opacity", opacity);
        result.addProperty("blendType", blendType);
        result.addProperty("targetX", targetX);
        result.addProperty("targetY", targetY);
        result.addProperty("targetZoomX", targetZoomX);
        result.addProperty("targetZoomY", targetZoomY);
        result.addProperty("targetOpacity", targetOpacity);
        result.addProperty("duration", duration);
        return result;
    }

    public static OriginalNpcPictureState restore(JsonObject snapshot) {
        if (integer(snapshot, "version") != 1) throw new IllegalArgumentException("Unsupported original picture snapshot");
        if (!snapshot.has("name") || !snapshot.get("name").isJsonPrimitive()
                || !snapshot.getAsJsonPrimitive("name").isString()) throw new IllegalArgumentException("Invalid original picture name");
        OriginalNpcPictureState state = new OriginalNpcPictureState(integer(snapshot, "number"));
        state.show(snapshot.get("name").getAsString(), integer(snapshot, "origin"), number(snapshot, "x"),
                number(snapshot, "y"), number(snapshot, "zoomX"), number(snapshot, "zoomY"),
                number(snapshot, "opacity"), integer(snapshot, "blendType"));
        state.move(state.origin, number(snapshot, "targetX"), number(snapshot, "targetY"),
                number(snapshot, "targetZoomX"), number(snapshot, "targetZoomY"), number(snapshot, "targetOpacity"),
                state.blendType, integer(snapshot, "duration"));
        return state;
    }

    private static void validate(int origin, double x, double y, double zoomX, double zoomY,
                                 double opacity, int blendType, int duration) {
        if (origin < 0 || origin > 1 || blendType < 0 || blendType > 2 || duration < 0 || duration > 360000
                || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(zoomX)
                || !Double.isFinite(zoomY) || !Double.isFinite(opacity) || opacity < 0.0D || opacity > 255.0D) {
            throw new IllegalArgumentException("Invalid original picture properties");
        }
    }

    private static int integer(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isNumber()) {
            throw new IllegalArgumentException("Invalid original picture integer: " + key);
        }
        return object.get(key).getAsBigDecimal().intValueExact();
    }

    private static double number(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isNumber()) {
            throw new IllegalArgumentException("Invalid original picture number: " + key);
        }
        return object.get(key).getAsDouble();
    }
}

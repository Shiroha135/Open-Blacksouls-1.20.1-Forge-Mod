package com.BlackSouls.BlackSoulsMod.client;

import java.util.List;

public final class OriginalNpcPresentationState {
    private int brightness = 255;
    private int fadeOut;
    private int fadeIn;
    private int transferBrightness = 255;
    private int transferFadeFrames;
    private boolean transferFadeOut;
    private final double[] tone = new double[4];
    private double[] toneTarget = new double[4];
    private int toneDuration;
    private final double[] flash = new double[4];
    private int flashDuration;
    private double shake;
    private int shakePower;
    private int shakeSpeed;
    private int shakeDuration;
    private int shakeDirection = 1;

    public void apply(int code, List<Double> values) {
        switch (code) {
            case 221 -> {
                if (transferFade(values)) {
                    int elapsed = values.size() == 2 ? values.get(1).intValue() : 0;
                    transferFadeFrames = 30 - elapsed;
                    transferFadeOut = true;
                    if (values.size() == 2) transferBrightness = 255 - 255 * elapsed / 30;
                } else {
                    fadeOut = 30;
                    fadeIn = 0;
                }
            }
            case 222 -> {
                if (transferFade(values)) {
                    int elapsed = values.size() == 2 ? values.get(1).intValue() : 0;
                    transferFadeFrames = 30 - elapsed;
                    transferFadeOut = false;
                    if (values.size() == 2) transferBrightness = 255 * elapsed / 30;
                } else {
                    fadeIn = 30;
                    fadeOut = 0;
                }
            }
            case 223 -> {
                toneTarget = new double[]{values.get(0), values.get(1), values.get(2), values.get(3)};
                toneDuration = values.get(4).intValue();
                if (toneDuration == 0) {
                    System.arraycopy(toneTarget, 0, tone, 0, 4);
                }
            }
            case 224 -> {
                for (int index = 0; index < 4; index++) {
                    flash[index] = values.get(index);
                }
                flashDuration = values.get(4).intValue();
            }
            case 225 -> {
                shakePower = values.get(0).intValue();
                shakeSpeed = values.get(1).intValue();
                shakeDuration = values.get(2).intValue();
            }
            default -> throw new IllegalArgumentException("Unsupported original presentation state command " + code);
        }
    }

    public void advanceFrames(int frames) {
        for (int frame = 0; frame < frames; frame++) {
            if (fadeOut > 0) {
                brightness = brightness * (fadeOut - 1) / fadeOut;
                fadeOut--;
            }
            if (fadeIn > 0) {
                brightness = (brightness * (fadeIn - 1) + 255) / fadeIn;
                fadeIn--;
            }
            if (transferFadeFrames > 0) {
                int value = 255 * (31 - transferFadeFrames) / 30;
                transferBrightness = transferFadeOut ? 255 - value : value;
                transferFadeFrames--;
            }
            if (toneDuration > 0) {
                for (int index = 0; index < 4; index++) {
                    tone[index] = (tone[index] * (toneDuration - 1) + toneTarget[index]) / toneDuration;
                }
                toneDuration--;
            }
            if (flashDuration > 0) {
                flash[3] = flash[3] * (flashDuration - 1) / flashDuration;
                flashDuration--;
            }
            if (shakeDuration > 0 || shake != 0.0D) {
                double delta = shakePower * shakeSpeed * shakeDirection / 10.0D;
                if (shakeDuration <= 1 && shake * (shake + delta) < 0.0D) {
                    shake = 0.0D;
                } else {
                    shake += delta;
                }
                if (shake > shakePower * 2.0D) {
                    shakeDirection = -1;
                }
                if (shake < -shakePower * 2.0D) {
                    shakeDirection = 1;
                }
                shakeDuration--;
            }
        }
    }

    public int brightness() {
        return brightness * transferBrightness / 255;
    }

    public double[] tone() {
        return tone.clone();
    }

    public double[] flash() {
        return flash.clone();
    }

    public double shake() {
        return shake;
    }

    public boolean active() {
        return brightness() != 255 || flash[3] > 0.0D || shake != 0.0D
                || tone[0] != 0.0D || tone[1] != 0.0D || tone[2] != 0.0D || tone[3] != 0.0D;
    }

    private static boolean transferFade(List<Double> values) {
        if (values == null) throw new IllegalArgumentException("Missing original fade values");
        if (values.isEmpty()) return false;
        if (values.equals(List.of(1.0D))) return true;
        if (values.size() == 2 && values.get(0) != null && values.get(0) == 2.0D && values.get(1) != null
                && Double.isFinite(values.get(1)) && values.get(1) == Math.rint(values.get(1))
                && values.get(1) >= 0 && values.get(1) <= 30) return true;
        throw new IllegalArgumentException("Invalid original transfer fade marker");
    }

    public static int balloonFrame(int remainingFrames) {
        return Math.max(0, Math.min(7, 7 - Math.max((remainingFrames - 12) / 8, 0)));
    }
}

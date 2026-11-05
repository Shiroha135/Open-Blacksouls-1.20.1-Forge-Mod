package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.network.packets.ClientboundOriginalNpcPacket;

public final class OriginalNumberInput {
    private final int digits;
    private final int minimum;
    private final int maximum;
    private int value;
    private int cursor;

    public OriginalNumberInput(ClientboundOriginalNpcPacket.NumberView view) {
        digits = view.digits();
        minimum = view.minimum();
        maximum = view.maximum();
        value = view.initial();
    }

    public int value() {
        return value;
    }

    public int cursor() {
        return cursor;
    }

    public void select(int index) {
        cursor = Math.max(0, Math.min(digits - 1, index));
    }

    public void move(int delta) {
        cursor = Math.floorMod(cursor + delta, digits);
    }

    public void change(int delta) {
        int place = (int) Math.pow(10, digits - cursor - 1);
        int digit = value / place % 10;
        value = Math.max(minimum, Math.min(maximum, value - digit * place + Math.floorMod(digit + delta, 10) * place));
    }

    public String text() {
        String number = Integer.toString(value);
        return "0".repeat(digits - number.length()) + number;
    }
}

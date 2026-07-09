package com.BlackSouls.BlackSoulsMod.handler;

public class BSStatToast {
    public final String text;
    public final int color;
    public int timeLeft;

    public BSStatToast(String text, int color) {
        this.text = text;
        this.color = color;
        this.timeLeft = 40;
    }
}
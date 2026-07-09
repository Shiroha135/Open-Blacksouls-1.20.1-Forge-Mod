package com.BlackSouls.BlackSoulsMod.client.render;

public class VFXCell {
    public int textureIndex;
    public float offsetX, offsetY;
    public float scaleX, scaleY;
    public float rotation;
    public float alpha;
    public boolean mirror;
    public VFXCell(int textureIndex, float offsetX, float offsetY, float scaleX, float scaleY, float rotation, float alpha, boolean mirror) {
        this.textureIndex = textureIndex;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.rotation = rotation;
        this.alpha = alpha;
        this.mirror = mirror;
    }
}
package com.BlackSouls.BlackSoulsMod.client.render;

import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

public class VFXAnimation {
    public ResourceLocation texture1;
    public ResourceLocation texture2;
    public int cols = 5; 
    public int rows1;
    public int rows2;
    public List<VFXFrame> frames = new ArrayList<>();
    public List<VFXSoundTiming> soundTimings = new ArrayList<>();

    public VFXAnimation(String tex1, String tex2, int r1, int r2) {
        this.texture1 = (tex1 == null || tex1.isEmpty()) ? null : new ResourceLocation(tex1);
        this.texture2 = (tex2 == null || tex2.isEmpty()) ? null : new ResourceLocation(tex2);
        this.rows1 = r1;
        this.rows2 = r2;
    }
}

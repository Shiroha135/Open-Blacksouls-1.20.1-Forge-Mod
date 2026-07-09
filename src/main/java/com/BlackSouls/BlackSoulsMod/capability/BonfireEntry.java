package com.BlackSouls.BlackSoulsMod.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class BonfireEntry {
    public GlobalPos pos;
    public String name;
    public String description;

    public BonfireEntry(GlobalPos pos, String name, String description) {
        this.pos = pos;
        this.name = name;
        this.description = description;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", this.pos.dimension().location().toString());
        tag.putInt("X", this.pos.pos().getX());
        tag.putInt("Y", this.pos.pos().getY());
        tag.putInt("Z", this.pos.pos().getZ());
        tag.putString("Name", this.name);
        tag.putString("Description", this.description != null ? this.description : "");
        return tag;
    }

    public static BonfireEntry load(CompoundTag tag) {
        ResourceLocation dimLoc = new ResourceLocation(tag.getString("Dimension"));
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimLoc);
        BlockPos blockPos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        GlobalPos globalPos = GlobalPos.of(dimension, blockPos);

        String name = tag.getString("Name");
        String desc = tag.getString("Description");
        return new BonfireEntry(globalPos, name, desc);
    }
}
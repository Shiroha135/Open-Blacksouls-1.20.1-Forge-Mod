package com.BlackSouls.BlackSoulsMod.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiAccessor {
    @Accessor("toolHighlightTimer")
    int blacksouls$getToolHighlightTimer();

    @Accessor("lastToolHighlight")
    ItemStack blacksouls$getLastToolHighlight();
}

package com.BlackSouls.BlackSoulsMod.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("titleLabelX")
    int blacksouls$getTitleLabelX();

    @Accessor("titleLabelY")
    int blacksouls$getTitleLabelY();

    @Accessor("inventoryLabelX")
    int blacksouls$getInventoryLabelX();

    @Accessor("inventoryLabelY")
    int blacksouls$getInventoryLabelY();

    @Accessor("leftPos")
    int blacksouls$getLeftPos();

    @Accessor("topPos")
    int blacksouls$getTopPos();

    @Accessor("playerInventoryTitle")
    Component blacksouls$getPlayerInventoryTitle();
}

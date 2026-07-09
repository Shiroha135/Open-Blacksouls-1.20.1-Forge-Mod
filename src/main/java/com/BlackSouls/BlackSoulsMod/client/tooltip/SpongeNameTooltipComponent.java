package com.BlackSouls.BlackSoulsMod.client.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record SpongeNameTooltipComponent(Component text, String style) implements TooltipComponent {
}

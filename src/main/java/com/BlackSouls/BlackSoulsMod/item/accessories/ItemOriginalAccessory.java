package com.BlackSouls.BlackSoulsMod.item.accessories;

import com.BlackSouls.BlackSoulsMod.item.ItemBaubleBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class ItemOriginalAccessory extends ItemBaubleBase {
    public enum Profile {
        WORK_CLOTHES(2),
        ABYSS_ARMOR(3, 3),
        ABYSS_HELMET(3, 3),
        YELLOW_CLOTH(2, 2),
        PLAYWRIGHT_HEADSCARF(1),
        FALSE_ANGEL_CROWN(2),
        WINTER_MAGE_COAT(4, 4),
        WINTER_KNIGHT_ARMOR(4, 4),
        WINTER_KNIGHT_HELMET(4, 4),
        WINDLESS_CLOTHES(2, 2),
        MIRACLE_SHRINE_MAIDEN_GARB(3, 3);

        private final int statLines;
        private final int[] redLines;

        Profile(int statLines, int... redLines) {
            this.statLines = statLines;
            this.redLines = redLines;
        }

        private boolean isRed(int line) {
            return Arrays.stream(redLines).anyMatch(value -> value == line);
        }
    }

    private final Profile profile;

    public ItemOriginalAccessory(Profile profile, Properties properties) {
        super(properties);
        this.profile = profile;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, level, tooltip, flagIn);
        tooltip.add(Component.empty());
        for (int line = 1; line <= profile.statLines; line++) {
            tooltip.add(Component.translatable(this.getDescriptionId() + ".stat." + line)
                    .withStyle(profile.isRed(line) ? ChatFormatting.RED : ChatFormatting.AQUA));
        }
    }
}

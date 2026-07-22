package com.BlackSouls.BlackSoulsMod.item.rings;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class ItemOriginalRing extends ItemRingBase {
    public enum Profile {
        TENACIOUS(1),
        REBELLION(1),
        MIRACLE(2, 2),
        MURDER_CLOWN(3, 3),
        BLACK_GOAT(3, 3),
        BARBER(1),
        VANITY(2, 2),
        APPLE(2),
        LUNDINIAN(2, 2),
        PUMPKIN_KNIGHT(1),
        SNIPER(1),
        DEEP_ONE(1),
        WHITE_RAVEN(1),
        DULL_WOOD_GRAIN(1),
        TOTO(1),
        FOUR_LEAF_CLOVER(1),
        RECKLESS_HERO(2, 2),
        BANKER(1),
        HEAVEN(1),
        BOOTBLACK(1),
        BUTCHER(1),
        PROSTITUTE(2),
        EXORCISM(1),
        FIGHTER(2),
        TROLL(3, 2, 3),
        MOSQUITO(3),
        RED_TEARSTONE(2, 2),
        WALRUS(2, 2),
        HELL_DESTRUCTION(2, 2),
        HEART_KNIGHT(2),
        SPADE_KNIGHT(1),
        CLUB_KNIGHT(2),
        SIN(5, 5),
        STAR(2, 2),
        OGRE(2, 2),
        BEE(1),
        FRENZIED_KING(1, 1),
        IDATEN(1),
        MY_STRUGGLE(1),
        ADULTERY(1, 1),
        PUPPET(1),
        EDITH(0),
        PRICKETT(0),
        LIFE_PLUS_1(1), LIFE_PLUS_2(1), LIFE_PLUS_3(1),
        TENACIOUS_PLUS_1(1), TENACIOUS_PLUS_2(1), TENACIOUS_PLUS_3(1),
        PUYO_PLUS_1(2), PUYO_PLUS_2(2), PUYO_PLUS_3(2),
        HUNYA_PLUS_1(2), HUNYA_PLUS_2(2), HUNYA_PLUS_3(2),
        VOID_PLUS_1(1), VOID_PLUS_2(1), VOID_PLUS_3(1),
        EVIL_EYE_PLUS_1(1), EVIL_EYE_PLUS_2(1), EVIL_EYE_PLUS_3(1),
        GODDESS_PLUS_1(1), GODDESS_PLUS_2(1), GODDESS_PLUS_3(1),
        IRON_PROTECTION_PLUS_1(1), IRON_PROTECTION_PLUS_2(1), IRON_PROTECTION_PLUS_3(1),
        MAGIC_STONE_PLUS_1(2), MAGIC_STONE_PLUS_2(2), MAGIC_STONE_PLUS_3(2),
        SNIPER_PLUS_1(1), SNIPER_PLUS_2(1), SNIPER_PLUS_3(1),
        WASP_PLUS_1(1), WASP_PLUS_2(1), WASP_PLUS_3(1),
        BLADES_PLUS_1(1), BLADES_PLUS_2(1), BLADES_PLUS_3(1),
        GUARD_PLUS_1(1), GUARD_PLUS_2(1), GUARD_PLUS_3(1),
        WIND_GOD_PLUS_1(1), WIND_GOD_PLUS_2(1), WIND_GOD_PLUS_3(1),
        SPELL_PLUS_1(1), SPELL_PLUS_2(1), SPELL_PLUS_3(1),
        LUNDINIAN_PLUS_1(2, 2), LUNDINIAN_PLUS_2(2, 2), LUNDINIAN_PLUS_3(2, 2),
        CUT_DOWN(2, 2),
        GHOUL(1),
        ALMIGHTY(1),
        SIN_PLUS_1(5, 5), SIN_PLUS_2(5, 5), SIN_PLUS_3(5, 5),
        UNICORN(2),
        LION(2),
        TIGER_FOX(1),
        ICE_STONE(1),
        OLD_KING(2, 2),
        POLAR_BEAR(2, 1),
        DEFENSE_KING(1),
        BREAK_RESISTANCE(1),
        COUNTERATTACK(2),
        HOLY_FOREST(1),
        MOLASSES(2);

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

    public ItemOriginalRing(Profile profile, Properties properties) {
        super(properties);
        this.profile = profile;
    }

    public Profile getProfile() {
        return profile;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, level, tooltip, flagIn);
        for (int line = 1; line <= profile.statLines; line++) {
            tooltip.add(Component.translatable(this.getDescriptionId() + ".stat." + line)
                    .withStyle(profile.isRed(line) ? ChatFormatting.RED : ChatFormatting.AQUA));
        }
    }
}

package com.BlackSouls.BlackSoulsMod.util.skill;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

abstract class AbstractOriginalWeaponSkill extends AbstractWeaponCombatSkill {
    enum Family { MAGIC_BLADE, CLEAVER_AXE, RAGNAROK_ROUTE, BOW, MACE, HALBERD, BEAST_SAW, FORTRESS, DARK_SWORD, BROKEN_SWORD, WARHAMMER, FIST, KATANA, IRON_BALL, HANS_GUN, JUDGMENT_SCYTHE, STORM_RULER, DEMON_STAFF, MOONLIGHT_GREATSWORD, CORRUPT_SCYTHE, MAD_BOW, RLYEH_STAFF, DEEP_SEA_ANCHOR, LOST_SWORD, GLACHID, CHAINSAW, SPOON, HOLY_GUNBLADE, EUNICE_RAPIER, RAIDEN_AXES }

    private final Family family;
    private final boolean evolvedOnly;
    private final int minimumUpgradeLevel;
    private final int maximumUpgradeLevel;

    protected AbstractOriginalWeaponSkill(Family family, boolean evolvedOnly) {
        this(family, evolvedOnly, 0, Integer.MAX_VALUE);
    }

    protected AbstractOriginalWeaponSkill(Family family, int minimumUpgradeLevel) {
        this(family, false, minimumUpgradeLevel, Integer.MAX_VALUE);
    }

    protected AbstractOriginalWeaponSkill(Family family, int minimumUpgradeLevel, int maximumUpgradeLevel) {
        this(family, false, minimumUpgradeLevel, maximumUpgradeLevel);
    }

    private AbstractOriginalWeaponSkill(Family family, boolean evolvedOnly, int minimumUpgradeLevel, int maximumUpgradeLevel) {
        this.family = family;
        this.evolvedOnly = evolvedOnly;
        this.minimumUpgradeLevel = minimumUpgradeLevel;
        this.maximumUpgradeLevel = maximumUpgradeLevel;
    }

    @Override
    protected boolean isWeaponEquipped(Player player) {
        Item item = player.getMainHandItem().getItem();
        boolean equipped = switch (family) {
            case MAGIC_BLADE -> item == BlackSouls.DEMON_GOD_BLADE.get()
                    || (!evolvedOnly && item == BlackSouls.MAGIC_BLADE.get());
            case CLEAVER_AXE -> item == BlackSouls.SLAUGHTERER_GREATAXE.get()
                    || (!evolvedOnly && item == BlackSouls.MEAT_CLEAVER_GREATAXE.get());
            case RAGNAROK_ROUTE -> item == BlackSouls.RAGNAROK.get()
                    || (!evolvedOnly && item == BlackSouls.DOUBLE_EDGED_GREATSWORD.get());
            case BOW -> item == BlackSouls.BRAVE_BOW.get()
                    || (!evolvedOnly && item == BlackSouls.HUNTING_BOW.get());
            case MACE -> item == BlackSouls.DIVINE_PUNISHMENT_MACE.get()
                    || (!evolvedOnly && item == BlackSouls.MACE.get());
            case HALBERD -> item == BlackSouls.BAHAMUT.get()
                    || (!evolvedOnly && item == BlackSouls.HALBERD.get());
            case BEAST_SAW -> item == BlackSouls.BEAST_SLAYING_SAW_SWORD.get()
                    || (!evolvedOnly && item == BlackSouls.BEAST_HUNTER_SAW.get());
            case FORTRESS -> item == BlackSouls.GUARDIAN_FORTRESS.get()
                    || (!evolvedOnly && item == BlackSouls.SHIELD_GUARD_FORTRESS.get());
            case DARK_SWORD -> item == BlackSouls.DARK_BLADE.get()
                    || (!evolvedOnly && item == BlackSouls.DARK_SWORD.get());
            case BROKEN_SWORD -> item == BlackSouls.GRUDGE_SWORD.get()
                    || (!evolvedOnly && item == BlackSouls.BROKEN_SWORD.get());
            case WARHAMMER -> item == BlackSouls.ABERRANT_WARHAMMER.get()
                    || (!evolvedOnly && item == BlackSouls.WARHAMMER.get());
            case FIST -> item == BlackSouls.KAISER_GAUNTLET.get()
                    || (!evolvedOnly && item == BlackSouls.KNUCKLE_DUSTER.get());
            case KATANA -> item == BlackSouls.KISHIN_BLADE.get()
                    || (!evolvedOnly && item == BlackSouls.UCHIGATANA.get());
            case IRON_BALL -> item == BlackSouls.GREAT_IRON_BALL.get();
            case HANS_GUN -> item == BlackSouls.HANS_MACHINE_GUN.get();
            case JUDGMENT_SCYTHE -> item == BlackSouls.JUDGMENT_SCYTHE.get();
            case STORM_RULER -> item == BlackSouls.STORM_RULER.get();
            case DEMON_STAFF -> item == BlackSouls.DEMON_STAFF.get();
            case MOONLIGHT_GREATSWORD -> item == BlackSouls.MOONLIGHT_GREATSWORD.get();
            case CORRUPT_SCYTHE -> item == BlackSouls.CORRUPT_JABBERWOCK_SCYTHE.get();
            case MAD_BOW -> item == BlackSouls.MAD_BOW_JUBJUB.get();
            case RLYEH_STAFF -> item == BlackSouls.RLYEH_STAFF.get();
            case DEEP_SEA_ANCHOR -> item == BlackSouls.DEEP_SEA_KNIGHTS_ANCHOR.get();
            case LOST_SWORD -> item == BlackSouls.LOST_SWORD.get();
            case GLACHID -> item == BlackSouls.GLACHID.get();
            case CHAINSAW -> item == BlackSouls.SLAUGHTERERS_CHAINSAW.get();
            case SPOON -> item == BlackSouls.MOCK_TURTLE_SOUP_LADLE.get();
            case HOLY_GUNBLADE -> item == BlackSouls.HOLY_GUNBLADE.get();
            case EUNICE_RAPIER -> item == BlackSouls.EUNICES_RAPIER.get();
            case RAIDEN_AXES -> item == BlackSouls.RAIDENS_DUAL_AXES.get();
        };
        if (!equipped) return false;
        int level = getUpgradeLevel(player);
        return level >= minimumUpgradeLevel && level <= maximumUpgradeLevel;
    }

    protected int getUpgradeLevel(Player player) {
        net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
        return stack.hasTag() ? Math.max(0, Math.min(5, stack.getTag().getInt("bs2_upgrade_level"))) : 0;
    }
}

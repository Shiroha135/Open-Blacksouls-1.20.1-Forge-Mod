package com.BlackSouls.BlackSoulsMod.handler;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.BlackSouls.BlackSoulsMod.item.weapon.ItemBSWeaponBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WeaponUpgradeHandler {

    
    private static UpgradeReq getRequirement(Item weapon, int currentLevel) {
        
        if (weapon == BlackSouls.KNIGHT_SWORD.get()
                || weapon == BlackSouls.THIEFS_DAGGER.get()
                || weapon == BlackSouls.GREAT_SWORD.get()
                || weapon == BlackSouls.BROAD_SPEAR.get()
                || weapon == BlackSouls.VORPAL_BLADE.get()
                || weapon == BlackSouls.CLUB.get()
                || weapon == BlackSouls.MAGIC_BLADE.get()
                || weapon == BlackSouls.MAGICIANS_STAFF.get()
                || weapon == BlackSouls.DOUBLE_EDGED_GREATSWORD.get()
                || weapon == BlackSouls.MEAT_CLEAVER_GREATAXE.get()
                || weapon == BlackSouls.HUNTING_BOW.get()
                || weapon == BlackSouls.MACE.get()
                || weapon == BlackSouls.HALBERD.get()
                || weapon == BlackSouls.BEAST_HUNTER_SAW.get()
                || weapon == BlackSouls.SHIELD_GUARD_FORTRESS.get()
                || weapon == BlackSouls.DARK_SWORD.get()
                || weapon == BlackSouls.BROKEN_SWORD.get()
                || weapon == BlackSouls.WARHAMMER.get()
                || weapon == BlackSouls.KNUCKLE_DUSTER.get()
                || weapon == BlackSouls.UCHIGATANA.get()) {
            return getKnightSwordRoute(currentLevel);
        }

        
        if (weapon == BlackSouls.ANDOR_SWORD.get()
                || weapon == BlackSouls.DRAKE_SWORD.get()
                || weapon == BlackSouls.BANDERSNATCH_SWORD.get()
                || weapon == BlackSouls.GREAT_IRON_BALL.get()
                || weapon == BlackSouls.HANS_MACHINE_GUN.get()
                || weapon == BlackSouls.JUDGMENT_SCYTHE.get()
                || weapon == BlackSouls.STORM_RULER.get()
                || weapon == BlackSouls.DEMON_STAFF.get()
                || weapon == BlackSouls.MOONLIGHT_GREATSWORD.get()
                || weapon == BlackSouls.CORRUPT_JABBERWOCK_SCYTHE.get()
                || weapon == BlackSouls.MAD_BOW_JUBJUB.get()
                || weapon == BlackSouls.MIRANDA_AXE.get()
                || weapon == BlackSouls.RLYEH_STAFF.get()
                || weapon == BlackSouls.DEEP_SEA_KNIGHTS_ANCHOR.get()
                || weapon == BlackSouls.LOST_SWORD.get()
                || weapon == BlackSouls.GLACHID.get()
                || weapon == BlackSouls.SLAUGHTERERS_CHAINSAW.get()
                || weapon == BlackSouls.MOCK_TURTLE_SOUP_LADLE.get()
                || weapon == BlackSouls.DIVINE_ANGEL_DUAL_SWORDS.get()
                || weapon == BlackSouls.HOLY_GUNBLADE.get()
                || weapon == BlackSouls.EUNICES_RAPIER.get()
                || weapon == BlackSouls.RAIDENS_DUAL_AXES.get()) {
            return getSpecialWeaponRoute(currentLevel);
        }

        return null; 
    }

    
    private static UpgradeReq getKnightSwordRoute(int level) {
        Item shard = BlackSouls.UPGRADE_SHARD.get();               
        Item largeShard = BlackSouls.UPGRADE_LARGE_SHARD.get();    
        Item chunk = BlackSouls.UPGRADE_CHUNK.get();               
        Item slab = BlackSouls.UPGRADE_SLAB.get();                 

        return switch (level) {
            case 0 -> new UpgradeReq(shard, 1);       
            case 1 -> new UpgradeReq(shard, 3);       
            case 2 -> new UpgradeReq(shard, 5);       
            case 3 -> new UpgradeReq(largeShard, 1);  
            case 4 -> new UpgradeReq(largeShard, 3);  
            case 5 -> new UpgradeReq(largeShard, 5);  
            case 6 -> new UpgradeReq(chunk, 1);       
            case 7 -> new UpgradeReq(chunk, 3);       
            case 8 -> new UpgradeReq(chunk, 5);       
            case 9 -> new UpgradeReq(slab, 1);        
            default -> null; 
        };
    }

    
    private static UpgradeReq getSpecialWeaponRoute(int level) {
        Item shard = BlackSouls.MYSTERIOUS_SHARD.get();

        return switch (level) {
            
            case 0, 1, 2, 3, 4 -> new UpgradeReq(shard, 1);
            
            default -> null;
        };
    }

    @SubscribeEvent
    public static void onAnvilUpgrade(AnvilUpdateEvent event) {
        ItemStack weapon = event.getLeft();
        ItemStack material = event.getRight();

        
        if (weapon.isEmpty()) return;
        if (material.isEmpty()) return;

        
        int currentLevel = 0;
        if (weapon.hasTag() && weapon.getTag().contains("bs2_upgrade_level")) {
            currentLevel = weapon.getTag().getInt("bs2_upgrade_level");
        }

        
        UpgradeReq req = getRequirement(weapon.getItem(), currentLevel);
        if (req == null) return;

        
        if (material.getItem() == req.material && material.getCount() >= req.count) {

            
            if (currentLevel == 9 && getEvolutionResult(weapon.getItem()) != null) {
                Item evolvedItem = getEvolutionResult(weapon.getItem());
                ItemStack evolvedWeapon = new ItemStack(evolvedItem);
                
                if (weapon.isEnchanted()) {
                    evolvedWeapon.getOrCreateTag().put("Enchantments", weapon.getEnchantmentTags().copy());
                }

                event.setOutput(evolvedWeapon); 
                event.setMaterialCost(req.count); 
                event.setCost(30); 
            }
            
            else {
                ItemStack upgradedWeapon = weapon.copy();
                int newLevel = currentLevel + 1;

                
                upgradedWeapon.getOrCreateTag().putInt("bs2_upgrade_level", newLevel);

                
                String pureName = Component.translatable(weapon.getItem().getDescriptionId()).getString();
                upgradedWeapon.setHoverName(Component.literal(pureName + " +" + newLevel));

                event.setOutput(upgradedWeapon);
                event.setMaterialCost(req.count); 
                event.setCost(newLevel * 2);      
            }
        }
    }

    private static Item getEvolutionResult(Item weapon) {
        if (weapon == BlackSouls.KNIGHT_SWORD.get()) return BlackSouls.KNIGHT_KING_SWORD.get();
        if (weapon == BlackSouls.THIEFS_DAGGER.get()) return BlackSouls.GREAT_THIEFS_DAGGER.get();
        if (weapon == BlackSouls.GREAT_SWORD.get()) return BlackSouls.GIANT_SWORD.get();
        if (weapon == BlackSouls.BROAD_SPEAR.get()) return BlackSouls.GUNGNIR.get();
        if (weapon == BlackSouls.VORPAL_BLADE.get()) return BlackSouls.VORPAL_SWORD.get();
        if (weapon == BlackSouls.CLUB.get()) return BlackSouls.KING_CLUB.get();
        if (weapon == BlackSouls.MAGIC_BLADE.get()) return BlackSouls.DEMON_GOD_BLADE.get();
        if (weapon == BlackSouls.MAGICIANS_STAFF.get()) return BlackSouls.ALL_CREATION_STAFF.get();
        if (weapon == BlackSouls.DOUBLE_EDGED_GREATSWORD.get()) return BlackSouls.RAGNAROK.get();
        if (weapon == BlackSouls.MEAT_CLEAVER_GREATAXE.get()) return BlackSouls.SLAUGHTERER_GREATAXE.get();
        if (weapon == BlackSouls.HUNTING_BOW.get()) return BlackSouls.BRAVE_BOW.get();
        if (weapon == BlackSouls.MACE.get()) return BlackSouls.DIVINE_PUNISHMENT_MACE.get();
        if (weapon == BlackSouls.HALBERD.get()) return BlackSouls.BAHAMUT.get();
        if (weapon == BlackSouls.BEAST_HUNTER_SAW.get()) return BlackSouls.BEAST_SLAYING_SAW_SWORD.get();
        if (weapon == BlackSouls.SHIELD_GUARD_FORTRESS.get()) return BlackSouls.GUARDIAN_FORTRESS.get();
        if (weapon == BlackSouls.DARK_SWORD.get()) return BlackSouls.DARK_BLADE.get();
        if (weapon == BlackSouls.BROKEN_SWORD.get()) return BlackSouls.GRUDGE_SWORD.get();
        if (weapon == BlackSouls.WARHAMMER.get()) return BlackSouls.ABERRANT_WARHAMMER.get();
        if (weapon == BlackSouls.KNUCKLE_DUSTER.get()) return BlackSouls.KAISER_GAUNTLET.get();
        if (weapon == BlackSouls.UCHIGATANA.get()) return BlackSouls.KISHIN_BLADE.get();
        return null;
    }

    
    private static class UpgradeReq {
        Item material;
        int count;
        UpgradeReq(Item material, int count) {
            this.material = material;
            this.count = count;
        }
    }
}

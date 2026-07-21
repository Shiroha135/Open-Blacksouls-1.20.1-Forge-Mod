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
                || weapon == BlackSouls.CLUB.get()) {
            return getKnightSwordRoute(currentLevel);
        }

        
        if (weapon == BlackSouls.ANDOR_SWORD.get()
                || weapon == BlackSouls.DRAKE_SWORD.get()
                || weapon == BlackSouls.BANDERSNATCH_SWORD.get()) {
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

        
        if (weapon.isEmpty() || !(weapon.getItem() instanceof ItemBSWeaponBase)) return;
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

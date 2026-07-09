package com.BlackSouls.BlackSoulsMod.item.weapon;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

public class ItemBSWeaponBase extends SwordItem {


    
    protected float stunChance = 0.0f; 
    protected int stunDuration = 40;   

    public ItemBSWeaponBase(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    
    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        if (ToolActions.SWORD_SWEEP == toolAction) {
            return false;
        }
        return super.canPerformAction(stack, toolAction);
    }

    
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }


    
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        
        if (this.stunChance > 0.0f && !attacker.level().isClientSide()) {
            if (Math.random() < this.stunChance) {
                if (BlackSouls.BUFF_STUN.isPresent()) {
                    target.addEffect(new MobEffectInstance(BlackSouls.BUFF_STUN.get(), this.stunDuration, 0));
                }
            }
        }
        return true; 
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entityMining) {
        return true; 
    }
}
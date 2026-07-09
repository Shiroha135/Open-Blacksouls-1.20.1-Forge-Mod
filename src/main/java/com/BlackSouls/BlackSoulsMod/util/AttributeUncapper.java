package com.BlackSouls.BlackSoulsMod.util;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AttributeUncapper {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            
            double uncapValue = Integer.MAX_VALUE;
            
            unlock(Attributes.MAX_HEALTH, uncapValue);
            
            unlock(Attributes.ARMOR, uncapValue);
            
            unlock(Attributes.ARMOR_TOUGHNESS, uncapValue);
            
            unlock(Attributes.ATTACK_DAMAGE, uncapValue);
        });
    }
    
    private static void unlock(Attribute attribute, double newMax) {
        if (attribute instanceof RangedAttribute ranged) {
            try {
                
                for (Field field : RangedAttribute.class.getDeclaredFields()) {
                    
                    if (field.getType() == double.class) {
                        field.setAccessible(true);
                        double currentVal = field.getDouble(ranged);
                        if (currentVal == 1024.0D || currentVal == 30.0D || currentVal == 20.0D || currentVal == 2048.0D) {
                            field.setDouble(ranged, newMax);
                            LOGGER.debug("DEBUG：成功强拆属性上限喵。属性:{},新上限:{}", attribute.getDescriptionId(), newMax);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("DEBUG：强拆属性上限失败了喵。属性:{}", attribute.getDescriptionId(), e);
            }
        }
    }
}
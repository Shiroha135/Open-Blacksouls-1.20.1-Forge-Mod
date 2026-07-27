package com.BlackSouls.BlackSoulsMod.entity;

import com.BlackSouls.BlackSoulsMod.BlackSouls;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class EntityCorpseEatingRabbit extends EntityTurnBattleMonster {
    private static final ResourceLocation BATTLE_TEXTURE =
            new ResourceLocation(BlackSouls.MODID, "textures/entity/corpse_eating_rabbit.png");

    public EntityCorpseEatingRabbit(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.ATTACK_DAMAGE, 40.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.LUCK, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    public ResourceLocation getTurnBattleTexture() {
        return BATTLE_TEXTURE;
    }

    @Override
    public int getTurnBattleTextureWidth() {
        return 127;
    }

    @Override
    public int getTurnBattleTextureHeight() {
        return 121;
    }

    @Override
    public double getTurnBattleAttack() {
        return 40.0D;
    }

    @Override
    public double getTurnBattleDefense() {
        return 10.0D;
    }

    @Override
    public double getTurnBattleAgility() {
        return 40.0D;
    }

    @Override
    public long getTurnBattleSoulReward() {
        return 38L;
    }

    @Override
    public String getTurnBattleAttackText() {
        return "进行了撕咬";
    }
}

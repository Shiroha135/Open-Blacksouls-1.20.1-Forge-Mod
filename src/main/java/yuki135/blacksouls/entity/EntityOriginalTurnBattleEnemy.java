package yuki135.blacksouls.entity;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.util.BSOriginalEnemyData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

@SuppressWarnings("removal")
public class EntityOriginalTurnBattleEnemy extends EntityTurnBattleMonster {
    private static final EntityDataAccessor<Boolean> HEAD_SEARCHING = SynchedEntityData.defineId(
            EntityOriginalTurnBattleEnemy.class, EntityDataSerializers.BOOLEAN);
    private final Profile profile;

    public EntityOriginalTurnBattleEnemy(EntityType<? extends Monster> type, Level level, Profile profile) {
        super(type, level);
        this.profile = profile;
    }

    public static AttributeSupplier.Builder createAttributes(Profile profile) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, profile.health)
                .add(Attributes.ATTACK_DAMAGE, profile.attack)
                .add(Attributes.ARMOR, profile.defense)
                .add(Attributes.MOVEMENT_SPEED, profile.movementSpeed)
                .add(Attributes.LUCK, profile.luck)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    public Profile getProfile() {
        return this.profile;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HEAD_SEARCHING, false);
    }

    public void setHeadSearching(boolean searching) {
        this.entityData.set(HEAD_SEARCHING, searching);
    }

    public boolean isHeadSearching() {
        return this.entityData.get(HEAD_SEARCHING);
    }

    public int getOriginalBattleProfileId() {
        return this.profile == Profile.WEREWOLF ? isHeadSearching() ? 7 : 5 : 0;
    }

    @Override
    public ResourceLocation getTurnBattleTexture() {
        if (this.profile == Profile.WEREWOLF) {
            return BSOriginalEnemyData.get(getOriginalBattleProfileId()).texture();
        }
        return this.profile.texture;
    }

    @Override
    public int getTurnBattleTextureWidth() {
        if (this.profile == Profile.WEREWOLF) {
            return BSOriginalEnemyData.get(getOriginalBattleProfileId()).textureWidth();
        }
        return this.profile.textureWidth;
    }

    @Override
    public int getTurnBattleTextureHeight() {
        if (this.profile == Profile.WEREWOLF) {
            return BSOriginalEnemyData.get(getOriginalBattleProfileId()).textureHeight();
        }
        return this.profile.textureHeight;
    }

    @Override
    public double getTurnBattleAttack() {
        return this.profile.attack;
    }

    @Override
    public double getTurnBattleDefense() {
        return this.profile.defense;
    }

    @Override
    public double getTurnBattleAgility() {
        return this.profile.agility;
    }

    @Override
    public long getTurnBattleSoulReward() {
        return this.profile.souls;
    }

    @Override
    public String getTurnBattleAttackText() {
        return this.profile.attackText;
    }

    public enum Profile {
        HEADLESS_UNDEAD("headless_undead", 77, 180, 180.0D, 55.0D, 10.0D, 12.0D, 8.0D,
                48L, 0.24D, "挥动了武器"),
        CORRUPT_DOG("corrupt_dog", 168, 190, 220.0D, 44.0D, 12.0D, 85.0D, 8.0D,
                100L, 0.32D, "进行了扑咬"),
        WEREWOLF("werewolf", 361, 230, 800.0D, 75.0D, 20.0D, 40.0D, 8.0D,
                338L, 0.30D, "挥下了利爪");

        private final ResourceLocation texture;
        private final int textureWidth;
        private final int textureHeight;
        private final double health;
        private final double attack;
        private final double defense;
        private final double agility;
        private final double luck;
        private final long souls;
        private final double movementSpeed;
        private final String attackText;

        Profile(String textureName, int textureWidth, int textureHeight, double health, double attack,
                double defense, double agility, double luck, long souls, double movementSpeed,
                String attackText) {
            this.texture = new ResourceLocation(BlackSouls.MODID, "textures/entity/" + textureName + ".png");
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.health = health;
            this.attack = attack;
            this.defense = defense;
            this.agility = agility;
            this.luck = luck;
            this.souls = souls;
            this.movementSpeed = movementSpeed;
            this.attackText = attackText;
        }

        public ResourceLocation texture() {
            return this.texture;
        }

        public float aspectRatio() {
            return (float) this.textureWidth / this.textureHeight;
        }

        public float worldRenderHeight() {
            return switch (this) {
                case HEADLESS_UNDEAD -> 1.8F;
                case CORRUPT_DOG -> 1.3F;
                case WEREWOLF -> 1.8F;
            };
        }

        public float shadowRadius() {
            return switch (this) {
                case HEADLESS_UNDEAD -> 0.35F;
                case CORRUPT_DOG -> 0.45F;
                case WEREWOLF -> 0.65F;
            };
        }
    }
}

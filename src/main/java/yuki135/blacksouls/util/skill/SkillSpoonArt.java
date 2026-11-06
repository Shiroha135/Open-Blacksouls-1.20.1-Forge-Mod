package yuki135.blacksouls.util.skill;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.handler.StatEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

@SuppressWarnings("removal")
public class SkillSpoonArt extends AbstractOriginalWeaponSkill {
    public SkillSpoonArt() {
        super(Family.SPOON, 5);
    }

    @Override public String getSkillId() { return "bs2_skill_delicious_turtle_soup"; }
    @Override public float getManaCost() { return 0.0F; }
    @Override public int getBaseCooldownTicks() { return 0; }
    @Override public String getTranslationKey() { return "skill.blacksouls.bs2_skill_delicious_turtle_soup.name"; }
    @Override public ChatFormatting getTextColor() { return ChatFormatting.GREEN; }
    @Override public ResourceLocation getIcon() { return new ResourceLocation(BlackSouls.MODID, "textures/gui/skills/delicious_turtle_soup.png"); }

    @Override
    public void execute(ServerPlayer player, BSPlayerStats stats) {
        player.sendSystemMessage(Component.translatable("message.blacksouls.skill.delicious_turtle_soup.use", player.getName().getString()).withStyle(ChatFormatting.WHITE));
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(player, 37);
        playSound(player, BlackSouls.ICE1_EVENT.get(), 0.5F);
        double recovery = stats.getOriginalSpiritRate(23, 2);
        player.heal((float) Math.floor(player.getMaxHealth() * 0.5D * recovery));
        stats.mp = Math.min(stats.maxMp, stats.mp + Math.floor(stats.maxMp * 0.5D * recovery));
        StatEventHandler.syncToClient(player);
    }

    @Override
    public void executeInTurnBattle(ServerPlayer player, BSPlayerStats stats, LivingEntity target) {
        if (!(target instanceof ServerPlayer ally)) {
            execute(player, stats);
            return;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        playAnimation(ally, 37);
        playSound(ally, BlackSouls.ICE1_EVENT.get(), 0.5F);
        double recovery = StatEventHandler.getOriginalSpiritRate(ally, 23, 2);
        ally.heal((float) Math.floor(ally.getMaxHealth() * 0.5D * recovery));
        ally.getCapability(BSPlayerStats.CAPABILITY).ifPresent(allyStats -> {
            allyStats.mp = Math.min(allyStats.maxMp, allyStats.mp + Math.floor(allyStats.maxMp * 0.5D * recovery));
            StatEventHandler.syncToClient(ally);
        });
    }
}

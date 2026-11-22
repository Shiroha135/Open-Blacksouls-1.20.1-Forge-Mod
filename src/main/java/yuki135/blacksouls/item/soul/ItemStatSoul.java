package yuki135.blacksouls.item.soul;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.handler.StatEventHandler;
import yuki135.blacksouls.item.ItemLoreBase;
import yuki135.blacksouls.story.OriginalSenService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemStatSoul extends ItemLoreBase {

    private final double addHp;
    private final double addMp;
    private final double addAtk;
    private final double addDef;
    private final double addMagicAttack;
    private final double addMagicDefense;
    private final double addLuck;
    private final double addSpeed;
    private final int senDelta;

    public ItemStatSoul(Properties properties, double hp, double mp, double atk, double def, double mAtk, double mDef, double luck, double speed) {
        this(properties, hp, mp, atk, def, mAtk, mDef, luck, speed, 0);
    }

    public ItemStatSoul(Properties properties, double hp, double mp, double atk, double def, double mAtk, double mDef, double luck, double speed, int senDelta) {
        super(properties);
        this.addHp = hp;
        this.addMp = mp;
        this.addAtk = atk;
        this.addDef = def;
        this.addMagicAttack = mAtk;
        this.addMagicDefense = mDef;
        this.addLuck = luck;
        this.addSpeed = speed;
        this.senDelta = senDelta;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY).orElse(null);
            if (stats == null) {
                return InteractionResultHolder.fail(stack);
            }
            long changedSen = (long) stats.sen + senDelta;
            if (changedSen < Integer.MIN_VALUE || changedSen > Integer.MAX_VALUE
                    || senDelta != 0 && !(player instanceof ServerPlayer)) {
                return InteractionResultHolder.fail(stack);
            }

            if (addHp > 0) stats.addPermanentStat("HP", addHp);
            if (addMp > 0) stats.addPermanentStat("MP", addMp);
            if (addAtk > 0) stats.addPermanentStat("ATK", addAtk);
            if (addDef > 0) stats.addPermanentStat("DEF", addDef);
            if (addMagicAttack > 0) stats.addPermanentStat("MATK", addMagicAttack);
            if (addMagicDefense > 0) stats.addPermanentStat("MDEF", addMagicDefense);
            if (addLuck > 0) stats.addPermanentStat("LUC", addLuck);
            if (addSpeed > 0) stats.addPermanentStat("SPEED", addSpeed);

            stats.recalculateStats();

            if (addHp > 0) {
                player.heal((float) addHp);
            }
            if (addMp > 0) {
                stats.mp += addMp;
                if (stats.mp > stats.maxMp) {
                    stats.mp = stats.maxMp;
                }
            }

            if (senDelta != 0) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                int before = stats.sen;
                OriginalSenService.writeValue(serverPlayer, changedSen);
                OriginalSenService.afterVariableOperation(serverPlayer, senDelta > 0 ? 1 : 2,
                        Math.abs((long) senDelta), before, changedSen);
            }

            StatEventHandler.applyStats(player);
            StatEventHandler.syncToClient(player);

            player.sendSystemMessage(Component.translatable("message.blacksouls.soul_absorb").withStyle(ChatFormatting.GOLD));

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    BlackSouls.ITEM1_EVENT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, level, tooltip, flagIn);

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.blacksouls.stat_soul.effect").withStyle(ChatFormatting.GOLD));

        if (addHp > 0) tooltip.add(Component.literal(" HP +" + (int) addHp).withStyle(ChatFormatting.RED));
        if (addMp > 0) tooltip.add(Component.literal(" MP +" + (int) addMp).withStyle(ChatFormatting.AQUA));
        if (addAtk > 0) tooltip.add(Component.literal(" ").append(Component.translatable("tooltip.blacksouls.stat.atk")).append(" +" + (int) addAtk).withStyle(ChatFormatting.DARK_RED));
        if (addDef > 0) tooltip.add(Component.literal(" ").append(Component.translatable("tooltip.blacksouls.stat.def")).append(" +" + (int) addDef).withStyle(ChatFormatting.GREEN));
        if (addMagicAttack > 0) tooltip.add(Component.literal(" ").append(Component.translatable("tooltip.blacksouls.stat.matk")).append(" +" + (int) addMagicAttack).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (addMagicDefense > 0) tooltip.add(Component.literal(" ").append(Component.translatable("tooltip.blacksouls.stat.mdef")).append(" +" + (int) addMagicDefense).withStyle(ChatFormatting.BLUE));
        if (addLuck > 0) tooltip.add(Component.literal(" ").append(Component.translatable("tooltip.blacksouls.stat.luc")).append(" +" + (int) addLuck).withStyle(ChatFormatting.YELLOW));
        if (addSpeed > 0) tooltip.add(Component.literal(" ").append(Component.translatable("tooltip.blacksouls.stat.speed")).append(" +" + (int) addSpeed).withStyle(ChatFormatting.WHITE));
        if (senDelta != 0) tooltip.add(Component.literal(" SEN " + (senDelta > 0 ? "+" : "") + senDelta).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}

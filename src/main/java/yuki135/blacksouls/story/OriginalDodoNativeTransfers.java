package yuki135.blacksouls.story;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.util.HokoniwaDestination;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class OriginalDodoNativeTransfers {
    private static final List<NativeCommand> COMMANDS = List.of(
            new NativeCommand(new BlockPos(873, 9, -584), "tp @p 853.5 -59 -622.5", "map037_ev020"),
            new NativeCommand(new BlockPos(853, -61, -624), "tp @p 873.5 11 -584.5", "map063_ev017"));

    private OriginalDodoNativeTransfers() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCommand(CommandEvent event) {
        if (event.isCanceled() || event.getException() != null) return;
        var parsed = event.getParseResults();
        CommandSourceStack source = parsed.getContext().getSource();
        if (source.getEntity() != null || !HokoniwaDestination.DIMENSION.equals(source.getLevel().dimension())) return;
        String command = parsed.getReader().getString();
        NativeCommand nativeCommand = COMMANDS.stream().filter(candidate -> candidate.command().equals(command)
                && source.getPosition().equals(Vec3.atCenterOf(candidate.position()))).findFirst().orElse(null);
        if (nativeCommand == null || !source.getLevel().hasChunkAt(nativeCommand.position())
                || !source.getLevel().getBlockState(nativeCommand.position()).is(Blocks.COMMAND_BLOCK)
                || !(source.getLevel().getBlockEntity(nativeCommand.position()) instanceof CommandBlockEntity block)
                || block.isRemoved() || source.source != block.getCommandBlock()
                || !block.getCommandBlock().getCommand().equals(command)) return;
        try {
            CommandContext<CommandSourceStack> context = parsed.getContext().build(command);
            while (context.getChild() != null) context = context.getChild();
            var players = EntityArgument.getPlayers(context, "targets");
            if (players.size() != 1) return;
            ServerPlayer player = players.iterator().next();
            if (player.serverLevel() != source.getLevel() || player.distanceToSqr(source.getPosition()) > 64.0D) return;
            if (OriginalDodoEntry.claimNative(player, nativeCommand.sourceEvent())) event.setCanceled(true);
        } catch (CommandSyntaxException | RuntimeException exception) {
            BlackSouls.LOGGER.debug("Unable to claim original Dodo native transfer {}", nativeCommand.sourceEvent(), exception);
        }
    }

    private record NativeCommand(BlockPos position, String command, String sourceEvent) {
    }
}

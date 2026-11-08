package yuki135.blacksouls.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.story.OriginalOpeningBonfireFlow;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID, value = Dist.CLIENT)
public final class OriginalOpeningBonfireMask {
    private OriginalOpeningBonfireMask() {
    }

    public static void refresh() {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var pos = OriginalOpeningBonfireFlow.FALLING_BONFIRE;
        var opening=OriginalOpeningController.state();
        boolean arriving=opening!=null&&!opening.preview()&&opening.stage()==yuki135.blacksouls.story.OriginalOpeningProfile.Stage.ARRIVAL_PENDING;
        if (level != null && minecraft.player != null && level.hasChunkAt(pos)
                && OriginalOpeningBonfireFlow.isFallingBonfire(level.dimension(),pos)
                && (arriving||OriginalOpeningBonfireFlow.isHidden(minecraft.player, pos))
                && !level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 19);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) refresh();
    }
}

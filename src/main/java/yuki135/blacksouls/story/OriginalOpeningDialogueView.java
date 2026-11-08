package yuki135.blacksouls.story;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerPlayer;
import yuki135.blacksouls.entity.EntityOpeningActor;

public final class OriginalOpeningDialogueView {
    private OriginalOpeningDialogueView() { }

    public static boolean focus(ServerPlayer player,EntityOpeningActor actor) {
        if(player==null||actor==null||player.connection==null||player.isRemoved()||!player.server.isSameThread()||!player.isAlive()||actor.isRemoved()||!actor.isAlive()
                ||actor.isInvisible()||actor.level()!=player.level()||!player.getUUID().equals(actor.ownerId())
                ||!OriginalOpeningScenes.DIMENSION.equals(player.level().dimension())||player.distanceToSqr(actor)>64D)return false;
        player.lookAt(EntityAnchorArgument.Anchor.EYES,actor,EntityAnchorArgument.Anchor.EYES);
        return true;
    }

    public static boolean focusAlice(ServerPlayer player,int leaseSlot,int command) {
        if(player==null||!OriginalOpeningCoda.aliceChoice(command)||!OriginalOpeningCoda.aliceVisible(command))return false;
        for(var entity:player.serverLevel().getAllEntities()) {
            if(entity instanceof EntityOpeningActor actor&&actor.originId()==0&&actor.leaseSlot()==leaseSlot&&focus(player,actor))return true;
        }
        return false;
    }
}

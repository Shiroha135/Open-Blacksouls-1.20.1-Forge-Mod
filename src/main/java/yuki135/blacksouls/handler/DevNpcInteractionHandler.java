package yuki135.blacksouls.handler;

import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.entity.DialogueResettable;
import yuki135.blacksouls.entity.EntityOriginalNpc;
import yuki135.blacksouls.combat.TurnBattleManager;
import yuki135.blacksouls.item.ItemDevTool;
import yuki135.blacksouls.util.OriginalNpcMmdBindings;
import yuki135.mmdskin.render.entity.CustomEntityPoseProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlackSouls.MODID)
public final class DevNpcInteractionHandler {
    public static boolean isNpc(Entity target) {
        return target instanceof EntityOriginalNpc || target instanceof DialogueResettable;
    }

    public static boolean supportsSitting(Entity target) {
        return target instanceof CustomEntityPoseProvider && (!(target instanceof EntityOriginalNpc npc)
                || OriginalNpcMmdBindings.modelFor(npc.getNpcId(), npc.getRouteCharacterName(), npc.getCharacterIndex()) != null);
    }

    public static boolean erase(ServerPlayer player, Entity target) {
        if (!player.isAlive() || !isNpc(target) || target.isRemoved() || target.level() != player.level()
                || !player.getMainHandItem().is(BlackSouls.DEV_STAT_TOOL.get()) || !player.canReach(target, 0.0D)) {
            return false;
        }
        if (!player.isCreative() || !player.hasPermissions(4)) {
            player.displayClientMessage(Component.translatable("message.blacksouls.dev.no_permission").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (TurnBattleManager.isInBattle(player)
                || target instanceof net.minecraft.world.entity.LivingEntity living && TurnBattleManager.isInBattle(living)) {
            player.displayClientMessage(Component.translatable("message.blacksouls.dev.npc_in_battle").withStyle(ChatFormatting.RED), true);
            return false;
        }
        Component name = target.getDisplayName();
        if (target instanceof net.minecraft.world.entity.PathfinderMob actor) {
            yuki135.blacksouls.story.OriginalNpcService.forgetRemovedActor(actor);
        }
        target.discard();
        player.displayClientMessage(Component.translatable("message.blacksouls.dev.npc_erased", name), true);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity().getMainHandItem().getItem() instanceof ItemDevTool) || !isNpc(event.getTarget())) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            erase(player, event.getTarget());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && !event.getEntity().isShiftKeyDown()
                && event.getItemStack().getItem() instanceof ItemDevTool && supportsSitting(event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
            if (event.getLevel().isClientSide()) {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> ClientHandler.open(event.getTarget()));
            } else if (event.getEntity() instanceof ServerPlayer player
                    && event.getTarget() instanceof EntityOriginalNpc npc) {
                yuki135.blacksouls.story.OriginalNpcService.reconnectDeveloperReplacement(player, npc);
            }
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getEntity().isShiftKeyDown()
                || !event.getItemStack().isEmpty() || !supportsSitting(event.getTarget())
                || !(event.getTarget() instanceof CustomEntityPoseProvider pose)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        if (event.getEntity() instanceof ServerPlayer player && player.isAlive() && event.getTarget().isAlive()
                && player.canReach(event.getTarget(), 0.0D)) {
            pose.setSitting(!pose.isSitting());
            if (pose.isSitting() && event.getTarget() instanceof net.minecraft.world.entity.Mob mob) {
                mob.getNavigation().stop();
                mob.setDeltaMovement(0.0D, mob.getDeltaMovement().y, 0.0D);
            }
        }
    }

    private DevNpcInteractionHandler() {
    }

    private static final class ClientHandler {
        private static void open(Entity target) {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new yuki135.blacksouls.client.gui.GuiNpcModelOffset(target));
        }
    }
}

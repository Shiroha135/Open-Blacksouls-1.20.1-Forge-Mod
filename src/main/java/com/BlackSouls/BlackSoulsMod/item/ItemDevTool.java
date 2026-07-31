package com.BlackSouls.BlackSoulsMod.item;

import com.BlackSouls.BlackSoulsMod.entity.DialogueResettable;
import com.BlackSouls.BlackSoulsMod.entity.EntityRedHood;
import com.BlackSouls.BlackSoulsMod.network.WhiteBearShopService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class ItemDevTool extends Item {

    public ItemDevTool(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        if (level.getBlockState(pos).is(BlockTags.CAMPFIRES)) {
            return true;
        }
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock());
        return blockId != null
                && blockId.getNamespace().equals("blacksouls2")
                && (blockId.getPath().equals("acquisition_light")
                || blockId.getPath().equals("advisory_message")
                || blockId.getPath().equals("falling_room_corpse")
                || blockId.getPath().equals("bai_xiong_mao_shop"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (!player.isShiftKeyDown() || !ClientHandler.isCompatEditorTarget()) {
                    ClientHandler.openGui();
                }
            });
        }
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (hand == InteractionHand.MAIN_HAND && target instanceof EntityRedHood redHood) {
            if (player.isShiftKeyDown()) {
                if (player.level().isClientSide) {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            ClientHandler.openRedHoodAnimationEditor(redHood.getId(), redHood.getMmdAnimation()));
                }
            } else if (!player.level().isClientSide) {
                redHood.facePlayer(player);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        if (player.isShiftKeyDown() && target instanceof DialogueResettable resettable) {
            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                resettable.resetDialogue(serverPlayer);
                serverPlayer.displayClientMessage(
                        Component.translatable("message.blacksouls.dev.dialogue_reset.entity", target.getDisplayName()),
                        true
                );
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(
                context.getLevel().getBlockState(context.getClickedPos()).getBlock()
        );
        if (blockId != null
                && blockId.getNamespace().equals("blacksouls2")
                && blockId.getPath().equals("bai_xiong_mao_shop")) {
            if (!context.getLevel().isClientSide && player instanceof ServerPlayer serverPlayer) {
                WhiteBearShopService.resetDialogue(serverPlayer);
                serverPlayer.displayClientMessage(
                        Component.translatable("message.blacksouls.dev.dialogue_reset.white_bear"),
                        true
                );
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }

    private static class ClientHandler {
        private static boolean isCompatEditorTarget() {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.level == null || !(minecraft.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)) {
                return false;
            }
            net.minecraft.resources.ResourceLocation blockId = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(
                    minecraft.level.getBlockState(hit.getBlockPos()).getBlock()
            );
            return minecraft.level.getBlockState(hit.getBlockPos()).is(net.minecraft.tags.BlockTags.CAMPFIRES)
                    || blockId != null
                    && blockId.getNamespace().equals("blacksouls2")
                    && (blockId.getPath().equals("acquisition_light")
                    || blockId.getPath().equals("advisory_message")
                    || blockId.getPath().equals("falling_room_corpse"));
        }

        private static void openGui() {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.BlackSouls.BlackSoulsMod.client.gui.GuiDevPanel()
            );
        }

        private static void openRedHoodAnimationEditor(int entityId, String currentAnimation) {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.BlackSouls.BlackSoulsMod.client.gui.GuiRedHoodAnimationEditor(
                            entityId,
                            currentAnimation
                    )
            );
        }
    }
}

package com.BlackSouls.BlackSoulsMod.mixin.client;

import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBounds;
import com.BlackSouls.BlackSoulsMod.compat.scene.SceneSpawnerBossState;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundSetSceneSpawnerBoundsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;

@Pseudo
@Mixin(targets = "cn.zhenhongliya.blacksouls2compat.client.SceneSpawnerScreen", remap = false)
public abstract class SceneSpawnerScreenMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    @Unique
    private static final int BLACKSOULS_NORMAL_COLOR = 0xE0E0E0;
    @Unique
    private static final int BLACKSOULS_INVALID_COLOR = 0xFF5555;

    @Shadow(remap = false)
    private EditBox sceneIdBox;

    @Unique
    private EditBox blacksouls$rangeXBox;
    @Unique
    private EditBox blacksouls$rangeZBox;
    @Unique
    private CycleButton<Boolean> blacksouls$bossModeButton;

    private SceneSpawnerScreenMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"), remap = false)
    private void blacksouls$addBoundsFields(CallbackInfo callback) {
        sceneIdBox.setWidth(126);
        int rangeX = SceneSpawnerBounds.DEFAULT_RANGE;
        int rangeZ = SceneSpawnerBounds.DEFAULT_RANGE;
        boolean bossMode = false;
        BlockPos pos = blacksouls$getSpawnerPos();
        Minecraft minecraft = Minecraft.getInstance();
        if (pos != null && minecraft.level != null) {
            BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
            if (blockEntity instanceof SceneSpawnerBounds bounds) {
                rangeX = bounds.blacksouls$getRangeX();
                rangeZ = bounds.blacksouls$getRangeZ();
            }
            if (blockEntity instanceof SceneSpawnerBossState bossState) {
                bossMode = bossState.blacksouls$isBossMode();
            }
        }

        blacksouls$rangeXBox = blacksouls$createRangeBox(leftPos + 154, topPos + 32,
                "gui.blacksouls.scene_spawner.range_x", rangeX);
        blacksouls$rangeZBox = blacksouls$createRangeBox(leftPos + 186, topPos + 32,
                "gui.blacksouls.scene_spawner.range_z", rangeZ);
        addRenderableWidget(blacksouls$rangeXBox);
        addRenderableWidget(blacksouls$rangeZBox);
        blacksouls$compactActionButtons();
        blacksouls$bossModeButton = CycleButton.onOffBuilder(bossMode).create(
                leftPos + 20, topPos + 98, 58, 20,
                Component.translatable("gui.blacksouls.scene_spawner.boss"),
                (button, value) -> {
                });
        addRenderableWidget(blacksouls$bossModeButton);
    }

    @Unique
    private void blacksouls$compactActionButtons() {
        for (var renderable : renderables) {
            if (!(renderable instanceof Button button) || button.getY() != topPos + 98) {
                continue;
            }
            if (button.getX() == leftPos + 20) {
                button.setX(leftPos + 84);
                button.setWidth(62);
            } else if (button.getX() == leftPos + 118) {
                button.setX(leftPos + 150);
                button.setWidth(60);
            }
        }
    }

    @Unique
    private EditBox blacksouls$createRangeBox(int x, int y, String narrationKey, int value) {
        EditBox box = new EditBox(font, x, y, 24, 20, Component.translatable(narrationKey));
        box.setMaxLength(3);
        box.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        box.setValue(Integer.toString(value));
        return box;
    }

    @Inject(method = {"containerTick", "m_181908_"}, at = @At("TAIL"), remap = false)
    private void blacksouls$tickBoundsFields(CallbackInfo callback) {
        if (blacksouls$rangeXBox != null) {
            blacksouls$rangeXBox.tick();
        }
        if (blacksouls$rangeZBox != null) {
            blacksouls$rangeZBox.tick();
        }
    }

    @Inject(method = {"renderLabels", "m_280003_"}, at = @At("TAIL"), remap = false)
    private void blacksouls$renderBoundsLabels(GuiGraphics graphics, int mouseX, int mouseY,
                                                CallbackInfo callback) {
        graphics.drawString(font, Component.translatable("gui.blacksouls.scene_spawner.range"),
                154, 20, 0xA0A0A0, false);
        graphics.drawString(font, Component.literal("x"), 180, 38, 0xE0E0E0, false);
    }

    @Inject(method = "save", at = @At("HEAD"), cancellable = true, remap = false)
    private void blacksouls$validateBounds(CallbackInfo callback) {
        Integer rangeX = blacksouls$parseRange(blacksouls$rangeXBox);
        Integer rangeZ = blacksouls$parseRange(blacksouls$rangeZBox);
        if (rangeX == null || rangeZ == null) {
            callback.cancel();
        }
    }

    @Inject(method = "save", at = @At("TAIL"), remap = false)
    private void blacksouls$saveBounds(CallbackInfo callback) {
        BlockPos pos = blacksouls$getSpawnerPos();
        Integer rangeX = blacksouls$parseRange(blacksouls$rangeXBox);
        Integer rangeZ = blacksouls$parseRange(blacksouls$rangeZBox);
        if (pos != null && rangeX != null && rangeZ != null) {
            NetworkHandler.sendToServer(new ServerboundSetSceneSpawnerBoundsPacket(
                    pos, rangeX, rangeZ,
                    blacksouls$bossModeButton != null && blacksouls$bossModeButton.getValue()));
        }
    }

    @Unique
    private Integer blacksouls$parseRange(EditBox box) {
        if (box == null) {
            return null;
        }
        try {
            int value = Integer.parseInt(box.getValue());
            if (value >= 1 && value <= SceneSpawnerBounds.MAX_RANGE) {
                box.setTextColor(BLACKSOULS_NORMAL_COLOR);
                return value;
            }
        } catch (NumberFormatException ignored) {
        }
        box.setTextColor(BLACKSOULS_INVALID_COLOR);
        return null;
    }

    @Unique
    private BlockPos blacksouls$getSpawnerPos() {
        try {
            Object value = menu.getClass().getMethod("getPos").invoke(menu);
            return value instanceof BlockPos pos ? pos : null;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            return null;
        }
    }
}

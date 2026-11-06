package com.BlackSouls.BlackSoulsMod.client.gui;

import com.BlackSouls.BlackSoulsMod.client.render.DevGlassRenderer;
import com.BlackSouls.BlackSoulsMod.network.NetworkHandler;
import com.BlackSouls.BlackSoulsMod.network.packets.ServerboundNpcModelOffsetPacket;
import com.shiroha.mmdskin.render.entity.CustomEntityModelOffsetProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.UUID;

public final class GuiNpcModelOffset extends Screen {
    private final int entityId;
    private final UUID entityUuid;
    private final Component name;
    private Vec3 original;
    private final EditBox[] fields = new EditBox[3];
    private int left;
    private int top;
    private int panelWidth;
    private Button save;
    private boolean submitted;

    public GuiNpcModelOffset(Entity target) {
        super(Component.translatable("gui.blacksouls.dev.model_offset.title"));
        this.entityId = target.getId();
        this.entityUuid = target.getUUID();
        this.name = target.getDisplayName();
        this.original = ((CustomEntityModelOffsetProvider) target).getMmdModelOffset();
    }

    @Override
    protected void init() {
        Vec3 value = fields[0] == null ? original : values();
        if (value == null) value = original;
        panelWidth = Math.min(238, width - 16);
        left = width - panelWidth - 8;
        top = Math.max(8, (height - 236) / 2);
        double[] initial = {value.x, value.y, value.z};
        for (int i = 0; i < fields.length; i++) {
            int axis = i;
            int y = top + 64 + i * 38;
            fields[i] = new EditBox(font, left + 72, y, panelWidth - 118, 18, Component.literal("XYZ".substring(i, i + 1)));
            fields[i].setBordered(false);
            fields[i].setMaxLength(20);
            fields[i].setValue(format(initial[i]));
            fields[i].setResponder(ignored -> preview());
            addRenderableWidget(fields[i]);
            addRenderableWidget(button(left + 46, y - 3, 20, 24, Component.literal("−"), () -> adjust(axis, -0.05D)));
            addRenderableWidget(button(left + panelWidth - 40, y - 3, 24, 24, Component.literal("+"), () -> adjust(axis, 0.05D)));
        }
        int half = (panelWidth - 42) / 2;
        save = addRenderableWidget(button(left + 16, top + 184, half, 24,
                Component.translatable("gui.blacksouls.dev.model_offset.save"), this::submit));
        addRenderableWidget(button(left + 26 + half, top + 184, half, 24,
                Component.translatable("gui.blacksouls.dev.model_offset.zero"), () -> {
                    for (EditBox field : fields) field.setValue("0");
                }));
        preview();
    }

    private Button button(int x, int y, int width, int height, Component label, Runnable action) {
        return new Button(x, y, width, height, label, ignored -> action.run(), narration -> narration.get()) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                DevGlassRenderer.fill(graphics, getX(), getY(), getWidth(), getHeight(), 6.0F,
                        isHoveredOrFocused() && active ? 0x804C91BA : 0x4824394A);
                graphics.drawString(font, getMessage(), getX() + (getWidth() - font.width(getMessage())) / 2,
                        getY() + (getHeight() - font.lineHeight) / 2, active ? 0xFFE8F5FF : 0xFF7D8C9D, false);
            }
        };
    }

    private Entity target() {
        Entity target = minecraft == null || minecraft.level == null ? null : minecraft.level.getEntity(entityId);
        return target != null && target.getUUID().equals(entityUuid) && !target.isRemoved() ? target : null;
    }

    private Vec3 values() {
        try {
            Vec3 result = new Vec3(Double.parseDouble(fields[0].getValue()), Double.parseDouble(fields[1].getValue()), Double.parseDouble(fields[2].getValue()));
            return CustomEntityModelOffsetProvider.isValidOffset(result) ? result : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void preview() {
        Vec3 value = values();
        if (save != null) save.active = value != null;
        if (value != null && target() instanceof CustomEntityModelOffsetProvider provider) provider.setMmdModelOffset(value);
    }

    private void adjust(int axis, double amount) {
        try {
            double value = Math.max(-4.0D, Math.min(4.0D, Double.parseDouble(fields[axis].getValue()) + amount));
            if (Double.isFinite(value)) fields[axis].setValue(format(value));
        } catch (NumberFormatException ignored) {
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void submit() {
        Vec3 value = values();
        if (value == null || target() == null) return;
        NetworkHandler.sendToServer(new ServerboundNpcModelOffsetPacket(entityId, entityUuid, value));
        save.active = false;
    }

    public void receive(UUID uuid, Vec3 offset, boolean accepted) {
        if (!entityUuid.equals(uuid)) return;
        original = offset;
        if (accepted) {
            submitted = true;
            onClose();
        } else {
            fields[0].setValue(format(offset.x));
            fields[1].setValue(format(offset.y));
            fields[2].setValue(format(offset.z));
            preview();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        DevGlassRenderer.beginFrame(graphics);
        DevGlassRenderer.panel(graphics, left, top, panelWidth, 236, 12, 0.94F, true);
        graphics.drawString(font, title, left + 16, top + 14, 0xFFF3FAFF, false);
        graphics.drawString(font, font.plainSubstrByWidth(name.getString(), panelWidth - 32), left + 16, top + 32, 0xFF96BCD4, false);
        for (int i = 0; i < fields.length; i++) {
            int y = top + 64 + i * 38;
            graphics.drawString(font, "XYZ".substring(i, i + 1), left + 16, y + 2, 0xFFC8E8F7, false);
            DevGlassRenderer.fill(graphics, fields[i].getX() - 4, y - 3, fields[i].getWidth() + 8, 24, 6, 0x45162534);
        }
        graphics.drawString(font, Component.translatable("gui.blacksouls.dev.model_offset.hint"), left + 16, top + 218, 0xFFA4B7C8, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        if (target() == null) onClose();
    }

    @Override
    public void removed() {
        if (!submitted && target() instanceof CustomEntityModelOffsetProvider provider) provider.setMmdModelOffset(original);
        DevGlassRenderer.release();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

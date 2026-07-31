package com.shiroha.mmdskin.forge.config;

import com.shiroha.mmdskin.asset.catalog.ModelCatalogEntry;
import com.shiroha.mmdskin.config.ConfigData;
import com.shiroha.mmdskin.config.PhysicsConfigSnapshot;
import com.shiroha.mmdskin.config.UIConstants;
import com.shiroha.mmdskin.render.bootstrap.ClientRenderRuntime;
import com.shiroha.mmdskin.render.entity.MobReplacementTargets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

public final class ModConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int TAB_HEIGHT = 20;
    private static final int TAB_GAP = 4;
    private static final int PAGE_MARGIN = 10;

    private final Screen parent;
    private final ConfigData liveData;
    private final ConfigData data;
    private Category category = Category.RENDER;
    private List<SettingRow> rows = List.of();
    private int firstRow;
    private int visibleRows;
    private int listTop;
    private int listBottom;
    private int listLeft;
    private int listWidth;
    private Component hoveredTooltip;

    private ModConfigScreen(Screen parent) {
        super(Component.translatable("gui.mmdskin.mod_settings.title"));
        this.parent = parent;
        this.liveData = MmdSkinConfig.getData();
        this.data = new ConfigData();
        this.liveData.copyTo(this.data);
    }

    public static Screen create(Screen parent) {
        return new ModConfigScreen(parent);
    }

    @Override
    protected void init() {
        rows = createRows(category);
        firstRow = Mth.clamp(firstRow, 0, maxFirstRow());
        refreshWidgets();
    }

    private void refreshWidgets() {
        clearWidgets();

        int availableWidth = Math.max(1, this.width - PAGE_MARGIN * 2);
        int columns = Math.min(Category.values().length, Math.max(2, (availableWidth + TAB_GAP) / 96));
        int tabWidth = (availableWidth - TAB_GAP * (columns - 1)) / columns;
        int tabRows = (Category.values().length + columns - 1) / columns;

        for (int index = 0; index < Category.values().length; index++) {
            Category tab = Category.values()[index];
            int column = index % columns;
            int row = index / columns;
            Button button = Button.builder(Component.translatable(tab.labelKey), ignored -> selectCategory(tab))
                    .bounds(PAGE_MARGIN + column * (tabWidth + TAB_GAP), 24 + row * (TAB_HEIGHT + 2), tabWidth, TAB_HEIGHT)
                    .build();
            button.active = tab != category;
            addRenderableWidget(button);
        }

        listTop = 24 + tabRows * (TAB_HEIGHT + 2) + 5;
        listBottom = Math.max(listTop + ROW_HEIGHT, this.height - 32);
        listLeft = PAGE_MARGIN;
        listWidth = availableWidth;
        visibleRows = Math.max(1, (listBottom - listTop - 4) / ROW_HEIGHT);
        firstRow = Mth.clamp(firstRow, 0, maxFirstRow());

        int end = Math.min(rows.size(), firstRow + visibleRows);
        for (int index = firstRow; index < end; index++) {
            int y = listTop + 2 + (index - firstRow) * ROW_HEIGHT;
            rows.get(index).addWidgets(this, listLeft + 5, y, listWidth - 14);
        }

        int buttonWidth = Math.min(150, Math.max(90, (this.width - 28) / 2));
        int center = this.width / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> saveAndClose())
                .bounds(center - buttonWidth - 2, this.height - 26, buttonWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), ignored -> onClose())
                .bounds(center + 2, this.height - 26, buttonWidth, 20)
                .build());
    }

    private void selectCategory(Category selected) {
        if (category == selected) {
            return;
        }
        category = selected;
        firstRow = 0;
        rows = createRows(category);
        refreshWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        graphics.fill(listLeft, listTop, listLeft + listWidth, listBottom, 0x90000000);

        hoveredTooltip = null;
        int end = Math.min(rows.size(), firstRow + visibleRows);
        for (int index = firstRow; index < end; index++) {
            int y = listTop + 2 + (index - firstRow) * ROW_HEIGHT;
            SettingRow row = rows.get(index);
            if ((index - firstRow & 1) == 0) {
                graphics.fill(listLeft + 2, y - 1, listLeft + listWidth - 5, y + 21, 0x283F3F3F);
            }
            row.render(this, graphics, listLeft + 5, y, listWidth - 14, mouseX, mouseY);
            if (row.isLabelHovered(this, listLeft + 5, y, listWidth - 14, mouseX, mouseY)) {
                hoveredTooltip = row.tooltip;
            }
        }

        renderScrollbar(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hoveredTooltip != null) {
            graphics.renderTooltip(this.font, hoveredTooltip, mouseX, mouseY);
        }
    }

    private void renderScrollbar(GuiGraphics graphics) {
        if (rows.size() <= visibleRows) {
            return;
        }
        int trackX = listLeft + listWidth - 6;
        int trackTop = listTop + 2;
        int trackHeight = listBottom - listTop - 4;
        int thumbHeight = Math.max(12, trackHeight * visibleRows / rows.size());
        int travel = trackHeight - thumbHeight;
        int thumbY = trackTop + Math.round(travel * (firstRow / (float) maxFirstRow()));
        graphics.fill(trackX, trackTop, trackX + 3, trackTop + trackHeight, 0x60404040);
        graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFFD0D0D0);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= listTop && mouseY <= listBottom) {
            int previous = firstRow;
            firstRow = Mth.clamp(firstRow + (delta > 0.0 ? -1 : 1), 0, maxFirstRow());
            if (firstRow != previous) {
                refreshWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && rows.size() > visibleRows
                && mouseX >= listLeft + listWidth - 9 && mouseX <= listLeft + listWidth
                && mouseY >= listTop && mouseY <= listBottom) {
            double ratio = Mth.clamp((mouseY - listTop) / Math.max(1.0, listBottom - listTop), 0.0, 1.0);
            firstRow = Mth.clamp((int) Math.round(ratio * maxFirstRow()), 0, maxFirstRow());
            refreshWidgets();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            int direction = keyCode == GLFW.GLFW_KEY_PAGE_UP ? -1 : 1;
            firstRow = Mth.clamp(firstRow + direction * visibleRows, 0, maxFirstRow());
            refreshWidgets();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int maxFirstRow() {
        return Math.max(0, rows.size() - Math.max(1, visibleRows));
    }

    private void saveAndClose() {
        data.copyTo(liveData);
        saveConfig(liveData);
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private List<SettingRow> createRows(Category selected) {
        List<SettingRow> result = new ArrayList<>();
        switch (selected) {
            case RENDER -> {
                result.add(toggle("gui.mmdskin.mod_settings.opengl_lighting", "gui.mmdskin.mod_settings.opengl_lighting.tooltip", () -> data.openGLEnableLighting, value -> data.openGLEnableLighting = value));
                result.add(toggle("gui.mmdskin.mod_settings.mmd_shader", "gui.mmdskin.mod_settings.mmd_shader.tooltip", () -> data.mmdShaderEnabled, value -> data.mmdShaderEnabled = value));
                result.add(toggle("gui.mmdskin.mod_settings.first_person_model", "gui.mmdskin.mod_settings.first_person_model.tooltip", () -> data.firstPersonModelEnabled, value -> data.firstPersonModelEnabled = value));
                result.add(slider("gui.mmdskin.mod_settings.first_person_camera_forward_offset", "gui.mmdskin.mod_settings.first_person_camera_forward_offset.tooltip", -100, 500, () -> Math.round(data.firstPersonCameraForwardOffset * 1000.0F), value -> data.firstPersonCameraForwardOffset = value / 1000.0F, value -> decimal(value / 1000.0F, 3)));
                result.add(slider("gui.mmdskin.mod_settings.first_person_camera_vertical_offset", "gui.mmdskin.mod_settings.first_person_camera_vertical_offset.tooltip", -500, 500, () -> Math.round(data.firstPersonCameraVerticalOffset * 1000.0F), value -> data.firstPersonCameraVerticalOffset = value / 1000.0F, value -> decimal(value / 1000.0F, 3)));
            }
            case PERFORMANCE -> {
                result.add(slider("gui.mmdskin.mod_settings.model_pool_max", "gui.mmdskin.mod_settings.model_pool_max.tooltip", 5, 100, () -> data.modelPoolMaxCount, value -> data.modelPoolMaxCount = value, ModConfigScreen::number));
                result.add(toggle("gui.mmdskin.mod_settings.gpu_skinning", "gui.mmdskin.mod_settings.gpu_skinning.tooltip", () -> data.gpuSkinningEnabled, value -> data.gpuSkinningEnabled = value));
                result.add(toggle("gui.mmdskin.mod_settings.gpu_morph", "gui.mmdskin.mod_settings.gpu_morph.tooltip", () -> data.gpuMorphEnabled, value -> data.gpuMorphEnabled = value));
                result.add(slider("gui.mmdskin.mod_settings.max_bones", "gui.mmdskin.mod_settings.max_bones.tooltip", 512, 4096, () -> data.maxBones, value -> data.maxBones = value, ModConfigScreen::number));
                result.add(slider("gui.mmdskin.mod_settings.texture_cache_budget", "gui.mmdskin.mod_settings.texture_cache_budget.tooltip", 64, 1024, () -> data.textureCacheBudgetMB, value -> data.textureCacheBudgetMB = value, value -> Component.literal(value + " MB")));
                result.add(slider("gui.mmdskin.mod_settings.max_visible_models", "gui.mmdskin.mod_settings.max_visible_models.tooltip", 1, 50, () -> data.maxVisibleModelsPerFrame, value -> data.maxVisibleModelsPerFrame = value, ModConfigScreen::number));
            }
            case MME -> {
                result.add(toggle("gui.mmdskin.mod_settings.mme_enabled", "gui.mmdskin.mod_settings.mme_enabled.tooltip", () -> data.mmeRenderingEnabled, value -> data.mmeRenderingEnabled = value));
                result.add(slider("gui.mmdskin.mod_settings.mme_exposure", "gui.mmdskin.mod_settings.mme_exposure.tooltip", 50, 200, () -> Math.round(data.mmeExposure * 100.0F), value -> data.mmeExposure = value / 100.0F, ModConfigScreen::percent));
                result.add(slider("gui.mmdskin.mod_settings.mme_roughness", "gui.mmdskin.mod_settings.mme_roughness.tooltip", 0, 100, () -> Math.round(data.mmeRoughness * 100.0F), value -> data.mmeRoughness = value / 100.0F, ModConfigScreen::percent));
                result.add(slider("gui.mmdskin.mod_settings.mme_specular", "gui.mmdskin.mod_settings.mme_specular.tooltip", 0, 200, () -> Math.round(data.mmeSpecularIntensity * 100.0F), value -> data.mmeSpecularIntensity = value / 100.0F, ModConfigScreen::percent));
                result.add(slider("gui.mmdskin.mod_settings.mme_soft_shadow", "gui.mmdskin.mod_settings.mme_soft_shadow.tooltip", 0, 100, () -> Math.round(data.mmeSoftShadow * 100.0F), value -> data.mmeSoftShadow = value / 100.0F, ModConfigScreen::percent));
                result.add(slider("gui.mmdskin.mod_settings.mme_rim_power", "gui.mmdskin.mod_settings.mme_rim_power.tooltip", 5, 80, () -> Math.round(data.mmeRimPower * 10.0F), value -> data.mmeRimPower = value / 10.0F, value -> decimal(value / 10.0F, 1)));
                result.add(slider("gui.mmdskin.mod_settings.mme_rim_intensity", "gui.mmdskin.mod_settings.mme_rim_intensity.tooltip", 0, 100, () -> Math.round(data.mmeRimIntensity * 100.0F), value -> data.mmeRimIntensity = value / 100.0F, ModConfigScreen::percent));
                result.add(slider("gui.mmdskin.mod_settings.mme_subsurface", "gui.mmdskin.mod_settings.mme_subsurface.tooltip", 0, 100, () -> Math.round(data.mmeSubsurface * 100.0F), value -> data.mmeSubsurface = value / 100.0F, ModConfigScreen::percent));
                result.add(slider("gui.mmdskin.mod_settings.mme_hair_anisotropy", "gui.mmdskin.mod_settings.mme_hair_anisotropy.tooltip", 0, 100, () -> Math.round(data.mmeHairAnisotropy * 100.0F), value -> data.mmeHairAnisotropy = value / 100.0F, ModConfigScreen::percent));
                result.add(slider("gui.mmdskin.mod_settings.mme_fill_light", "gui.mmdskin.mod_settings.mme_fill_light.tooltip", 0, 100, () -> Math.round(data.mmeFillLight * 100.0F), value -> data.mmeFillLight = value / 100.0F, ModConfigScreen::percent));
            }
            case TOON -> {
                result.add(toggle("gui.mmdskin.mod_settings.toon_enabled", "gui.mmdskin.mod_settings.toon_enabled.tooltip", () -> data.toonRenderingEnabled, value -> data.toonRenderingEnabled = value));
                result.add(slider("gui.mmdskin.mod_settings.toon_levels", "gui.mmdskin.mod_settings.toon_levels.tooltip", 2, 5, () -> data.toonLevels, value -> data.toonLevels = value, ModConfigScreen::number));
                result.add(slider("gui.mmdskin.mod_settings.toon_rim_power", "gui.mmdskin.mod_settings.toon_rim_power.tooltip", 10, 100, () -> Math.round(data.toonRimPower * 10.0F), value -> data.toonRimPower = value / 10.0F, value -> decimal(value / 10.0F, 1)));
                result.add(slider("gui.mmdskin.mod_settings.toon_rim_intensity", "gui.mmdskin.mod_settings.toon_rim_intensity.tooltip", 0, 100, () -> Math.round(data.toonRimIntensity * 100.0F), value -> data.toonRimIntensity = value / 100.0F, ModConfigScreen::percent));
                result.add(slider("gui.mmdskin.mod_settings.toon_shadow_r", "gui.mmdskin.mod_settings.toon_shadow.tooltip", 0, 100, () -> Math.round(data.toonShadowR * 100.0F), value -> data.toonShadowR = value / 100.0F, ModConfigScreen::percent));
                result.add(slider("gui.mmdskin.mod_settings.toon_shadow_g", "gui.mmdskin.mod_settings.toon_shadow.tooltip", 0, 100, () -> Math.round(data.toonShadowG * 100.0F), value -> data.toonShadowG = value / 100.0F, ModConfigScreen::percent));
                result.add(slider("gui.mmdskin.mod_settings.toon_shadow_b", "gui.mmdskin.mod_settings.toon_shadow.tooltip", 0, 100, () -> Math.round(data.toonShadowB * 100.0F), value -> data.toonShadowB = value / 100.0F, ModConfigScreen::percent));
                result.add(slider("gui.mmdskin.mod_settings.toon_specular_power", "gui.mmdskin.mod_settings.toon_specular_power.tooltip", 1, 128, () -> Math.round(data.toonSpecularPower), value -> data.toonSpecularPower = value, ModConfigScreen::number));
                result.add(slider("gui.mmdskin.mod_settings.toon_specular_intensity", "gui.mmdskin.mod_settings.toon_specular_intensity.tooltip", 0, 100, () -> Math.round(data.toonSpecularIntensity * 100.0F), value -> data.toonSpecularIntensity = value / 100.0F, ModConfigScreen::percent));
                result.add(toggle("gui.mmdskin.mod_settings.toon_outline", "gui.mmdskin.mod_settings.toon_outline.tooltip", () -> data.toonOutlineEnabled, value -> data.toonOutlineEnabled = value));
                result.add(slider("gui.mmdskin.mod_settings.toon_outline_width", "gui.mmdskin.mod_settings.toon_outline_width.tooltip", 1, 100, () -> Math.round(data.toonOutlineWidth * 1000.0F), value -> data.toonOutlineWidth = value / 1000.0F, value -> decimal(value / 1000.0F, 3)));
                result.add(slider("gui.mmdskin.mod_settings.toon_outline_r", "gui.mmdskin.mod_settings.toon_outline_color.tooltip", 0, 100, () -> Math.round(data.toonOutlineR * 100.0F), value -> data.toonOutlineR = value / 100.0F, ModConfigScreen::percent));
                result.add(slider("gui.mmdskin.mod_settings.toon_outline_g", "gui.mmdskin.mod_settings.toon_outline_color.tooltip", 0, 100, () -> Math.round(data.toonOutlineG * 100.0F), value -> data.toonOutlineG = value / 100.0F, ModConfigScreen::percent));
                result.add(slider("gui.mmdskin.mod_settings.toon_outline_b", "gui.mmdskin.mod_settings.toon_outline_color.tooltip", 0, 100, () -> Math.round(data.toonOutlineB * 100.0F), value -> data.toonOutlineB = value / 100.0F, ModConfigScreen::percent));
            }
            case PHYSICS -> {
                result.add(toggle("gui.mmdskin.mod_settings.physics_enabled", "gui.mmdskin.mod_settings.physics_enabled.tooltip", () -> data.physicsEnabled, value -> data.physicsEnabled = value));
                result.add(slider("gui.mmdskin.mod_settings.physics_gravity", "gui.mmdskin.mod_settings.physics_gravity.tooltip", 10, 200, () -> Math.round(-data.physicsGravityY), value -> data.physicsGravityY = -value, ModConfigScreen::number));
                result.add(slider("gui.mmdskin.mod_settings.physics_fps", "gui.mmdskin.mod_settings.physics_fps.tooltip", 30, 120, () -> Math.round(data.physicsFps), value -> data.physicsFps = value, ModConfigScreen::number));
                result.add(slider("gui.mmdskin.mod_settings.physics_substeps", "gui.mmdskin.mod_settings.physics_substeps.tooltip", 1, 10, () -> data.physicsMaxSubstepCount, value -> data.physicsMaxSubstepCount = value, ModConfigScreen::number));
                result.add(slider("gui.mmdskin.mod_settings.physics_inertia", "gui.mmdskin.mod_settings.physics_inertia.tooltip", 0, 300, () -> Math.round(data.physicsInertiaStrength * 100.0F), value -> data.physicsInertiaStrength = value / 100.0F, ModConfigScreen::percent));
                result.add(slider("gui.mmdskin.mod_settings.physics_max_linear_velocity", "gui.mmdskin.mod_settings.physics_max_linear_velocity.tooltip", 0, 100, () -> Math.round(data.physicsMaxLinearVelocity), value -> data.physicsMaxLinearVelocity = value, ModConfigScreen::number));
                result.add(slider("gui.mmdskin.mod_settings.physics_max_angular_velocity", "gui.mmdskin.mod_settings.physics_max_angular_velocity.tooltip", 0, 100, () -> Math.round(data.physicsMaxAngularVelocity), value -> data.physicsMaxAngularVelocity = value, ModConfigScreen::number));
                result.add(toggle("gui.mmdskin.mod_settings.physics_joints_enabled", "gui.mmdskin.mod_settings.physics_joints_enabled.tooltip", () -> data.physicsJointsEnabled, value -> data.physicsJointsEnabled = value));
                result.add(toggle("gui.mmdskin.mod_settings.physics_kinematic_filter", "gui.mmdskin.mod_settings.physics_kinematic_filter.tooltip", () -> data.physicsKinematicFilter, value -> data.physicsKinematicFilter = value));
                result.add(toggle("gui.mmdskin.mod_settings.physics_debug_log", "gui.mmdskin.mod_settings.physics_debug_log.tooltip", () -> data.physicsDebugLog, value -> data.physicsDebugLog = value));
                result.add(slider("gui.mmdskin.mod_settings.max_physics_models", "gui.mmdskin.mod_settings.max_physics_models.tooltip", 1, 50, () -> data.maxPhysicsModelsPerFrame, value -> data.maxPhysicsModelsPerFrame = value, ModConfigScreen::number));
            }
            case DEBUG -> result.add(toggle("gui.mmdskin.mod_settings.debug_hud", "gui.mmdskin.mod_settings.debug_hud.tooltip", () -> data.debugHudEnabled, value -> data.debugHudEnabled = value));
            case VR -> {
                result.add(toggle("gui.mmdskin.mod_settings.vr_enabled", "gui.mmdskin.mod_settings.vr_enabled.tooltip", () -> data.vrEnabled, value -> data.vrEnabled = value));
                result.add(slider("gui.mmdskin.mod_settings.vr_arm_ik_strength", "gui.mmdskin.mod_settings.vr_arm_ik_strength.tooltip", 0, 100, () -> Math.round(data.vrArmIKStrength * 100.0F), value -> data.vrArmIKStrength = value / 100.0F, ModConfigScreen::percent));
            }
            case MOB -> {
                for (MobReplacementTargets.Target target : MobReplacementTargets.all()) {
                    result.add(new MobSettingRow(target));
                }
            }
        }
        return result;
    }

    private static SettingRow toggle(String labelKey, String tooltipKey, BooleanSupplier getter, Consumer<Boolean> setter) {
        return new BooleanSettingRow(Component.translatable(labelKey), Component.translatable(tooltipKey), getter, setter);
    }

    private static SettingRow slider(String labelKey, String tooltipKey, int min, int max, IntSupplier getter, IntConsumer setter, IntFunction<Component> formatter) {
        return new SliderSettingRow(Component.translatable(labelKey), Component.translatable(tooltipKey), min, max, getter, setter, formatter);
    }

    private static Component decimal(float value, int places) {
        return Component.literal(String.format(Locale.ROOT, "%." + places + "f", value));
    }

    private static Component percent(int value) {
        return Component.literal(value + "%");
    }

    private static Component number(int value) {
        return Component.literal(Integer.toString(value));
    }

    static void saveConfig(ConfigData data) {
        cleanupInvalidMobReplacements(data);
        MmdSkinConfig.save();
        ClientRenderRuntime.get().renderBackendSettings().setGpuSkinningEnabled(data.gpuSkinningEnabled);
        ClientRenderRuntime.get().renderBackendSettings().setShaderEnabled(data.mmdShaderEnabled);
        ClientRenderRuntime.get().modelRepository().reloadAll();
        ClientRenderRuntime.get().applyPhysicsConfig(PhysicsConfigSnapshot.from(data));
    }

    static String getMobReplacementValue(ConfigData data, String entityTypeId) {
        String currentValue = data.mobModelReplacements.getOrDefault(entityTypeId, UIConstants.DEFAULT_MODEL_NAME);
        return currentValue == null || currentValue.isBlank() ? UIConstants.DEFAULT_MODEL_NAME : currentValue;
    }

    static List<String> createModelSelections() {
        List<String> selections = new ArrayList<>();
        selections.add(UIConstants.DEFAULT_MODEL_NAME);
        for (ModelCatalogEntry modelInfo : ModelCatalogEntry.scanModels()) {
            String folderName = modelInfo.getFolderName();
            if (!folderName.isBlank() && !selections.contains(folderName)) {
                selections.add(folderName);
            }
        }
        return selections;
    }

    static Component toModelSelectionComponent(String modelName) {
        if (modelName == null || modelName.isBlank() || UIConstants.DEFAULT_MODEL_NAME.equals(modelName)) {
            return Component.translatable("gui.mmdskin.mod_settings.mob_replacement.vanilla");
        }
        return Component.literal(modelName);
    }

    static void saveMobReplacementSelection(ConfigData data, String entityTypeId, String value) {
        if (value == null || value.isBlank() || UIConstants.DEFAULT_MODEL_NAME.equals(value)) {
            data.mobModelReplacements.remove(entityTypeId);
        } else {
            data.mobModelReplacements.put(entityTypeId, value);
        }
    }

    static void cleanupInvalidMobReplacements(ConfigData data) {
        Iterator<String> iterator = data.mobModelReplacements.values().iterator();
        while (iterator.hasNext()) {
            String modelName = iterator.next();
            if (modelName == null || modelName.isBlank() || ModelCatalogEntry.findByFolderName(modelName) == null) {
                iterator.remove();
            }
        }
    }

    private static String fit(Font font, String value, int maxWidth) {
        if (font.width(value) <= maxWidth) {
            return value;
        }
        String suffix = "...";
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return value.substring(0, end) + suffix;
    }

    private enum Category {
        RENDER("gui.mmdskin.mod_settings.tab.render"),
        PERFORMANCE("gui.mmdskin.mod_settings.tab.performance"),
        MME("gui.mmdskin.mod_settings.tab.mme"),
        TOON("gui.mmdskin.mod_settings.tab.toon"),
        PHYSICS("gui.mmdskin.mod_settings.tab.physics"),
        DEBUG("gui.mmdskin.mod_settings.tab.debug"),
        VR("gui.mmdskin.mod_settings.tab.vr"),
        MOB("gui.mmdskin.mod_settings.tab.mob");

        private final String labelKey;

        Category(String labelKey) {
            this.labelKey = labelKey;
        }
    }

    private abstract static class SettingRow {
        final Component label;
        final Component tooltip;

        SettingRow(Component label, Component tooltip) {
            this.label = label;
            this.tooltip = tooltip;
        }

        abstract void addWidgets(ModConfigScreen screen, int x, int y, int width);

        void render(ModConfigScreen screen, GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY) {
            int controlWidth = Math.min(180, Math.max(112, width / 3));
            graphics.drawString(screen.font, fit(screen.font, displayText(), width - controlWidth - 12), x + 3, y + 6, 0xE8E8E8, false);
        }

        String displayText() {
            return label.getString();
        }

        boolean isLabelHovered(ModConfigScreen screen, int x, int y, int width, int mouseX, int mouseY) {
            int controlWidth = Math.min(180, Math.max(112, width / 3));
            return tooltip != null && mouseX >= x && mouseX < x + width - controlWidth - 4 && mouseY >= y && mouseY < y + 20;
        }

        void attachTooltip(AbstractWidget widget) {
            if (tooltip != null) {
                widget.setTooltip(Tooltip.create(tooltip));
            }
        }
    }

    private static final class BooleanSettingRow extends SettingRow {
        private final BooleanSupplier getter;
        private final Consumer<Boolean> setter;

        BooleanSettingRow(Component label, Component tooltip, BooleanSupplier getter, Consumer<Boolean> setter) {
            super(label, tooltip);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        void addWidgets(ModConfigScreen screen, int x, int y, int width) {
            int controlWidth = Math.min(180, Math.max(112, width / 3));
            Button button = Button.builder(stateText(), pressed -> {
                setter.accept(!getter.getAsBoolean());
                pressed.setMessage(stateText());
            }).bounds(x + width - controlWidth, y, controlWidth, 20).build();
            attachTooltip(button);
            screen.addRenderableWidget(button);
        }

        private Component stateText() {
            return Component.translatable(getter.getAsBoolean() ? "options.on" : "options.off");
        }
    }

    private static final class SliderSettingRow extends SettingRow {
        private final int min;
        private final int max;
        private final IntSupplier getter;
        private final IntConsumer setter;
        private final IntFunction<Component> formatter;

        SliderSettingRow(Component label, Component tooltip, int min, int max, IntSupplier getter, IntConsumer setter, IntFunction<Component> formatter) {
            super(label, tooltip);
            this.min = min;
            this.max = max;
            this.getter = getter;
            this.setter = setter;
            this.formatter = formatter;
        }

        @Override
        void addWidgets(ModConfigScreen screen, int x, int y, int width) {
            int controlWidth = Math.min(180, Math.max(112, width / 3));
            ValueSlider slider = new ValueSlider(x + width - controlWidth, y, controlWidth, min, max, getter, setter, formatter);
            attachTooltip(slider);
            screen.addRenderableWidget(slider);
        }
    }

    private final class MobSettingRow extends SettingRow {
        private final MobReplacementTargets.Target target;
        private final String entityTypeId;

        MobSettingRow(MobReplacementTargets.Target target) {
            super(target.displayName(), null);
            this.target = target;
            this.entityTypeId = target.entityTypeId().toString();
        }

        @Override
        void addWidgets(ModConfigScreen screen, int x, int y, int width) {
            int resetWidth = 64;
            int chooseWidth = 76;
            int right = x + width;
            screen.addRenderableWidget(Button.builder(Component.translatable("gui.mmdskin.mod_settings.mob_replacement.choose"), ignored -> {
                String current = getMobReplacementValue(data, entityTypeId);
                Minecraft.getInstance().setScreen(new MobReplacementModelPickerScreen(
                        screen, target, current, value -> saveMobReplacementSelection(data, entityTypeId, value)));
            }).bounds(right - resetWidth - chooseWidth - 4, y, chooseWidth, 20).build());
            screen.addRenderableWidget(Button.builder(Component.translatable("gui.mmdskin.mod_settings.mob_replacement.reset"), ignored -> {
                saveMobReplacementSelection(data, entityTypeId, UIConstants.DEFAULT_MODEL_NAME);
            }).bounds(right - resetWidth, y, resetWidth, 20).build());
        }

        @Override
        String displayText() {
            return label.getString() + ": " + toModelSelectionComponent(getMobReplacementValue(data, entityTypeId)).getString();
        }
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntConsumer setter;
        private final IntFunction<Component> formatter;

        ValueSlider(int x, int y, int width, int min, int max, IntSupplier getter, IntConsumer setter, IntFunction<Component> formatter) {
            super(x, y, width, 20, Component.empty(), normalize(getter.getAsInt(), min, max));
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.formatter = formatter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(formatter.apply(currentValue()));
        }

        @Override
        protected void applyValue() {
            setter.accept(currentValue());
        }

        private int currentValue() {
            return min + (int) Math.round(value * (max - min));
        }

        private static double normalize(int value, int min, int max) {
            if (max <= min) {
                return 0.0;
            }
            return Mth.clamp((value - min) / (double) (max - min), 0.0, 1.0);
        }
    }
}

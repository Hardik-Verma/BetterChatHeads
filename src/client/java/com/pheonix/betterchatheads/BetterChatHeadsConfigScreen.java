package com.pheonix.betterchatheads;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class BetterChatHeadsConfigScreen extends Screen {
    private static final int OUTER_BG = 0xB40A0D12;
    private static final int PANEL_BG = 0xE0141820;
    private static final int PANEL_STROKE = 0x80323848;
    private static final int ROW_BG = 0x66202833;
    private static final int ACCENT = 0xFFA855F7;
    private static final int LABEL = 0xFFF5F5F5;
    private static final int MUTED = 0xFFA3A3A3;

    private final Screen parent;
    private final BetterChatHeadsConfig draft;
    private final BetterChatHeadsConfig defaults = new BetterChatHeadsConfig();
    private final List<Row> rows = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int leftColumnX;
    private int rightColumnX;
    private int columnWidth;

    public BetterChatHeadsConfigScreen(Screen parent) {
        super(Text.translatable("betterchatheads.config.title"));
        this.parent = parent;
        this.draft = BetterChatHeadsClient.config().copy();
    }

    @Override
    protected void init() {
        this.rows.clear();
        this.clearChildren();

        this.panelWidth = Math.min(this.width - 64, 760);
        this.panelHeight = Math.min(this.height - 44, 360);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        this.columnWidth = (this.panelWidth - 60) / 2;
        this.leftColumnX = this.panelX + 20;
        this.rightColumnX = this.leftColumnX + this.columnWidth + 20;

        int leftY = this.panelY + 66;
        int rightY = this.panelY + 66;

        leftY = this.addBooleanRow(this.leftColumnX, leftY,
                "betterchatheads.config.enable_chat_heads",
                () -> this.draft.enableChatHeads,
                value -> this.draft.enableChatHeads = value,
                this.defaults.enableChatHeads,
                "betterchatheads.config.enable_chat_heads.tooltip");
        leftY = this.addBooleanRow(this.leftColumnX, leftY,
                "betterchatheads.config.show_own_chat_head",
                () -> this.draft.showOwnChatHead,
                value -> this.draft.showOwnChatHead = value,
                this.defaults.showOwnChatHead,
                "betterchatheads.config.show_own_chat_head.tooltip");
        leftY = this.addBooleanRow(this.leftColumnX, leftY,
                "betterchatheads.config.show_chat_indicator",
                () -> this.draft.showChatIndicator,
                value -> this.draft.showChatIndicator = value,
                this.defaults.showChatIndicator,
                "betterchatheads.config.show_chat_indicator.tooltip");
        leftY = this.addBooleanRow(this.leftColumnX, leftY,
                "betterchatheads.config.show_chat_background",
                () -> this.draft.showVanillaChatBackground,
                value -> this.draft.showVanillaChatBackground = value,
                this.defaults.showVanillaChatBackground,
                "betterchatheads.config.show_chat_background.tooltip");
        leftY = this.addIntRow(this.leftColumnX, leftY,
                "betterchatheads.config.chat_animation_ticks", 0, 8,
                () -> this.draft.chatAnimationTicks,
                value -> this.draft.chatAnimationTicks = value,
                this.defaults.chatAnimationTicks,
                "betterchatheads.config.chat_animation_ticks.tooltip");
        leftY = this.addIntRow(this.leftColumnX, leftY,
                "betterchatheads.config.chat_head_padding", 0, 8,
                () -> this.draft.chatHeadPadding,
                value -> this.draft.chatHeadPadding = value,
                this.defaults.chatHeadPadding,
                "betterchatheads.config.chat_head_padding.tooltip");
        leftY = this.addIntRow(this.leftColumnX, leftY,
                "betterchatheads.config.chat_vertical_offset", -6, 6,
                () -> this.draft.chatHeadVerticalOffset,
                value -> this.draft.chatHeadVerticalOffset = value,
                this.defaults.chatHeadVerticalOffset,
                "betterchatheads.config.chat_vertical_offset.tooltip");
        this.addIntRow(this.leftColumnX, leftY,
                "betterchatheads.config.head_size", 4, 16,
                () -> this.draft.headSize,
                value -> this.draft.headSize = value,
                this.defaults.headSize,
                "betterchatheads.config.head_size.tooltip");

        rightY = this.addBooleanRow(this.rightColumnX, rightY,
                "betterchatheads.config.enable_tab_heads",
                () -> this.draft.enableTabHeads,
                value -> this.draft.enableTabHeads = value,
                this.defaults.enableTabHeads,
                "betterchatheads.config.enable_tab_heads.tooltip");
        rightY = this.addBooleanRow(this.rightColumnX, rightY,
                "betterchatheads.config.render_overlay_layer",
                () -> this.draft.renderOverlayLayer,
                value -> this.draft.renderOverlayLayer = value,
                this.defaults.renderOverlayLayer,
                "betterchatheads.config.render_overlay_layer.tooltip");
        rightY = this.addIntRow(this.rightColumnX, rightY,
                "betterchatheads.config.chat_head_3dness", 0, 2,
                () -> this.draft.chatHead3dness,
                value -> this.draft.chatHead3dness = value,
                this.defaults.chatHead3dness,
                "betterchatheads.config.chat_head_3dness.tooltip");
        rightY = this.addBooleanRow(this.rightColumnX, rightY,
                "betterchatheads.config.use_default_fallback",
                () -> this.draft.useDefaultHeadWhenMissingSkin,
                value -> this.draft.useDefaultHeadWhenMissingSkin = value,
                this.defaults.useDefaultHeadWhenMissingSkin,
                "betterchatheads.config.use_default_fallback.tooltip");
        rightY = this.addIntRow(this.rightColumnX, rightY,
                "betterchatheads.config.tab_head_padding", 0, 8,
                () -> this.draft.tabHeadPadding,
                value -> this.draft.tabHeadPadding = value,
                this.defaults.tabHeadPadding,
                "betterchatheads.config.tab_head_padding.tooltip");
        rightY = this.addBooleanRow(this.rightColumnX, rightY,
                "betterchatheads.config.show_keybind_feedback",
                () -> this.draft.showKeybindActionBarFeedback,
                value -> this.draft.showKeybindActionBarFeedback = value,
                this.defaults.showKeybindActionBarFeedback,
                "betterchatheads.config.show_keybind_feedback.tooltip");
        rightY = this.addBooleanRow(this.rightColumnX, rightY,
                "betterchatheads.config.enable_update_checker",
                () -> this.draft.enableUpdateChecker,
                value -> this.draft.enableUpdateChecker = value,
                this.defaults.enableUpdateChecker,
                "betterchatheads.config.enable_update_checker.tooltip");
        this.addBooleanRow(this.rightColumnX, rightY,
                "betterchatheads.config.allow_update_prompt_ingame",
                () -> this.draft.allowUpdatePromptInGame,
                value -> this.draft.allowUpdatePromptInGame = value,
                this.defaults.allowUpdatePromptInGame,
                "betterchatheads.config.allow_update_prompt_ingame.tooltip");

        int buttonY = this.panelY + this.panelHeight - 34;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("betterchatheads.config.save"), button -> {
            BetterChatHeadsClient.applyConfig(this.draft);
            assert this.client != null;
            this.client.setScreen(this.parent);
        }).dimensions(this.panelX + this.panelWidth - 218, buttonY, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("betterchatheads.config.cancel"), button -> {
            assert this.client != null;
            this.client.setScreen(this.parent);
        }).dimensions(this.panelX + this.panelWidth - 110, buttonY, 100, 20).build());
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(this.parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, OUTER_BG);
        context.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, PANEL_BG);
        context.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + 1, PANEL_STROKE);
        context.fill(this.panelX, this.panelY + this.panelHeight - 1, this.panelX + this.panelWidth, this.panelY + this.panelHeight, PANEL_STROKE);
        context.fill(this.panelX, this.panelY, this.panelX + 1, this.panelY + this.panelHeight, PANEL_STROKE);
        context.fill(this.panelX + this.panelWidth - 1, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, PANEL_STROKE);
        context.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + 3, ACCENT);

        for (Row row : this.rows) {
            context.fill(row.x, row.y, row.x + this.columnWidth, row.y + 26, ROW_BG);
        }

        super.render(context, mouseX, mouseY, delta);

        context.drawTextWithShadow(this.textRenderer, this.title, this.panelX + 20, this.panelY + 14, LABEL);
        context.drawText(this.textRenderer, Text.translatable("betterchatheads.config.subtitle"), this.panelX + 20, this.panelY + 28, MUTED, false);
        context.drawText(this.textRenderer, Text.translatable("betterchatheads.config.section.chat"), this.leftColumnX, this.panelY + 48, ACCENT, false);
        context.drawText(this.textRenderer, Text.translatable("betterchatheads.config.section.utility"), this.rightColumnX, this.panelY + 48, ACCENT, false);
        context.drawText(this.textRenderer, Text.translatable("betterchatheads.config.hint"), this.panelX + 20, this.panelY + this.panelHeight - 28, MUTED, false);

        for (Row row : this.rows) {
            context.drawTextWithShadow(this.textRenderer, row.label, row.x + 8, row.y + 9, LABEL);
        }
    }

    private int addBooleanRow(int x, int y, String labelKey, BooleanGetter getter, BooleanSetter setter, boolean defaultValue, String tooltipKey) {
        int resetWidth = 42;
        int controlWidth = 98;
        int resetX = x + this.columnWidth - resetWidth - 8;
        ButtonWidget toggle = ButtonWidget.builder(booleanValueText(getter.get()), button -> {
            setter.set(!getter.get());
            button.setMessage(booleanValueText(getter.get()));
        }).dimensions(resetX - controlWidth - 6, y + 3, controlWidth, 20)
                .tooltip(Tooltip.of(Text.translatable(tooltipKey)))
                .build();

        ButtonWidget reset = ButtonWidget.builder(Text.translatable("betterchatheads.config.reset"), button -> {
            setter.set(defaultValue);
            toggle.setMessage(booleanValueText(getter.get()));
        }).dimensions(resetX, y + 3, resetWidth, 20)
                .tooltip(Tooltip.of(Text.translatable(tooltipKey)))
                .build();

        this.rows.add(new Row(Text.translatable(labelKey), x, y));
        this.addDrawableChild(toggle);
        this.addDrawableChild(reset);
        return y + 30;
    }

    private int addIntRow(int x, int y, String labelKey, int min, int max, IntGetter getter, IntSetter setter, int defaultValue, String tooltipKey) {
        int resetWidth = 42;
        int controlRight = x + this.columnWidth - 8 - resetWidth - 6;
        ButtonWidget value = ButtonWidget.builder(intValueText(getter.get()), button -> {
            setter.set(getter.get() >= max ? min : getter.get() + 1);
            button.setMessage(intValueText(getter.get()));
        }).dimensions(controlRight - 78, y + 3, 38, 20)
                .tooltip(Tooltip.of(Text.translatable(tooltipKey)))
                .build();

        ButtonWidget decrease = ButtonWidget.builder(Text.literal("-"), button -> {
            setter.set(Math.max(min, getter.get() - 1));
            value.setMessage(intValueText(getter.get()));
        }).dimensions(controlRight - 104, y + 3, 20, 20)
                .tooltip(Tooltip.of(Text.translatable(tooltipKey)))
                .build();

        ButtonWidget increase = ButtonWidget.builder(Text.literal("+"), button -> {
            setter.set(Math.min(max, getter.get() + 1));
            value.setMessage(intValueText(getter.get()));
        }).dimensions(controlRight - 34, y + 3, 20, 20)
                .tooltip(Tooltip.of(Text.translatable(tooltipKey)))
                .build();

        ButtonWidget reset = ButtonWidget.builder(Text.translatable("betterchatheads.config.reset"), button -> {
            setter.set(defaultValue);
            value.setMessage(intValueText(getter.get()));
        }).dimensions(x + this.columnWidth - resetWidth - 8, y + 3, resetWidth, 20)
                .tooltip(Tooltip.of(Text.translatable(tooltipKey)))
                .build();

        this.rows.add(new Row(Text.translatable(labelKey), x, y));
        this.addDrawableChild(decrease);
        this.addDrawableChild(value);
        this.addDrawableChild(increase);
        this.addDrawableChild(reset);
        return y + 30;
    }

    private static Text booleanValueText(boolean value) {
        return Text.translatable(value ? "betterchatheads.config.enabled" : "betterchatheads.config.disabled");
    }

    private static Text intValueText(int value) {
        return Text.literal(Integer.toString(value));
    }

    private record Row(Text label, int x, int y) {
    }

    @FunctionalInterface
    private interface BooleanGetter {
        boolean get();
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }

    @FunctionalInterface
    private interface IntGetter {
        int get();
    }

    @FunctionalInterface
    private interface IntSetter {
        void set(int value);
    }

}

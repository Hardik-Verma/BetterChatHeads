package com.pheonix.betterchatheads;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public final class BetterChatHeadsUpdateScreen extends ConfirmScreen {
    private final Screen parent;
    private final BetterChatHeadsUpdateChecker.UpdateInfo updateInfo;

    public BetterChatHeadsUpdateScreen(Screen parent, BetterChatHeadsUpdateChecker.UpdateInfo updateInfo) {
        super(callback(parent, updateInfo),
                Text.translatable("betterchatheads.update.title", updateInfo.version()),
                updateBody(updateInfo),
                Text.translatable("betterchatheads.update.open_link"),
                Text.translatable("betterchatheads.update.later"));
        this.parent = parent;
        this.updateInfo = updateInfo;
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(this.parent);
    }

    private static BooleanConsumer callback(Screen parent, BetterChatHeadsUpdateChecker.UpdateInfo updateInfo) {
        return accepted -> {
            if (accepted) {
                Util.getOperatingSystem().open(updateInfo.updateUrl());
            }

            MinecraftClient.getInstance().setScreen(parent);
        };
    }

    private static Text updateBody(BetterChatHeadsUpdateChecker.UpdateInfo updateInfo) {
        String changelog = updateInfo.changelog() == null || updateInfo.changelog().isBlank()
                ? ""
                : "\n\n" + updateInfo.changelog().trim();
        return Text.literal("A newer Better Chat Heads build is available." + changelog);
    }
}

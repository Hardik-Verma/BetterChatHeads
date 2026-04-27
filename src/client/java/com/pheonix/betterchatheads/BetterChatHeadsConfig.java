package com.pheonix.betterchatheads;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BetterChatHeadsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("betterchatheads.json");

    public boolean enableChatHeads = true;
    public boolean enableTabHeads = true;
    public boolean renderOverlayLayer = true;
    public int chatHead3dness = 1;
    public int headSize = 8;
    public int chatHeadPadding = 3;
    public int tabHeadPadding = 2;
    public boolean useDefaultHeadWhenMissingSkin = true;
    public boolean showOwnChatHead = true;
    public boolean showKeybindActionBarFeedback = true;
    public boolean enableUpdateChecker = true;
    public boolean allowUpdatePromptInGame = false;

    public boolean removeChatBackground = true;
    public boolean animateMessages = true;
    public int animationDurationMs = 200;
    public int animationSlidePixels = 3;
    public boolean compactFormat = true;
    public String separatorBeforeName = "";
    public String separatorAfterName = "\u2192";
    public String separatorColor = "#9ca3af";
    public String arrowColor = "#9ca3af";
    public String playerNameColor = "#f3f4f6";
    public String messageColor = "#d1d5db";

    // Compatibility fields kept for the current in-game config screen.
    public boolean showChatIndicator = true;
    public boolean showVanillaChatBackground = false;
    public int chatHeadVerticalOffset = 0;
    public int chatAnimationTicks = 4;

    public static BetterChatHeadsConfig load() {
        BetterChatHeadsConfig config = new BetterChatHeadsConfig();
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                BetterChatHeadsConfig loaded = GSON.fromJson(reader, BetterChatHeadsConfig.class);
                if (loaded != null) {
                    config = loaded;
                }
            } catch (IOException exception) {
                BetterChatHeadsClient.LOGGER.warn("Failed to read config, using defaults", exception);
            }
        }

        config.sanitize();
        config.save();
        return config;
    }

    public BetterChatHeadsConfig copy() {
        BetterChatHeadsConfig copy = new BetterChatHeadsConfig();
        copy.enableChatHeads = this.enableChatHeads;
        copy.enableTabHeads = this.enableTabHeads;
        copy.renderOverlayLayer = this.renderOverlayLayer;
        copy.chatHead3dness = this.chatHead3dness;
        copy.headSize = this.headSize;
        copy.chatHeadPadding = this.chatHeadPadding;
        copy.tabHeadPadding = this.tabHeadPadding;
        copy.useDefaultHeadWhenMissingSkin = this.useDefaultHeadWhenMissingSkin;
        copy.showOwnChatHead = this.showOwnChatHead;
        copy.showKeybindActionBarFeedback = this.showKeybindActionBarFeedback;
        copy.enableUpdateChecker = this.enableUpdateChecker;
        copy.allowUpdatePromptInGame = this.allowUpdatePromptInGame;
        copy.removeChatBackground = this.removeChatBackground;
        copy.animateMessages = this.animateMessages;
        copy.animationDurationMs = this.animationDurationMs;
        copy.animationSlidePixels = this.animationSlidePixels;
        copy.compactFormat = this.compactFormat;
        copy.separatorBeforeName = this.separatorBeforeName;
        copy.separatorAfterName = this.separatorAfterName;
        copy.separatorColor = this.separatorColor;
        copy.arrowColor = this.arrowColor;
        copy.playerNameColor = this.playerNameColor;
        copy.messageColor = this.messageColor;
        copy.showChatIndicator = this.showChatIndicator;
        copy.showVanillaChatBackground = this.showVanillaChatBackground;
        copy.chatHeadVerticalOffset = this.chatHeadVerticalOffset;
        copy.chatAnimationTicks = this.chatAnimationTicks;
        return copy;
    }

    public void applyFrom(BetterChatHeadsConfig other) {
        this.enableChatHeads = other.enableChatHeads;
        this.enableTabHeads = other.enableTabHeads;
        this.renderOverlayLayer = other.renderOverlayLayer;
        this.chatHead3dness = other.chatHead3dness;
        this.headSize = other.headSize;
        this.chatHeadPadding = other.chatHeadPadding;
        this.tabHeadPadding = other.tabHeadPadding;
        this.useDefaultHeadWhenMissingSkin = other.useDefaultHeadWhenMissingSkin;
        this.showOwnChatHead = other.showOwnChatHead;
        this.showKeybindActionBarFeedback = other.showKeybindActionBarFeedback;
        this.enableUpdateChecker = other.enableUpdateChecker;
        this.allowUpdatePromptInGame = other.allowUpdatePromptInGame;

        this.removeChatBackground = !other.showVanillaChatBackground;
        this.animateMessages = other.chatAnimationTicks > 0 || other.animateMessages;
        this.animationDurationMs = other.animationDurationMs > 0 ? other.animationDurationMs : other.chatAnimationTicks * 50;
        this.animationSlidePixels = other.animationSlidePixels;
        this.compactFormat = other.compactFormat;
        this.separatorBeforeName = other.separatorBeforeName;
        this.separatorAfterName = other.separatorAfterName;
        this.separatorColor = other.separatorColor;
        this.arrowColor = other.arrowColor;
        this.playerNameColor = other.playerNameColor;
        this.messageColor = other.messageColor;

        this.showChatIndicator = other.showChatIndicator;
        this.showVanillaChatBackground = other.showVanillaChatBackground;
        this.chatHeadVerticalOffset = other.chatHeadVerticalOffset;
        this.chatAnimationTicks = other.chatAnimationTicks;
        this.sanitize();
    }

    public void save() {
        this.sanitize();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException exception) {
            BetterChatHeadsClient.LOGGER.warn("Failed to save config", exception);
        }
    }

    public int tabHorizontalOffset() {
        return this.headSize + this.tabHeadPadding;
    }

    public int separatorColorRgb() {
        return parseHexColor(this.separatorColor, 0x9CA3AF);
    }

    public int arrowColorRgb() {
        return parseHexColor(this.arrowColor, 0x9CA3AF);
    }

    public int playerNameColorRgb() {
        return parseHexColor(this.playerNameColor, 0xF3F4F6);
    }

    public int messageColorRgb() {
        return parseHexColor(this.messageColor, 0xD1D5DB);
    }

    public int animationDurationMsSafe() {
        return this.animateMessages ? this.animationDurationMs : 0;
    }

    private void sanitize() {
        this.headSize = clamp(this.headSize, 4, 16);
        this.chatHead3dness = clamp(this.chatHead3dness, 0, 2);
        this.chatHeadPadding = clamp(this.chatHeadPadding, 0, 8);
        this.tabHeadPadding = clamp(this.tabHeadPadding, 0, 8);
        this.chatHeadVerticalOffset = clamp(this.chatHeadVerticalOffset, -6, 6);
        this.animationDurationMs = clamp(this.animationDurationMs, 0, 400);
        this.animationSlidePixels = clamp(this.animationSlidePixels, 0, 8);
        this.separatorBeforeName = sanitizeToken(this.separatorBeforeName, "");
        this.separatorAfterName = sanitizeToken(this.separatorAfterName, "\u2192");
        this.separatorColor = sanitizeColor(this.separatorColor, "#9ca3af");
        this.arrowColor = sanitizeColor(this.arrowColor, "#9ca3af");
        this.playerNameColor = sanitizeColor(this.playerNameColor, "#f3f4f6");
        this.messageColor = sanitizeColor(this.messageColor, "#d1d5db");
        this.showVanillaChatBackground = !this.removeChatBackground;
        this.chatAnimationTicks = this.animateMessages ? clamp(Math.round(this.animationDurationMs / 50.0f), 0, 8) : 0;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String sanitizeToken(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        return switch (trimmed) {
            case ">>", "Ã‚Â»", "Â»", "â€º", "›", "››" -> "";
            case "-->", "Ã¢â€ â€™", "→" -> "\u2192";
            default -> trimmed;
        };
    }

    private static String sanitizeColor(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.strip();
        if ("#8b7aa8".equalsIgnoreCase(trimmed)
                || "#a855f7".equalsIgnoreCase(trimmed)
                || "#666666".equalsIgnoreCase(trimmed)) {
            return fallback;
        }
        if (trimmed.matches("^#[0-9a-fA-F]{6}$")) {
            return trimmed.toLowerCase();
        }
        return fallback;
    }

    private static int parseHexColor(String value, int fallback) {
        try {
            return Integer.parseInt(value.substring(1), 16);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}

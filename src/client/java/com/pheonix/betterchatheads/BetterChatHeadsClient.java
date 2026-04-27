package com.pheonix.betterchatheads;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class BetterChatHeadsClient implements ClientModInitializer {
    public static final String MOD_ID = "betterchatheads";
    public static final Logger LOGGER = LoggerFactory.getLogger("BetterChatHeads");
    private static final KeyBinding.Category KEY_CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "keys"));

    private static BetterChatHeadsConfig config;
    private static PlayerSkinResolver skinResolver;
    private static SkinHeadRenderer headRenderer;
    private static final Map<String, Long> CHAT_ANIMATIONS = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return this.size() > 256;
        }
    };
    private static final Map<String, ChatLineSender> CHAT_LINE_SENDERS = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ChatLineSender> eldest) {
            return this.size() > 256;
        }
    };
    private static final Map<String, ChatLineSender> CHAT_CONTENT_SENDERS = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ChatLineSender> eldest) {
            return this.size() > 256;
        }
    };
    private static final Map<String, ParsedChatLine> PARSED_CHAT_LINES = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ParsedChatLine> eldest) {
            return this.size() > 256;
        }
    };
    private static final Map<String, ParsedChatLine> PARSED_CHAT_CONTENT = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ParsedChatLine> eldest) {
            return this.size() > 256;
        }
    };
    private static ChatLineSender currentIncomingChatSender;
    private static ParsedChatLine currentVisibleChatLineData;
    private static int currentVisibleChatLineIndex;

    private static KeyBinding openConfigKeyBinding;
    private static KeyBinding toggleChatHeadsKeyBinding;
    private static KeyBinding toggleTabHeadsKeyBinding;
    private static KeyBinding toggleOverlayKeyBinding;
    private static KeyBinding cycleHeadSizeKeyBinding;

    @Override
    public void onInitializeClient() {
        config = BetterChatHeadsConfig.load();
        MinecraftClient client = MinecraftClient.getInstance();
        skinResolver = new PlayerSkinResolver(client);
        headRenderer = new SkinHeadRenderer(client, skinResolver);

        openConfigKeyBinding = registerKey("open_config", GLFW.GLFW_KEY_B);
        toggleChatHeadsKeyBinding = registerUnboundKey("toggle_chat_heads");
        toggleTabHeadsKeyBinding = registerUnboundKey("toggle_tab_heads");
        toggleOverlayKeyBinding = registerUnboundKey("toggle_overlay");
        cycleHeadSizeKeyBinding = registerUnboundKey("cycle_head_size");

        ClientTickEvents.END_CLIENT_TICK.register(BetterChatHeadsClient::tickClient);
        BetterChatHeadsUpdateChecker.start();
    }

    public static BetterChatHeadsConfig config() {
        return config;
    }

    public static PlayerSkinResolver skinResolver() {
        return skinResolver;
    }

    public static SkinHeadRenderer headRenderer() {
        return headRenderer;
    }

    public static String currentVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    public static void applyConfig(BetterChatHeadsConfig newConfig) {
        config.applyFrom(newConfig);
        config.save();
        skinResolver.clearCache();
        BetterChatHeadsUpdateChecker.onConfigUpdated();
    }

    public static void registerChatAnimation(String key, long startedAtMs) {
        synchronized (CHAT_ANIMATIONS) {
            CHAT_ANIMATIONS.put(key, startedAtMs);
        }
    }

    public static long getChatAnimationStart(String key) {
        synchronized (CHAT_ANIMATIONS) {
            Long exact = CHAT_ANIMATIONS.get(key);
            if (exact != null) {
                return exact;
            }

            int separator = key.lastIndexOf('|');
            if (separator < 0) {
                return -1L;
            }

            String contentKey = key.substring(0, separator);
            long bestMatch = -1L;
            for (Map.Entry<String, Long> entry : CHAT_ANIMATIONS.entrySet()) {
                String candidate = entry.getKey();
                int candidateSeparator = candidate.lastIndexOf('|');
                if (candidateSeparator < 0) {
                    continue;
                }
                if (candidate.regionMatches(0, contentKey, 0, candidateSeparator)
                        && candidateSeparator == contentKey.length()) {
                    bestMatch = Math.max(bestMatch, entry.getValue());
                }
            }
            return bestMatch;
        }
    }

    public static void beginIncomingChatSender(UUID senderUuid, String senderName) {
        currentIncomingChatSender = new ChatLineSender(senderUuid, senderName);
    }

    public static void clearIncomingChatSender() {
        currentIncomingChatSender = null;
    }

    public static ChatLineSender currentIncomingChatSender() {
        return currentIncomingChatSender;
    }

    public static void setCurrentVisibleChatLineData(ParsedChatLine parsedChatLine) {
        currentVisibleChatLineData = parsedChatLine;
        currentVisibleChatLineIndex = 0;
    }

    public static ParsedChatLine currentVisibleChatLineData() {
        return currentVisibleChatLineData;
    }

    public static void clearCurrentVisibleChatLineData() {
        currentVisibleChatLineData = null;
        currentVisibleChatLineIndex = 0;
    }

    public static int currentVisibleChatLineIndex() {
        return currentVisibleChatLineIndex;
    }

    public static void incrementCurrentVisibleChatLineIndex() {
        currentVisibleChatLineIndex++;
    }

    public static void registerChatLineSender(String key, ChatLineSender sender) {
        if (sender == null) {
            return;
        }
        synchronized (CHAT_LINE_SENDERS) {
            CHAT_LINE_SENDERS.put(key, sender);
        }
    }

    public static void registerChatContentSender(String content, ChatLineSender sender) {
        if (sender == null || content == null || content.isBlank()) {
            return;
        }
        synchronized (CHAT_CONTENT_SENDERS) {
            CHAT_CONTENT_SENDERS.put(content, sender);
        }
    }

    public static ChatLineSender getChatLineSender(String key) {
        synchronized (CHAT_LINE_SENDERS) {
            ChatLineSender exact = CHAT_LINE_SENDERS.get(key);
            if (exact != null) {
                return exact;
            }

            int separator = key.lastIndexOf('|');
            if (separator < 0) {
                return null;
            }

            String contentKey = key.substring(0, separator);
            ChatLineSender bestMatch = null;
            int bestScore = -1;
            for (Map.Entry<String, ChatLineSender> entry : CHAT_LINE_SENDERS.entrySet()) {
                String candidate = entry.getKey();
                int candidateSeparator = candidate.lastIndexOf('|');
                if (candidateSeparator < 0) {
                    continue;
                }
                String candidateContent = candidate.substring(0, candidateSeparator);
                if (candidateContent.equals(contentKey)) {
                    return entry.getValue();
                }
                if (candidateContent.contains(contentKey) || contentKey.contains(candidateContent)) {
                    int score = Math.min(candidateContent.length(), contentKey.length());
                    if (score > bestScore) {
                        bestScore = score;
                        bestMatch = entry.getValue();
                    }
                }
            }
            return bestMatch;
        }
    }

    public static ChatLineSender getChatContentSender(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        synchronized (CHAT_CONTENT_SENDERS) {
            ChatLineSender exact = CHAT_CONTENT_SENDERS.get(content);
            if (exact != null) {
                return exact;
            }

            ChatLineSender bestMatch = null;
            int bestScore = -1;
            for (Map.Entry<String, ChatLineSender> entry : CHAT_CONTENT_SENDERS.entrySet()) {
                String candidate = entry.getKey();
                if (candidate.contains(content) || content.contains(candidate)) {
                    int score = Math.min(candidate.length(), content.length());
                    if (score > bestScore) {
                        bestScore = score;
                        bestMatch = entry.getValue();
                    }
                }
            }
            return bestMatch;
        }
    }

    public static void registerParsedChatLine(String key, ParsedChatLine parsedChatLine) {
        if (key == null || key.isBlank() || parsedChatLine == null) {
            return;
        }
        synchronized (PARSED_CHAT_LINES) {
            PARSED_CHAT_LINES.put(key, parsedChatLine);
        }
    }

    public static void registerParsedChatContent(String content, ParsedChatLine parsedChatLine) {
        if (content == null || content.isBlank() || parsedChatLine == null) {
            return;
        }
        synchronized (PARSED_CHAT_CONTENT) {
            PARSED_CHAT_CONTENT.put(content, parsedChatLine);
        }
    }

    public static ParsedChatLine getParsedChatLine(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        synchronized (PARSED_CHAT_LINES) {
            ParsedChatLine exact = PARSED_CHAT_LINES.get(key);
            if (exact != null) {
                return exact;
            }

            int separator = key.lastIndexOf('|');
            String keyContent = separator >= 0 ? key.substring(0, separator) : key;
            ParsedChatLine bestMatch = null;
            int bestScore = -1;
            for (Map.Entry<String, ParsedChatLine> entry : PARSED_CHAT_LINES.entrySet()) {
                String candidate = entry.getKey();
                int candidateSeparator = candidate.lastIndexOf('|');
                String candidateContent = candidateSeparator >= 0 ? candidate.substring(0, candidateSeparator) : candidate;
                if (candidateContent.equals(keyContent)) {
                    return entry.getValue();
                }
                if (candidateContent.contains(keyContent) || keyContent.contains(candidateContent)) {
                    int score = Math.min(candidateContent.length(), keyContent.length());
                    if (score > bestScore) {
                        bestScore = score;
                        bestMatch = entry.getValue();
                    }
                }
            }
            return bestMatch;
        }
    }

    public static ParsedChatLine getParsedChatContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        synchronized (PARSED_CHAT_CONTENT) {
            ParsedChatLine exact = PARSED_CHAT_CONTENT.get(content);
            if (exact != null) {
                return exact;
            }

            ParsedChatLine bestMatch = null;
            int bestScore = -1;
            for (Map.Entry<String, ParsedChatLine> entry : PARSED_CHAT_CONTENT.entrySet()) {
                String candidate = entry.getKey();
                if (candidate.contains(content) || content.contains(candidate)) {
                    int score = Math.min(candidate.length(), content.length());
                    if (score > bestScore) {
                        bestScore = score;
                        bestMatch = entry.getValue();
                    }
                }
            }
            return bestMatch;
        }
    }

    private static void tickClient(MinecraftClient client) {
        while (openConfigKeyBinding.wasPressed()) {
            client.setScreen(new BetterChatHeadsConfigScreen(client.currentScreen));
        }

        while (toggleChatHeadsKeyBinding.wasPressed()) {
            config.enableChatHeads = !config.enableChatHeads;
            config.save();
            feedback(client, config.enableChatHeads ? "betterchatheads.feedback.chat_heads_on" : "betterchatheads.feedback.chat_heads_off");
        }

        while (toggleTabHeadsKeyBinding.wasPressed()) {
            config.enableTabHeads = !config.enableTabHeads;
            config.save();
            feedback(client, config.enableTabHeads ? "betterchatheads.feedback.tab_heads_on" : "betterchatheads.feedback.tab_heads_off");
        }

        while (toggleOverlayKeyBinding.wasPressed()) {
            config.renderOverlayLayer = !config.renderOverlayLayer;
            config.save();
            skinResolver.clearCache();
            feedback(client, config.renderOverlayLayer ? "betterchatheads.feedback.overlay_on" : "betterchatheads.feedback.overlay_off");
        }

        while (cycleHeadSizeKeyBinding.wasPressed()) {
            config.headSize = config.headSize >= 16 ? 4 : config.headSize + 1;
            config.save();
            feedback(client, Text.translatable("betterchatheads.feedback.head_size", config.headSize));
        }

        BetterChatHeadsUpdateChecker.tick(client);
    }

    private static KeyBinding registerKey(String suffix, int glfwKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.betterchatheads." + suffix,
                InputUtil.Type.KEYSYM,
                glfwKey,
                KEY_CATEGORY
        ));
    }

    private static KeyBinding registerUnboundKey(String suffix) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.betterchatheads." + suffix,
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                KEY_CATEGORY
        ));
    }

    private static void feedback(MinecraftClient client, String translationKey) {
        feedback(client, Text.translatable(translationKey));
    }

    private static void feedback(MinecraftClient client, Text text) {
        if (!config.showKeybindActionBarFeedback || client.player == null) {
            return;
        }
        client.player.sendMessage(text, true);
    }

    public record ChatLineSender(UUID senderUuid, String senderName) {
        public boolean isLocalPlayer(MinecraftClient client) {
            return senderUuid != null && client.player != null && senderUuid.equals(client.player.getUuid());
        }
    }

    public record ParsedChatLine(String playerName, String message, boolean restyleChat, String prefixText) {
    }
}

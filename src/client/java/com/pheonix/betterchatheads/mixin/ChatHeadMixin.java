package com.pheonix.betterchatheads.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pheonix.betterchatheads.BetterChatHeadsClient;
import com.pheonix.betterchatheads.PlayerSkinResolver;
import com.pheonix.betterchatheads.mixininterface.ParsedChatLineHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.sugar.Local;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public abstract class ChatHeadMixin {
    @Unique
    private static final Pattern betterchatheads$repeatSuffix = Pattern.compile("^(.*) \\[(\\d+)x]$");

    @Shadow private MinecraftClient client;
    @Shadow private List<ChatHudLine> messages;

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/ChatHud;addVisibleMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V"
            ),
            cancellable = true
    )
    private void betterchatheads$trackLatestPlayerMessage(
            Text text,
            MessageSignatureData signature,
            MessageIndicator indicator,
            CallbackInfo ci,
            @Local ChatHudLine line
    ) {
        String content = text.getString();
        BetterChatHeadsClient.ChatLineSender sender = BetterChatHeadsClient.currentIncomingChatSender();
        if (sender == null) {
            sender = BetterChatHeadsClient.getChatContentSender(content);
        }
        BetterChatHeadsClient.ParsedChatLine parsed = null;
        if (sender != null) {
            if (PlayerSkinResolver.isSkippableSystemMessage(content)) {
                return;
            }

            String senderName = sender.senderName();
            if ((senderName == null || senderName.isBlank()) && sender.senderUuid() != null) {
                senderName = BetterChatHeadsClient.skinResolver()
                        .resolvePlayerEntry(sender.senderUuid())
                        .map(entry -> entry.getProfile().name())
                        .orElse(null);
            }

            if (senderName != null && !senderName.isBlank()) {
                var vanillaMatch = BetterChatHeadsClient.skinResolver().extractPlayerChat(content).orElse(null);
                boolean restyle = vanillaMatch != null && vanillaMatch.playerName().equalsIgnoreCase(senderName);
                String message = restyle ? vanillaMatch.message() : content;
                parsed = new BetterChatHeadsClient.ParsedChatLine(senderName, message, restyle, "");
            }
        }

        if (parsed == null) {
            parsed = BetterChatHeadsClient.skinResolver()
                    .resolveChatSender(content)
                    .map(match -> new BetterChatHeadsClient.ParsedChatLine(
                            match.playerName(),
                            match.message(),
                            match.restyleChat(),
                            match.prefixText()
                    ))
                    .orElse(null);
        }

        if (parsed == null) {
            return;
        }

        if (betterchatheads$collapseDuplicate(parsed, signature, indicator, ci)) {
            return;
        }

        BetterChatHeadsClient.registerParsedChatContent(content, parsed);
        ((ParsedChatLineHolder) (Object) line).betterchatheads$setParsedChatLine(parsed);
        String key = betterchatheads$animationKey(line);
        BetterChatHeadsClient.registerParsedChatLine(key, parsed);
    }

    @Inject(method = "addVisibleMessage", at = @At("HEAD"))
    private void betterchatheads$beginVisibleLineTransfer(ChatHudLine line, CallbackInfo ci) {
        BetterChatHeadsClient.setCurrentVisibleChatLineData(((ParsedChatLineHolder) (Object) line).betterchatheads$getParsedChatLine());
    }

    @Inject(method = "addVisibleMessage", at = @At("RETURN"))
    private void betterchatheads$endVisibleLineTransfer(ChatHudLine line, CallbackInfo ci) {
        BetterChatHeadsClient.clearCurrentVisibleChatLineData();
    }

    @WrapOperation(
            method = "method_75802",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/ChatHud$Backend;fill(IIIII)V"
            )
    )
    private static void betterchatheads$removeChatBackground(
            ChatHud.Backend backend,
            int x1,
            int y1,
            int x2,
            int y2,
            int color,
            Operation<Void> original
    ) {
        if (BetterChatHeadsClient.config().removeChatBackground) {
            return;
        }
        original.call(backend, x1, y1, x2, y2, color);
    }

    @Unique
    private static String betterchatheads$animationKey(ChatHudLine line) {
        return line.content().getString() + "|" + line.creationTick();
    }

    @Unique
    private boolean betterchatheads$collapseDuplicate(
            BetterChatHeadsClient.ParsedChatLine parsed,
            MessageSignatureData signature,
            MessageIndicator indicator,
            CallbackInfo ci
    ) {
        if (this.messages.isEmpty()) {
            return false;
        }

        ChatHudLine latestLine = this.messages.get(0);
        BetterChatHeadsClient.ParsedChatLine latestParsed = ((ParsedChatLineHolder) (Object) latestLine).betterchatheads$getParsedChatLine();
        if (latestParsed == null) {
            return false;
        }

        if (!latestParsed.playerName().equalsIgnoreCase(parsed.playerName())) {
            return false;
        }

        String latestBaseMessage = betterchatheads$stripRepeatSuffix(latestParsed.message());
        String incomingBaseMessage = betterchatheads$stripRepeatSuffix(parsed.message());
        if (!latestBaseMessage.equals(incomingBaseMessage)) {
            return false;
        }

        int repeatCount = Math.max(betterchatheads$extractRepeatCount(latestParsed.message()) + 1, 2);
        String mergedMessage = latestBaseMessage + " [" + repeatCount + "x]";

        BetterChatHeadsClient.ParsedChatLine mergedParsed = new BetterChatHeadsClient.ParsedChatLine(
                parsed.playerName(),
                mergedMessage,
                parsed.restyleChat(),
                parsed.prefixText()
        );

        Text mergedText = parsed.restyleChat()
                ? Text.literal("<" + parsed.playerName() + "> " + mergedMessage)
                : Text.literal(latestLine.content().getString().replace(latestParsed.message(), mergedMessage));

        ChatHudLine replacement = new ChatHudLine(
                this.client.inGameHud.getTicks(),
                mergedText,
                signature,
                indicator
        );
        ((ParsedChatLineHolder) (Object) replacement).betterchatheads$setParsedChatLine(mergedParsed);

        ChatHudStorageAccessor accessor = (ChatHudStorageAccessor) (Object) this;
        accessor.betterchatheads$getMessages().set(0, replacement);
        betterchatheads$removeLeadingVisibleLines(accessor.betterchatheads$getVisibleMessages());
        accessor.betterchatheads$invokeAddVisibleMessage(replacement);
        BetterChatHeadsClient.registerParsedChatContent(mergedText.getString(), mergedParsed);
        BetterChatHeadsClient.registerParsedChatLine(betterchatheads$animationKey(replacement), mergedParsed);
        ci.cancel();
        return true;
    }

    @Unique
    private static void betterchatheads$removeLeadingVisibleLines(List<ChatHudLine.Visible> visibleMessages) {
        while (!visibleMessages.isEmpty()) {
            ChatHudLine.Visible removed = visibleMessages.remove(0);
            if (removed.endOfEntry()) {
                break;
            }
        }
    }

    @Unique
    private static int betterchatheads$extractRepeatCount(String message) {
        if (message == null) {
            return 1;
        }
        Matcher matcher = betterchatheads$repeatSuffix.matcher(message);
        if (!matcher.matches()) {
            return 1;
        }
        try {
            return Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    @Unique
    private static String betterchatheads$stripRepeatSuffix(String message) {
        if (message == null) {
            return "";
        }
        Matcher matcher = betterchatheads$repeatSuffix.matcher(message);
        return matcher.matches() ? matcher.group(1) : message;
    }
}

package com.pheonix.betterchatheads.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.pheonix.betterchatheads.BetterChatHeadsClient;
import com.pheonix.betterchatheads.mixininterface.ParsedChatLineHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.gui.hud.ChatHud$1")
public abstract class ChatHudLineMixin {
    @WrapOperation(
            method = "accept",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/ChatHud$Backend;text(IFLnet/minecraft/text/OrderedText;)Z"
            )
    )
    private boolean betterchatheads$renderStyledPlayerLine(
            ChatHud.Backend backend,
            int y,
            float opacity,
            OrderedText orderedText,
            Operation<Boolean> original,
            ChatHudLine.Visible visibleLine,
            int lineIndex,
            float lineOpacity
    ) {
        if (!BetterChatHeadsClient.config().enableChatHeads) {
            return original.call(backend, y, opacity, orderedText);
        }

        BetterChatHeadsClient.ParsedChatLine parsed = ((ParsedChatLineHolder) (Object) visibleLine).betterchatheads$getParsedChatLine();
        if (parsed == null) {
            return original.call(backend, y, opacity, orderedText);
        }
        int lineIndexWithinEntry = ((ParsedChatLineHolder) (Object) visibleLine).betterchatheads$getLineIndexWithinEntry();

        if (!BetterChatHeadsClient.config().showOwnChatHead
                && BetterChatHeadsClient.skinResolver().isLocalPlayer(parsed.playerName())) {
            return original.call(backend, y, opacity, orderedText);
        }

        PlayerListEntry entry = BetterChatHeadsClient.skinResolver()
                .resolvePlayerEntry(parsed.playerName())
                .orElse(null);

        DrawContext context = betterchatheads$extractContext(backend);
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        if (context == null || textRenderer == null) {
            return original.call(backend, y, opacity, orderedText);
        }

        if (parsed.restyleChat()) {
            betterchatheads$drawStyledChatLine(context, textRenderer, visibleLine, y, opacity, orderedText, parsed, entry, lineIndexWithinEntry);
            return false;
        }

        return betterchatheads$drawOriginalChatLineWithHead(context, backend, original, textRenderer, visibleLine, y, opacity, orderedText, parsed, lineIndexWithinEntry);
    }

    @Unique
    private static void betterchatheads$drawStyledChatLine(
            DrawContext context,
            TextRenderer textRenderer,
            ChatHudLine.Visible visibleLine,
            int y,
            float opacity,
            OrderedText orderedText,
            BetterChatHeadsClient.ParsedChatLine parsed,
            PlayerListEntry entry,
            int lineIndexWithinEntry
    ) {
        var config = BetterChatHeadsClient.config();
        int headSize = config.headSize;
        float progress = betterchatheads$animationProgress(visibleLine);
        int slideX = Math.round((1.0F - progress) * config.animationSlidePixels);
        int headX = -slideX;
        int baselineY = y;
        int headY = baselineY + (textRenderer.fontHeight - headSize) / 2 + config.chatHeadVerticalOffset;
        Text bulletText = Text.literal("\u2022");
        int bulletX = headX + headSize + config.chatHeadPadding;
        int bulletWidth = textRenderer.getWidth(bulletText);
        int nameX = bulletX + bulletWidth + 4;
        int tint = betterchatheads$withOpacity(0xFFFFFF, opacity * progress);
        Text arrowText = Text.literal(config.separatorAfterName);
        int arrowWidth = textRenderer.getWidth(arrowText);
        int messageStartX = nameX + textRenderer.getWidth(parsed.playerName()) + 4 + arrowWidth + 4;

        if (lineIndexWithinEntry > 0) {
            context.drawTextWithShadow(
                    textRenderer,
                    orderedText,
                    messageStartX,
                    baselineY,
                    betterchatheads$withOpacity(config.messageColorRgb(), opacity * progress)
            );
            return;
        }

        BetterChatHeadsClient.headRenderer().renderChatHead(
                context,
                headX,
                headY,
                parsed.playerName(),
                tint
        );

        context.drawTextWithShadow(
                textRenderer,
                bulletText,
                bulletX,
                baselineY,
                betterchatheads$withOpacity(config.arrowColorRgb(), opacity * progress)
        );

        context.drawTextWithShadow(
                textRenderer,
                parsed.playerName(),
                nameX,
                baselineY,
                betterchatheads$withOpacity(betterchatheads$getPlayerNameColor(entry), opacity * progress)
        );

        String message = parsed.message() == null ? "" : parsed.message().stripLeading();
        if (message.isEmpty()) {
            return;
        }

        int arrowX = nameX + textRenderer.getWidth(parsed.playerName()) + 4;
        context.drawTextWithShadow(
                textRenderer,
                arrowText,
                arrowX,
                baselineY,
                betterchatheads$withOpacity(config.arrowColorRgb(), opacity * progress)
        );
        int messageX = arrowX + textRenderer.getWidth(arrowText) + 4;
        context.drawTextWithShadow(
                textRenderer,
                message,
                messageX,
                baselineY,
                betterchatheads$withOpacity(config.messageColorRgb(), opacity * progress)
        );
    }

    @Unique
    private static boolean betterchatheads$drawOriginalChatLineWithHead(
            DrawContext context,
            ChatHud.Backend backend,
            Operation<Boolean> original,
            TextRenderer textRenderer,
            ChatHudLine.Visible visibleLine,
            int y,
            float opacity,
            OrderedText orderedText,
            BetterChatHeadsClient.ParsedChatLine parsed,
            int lineIndexWithinEntry
    ) {
        var config = BetterChatHeadsClient.config();
        float progress = betterchatheads$animationProgress(visibleLine);
        int slideX = Math.round((1.0F - progress) * config.animationSlidePixels);
        int headSize = config.headSize;
        int headX = -slideX;
        int headY = y + (textRenderer.fontHeight - headSize) / 2 + config.chatHeadVerticalOffset;
        int textTranslateX = headSize + config.chatHeadPadding - slideX;
        int tint = betterchatheads$withOpacity(0xFFFFFF, opacity * progress);

        if (lineIndexWithinEntry == 0) {
            BetterChatHeadsClient.headRenderer().renderChatHead(
                    context,
                    headX,
                    headY,
                    parsed.playerName(),
                    tint
            );
        }
        context.drawTextWithShadow(
                textRenderer,
                orderedText,
                textTranslateX,
                y,
                tint
        );
        return false;
    }

    @Unique
    private static float betterchatheads$animationProgress(ChatHudLine.Visible visibleLine) {
        int durationTicks = BetterChatHeadsClient.config().chatAnimationTicks;
        if (!BetterChatHeadsClient.config().animateMessages || durationTicks <= 0) {
            return 1.0F;
        }
        int currentTicks = MinecraftClient.getInstance().inGameHud.getTicks();
        int elapsedTicks = Math.max(0, currentTicks - visibleLine.addedTime());
        float linear = elapsedTicks / (float) durationTicks;
        float clamped = Math.max(0.0F, Math.min(1.0F, linear));
        return 1.0F - (float) Math.pow(1.0F - clamped, 3.0F);
    }

    @Unique
    private static int betterchatheads$withOpacity(int rgb, float opacity) {
        int alpha = Math.max(0, Math.min(255, Math.round(opacity * 255.0F)));
        return (alpha << 24) | rgb;
    }

    @Unique
    private static int betterchatheads$getPlayerNameColor(PlayerListEntry entry) {
        if (entry.getDisplayName() != null && entry.getDisplayName().getStyle().getColor() != null) {
            return entry.getDisplayName().getStyle().getColor().getRgb();
        }
        if (entry.getScoreboardTeam() != null && entry.getScoreboardTeam().getColor().getColorValue() != null) {
            return entry.getScoreboardTeam().getColor().getColorValue();
        }
        return BetterChatHeadsClient.config().playerNameColorRgb();
    }

    @Unique
    private static DrawContext betterchatheads$extractContext(ChatHud.Backend backend) {
        if (backend instanceof ChatHudHudAccessor hudAccessor) {
            return hudAccessor.betterchatheads$getContext();
        }
        if (backend instanceof ChatHudInteractableAccessor interactableAccessor) {
            return interactableAccessor.betterchatheads$getContext();
        }
        return null;
    }

}

package com.pheonix.betterchatheads;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;

import java.util.UUID;

public final class SkinHeadRenderer {
    private final MinecraftClient client;
    private final PlayerSkinResolver skinResolver;

    public SkinHeadRenderer(MinecraftClient client, PlayerSkinResolver skinResolver) {
        this.client = client;
        this.skinResolver = skinResolver;
    }

    public void renderChatHead(DrawContext context, int x, int y, String playerName, int tintColor) {
        if (!BetterChatHeadsClient.config().enableChatHeads || this.client.getNetworkHandler() == null) {
            return;
        }

        PlayerListEntry entry = this.skinResolver.resolvePlayerEntry(playerName).orElse(null);
        if (entry == null) {
            return;
        }

        renderHead(context, x, y, this.skinResolver.resolve(entry).skinTextures(), tintColor);
    }

    public void renderChatHead(DrawContext context, int x, int y, UUID senderUuid, String senderName, int tintColor) {
        if (!BetterChatHeadsClient.config().enableChatHeads) {
            return;
        }

        PlayerSkinResolver.ResolvedHead resolvedHead = senderUuid != null
                ? this.skinResolver.resolveByUuid(senderUuid, senderName)
                : this.skinResolver.fallbackHead(null, senderName);
        renderHead(context, x, y, resolvedHead.skinTextures(), tintColor);
    }

    private void renderHead(DrawContext context, int x, int y, SkinTextures skinTextures, int tintColor) {
        if (skinTextures == null || skinTextures.body() == null || skinTextures.body().texturePath() == null) {
            return;
        }

        int size = BetterChatHeadsClient.config().headSize;
        if (BetterChatHeadsClient.config().renderOverlayLayer) {
            if (BetterChatHeadsClient.config().chatHead3dness > 0) {
                PlayerSkinDrawer.draw(context, skinTextures.body().texturePath(), x, y, size, false, false, tintColor);

                float scale = 1.0F + (BetterChatHeadsClient.config().chatHead3dness * 0.12F);
                float centerX = x + (size / 2.0F);
                float centerY = y + (size / 2.0F);

                context.getMatrices().pushMatrix();
                context.getMatrices().translate(centerX, centerY);
                context.getMatrices().scale(scale, scale);
                context.getMatrices().translate(-centerX, -centerY);
                PlayerSkinDrawer.draw(context, skinTextures.body().texturePath(), x, y, size, true, false, tintColor);
                context.getMatrices().popMatrix();
                return;
            }

            PlayerSkinDrawer.draw(context, skinTextures, x, y, size, tintColor);
            return;
        }

        PlayerSkinDrawer.draw(context, skinTextures.body().texturePath(), x, y, size, false, false, tintColor);
    }
}

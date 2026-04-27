package com.pheonix.betterchatheads.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.pheonix.betterchatheads.BetterChatHeadsClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(
            method = "onChatMessage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/message/MessageHandler;onChatMessage(Lnet/minecraft/network/message/SignedMessage;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/network/message/MessageType$Parameters;)V"
            )
    )
    private void betterchatheads$cacheVerifiedChatSender(
            ChatMessageS2CPacket packet,
            CallbackInfo ci,
            @Local SignedMessage message,
            @Local PlayerListEntry entry
    ) {
        UUID senderUuid = packet.sender();
        String senderName = entry != null ? entry.getProfile().name() : null;
        BetterChatHeadsClient.ChatLineSender sender = new BetterChatHeadsClient.ChatLineSender(senderUuid, senderName);

        BetterChatHeadsClient.registerChatContentSender(betterchatheads$safeString(packet.unsignedContent()), sender);
        BetterChatHeadsClient.registerChatContentSender(betterchatheads$safeString(message.getContent()), sender);

        MessageType.Parameters parameters = packet.serializedParameters();
        Text decorated = parameters.applyChatDecoration(message.getContent());
        BetterChatHeadsClient.registerChatContentSender(betterchatheads$safeString(decorated), sender);
    }

    private static String betterchatheads$safeString(Text text) {
        return text == null ? null : text.getString();
    }
}

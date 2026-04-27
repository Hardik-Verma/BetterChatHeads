package com.pheonix.betterchatheads.mixin;

import com.mojang.authlib.GameProfile;
import com.pheonix.betterchatheads.BetterChatHeadsClient;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(MessageHandler.class)
public abstract class MessageHandlerMixin {
    @Inject(method = "onChatMessage", at = @At("HEAD"))
    private void betterchatheads$beginVerifiedChatSender(SignedMessage message, GameProfile profile, MessageType.Parameters params, CallbackInfo ci) {
        UUID senderUuid = message != null && !message.isSenderMissing() ? message.getSender() : profile != null ? profile.id() : null;
        String senderName = profile != null ? profile.name() : null;
        BetterChatHeadsClient.beginIncomingChatSender(senderUuid, senderName);
        if (message != null && params != null) {
            Text decorated = params.applyChatDecoration(message.getContent());
            BetterChatHeadsClient.registerChatContentSender(decorated.getString(), new BetterChatHeadsClient.ChatLineSender(senderUuid, senderName));
        }
    }

    @Inject(method = "onChatMessage", at = @At("TAIL"))
    private void betterchatheads$endVerifiedChatSender(SignedMessage message, GameProfile profile, MessageType.Parameters params, CallbackInfo ci) {
        BetterChatHeadsClient.clearIncomingChatSender();
    }

    @Inject(method = "onUnverifiedMessage", at = @At("HEAD"))
    private void betterchatheads$beginUnverifiedChatSender(UUID sender, MessageSignatureData signature, MessageType.Parameters params, CallbackInfo ci) {
        BetterChatHeadsClient.beginIncomingChatSender(sender, null);
    }

    @Inject(method = "onUnverifiedMessage", at = @At("TAIL"))
    private void betterchatheads$endUnverifiedChatSender(UUID sender, MessageSignatureData signature, MessageType.Parameters params, CallbackInfo ci) {
        BetterChatHeadsClient.clearIncomingChatSender();
    }
}

package com.pheonix.betterchatheads.mixin;

import com.pheonix.betterchatheads.BetterChatHeadsClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerListHud.class)
abstract class TabListHeadMixin {
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/ClientConnection;isEncrypted()Z"
            )
    )
    private boolean betterchatheads$forceTabHeadsWhenNeeded(ClientConnection connection) {
        return connection.isEncrypted() || BetterChatHeadsClient.config().enableTabHeads;
    }
}

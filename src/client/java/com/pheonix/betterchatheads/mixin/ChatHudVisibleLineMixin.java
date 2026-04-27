package com.pheonix.betterchatheads.mixin;

import com.pheonix.betterchatheads.BetterChatHeadsClient;
import com.pheonix.betterchatheads.mixininterface.ParsedChatLineHolder;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHudLine.Visible.class)
public abstract class ChatHudVisibleLineMixin implements ParsedChatLineHolder {
    @Unique
    private BetterChatHeadsClient.ParsedChatLine betterchatheads$parsedChatLine;
    @Unique
    private int betterchatheads$lineIndexWithinEntry;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void betterchatheads$attachCurrentParsedLine(CallbackInfo ci) {
        this.betterchatheads$parsedChatLine = BetterChatHeadsClient.currentVisibleChatLineData();
        this.betterchatheads$lineIndexWithinEntry = BetterChatHeadsClient.currentVisibleChatLineIndex();
        BetterChatHeadsClient.incrementCurrentVisibleChatLineIndex();
    }

    @Override
    public void betterchatheads$setParsedChatLine(BetterChatHeadsClient.ParsedChatLine parsedChatLine) {
        this.betterchatheads$parsedChatLine = parsedChatLine;
    }

    @Override
    public BetterChatHeadsClient.ParsedChatLine betterchatheads$getParsedChatLine() {
        return this.betterchatheads$parsedChatLine;
    }

    @Override
    public void betterchatheads$setLineIndexWithinEntry(int lineIndex) {
        this.betterchatheads$lineIndexWithinEntry = lineIndex;
    }

    @Override
    public int betterchatheads$getLineIndexWithinEntry() {
        return this.betterchatheads$lineIndexWithinEntry;
    }
}

package com.pheonix.betterchatheads.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatHud.class)
public interface ChatHudStorageAccessor {
    @Accessor("messages")
    List<ChatHudLine> betterchatheads$getMessages();

    @Accessor("visibleMessages")
    List<ChatHudLine.Visible> betterchatheads$getVisibleMessages();

    @Invoker("addVisibleMessage")
    void betterchatheads$invokeAddVisibleMessage(ChatHudLine line);
}

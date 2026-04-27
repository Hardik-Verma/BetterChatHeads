package com.pheonix.betterchatheads.mixin;

import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.gui.hud.ChatHud$Hud")
public interface ChatHudHudAccessor {
    @Accessor("context")
    DrawContext betterchatheads$getContext();
}

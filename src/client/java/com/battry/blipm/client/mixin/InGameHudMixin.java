package com.battry.blipm.client.mixin;

import com.battry.blipm.client.ImprovedPrivateMessagesClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class InGameHudMixin {

    // 1.21.9+: render(DrawContext, RenderTickCounter) — float tickDelta is gone
    @Inject(method = "render", at = @At("TAIL"))
    private void blipm$renderUnreadBadge(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        ImprovedPrivateMessagesClient client = ImprovedPrivateMessagesClient.INSTANCE;
        if (client == null) return;

        int unread = client.getTotalUnread();
        if (unread <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.screen != null) return;

        String text = "✉ " + (unread > 99 ? "99+" : unread);
        int x = 4;
        int y = context.guiHeight() - 50;

        int textWidth = mc.font.width(text);
        context.fill(x - 2, y - 2, x + textWidth + 4, y + 10, 0xAA000000);
        context.drawString(mc.font, text, x, y, 0xFFFFFFFF);

        if (ImprovedPrivateMessagesClient.openMessagesKey != null) {
            String hint = "[" + ImprovedPrivateMessagesClient.openMessagesKey
                .getTranslatedKeyMessage().getString() + "] Messages";
            context.drawString(mc.font, hint, x, y + 12, 0x88AAAAAA);
        }
    }
}
package com.battry.blipm.client.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;

/**
 * ChatHudMixin — intentionally empty.
 *
 * Chat interception is handled via Fabric's ClientReceiveMessageEvents API
 * in ImprovedPrivateMessagesClient, which covers both game messages and
 * system messages on all server types (online, offline/cracked, LAN).
 * No ChatHud method injection is needed or safe across MC versions.
 */
@Mixin(ChatComponent.class)
public class ChatHudMixin {
}
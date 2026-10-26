package com.battry.blipm.client;

import com.battry.blipm.ImprovedPrivateMessages;
import com.battry.blipm.client.gui.MessagesScreen;
import com.battry.blipm.client.storage.ConversationStorage;
import com.battry.blipm.client.util.WhisperPatterns;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ImprovedPrivateMessagesClient implements ClientModInitializer {

    public static ImprovedPrivateMessagesClient INSTANCE;
    public static KeyMapping openMessagesKey;

    private String currentServer = "singleplayer";
    private int totalUnread = 0;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;

        // ── Keybind ───────────────────────────────────────────────────────────
        KeyMapping.Category blipmCategory = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("improved-private-messages", "category")
        );
        openMessagesKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "blipm.keybind.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            blipmCategory
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMessagesKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new MessagesScreen());
                }
            }
        });

        // ── Chat interception ─────────────────────────────────────────────────
        //
        // GAME  = any message sent by the server (system/plugin output, /msg,
        //         /tell on cracked AND online servers — this is the one we need)
        // CHAT  = signed player-to-player chat (online servers only)
        //
        // Whispers from /msg on cracked servers arrive as GAME messages,
        // so registering GAME is sufficient for the primary use case.
        // We also register CHAT as a fallback for online-mode servers that
        // route /tell through signed chat instead of game messages.

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) handleIncoming(message);
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            handleIncoming(message);
        });

        // ── Server tracking ───────────────────────────────────────────────────
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerInfo info = client.getCurrentServerEntry();
            String address = (info != null && info.address != null)
                ? info.address : "singleplayer";
            onServerJoin(address);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            onServerDisconnect();
        });

        ImprovedPrivateMessages.LOGGER.info("[BLIPM] Initialized. Press M to open messages.");
    }

    private void handleIncoming(Component message) {
        if (message == null) return;
        String raw = message.getString();
        if (raw == null || raw.isBlank()) return;

        if (!WhisperPatterns.looksLikeWhisper(raw)) return;

        WhisperPatterns.WhisperMatch match = WhisperPatterns.parse(raw);
        if (match == null) return;

        ConversationStorage.INSTANCE.addIncomingMessage(match.sender(), match.content());
        this.totalUnread = ConversationStorage.INSTANCE.getTotalUnreadCount();

        ImprovedPrivateMessages.LOGGER.debug(
            "[BLIPM] Intercepted whisper from {}: {}", match.sender(), match.content());
    }

    public void onServerJoin(String serverAddress) {
        this.currentServer = serverAddress;
        ConversationStorage.INSTANCE.loadForServer(serverAddress);
        this.totalUnread = ConversationStorage.INSTANCE.getTotalUnreadCount();
    }

    public void onServerDisconnect() {
        ConversationStorage.INSTANCE.saveCurrentServer();
        this.currentServer = "singleplayer";
        this.totalUnread = 0;
    }

    public String getCurrentServer()      { return currentServer; }
    public int getTotalUnread()           { return totalUnread; }
    public void setTotalUnread(int count) { this.totalUnread = count; }
    public void onWhisperReceived(String sender) {
        this.totalUnread = ConversationStorage.INSTANCE.getTotalUnreadCount();
    }
}
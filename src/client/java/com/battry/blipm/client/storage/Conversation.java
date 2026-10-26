package com.battry.blipm.client.storage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A single conversation with one player on one server.
 *
 * History is capped at MAX_MESSAGES (500). When the cap is exceeded the oldest
 * entry is dropped from the front of the deque — no manual cleanup needed.
 */
public class Conversation {

    public static final int MAX_MESSAGES = 200;

    /** The other player's exact Minecraft username. */
    public String playerName;

    /** Whether this conversation is pinned to the top of the sidebar. */
    public boolean pinned = false;

    /** Optional local-only display nickname. */
    public String nickname = null;

    /** Unread count since last time this conversation was opened in the GUI. */
    public int unreadCount = 0;

    /** Timestamp of the most recent message (used for sidebar sorting). */
    public long lastActivity = 0L;

    private final Deque<StoredMessage> messages = new ArrayDeque<>();

    public Conversation(String playerName) {
        this.playerName = playerName;
    }

    // ── Message management ────────────────────────────────────────────────────

    public void addMessage(StoredMessage msg) {
        messages.addLast(msg);
        lastActivity = msg.timestamp;

        // Enforce cap
        while (messages.size() > MAX_MESSAGES) {
            messages.pollFirst();
        }

        if (msg.direction == StoredMessage.Direction.INCOMING) {
            unreadCount++;
        }
    }

    /** Reset unread counter — call when the player opens this conversation. */
    public void markRead() {
        unreadCount = 0;
    }

    public List<StoredMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    /** Replaces message history — used when deserializing from disk. */
    public void setMessages(List<StoredMessage> list) {
        messages.clear();
        messages.addAll(list);
    }

    public int getMessageCount() {
        return messages.size();
    }

    /** The name shown in the UI: nickname if set, otherwise playerName. */
    public String getDisplayName() {
        return (nickname != null && !nickname.isBlank()) ? nickname : playerName;
    }

    /** One-line preview shown in the sidebar below the player name. */
    public String getLastMessagePreview() {
        if (messages.isEmpty()) return "";
        StoredMessage last = ((ArrayDeque<StoredMessage>) messages).peekLast();
        if (last == null) return "";
        String prefix = last.direction == StoredMessage.Direction.OUTGOING ? "You: " : "";
        String text   = last.content.length() > 40
            ? last.content.substring(0, 37) + "..."
            : last.content;
        return prefix + text;
    }
}
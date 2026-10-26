package com.battry.blipm.client.storage;

/**
 * A single stored message in a conversation.
 *
 * direction:
 *   INCOMING = received from another player (a whisper sent to us)
 *   OUTGOING = we sent this through the BLIPM UI (via /msg)
 */
public class StoredMessage {

    public enum Direction { INCOMING, OUTGOING }

    public final String author;       // Player name
    public final String content;      // Raw text
    public final long timestamp;      // System.currentTimeMillis()
    public final Direction direction;

    public StoredMessage(String author, String content, long timestamp, Direction direction) {
        this.author    = author;
        this.content   = content;
        this.timestamp = timestamp;
        this.direction = direction;
    }

    /** Convenience constructor — timestamps to now. */
    public StoredMessage(String author, String content, Direction direction) {
        this(author, content, System.currentTimeMillis(), direction);
    }

    @Override
    public String toString() {
        return "[" + direction + "] " + author + ": " + content;
    }
}

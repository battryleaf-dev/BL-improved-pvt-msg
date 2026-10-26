package com.battry.blipm.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WhisperPatterns — detects incoming whispers from any server chat format.
 *
 * Supports 15+ patterns covering vanilla, EssentialsX, Hypixel, HMChat, and more.
 * All patterns strip §X color codes before matching.
 *
 * Each pattern MUST have exactly two capturing groups:
 *   group(1) = sender username
 *   group(2) = message content
 *
 * To add support for a new server format, append a Pattern to the static block.
 * Patterns are checked in order and the first match wins, so more specific
 * patterns must come before generic ones.
 */
public final class WhisperPatterns {

    private WhisperPatterns() {}

    public record WhisperMatch(String sender, String content) {}

    private static final Pattern COLOR_CODE = Pattern.compile("§[0-9a-fk-orA-FK-OR]");

    /** One or more literal asterisks (any amount) before a name, e.g. "***Steve". */
    private static final String STARS = "\\*+";

    /** Zero or more literal asterisks (any amount) before a name. */
    private static final String OPT_STARS = "\\**";

    private static final List<Pattern> PATTERNS = new ArrayList<>();

    static {
        // ------------------------------------------------------------
        // Asterisk-prefixed names (must come BEFORE the generic patterns)
        // ------------------------------------------------------------

        // From ***Steve: hello  /  From [VIP] ***Steve: hello  /  From Steve: hello
        PATTERNS.add(p("^From\\s+(?:\\[[^\\]]+\\]\\s*)?" + OPT_STARS + "(\\w+):\\s*(.+)"));

        // *Steve whispers: hello  /  ***Steve whispers to you: hello
        PATTERNS.add(p("^" + STARS + "(\\w+)\\s+whispers(?:\\s+to you)?:\\s*(.+)"));

        // ***Steve -> you: hello
        PATTERNS.add(p("^" + STARS + "(\\w+)\\s*->\\s*(?:me|you):\\s*(.+)"));

        // [***Steve -> me] hello
        PATTERNS.add(p("^\\[" + STARS + "(\\w+)\\s*->\\s*(?:me|you)\\]\\s*(.+)"));

        // [PM] ***Steve: hello
        PATTERNS.add(p("^\\[PM\\]\\s*" + STARS + "(\\w+):\\s*(.+)"));

        // Message from ***Steve: hello  /  Msg from ***Steve: hello
        // PM from ***Steve: hello       /  DM from ***Steve: hello
        PATTERNS.add(p("^(?:Message|Msg|PM|DM) from\\s+" + STARS + "(\\w+):\\s*(.+)"));

        // ------------------------------------------------------------
        // Vanilla / near-vanilla
        // ------------------------------------------------------------
        PATTERNS.add(p("(\\w+) whispers to you: (.+)"));
        PATTERNS.add(p("(\\w+) whispers: (.+)"));

        // [Steve -> me] hello  /  [Steve -> you] hello
        PATTERNS.add(p("\\[(\\w+)\\s*->\\s*(?:me|you)\\]\\s*(.+)"));

        // Steve -> You: hello
        PATTERNS.add(p("(\\w+)\\s*->\\s*[Yy]ou:\\s*(.+)"));

        // [PM from Steve]: hello
        PATTERNS.add(p("\\[PM from (\\w+)\\]:\\s*(.+)"));

        // (From Steve) hello
        PATTERNS.add(p("\\(From (\\w+)\\)\\s*(.+)"));

        // [whisper] Steve -> You: hello
        PATTERNS.add(p("\\[whisper\\]\\s*(\\w+)\\s*->\\s*[Yy]ou:\\s*(.+)"));

        // [Msg] Steve → you: hello
        PATTERNS.add(p("\\[Msg\\]\\s*(\\w+)\\s*[→>]\\s*(?:you|me):\\s*(.+)"));

        // [Private] Steve: hello  (EssentialsX)
        PATTERNS.add(p("\\[Private\\]\\s*(\\w+):\\s*(.+)"));

        // [DM] Steve: hello
        PATTERNS.add(p("\\[DM\\]\\s*(\\w+):\\s*(.+)"));

        // MSG | Steve -> You: hello
        PATTERNS.add(p("MSG\\s*\\|\\s*(\\w+)\\s*->\\s*[Yy]ou:\\s*(.+)"));

        // From Steve: hello  (Hypixel)
        PATTERNS.add(p("From (\\w+): (.+)"));

        // [Steve] (whisper) hello
        PATTERNS.add(p("\\[(\\w+)\\]\\s*\\(whisper\\)\\s*(.+)"));

        // CubeCraft
        PATTERNS.add(p("(\\w+)\\s*>>\\s*(?:me|you)\\s+(.+)"));

        PATTERNS.add(p("\\[(\\w+)\\s*->\\s*(?:me|you)\\]\\s+(.+)"));

        PATTERNS.add(p("\\[[^\\]]+\\]\\s*(\\w+)\\s*->\\s*(?:you|me):\\s*(.+)"));
        PATTERNS.add(p("\\[[^\\]]+\\]\\s*(\\w+)\\s+whispers:\\s*(.+)"));
        PATTERNS.add(p("(\\w+)\\s*[→➜⇒]\\s*(?:you|me):\\s*(.+)"));
        PATTERNS.add(p("Message from (\\w+):\\s*(.+)"));
        PATTERNS.add(p("Msg from (\\w+):\\s*(.+)"));
        PATTERNS.add(p("PM from (\\w+):\\s*(.+)"));
        PATTERNS.add(p("\\[(\\w+)\\s*[-=]+>\\s*(?:me|you)\\]\\s*(.+)"));
        PATTERNS.add(p("\\[PM\\]\\s*(\\w+):\\s*(.+)"));
        PATTERNS.add(p("DM from (\\w+):\\s*(.+)"));

        // ✉ Steve: hello  (some fancy servers)
        PATTERNS.add(p("[✉📩]\\s*(\\w+):\\s*(.+)"));
    }

    private static Pattern p(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Attempt to parse an incoming chat line as a whisper.
     *
     * @param rawMessage Raw string (may contain § codes)
     * @return A WhisperMatch if matched, null otherwise
     */
    public static WhisperMatch parse(String rawMessage) {
        String clean = COLOR_CODE.matcher(rawMessage).replaceAll("").trim();
        for (Pattern pattern : PATTERNS) {
            Matcher m = pattern.matcher(clean);
            if (m.find()) {
                String sender  = m.group(1).trim();
                String content = m.group(2).trim();
                // Sanity check: Minecraft names are 1–16 chars, alphanumeric + underscore
                if (!sender.isEmpty() && sender.length() <= 16) {
                    return new WhisperMatch(sender, content);
                }
            }
        }
        return null;
    }

    /**
     * Fast pre-check before doing full regex work.
     * Returns true if the message MIGHT be a whisper.
     */
    public static boolean looksLikeWhisper(String rawMessage) {
        String lower = rawMessage.toLowerCase();
        return lower.contains("whisper")
            || lower.contains("-> you")
            || lower.contains("-> me")
            || lower.contains("[pm ")
            || lower.contains("(from ")
            || lower.contains("[private]")
            || lower.contains("[dm]")
            || lower.contains("[msg]")
            || lower.contains("from ");
    }
}
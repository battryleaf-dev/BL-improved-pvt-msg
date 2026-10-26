package com.battry.blipm.client.storage;

import com.battry.blipm.ImprovedPrivateMessages;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * ConversationStorage — per-server JSON persistence.
 *
 * On-disk layout inside .minecraft/blipm/:
 *
 *   blipm/
 *     hypixel.net_25565/
 *       conversations.json
 *       friends.json
 *     play.someserver.com_25565/
 *       conversations.json
 *       friends.json
 *     singleplayer/
 *       conversations.json
 *       friends.json
 *
 * Saves immediately after every write (addIncomingMessage, addOutgoingMessage,
 * deleteConversation) and on disconnect. Lightweight given text-only content.
 */
public class ConversationStorage {

    public static final ConversationStorage INSTANCE = new ConversationStorage();

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(StoredMessage.Direction.class, new DirectionAdapter())
        .create();

    /** Root storage directory: .minecraft/blipm/ */
    private final Path rootDir;

    /** In-memory map for the currently loaded server. Key = playerName.toLowerCase() */
    private final Map<String, Conversation> conversations = new LinkedHashMap<>();

    /** Friend list for current server. */
    private final LinkedHashSet<String> friends = new LinkedHashSet<>();

    private String loadedServer = null;

    private ConversationStorage() {
        rootDir = FabricLoader.getInstance().getGameDir().resolve("blipm");
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            ImprovedPrivateMessages.LOGGER.error("[BLIPM] Failed to create storage root: {}", e.getMessage());
        }
    }

    // ── Server lifecycle ──────────────────────────────────────────────────────

    public void loadForServer(String serverAddress) {
        if (loadedServer != null) {
            saveCurrentServer();
        }
        conversations.clear();
        friends.clear();
        loadedServer = serverAddress;

        Path serverDir = getServerDir(serverAddress);
        try { Files.createDirectories(serverDir); } catch (IOException ignored) {}

        loadConversations(serverDir);
        loadFriends(serverDir);
    }

    public void saveCurrentServer() {
        if (loadedServer == null) return;
        Path serverDir = getServerDir(loadedServer);
        saveConversations(serverDir);
        saveFriends(serverDir);
    }

    // ── Conversation access ───────────────────────────────────────────────────

    public Conversation getOrCreate(String playerName) {
        return conversations.computeIfAbsent(
            playerName.toLowerCase(Locale.ROOT),
            k -> new Conversation(playerName)
        );
    }

    public Optional<Conversation> get(String playerName) {
        return Optional.ofNullable(conversations.get(playerName.toLowerCase(Locale.ROOT)));
    }

    /** Returns the conversation for the given player name, or null if not found. */
    public Conversation getByName(String playerName) {
        return conversations.get(playerName.toLowerCase(Locale.ROOT));
    }

    /** All conversations sorted: pinned first, then by lastActivity descending. */
    public List<Conversation> getAllSorted() {
        List<Conversation> list = new ArrayList<>(conversations.values());
        list.sort((a, b) -> {
            if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
            return Long.compare(b.lastActivity, a.lastActivity);
        });
        return list;
    }

    public void addIncomingMessage(String sender, String content) {
        getOrCreate(sender).addMessage(
            new StoredMessage(sender, content, StoredMessage.Direction.INCOMING));
        saveCurrentServer();
    }

    public void addOutgoingMessage(String recipient, String content, String selfName) {
        getOrCreate(recipient).addMessage(
            new StoredMessage(selfName, content, StoredMessage.Direction.OUTGOING));
        saveCurrentServer();
    }

    public void deleteConversation(String playerName) {
        conversations.remove(playerName.toLowerCase(Locale.ROOT));
        saveCurrentServer();
    }

    public int getTotalUnreadCount() {
        return conversations.values().stream().mapToInt(c -> c.unreadCount).sum();
    }

    // ── Friend list ───────────────────────────────────────────────────────────

    public void addFriend(String name)    { friends.add(name);    saveCurrentServer(); }
    public void removeFriend(String name) { friends.remove(name); saveCurrentServer(); }
    public boolean isFriend(String name)  { return friends.contains(name); }
    public List<String> getFriends()      { return new ArrayList<>(friends); }

    // ── JSON I/O ──────────────────────────────────────────────────────────────

    private void loadConversations(Path serverDir) {
        Path file = serverDir.resolve("conversations.json");
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<ConversationJson>>(){}.getType();
            List<ConversationJson> data = GSON.fromJson(r, type);
            if (data == null) return;
            for (ConversationJson cj : data) {
                Conversation c = new Conversation(cj.playerName);
                c.pinned       = cj.pinned;
                c.nickname     = cj.nickname;
                c.unreadCount  = cj.unreadCount;
                c.lastActivity = cj.lastActivity;
                if (cj.messages != null) c.setMessages(cj.messages);
                conversations.put(cj.playerName.toLowerCase(Locale.ROOT), c);
            }
        } catch (Exception e) {
            ImprovedPrivateMessages.LOGGER.error("[BLIPM] Failed to load conversations: {}", e.getMessage());
        }
    }

    private void saveConversations(Path serverDir) {
        Path file = serverDir.resolve("conversations.json");
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            List<ConversationJson> data = new ArrayList<>();
            for (Conversation c : conversations.values()) {
                ConversationJson cj = new ConversationJson();
                cj.playerName   = c.playerName;
                cj.pinned       = c.pinned;
                cj.nickname     = c.nickname;
                cj.unreadCount  = c.unreadCount;
                cj.lastActivity = c.lastActivity;
                cj.messages     = c.getMessages();
                data.add(cj);
            }
            GSON.toJson(data, w);
        } catch (Exception e) {
            ImprovedPrivateMessages.LOGGER.error("[BLIPM] Failed to save conversations: {}", e.getMessage());
        }
    }

    private void loadFriends(Path serverDir) {
        Path file = serverDir.resolve("friends.json");
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<List<String>>(){}.getType();
            List<String> data = GSON.fromJson(r, type);
            if (data != null) friends.addAll(data);
        } catch (Exception e) {
            ImprovedPrivateMessages.LOGGER.error("[BLIPM] Failed to load friends: {}", e.getMessage());
        }
    }

    private void saveFriends(Path serverDir) {
        Path file = serverDir.resolve("friends.json");
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(new ArrayList<>(friends), w);
        } catch (Exception e) {
            ImprovedPrivateMessages.LOGGER.error("[BLIPM] Failed to save friends: {}", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Path getServerDir(String server) {
        String safe = server
            .replaceAll("[^a-zA-Z0-9._\\-]", "_")
            .toLowerCase(Locale.ROOT);
        return rootDir.resolve(safe);
    }

    // ── JSON DTOs ─────────────────────────────────────────────────────────────

    private static class ConversationJson {
        String playerName;
        boolean pinned;
        String nickname;
        int unreadCount;
        long lastActivity;
        List<StoredMessage> messages;
    }

    private static class DirectionAdapter
        implements JsonSerializer<StoredMessage.Direction>, JsonDeserializer<StoredMessage.Direction> {

        @Override
        public JsonElement serialize(StoredMessage.Direction src, java.lang.reflect.Type t, JsonSerializationContext ctx) {
            return new JsonPrimitive(src.name());
        }
        @Override
        public StoredMessage.Direction deserialize(JsonElement json, java.lang.reflect.Type t, JsonDeserializationContext ctx) {
            return StoredMessage.Direction.valueOf(json.getAsString());
        }
    }
}
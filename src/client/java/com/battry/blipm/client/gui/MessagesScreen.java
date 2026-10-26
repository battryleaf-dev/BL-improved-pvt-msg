package com.battry.blipm.client.gui;

import com.battry.blipm.client.ImprovedPrivateMessagesClient;
import com.battry.blipm.client.storage.Conversation;
import com.battry.blipm.client.storage.ConversationStorage;
import com.battry.blipm.client.storage.StoredMessage;
import com.mojang.authlib.GameProfile;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

public class MessagesScreen extends Screen {

    // ── Tabs ──────────────────────────────────────────────────────────────────
    private enum Tab { ONLINE, FRIENDS, OFFLINE }
    private Tab activeTab = Tab.ONLINE;

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int SIDEBAR_W  = 200;
    private static final int HEADER_H   = 24;
    private static final int TAB_H      = 22;   // tab bar height below header
    private static final int ENTRY_H    = 44;
    private static final int HEAD_SIZE  = 28;
    private static final int INPUT_H    = 22;
    private static final int PAD        = 6;
    private static final int SEARCH_H   = 18;
    private static final int NEW_BTN_H  = 20;
    private static final int MSG_HEAD   = 20;
    private static final int LINE_H     = 10;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_BG        = 0xFF1E1F22;
    private static final int C_SIDEBAR   = 0xFF2B2D31;
    private static final int C_PANEL     = 0xFF313338;
    private static final int C_HEADER    = 0xFF1E1F22;
    private static final int C_SELECTED  = 0xFF404249;
    private static final int C_HOVER     = 0xFF35373C;
    private static final int C_SEP       = 0xFF3F4147;
    private static final int C_TEXT      = 0xFFDCDDDE;
    private static final int C_NAME      = 0xFFFFFFFF;
    private static final int C_DIM       = 0xFF96989D;
    private static final int C_BADGE     = 0xFFED4245;
    private static final int C_PINNED    = 0xFFFFCC44;
    private static final int C_INPUT_BG  = 0xFF383A40;
    private static final int C_TIMESTAMP = 0xFF72767D;
    private static final int C_TAB_BG    = 0xFF232428;
    private static final int C_TAB_ACT   = 0xFF404249;
    private static final int C_TAB_TXT   = 0xFF96989D;
    private static final int C_TAB_ATXT  = 0xFFFFFFFF;
    private static final int C_ONLINE    = 0xFF3BA55D;   // green dot
    private static final int C_OFFLINE   = 0xFF747F8D;   // grey dot

    // ── Context menu ──────────────────────────────────────────────────────────
    private Conversation ctxConv     = null;  // which conv the menu is for
    private int          ctxX        = 0;
    private int          ctxY        = 0;
    private boolean      ctxVisible  = false;

    // ── State ─────────────────────────────────────────────────────────────────
    private Conversation selectedConversation = null;
    private int chatScroll    = 0;
    private int sidebarScroll = 0;  // pixels scrolled down in the contact list

    private EditBox messageInput;
    private EditBox searchField;
    private EditBox newConvField;
    private Button    newConvGoBtn;
    private boolean showingNewConvField = false;
    private String  newConvError        = null;
    private long    newConvErrorTime    = 0;

    // ── Emoticon picker ───────────────────────────────────────────────────────
    private boolean emoticonPickerOpen  = false;
    private int     emoticonPickerX     = 0;
    private int     emoticonPickerY     = 0;
    private int     emoticonScroll      = 0;  // row offset
    private static final int EMO_COLS   = 4;
    private static final int EMO_W      = 58;
    private static final int EMO_H      = 16;
    private static final int EMO_PAD    = 4;
    private static final int EMO_ROWS_V = 6;  // visible rows
    private static final String[][] EMOTICONS = {
        {":)", ":-)", ":D", ":-D", "=D", "^_^", "^~^", "uwu"},
        {":(", ":-(", ":/", ":-/", "T_T", "TT_TT", "T~T", "QQ"},
        {":O", ":-O", "o_O", "O_o", "o.O", "O.o", "0_0", "O_O"},
        {";)", ";-)", ";D", ";-D", "^_~", "~_^", ";3", ";^)"},
        {">:(", ">:-(", ">:O", ">.<", ">.>", "<.<", ">_<", "D:<"},
        {"<3", "</3", ":*", ":-*", "^3^", "(^3^)", "(*^3^*)", "xoxo"},
        {"lol", "lmao", "rofl", "xD", "xd", "haha", "hehe", "hihi"},
        {"(y)", "(n)", "\\o/", "o/", "\\o", "m(", "orz", "OTL"},
        {"( 'o') J", "(TT--TT)", "( ; '^')", ">_<", ">^<", "J_^l", "\\(^)/", "(*^_^*)"},
        {"o/", "bye~", "cya", "bbl", "gtg", "brb", "afk", "wb"},
        {"^_____^", "~_~", "+_+", "=.=", ":-3", ";D", "(⌐■_■)", "(•_•)",
         "(●'◡'●)", "(ʘ ͟ʖ ʘ)", "(ʘ ͜ʖ ʘ)", "( ͡• ͜ʖ ͡• )", "( ͡~ ͜ʖ ͡°)",
         "( ͡° ͜ʖ ͡°)", "( ͠° ͟ʖ ͡°)", "( ͡ಠ ʖ̯ ͡ಠ)", "( ఠ ͟ʖ ఠ)",
         "¯\\_(ツ)_/¯", "¯\\_( ͡° ͜ʖ ͡°)_/¯", "(╬▔皿▔)╯", "(⊙ˍ⊙)",
         "(⊙_⊙)？", "(＠_＠;)", "(。﹏。)", "._______.", "(￣▽￣)",
         "♨︎_♨︎", "⚆_⚆", "(•ˋ _ ˊ•)", "(⊙_◎)"},
    };

    private final List<Button> convButtons = new ArrayList<>();
    private final List<Button> tabButtons  = new ArrayList<>();

    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("h:mm a");
    // ── Chat RAM: remembers the last opened conversation across screen opens ──
    private static String lastOpenedPlayerName = null;
    private static final Map<String, PlayerSkin> skinCache = new HashMap<>();

    public MessagesScreen() {
        super(Component.translatable("blipm.gui.title"));
    }

    // ── init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        convButtons.clear();
        tabButtons.clear();

        int panelX = SIDEBAR_W + 1;
        int panelW = this.width - panelX;
        int inputY = this.height - INPUT_H - PAD;
        int inputW = panelW - 70 - PAD * 4 - 22;

        // Header close button
        this.addRenderableWidget(Button.builder(
            Component.literal("✕ Close"), btn -> this.onClose()
        ).bounds(this.width - 72, 2, 70, 18).build());

        // ── Tab buttons ───────────────────────────────────────────────────────
        int tabW = (SIDEBAR_W - 2) / 3;
        int tabY = HEADER_H;
        String[] labels = { "Online", "Friends", "Offline" };
        Tab[]    tabs   = Tab.values();
        for (int i = 0; i < 3; i++) {
            final Tab t = tabs[i];
            final String lbl = labels[i];
            int tx = 1 + i * tabW;
            Button tb = Button.builder(Component.literal(lbl), btn -> {
                activeTab = t;
                sidebarScroll = 0;
                rebuildConvButtons();
            }).bounds(tx, tabY, tabW, TAB_H).build();
            tb.setAlpha(0f);
            tabButtons.add(tb);
            this.addRenderableWidget(tb);
        }

        // Search field (below tab bar)
        int searchY = HEADER_H + TAB_H + PAD;
        searchField = new EditBox(
            this.font, PAD, searchY, SIDEBAR_W - PAD * 2, SEARCH_H, Component.literal(""));
        searchField.setMaxLength(32);
        searchField.setHint(Component.literal("Search..."));
        searchField.setResponder(s -> rebuildConvButtons());
        this.addRenderableWidget(searchField);

        // New conversation button
        int newBtnY = this.height - NEW_BTN_H - PAD;
        this.addRenderableWidget(Button.builder(
            Component.literal("+ New Chat"), btn -> toggleNewConvField()
        ).bounds(PAD, newBtnY, SIDEBAR_W - PAD * 2, NEW_BTN_H).build());

        // New conv field + Go button
        int goBtnW = 30;
        int fieldW = SIDEBAR_W - PAD * 2 - PAD - goBtnW;
        int fieldY = newBtnY - SEARCH_H - PAD;
        newConvField = new EditBox(
            this.font, PAD, fieldY, fieldW, SEARCH_H, Component.literal(""));
        newConvField.setMaxLength(16);
        newConvField.setHint(Component.literal("Player name..."));
        newConvField.setVisible(false);
        this.addRenderableWidget(newConvField);

        newConvGoBtn = Button.builder(
            Component.literal("Go"), btn -> startNewConversation()
        ).bounds(PAD + fieldW + PAD, fieldY, goBtnW, SEARCH_H).build();
        newConvGoBtn.visible = false;
        this.addRenderableWidget(newConvGoBtn);

        // Message input + Send
        messageInput = new EditBox(
            this.font, panelX + PAD, inputY, inputW, INPUT_H, Component.literal(""));
        messageInput.setMaxLength(256);
        updateInputPlaceholder();
        this.addRenderableWidget(messageInput);

        // Emoticon picker button
        int emoBtnX = panelX + PAD + inputW + PAD;
        this.addRenderableWidget(Button.builder(
            Component.literal("\u263A"), btn -> toggleEmoticonPicker(emoBtnX, inputY)
        ).bounds(emoBtnX, inputY, 22, INPUT_H).build());

        // Send button
        this.addRenderableWidget(Button.builder(
            Component.translatable("blipm.gui.send"), btn -> sendMessage()
        ).bounds(emoBtnX + 22 + PAD, inputY, 48, INPUT_H).build());

        rebuildConvButtons();

        // Chat RAM: restore the last opened conversation, fall back to first in list
        if (selectedConversation == null) {
            Conversation restored = null;
            if (lastOpenedPlayerName != null) {
                restored = ConversationStorage.INSTANCE.getByName(lastOpenedPlayerName);
            }
            if (restored != null) {
                // Find which tab this conversation belongs to and switch to it
                Set<String> online = getOnlineNames();
                boolean isOnline = online.contains(restored.playerName.toLowerCase(Locale.ROOT));
                boolean isFriend = ConversationStorage.INSTANCE.isFriend(restored.playerName);
                if (isOnline)       activeTab = Tab.ONLINE;
                else if (isFriend)  activeTab = Tab.FRIENDS;
                else                activeTab = Tab.OFFLINE;
                rebuildConvButtons();
                selectConversation(restored);
            } else {
                List<Conversation> all = getTabConversations();
                if (!all.isEmpty()) selectConversation(all.get(0));
            }
        }
    }

    // ── Tab filtering ─────────────────────────────────────────────────────────

    /** Returns the online player names set from the current server tab list. */
    private Set<String> getOnlineNames() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return Collections.emptySet();
        return mc.getConnection().getOnlinePlayers().stream()
            .map(e -> e.getProfile().name().toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
    }

    /**
     * Returns conversations for the active tab, sorted: starred first, then by lastActivity.
     */
    private List<Conversation> getTabConversations() {
        String query = searchField != null ? searchField.getValue().toLowerCase() : "";
        Set<String> online = getOnlineNames();
        List<Conversation> all = ConversationStorage.INSTANCE.getAllSorted();

        List<Conversation> filtered = all.stream().filter(c -> {
            if (!query.isBlank() && !c.playerName.toLowerCase().contains(query)) return false;
            boolean isOnline = online.contains(c.playerName.toLowerCase(Locale.ROOT));
            boolean isFriend = ConversationStorage.INSTANCE.isFriend(c.playerName);
            return switch (activeTab) {
                case ONLINE  -> isOnline;
                case FRIENDS -> isFriend;
                case OFFLINE -> !isOnline;
            };
        }).collect(Collectors.toList());

        // Sort: starred first, then by lastActivity descending
        filtered.sort((a, b) -> {
            if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
            return Long.compare(b.lastActivity, a.lastActivity);
        });

        return filtered;
    }

    private void rebuildConvButtons() {
        for (Button b : convButtons) this.removeWidget(b);
        convButtons.clear();

        List<Conversation> convs = getTabConversations();

        int listStartY = HEADER_H + TAB_H + PAD + SEARCH_H + PAD;
        int listMaxY   = this.height - NEW_BTN_H - PAD * 2 - (showingNewConvField ? SEARCH_H + PAD : 0);

        // Clamp sidebarScroll so we can't scroll past the last entry
        int totalContentH = convs.size() * ENTRY_H;
        int visibleH = listMaxY - listStartY;
        sidebarScroll = Math.max(0, Math.min(sidebarScroll, Math.max(0, totalContentH - visibleH)));

        for (int i = 0; i < convs.size(); i++) {
            final Conversation conv = convs.get(i);
            int y = listStartY + i * ENTRY_H - sidebarScroll;
            // Only add a button if the entry is (at least partially) visible
            if (y + ENTRY_H <= listStartY || y >= listMaxY) continue;
            Button btn = Button.builder(Component.empty(), b -> {
                ctxVisible = false;
                selectConversation(conv);
            }).bounds(0, y, SIDEBAR_W, ENTRY_H).build();
            btn.setAlpha(0f);
            convButtons.add(btn);
            this.addRenderableWidget(btn);
        }
    }

    private void toggleNewConvField() {
        showingNewConvField = !showingNewConvField;
        newConvField.setVisible(showingNewConvField);
        if (newConvGoBtn != null) newConvGoBtn.visible = showingNewConvField;
        if (showingNewConvField) { newConvField.setValue(""); setFocused(newConvField); }
        rebuildConvButtons();
    }

    private void startNewConversation() {
        String name = newConvField.getValue().trim();
        if (name.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        boolean online = false;
        if (mc.getConnection() != null) {
            for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
                if (entry.getProfile().name().equalsIgnoreCase(name)) {
                    online = true;
                    name = entry.getProfile().name();
                    break;
                }
            }
        }

        if (!online) {
            newConvError = "Player is not online!";
            newConvErrorTime = System.currentTimeMillis();
            return;
        }

        newConvError = null;
        Conversation conv = ConversationStorage.INSTANCE.getOrCreate(name);
        selectConversation(conv);
        showingNewConvField = false;
        newConvField.setVisible(false);
        if (newConvGoBtn != null) newConvGoBtn.visible = false;
        newConvField.setValue("");
        activeTab = Tab.ONLINE; // jump to Online tab
        rebuildConvButtons();
    }

    // ── render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, C_PANEL);
        ctx.fill(0, 0, SIDEBAR_W, this.height, C_SIDEBAR);
        ctx.fill(0, 0, this.width, HEADER_H, C_HEADER);
        ctx.fill(SIDEBAR_W, 0, SIDEBAR_W + 1, this.height, C_SEP);
        ctx.fill(SIDEBAR_W + 1, this.height - INPUT_H - PAD * 2, this.width, this.height, C_BG);

        ctx.drawString(font, Component.translatable("blipm.gui.title"), PAD + 2, PAD + 4, C_NAME);

        renderTabBar(ctx);
        renderSidebar(ctx, mouseX, mouseY);
        renderChatPanel(ctx);

        super.render(ctx, mouseX, mouseY, delta);

        // Emoticon picker
        if (emoticonPickerOpen) renderEmoticonPicker(ctx, mouseX, mouseY);

        // Context menu drawn last (on top of everything)
        if (ctxVisible && ctxConv != null) {
            renderContextMenu(ctx, mouseX, mouseY);
        }
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────

    private void renderTabBar(GuiGraphics ctx) {
        ctx.fill(0, HEADER_H, SIDEBAR_W, HEADER_H + TAB_H, C_TAB_BG);

        int tabW = (SIDEBAR_W - 2) / 3;
        String[] labels = { "Online", "Friends", "Offline" };
        Tab[]    tabs   = Tab.values();
        Set<String> online = getOnlineNames();

        for (int i = 0; i < 3; i++) {
            Tab t  = tabs[i];
            int tx = 1 + i * tabW;
            int ty = HEADER_H;
            boolean active = activeTab == t;

            if (active) {
                ctx.fill(tx, ty, tx + tabW, ty + TAB_H, C_TAB_ACT);
                // active indicator line at bottom
                ctx.fill(tx + 2, ty + TAB_H - 2, tx + tabW - 2, ty + TAB_H, 0xFF5865F2);
            }

            // Count badge for each tab
            long count = ConversationStorage.INSTANCE.getAllSorted().stream().filter(c -> {
                boolean isOnline = online.contains(c.playerName.toLowerCase(Locale.ROOT));
                boolean isFriend = ConversationStorage.INSTANCE.isFriend(c.playerName);
                return switch (t) {
                    case ONLINE  -> isOnline;
                    case FRIENDS -> isFriend;
                    case OFFLINE -> !isOnline;
                };
            }).count();

            int lblColor = active ? C_TAB_ATXT : C_TAB_TXT;
            String label = labels[i];
            ctx.drawCenteredString(font, Component.literal(label), tx + tabW / 2, ty + 6, lblColor);

            // Tiny count in corner
            if (count > 0) {
                String cnt = count > 99 ? "99+" : String.valueOf(count);
                int cw = font.width(cnt) + 4;
                ctx.fill(tx + tabW - cw - 2, ty + 3, tx + tabW - 2, ty + 11, C_BADGE);
                ctx.drawString(font, Component.literal(cnt), tx + tabW - cw, ty + 4, 0xFFFFFFFF);
            }

            // Separator between tabs
            if (i < 2) ctx.fill(tx + tabW, ty + 3, tx + tabW + 1, ty + TAB_H - 3, C_SEP);
        }

        ctx.fill(0, HEADER_H + TAB_H, SIDEBAR_W, HEADER_H + TAB_H + 1, C_SEP);
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private void renderSidebar(GuiGraphics ctx, int mouseX, int mouseY) {
        List<Conversation> convs = getTabConversations();
        Set<String> online = getOnlineNames();

        int listStartY = HEADER_H + TAB_H + PAD + SEARCH_H + PAD;
        int listMaxY   = this.height - NEW_BTN_H - PAD * 2 - (showingNewConvField ? SEARCH_H + PAD : 0);

        for (int i = 0; i < convs.size(); i++) {
            Conversation c = convs.get(i);
            int ey = listStartY + i * ENTRY_H - sidebarScroll;
            int eb = ey + ENTRY_H;
            if (eb <= listStartY) continue;  // fully above visible area
            if (ey >= listMaxY) break;        // fully below visible area

            boolean selected = c == selectedConversation;
            boolean hovered  = !ctxVisible && mouseX >= 0 && mouseX < SIDEBAR_W && mouseY >= ey && mouseY < eb;

            if (selected)     ctx.fill(2, ey + 1, SIDEBAR_W - 2, eb - 1, C_SELECTED);
            else if (hovered) ctx.fill(2, ey + 1, SIDEBAR_W - 2, eb - 1, C_HOVER);

            // Player head
            int headX = PAD + 2;
            int headY = ey + (ENTRY_H - HEAD_SIZE) / 2;
            drawPlayerHead(ctx, c.playerName, headX, headY, HEAD_SIZE);

            // Online dot (bottom-right of head)
            boolean isOnline = online.contains(c.playerName.toLowerCase(Locale.ROOT));
            int dotColor = isOnline ? C_ONLINE : C_OFFLINE;
            ctx.fill(headX + HEAD_SIZE - 6, headY + HEAD_SIZE - 6,
                     headX + HEAD_SIZE,     headY + HEAD_SIZE, dotColor);

            // Name row
            int tx   = headX + HEAD_SIZE + PAD;
            int nameY = ey + 8;
            String displayName = c.pinned ? ("★ " + c.getDisplayName()) : c.getDisplayName();
            ctx.drawString(font, Component.literal(truncate(displayName, 16)), tx, nameY, selected ? C_NAME : C_TEXT);

            // Friend tag
            if (ConversationStorage.INSTANCE.isFriend(c.playerName)) {
                ctx.drawString(font, Component.literal("♥"), SIDEBAR_W - 14, nameY, 0xFFFF6B9D);
            }

            // Unread badge
            if (c.unreadCount > 0) {
                String badge = c.unreadCount > 9 ? "9+" : String.valueOf(c.unreadCount);
                int bw = font.width(badge) + 6;
                int bx = SIDEBAR_W - bw - PAD;
                if (ConversationStorage.INSTANCE.isFriend(c.playerName)) bx -= 12;
                ctx.fill(bx, ey + 6, bx + bw, ey + 16, C_BADGE);
                ctx.drawString(font, Component.literal(badge), bx + 3, ey + 7, 0xFFFFFFFF);
            }

            // Preview
            String preview = c.getLastMessagePreview();
            if (!preview.isEmpty()) {
                int maxW = SIDEBAR_W - tx - PAD - (c.unreadCount > 0 ? 22 : 0);
                while (preview.length() > 2 && font.width(preview) > maxW)
                    preview = preview.substring(0, preview.length() - 3) + "..";
                ctx.drawString(font, Component.literal(preview), tx, nameY + LINE_H + 2, C_DIM);
            }
        }

        if (convs.isEmpty()) {
            String emptyMsg = switch (activeTab) {
                case ONLINE  -> "No online players";
                case FRIENDS -> "No friends yet";
                case OFFLINE -> "No offline players";
            };
            ctx.drawCenteredString(font, Component.literal(emptyMsg),
                SIDEBAR_W / 2, listStartY + 20, C_DIM);
        }

        // Scrollbar — only shown when content overflows
        int totalContentH = convs.size() * ENTRY_H;
        int visibleH = listMaxY - listStartY;
        if (totalContentH > visibleH) {
            int trackX = SIDEBAR_W - 4;
            ctx.fill(trackX, listStartY, trackX + 3, listMaxY, 0x33FFFFFF);
            int thumbH = Math.max(16, visibleH * visibleH / totalContentH);
            int thumbY = listStartY + (int)((long) sidebarScroll * (visibleH - thumbH) / Math.max(1, totalContentH - visibleH));
            ctx.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xAAFFFFFF);
        }

        // Error message
        if (newConvError != null && showingNewConvField) {
            long elapsed = System.currentTimeMillis() - newConvErrorTime;
            if (elapsed < 3000) {
                int errY = this.height - NEW_BTN_H - PAD * 2 - SEARCH_H - PAD - 12;
                ctx.drawCenteredString(font, Component.literal(newConvError), SIDEBAR_W / 2, errY, 0xFFFF5555);
            } else {
                newConvError = null;
            }
        }
    }

    // ── Context menu ──────────────────────────────────────────────────────────

    private static final int CTX_W  = 110;
    private static final int CTX_IH = 14;  // item height

    private void renderContextMenu(GuiGraphics ctx, int mouseX, int mouseY) {
        if (ctxConv == null) return;
        boolean isFriend = ConversationStorage.INSTANCE.isFriend(ctxConv.playerName);
        boolean isStarred = ctxConv.pinned;

        String[] items = {
            isStarred ? "★ Unstar"   : "☆ Star",
            isFriend  ? "♥ Unfriend" : "♡ Friend",
            "✕ Delete chat"
        };

        int menuH = items.length * CTX_IH + PAD * 2;

        // clamp to screen
        int mx = Math.min(ctxX, this.width  - CTX_W - 2);
        int my = Math.min(ctxY, this.height - menuH - 2);

        ctx.fill(mx, my, mx + CTX_W, my + menuH, 0xFF18191C);
        ctx.fill(mx, my, mx + CTX_W, my + 1, C_SEP);
        ctx.fill(mx, my + menuH - 1, mx + CTX_W, my + menuH, C_SEP);
        ctx.fill(mx, my, mx + 1, my + menuH, C_SEP);
        ctx.fill(mx + CTX_W - 1, my, mx + CTX_W, my + menuH, C_SEP);

        for (int i = 0; i < items.length; i++) {
            int iy = my + PAD + i * CTX_IH;
            boolean hov = mouseX >= mx && mouseX < mx + CTX_W && mouseY >= iy && mouseY < iy + CTX_IH;
            if (hov) ctx.fill(mx + 1, iy, mx + CTX_W - 1, iy + CTX_IH, C_HOVER);
            int col = i == 2 ? 0xFFED4245 : C_TEXT;
            ctx.drawString(font, Component.literal(items[i]), mx + PAD, iy + 2, col);
        }
    }

    private boolean handleContextMenuClick(double mouseX, double mouseY) {
        if (!ctxVisible || ctxConv == null) return false;
        boolean isFriend = ConversationStorage.INSTANCE.isFriend(ctxConv.playerName);

        String[] actions = { "star", "friend", "delete" };
        int menuH = 3 * CTX_IH + PAD * 2;
        int mx = Math.min(ctxX, this.width  - CTX_W - 2);
        int my = Math.min(ctxY, this.height - menuH - 2);

        if (mouseX < mx || mouseX >= mx + CTX_W || mouseY < my || mouseY >= my + menuH) {
            ctxVisible = false;
            return true; // clicked outside — dismiss
        }

        int idx = (int)((mouseY - my - PAD) / CTX_IH);
        if (idx < 0 || idx >= 3) { ctxVisible = false; return true; }

        switch (actions[idx]) {
            case "star"   -> { ctxConv.pinned = !ctxConv.pinned; ConversationStorage.INSTANCE.saveCurrentServer(); }
            case "friend" -> {
                if (isFriend) ConversationStorage.INSTANCE.removeFriend(ctxConv.playerName);
                else          ConversationStorage.INSTANCE.addFriend(ctxConv.playerName);
            }
            case "delete" -> {
                ConversationStorage.INSTANCE.deleteConversation(ctxConv.playerName);
                if (selectedConversation == ctxConv) selectedConversation = null;
                updateInputPlaceholder();
            }
        }

        ctxVisible = false;
        rebuildConvButtons();
        return true;
    }

    // ── Chat panel ────────────────────────────────────────────────────────────

    private void renderChatPanel(GuiGraphics ctx) {
        int px   = SIDEBAR_W + 1;
        int pw   = this.width - px;
        int topY = HEADER_H;
        int botY = this.height - INPUT_H - PAD * 2 - PAD;

        if (selectedConversation == null) {
            ctx.drawCenteredString(font,
                Component.literal("Select a conversation or start a new one"),
                px + pw / 2, topY + (botY - topY) / 2, C_DIM);
            return;
        }

        ctx.fill(px, topY, this.width, topY + HEADER_H, C_HEADER);
        ctx.fill(px, topY + HEADER_H - 1, this.width, topY + HEADER_H, C_SEP);
        drawPlayerHead(ctx, selectedConversation.playerName, px + PAD, topY + 2, 18);
        ctx.drawString(font,
            Component.literal(selectedConversation.getDisplayName()), px + PAD + 18 + PAD, topY + 6, C_NAME);

        int areaTop = topY + HEADER_H + PAD * 4;
        int areaBot = botY - PAD;
        int textMaxW = pw - MSG_HEAD - PAD * 4 - 12;

        List<StoredMessage> msgs = selectedConversation.getMessages();
        if (msgs.isEmpty()) {
            ctx.drawCenteredString(font,
                Component.literal("No messages yet"), px + pw / 2, areaTop + 20, C_DIM);
            return;
        }

        record MsgBlock(StoredMessage msg, List<String> lines, boolean showHeader, int totalH) {}
        List<MsgBlock> blocks = new ArrayList<>();

        for (int i = 0; i < msgs.size(); i++) {
            StoredMessage msg  = msgs.get(i);
            StoredMessage prev = i > 0 ? msgs.get(i - 1) : null;
            boolean showHeader = prev == null || !prev.author.equals(msg.author);
            List<String> lines = wrapText(msg.content, textMaxW);
            int h = (showHeader ? LINE_H + 2 : 0) + lines.size() * (LINE_H + 1) + PAD;
            blocks.add(new MsgBlock(msg, lines, showHeader, h));
        }

        // Render top-down with chatScroll offset
        // chatScroll=0 → newest messages at bottom (default)
        // chatScroll increases → scroll up toward older messages
        int totalContentH = blocks.stream().mapToInt(MsgBlock::totalH).sum();
        int areaH = areaBot - areaTop;
        // startY: where the first (oldest) message top edge is
        int startY = areaBot - totalContentH + chatScroll;

        int renderY = startY;
        for (int i = 0; i < blocks.size(); i++) {
            MsgBlock b = blocks.get(i);
            int blockBot = renderY + b.totalH();

            if (blockBot < areaTop) { renderY += b.totalH(); continue; } // above viewport
            if (renderY > areaBot)  break;                                // below viewport

            int avatarX = px + PAD;
            int textX   = avatarX + MSG_HEAD + PAD + 2;

            if (b.showHeader()) drawPlayerHead(ctx, b.msg().author, avatarX, renderY + 1, MSG_HEAD);

            int lineY = renderY;
            if (b.showHeader()) {
                String timeStr = TIME_FMT.format(new Date(b.msg().timestamp));
                int nameW = font.width(b.msg().author);
                ctx.drawString(font, Component.literal(b.msg().author), textX, lineY + 1, C_NAME);
                ctx.drawString(font, Component.literal(timeStr), textX + nameW + 4, lineY + 2, C_TIMESTAMP);
                lineY += LINE_H + 2;
            }
            for (String line : b.lines()) {
                if (lineY + LINE_H >= areaTop && lineY <= areaBot)
                    ctx.drawString(font, Component.literal(line), textX, lineY + 1, C_TEXT);
                lineY += LINE_H + 1;
            }

            renderY += b.totalH();
        }
    }

    // ── Player head ───────────────────────────────────────────────────────────

    private void drawPlayerHead(GuiGraphics ctx, String playerName, int x, int y, int size) {
        PlayerSkin skin = getSkin(playerName);
        if (skin == null) {
            ctx.fill(x, y, x + size, y + size, 0xFF555555);
            String initial = playerName.isEmpty() ? "?" : playerName.substring(0, 1).toUpperCase();
            ctx.drawCenteredString(font, Component.literal(initial), x + size / 2, y + size / 2 - 4, C_TEXT);
            return;
        }
        net.minecraft.client.gui.components.PlayerFaceRenderer.draw(ctx, skin, x, y, size);
    }

    private PlayerSkin getSkin(String playerName) {
        PlayerSkin cached = skinCache.get(playerName);

        Minecraft mc = Minecraft.getInstance();

        // Always check tab list first — if online, grab fresh skin and update cache
        if (mc.getConnection() != null) {
            for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
                if (entry.getProfile().name().equalsIgnoreCase(playerName)) {
                    PlayerSkin t = entry.getSkin();
                    skinCache.put(playerName, t);
                    return t;
                }
            }
        }

        // Player is offline — return cached skin, never overwrite with null
        if (cached != null) return cached;

        // Never seen before — async fetch, don't write null so we retry next call
        GameProfile profile = new GameProfile(UUID.randomUUID(), playerName);
        mc.getSkinManager().get(profile).thenAccept(t ->
            t.ifPresent(skin -> skinCache.put(playerName, skin))
        );
        return null;
    }

    // ── Word wrap ─────────────────────────────────────────────────────────────

    private List<String> wrapText(String text, int maxW) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) { result.add(""); return result; }
        String rem = text;
        while (!rem.isEmpty()) {
            int fit = 0;
            while (fit < rem.length() && font.width(rem.substring(0, fit + 1)) <= maxW) fit++;
            if (fit == 0) fit = 1;
            if (fit >= rem.length()) { result.add(rem); break; }
            int brk = -1;
            for (int k = fit; k >= Math.max(0, fit - 10); k--)
                if (rem.charAt(k) == ' ') { brk = k; break; }
            if (brk > 0) { result.add(rem.substring(0, brk)); rem = rem.substring(brk + 1); }
            else          { result.add(rem.substring(0, fit)); rem = rem.substring(fit); }
        }
        return result;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void selectConversation(Conversation conv) {
        selectedConversation = conv;
        lastOpenedPlayerName = conv.playerName;  // Chat RAM
        conv.markRead();
        chatScroll = 0;
        if (ImprovedPrivateMessagesClient.INSTANCE != null)
            ImprovedPrivateMessagesClient.INSTANCE.setTotalUnread(
                ConversationStorage.INSTANCE.getTotalUnreadCount());
        updateInputPlaceholder();
        if (messageInput != null) setFocused(messageInput);  // always focus chatbox
    }

    private void sendMessage() {
        if (selectedConversation == null || minecraft == null) return;
        String text = messageInput.getValue().trim();
        if (text.isEmpty()) return;
        String recipient = selectedConversation.playerName;
        if (minecraft.getConnection() != null)
            minecraft.getConnection().sendCommand("msg " + recipient + " " + text);
        String selfName = (minecraft.player != null) ? minecraft.player.getGameProfile().name() : "You";
        ConversationStorage.INSTANCE.addOutgoingMessage(recipient, text, selfName);
        chatScroll = 0;
        messageInput.setValue("");
        rebuildConvButtons();
    }

    private void updateInputPlaceholder() {
        if (messageInput == null) return;
        messageInput.setHint(selectedConversation != null
            ? Component.translatable("blipm.gui.message_placeholder", selectedConversation.getDisplayName())
            : Component.literal("Select a conversation..."));
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 2) + ".." : s;
    }

    // ── Input handling ────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button    = click.button();

        // Right-click on a sidebar entry = open context menu
        if (button == 1 && mouseX < SIDEBAR_W && !ctxVisible) {
            List<Conversation> convs = getTabConversations();
            int listStartY = HEADER_H + TAB_H + PAD + SEARCH_H + PAD;
            int listMaxY   = this.height - NEW_BTN_H - PAD * 2 - (showingNewConvField ? SEARCH_H + PAD : 0);
            for (int i = 0; i < convs.size(); i++) {
                int ey = listStartY + i * ENTRY_H - sidebarScroll;
                if (ey + ENTRY_H <= listStartY) continue;
                if (ey >= listMaxY) break;
                if (mouseY >= ey && mouseY < ey + ENTRY_H) {
                    ctxConv    = convs.get(i);
                    ctxX       = (int) mouseX;
                    ctxY       = (int) mouseY;
                    ctxVisible = true;
                    return true;
                }
            }
        }

        // Emoticon picker click
        if (emoticonPickerOpen) return handleEmoticonClick(mouseX, mouseY);

        // Any click while context menu open — handle or dismiss
        if (ctxVisible) {
            return handleContextMenuClick(mouseX, mouseY);
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.key();
        if (emoticonPickerOpen && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            emoticonPickerOpen = false; return true;
        }
        if (ctxVisible) { ctxVisible = false; return true; }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            if (newConvField != null && newConvField.isFocused() && newConvField.isVisible()) {
                startNewConversation(); return true;
            }
            if (messageInput != null && messageInput.isFocused()) {
                sendMessage(); return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        // Emoticon picker scroll — checked first before anything else
        if (emoticonPickerOpen) {
            java.util.List<String> all2 = new java.util.ArrayList<>();
            for (String[] g : EMOTICONS) java.util.Collections.addAll(all2, g);
            int totalRows2 = (int) Math.ceil(all2.size() / (double) EMO_COLS);
            int pickerW2   = EMO_COLS * (EMO_W + EMO_PAD) + EMO_PAD;
            int pickerH2   = EMO_ROWS_V * (EMO_H + EMO_PAD) + EMO_PAD * 2 + 12;
            if (mouseX >= emoticonPickerX && mouseX < emoticonPickerX + pickerW2
             && mouseY >= emoticonPickerY && mouseY < emoticonPickerY + pickerH2) {
                emoticonScroll -= (int) v;
                emoticonScroll  = Math.max(0, Math.min(emoticonScroll, Math.max(0, totalRows2 - EMO_ROWS_V)));
                return true;
            }
        }
        if (mouseX < SIDEBAR_W) {
            // Scroll the contact list
            List<Conversation> convs = getTabConversations();
            int listStartY = HEADER_H + TAB_H + PAD + SEARCH_H + PAD;
            int listMaxY   = this.height - NEW_BTN_H - PAD * 2 - (showingNewConvField ? SEARCH_H + PAD : 0);
            int visibleH   = listMaxY - listStartY;
            int totalH     = convs.size() * ENTRY_H;
            int maxScroll  = Math.max(0, totalH - visibleH);
            sidebarScroll -= (int)(v * ENTRY_H);
            sidebarScroll  = Math.max(0, Math.min(sidebarScroll, maxScroll));
            rebuildConvButtons(); // reposition invisible hit-boxes
            return true;
        } else if (mouseX > SIDEBAR_W) {
            // wheel up (v>0)   = see older msgs = scroll UP = chatScroll increases
            // wheel down (v<0) = see newer msgs = scroll DOWN = chatScroll decreases toward 0
            int step = (LINE_H + 1) * 3;
            chatScroll += (int)(v * step);
            chatScroll  = Math.max(0, chatScroll);
            chatScroll  = Math.min(chatScroll, computeMaxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, h, v);
    }

    private int computeMaxScroll() {
        if (selectedConversation == null) return 0;
        int pw       = this.width - SIDEBAR_W - 1;
        int textMaxW = pw - MSG_HEAD - PAD * 4 - 12;
        int totalH   = 0;
        List<StoredMessage> msgs = selectedConversation.getMessages();
        for (int i = 0; i < msgs.size(); i++) {
            StoredMessage msg  = msgs.get(i);
            StoredMessage prev = i > 0 ? msgs.get(i - 1) : null;
            boolean showHeader = prev == null || !prev.author.equals(msg.author);
            List<String> lines = wrapText(msg.content, textMaxW);
            totalH += (showHeader ? LINE_H + 2 : 0) + lines.size() * (LINE_H + 1) + PAD;
        }
        int topY  = HEADER_H;
        int botY  = this.height - INPUT_H - PAD * 2 - PAD;
        int areaH = (botY - PAD) - (topY + HEADER_H + PAD);
        return Math.max(0, totalH - areaH);
    }


    // ── Emoticon picker ───────────────────────────────────────────────────────

    private void toggleEmoticonPicker(int btnX, int btnY) {
        emoticonPickerOpen = !emoticonPickerOpen;
        if (emoticonPickerOpen) {
            int totalRows = (int) Math.ceil(
                java.util.Arrays.stream(EMOTICONS).mapToInt(a -> a.length).sum() / (double) EMO_COLS);
            int pickerH = EMO_ROWS_V * (EMO_H + EMO_PAD) + EMO_PAD * 2 + 12; // +12 for header
            int pickerW = EMO_COLS * (EMO_W + EMO_PAD) + EMO_PAD;
            emoticonPickerX = Math.max(SIDEBAR_W + PAD, btnX - pickerW + 22);
            emoticonPickerY = btnY - pickerH - PAD;
            emoticonScroll  = 0;
            ctxVisible      = false;
        }
    }

    private void renderEmoticonPicker(GuiGraphics ctx, int mouseX, int mouseY) {
        // Flatten all emoticons into one list
        java.util.List<String> all = new java.util.ArrayList<>();
        for (String[] group : EMOTICONS) java.util.Collections.addAll(all, group);

        int totalRows = (int) Math.ceil(all.size() / (double) EMO_COLS);
        int pickerW   = EMO_COLS * (EMO_W + EMO_PAD) + EMO_PAD;
        int pickerH   = EMO_ROWS_V * (EMO_H + EMO_PAD) + EMO_PAD * 2 + 12;

        int px = emoticonPickerX;
        int py = emoticonPickerY;

        // Background + border
        ctx.fill(px, py, px + pickerW, py + pickerH, 0xFF18191C);
        ctx.fill(px, py, px + pickerW, py + 1, C_SEP);
        ctx.fill(px, py + pickerH - 1, px + pickerW, py + pickerH, C_SEP);
        ctx.fill(px, py, px + 1, py + pickerH, C_SEP);
        ctx.fill(px + pickerW - 1, py, px + pickerW, py + pickerH, C_SEP);

        // Header
        ctx.drawCenteredString(font, Component.literal("Emoticons"),
            px + pickerW / 2, py + 3, C_DIM);
        ctx.fill(px + 1, py + 12, px + pickerW - 1, py + 13, C_SEP);

        // Clamp scroll
        int maxScroll = Math.max(0, totalRows - EMO_ROWS_V);
        emoticonScroll = Math.max(0, Math.min(emoticonScroll, maxScroll));

        // Draw emoticons
        int contentY = py + 14;
        for (int row = emoticonScroll; row < Math.min(emoticonScroll + EMO_ROWS_V, totalRows); row++) {
            for (int col = 0; col < EMO_COLS; col++) {
                int idx = row * EMO_COLS + col;
                if (idx >= all.size()) break;
                String emo = all.get(idx);
                int ex = px + EMO_PAD + col * (EMO_W + EMO_PAD);
                int ey = contentY + (row - emoticonScroll) * (EMO_H + EMO_PAD);
                boolean hov = mouseX >= ex && mouseX < ex + EMO_W
                           && mouseY >= ey && mouseY < ey + EMO_H;
                if (hov) ctx.fill(ex, ey, ex + EMO_W, ey + EMO_H, C_HOVER);
                String label = emo;
                while (label.length() > 1 && font.width(label) > EMO_W - 4)
                    label = label.substring(0, label.length() - 1);
                ctx.drawString(font, Component.literal(label),
                    ex + 2, ey + (EMO_H - 8) / 2, C_TEXT);
            }
        }

        // Scrollbar — same style as sidebar
        if (totalRows > EMO_ROWS_V) {
            int visibleH = EMO_ROWS_V * (EMO_H + EMO_PAD);
            int totalH   = totalRows  * (EMO_H + EMO_PAD);
            int trackX   = px + pickerW - 4;
            int trackY   = contentY;
            int trackH   = visibleH;
            ctx.fill(trackX, trackY, trackX + 3, trackY + trackH, 0x33FFFFFF);
            int thumbH = Math.max(12, visibleH * visibleH / totalH);
            int thumbY = trackY + (int)((long) emoticonScroll * (visibleH - thumbH) / Math.max(1, totalRows - EMO_ROWS_V));
            ctx.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xAAFFFFFF);
        }
    }

    private boolean handleEmoticonClick(double mouseX, double mouseY) {
        if (!emoticonPickerOpen) return false;

        java.util.List<String> all = new java.util.ArrayList<>();
        for (String[] group : EMOTICONS) java.util.Collections.addAll(all, group);

        int totalRows = (int) Math.ceil(all.size() / (double) EMO_COLS);
        int pickerW   = EMO_COLS * (EMO_W + EMO_PAD) + EMO_PAD;
        int pickerH   = EMO_ROWS_V * (EMO_H + EMO_PAD) + EMO_PAD * 2 + 12;

        int px = emoticonPickerX;
        int py = emoticonPickerY;

        // Click outside = close
        if (mouseX < px || mouseX >= px + pickerW || mouseY < py || mouseY >= py + pickerH) {
            emoticonPickerOpen = false;
            return true;
        }

        int contentY = py + 14;
        for (int row = emoticonScroll; row < Math.min(emoticonScroll + EMO_ROWS_V, totalRows); row++) {
            for (int col = 0; col < EMO_COLS; col++) {
                int idx = row * EMO_COLS + col;
                if (idx >= all.size()) break;
                int ex = px + EMO_PAD + col * (EMO_W + EMO_PAD);
                int ey = contentY + (row - emoticonScroll) * (EMO_H + EMO_PAD);
                if (mouseX >= ex && mouseX < ex + EMO_W && mouseY >= ey && mouseY < ey + EMO_H) {
                    // Insert emoticon into message input
                    if (messageInput != null) {
                        String current = messageInput.getValue();
                        String insert  = all.get(idx);
                        // Add space before if needed
                        if (!current.isEmpty() && !current.endsWith(" ")) insert = " " + insert;
                        messageInput.setValue(current + insert);
                        messageInput.moveCursorToEnd(false);
                        setFocused(messageInput);
                    }
                    emoticonPickerOpen = false;
                    return true;
                }
            }
        }
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics ctx, int mouseX, int mouseY, float delta) {}
}
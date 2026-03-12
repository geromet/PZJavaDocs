/*
 * Decompiled with CFR 0.152.
 */
package zombie.chat.defaultChats;

import zombie.characters.IsoPlayer;
import zombie.chat.ChatSettings;
import zombie.chat.ChatTab;
import zombie.chat.defaultChats.RangeBasedChat;
import zombie.core.Color;
import zombie.core.network.ByteBufferReader;
import zombie.network.chat.ChatType;

public class ShoutChat
extends RangeBasedChat {
    public ShoutChat(ByteBufferReader bb, ChatTab tab, IsoPlayer owner) {
        super(bb, ChatType.shout, tab, owner);
        if (!this.isCustomSettings()) {
            this.setSettings(ShoutChat.getDefaultSettings());
        }
    }

    public ShoutChat(int id, ChatTab tab) {
        super(id, ChatType.shout, tab);
        if (!this.isCustomSettings()) {
            this.setSettings(ShoutChat.getDefaultSettings());
        }
    }

    public ShoutChat() {
        super(ChatType.shout);
        this.setSettings(ShoutChat.getDefaultSettings());
    }

    public static ChatSettings getDefaultSettings() {
        ChatSettings settings = new ChatSettings();
        settings.setBold(true);
        settings.setFontColor(new Color(255, 51, 51, 255));
        settings.setShowAuthor(true);
        settings.setShowChatTitle(true);
        settings.setShowTimestamp(true);
        settings.setUnique(true);
        settings.setAllowColors(false);
        settings.setAllowFonts(false);
        settings.setAllowBBcode(false);
        settings.setEqualizeLineHeights(true);
        settings.setRange(60.0f);
        return settings;
    }
}


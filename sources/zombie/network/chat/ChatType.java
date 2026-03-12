/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.chat;

public enum ChatType {
    notDefined(-1, ""),
    general(0, "UI_chat_general_chat_title_id"),
    whisper(1, "UI_chat_private_chat_title_id"),
    say(2, "UI_chat_local_chat_title_id"),
    shout(3, "UI_chat_local_chat_title_id"),
    faction(4, "UI_chat_faction_chat_title_id"),
    safehouse(5, "UI_chat_safehouse_chat_title_id"),
    radio(6, "UI_chat_radio_chat_title_id"),
    admin(7, "UI_chat_admin_chat_title_id"),
    server(8, "UI_chat_server_chat_title_id");

    private final int value;
    private final String titleId;

    public static ChatType valueOf(Integer value) {
        if (ChatType.general.value == value) {
            return general;
        }
        if (ChatType.whisper.value == value) {
            return whisper;
        }
        if (ChatType.say.value == value) {
            return say;
        }
        if (ChatType.shout.value == value) {
            return shout;
        }
        if (ChatType.faction.value == value) {
            return faction;
        }
        if (ChatType.safehouse.value == value) {
            return safehouse;
        }
        if (ChatType.radio.value == value) {
            return radio;
        }
        if (ChatType.admin.value == value) {
            return admin;
        }
        if (ChatType.server.value == value) {
            return server;
        }
        return notDefined;
    }

    private ChatType(Integer value, String titleId) {
        this.value = value;
        this.titleId = titleId;
    }

    public int getValue() {
        return this.value;
    }

    public String getTitleID() {
        return this.titleId;
    }
}


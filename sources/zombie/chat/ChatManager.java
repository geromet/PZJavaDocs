/*
 * Decompiled with CFR 0.152.
 */
package zombie.chat;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.Lua.LuaEventManager;
import zombie.SystemDisabler;
import zombie.characters.Faction;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatBase;
import zombie.chat.ChatMessage;
import zombie.chat.ChatTab;
import zombie.chat.ChatUtility;
import zombie.chat.ServerChatMessage;
import zombie.chat.WhisperChatCreation;
import zombie.chat.defaultChats.AdminChat;
import zombie.chat.defaultChats.FactionChat;
import zombie.chat.defaultChats.GeneralChat;
import zombie.chat.defaultChats.RadioChat;
import zombie.chat.defaultChats.SafehouseChat;
import zombie.chat.defaultChats.SayChat;
import zombie.chat.defaultChats.ServerChat;
import zombie.chat.defaultChats.ShoutChat;
import zombie.chat.defaultChats.WhisperChat;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.raknet.VoiceManagerData;
import zombie.debug.DebugLog;
import zombie.inventory.types.Radio;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.chat.ChatType;
import zombie.radio.devices.DeviceData;

public class ChatManager {
    private static ChatManager instance;
    private UdpConnection serverConnection;
    private final HashMap<Integer, ChatBase> mpChats;
    private final HashMap<String, WhisperChat> whisperChats;
    private final HashMap<String, WhisperChatCreation> whisperChatCreation = new HashMap();
    private final HashMap<Short, ChatTab> tabs;
    private ChatTab focusTab;
    private IsoPlayer player;
    private String myNickname;
    private boolean singlePlayerMode;
    private GeneralChat generalChat;
    private SayChat sayChat;
    private ShoutChat shoutChat;
    private FactionChat factionChat;
    private SafehouseChat safehouseChat;
    private RadioChat radioChat;
    private AdminChat adminChat;
    private ServerChat serverChat;
    private Stage chatManagerStage = Stage.notStarted;
    private static volatile ZLogger logger;
    private static final String logNamePrefix = "client chat";

    private ChatManager() {
        this.mpChats = new HashMap();
        this.tabs = new HashMap();
        this.whisperChats = new HashMap();
    }

    public static ChatManager getInstance() {
        if (instance == null) {
            instance = new ChatManager();
        }
        return instance;
    }

    public boolean isSinglePlayerMode() {
        return this.singlePlayerMode;
    }

    public boolean isWorking() {
        return this.chatManagerStage == Stage.working;
    }

    public void init(boolean isSinglePlayer, IsoPlayer owner) {
        LoggerManager.init();
        LoggerManager.createLogger("client chat " + owner.getDisplayName(), Core.debug);
        logger = LoggerManager.getLogger("client chat " + owner.getDisplayName());
        logger.write("Init chat system...", "info");
        logger.write("Mode: " + (isSinglePlayer ? "single player" : "multiplayer"), "info");
        if (SystemDisabler.printDetailedInfo()) {
            logger.write("Chat owner: " + owner.getDisplayName(), "info");
        }
        this.chatManagerStage = Stage.starting;
        this.singlePlayerMode = isSinglePlayer;
        this.generalChat = null;
        this.sayChat = null;
        this.shoutChat = null;
        this.factionChat = null;
        this.safehouseChat = null;
        this.radioChat = null;
        this.adminChat = null;
        this.serverChat = null;
        this.mpChats.clear();
        this.tabs.clear();
        this.focusTab = null;
        this.whisperChats.clear();
        this.player = owner;
        this.myNickname = this.player.username;
        if (isSinglePlayer) {
            this.serverConnection = null;
            this.sayChat = new SayChat();
            this.sayChat.Init();
            this.generalChat = new GeneralChat();
            this.shoutChat = new ShoutChat();
            this.shoutChat.Init();
            this.radioChat = new RadioChat();
            this.radioChat.Init();
            this.adminChat = new AdminChat();
        } else {
            this.serverConnection = GameClient.connection;
            LuaEventManager.triggerEvent("OnChatWindowInit");
        }
    }

    public void processInitPlayerChatPacket(ByteBufferReader bb) {
        this.init(false, IsoPlayer.getInstance());
        int cnt = bb.getShort();
        for (int i = 0; i < cnt; ++i) {
            ChatTab newTab = new ChatTab(bb.getShort(), bb.getUTF());
            this.tabs.put(newTab.getID(), newTab);
        }
        this.addTab((short)0);
        this.focusOnTab(this.tabs.get((short)0).getID());
        LuaEventManager.triggerEvent("OnSetDefaultTab", this.tabs.get((short)0).getTitle());
    }

    public void setFullyConnected() {
        this.chatManagerStage = Stage.working;
    }

    public void processAddTabPacket(ByteBufferReader bb) {
        this.addTab(bb.getShort());
    }

    public void processRemoveTabPacket(ByteBufferReader bb) {
        this.removeTab(bb.getShort());
    }

    public void processJoinChatPacket(ByteBufferReader bb) {
        ChatBase joinedChat;
        ChatType type = ChatType.valueOf(bb.getInt());
        ChatTab tab = this.tabs.get(bb.getShort());
        switch (type) {
            case general: {
                this.generalChat = new GeneralChat(bb, tab, this.player);
                joinedChat = this.generalChat;
                break;
            }
            case say: {
                this.sayChat = new SayChat(bb, tab, this.player);
                this.sayChat.Init();
                joinedChat = this.sayChat;
                break;
            }
            case shout: {
                this.shoutChat = new ShoutChat(bb, tab, this.player);
                this.shoutChat.Init();
                joinedChat = this.shoutChat;
                break;
            }
            case whisper: {
                WhisperChat chat = new WhisperChat(bb, tab, this.player);
                chat.init();
                this.whisperChats.put(chat.getCompanionName(), chat);
                joinedChat = chat;
                break;
            }
            case faction: {
                this.factionChat = new FactionChat(bb, tab, this.player);
                joinedChat = this.factionChat;
                break;
            }
            case safehouse: {
                this.safehouseChat = new SafehouseChat(bb, tab, this.player);
                joinedChat = this.safehouseChat;
                break;
            }
            case radio: {
                this.radioChat = new RadioChat(bb, tab, this.player);
                this.radioChat.Init();
                joinedChat = this.radioChat;
                break;
            }
            case admin: {
                this.adminChat = new AdminChat(bb, tab, this.player);
                joinedChat = this.adminChat;
                break;
            }
            case server: {
                this.serverChat = new ServerChat(bb, tab, this.player);
                joinedChat = this.serverChat;
                break;
            }
            default: {
                DebugLog.log("Chat of type '" + String.valueOf((Object)type) + "' is not supported to join to");
                return;
            }
        }
        this.mpChats.put(joinedChat.getID(), joinedChat);
        joinedChat.setFontSize(Core.getInstance().getOptionChatFontSize());
        joinedChat.setShowTimestamp(Core.getInstance().isOptionShowChatTimestamp());
        joinedChat.setShowTitle(Core.getInstance().isOptionShowChatTitle());
    }

    public void processLeaveChatPacket(ByteBufferReader bb) {
        Integer id = bb.getInt();
        ChatType type = ChatType.valueOf(bb.getInt());
        switch (type) {
            case general: 
            case say: 
            case shout: 
            case radio: 
            case server: {
                DebugLog.log("Chat type is '" + String.valueOf((Object)type) + "'. Can't leave it. Ignored.");
                break;
            }
            case admin: {
                this.mpChats.remove(id);
                this.removeTab(this.adminChat.getTabID());
                this.adminChat = null;
                DebugLog.log("You leaved admin chat");
                break;
            }
            case faction: {
                this.mpChats.remove(id);
                this.factionChat = null;
                DebugLog.log("You leaved faction chat");
                break;
            }
            case whisper: {
                this.whisperChats.remove(((WhisperChat)this.mpChats.get(id)).getCompanionName());
                this.mpChats.remove(id);
                break;
            }
            case safehouse: {
                this.mpChats.remove(id);
                this.safehouseChat = null;
                DebugLog.log("You leaved safehouse chat");
                break;
            }
            default: {
                DebugLog.log("Chat of type '" + String.valueOf((Object)type) + "' is not supported to leave to");
            }
        }
    }

    public void processPlayerNotFound(String destPlayerName) {
        logger.write("Got player not found packet", "info");
        WhisperChatCreation whisperChatCreation = this.whisperChatCreation.get(destPlayerName);
        if (whisperChatCreation == null) {
            return;
        }
        whisperChatCreation.status = WhisperChat.ChatStatus.PlayerNotFound;
    }

    public ChatMessage unpackMessage(ByteBufferReader bb) {
        int id = bb.getInt();
        ChatBase chat = this.mpChats.get(id);
        return chat.unpackMessage(bb);
    }

    public void processChatMessagePacket(ByteBufferReader bb) {
        ChatMessage msg = this.unpackMessage(bb);
        ChatBase chat = msg.getChat();
        if (ChatUtility.chatStreamEnabled(chat.getType())) {
            chat.showMessage(msg);
            logger.write("Got message from server: " + String.valueOf(msg), "info");
        } else {
            DebugLog.log("Can't process message '" + msg.getText() + "' because '" + String.valueOf((Object)chat.getType()) + "' chat is disabled");
            logger.write("Can't process message '" + msg.getText() + "' because '" + String.valueOf((Object)chat.getType()) + "' chat is disabled", "warning");
        }
    }

    public void updateChatSettings(String fontSize, boolean showTimestamp, boolean showTitle) {
        Core.getInstance().setOptionChatFontSize(fontSize);
        Core.getInstance().setOptionShowChatTimestamp(showTimestamp);
        Core.getInstance().setOptionShowChatTitle(showTitle);
        for (ChatBase chat : this.mpChats.values()) {
            chat.setFontSize(fontSize);
            chat.setShowTimestamp(showTimestamp);
            chat.setShowTitle(showTitle);
        }
    }

    public void showInfoMessage(String msg) {
        ChatMessage chatMessage = this.sayChat.createInfoMessage(msg);
        this.sayChat.showMessage(chatMessage);
    }

    public void showInfoMessage(String author, String msg) {
        if (this.sayChat == null) {
            return;
        }
        ChatMessage chatMessage = this.sayChat.createInfoMessage(msg);
        chatMessage.setAuthor(author);
        this.sayChat.showMessage(chatMessage);
    }

    public void sendMessageToChat(String author, ChatType type, String msg) {
        if ((msg = msg.trim()).isEmpty()) {
            return;
        }
        ChatBase chat = this.getChat(type);
        if (chat == null) {
            if (Core.debug) {
                throw new IllegalArgumentException("Chat '" + String.valueOf((Object)type) + "' is null. Chat should be init before use!");
            }
            this.showChatDisabledMessage(type);
            return;
        }
        ChatMessage chatMessage = chat.createMessage(msg);
        chatMessage.setAuthor(author);
        this.sendMessageToChat(chat, chatMessage);
    }

    public void sendMessageToChat(ChatType type, String msg) {
        this.sendMessageToChat(this.player.getUsername(), type, msg);
    }

    public synchronized void sendWhisperMessage(String destPlayerName, String msg) {
        logger.write("Send message '" + msg + "' for player '" + destPlayerName + "' in whisper chat", "info");
        if (ChatUtility.chatStreamEnabled(ChatType.whisper)) {
            if (destPlayerName == null || destPlayerName.equalsIgnoreCase(this.myNickname)) {
                logger.write("Message can't be send to yourself");
                this.showServerChatMessage(Translator.getText("UI_chat_whisper_message_to_yourself_error"));
                return;
            }
            if (this.whisperChats.containsKey(destPlayerName)) {
                WhisperChat chat = this.whisperChats.get(destPlayerName);
                this.sendMessageToChat(chat, chat.createMessage(msg));
                return;
            }
            if (this.whisperChatCreation.containsKey(destPlayerName)) {
                WhisperChatCreation whisperChatCreation1 = this.whisperChatCreation.get(destPlayerName);
                whisperChatCreation1.messages.add(msg);
                return;
            }
            WhisperChatCreation whisperChatCreation1 = this.createWhisperChat(destPlayerName);
            whisperChatCreation1.messages.add(msg);
        } else {
            logger.write("Whisper chat is disabled", "info");
            this.showChatDisabledMessage(ChatType.whisper);
        }
    }

    public Boolean isPlayerCanUseChat(ChatType chat) {
        if (!ChatUtility.chatStreamEnabled(chat)) {
            return false;
        }
        switch (chat) {
            case radio: {
                return this.isPlayerCanUseRadioChat();
            }
            case admin: {
                return this.player.isAccessLevel("admin");
            }
            case faction: {
                return Faction.isAlreadyInFaction(this.player);
            }
            case safehouse: {
                return SafeHouse.hasSafehouse(this.player) != null;
            }
        }
        return true;
    }

    public void focusOnTab(Short id) {
        for (ChatTab tab : this.tabs.values()) {
            if (tab.getID() != id.shortValue()) continue;
            this.focusTab = tab;
            return;
        }
        throw new RuntimeException("Tab with id = '" + id + "' not found");
    }

    public String getTabName(short tabID) {
        if (this.tabs.containsKey(tabID)) {
            return this.tabs.get(tabID).getTitle();
        }
        return Short.toString(tabID);
    }

    public ChatTab getFocusTab() {
        return this.focusTab;
    }

    public void showRadioMessage(ChatMessage msg) {
        this.radioChat.showMessage(msg);
    }

    public void showRadioMessage(String text, int channel) {
        ChatMessage chatMessage = this.radioChat.createMessage(text);
        if (channel != 0) {
            chatMessage.setRadioChannel(channel);
        }
        this.radioChat.showMessage(chatMessage);
    }

    public void showStaticRadioSound(String text) {
        this.radioChat.showMessage(this.radioChat.createStaticSoundMessage(text));
    }

    public ChatMessage createRadiostationMessage(String text, int channel) {
        return this.radioChat.createBroadcastingMessage(text, channel);
    }

    public void showServerChatMessage(String msg) {
        ServerChatMessage chatMessage = this.serverChat.createServerMessage(msg);
        this.serverChat.showMessage(chatMessage);
    }

    private void addMessage(int chatID, String msgAuthor, String msg) {
        ChatBase chat = this.mpChats.get(chatID);
        chat.showMessage(msg, msgAuthor);
    }

    public void addMessage(String msgAuthor, String msg) throws RuntimeException {
        if (this.generalChat == null) {
            throw new RuntimeException();
        }
        this.addMessage(this.generalChat.getID(), msgAuthor, msg);
    }

    private void sendMessageToChat(ChatBase chat, ChatMessage chatMessage) {
        if (chat.getType() == ChatType.radio) {
            if (Core.debug) {
                throw new IllegalArgumentException("You can't send message to radio directly. Use radio and send say message");
            }
            DebugLog.log("You try to use radio chat directly. It's restricted. Try to use say chat");
            return;
        }
        chat.showMessage(chatMessage);
        if (chat.isEnabled()) {
            if (!this.isSinglePlayerMode() && !chatMessage.isLocal()) {
                DeviceData deviceData = this.getTransmittingRadio();
                chat.sendToServer(chatMessage, deviceData);
                if (deviceData != null && chat.isSendingToRadio()) {
                    ChatMessage radioMessage = this.radioChat.createMessage(chatMessage.getText());
                    radioMessage.setRadioChannel(deviceData.getChannel());
                    this.radioChat.sendToServer(radioMessage, deviceData);
                }
            }
        } else {
            this.showChatDisabledMessage(chat.getType());
        }
    }

    private ChatBase getChat(ChatType type) {
        if (type == ChatType.whisper) {
            throw new IllegalArgumentException("Whisper not unique chat");
        }
        switch (type) {
            case admin: {
                return this.adminChat;
            }
            case radio: {
                return this.radioChat;
            }
            case general: {
                return this.generalChat;
            }
            case say: {
                return this.sayChat;
            }
            case shout: {
                return this.shoutChat;
            }
            case faction: {
                return this.factionChat;
            }
            case safehouse: {
                return this.safehouseChat;
            }
            case server: {
                return this.serverChat;
            }
        }
        throw new IllegalArgumentException("Chat type is undefined");
    }

    private void addTab(short id) {
        ChatTab newTab = this.tabs.get(id);
        if (newTab.isEnabled()) {
            return;
        }
        newTab.setEnabled(true);
        LuaEventManager.triggerEvent("OnTabAdded", newTab.getTitle(), newTab.getID());
    }

    private void removeTab(Short id) {
        ChatTab tab = this.tabs.get(id);
        if (!tab.isEnabled()) {
            return;
        }
        LuaEventManager.triggerEvent("OnTabRemoved", tab.getTitle(), tab.getID());
        tab.setEnabled(false);
    }

    private WhisperChatCreation createWhisperChat(String destPlayerName) {
        logger.write("Whisper chat is not created for '" + destPlayerName + "'", "info");
        WhisperChatCreation whisperChatCreation1 = new WhisperChatCreation();
        whisperChatCreation1.destPlayerName = destPlayerName;
        whisperChatCreation1.status = WhisperChat.ChatStatus.Creating;
        whisperChatCreation1.createTime = System.currentTimeMillis();
        this.whisperChatCreation.put(destPlayerName, whisperChatCreation1);
        ByteBufferWriter b = this.serverConnection.startPacket();
        PacketTypes.PacketType.PlayerStartPMChat.doPacket(b);
        b.putUTF(this.myNickname);
        b.putUTF(destPlayerName);
        PacketTypes.PacketType.PlayerStartPMChat.send(this.serverConnection);
        logger.write("'Start PM chat' package sent. Waiting for a creating whisper chat by server...", "info");
        return whisperChatCreation1;
    }

    public static void UpdateClient() {
        if (instance == null) {
            return;
        }
        try {
            instance.updateClient();
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
    }

    private void updateClient() {
        if (!this.isWorking()) {
            return;
        }
        this.updateWhisperChat();
    }

    private void updateWhisperChat() {
        if (this.whisperChatCreation.isEmpty()) {
            return;
        }
        long time = System.currentTimeMillis();
        ArrayList<WhisperChatCreation> whisperChatCreationList = new ArrayList<WhisperChatCreation>(this.whisperChatCreation.values());
        for (WhisperChatCreation whisperChatCreation1 : whisperChatCreationList) {
            if (this.whisperChats.containsKey(whisperChatCreation1.destPlayerName)) {
                WhisperChat chat = this.whisperChats.get(whisperChatCreation1.destPlayerName);
                logger.write("Whisper chat created between '" + this.myNickname + "' and '" + whisperChatCreation1.destPlayerName + "' and has id = " + chat.getID(), "info");
                this.whisperChatCreation.remove(whisperChatCreation1.destPlayerName);
                for (String message : whisperChatCreation1.messages) {
                    this.sendMessageToChat(chat, chat.createMessage(message));
                }
                continue;
            }
            if (whisperChatCreation1.status == WhisperChat.ChatStatus.PlayerNotFound) {
                logger.write("Player '" + whisperChatCreation1.destPlayerName + "' is not found. Chat is not created", "info");
                this.whisperChatCreation.remove(whisperChatCreation1.destPlayerName);
                this.showServerChatMessage(Translator.getText("UI_chat_whisper_player_not_found_error", whisperChatCreation1.destPlayerName));
                continue;
            }
            if (whisperChatCreation1.status != WhisperChat.ChatStatus.Creating || time - whisperChatCreation1.createTime < 10000L) continue;
            logger.write("Whisper chat is not created by timeout. See server chat logs", "error");
            this.whisperChatCreation.remove(whisperChatCreation1.destPlayerName);
        }
    }

    private void showChatDisabledMessage(ChatType expectedChat) {
        StringBuilder text = new StringBuilder();
        text.append(Translator.getText("UI_chat_chat_disabled_msg", Translator.getText(expectedChat.getTitleID())));
        ArrayList<ChatType> allowedChats = ChatUtility.getAllowedChatStreams();
        for (ChatType chat : allowedChats) {
            if (!this.isPlayerCanUseChat(chat).booleanValue()) continue;
            text.append("    * ").append(Translator.getText(chat.getTitleID())).append(" <LINE> ");
        }
        this.showServerChatMessage(text.toString());
    }

    private boolean isPlayerCanUseRadioChat() {
        Radio radio = this.player.getEquipedRadio();
        if (radio != null && radio.getDeviceData() != null) {
            boolean showRadioMessage = radio.getDeviceData().getIsTurnedOn();
            showRadioMessage &= radio.getDeviceData().getIsTwoWay();
            showRadioMessage &= radio.getDeviceData().getIsPortable();
            return showRadioMessage &= !radio.getDeviceData().getMicIsMuted();
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private DeviceData getTransmittingRadio() {
        if (this.player.getOnlineID() == -1) {
            return null;
        }
        VoiceManagerData myRadioData = VoiceManagerData.get(this.player.getOnlineID());
        ArrayList<VoiceManagerData.RadioData> arrayList = myRadioData.radioData;
        synchronized (arrayList) {
            return myRadioData.radioData.stream().filter(VoiceManagerData.RadioData::isTransmissionAvailable).findFirst().map(VoiceManagerData.RadioData::getDeviceData).orElse(null);
        }
    }

    private static enum Stage {
        notStarted,
        starting,
        working;

    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.DiscordSender;
import zombie.network.GameServer;
import zombie.network.server.IEventController;
import zombie.util.StringUtils;

public class DiscordBot
implements IEventController {
    private final DiscordSender sender;
    private final String serverName;
    private TextChannel chatChannel;
    private TextChannel logChannel;
    private TextChannel commandChannel;

    @Override
    public void process(String event) {
        this.logEvent(event);
    }

    public void logEvent(String event) {
        if (this.logChannel != null) {
            this.logChannel.sendMessage(event);
        }
    }

    public void sendMessage(String user, String text) {
        if (this.chatChannel != null) {
            this.chatChannel.sendMessage(user + ": " + text);
        }
    }

    public DiscordBot(String serverName, DiscordSender sender) {
        this.serverName = serverName;
        this.sender = sender;
    }

    public void connect(boolean enabled, String token, String chatChannelName, String logChannelName, String commandChannelName) {
        if (enabled && !StringUtils.isNullOrEmpty(token)) {
            try {
                DiscordApi api = new DiscordApiBuilder().setToken(token).setIntents(Intent.GUILD_MESSAGES, Intent.MESSAGE_CONTENT).login().join();
                if (api != null) {
                    if (!StringUtils.isNullOrEmpty(logChannelName)) {
                        this.logChannel = api.getTextChannelsByName(logChannelName).stream().findFirst().orElse(null);
                    } else {
                        DebugType.Discord.debugln("log channel name not configured");
                    }
                    if (!StringUtils.isNullOrEmpty(chatChannelName)) {
                        this.chatChannel = api.getTextChannelsByName(chatChannelName).stream().findFirst().orElse(null);
                    } else {
                        DebugType.Discord.debugln("chat channel name not configured");
                    }
                    if (!StringUtils.isNullOrEmpty(commandChannelName)) {
                        this.commandChannel = api.getTextChannelsByName(commandChannelName).stream().findFirst().orElse(null);
                    } else {
                        DebugType.Discord.debugln("command channel name not configured");
                    }
                    if (this.chatChannel != null || this.commandChannel != null) {
                        api.updateUsername(this.serverName);
                        api.addMessageCreateListener(this::receiveMessage);
                        DebugType.Discord.println("invite-url %s", api.createBotInvite());
                    }
                }
            }
            catch (Exception e) {
                DebugType.General.printException(e, "Can't connect to discord", LogSeverity.Warning);
            }
        }
    }

    private void receiveMessage(MessageCreateEvent messageCreateEvent) {
        String response;
        String content;
        TextChannel messageChannel = messageCreateEvent.getChannel();
        if (messageChannel == null) {
            return;
        }
        Message message = messageCreateEvent.getMessage();
        if (message == null) {
            return;
        }
        if (this.chatChannel != null && this.chatChannel.getId() == messageChannel.getId() && !message.getAuthor().isYourself() && !(content = DiscordBot.removeSmilesAndImages(message.getReadableContent())).isEmpty()) {
            this.sender.sendMessageFromDiscord(message.getAuthor().getDisplayName(), content);
        }
        if (this.commandChannel != null && this.commandChannel.getId() == messageChannel.getId() && !message.getAuthor().isYourself() && (response = GameServer.rcon(message.getReadableContent())) != null) {
            this.commandChannel.sendMessage(response);
        }
    }

    private static String removeSmilesAndImages(String content) {
        StringBuilder sb = new StringBuilder();
        char[] cArray = content.toCharArray();
        int n = cArray.length;
        for (int i = 0; i < n; ++i) {
            Character cur = Character.valueOf(cArray[i]);
            if (Character.isLowSurrogate(cur.charValue()) || Character.isHighSurrogate(cur.charValue())) continue;
            sb.append(cur);
        }
        return sb.toString();
    }
}


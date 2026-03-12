/*
 * Decompiled with CFR 0.152.
 */
package zombie.chat;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.raknet.UdpConnection;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.network.chat.ChatType;

public final class ChatUtility {
    private static final boolean useEuclidean = true;
    private static final HashMap<String, String> allowedChatIcons = new HashMap();
    private static final HashMap<String, String> allowedChatIconsFull = new HashMap();
    private static final StringBuilder builder = new StringBuilder();
    private static final StringBuilder builderTest = new StringBuilder();

    private ChatUtility() {
    }

    public static float getScrambleValue(IsoObject src, IsoPlayer dest, float baseRange) {
        return ChatUtility.getScrambleValue(src.getX(), src.getY(), src.getZ(), src.getSquare(), dest, baseRange);
    }

    public static float getScrambleValue(float srcX, float srcY, float srcZ, IsoGridSquare srcSquare, IsoPlayer dest, float baseRange) {
        float dist;
        float mod = 1.0f;
        boolean forceScramble = false;
        boolean forceDisplay = false;
        if (srcSquare != null && dest.getSquare() != null) {
            if (dest.getBuilding() != null && srcSquare.getBuilding() != null && dest.getBuilding() == srcSquare.getBuilding()) {
                if (dest.getSquare().getRoom() == srcSquare.getRoom()) {
                    mod = (float)((double)mod * 2.0);
                    forceDisplay = true;
                } else if (Math.abs(dest.getZ() - srcZ) < 1.0f) {
                    mod = (float)((double)mod * 2.0);
                }
            } else if (dest.getBuilding() != null || srcSquare.getBuilding() != null) {
                mod = (float)((double)mod * 0.5);
                forceScramble = true;
            }
            if (Math.abs(dest.getZ() - srcZ) >= 1.0f) {
                mod = (float)((double)mod - (double)mod * ((double)Math.abs(dest.getZ() - srcZ) * 0.25));
                forceScramble = true;
            }
        }
        float range = baseRange * mod;
        float scramble = 1.0f;
        if (mod > 0.0f && ChatUtility.playerWithinBounds(srcX, srcY, dest, range) && (dist = ChatUtility.getDistance(srcX, srcY, dest)) >= 0.0f && dist < range) {
            float fullRange = range * 0.6f;
            if (forceDisplay || !forceScramble && dist < fullRange) {
                scramble = 0.0f;
            } else if (range - fullRange != 0.0f && (scramble = (dist - fullRange) / (range - fullRange)) < 0.2f) {
                scramble = 0.2f;
            }
        }
        return scramble;
    }

    public static boolean playerWithinBounds(IsoObject source2, IsoObject dest, float dist) {
        return ChatUtility.playerWithinBounds(source2.getX(), source2.getY(), dest, dist);
    }

    public static boolean playerWithinBounds(float srcX, float srcY, IsoObject dest, float dist) {
        if (dest == null) {
            return false;
        }
        return dest.getX() > srcX - dist && dest.getX() < srcX + dist && dest.getY() > srcY - dist && dest.getY() < srcY + dist;
    }

    public static float getDistance(IsoObject source2, IsoPlayer dest) {
        if (dest == null) {
            return -1.0f;
        }
        return (float)Math.sqrt(Math.pow(source2.getX() - dest.getX(), 2.0) + Math.pow(source2.getY() - dest.getY(), 2.0));
    }

    public static float getDistance(float srcX, float srcY, IsoPlayer dest) {
        if (dest == null) {
            return -1.0f;
        }
        return (float)Math.sqrt(Math.pow(srcX - dest.getX(), 2.0) + Math.pow(srcY - dest.getY(), 2.0));
    }

    public static UdpConnection findConnection(short playerOnlineID) {
        UdpConnection targetConnection = null;
        if (GameServer.udpEngine != null) {
            block0: for (int i = 0; i < GameServer.udpEngine.connections.size(); ++i) {
                UdpConnection connection = GameServer.udpEngine.connections.get(i);
                for (int j = 0; j < connection.playerIds.length; ++j) {
                    if (connection.playerIds[j] != playerOnlineID) continue;
                    targetConnection = connection;
                    continue block0;
                }
            }
        }
        if (targetConnection == null) {
            DebugLog.log("Connection with PlayerID ='" + playerOnlineID + "' not found!");
        }
        return targetConnection;
    }

    public static UdpConnection findConnection(String playerName) {
        UdpConnection founded = null;
        if (GameServer.udpEngine != null) {
            block0: for (int i = 0; i < GameServer.udpEngine.connections.size() && founded == null; ++i) {
                UdpConnection connection = GameServer.udpEngine.connections.get(i);
                for (int j = 0; j < connection.players.length; ++j) {
                    if (connection.players[j] == null || !connection.players[j].username.equalsIgnoreCase(playerName)) continue;
                    founded = connection;
                    continue block0;
                }
            }
        }
        if (founded == null) {
            DebugLog.DetailedInfo.trace("Player with nickname = '" + playerName + "' not found!");
        }
        return founded;
    }

    public static IsoPlayer findPlayer(int playerOnlineID) {
        IsoPlayer foundedPlayer = null;
        if (GameServer.udpEngine != null) {
            block0: for (int i = 0; i < GameServer.udpEngine.connections.size(); ++i) {
                UdpConnection connection = GameServer.udpEngine.connections.get(i);
                for (int j = 0; j < connection.playerIds.length; ++j) {
                    if (connection.playerIds[j] != playerOnlineID) continue;
                    foundedPlayer = connection.players[j];
                    continue block0;
                }
            }
        }
        if (foundedPlayer == null) {
            DebugLog.log("Player with PlayerID ='" + playerOnlineID + "' not found!");
        }
        return foundedPlayer;
    }

    public static String findPlayerName(int playerOnlineID) {
        return ChatUtility.findPlayer(playerOnlineID).getUsername();
    }

    public static IsoPlayer findPlayer(String playerNickname) {
        IsoPlayer foundedPlayer = null;
        if (GameClient.client) {
            foundedPlayer = GameClient.instance.getPlayerFromUsername(playerNickname);
        } else if (GameServer.server) {
            foundedPlayer = GameServer.getPlayerByUserName(playerNickname);
        }
        if (foundedPlayer == null) {
            DebugLog.DetailedInfo.trace("Player with nickname = '" + playerNickname + "' not found!");
        }
        return foundedPlayer;
    }

    public static ArrayList<ChatType> getAllowedChatStreams() {
        String optionValue = ServerOptions.getInstance().chatStreams.getValue();
        optionValue = optionValue.replaceAll("\"", "");
        String[] enabledStreams = optionValue.split(",");
        ArrayList<ChatType> allowedStreams = new ArrayList<ChatType>();
        allowedStreams.add(ChatType.server);
        String[] stringArray = enabledStreams;
        int n = stringArray.length;
        block20: for (int i = 0; i < n; ++i) {
            String streamShortName;
            switch (streamShortName = stringArray[i]) {
                case "s": {
                    allowedStreams.add(ChatType.say);
                    continue block20;
                }
                case "r": {
                    allowedStreams.add(ChatType.radio);
                    continue block20;
                }
                case "a": {
                    allowedStreams.add(ChatType.admin);
                    continue block20;
                }
                case "w": {
                    allowedStreams.add(ChatType.whisper);
                    continue block20;
                }
                case "y": {
                    allowedStreams.add(ChatType.shout);
                    continue block20;
                }
                case "sh": {
                    allowedStreams.add(ChatType.safehouse);
                    continue block20;
                }
                case "f": {
                    allowedStreams.add(ChatType.faction);
                    continue block20;
                }
                case "all": {
                    if (!ServerOptions.getInstance().globalChat.getValue()) continue block20;
                    allowedStreams.add(ChatType.general);
                }
            }
        }
        return allowedStreams;
    }

    public static boolean chatStreamEnabled(ChatType type) {
        ArrayList<ChatType> enabledStreams = ChatUtility.getAllowedChatStreams();
        return enabledStreams.contains((Object)type);
    }

    public static void InitAllowedChatIcons() {
        allowedChatIcons.clear();
        Texture.collectAllIcons(allowedChatIcons, allowedChatIconsFull);
    }

    private static String getColorString(String input, boolean doFloat) {
        String[] split;
        if (Colors.ColorExists(input)) {
            Color c = Colors.GetColorByName(input);
            if (doFloat) {
                return c.getRedFloat() + "," + c.getGreenFloat() + "," + c.getBlueFloat();
            }
            return c.getRed() + "," + c.getGreen() + "," + c.getBlue();
        }
        if (input.length() <= 11 && input.contains(",") && (split = input.split(",")).length == 3) {
            int r = ChatUtility.parseColorInt(split[0]);
            int g = ChatUtility.parseColorInt(split[1]);
            int b = ChatUtility.parseColorInt(split[2]);
            if (r != -1 && g != -1 && b != -1) {
                if (doFloat) {
                    return (float)r / 255.0f + "," + (float)g / 255.0f + "," + (float)b / 255.0f;
                }
                return r + "," + g + "," + b;
            }
        }
        return null;
    }

    private static int parseColorInt(String str) {
        try {
            int i = Integer.parseInt(str);
            if (i >= 0 && i <= 255) {
                return i;
            }
            return -1;
        }
        catch (Exception e) {
            return -1;
        }
    }

    public static String parseStringForChatBubble(String str) {
        try {
            builder.delete(0, builder.length());
            builderTest.delete(0, builderTest.length());
            str = str.replaceAll("\\[br/]", "");
            str = str.replaceAll("\\[cdt=", "");
            char[] chars = str.toCharArray();
            boolean hasOpened = false;
            boolean hasOpenedColor = false;
            int maxImages = 10;
            int imgCount = 0;
            for (int i = 0; i < chars.length; ++i) {
                char c = chars[i];
                if (c == '*') {
                    if (!hasOpened) {
                        hasOpened = true;
                        continue;
                    }
                    String s = builderTest.toString();
                    builderTest.delete(0, builderTest.length());
                    String colorStr = ChatUtility.getColorString(s, false);
                    if (colorStr != null) {
                        if (hasOpenedColor) {
                            builder.append("[/]");
                        }
                        builder.append("[col=");
                        builder.append(colorStr);
                        builder.append(']');
                        hasOpened = false;
                        hasOpenedColor = true;
                        continue;
                    }
                    if (imgCount < 10 && (s.equalsIgnoreCase("music") || allowedChatIcons.containsKey(s.toLowerCase()))) {
                        if (hasOpenedColor) {
                            builder.append("[/]");
                            hasOpenedColor = false;
                        }
                        builder.append("[img=");
                        builder.append(s.equalsIgnoreCase("music") ? "music" : allowedChatIcons.get(s.toLowerCase()));
                        builder.append(']');
                        hasOpened = false;
                        ++imgCount;
                        continue;
                    }
                    builder.append('*');
                    builder.append(s);
                    continue;
                }
                if (hasOpened) {
                    builderTest.append(c);
                    continue;
                }
                builder.append(c);
            }
            if (hasOpened) {
                builder.append('*');
                String s = builderTest.toString();
                if (!s.isEmpty()) {
                    builder.append(s);
                }
                if (hasOpenedColor) {
                    builder.append("[/]");
                }
            }
            return builder.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            return str;
        }
    }

    public static String parseStringForChatLog(String str) {
        try {
            builder.delete(0, builder.length());
            builderTest.delete(0, builderTest.length());
            char[] chars = str.toCharArray();
            boolean hasOpened = false;
            boolean hasOpenedColor = false;
            int maxImages = 10;
            int imgCount = 0;
            for (int i = 0; i < chars.length; ++i) {
                char c = chars[i];
                if (c == '*') {
                    if (!hasOpened) {
                        hasOpened = true;
                        continue;
                    }
                    String s = builderTest.toString();
                    builderTest.delete(0, builderTest.length());
                    String colorStr = ChatUtility.getColorString(s, true);
                    if (colorStr != null) {
                        builder.append(" <RGB:");
                        builder.append(colorStr);
                        builder.append('>');
                        hasOpened = false;
                        hasOpenedColor = true;
                        continue;
                    }
                    if (imgCount < 10 && (s.equalsIgnoreCase("music") || allowedChatIconsFull.containsKey(s.toLowerCase()))) {
                        if (hasOpenedColor) {
                            builder.append(" <RGB:");
                            builder.append("1.0,1.0,1.0");
                            builder.append('>');
                            hasOpenedColor = false;
                        }
                        String texid = s.equalsIgnoreCase("music") ? "Icon_music_notes" : allowedChatIconsFull.get(s.toLowerCase());
                        Texture tex = Texture.getSharedTexture(texid);
                        if (Texture.getSharedTexture(texid) != null) {
                            int w = (int)((float)tex.getWidth() * 0.5f);
                            int h = (int)((float)tex.getHeight() * 0.5f);
                            if (s.equalsIgnoreCase("music")) {
                                w = (int)((float)tex.getWidth() * 0.75f);
                                h = (int)((float)tex.getHeight() * 0.75f);
                            }
                            builder.append("<IMAGE:");
                            builder.append(texid);
                            builder.append("," + w + "," + h + ">");
                            hasOpened = false;
                            ++imgCount;
                            continue;
                        }
                    }
                    builder.append('*');
                    builder.append(s);
                    continue;
                }
                if (hasOpened) {
                    builderTest.append(c);
                    continue;
                }
                builder.append(c);
            }
            if (hasOpened) {
                builder.append('*');
                String s = builderTest.toString();
                if (!s.isEmpty()) {
                    builder.append(s);
                }
            }
            return builder.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            return str;
        }
    }
}


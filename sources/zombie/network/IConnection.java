/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.Vector3;
import zombie.network.ClientServerMap;
import zombie.network.PacketTypes;
import zombie.network.PlayerDownloadServer;
import zombie.network.anticheats.PacketValidator;
import zombie.network.packets.INetworkPacket;

public interface IConnection {
    public INetworkPacket getPacket(PacketTypes.PacketType var1);

    public IsoPlayer getPlayerAt(int var1);

    public void setPlayerAt(int var1, IsoPlayer var2);

    public ByteBufferWriter startPacket();

    public void endPacket(int var1, int var2, byte var3);

    public void cancelPacket();

    public String getUserName();

    public boolean hasPlayer(String var1);

    public Role getRole();

    public void setRole(Role var1);

    public boolean isRelevantTo(float var1, float var2);

    public long getConnectedGUID();

    public String getIDStr();

    public boolean wasInLoadingQueue();

    public PlayerDownloadServer getPlayerDownloadServer();

    public long getLastConnection();

    public int getMTUSize();

    public void setConnectionTimestamp(long var1);

    public PacketValidator getValidator();

    public int getChunkGridWidth();

    public void setChunkGridWidth(int var1);

    public String getUserName(int var1);

    public void setUserName(int var1, String var2);

    public Vector3 getRelevantPos(int var1);

    public void setRelevantPos(int var1, Vector3 var2);

    public void setConnectArea(int var1, Vector3 var2);

    public short getPlayerId(int var1);

    public void setPlayerId(int var1, short var2);

    public void setRelevantRange(byte var1);

    public void forceDisconnect(String var1);

    public void setFullyConnected();

    public long getSteamId();

    public String getIP();

    public void setLoadedCells(int var1, ClientServerMap var2);

    public ClientServerMap getLoadedCell(int var1);
}


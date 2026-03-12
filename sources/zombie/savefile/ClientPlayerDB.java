/*
 * Decompiled with CFR 0.152.
 */
package zombie.savefile;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public final class ClientPlayerDB {
    private static ClientPlayerDB instance;
    private static boolean allow;
    public NetworkCharacterProfile networkProfile;

    public static void setAllow(boolean en) {
        allow = en;
    }

    public static boolean isAllow() {
        return allow;
    }

    public static synchronized ClientPlayerDB getInstance() {
        if (instance == null && allow) {
            instance = new ClientPlayerDB();
        }
        return instance;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public void close() {
        instance = null;
        allow = false;
    }

    public ArrayList<IsoPlayer> getAllNetworkPlayers() {
        ArrayList<IsoPlayer> players = new ArrayList<IsoPlayer>();
        for (int i = 1; i < this.networkProfile.playerCount; ++i) {
            byte[] buf = this.getClientLoadNetworkPlayerData(i + 1);
            if (buf == null) continue;
            ByteBuffer bufferForLoadPlayer = ByteBuffer.allocate(buf.length);
            bufferForLoadPlayer.rewind();
            bufferForLoadPlayer.put(buf);
            bufferForLoadPlayer.rewind();
            try {
                IsoPlayer player = new IsoPlayer(IsoWorld.instance.currentCell);
                player.serverPlayerIndex = i + 1;
                player.load(bufferForLoadPlayer, this.networkProfile.worldVersion[i]);
                if (this.networkProfile.isDead[i]) {
                    player.getBodyDamage().setOverallBodyHealth(0.0f);
                    player.setHealth(0.0f);
                }
                players.add(player);
                continue;
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
            }
        }
        return players;
    }

    private boolean isClientLoadNetworkCharacterCompleted() {
        return this.networkProfile != null && this.networkProfile.isLoaded;
    }

    public boolean isAliveMainNetworkPlayer() {
        return !this.networkProfile.isDead[0];
    }

    public boolean clientLoadNetworkPlayer() {
        if (this.networkProfile != null && this.networkProfile.isLoaded && this.networkProfile.username.equals(GameClient.username) && this.networkProfile.server.equals(GameClient.ip)) {
            return this.networkProfile.playerCount > 0;
        }
        if (GameClient.connection == null) {
            return false;
        }
        if (this.networkProfile != null) {
            this.networkProfile = null;
        }
        INetworkPacket.send(PacketTypes.PacketType.LoadPlayerProfile, new Object[0]);
        int timeout2 = 200;
        while (timeout2-- > 0) {
            if (this.isClientLoadNetworkCharacterCompleted()) {
                return this.networkProfile.playerCount > 0;
            }
            try {
                Thread.sleep(50L);
            }
            catch (InterruptedException e) {
                ExceptionLogger.logException(e);
            }
        }
        return false;
    }

    public byte[] getClientLoadNetworkPlayerData(int playerIndex) {
        if (this.networkProfile != null && this.networkProfile.isLoaded && this.networkProfile.username.equals(GameClient.username) && this.networkProfile.server.equals(GameClient.ip)) {
            switch (playerIndex) {
                case 1: 
                case 2: 
                case 3: 
                case 4: {
                    return this.networkProfile.character[playerIndex - 1];
                }
            }
            return null;
        }
        if (!this.clientLoadNetworkPlayer()) {
            return null;
        }
        switch (playerIndex) {
            case 1: 
            case 2: 
            case 3: 
            case 4: {
                return this.networkProfile.character[playerIndex - 1];
            }
        }
        return null;
    }

    public boolean loadNetworkPlayer() {
        try {
            byte[] buffer = this.getClientLoadNetworkPlayerData(1);
            if (buffer != null) {
                ByteBuffer bufferForLoadPlayer = ByteBuffer.allocate(buffer.length);
                bufferForLoadPlayer.rewind();
                bufferForLoadPlayer.put(buffer);
                bufferForLoadPlayer.rewind();
                if (IsoPlayer.getInstance() == null) {
                    IsoPlayer.setInstance(new IsoPlayer(IsoCell.getInstance()));
                    IsoPlayer.players[0] = IsoPlayer.getInstance();
                }
                IsoPlayer.getInstance().serverPlayerIndex = 1;
                IsoPlayer.getInstance().load(bufferForLoadPlayer, this.networkProfile.worldVersion[0]);
                return true;
            }
        }
        catch (Exception e1) {
            ExceptionLogger.logException(e1);
        }
        return false;
    }

    public boolean loadNetworkPlayerInfo(int playerIndex) {
        if (this.networkProfile != null && this.networkProfile.isLoaded && this.networkProfile.username.equals(GameClient.username) && this.networkProfile.server.equals(GameClient.ip) && playerIndex >= 1 && playerIndex <= 4 && playerIndex <= this.networkProfile.playerCount) {
            int worldX = PZMath.fastfloor(this.networkProfile.x[playerIndex - 1] / 8.0f) + IsoWorld.saveoffsetx * 30;
            int worldY = PZMath.fastfloor(this.networkProfile.y[playerIndex - 1] / 8.0f) + IsoWorld.saveoffsety * 30;
            IsoChunkMap.worldXa = PZMath.fastfloor(this.networkProfile.x[playerIndex - 1]);
            IsoChunkMap.worldYa = PZMath.fastfloor(this.networkProfile.y[playerIndex - 1]);
            IsoChunkMap.worldZa = PZMath.fastfloor(this.networkProfile.z[playerIndex - 1]);
            IsoChunkMap.worldXa += 300 * IsoWorld.saveoffsetx;
            IsoChunkMap.worldYa += 300 * IsoWorld.saveoffsety;
            IsoChunkMap.SWorldX[0] = worldX;
            IsoChunkMap.SWorldY[0] = worldY;
            IsoChunkMap.SWorldX[0] = IsoChunkMap.SWorldX[0] + 30 * IsoWorld.saveoffsetx;
            IsoChunkMap.SWorldY[0] = IsoChunkMap.SWorldY[0] + 30 * IsoWorld.saveoffsety;
            return true;
        }
        return false;
    }

    public void forgetPlayer(int serverPlayerIndex) {
        if (this.networkProfile != null && serverPlayerIndex >= 1 && serverPlayerIndex <= 4) {
            this.networkProfile.character[serverPlayerIndex - 1] = null;
            this.networkProfile.isDead[serverPlayerIndex - 1] = true;
        }
    }

    public int getNextServerPlayerIndex() {
        if (this.networkProfile != null && this.networkProfile.isLoaded && this.networkProfile.username.equals(GameClient.username) && this.networkProfile.server.equals(GameClient.ip)) {
            for (int i = 1; i < 4; ++i) {
                if (this.networkProfile.character[i] != null && !this.networkProfile.isDead[i]) continue;
                return i + 1;
            }
        }
        return 2;
    }

    public static final class NetworkCharacterProfile {
        public boolean isLoaded = false;
        public final byte[][] character = new byte[4][];
        public String username;
        public String server;
        public int playerCount = 0;
        public final int[] worldVersion = new int[4];
        public final float[] x = new float[4];
        public final float[] y = new float[4];
        public final float[] z = new float[4];
        public final boolean[] isDead = new boolean[4];
    }
}


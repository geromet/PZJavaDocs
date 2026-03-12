/*
 * Decompiled with CFR 0.152.
 */
package zombie.popman;

import java.util.ArrayList;
import zombie.ai.State;
import zombie.ai.states.ZombieEatBodyState;
import zombie.ai.states.ZombieIdleState;
import zombie.ai.states.ZombieSittingState;
import zombie.ai.states.ZombieTurnAlerted;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.NetworkVariables;
import zombie.network.ServerOptions;
import zombie.network.packets.character.ZombieListPacket;
import zombie.popman.NetworkZombieList;
import zombie.popman.NetworkZombiePacker;
import zombie.util.hash.PZHash;

public class NetworkZombieManager {
    private static final NetworkZombieManager instance = new NetworkZombieManager();
    private final NetworkZombieList owns = new NetworkZombieList();
    private static final float NospottedDistanceSquared = 16.0f;

    public static NetworkZombieManager getInstance() {
        return instance;
    }

    public int getAuthorizedZombieCount(UdpConnection con) {
        return (int)IsoWorld.instance.currentCell.getZombieList().stream().filter(z -> z.getOwner() == con).count();
    }

    public int getUnauthorizedZombieCount() {
        return (int)IsoWorld.instance.currentCell.getZombieList().stream().filter(z -> z.getOwner() == null).count();
    }

    public static boolean canSpotted(IsoZombie zombie) {
        if (zombie.isRemoteZombie()) {
            return false;
        }
        if (zombie.target != null && IsoUtils.DistanceToSquared(zombie.getX(), zombie.getY(), zombie.target.getX(), zombie.target.getY()) < 16.0f) {
            return false;
        }
        State state = zombie.getCurrentState();
        return state == null || state == ZombieIdleState.instance() || state == ZombieEatBodyState.instance() || state == ZombieSittingState.instance() || state == ZombieTurnAlerted.instance();
    }

    public void updateAuth(IsoZombie zombie) {
        UdpConnection c;
        int n;
        float d;
        IsoPlayer isoPlayer;
        Object c2;
        if (!GameServer.server) {
            return;
        }
        if (System.currentTimeMillis() - zombie.lastChangeOwner < 2000L && zombie.getOwner() != null) {
            return;
        }
        if (ServerOptions.getInstance().switchZombiesOwnershipEachUpdate.getValue() && GameServer.getPlayerCount() > 1) {
            if (zombie.getOwner() == null) {
                for (int i = 0; i < GameServer.udpEngine.connections.size(); ++i) {
                    UdpConnection c3 = GameServer.udpEngine.connections.get(i);
                    if (c3 == null) continue;
                    this.moveZombie(zombie, c3, null);
                    break;
                }
            } else {
                int idx = GameServer.udpEngine.connections.indexOf(zombie.getOwner()) + 1;
                for (int i = 0; i < GameServer.udpEngine.connections.size(); ++i) {
                    UdpConnection c4 = GameServer.udpEngine.connections.get((i + idx) % GameServer.udpEngine.connections.size());
                    if (c4 == null) continue;
                    this.moveZombie(zombie, c4, null);
                    break;
                }
            }
            return;
        }
        IGrappleable i = zombie.getWrappedGrappleable().getGrappledBy();
        if (i instanceof IsoPlayer && (c2 = GameServer.getConnectionFromPlayer(isoPlayer = (IsoPlayer)i)) != null && ((UdpConnection)c2).isFullyConnected() && !GameServer.isDelayedDisconnect((UdpConnection)c2)) {
            this.moveZombie(zombie, (UdpConnection)c2, isoPlayer);
            return;
        }
        c2 = zombie.target;
        if (c2 instanceof IsoPlayer && (c2 = GameServer.getConnectionFromPlayer(isoPlayer = (IsoPlayer)c2)) != null && ((UdpConnection)c2).isFullyConnected() && !GameServer.isDelayedDisconnect((UdpConnection)c2) && !Float.isInfinite(d = isoPlayer.getRelevantAndDistance(zombie.getX(), zombie.getY(), ((UdpConnection)c2).getRelevantRange() - 2))) {
            this.moveZombie(zombie, (UdpConnection)c2, isoPlayer);
            return;
        }
        UdpConnection connection = zombie.getOwner();
        IsoPlayer player = zombie.getOwnerPlayer();
        float distance = Float.POSITIVE_INFINITY;
        if (connection != null) {
            distance = connection.getRelevantAndDistance(zombie.getX(), zombie.getY(), zombie.getZ());
        }
        for (n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            c = GameServer.udpEngine.connections.get(n);
            if (c == connection || GameServer.isDelayedDisconnect(c)) continue;
            for (IsoPlayer p : c.players) {
                float d2;
                if (p == null || !p.isAlive() || Float.isInfinite(d2 = p.getRelevantAndDistance(zombie.getX(), zombie.getY(), c.getRelevantRange() - 2)) || connection != null && !(distance > d2 * 1.618034f)) continue;
                connection = c;
                distance = d2;
                player = p;
            }
        }
        if (connection == null && zombie.isReanimatedPlayer()) {
            for (n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
                c = GameServer.udpEngine.connections.get(n);
                if (c == connection || GameServer.isDelayedDisconnect(c)) continue;
                for (IsoPlayer p : c.players) {
                    if (p == null || !p.isDead() || p.reanimatedCorpse != zombie) continue;
                    connection = c;
                    player = p;
                }
            }
        }
        if (connection != null && !connection.RelevantTo(zombie.getX(), zombie.getY(), (connection.getRelevantRange() - 2) * 10)) {
            connection = null;
        }
        this.moveZombie(zombie, connection, player);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void moveZombie(IsoZombie zombie, UdpConnection to, IsoPlayer player) {
        if (zombie.isDead()) {
            if (zombie.getOwner() == null && zombie.getOwnerPlayer() == null) {
                zombie.die();
            } else if (NetworkVariables.ZombieState.OnGround == zombie.realState) {
                Object object = this.owns.lock;
                synchronized (object) {
                    zombie.setOwner(null);
                    zombie.setOwnerPlayer(null);
                    zombie.getNetworkCharacterAI().resetSpeedLimiter();
                }
                NetworkZombiePacker.getInstance().setExtraUpdate();
            }
            return;
        }
        if (player != null && player.getVehicle() != null && player.getVehicle().getSpeed2D() > 2.0f && player.getVehicle().getDriver() != player && player.getVehicle().getDriver() instanceof IsoPlayer) {
            player = (IsoPlayer)player.getVehicle().getDriver();
            to = GameServer.getConnectionFromPlayer(player);
        }
        if (zombie.getOwner() == to) {
            return;
        }
        Object object = this.owns.lock;
        synchronized (object) {
            NetworkZombieList.NetworkZombie nz;
            if (zombie.getOwner() != null && (nz = this.owns.getNetworkZombie(zombie.getOwner())) != null && !nz.zombies.remove(zombie)) {
                DebugLog.log("moveZombie: There are no zombies in nz.zombies.");
            }
            if (to != null) {
                NetworkZombieList.NetworkZombie nz2 = this.owns.getNetworkZombie(to);
                if (nz2 != null) {
                    nz2.zombies.add(zombie);
                    zombie.setOwner(to);
                    zombie.setOwnerPlayer(player);
                    zombie.getNetworkCharacterAI().resetSpeedLimiter();
                    to.timerSendZombie.reset(0L);
                }
            } else {
                zombie.setOwner(null);
                zombie.setOwnerPlayer(null);
                zombie.getNetworkCharacterAI().resetSpeedLimiter();
            }
        }
        zombie.lastChangeOwner = System.currentTimeMillis();
        NetworkZombiePacker.getInstance().setExtraUpdate();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int getZombieAuth(UdpConnection connection, ZombieListPacket packet) {
        int hash = PZHash.fnv_32_init();
        NetworkZombieList.NetworkZombie nz = this.owns.getNetworkZombie(connection);
        packet.zombiesAuth.clear();
        Object object = this.owns.lock;
        synchronized (object) {
            nz.zombies.removeIf(zombie -> zombie.onlineId == -1);
            for (IsoZombie zombie2 : nz.zombies) {
                if (zombie2.onlineId != -1) {
                    packet.zombiesAuth.add(zombie2.onlineId);
                    hash = PZHash.fnv_32_hash(hash, zombie2.onlineId);
                    continue;
                }
                DebugLog.General.error("getZombieAuth: zombie.OnlineID == -1");
            }
        }
        return hash;
    }

    public void clearTargetAuth(IConnection connection, IsoPlayer player) {
        if (Core.debug) {
            DebugLog.log(DebugType.Multiplayer, "Clear zombies target and auth for player id=" + player.getOnlineID());
        }
        if (GameServer.server) {
            for (int i = 0; i < IsoWorld.instance.currentCell.getZombieList().size(); ++i) {
                IsoZombie zombie = IsoWorld.instance.currentCell.getZombieList().get(i);
                if (zombie.target == player) {
                    zombie.setTarget(null);
                }
                if (zombie.getOwner() != connection) continue;
                zombie.setOwner(null);
                zombie.setOwnerPlayer(null);
                zombie.getNetworkCharacterAI().resetSpeedLimiter();
                NetworkZombieManager.getInstance().updateAuth(zombie);
            }
        }
    }

    public static void removeZombies(UdpConnection connection) {
        int radius = (IsoChunkMap.chunkGridWidth + 2) * 8;
        for (IsoPlayer player : connection.players) {
            if (player == null) continue;
            ArrayList<IsoZombie> zl = IsoWorld.instance.currentCell.getZombieList();
            ArrayList<IsoZombie> zombiesForDelete = new ArrayList<IsoZombie>();
            for (IsoZombie zombie : zl) {
                if (!(Math.abs(zombie.getX() - player.getX()) < (float)radius) || !(Math.abs(zombie.getY() - player.getY()) < (float)radius)) continue;
                zombiesForDelete.add(zombie);
            }
            for (IsoZombie zombie : zombiesForDelete) {
                NetworkZombiePacker.getInstance().deleteZombie(zombie);
                zombie.removeFromWorld();
                zombie.removeFromSquare();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void recheck(IConnection connection) {
        Object object = this.owns.lock;
        synchronized (object) {
            NetworkZombieList.NetworkZombie nz = this.owns.getNetworkZombie(connection);
            if (nz != null) {
                nz.zombies.removeIf(zombie -> zombie.getOwner() != connection);
            }
        }
    }
}


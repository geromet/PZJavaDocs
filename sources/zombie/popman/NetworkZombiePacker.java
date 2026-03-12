/*
 * Decompiled with CFR 0.152.
 */
package zombie.popman;

import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import zombie.ai.states.ZombieTurnAlerted;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.NetworkZombieVariables;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.character.ZombieListPacket;
import zombie.network.packets.character.ZombiePacket;
import zombie.network.packets.character.ZombieSynchronizationPacket;
import zombie.popman.NetworkZombieList;
import zombie.popman.NetworkZombieManager;

public class NetworkZombiePacker {
    private static final NetworkZombiePacker instance = new NetworkZombiePacker();
    private final ArrayList<DeletedZombie> zombiesDeleted = new ArrayList();
    private final ArrayList<DeletedZombie> zombiesDeletedForSending = new ArrayList();
    private final HashSet<IsoZombie> zombiesReceived = new HashSet();
    private final ArrayList<IsoZombie> zombiesProcessing = new ArrayList();
    public final NetworkZombieList zombiesRequest = new NetworkZombieList();
    private final ZombiePacket packet = new ZombiePacket();
    private final HashSet<IConnection> extraUpdate = new HashSet();
    UpdateLimit zombieSynchronizationReliableLimit = new UpdateLimit(5000L);

    public static NetworkZombiePacker getInstance() {
        return instance;
    }

    public void setExtraUpdate() {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (!c.isFullyConnected()) continue;
            this.extraUpdate.add(c);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void deleteZombie(IsoZombie z) {
        ArrayList<DeletedZombie> arrayList = this.zombiesDeleted;
        synchronized (arrayList) {
            this.zombiesDeleted.add(new DeletedZombie(this, z.onlineId, z.getX(), z.getY()));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void parseZombie(ByteBufferReader bb, IConnection connection) {
        this.packet.parse(bb, connection);
        if (this.packet.id == -1) {
            DebugLog.General.error("NetworkZombiePacker.parseZombie id=" + this.packet.id);
            return;
        }
        try {
            IsoZombie zombie = ServerMap.instance.zombieMap.get(this.packet.id);
            if (zombie == null) {
                return;
            }
            if (zombie.getOwner() != connection) {
                NetworkZombieManager.getInstance().recheck(connection);
                this.extraUpdate.add(connection);
                return;
            }
            this.applyZombie(zombie);
            zombie.lastRemoteUpdate = 0;
            if (!IsoWorld.instance.currentCell.getZombieList().contains(zombie)) {
                IsoWorld.instance.currentCell.getZombieList().add(zombie);
            }
            if (!IsoWorld.instance.currentCell.getObjectList().contains(zombie)) {
                IsoWorld.instance.currentCell.getObjectList().add(zombie);
            }
            if (zombie.isDead()) {
                zombie.die();
            }
            zombie.zombiePacket.copy(this.packet);
            zombie.zombiePacketUpdated = true;
            HashSet<IsoZombie> hashSet = this.zombiesReceived;
            synchronized (hashSet) {
                this.zombiesReceived.add(zombie);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void postupdate() {
        this.updateAuth();
        Object object = this.zombiesReceived;
        synchronized (object) {
            this.zombiesProcessing.clear();
            this.zombiesProcessing.addAll(this.zombiesReceived);
            this.zombiesReceived.clear();
        }
        object = this.zombiesDeleted;
        synchronized (object) {
            this.zombiesDeletedForSending.clear();
            this.zombiesDeletedForSending.addAll(this.zombiesDeleted);
            this.zombiesDeleted.clear();
        }
        for (UdpConnection connection : GameServer.udpEngine.connections) {
            if (connection == null || !connection.isFullyConnected()) continue;
            ZombieListPacket packet = (ZombieListPacket)connection.getPacket(PacketTypes.PacketType.ZombieList);
            int newHash = NetworkZombieManager.getInstance().getZombieAuth(connection, packet);
            if (connection.getZombieListHash() != newHash) {
                connection.setZombieListHash(newHash);
                ByteBufferWriter b = connection.startPacket();
                PacketTypes.PacketType.ZombieList.doPacket(b);
                packet.write(b);
                PacketTypes.PacketType.ZombieList.send(connection);
            }
            this.send(connection);
        }
    }

    private void updateAuth() {
        ArrayList<IsoZombie> zl = IsoWorld.instance.currentCell.getZombieList();
        for (int i = 0; i < zl.size(); ++i) {
            IsoZombie z = zl.get(i);
            NetworkZombieManager.getInstance().updateAuth(z);
        }
    }

    public int getZombieData(UdpConnection connection, ZombieSynchronizationPacket packet) {
        packet.sendQueue.clear();
        int realCount = 0;
        try {
            NetworkZombieList.NetworkZombie nzr = this.zombiesRequest.getNetworkZombie(connection);
            while (!nzr.zombies.isEmpty()) {
                IsoZombie z = nzr.zombies.poll();
                z.zombiePacket.set(z);
                if (z.onlineId == -1) continue;
                packet.sendQueue.add(z);
                z.zombiePacketUpdated = false;
                if (++realCount < 300) continue;
                break;
            }
            for (int k = 0; k < this.zombiesProcessing.size(); ++k) {
                IsoZombie z = this.zombiesProcessing.get(k);
                if (z.getOwner() == null || z.getOwner() == connection || !connection.RelevantTo(z.getX(), z.getY(), (connection.getRelevantRange() - 2) * 10) || z.onlineId == -1) continue;
                packet.sendQueue.add(z);
                z.zombiePacketUpdated = false;
                ++realCount;
            }
        }
        catch (BufferOverflowException e) {
            e.printStackTrace();
        }
        return realCount;
    }

    public void send(UdpConnection connection) {
        if (!this.zombiesDeletedForSending.isEmpty()) {
            INetworkPacket.send(connection, PacketTypes.PacketType.ZombieDeleteOnClient, connection, this.zombiesDeletedForSending);
        }
        ZombieSynchronizationPacket packet = (ZombieSynchronizationPacket)connection.getPacket(PacketTypes.PacketType.ZombieSynchronizationReliable);
        packet.hasNeighborPlayer = connection.isNeighborPlayer();
        int countData = this.getZombieData(connection, packet);
        if (countData <= 0 && !connection.timerSendZombie.check() && !this.extraUpdate.contains(connection)) {
            return;
        }
        this.extraUpdate.remove(connection);
        connection.timerSendZombie.reset(3800L);
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType packetType = this.zombieSynchronizationReliableLimit.Check() ? PacketTypes.PacketType.ZombieSynchronizationReliable : PacketTypes.PacketType.ZombieSynchronizationUnreliable;
        packetType.doPacket(b);
        packet.write(b);
        packetType.send(connection);
    }

    private void applyZombie(IsoZombie zombie) {
        IsoGridSquare g = IsoWorld.instance.currentCell.getGridSquare(PZMath.fastfloor(this.packet.x), PZMath.fastfloor(this.packet.y), PZMath.fastfloor(this.packet.z));
        zombie.setLastX(zombie.setNextX(zombie.setX(this.packet.realX)));
        zombie.setLastY(zombie.setNextY(zombie.setY(this.packet.realY)));
        zombie.setLastZ(zombie.setZ(this.packet.realZ));
        zombie.setForwardDirection(Vector2.fromLengthDirection(1.0f, this.packet.dirAngle));
        zombie.setCurrent(g);
        if (g != zombie.getMovingSquare()) {
            zombie.setMovingSquareNow();
        }
        zombie.networkAi.targetX = this.packet.x;
        zombie.networkAi.targetY = this.packet.y;
        zombie.networkAi.targetZ = this.packet.z;
        zombie.networkAi.predictionType = this.packet.predictionType;
        zombie.setHealth((float)this.packet.health / 1000.0f);
        zombie.setSpeedMod((float)this.packet.speedMod / 1000.0f);
        if (this.packet.target == -1) {
            zombie.setTargetSeenTime(0.0f);
            zombie.target = null;
        } else {
            IsoPlayer target = null;
            if (GameClient.client) {
                target = GameClient.IDToPlayerMap.get(this.packet.target);
            } else if (GameServer.server) {
                target = GameServer.IDToPlayerMap.get(this.packet.target);
            }
            if (target != zombie.target) {
                zombie.setTargetSeenTime(0.0f);
                zombie.target = target;
            }
        }
        zombie.timeSinceSeenFlesh = this.packet.timeSinceSeenFlesh;
        zombie.set(ZombieTurnAlerted.TARGET_ANGLE, Float.valueOf((float)this.packet.smParamTargetAngle / 1000.0f));
        NetworkZombieVariables.setBooleanVariables(zombie, this.packet.booleanVariables);
        zombie.setWalkType(this.packet.walkType.toString());
        zombie.realState = this.packet.realState;
    }

    public class DeletedZombie {
        public short onlineId;
        public float x;
        public float y;

        public DeletedZombie(NetworkZombiePacker this$0, short onlineId, float x, float y) {
            Objects.requireNonNull(this$0);
            this.onlineId = onlineId;
            this.x = x;
            this.y = y;
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.popman;

import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import zombie.VirtualZombieManager;
import zombie.ai.states.ZombieHitReactionState;
import zombie.ai.states.ZombieOnGroundState;
import zombie.ai.states.ZombieTurnAlerted;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.NetworkZombieVariables;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.character.ZombiePacket;
import zombie.network.packets.character.ZombieSimulationPacket;
import zombie.popman.ZombieCountOptimiser;

public class NetworkZombieSimulator {
    public static final int MAX_ZOMBIES_PER_UPDATE = 300;
    private static final NetworkZombieSimulator instance = new NetworkZombieSimulator();
    private static final ZombiePacket zombiePacket = new ZombiePacket();
    public final ArrayList<Short> unknownZombies = new ArrayList();
    private final HashSet<Short> authoriseZombies = new HashSet();
    private final ArrayDeque<IsoZombie> sendQueue = new ArrayDeque();
    private final ArrayDeque<IsoZombie> extraSendQueue = new ArrayDeque();
    private HashSet<Short> authoriseZombiesCurrent = new HashSet();
    private HashSet<Short> authoriseZombiesLast = new HashSet();
    UpdateLimit zombieSimulationReliableLimit = new UpdateLimit(1000L);

    public static NetworkZombieSimulator getInstance() {
        return instance;
    }

    public int getAuthorizedZombieCount() {
        return (int)IsoWorld.instance.currentCell.getZombieList().stream().filter(z -> z.getOwner() == GameClient.connection).count();
    }

    public int getUnauthorizedZombieCount() {
        return (int)IsoWorld.instance.currentCell.getZombieList().stream().filter(z -> z.getOwner() == null).count();
    }

    public void clear() {
        HashSet<Short> temp = this.authoriseZombiesCurrent;
        this.authoriseZombiesCurrent = this.authoriseZombiesLast;
        this.authoriseZombiesLast = temp;
        this.authoriseZombiesLast.removeIf(zombieId -> GameClient.getZombie(zombieId) == null);
        this.authoriseZombiesCurrent.clear();
    }

    public void addExtraUpdate(IsoZombie zombie) {
        if (!GameClient.client && !GameServer.server) {
            return;
        }
        if (zombie.getOwner() == GameClient.connection && !this.extraSendQueue.contains(zombie)) {
            this.extraSendQueue.add(zombie);
        }
    }

    public void add(short onlineId) {
        this.authoriseZombiesCurrent.add(onlineId);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void added() {
        Sets.SetView<Short> newZombies = Sets.difference(this.authoriseZombiesCurrent, this.authoriseZombiesLast);
        for (Short zombieId : newZombies) {
            IsoZombie z = GameClient.getZombie(zombieId);
            if (z != null && z.onlineId == zombieId) {
                this.becomeLocal(z);
                continue;
            }
            if (this.unknownZombies.contains(zombieId)) continue;
            this.unknownZombies.add(zombieId);
        }
        Sets.SetView<Short> removeZombies = Sets.difference(this.authoriseZombiesLast, this.authoriseZombiesCurrent);
        for (Short zombieId : removeZombies) {
            IsoZombie z = GameClient.getZombie(zombieId);
            if (z == null) continue;
            this.becomeRemote(z);
        }
        HashSet<Short> hashSet = this.authoriseZombies;
        synchronized (hashSet) {
            this.authoriseZombies.clear();
            this.authoriseZombies.addAll(this.authoriseZombiesCurrent);
        }
    }

    public void becomeLocal(IsoZombie z) {
        z.lastRemoteUpdate = 0;
        z.setOwner(GameClient.connection);
        z.setOwnerPlayer(IsoPlayer.getInstance());
        z.allowRepathDelay = 0.0f;
        z.networkAi.mindSync.restorePFBTarget();
    }

    public void becomeRemote(IsoZombie z) {
        if (z.isDead() && z.getOwner() == GameClient.connection) {
            z.getNetworkCharacterAI().setLocal(true);
        }
        z.lastRemoteUpdate = 0;
        z.setOwner(null);
        z.setOwnerPlayer(null);
        if (z.group != null) {
            z.group.remove(z);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isZombieSimulated(Short zombieId) {
        HashSet<Short> hashSet = this.authoriseZombies;
        synchronized (hashSet) {
            return this.authoriseZombies.contains(zombieId);
        }
    }

    public void receivePacket(ByteBufferReader b, IConnection connection) {
        if (!DebugOptions.instance.network.client.updateZombiesFromPacket.getValue()) {
            return;
        }
        short num = b.getShort();
        for (short n = 0; n < num; n = (short)(n + 1)) {
            this.parseZombie(b, connection);
        }
    }

    private void parseZombie(ByteBufferReader b, IConnection connection) {
        ZombiePacket packet = zombiePacket;
        packet.parse(b, connection);
        if (packet.id == -1) {
            DebugType.General.error("NetworkZombieSimulator.parseZombie id=" + packet.id);
            return;
        }
        try {
            IsoZombie zombie = GameClient.IDToZombieMap.get(packet.id);
            if (zombie == null) {
                if (IsoDeadBody.isDead(packet.id)) {
                    DebugType.Death.debugln("Skip dead zombie creation id=%d", packet.id);
                    return;
                }
                IsoGridSquare g = IsoWorld.instance.currentCell.getGridSquare(packet.realX, packet.realY, (double)packet.realZ);
                if (g != null) {
                    VirtualZombieManager.instance.choices.clear();
                    VirtualZombieManager.instance.choices.add(g);
                    zombie = VirtualZombieManager.instance.createRealZombieAlways(packet.outfitId, IsoDirections.getRandom(), false);
                    zombie.getHumanVisual().setSkinTextureIndex(packet.skinTextureIndex);
                    DebugType.ActionSystem.debugln("ParseZombie: CreateRealZombieAlways id=%s", packet.id);
                    if (zombie != null) {
                        zombie.setFakeDead(false);
                        zombie.onlineId = packet.id;
                        GameClient.IDToZombieMap.put(packet.id, zombie);
                        zombie.setLastX(zombie.setNextX(zombie.setX(packet.realX)));
                        zombie.setLastY(zombie.setNextY(zombie.setY(packet.realY)));
                        zombie.setLastZ(zombie.setZ(packet.realZ));
                        zombie.setDir(IsoDirections.fromAngle(packet.dirAngle));
                        zombie.getAnimationPlayer().setTargetAngle(packet.dirAngle);
                        zombie.getAnimationPlayer().setAngleToTarget();
                        zombie.setForwardDirection(Vector2.fromLengthDirection(1.0f, packet.dirAngle));
                        zombie.setCurrent(g);
                        zombie.networkAi.targetX = packet.x;
                        zombie.networkAi.targetY = packet.y;
                        zombie.networkAi.targetZ = packet.z;
                        zombie.networkAi.predictionType = packet.predictionType;
                        zombie.networkAi.reanimatedBodyId.set(packet.reanimatedBodyId);
                        zombie.setHealth((float)packet.health / 1000.0f);
                        zombie.setSpeedMod((float)packet.speedMod / 1000.0f);
                        if (packet.target == -1) {
                            zombie.setTargetSeenTime(0.0f);
                            zombie.target = null;
                        } else {
                            IsoPlayer target = null;
                            if (GameClient.client) {
                                target = GameClient.IDToPlayerMap.get(packet.target);
                            } else if (GameServer.server) {
                                target = GameServer.IDToPlayerMap.get(packet.target);
                            }
                            if (target != zombie.target) {
                                zombie.setTargetSeenTime(0.0f);
                                zombie.target = target;
                            }
                        }
                        zombie.timeSinceSeenFlesh = packet.timeSinceSeenFlesh;
                        zombie.set(ZombieTurnAlerted.TARGET_ANGLE, Float.valueOf((float)packet.smParamTargetAngle / 1000.0f));
                        NetworkZombieVariables.setBooleanVariables(zombie, packet.booleanVariables);
                        if (zombie.isKnockedDown()) {
                            zombie.setOnFloor(true);
                            zombie.changeState(ZombieOnGroundState.instance());
                        }
                        zombie.setWalkType(packet.walkType.toString());
                        zombie.realState = packet.realState;
                        if (zombie.isReanimatedPlayer()) {
                            IsoDeadBody deadBody = (IsoDeadBody)zombie.networkAi.reanimatedBodyId.getObject();
                            if (deadBody != null) {
                                zombie.setDir(deadBody.getDir());
                                zombie.setForwardDirection(deadBody.getDir().ToVector());
                                zombie.setFallOnFront(deadBody.isFallOnFront());
                            }
                            zombie.getStateMachine().changeState(ZombieOnGroundState.instance(), null, false);
                            zombie.neverDoneAlpha = false;
                        }
                        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
                            IsoPlayer player = IsoPlayer.players[playerIndex];
                            if (g.isCanSee(playerIndex)) {
                                zombie.setAlphaAndTarget(playerIndex, 1.0f);
                            }
                            if (player == null || player.reanimatedCorpseId != packet.id || packet.id == -1) continue;
                            player.reanimatedCorpseId = -1;
                            player.reanimatedCorpse = zombie;
                        }
                        zombie.networkAi.mindSync.parse(packet);
                        if (packet.grappledBy.getPlayer() != null) {
                            zombie.Grappled(packet.grappledBy.getPlayer(), packet.grappledBy.getPlayer().bareHands, 1.0f, packet.sharedGrappleType);
                        }
                    } else {
                        DebugType.Zombie.error("Error: VirtualZombieManager can't create zombie");
                    }
                }
                if (zombie == null) {
                    return;
                }
            }
            if (NetworkZombieSimulator.getInstance().isZombieSimulated(zombie.onlineId)) {
                zombie.setOwner(GameClient.connection);
                zombie.setOwnerPlayer(IsoPlayer.getInstance());
                return;
            }
            zombie.setOwner(null);
            zombie.setOwnerPlayer(null);
            if (!zombie.networkAi.isHitByVehicle() || !zombie.isCurrentState(ZombieHitReactionState.instance())) {
                zombie.networkAi.parse(packet);
                zombie.networkAi.mindSync.parse(packet);
            }
            zombie.lastRemoteUpdate = 0;
            if (!IsoWorld.instance.currentCell.getZombieList().contains(zombie)) {
                IsoWorld.instance.currentCell.getZombieList().add(zombie);
            }
            if (!IsoWorld.instance.currentCell.getObjectList().contains(zombie)) {
                IsoWorld.instance.currentCell.getObjectList().add(zombie);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean anyUnknownZombies() {
        return !this.unknownZombies.isEmpty();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void send() {
        ByteBufferWriter b;
        IsoZombie z;
        if (this.authoriseZombies.isEmpty() && this.unknownZombies.isEmpty()) {
            return;
        }
        if (!ZombieCountOptimiser.zombiesForDelete.isEmpty()) {
            INetworkPacket.send(PacketTypes.PacketType.ZombieDelete, new Object[0]);
        }
        if (!NetworkZombieSimulator.getInstance().unknownZombies.isEmpty()) {
            INetworkPacket.send(PacketTypes.PacketType.ZombieRequest, new Object[0]);
        }
        if (this.sendQueue.isEmpty()) {
            HashSet<Short> hashSet = this.authoriseZombies;
            synchronized (hashSet) {
                for (Short zombieId : this.authoriseZombies) {
                    IsoZombie z2 = GameClient.getZombie(zombieId);
                    if (z2 == null || z2.onlineId == -1) continue;
                    this.sendQueue.add(z2);
                }
            }
        }
        ZombieSimulationPacket packet = (ZombieSimulationPacket)GameClient.connection.getPacket(PacketTypes.PacketType.ZombieSimulationReliable);
        packet.sendQueue.clear();
        while (!this.sendQueue.isEmpty()) {
            z = this.sendQueue.poll();
            this.extraSendQueue.remove(z);
            if (z.onlineId == -1) continue;
            packet.sendQueue.add(z);
            z.networkAi.targetX = z.realx = z.getX();
            z.networkAi.targetY = z.realy = z.getY();
            z.realz = (byte)z.getZi();
            z.networkAi.targetZ = z.realz;
            if (packet.sendQueue.size() < 300) continue;
            break;
        }
        if (!packet.sendQueue.isEmpty() || !this.unknownZombies.isEmpty()) {
            b = GameClient.connection.startPacket();
            PacketTypes.PacketType packetType = !this.unknownZombies.isEmpty() && this.zombieSimulationReliableLimit.Check() ? PacketTypes.PacketType.ZombieSimulationReliable : PacketTypes.PacketType.ZombieSimulationUnreliable;
            packetType.doPacket(b);
            packet.write(b);
            packetType.send(GameClient.connection);
        }
        if (!this.extraSendQueue.isEmpty()) {
            packet.sendQueue.clear();
            while (!this.extraSendQueue.isEmpty()) {
                z = this.extraSendQueue.poll();
                if (z.onlineId == -1) continue;
                packet.sendQueue.add(z);
                z.networkAi.targetX = z.realx = z.getX();
                z.networkAi.targetY = z.realy = z.getY();
                z.realz = (byte)PZMath.fastfloor(z.getZ());
                z.networkAi.targetZ = z.realz;
            }
            if (!packet.sendQueue.isEmpty()) {
                b = GameClient.connection.startPacket();
                PacketTypes.PacketType.ZombieSimulationReliable.doPacket(b);
                packet.write(b);
                PacketTypes.PacketType.ZombieSimulationReliable.send(GameClient.connection);
            }
        }
    }

    public void remove(IsoZombie zombie) {
        if (zombie == null || zombie.onlineId == -1) {
            return;
        }
        GameClient.IDToZombieMap.remove(zombie.onlineId);
    }

    public void clearTargetAuth(IsoPlayer player) {
        DebugType.Multiplayer.debugln("Clear zombies target and auth for player id=%s", player.getOnlineID());
        if (GameClient.client) {
            for (IsoZombie zombie : GameClient.IDToZombieMap.valueCollection()) {
                if (zombie.target != player) continue;
                zombie.setTarget(null);
            }
        }
    }
}


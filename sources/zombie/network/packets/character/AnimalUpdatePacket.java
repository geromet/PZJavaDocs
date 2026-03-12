/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.character;

import java.io.IOException;
import java.util.HashSet;
import zombie.characters.Capability;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoDirections;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoHutch;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.character.AnimalPacket;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.popman.animal.AnimalSynchronizationManager;

@PacketSetting(ordering=0, priority=2, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class AnimalUpdatePacket
implements INetworkPacket {
    protected final HashSet<Short> requested = new HashSet();
    protected final HashSet<Short> updated = new HashSet();
    protected final HashSet<Short> deleted = new HashSet();
    protected final HashSet<Short> pending = new HashSet();

    public HashSet<Short> getRequested() {
        return this.requested;
    }

    public HashSet<Short> getUpdated() {
        return this.updated;
    }

    public HashSet<Short> getDeleted() {
        return this.deleted;
    }

    public HashSet<Short> getPending() {
        return this.pending;
    }

    @Override
    public void write(ByteBufferWriter b) {
        IsoAnimal animal;
        b.putInt(this.deleted.size());
        for (short onlineID : this.deleted) {
            b.putShort(onlineID);
        }
        int count = 0;
        int countPosition = b.position();
        b.putInt(this.requested.size());
        int endPosition = b.position();
        try {
            for (short onlineID : this.requested) {
                if (GameServer.server) {
                    animal = AnimalInstanceManager.getInstance().get(onlineID);
                    if (animal == null) continue;
                    b.putShort(onlineID);
                    int sizePos = b.position();
                    int dataSize = 0;
                    b.putInt(dataSize);
                    DebugLog.Animal.noise("Send animal response id=%d type=%s", onlineID, animal.getNetworkCharacterAI().getAnimalPacket().type);
                    animal.getNetworkCharacterAI().getAnimalPacket().flags = (short)6;
                    animal.getNetworkCharacterAI().getAnimalPacket().write(b);
                    int startPosition = b.position();
                    animal.save(b.bb, false, false);
                    endPosition = b.position();
                    dataSize = endPosition - startPosition;
                    b.position(sizePos);
                    b.putInt(dataSize);
                    b.position(endPosition);
                    ++count;
                    continue;
                }
                if (!GameClient.client) continue;
                b.putShort(onlineID);
                DebugLog.Animal.noise("Send animal request id=%d", onlineID);
                ++count;
            }
        }
        catch (IOException e) {
            DebugLog.Animal.printException(e, "Can't save animals", LogSeverity.Error);
            b.position(endPosition);
            count = 0;
        }
        endPosition = b.position();
        b.position(countPosition);
        b.putInt(count);
        b.position(endPosition);
        count = 0;
        countPosition = b.position();
        b.putInt(this.updated.size());
        for (short onlineID : this.updated) {
            animal = AnimalInstanceManager.getInstance().get(onlineID);
            if (animal == null) continue;
            b.putShort(onlineID);
            animal.getNetworkCharacterAI().getAnimalPacket().write(b);
            ++count;
        }
        endPosition = b.position();
        b.position(countPosition);
        b.putInt(count);
        b.position(endPosition);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        AnimalPacket packet;
        short onlineID;
        int i;
        this.deleted.clear();
        int deletedCount = b.getInt();
        for (i = 0; i < deletedCount; ++i) {
            onlineID = b.getShort();
            IsoAnimal animal = AnimalInstanceManager.getInstance().get(onlineID);
            if (animal == null) continue;
            if (animal.getHutch() != null) {
                if (animal.getData().getHutchPosition() != -1) {
                    animal.getHutch().animalInside.remove(animal.getData().getHutchPosition());
                } else if (animal.getNestBoxIndex() != -1) {
                    animal.getHutch().getNestBox((Integer)Integer.valueOf((int)animal.getNestBoxIndex())).animal = null;
                }
            }
            if (animal.networkAi.getAnimalPacket().isDead()) continue;
            animal.remove();
        }
        this.requested.clear();
        int requestedCount = b.getInt();
        for (int i2 = 0; i2 < requestedCount; ++i2) {
            short onlineID2 = b.getShort();
            if (GameClient.client) {
                try {
                    packet = (AnimalPacket)connection.getPacket(PacketTypes.PacketType.AnimalPacket);
                    int dataSize = b.getInt();
                    packet.parse(b, connection);
                    if (!packet.isConsistent(connection)) {
                        b.position(b.position() + dataSize);
                        continue;
                    }
                    IsoAnimal animal = AnimalInstanceManager.getInstance().get(onlineID2);
                    if (animal == null) {
                        switch (packet.location) {
                            case 1: 
                            case 2: 
                            case 3: {
                                animal = new IsoAnimal(IsoWorld.instance.getCell(), 0, 0, 0, packet.type, packet.breed);
                                animal.setX(packet.squareX);
                                animal.setY(packet.squareY);
                                animal.setZ(packet.squareZ);
                                break;
                            }
                            case 0: 
                            case 4: {
                                animal = new IsoAnimal(IsoWorld.instance.getCell(), packet.squareX, packet.squareY, (int)packet.squareZ, packet.type, packet.breed);
                                animal.setDir(IsoDirections.fromAngle(packet.prediction.direction));
                                animal.getForwardDirection().setDirection(packet.prediction.direction);
                                break;
                            }
                            default: {
                                DebugLog.Animal.debugln("BAD animal response creation id=%d", onlineID2);
                            }
                        }
                        AnimalInstanceManager.getInstance().add(animal, onlineID2);
                        animal.setVariable("bPathfind", false);
                    }
                    animal.load(b.bb, IsoWorld.getWorldVersion(), false);
                    switch (packet.location) {
                        case 1: {
                            IsoHutch hutch = IsoHutch.getHutch(PZMath.fastfloor(packet.prediction.x), PZMath.fastfloor(packet.prediction.y), PZMath.fastfloor(packet.prediction.z));
                            if (hutch == null) break;
                            if (packet.hutchPosition != -1) {
                                animal.getData().setPreferredHutchPosition(packet.hutchPosition);
                                hutch.animalInside.put(Integer.valueOf(packet.hutchPosition), null);
                                hutch.addAnimalInside(animal, false);
                                break;
                            }
                            hutch.getNestBox((Integer)Integer.valueOf((int)packet.hutchNestBox)).animal = animal;
                            animal.hutch = hutch;
                            animal.getData().setHutchPosition(-1);
                            animal.nestBox = packet.hutchNestBox;
                            break;
                        }
                        case 3: {
                            animal.removeFromWorld();
                            animal.removeFromSquare();
                            break;
                        }
                        case 2: {
                            if (packet.vehicleId.getVehicle() == null || packet.vehicleId.getVehicle().animals.contains(animal)) break;
                            packet.vehicleId.getVehicle().addAnimalInTrailer(animal);
                            break;
                        }
                        case 0: {
                            animal.addToWorld();
                            break;
                        }
                        default: {
                            DebugLog.Animal.debugln("BAD animal response id=%d", onlineID2);
                        }
                    }
                    animal.networkAi.parse(packet);
                    DebugLog.Animal.noise("Receive animal response id=%d created in %s", onlineID2, animal.hutch == null ? "word" : (animal.nestBox == -1 ? "hutch" : "nest box"));
                }
                catch (IOException e) {
                    DebugLog.Animal.printException(e, String.format("Can't load animal id=%d", onlineID2), LogSeverity.Error);
                }
                continue;
            }
            if (!GameServer.server) continue;
            this.requested.add(onlineID2);
        }
        int updatedCount = b.getInt();
        for (i = 0; i < updatedCount; ++i) {
            onlineID = b.getShort();
            IsoAnimal animal = AnimalInstanceManager.getInstance().get(onlineID);
            if (animal != null) {
                packet = animal.getNetworkCharacterAI().getAnimalPacket();
                packet.parse(b, connection);
                animal.networkAi.parse(packet);
                continue;
            }
            packet = (AnimalPacket)connection.getPacket(PacketTypes.PacketType.AnimalPacket);
            packet.parse(b, connection);
            if (!GameClient.client) continue;
            this.requested.add(onlineID);
        }
        if (GameClient.client) {
            AnimalSynchronizationManager.getInstance().sendRequestToServer(connection);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        AnimalSynchronizationManager.getInstance().setRequested(connection, this.requested);
        AnimalSynchronizationManager.getInstance().setSendToClients(this.updated);
    }

    @Override
    public void processClient(UdpConnection connection) {
        AnimalSynchronizationManager.getInstance().setRequested(connection, this.requested);
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.actions;

import java.util.ArrayList;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketTypes;
import zombie.network.fields.character.AnimalID;
import zombie.network.packets.INetworkPacket;

public class AnimalEventPacket
implements INetworkPacket {
    @JSONField
    private byte eventId;
    @JSONField
    private final AnimalID animalId = new AnimalID();
    @JSONField
    private short alertedId;
    @JSONField
    public float x;
    @JSONField
    public float y;
    @JSONField
    public float z;
    @JSONField
    private String param;
    @JSONField
    private boolean isAlerted;
    private EventType event;

    @Override
    public void setData(Object ... values2) {
        if (values2.length == 3 && values2[0] instanceof IsoAnimal && values2[1] instanceof Boolean) {
            this.eventId = (byte)EventType.EventAlerted.ordinal();
            this.animalId.set((IsoAnimal)values2[0]);
            this.isAlerted = (Boolean)values2[1];
            this.alertedId = values2[2] instanceof IsoPlayer ? ((IsoPlayer)values2[2]).getOnlineID() : (short)-1;
        } else if (values2.length == 2 && values2[0] instanceof IsoAnimal && values2[1] instanceof IsoGridSquare) {
            this.eventId = (byte)EventType.EventEating.ordinal();
            this.animalId.set((IsoAnimal)values2[0]);
            this.x = ((IsoGridSquare)values2[1]).getX();
            this.y = ((IsoGridSquare)values2[1]).getY();
            this.z = ((IsoGridSquare)values2[1]).getZ();
        } else if (values2.length == 4 && values2[0] instanceof IsoAnimal && values2[1] instanceof Float && values2[2] instanceof Float && values2[3] instanceof Float) {
            this.eventId = (byte)EventType.EventClimbFence.ordinal();
            this.animalId.set((IsoAnimal)values2[0]);
            this.x = ((Float)values2[1]).floatValue();
            this.y = ((Float)values2[2]).floatValue();
            this.z = ((Float)values2[3]).floatValue();
        } else {
            DebugLog.Multiplayer.warn("%s.set get invalid arguments", this.getClass().getSimpleName());
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        boolean result;
        boolean bl = result = this.animalId.isConsistent(connection) && this.event != null;
        if (!result) {
            DebugLog.Multiplayer.debugln("[Event] is not consistent %s", this.getDescription());
        }
        return result;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.eventId = b.getByte();
        this.animalId.parse(b, connection);
        this.alertedId = b.getShort();
        this.x = b.getFloat();
        this.y = b.getFloat();
        this.z = b.getFloat();
        this.param = b.getUTF();
        this.isAlerted = b.getBoolean();
        if (this.eventId >= 0 && this.eventId < EventType.values().length) {
            this.event = EventType.values()[this.eventId];
        } else {
            DebugLog.Multiplayer.warn("Unknown event=" + this.eventId);
            this.event = null;
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.eventId);
        this.animalId.write(b);
        b.putShort(this.alertedId);
        b.putFloat(this.x);
        b.putFloat(this.y);
        b.putFloat(this.z);
        b.putUTF(this.param);
        b.putBoolean(this.isAlerted);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.isConsistent(connection)) {
            IsoGridSquare isoGridSquare;
            if (EventType.EventEating == this.event && (isoGridSquare = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z)) != null) {
                isoGridSquare.removeGrass();
            }
            for (UdpConnection c : GameServer.udpEngine.connections) {
                if (connection.getConnectedGUID() == c.getConnectedGUID() && EventType.EventEating != this.event || !c.isFullyConnected() || !this.animalId.getAnimal().checkCanSeeClient(c) || !c.isRelevantTo(this.animalId.getAnimal().getX(), this.animalId.getAnimal().getY())) continue;
                ByteBufferWriter bbw = c.startPacket();
                packetType.doPacket(bbw);
                this.write(bbw);
                packetType.send(c);
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.process(connection);
    }

    public void process(UdpConnection connection) {
        if (this.isConsistent(connection)) {
            DebugLog.Multiplayer.debugln(this.getDescription());
            switch (this.event.ordinal()) {
                case 0: {
                    this.animalId.getAnimal().setIsAlerted(this.isAlerted);
                    this.animalId.getAnimal().alertedChr = GameClient.IDToPlayerMap.get(this.alertedId);
                    break;
                }
                case 2: {
                    IsoGridSquare isoGridSquare = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
                    if (isoGridSquare == null || isoGridSquare.getFloor() == null || !isoGridSquare.checkHaveGrass()) break;
                    if (isoGridSquare.getFloor().getAttachedAnimSprite() == null) {
                        isoGridSquare.getFloor().setAttachedAnimSprite(new ArrayList<IsoSpriteInstance>());
                    }
                    isoGridSquare.getFloor().getAttachedAnimSprite().add(IsoSpriteInstance.get(IsoSprite.getSprite(IsoSpriteManager.instance, "blends_natural_01_87", 0)));
                    break;
                }
                case 1: {
                    IsoDirections assumedDirection = this.checkCurrentIsEventGridSquareFence(this.animalId.getAnimal());
                    if (assumedDirection == null) break;
                    this.animalId.getAnimal().climbOverFence(assumedDirection);
                }
            }
        }
    }

    private IsoDirections checkCurrentIsEventGridSquareFence(IsoGameCharacter character) {
        IsoGridSquare assumedGridSquare = character.getCell().getGridSquare(this.x, this.y, this.z);
        IsoGridSquare assumedGridSquareNextY = character.getCell().getGridSquare(this.x, this.y + 1.0f, this.z);
        IsoGridSquare assumedGridSquareNextX = character.getCell().getGridSquare(this.x + 1.0f, this.y, this.z);
        IsoDirections result = assumedGridSquare != null && assumedGridSquare.has(IsoFlagType.HoppableN) ? IsoDirections.N : (assumedGridSquare != null && assumedGridSquare.has(IsoFlagType.HoppableW) ? IsoDirections.W : (assumedGridSquareNextY != null && assumedGridSquareNextY.has(IsoFlagType.HoppableN) ? IsoDirections.S : (assumedGridSquareNextX != null && assumedGridSquareNextX.has(IsoFlagType.HoppableW) ? IsoDirections.E : null)));
        return result;
    }

    public static enum EventType {
        EventAlerted,
        EventClimbFence,
        EventEating,
        Unknown;

    }
}


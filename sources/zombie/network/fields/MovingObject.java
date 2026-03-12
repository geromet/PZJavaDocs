/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields;

import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.ServerMap;
import zombie.network.fields.INetworkPacketField;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;

public class MovingObject
implements INetworkPacketField {
    private final byte objectTypeNone = 0;
    private final byte objectTypeIsoObject = 1;
    private final byte objectTypePlayer = (byte)2;
    private final byte objectTypeZombie = (byte)3;
    private final byte objectTypeAnimal = (byte)4;
    private final byte objectTypeVehicle = (byte)5;
    private final byte objectTypeDeadBody = (byte)6;
    private final byte objectTypeMovingObject = (byte)7;
    @JSONField
    private byte objectType;
    @JSONField
    private short objectId;
    @JSONField
    private int squareX;
    @JSONField
    private int squareY;
    @JSONField
    private byte squareZ;
    private boolean isProcessed;
    private IsoObject object;

    public void set(IsoObject value) {
        Object square;
        this.object = value;
        this.isProcessed = true;
        if (this.object == null) {
            this.objectType = 0;
            this.objectId = 0;
            return;
        }
        IsoObject isoObject = this.object;
        if (isoObject instanceof IsoAnimal) {
            IsoAnimal isoAnimal = (IsoAnimal)isoObject;
            this.objectType = (byte)4;
            this.objectId = isoAnimal.getOnlineID();
            return;
        }
        isoObject = this.object;
        if (isoObject instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)isoObject;
            this.objectType = (byte)2;
            this.objectId = isoPlayer.getOnlineID();
            return;
        }
        isoObject = this.object;
        if (isoObject instanceof IsoZombie) {
            IsoZombie isoZombie = (IsoZombie)isoObject;
            this.objectType = (byte)3;
            this.objectId = isoZombie.getOnlineID();
            return;
        }
        isoObject = this.object;
        if (isoObject instanceof BaseVehicle) {
            BaseVehicle baseVehicle = (BaseVehicle)isoObject;
            this.objectType = (byte)5;
            this.objectId = baseVehicle.vehicleId;
            return;
        }
        isoObject = this.object;
        if (isoObject instanceof IsoDeadBody) {
            IsoDeadBody movingObject = (IsoDeadBody)isoObject;
            this.objectType = (byte)6;
            this.objectId = (short)this.object.getStaticMovingObjectIndex();
            square = movingObject.getSquare();
            if (square == null) {
                return;
            }
            this.squareX = ((IsoGridSquare)square).getX();
            this.squareY = ((IsoGridSquare)square).getY();
            this.squareZ = (byte)((IsoGridSquare)square).getZ();
            return;
        }
        square = this.object;
        if (square instanceof IsoMovingObject) {
            IsoMovingObject isoMovingObject = (IsoMovingObject)square;
            this.objectType = (byte)7;
            square = isoMovingObject.getCurrentSquare();
            if (square == null) {
                return;
            }
            this.objectId = (short)((IsoGridSquare)square).getMovingObjects().indexOf(this.object);
            this.squareX = ((IsoGridSquare)square).getX();
            this.squareY = ((IsoGridSquare)square).getY();
            this.squareZ = (byte)((IsoGridSquare)square).getZ();
        }
        this.objectType = 1;
        IsoGridSquare square2 = this.object.getSquare();
        if (square2 == null) {
            return;
        }
        this.objectId = (short)square2.getObjects().indexOf(this.object);
        this.squareX = square2.getX();
        this.squareY = square2.getY();
        this.squareZ = (byte)square2.getZ();
    }

    public IsoObject getObject() {
        if (!this.isProcessed) {
            IsoGridSquare square;
            if (this.objectType == 0) {
                this.object = null;
            }
            if (this.objectType == 4) {
                this.object = AnimalInstanceManager.getInstance().get(this.objectId);
            }
            if (this.objectType == 2) {
                if (GameServer.server) {
                    this.object = GameServer.IDToPlayerMap.get(this.objectId);
                } else if (GameClient.client) {
                    this.object = GameClient.IDToPlayerMap.get(this.objectId);
                }
            }
            if (this.objectType == 3) {
                if (GameServer.server) {
                    this.object = ServerMap.instance.zombieMap.get(this.objectId);
                } else if (GameClient.client) {
                    this.object = GameClient.IDToZombieMap.get(this.objectId);
                }
            }
            if (this.objectType == 5) {
                this.object = VehicleManager.instance.getVehicleByID(this.objectId);
            }
            if (this.objectType == 6) {
                square = IsoWorld.instance.currentCell.getGridSquare(this.squareX, this.squareY, this.squareZ);
                this.object = square == null || this.objectId >= square.getStaticMovingObjects().size() ? null : (IsoObject)square.getStaticMovingObjects().get(this.objectId);
            }
            if (this.objectType == 7) {
                square = IsoWorld.instance.currentCell.getGridSquare(this.squareX, this.squareY, this.squareZ);
                this.object = square == null || this.objectId >= square.getMovingObjects().size() || this.objectId < 0 ? null : (IsoObject)square.getMovingObjects().get(this.objectId);
            }
            if (this.objectType == 1) {
                square = IsoWorld.instance.currentCell.getGridSquare(this.squareX, this.squareY, this.squareZ);
                this.object = square == null || this.objectId >= square.getObjects().size() || this.objectId < 0 ? null : square.getObjects().get(this.objectId);
            }
            this.isProcessed = true;
        }
        return this.object;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.objectType = b.getByte();
        this.objectId = b.getShort();
        if (this.objectType == 7 || this.objectType == 6 || this.objectType == 1) {
            this.squareX = b.getInt();
            this.squareY = b.getInt();
            this.squareZ = b.getByte();
        }
        this.isProcessed = false;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.objectType);
        b.putShort(this.objectId);
        if (this.objectType == 7 || this.objectType == 6 || this.objectType == 1) {
            b.putInt(this.squareX);
            b.putInt(this.squareY);
            b.putByte(this.squareZ);
        }
    }

    @Override
    public int getPacketSizeBytes() {
        if (this.objectType == 7 || this.objectType == 6 || this.objectType == 1) {
            return 12;
        }
        return 3;
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.getObject() != null;
    }
}


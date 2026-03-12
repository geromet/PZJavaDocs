/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.character;

import java.io.IOException;
import java.util.Collection;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.AnimalGene;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoHutch;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.NetObject;
import zombie.network.fields.character.AnimalID;
import zombie.network.fields.character.PlayerID;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.network.packets.INetworkPacket;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.vehicles.BaseVehicle;

@PacketSetting(ordering=1, priority=1, reliability=3, requiredCapability=Capability.AnimalCheats, handlingType=3)
public class AnimalCommandPacket
implements INetworkPacket {
    @JSONField
    public Type type = Type.None;
    @JSONField
    public byte flags;
    @JSONField
    public AnimalID animalId = new AnimalID();
    @JSONField
    public PlayerID playerId = new PlayerID();
    @JSONField
    public VehicleID vehicleId = new VehicleID();
    @JSONField
    public NetObject objectId = new NetObject();
    @JSONField
    protected final ObjectID bodyId = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);
    @JSONField
    public int x;
    @JSONField
    public int y;
    @JSONField
    public int z;
    public InventoryItem item;

    @Override
    public void setData(Object ... values2) {
        if (values2.length == 2) {
            this.set((Type)((Object)values2[0]), (IsoAnimal)values2[1]);
        } else if (values2[1] instanceof IsoDeadBody) {
            this.set((Type)((Object)values2[0]), (IsoDeadBody)values2[1], (InventoryItem)values2[2]);
        } else if (values2[2] instanceof IsoGridSquare) {
            this.set((Type)((Object)values2[0]), (IsoAnimal)values2[1], (IsoGridSquare)values2[2]);
        } else if (values2[3] instanceof InventoryItem) {
            this.set((Type)((Object)values2[0]), (IsoAnimal)values2[1], (IsoPlayer)values2[2], (InventoryItem)values2[3]);
        } else if (values2[3] instanceof IsoHutch && values2.length == 4) {
            this.set((Type)((Object)values2[0]), (IsoAnimal)values2[1], (IsoPlayer)values2[2], (IsoHutch)values2[3]);
        } else if (values2[3] instanceof IsoHutch && values2.length == 5) {
            this.set((Type)((Object)values2[0]), (IsoAnimal)values2[1], (IsoPlayer)values2[2], (IsoObject)((IsoHutch)values2[3]), (InventoryItem)values2[4]);
        } else if (values2[3] instanceof BaseVehicle) {
            this.set((Type)((Object)values2[0]), (IsoAnimal)values2[1], (IsoPlayer)values2[2], (BaseVehicle)values2[3], (InventoryItem)values2[4]);
        } else if (values2[3] instanceof IsoObject && values2[4] instanceof InventoryItem) {
            this.set((Type)((Object)values2[0]), (IsoAnimal)values2[1], (IsoPlayer)values2[2], (IsoObject)values2[3], (InventoryItem)values2[4]);
        } else if (values2[0] == Type.AttachAnimalToPlayer) {
            this.set((Type)((Object)values2[0]), (IsoAnimal)values2[1], (IsoPlayer)values2[2], (IsoObject)values2[3], (Boolean)values2[4]);
        } else if (values2[0] == Type.AttachAnimalToTree) {
            this.set((Type)((Object)values2[0]), (IsoAnimal)values2[1], (IsoObject)values2[2], (Boolean)values2[3]);
        } else {
            DebugLog.Animal.error("No such packet set: %s", values2[0]);
        }
    }

    public void set(Type operation, IsoAnimal animal) {
        this.type = operation;
        this.animalId.set(animal);
    }

    public void set(Type operation, IsoAnimal animal, IsoGridSquare sq) {
        this.type = operation;
        this.animalId.set(animal);
        this.x = sq.getX();
        this.y = sq.getY();
        this.z = sq.getZ();
    }

    public void set(Type operation, IsoAnimal animal, IsoPlayer player, BaseVehicle vehicle, InventoryItem item) {
        this.type = operation;
        this.animalId.set(animal);
        this.playerId.set(player);
        this.vehicleId.set(vehicle);
        this.item = item;
    }

    public void set(Type operation, IsoAnimal animal, IsoPlayer player, IsoObject object, boolean remove) {
        this.type = operation;
        this.animalId.set(animal);
        this.playerId.set(player);
        this.objectId.setObject(object);
        this.flags = (byte)(remove ? 1 : 0);
    }

    public void set(Type operation, IsoAnimal animal, IsoObject object, boolean remove) {
        this.type = operation;
        this.animalId.set(animal);
        this.objectId.setObject(object);
        this.flags = (byte)(remove ? 1 : 0);
    }

    public void set(Type operation, IsoAnimal animal, IsoPlayer player, InventoryItem item) {
        this.type = operation;
        this.animalId.set(animal);
        this.playerId.set(player);
        this.item = item;
    }

    public void set(Type operation, IsoDeadBody body, InventoryItem item) {
        this.type = operation;
        this.bodyId.set(body.getObjectID());
        this.item = item;
    }

    public void set(Type operation, IsoAnimal animal, IsoPlayer player, IsoObject object, InventoryItem item) {
        this.type = operation;
        this.animalId.set(animal);
        this.playerId.set(player);
        this.objectId.setObject(object);
        this.item = item;
    }

    public void set(Type operation, IsoAnimal animal, IsoPlayer player, IsoHutch hutch) {
        this.type = operation;
        this.animalId.set(animal);
        this.playerId.set(player);
        this.objectId.setObject(hutch);
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putEnum(this.type);
        b.putByte(this.flags);
        this.animalId.write(b);
        if (Type.UpdateGenome == this.type) {
            Collection<AnimalGene> genes = this.animalId.getAnimal().getFullGenome().values();
            int sizePos = b.position();
            b.putInt(genes.size());
            try {
                for (AnimalGene gene : genes) {
                    gene.save(b.bb, false);
                }
            }
            catch (IOException e) {
                DebugLog.Multiplayer.printException(e, "Animal genome write failed", LogSeverity.Error);
                b.position(sizePos);
                b.putInt(0);
            }
        } else {
            this.playerId.write(b);
            this.vehicleId.write(b);
            this.objectId.write(b);
            this.bodyId.write(b);
            b.putInt(this.x);
            b.putInt(this.y);
            b.putInt(this.z);
            if (this.item != null) {
                b.putInt(this.item.getID());
                try {
                    this.item.saveWithSize(b.bb, false);
                }
                catch (Exception e) {
                    DebugLog.Multiplayer.printException(e, "AnimalInventoryItem save failed", LogSeverity.Error);
                }
            } else {
                b.putInt(-1);
            }
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.type = b.getEnum(Type.class);
        this.flags = b.getByte();
        this.animalId.parse(b, connection);
        if (Type.UpdateGenome == this.type) {
            int size = b.getInt();
            try {
                for (int i = 0; i < size; ++i) {
                    AnimalGene gene = new AnimalGene();
                    gene.load(b.bb, IsoWorld.getWorldVersion(), false);
                    this.animalId.getAnimal().getFullGenome().put(gene.name, gene);
                }
            }
            catch (IOException e) {
                DebugLog.Multiplayer.printException(e, "Animal genome read failed", LogSeverity.Error);
                return;
            }
        }
        this.playerId.parse(b, connection);
        this.vehicleId.parse(b, connection);
        this.objectId.parse(b, connection);
        this.bodyId.parse(b, connection);
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.getInt();
        int itemID = b.getInt();
        if (itemID != -1) {
            try {
                this.item = zombie.util.Type.tryCastTo(InventoryItem.loadItem(b.bb, IsoWorld.getWorldVersion()), InventoryItem.class);
            }
            catch (Exception e) {
                DebugLog.Multiplayer.printException(e, "AnimalInventoryItem load failed", LogSeverity.Error);
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.isConsistent(connection) && Type.UpdateGenome == this.type) {
            for (UdpConnection c : GameServer.udpEngine.connections) {
                if (connection.getConnectedGUID() == c.getConnectedGUID() || !c.isFullyConnected() || !c.isRelevantTo(this.animalId.getAnimal().getX(), this.animalId.getAnimal().getY())) continue;
                ByteBufferWriter bbw = c.startPacket();
                packetType.doPacket(bbw);
                this.write(bbw);
                packetType.send(c);
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.isConsistent(connection)) {
            switch (this.type.ordinal()) {
                case 1: {
                    if (!this.animalId.isConsistent(connection) || !this.playerId.isConsistent(connection) || !this.vehicleId.isConsistent(connection)) break;
                    this.vehicleId.getVehicle().animals.add(this.animalId.getAnimal());
                    this.animalId.getAnimal().setVehicle(this.vehicleId.getVehicle());
                    this.vehicleId.getVehicle().setCurrentTotalAnimalSize(this.vehicleId.getVehicle().getCurrentTotalAnimalSize() + this.animalId.getAnimal().getAnimalTrailerSize());
                    AnimalInventoryItem item = this.playerId.getPlayer().getInventory().getAnimalInventoryItem(this.animalId.getAnimal());
                    this.animalId.getAnimal().itemId = 0;
                    if (item != null) {
                        this.playerId.getPlayer().getInventory().Remove(item);
                        break;
                    }
                    DebugLog.Animal.error("Animal not found");
                    break;
                }
                case 2: {
                    if (!this.animalId.isConsistent(connection) || !this.vehicleId.isConsistent(connection) || this.vehicleId.getVehicle().animals.contains(this.animalId.getAnimal())) break;
                    this.vehicleId.getVehicle().animals.add(this.animalId.getAnimal());
                    if (this.animalId.getAnimal().mother != null) {
                        this.animalId.getAnimal().attachBackToMother = this.animalId.getAnimal().mother.animalId;
                    }
                    this.animalId.getAnimal().setVehicle(this.vehicleId.getVehicle());
                    if (this.animalId.getAnimal().getData().getAttachedPlayer() != null) {
                        this.animalId.getAnimal().getData().getAttachedPlayer().removeAttachedAnimal(this.animalId.getAnimal());
                        this.animalId.getAnimal().getData().setAttachedPlayer(null);
                    }
                    this.animalId.getAnimal().removeFromWorld();
                    this.animalId.getAnimal().removeFromSquare();
                    this.vehicleId.getVehicle().setCurrentTotalAnimalSize(this.vehicleId.getVehicle().getCurrentTotalAnimalSize() + this.animalId.getAnimal().getAnimalTrailerSize());
                    break;
                }
                case 3: {
                    if (!this.animalId.isConsistent(connection) || !this.playerId.isConsistent(connection) || !this.vehicleId.isConsistent(connection) || !this.vehicleId.getVehicle().animals.remove(this.animalId.getAnimal())) break;
                    Vector2 vec = this.vehicleId.getVehicle().getAreaCenter("AnimalEntry");
                    IsoAnimal newAnimal = new IsoAnimal(this.vehicleId.getVehicle().getSquare().getCell(), PZMath.fastfloor(vec.x), PZMath.fastfloor(vec.y), this.vehicleId.getVehicle().getSquare().z, this.animalId.getAnimal().getAnimalType(), this.animalId.getAnimal().getBreed());
                    newAnimal.copyFrom(this.animalId.getAnimal());
                    newAnimal.attachBackToMotherTimer = 10000.0f;
                    newAnimal.itemId = 0;
                    this.vehicleId.getVehicle().setCurrentTotalAnimalSize(this.vehicleId.getVehicle().getCurrentTotalAnimalSize() - this.animalId.getAnimal().getAnimalTrailerSize());
                    AnimalInstanceManager.getInstance().add(newAnimal, this.animalId.getID());
                    newAnimal.addToWorld();
                    break;
                }
                case 4: {
                    if (!this.animalId.isConsistent(connection) || !this.playerId.isConsistent(connection) || !this.vehicleId.isConsistent(connection) || !this.vehicleId.getVehicle().animals.remove(this.animalId.getAnimal())) break;
                    this.vehicleId.getVehicle().setCurrentTotalAnimalSize(this.vehicleId.getVehicle().getCurrentTotalAnimalSize() - this.animalId.getAnimal().getAnimalTrailerSize());
                    AnimalInstanceManager.getInstance().remove(this.animalId.getAnimal());
                    InventoryItem newAnimal = this.item;
                    if (newAnimal instanceof AnimalInventoryItem) {
                        AnimalInventoryItem animal = (AnimalInventoryItem)newAnimal;
                        AnimalInstanceManager.getInstance().add(animal.getAnimal(), this.animalId.getID());
                        this.playerId.getPlayer().getInventory().AddItem(animal);
                        this.playerId.getPlayer().setPrimaryHandItem(animal);
                        this.playerId.getPlayer().setSecondaryHandItem(animal);
                        this.animalId.getAnimal().itemId = this.item.id;
                        break;
                    }
                    DebugLog.Animal.warn("AnimalInventoryItem is null");
                    break;
                }
                case 5: {
                    if (!this.animalId.isConsistent(connection) || !this.playerId.isConsistent(connection)) break;
                    if ((this.flags & 1) != 0) {
                        this.playerId.getPlayer().getAttachedAnimals().remove(this.animalId.getAnimal());
                        this.animalId.getAnimal().getData().setAttachedPlayer(null);
                        break;
                    }
                    this.playerId.getPlayer().getAttachedAnimals().add(this.animalId.getAnimal());
                    this.animalId.getAnimal().getData().setAttachedTree(null);
                    this.animalId.getAnimal().getData().setAttachedPlayer(this.playerId.getPlayer());
                    break;
                }
                case 6: {
                    if (!this.animalId.isConsistent(connection) || !this.objectId.isConsistent(connection)) break;
                    if ((this.flags & 1) != 0) {
                        this.animalId.getAnimal().getData().setAttachedTree(null);
                        break;
                    }
                    this.animalId.getAnimal().getData().setAttachedTree(this.objectId.getObject());
                    this.playerId.getPlayer().removeAttachedAnimal(this.animalId.getAnimal());
                    this.animalId.getAnimal().getData().setAttachedPlayer(null);
                    break;
                }
                case 7: {
                    if (!this.animalId.isConsistent(connection) || !this.playerId.isConsistent(connection)) break;
                    InventoryItem newAnimal = this.item;
                    if (newAnimal instanceof AnimalInventoryItem) {
                        AnimalInventoryItem animal = (AnimalInventoryItem)newAnimal;
                        AnimalInstanceManager.getInstance().add(animal.getAnimal(), this.animalId.getID());
                        this.playerId.getPlayer().getInventory().AddItem(animal);
                        this.playerId.getPlayer().getAttachedAnimals().remove(this.animalId.getAnimal());
                        this.animalId.getAnimal().getData().setAttachedPlayer(null);
                        this.animalId.getAnimal().removeFromWorld();
                        this.animalId.getAnimal().removeFromSquare();
                        this.animalId.getAnimal().itemId = this.item.id;
                        this.animalId.getAnimal().playBreedSound("pick_up");
                        break;
                    }
                    DebugLog.Animal.warn("AnimalInventoryItem is null");
                    break;
                }
                case 12: {
                    if (!this.animalId.isConsistent(connection)) break;
                    IsoAnimal newAnimal = new IsoAnimal(IsoWorld.instance.getCell(), this.x, this.y, this.z, this.animalId.getAnimal().getAnimalType(), this.animalId.getAnimal().getBreed());
                    newAnimal.copyFrom(this.animalId.getAnimal());
                    newAnimal.itemId = 0;
                    newAnimal.attachBackToMotherTimer = 10000.0f;
                    AnimalInstanceManager.getInstance().add(newAnimal, this.animalId.getID());
                    newAnimal.addToWorld();
                    newAnimal.playBreedSound("put_down");
                    break;
                }
                case 8: {
                    IsoDeadBody deadBody = (IsoDeadBody)this.bodyId.getObject();
                    if (deadBody != null && this.playerId.isConsistent(connection)) {
                        Object functionObj = LuaManager.getFunctionObject("AnimalPartsDefinitions.butcherAnimalFromGround");
                        if (functionObj == null) break;
                        LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, deadBody, this.playerId.getPlayer());
                        break;
                    }
                    DebugLog.Animal.warn("IsoDeadBody is null");
                    break;
                }
                case 9: {
                    if (!this.animalId.isConsistent(connection) || !this.playerId.isConsistent(connection)) break;
                    InventoryItem inventoryItem = this.item;
                    if (inventoryItem instanceof InventoryItem) {
                        InventoryItem inventoryItem2 = inventoryItem;
                        this.animalId.getAnimal().feedFromHand(this.playerId.getPlayer(), inventoryItem2);
                        break;
                    }
                    DebugLog.Animal.warn("InventoryItem is null");
                    break;
                }
                case 10: 
                case 11: {
                    if (!this.animalId.isConsistent(connection) || !this.playerId.isConsistent(connection) || !this.objectId.isConsistent(connection)) break;
                    InventoryItem inventoryItem = this.item;
                    if (inventoryItem instanceof InventoryItem) {
                        InventoryItem inventoryItem3 = inventoryItem;
                        IsoObject isoObject = this.objectId.getObject();
                        if (isoObject instanceof IsoHutch) {
                            IsoHutch hutch = (IsoHutch)isoObject;
                            hutch.removeAnimal(this.animalId.getAnimal());
                            break;
                        }
                        DebugLog.Animal.warn("IsoHutch is null");
                        break;
                    }
                    DebugLog.Animal.warn("InventoryItem is null");
                    break;
                }
                case 14: {
                    break;
                }
                case 13: {
                    if (!this.animalId.isConsistent(connection) || !this.playerId.isConsistent(connection) || !this.objectId.isConsistent(connection)) break;
                    IsoObject isoObject = this.objectId.getObject();
                    if (isoObject instanceof IsoHutch) {
                        IsoHutch hutch = (IsoHutch)isoObject;
                        hutch.removeAnimal(this.animalId.getAnimal());
                        break;
                    }
                    DebugLog.Animal.warn("IsoHutch is null");
                    break;
                }
                default: {
                    DebugLog.Animal.warn("AnimalPacket \"%s\" is not supported", new Object[]{this.type});
                }
            }
        }
    }

    public static enum Type {
        None,
        AddAnimalFromHandsInTrailer,
        AddAnimalInTrailer,
        RemoveAnimalFromTrailer,
        RemoveAndGrabAnimalFromTrailer,
        AttachAnimalToPlayer,
        AttachAnimalToTree,
        PickupAnimal,
        ButcherAnimal,
        FeedAnimalFromHand,
        HutchGrabAnimal,
        HutchGrabCorpseAction,
        DropAnimal,
        HutchRemoveAnimal,
        UpdateGenome;

    }
}


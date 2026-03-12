/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.util.ArrayList;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class SyncVisualsPacket
implements INetworkPacket {
    @JSONField
    private final PlayerID playerId = new PlayerID();
    ItemVisuals itemVisuals = new ItemVisuals();
    private int itemVisualsSize;

    @Override
    public void setData(Object ... values2) {
        if (values2.length == 1 && values2[0] instanceof IsoPlayer) {
            this.set((IsoPlayer)values2[0]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(IsoPlayer player) {
        this.playerId.set(player);
        this.itemVisuals.clear();
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.playerId.getPlayer() != null && this.itemVisualsSize == this.itemVisuals.size();
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.playerId.parse(b, connection);
        if (this.playerId.isConsistent(connection)) {
            this.playerId.getPlayer().getItemVisuals(this.itemVisuals);
            this.itemVisualsSize = b.getByte();
            if (this.itemVisualsSize != this.itemVisuals.size()) {
                DebugLog.General.error("Player has " + this.itemVisuals.size() + " itemVisuals but server tries to sync " + this.itemVisualsSize + " ones");
                return;
            }
            for (int i = 0; i < this.itemVisualsSize; ++i) {
                ItemVisual itemVisual = (ItemVisual)this.itemVisuals.get(i);
                InventoryItem inventoryItem = itemVisual.getInventoryItem();
                if (inventoryItem instanceof Clothing) {
                    Clothing clothing = (Clothing)inventoryItem;
                    clothing.removeAllPatches();
                }
                for (int j = 0; j < BloodBodyPartType.MAX.index(); ++j) {
                    BloodBodyPartType part = BloodBodyPartType.FromIndex(j);
                    byte basicPatch = b.getByte();
                    byte denimPatch = b.getByte();
                    byte leatherPatch = b.getByte();
                    byte hole = b.getByte();
                    float dirt = b.getFloat();
                    float blood = b.getFloat();
                    itemVisual.removePatch(part.index());
                    itemVisual.removeHole(part.index());
                    if (basicPatch != 0) {
                        itemVisual.setBasicPatch(part);
                    }
                    if (denimPatch != 0) {
                        itemVisual.setDenimPatch(part);
                    }
                    if (leatherPatch != 0) {
                        itemVisual.setLeatherPatch(part);
                    }
                    if (hole != 0) {
                        itemVisual.setHole(part);
                    }
                    itemVisual.setDirt(part, dirt);
                    itemVisual.setBlood(part, blood);
                }
                this.parsePatches(b, itemVisual);
            }
        }
    }

    private void parsePatches(ByteBufferReader b, ItemVisual itemVisual) {
        byte patchesNum = b.getByte();
        if (patchesNum <= 0) {
            return;
        }
        for (byte i = 0; i < patchesNum; i = (byte)(i + 1)) {
            byte bloodBodyPartTypeIdx = b.getByte();
            byte tailorLvl = b.getByte();
            byte fabricType = b.getByte();
            boolean hasHole = b.getBoolean();
            InventoryItem inventoryItem = itemVisual.getInventoryItem();
            if (!(inventoryItem instanceof Clothing)) continue;
            Clothing clothing = (Clothing)inventoryItem;
            clothing.addPatchForSync(bloodBodyPartTypeIdx, tailorLvl, fabricType, hasHole);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.playerId.getPlayer().resetModelNextFrame();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID() || !c.isRelevantTo(this.playerId.getX(), this.playerId.getY())) continue;
            ByteBufferWriter b2 = c.startPacket();
            PacketTypes.PacketType.SyncVisuals.doPacket(b2);
            this.write(b2);
            PacketTypes.PacketType.SyncVisuals.send(c);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        this.playerId.getPlayer().getItemVisuals(this.itemVisuals);
        b.putByte(this.itemVisuals.size());
        ArrayList<Patch> patches = new ArrayList<Patch>(this.itemVisuals.size());
        for (int i = 0; i < this.itemVisuals.size(); ++i) {
            Clothing clothing;
            Object part;
            ItemVisual itemVisual = (ItemVisual)this.itemVisuals.get(i);
            for (int j = 0; j < BloodBodyPartType.MAX.index(); ++j) {
                part = BloodBodyPartType.FromIndex(j);
                b.putBoolean(itemVisual.getBasicPatch((BloodBodyPartType)((Object)part)) != 0.0f);
                b.putBoolean(itemVisual.getDenimPatch((BloodBodyPartType)((Object)part)) != 0.0f);
                b.putBoolean(itemVisual.getLeatherPatch((BloodBodyPartType)((Object)part)) != 0.0f);
                b.putBoolean(itemVisual.getHole((BloodBodyPartType)((Object)part)) != 0.0f);
                b.putFloat(itemVisual.getDirt((BloodBodyPartType)((Object)part)));
                b.putFloat(itemVisual.getBlood((BloodBodyPartType)((Object)part)));
            }
            patches.clear();
            part = itemVisual.getInventoryItem();
            if (part instanceof Clothing && (clothing = (Clothing)part).getPatchesNumber() > 0) {
                for (int j = 0; j < BloodBodyPartType.MAX.index(); ++j) {
                    Clothing.ClothingPatch clothingPatch = clothing.getPatchType(BloodBodyPartType.FromIndex(j));
                    if (clothingPatch == null) continue;
                    Patch patch = new Patch();
                    patch.partIndex = j;
                    patch.patch = clothingPatch;
                    patches.add(patch);
                }
            }
            this.writePatches(b, patches);
        }
    }

    private void writePatches(ByteBufferWriter b, ArrayList<Patch> patches) {
        b.putByte(patches.size());
        for (Patch patch : patches) {
            b.putByte(patch.partIndex);
            b.putByte(patch.patch.tailorLvl);
            b.putByte(patch.patch.fabricType);
            b.putBoolean(patch.patch.hasHole);
        }
    }

    private static final class Patch {
        int partIndex;
        Clothing.ClothingPatch patch;

        private Patch() {
        }
    }
}


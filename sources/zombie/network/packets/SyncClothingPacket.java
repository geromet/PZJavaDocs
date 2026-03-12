/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.MatchException
 */
package zombie.network.packets;

import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import zombie.Lua.LuaEventManager;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.WornItems.WornItem;
import zombie.characters.animals.IsoAnimal;
import zombie.core.ImmutableColor;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.Clothing;
import zombie.network.GameClient;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerGUI;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ResourceLocation;
import zombie.util.Type;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class SyncClothingPacket
implements INetworkPacket {
    @JSONField
    private final PlayerID playerId = new PlayerID();
    @JSONField
    private final ArrayList<ItemDescription> items = new ArrayList();

    @Override
    public void setData(Object ... values2) {
        if (values2.length == 1 && values2[0] instanceof IsoPlayer) {
            this.set((IsoPlayer)values2[0]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(IsoPlayer player) {
        if (player instanceof IsoAnimal) {
            DebugLog.General.printStackTrace("SyncClothingPacket.set receives IsoAnimal");
        }
        this.playerId.set(player);
        this.items.clear();
        this.playerId.getPlayer().getWornItems().forEach(item -> {
            if (item != null && item.getItem() != null) {
                this.items.add(new ItemDescription((WornItem)item));
            }
        });
    }

    void parseClothing(ByteBufferReader b, int itemId) {
        IsoPlayer player = this.playerId.getPlayer();
        if (player == null) {
            return;
        }
        Clothing clothing = Type.tryCastTo(player.getInventory().getItemWithID(itemId), Clothing.class);
        if (clothing != null) {
            clothing.removeAllPatches();
        }
        byte patchesNum = b.getByte();
        for (byte j = 0; j < patchesNum; j = (byte)(j + 1)) {
            byte bloodBodyPartTypeIdx = b.getByte();
            byte tailorLvl = b.getByte();
            byte fabricType = b.getByte();
            boolean hasHole = b.getBoolean();
            if (clothing == null) continue;
            ItemVisual itemVisual = clothing.getVisual();
            if (itemVisual instanceof ItemVisual) {
                Clothing.ClothingPatchFabricType patchFabric;
                ItemVisual itemVisual2 = itemVisual;
                itemVisual2.removeHole(bloodBodyPartTypeIdx);
                BloodBodyPartType bloodBodyPartType = BloodBodyPartType.FromIndex(bloodBodyPartTypeIdx);
                Clothing.ClothingPatchFabricType clothingPatchFabricType = patchFabric = Clothing.ClothingPatchFabricType.fromIndex(fabricType);
                int n = 0;
                switch (SwitchBootstraps.enumSwitch("enumSwitch", new Object[]{"Cotton", "Denim", "Leather"}, (Clothing.ClothingPatchFabricType)clothingPatchFabricType, n)) {
                    default: {
                        throw new MatchException(null, null);
                    }
                    case 0: {
                        itemVisual2.setBasicPatch(bloodBodyPartType);
                        break;
                    }
                    case 1: {
                        itemVisual2.setDenimPatch(bloodBodyPartType);
                        break;
                    }
                    case 2: {
                        itemVisual2.setLeatherPatch(bloodBodyPartType);
                    }
                    case -1: 
                }
            }
            clothing.addPatchForSync(bloodBodyPartTypeIdx, tailorLvl, fabricType, hasHole);
        }
    }

    void writeClothing(ByteBufferWriter b, int itemId) {
        IsoPlayer player = this.playerId.getPlayer();
        if (player == null) {
            b.putByte(0);
            return;
        }
        Clothing clothing = Type.tryCastTo(player.getInventory().getItemWithID(itemId), Clothing.class);
        if (clothing == null) {
            b.putByte(0);
            return;
        }
        b.putByte(clothing.getPatchesNumber());
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            Clothing.ClothingPatch patch = clothing.getPatchType(BloodBodyPartType.FromIndex(i));
            if (patch == null) continue;
            b.putByte(i);
            b.putByte(patch.tailorLvl);
            b.putByte(patch.fabricType);
            b.putBoolean(patch.hasHole);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.playerId.parse(b, connection);
        IsoPlayer player = this.playerId.getPlayer();
        if (player == null) {
            return;
        }
        this.items.clear();
        int size = b.getByte();
        for (int i = 0; i < size; ++i) {
            ItemDescription item = new ItemDescription();
            item.parse(b, connection);
            this.items.add(item);
            this.parseClothing(b, item.itemId);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        b.putByte(this.items.size());
        for (ItemDescription item : this.items) {
            item.write(b);
            this.writeClothing(b, item.itemId);
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.playerId.getPlayer() != null;
    }

    private boolean isItemsContains(int itemId, ItemBodyLocation location) {
        for (ItemDescription item : this.items) {
            if (item.itemId != itemId || !item.location.equals(location)) continue;
            return true;
        }
        return false;
    }

    private void process() {
        if (this.playerId.getPlayer().remote) {
            this.playerId.getPlayer().getItemVisuals().clear();
        }
        ArrayList itemsForDelete = new ArrayList();
        this.playerId.getPlayer().getWornItems().forEach(item -> {
            if (!this.isItemsContains(item.getItem().getID(), item.getLocation())) {
                itemsForDelete.add(item.getItem());
            }
        });
        for (InventoryItem inventoryItem : itemsForDelete) {
            this.playerId.getPlayer().getWornItems().remove(inventoryItem);
        }
        for (ItemDescription itemDescription : this.items) {
            Clothing wornItem = Type.tryCastTo(this.playerId.getPlayer().getWornItems().getItem(itemDescription.location), Clothing.class);
            int wornItemId = wornItem == null ? -1 : wornItem.getID();
            if (wornItemId == itemDescription.itemId) continue;
            InventoryItem itemForAdd = this.playerId.getPlayer().getInventory().getItemWithID(itemDescription.itemId);
            if (itemForAdd == null) {
                itemForAdd = InventoryItemFactory.CreateItem(itemDescription.itemType);
            }
            if (itemForAdd == null) continue;
            this.playerId.getPlayer().getWornItems().setItem(itemDescription.location, itemForAdd);
            if (!this.playerId.getPlayer().remote) continue;
            itemForAdd.getVisual().setTint(itemDescription.tint);
            itemForAdd.getVisual().setBaseTexture(itemDescription.baseTexture);
            itemForAdd.getVisual().setTextureChoice(itemDescription.textureChoice);
            this.playerId.getPlayer().getItemVisuals().add(itemForAdd.getVisual());
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (GameClient.client) {
            this.process();
            this.playerId.getPlayer().onWornItemsChanged();
        }
        this.playerId.getPlayer().resetModelNextFrame();
        LuaEventManager.triggerEvent("OnClothingUpdated", this.playerId.getPlayer());
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.process();
        if (ServerGUI.isCreated()) {
            this.playerId.getPlayer().resetModelNextFrame();
        }
        this.sendToClients(PacketTypes.PacketType.SyncClothing, null);
    }

    static class ItemDescription
    implements INetworkPacket {
        @JSONField
        int itemId;
        @JSONField
        String itemType;
        @JSONField
        ItemBodyLocation location;
        @JSONField
        ImmutableColor tint;
        @JSONField
        int textureChoice;
        @JSONField
        int baseTexture;

        public ItemDescription() {
        }

        public ItemDescription(WornItem item) {
            this.itemId = item.getItem().getID();
            this.itemType = item.getItem().getFullType();
            this.location = item.getLocation();
            this.baseTexture = item.getItem().getVisual() == null ? -1 : item.getItem().getVisual().getBaseTexture();
            this.textureChoice = item.getItem().getVisual() == null ? -1 : item.getItem().getVisual().getTextureChoice();
            this.tint = item.getItem().getVisual().getTint();
        }

        @Override
        public void write(ByteBufferWriter b) {
            b.putInt(this.itemId);
            b.putUTF(this.itemType);
            b.putUTF(this.location.toString());
            b.putInt(this.textureChoice);
            b.putInt(this.baseTexture);
            b.putFloat(this.tint.r);
            b.putFloat(this.tint.g);
            b.putFloat(this.tint.b);
            b.putFloat(this.tint.a);
        }

        @Override
        public void parse(ByteBufferReader b, IConnection connection) {
            this.itemId = b.getInt();
            this.itemType = b.getUTF();
            this.location = ItemBodyLocation.get(ResourceLocation.of(b.getUTF()));
            this.textureChoice = b.getInt();
            this.baseTexture = b.getInt();
            this.tint = new ImmutableColor(b.getFloat(), b.getFloat(), b.getFloat(), b.getFloat());
        }
    }
}


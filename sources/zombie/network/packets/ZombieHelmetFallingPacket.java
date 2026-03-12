/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.io.IOException;
import zombie.Lua.LuaEventManager;
import zombie.PersistentOutfits;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.WornItems.WornItem;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoFallingClothing;
import zombie.network.GameClient;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.objects.Item;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class ZombieHelmetFallingPacket
implements INetworkPacket {
    boolean isZombie = true;
    short characterIndex = (short)-1;
    InventoryItem item;
    float targetX;
    float targetY;
    float targetZ;
    private static final ItemVisuals tempItemVisuals = new ItemVisuals();

    public void set(IsoGameCharacter character, InventoryItem item, float x, float y, float z) {
        this.isZombie = character instanceof IsoZombie;
        this.characterIndex = character.getOnlineID();
        this.item = item;
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putBoolean(this.isZombie);
        b.putShort(this.characterIndex);
        b.putFloat(this.targetX);
        b.putFloat(this.targetY);
        b.putFloat(this.targetZ);
        try {
            this.item.saveWithSize(b.bb, false);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.isZombie = b.getBoolean();
        this.characterIndex = b.getShort();
        this.targetX = b.getFloat();
        this.targetY = b.getFloat();
        this.targetZ = b.getFloat();
        try {
            this.item = InventoryItem.loadItem(b.bb, 244);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoZombie zed = null;
        IsoPlayer player = null;
        if (this.isZombie) {
            zed = GameClient.IDToZombieMap.get(this.characterIndex);
        } else {
            player = GameClient.IDToPlayerMap.get(this.characterIndex);
        }
        if (zed != null && !zed.isUsingWornItems()) {
            IsoFallingClothing falling = new IsoFallingClothing(zed.getCell(), zed.getX(), zed.getY(), PZMath.min(zed.getZ() + 0.4f, (float)zed.getZi() + 0.95f), 0.2f, 0.2f, this.item);
            falling.targetX = this.targetX;
            falling.targetY = this.targetY;
            falling.targetZ = this.targetZ;
            zed.getItemVisuals(tempItemVisuals);
            for (int i = 0; i < tempItemVisuals.size(); ++i) {
                ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
                Item scriptItem = itemVisual.getScriptItem();
                if (!scriptItem.name.equals(this.item.getType())) continue;
                tempItemVisuals.remove(i);
                break;
            }
            zed.getItemVisuals().clear();
            zed.getItemVisuals().addAll(tempItemVisuals);
            zed.resetModelNextFrame();
            zed.onWornItemsChanged();
            PersistentOutfits.instance.setFallenHat(zed, true);
        } else if (player != null && player.getWornItems() != null && !player.getWornItems().isEmpty()) {
            IsoFallingClothing falling = new IsoFallingClothing(player.getCell(), player.getX(), player.getY(), PZMath.min(player.getZ() + 0.4f, (float)player.getZi() + 0.95f), 0.2f, 0.2f, this.item);
            falling.targetX = this.targetX;
            falling.targetY = this.targetY;
            falling.targetZ = this.targetZ;
            for (int i = 0; i < player.getWornItems().size(); ++i) {
                WornItem wornItem = player.getWornItems().get(i);
                InventoryItem playerItem = wornItem.getItem();
                if (!playerItem.getType().equals(this.item.getType())) continue;
                player.getInventory().Remove(playerItem);
                player.getWornItems().remove(playerItem);
                player.resetModelNextFrame();
                player.onWornItemsChanged();
                if (GameClient.client && player.isLocalPlayer()) {
                    INetworkPacket.send(PacketTypes.PacketType.SyncClothing, player);
                }
                if (player.isLocalPlayer()) {
                    LuaEventManager.triggerEvent("OnClothingUpdated", player);
                }
                break;
            }
        } else {
            IsoFallingClothing falling = new IsoFallingClothing(IsoWorld.instance.currentCell, this.targetX, this.targetY, this.targetZ, 0.0f, 0.0f, this.item);
            falling.targetX = this.targetX;
            falling.targetY = this.targetY;
            falling.targetZ = this.targetZ;
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.item != null && IsoWorld.instance.currentCell != null && IsoWorld.instance.currentCell.getGridSquare(this.targetX, this.targetY, this.targetZ) != null;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.WorldSoundManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoObjectChange;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.interfaces.IClothingWasherDryerLogic;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class ClothingDryerLogic
implements IClothingWasherDryerLogic {
    private final IsoObject object;
    private boolean activated;
    private long soundInstance = -1L;
    private float lastUpdate = -1.0f;
    private boolean cycleFinished;
    private float startTime;
    private final float cycleLengthMinutes = 90.0f;
    private boolean alreadyExecuted;

    public ClothingDryerLogic(IsoObject object) {
        this.object = object;
    }

    public IsoObject getObject() {
        return this.object;
    }

    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        this.activated = input.get() != 0;
    }

    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        output.put(this.isActivated() ? (byte)1 : 0);
    }

    @Override
    public void update() {
        if (this.getObject().getObjectIndex() == -1) {
            return;
        }
        if (this.getContainer() == null) {
            return;
        }
        if (!this.getContainer().isPowered()) {
            this.setActivated(false);
        }
        this.cycleFinished();
        this.updateSound();
        if (GameClient.client) {
            // empty if block
        }
        if (!this.isActivated()) {
            this.lastUpdate = -1.0f;
            return;
        }
        float worldAgeHours = (float)GameTime.getInstance().getWorldAgeHours();
        if (this.lastUpdate < 0.0f) {
            this.lastUpdate = worldAgeHours;
        } else if (this.lastUpdate > worldAgeHours) {
            this.lastUpdate = worldAgeHours;
        }
        float elapsedHours = worldAgeHours - this.lastUpdate;
        int elapsedMinutes = (int)(elapsedHours * 60.0f);
        if (elapsedMinutes < 1) {
            return;
        }
        this.lastUpdate = worldAgeHours;
        for (int i = 0; i < this.getContainer().getItems().size(); ++i) {
            Clothing clothing;
            float wetness;
            InventoryItem item = this.getContainer().getItems().get(i);
            if (item instanceof Clothing && (wetness = (clothing = (Clothing)item).getWetness()) > 0.0f) {
                clothing.setWetness(wetness -= (float)elapsedMinutes);
                if (GameServer.server) {
                    // empty if block
                }
            }
            if (!item.isWet() || item.getItemWhenDry() == null) continue;
            item.setWetCooldown(item.getWetCooldown() - (float)(elapsedMinutes * 250));
            if (!(item.getWetCooldown() <= 0.0f)) continue;
            Object dryItem = InventoryItemFactory.CreateItem(item.getItemWhenDry());
            this.getContainer().addItem((InventoryItem)dryItem);
            this.getContainer().Remove(item);
            --i;
            item.setWet(false);
            IsoWorld.instance.currentCell.addToProcessItemsRemove(item);
        }
    }

    @Override
    public void saveChange(IsoObjectChange change, KahluaTable tbl, ByteBufferWriter bb) {
        if (change == IsoObjectChange.DRYER_STATE) {
            bb.putBoolean(this.isActivated());
        }
    }

    @Override
    public void loadChange(IsoObjectChange change, ByteBufferReader bb) {
        if (change == IsoObjectChange.DRYER_STATE) {
            this.setActivated(bb.getBoolean());
        }
    }

    @Override
    public ItemContainer getContainer() {
        return this.getObject().getContainerByType("clothingdryer");
    }

    private void updateSound() {
        if (this.isActivated()) {
            if (!GameServer.server) {
                if (this.getObject().emitter != null && this.getObject().emitter.isPlaying("ClothingDryerFinished")) {
                    this.getObject().emitter.stopOrTriggerSoundByName("ClothingDryerFinished");
                }
                if (this.soundInstance == -1L) {
                    this.getObject().emitter = IsoWorld.instance.getFreeEmitter((float)this.getObject().getXi() + 0.5f, (float)this.getObject().getYi() + 0.5f, this.getObject().getZi());
                    IsoWorld.instance.setEmitterOwner(this.getObject().emitter, this.getObject());
                    this.soundInstance = this.getObject().emitter.playSoundLoopedImpl("ClothingDryerRunning");
                }
            }
            if (!GameClient.client) {
                WorldSoundManager.instance.addSoundRepeating((Object)this, this.getObject().square.x, this.getObject().square.y, this.getObject().square.z, 10, 10, false);
            }
        } else if (this.soundInstance != -1L) {
            this.getObject().emitter.stopOrTriggerSound(this.soundInstance);
            this.soundInstance = -1L;
            if (this.cycleFinished) {
                this.cycleFinished = false;
                this.getObject().emitter.playSoundImpl("ClothingDryerFinished", this.getObject());
            }
        }
    }

    private boolean cycleFinished() {
        if (this.isActivated()) {
            float elapsedHours;
            int elapsedMinutes;
            if (!this.alreadyExecuted) {
                this.startTime = (float)GameTime.getInstance().getWorldAgeHours();
                this.alreadyExecuted = true;
            }
            if ((float)(elapsedMinutes = (int)((elapsedHours = (float)GameTime.getInstance().getWorldAgeHours() - this.startTime) * 60.0f)) < 90.0f) {
                return false;
            }
            this.cycleFinished = true;
            this.setActivated(false);
        }
        return true;
    }

    @Override
    public boolean isItemAllowedInContainer(ItemContainer container, InventoryItem item) {
        if (this.isActivated()) {
            return false;
        }
        return this.getContainer() == container;
    }

    @Override
    public boolean isRemoveItemAllowedFromContainer(ItemContainer container, InventoryItem item) {
        if (!this.getContainer().isEmpty() && this.isActivated()) {
            return false;
        }
        return this.getContainer() == container;
    }

    @Override
    public boolean isActivated() {
        return this.activated;
    }

    @Override
    public void setActivated(boolean activated) {
        Thread thread2;
        boolean bUpdateGenerator = activated != this.activated;
        this.activated = activated;
        this.alreadyExecuted = false;
        if (bUpdateGenerator && ((thread2 = Thread.currentThread()) == GameWindow.gameThread || thread2 == GameServer.mainThread)) {
            IsoGenerator.updateGenerator(this.getObject().getSquare());
        }
    }

    @Override
    public void switchModeOn() {
    }

    @Override
    public void switchModeOff() {
        this.setActivated(false);
        this.updateSound();
        this.cycleFinished = false;
    }
}


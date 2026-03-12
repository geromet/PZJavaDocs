/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import fmod.fmod.FMODManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.WorldSoundManager;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoObjectChange;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.interfaces.IClothingWasherDryerLogic;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.objects.ItemBodyLocation;

public final class ClothingWasherLogic
implements IClothingWasherDryerLogic {
    private final IsoObject object;
    private boolean activated;
    private long soundInstance = -1L;
    private float lastUpdate = -1.0f;
    private boolean cycleFinished;
    private float startTime;
    private final float cycleLengthMinutes = 90.0f;
    private boolean alreadyExecuted;

    public ClothingWasherLogic(IsoObject object) {
        this.object = object;
    }

    public IsoObject getObject() {
        return this.object;
    }

    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        this.activated = input.get() != 0;
        this.lastUpdate = input.getFloat();
    }

    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        output.put(this.isActivated() ? (byte)1 : 0);
        output.putFloat(this.lastUpdate);
    }

    @Override
    public void update() {
        if (this.getObject().getObjectIndex() == -1) {
            return;
        }
        if (!this.getContainer().isPowered()) {
            this.setActivated(false);
        }
        this.updateSound();
        this.cycleFinished();
        if (GameClient.client) {
            // empty if block
        }
        if (this.getObject().getFluidAmount() <= 0.0f) {
            this.setActivated(false);
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
        this.getObject().useFluid(1 * elapsedMinutes);
        for (int i = 0; i < this.getContainer().getItems().size(); ++i) {
            InventoryContainer invContainer;
            InventoryItem item = this.getContainer().getItems().get(i);
            if (item instanceof Clothing) {
                float dirtyness;
                Clothing clothing = (Clothing)item;
                float bloodLevel = clothing.getBloodlevel();
                if (bloodLevel > 0.0f) {
                    this.removeBlood(clothing, elapsedMinutes * 2);
                }
                if ((dirtyness = clothing.getDirtiness()) > 0.0f) {
                    this.removeDirt(clothing, elapsedMinutes * 2);
                }
                clothing.setWetness(100.0f);
                continue;
            }
            if (item.getModData().rawget("ItemAfterCleaning") != null) {
                String resultItem = (String)item.getModData().rawget("ItemAfterCleaning");
                Double cleaningProgress = (Double)item.getModData().rawget("CleaningProgress");
                if (cleaningProgress == null) {
                    item.getModData().rawset("CleaningProgress", (Object)((double)elapsedMinutes * 3.0));
                } else if (cleaningProgress >= 100.0) {
                    this.getContainer().getItems().remove(item);
                    this.getContainer().addItem((InventoryItem)InventoryItemFactory.CreateItem(resultItem));
                } else {
                    item.getModData().rawset("CleaningProgress", (Object)(cleaningProgress + (double)(elapsedMinutes * 2)));
                }
            }
            if (!(item instanceof InventoryContainer) || !((invContainer = (InventoryContainer)item).getBloodLevel() > 0.0f)) continue;
            invContainer.setBloodLevel(invContainer.getBloodLevel() - (float)(elapsedMinutes * 2) / 100.0f);
        }
    }

    private void removeBlood(Clothing item, float amount) {
        ItemVisual itemVisual = item.getVisual();
        if (itemVisual == null) {
            return;
        }
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            BloodBodyPartType part = BloodBodyPartType.FromIndex(i);
            float blood = itemVisual.getBlood(part);
            if (!(blood > 0.0f)) continue;
            itemVisual.setBlood(part, blood - amount / 100.0f);
        }
        BloodClothingType.calcTotalBloodLevel(item);
    }

    private void removeDirt(Clothing item, float amount) {
        ItemVisual itemVisual = item.getVisual();
        if (itemVisual == null) {
            return;
        }
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            BloodBodyPartType part = BloodBodyPartType.FromIndex(i);
            float dirt = itemVisual.getDirt(part);
            if (!(dirt > 0.0f)) continue;
            itemVisual.setDirt(part, dirt - amount / 100.0f);
        }
        BloodClothingType.calcTotalDirtLevel(item);
    }

    @Override
    public void saveChange(IsoObjectChange change, KahluaTable tbl, ByteBufferWriter bb) {
        if (change == IsoObjectChange.WASHER_STATE) {
            bb.putBoolean(this.isActivated());
        }
    }

    @Override
    public void loadChange(IsoObjectChange change, ByteBufferReader bb) {
        if (change == IsoObjectChange.WASHER_STATE) {
            this.setActivated(bb.getBoolean());
        }
    }

    @Override
    public ItemContainer getContainer() {
        return this.getObject().getContainerByType("clothingwasher");
    }

    private void updateSound() {
        if (this.isActivated()) {
            if (!GameServer.server) {
                if (this.getObject().emitter != null && this.getObject().emitter.isPlaying("ClothingWasherFinished")) {
                    this.getObject().emitter.stopOrTriggerSoundByName("ClothingWasherFinished");
                }
                if (this.soundInstance == -1L) {
                    this.getObject().emitter = IsoWorld.instance.getFreeEmitter((float)this.getObject().getXi() + 0.5f, (float)this.getObject().getYi() + 0.5f, this.getObject().getZi());
                    IsoWorld.instance.setEmitterOwner(this.getObject().emitter, this.getObject());
                    this.soundInstance = this.getObject().emitter.playSoundLoopedImpl("ClothingWasherRunning");
                    ItemContainer container = this.getContainer();
                    boolean bHasNoisyItems = this.hasNoisyItems(container);
                    this.getObject().emitter.setParameterValue(this.soundInstance, FMODManager.instance.getParameterDescription("ClothingWasherLoaded"), bHasNoisyItems ? 1.0f : 0.0f);
                }
            }
            if (!GameClient.client) {
                int radius = this.hasNoisyItems(this.getContainer()) ? 20 : 10;
                WorldSoundManager.instance.addSoundRepeating((Object)this, this.getObject().square.x, this.getObject().square.y, this.getObject().square.z, radius, 10, false);
            }
        } else if (this.soundInstance != -1L) {
            this.getObject().emitter.stopOrTriggerSound(this.soundInstance);
            this.soundInstance = -1L;
            if (this.cycleFinished) {
                this.cycleFinished = false;
                this.getObject().emitter.playSoundImpl("ClothingWasherFinished", this.getObject());
            }
        }
    }

    private boolean hasNoisyItems(ItemContainer container) {
        if (container == null) {
            return false;
        }
        ArrayList<InventoryItem> items = container.getItems();
        if (items == null || items.isEmpty()) {
            return false;
        }
        float nonClothingWeight = 0.0f;
        for (int i = 0; i < items.size(); ++i) {
            InventoryItem item = items.get(i);
            if (!(item instanceof Clothing)) {
                nonClothingWeight += item.getActualWeight();
                continue;
            }
            if (item.getBodyLocation() != ItemBodyLocation.SHOES) continue;
            return true;
        }
        return nonClothingWeight >= 5.0f;
    }

    @Override
    public boolean isItemAllowedInContainer(ItemContainer container, InventoryItem item) {
        if (container != this.getContainer()) {
            return false;
        }
        return !this.isActivated();
    }

    @Override
    public boolean isRemoveItemAllowedFromContainer(ItemContainer container, InventoryItem item) {
        if (container != this.getContainer()) {
            return false;
        }
        return container.isEmpty() || !this.isActivated();
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


/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeMap;
import zombie.SharedDescriptors;
import zombie.ZomboidFileSystem;
import zombie.characters.AttachedItems.AttachedWeaponDefinitions;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.characters.WornItems.WornItem;
import zombie.characters.ZombiesStageDefinitions;
import zombie.characters.ZombiesZoneDefinition;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.Clothing;
import zombie.iso.IsoWorld;
import zombie.iso.SliceY;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemType;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

public class PersistentOutfits {
    public static final PersistentOutfits instance = new PersistentOutfits();
    public static final int INVALID_ID = 0;
    public static final int FEMALE_BIT = Integer.MIN_VALUE;
    public static final int NO_HAT_BIT = 32768;
    private static final int FILE_VERSION_1 = 1;
    private static final int FILE_VERSION_LATEST = 1;
    private static final byte[] FILE_MAGIC = new byte[]{80, 83, 84, 90};
    private static final int NUM_SEEDS = 500;
    private final long[] seeds = new long[500];
    private final ArrayList<String> outfitNames = new ArrayList();
    private final DataList all = new DataList();
    private final DataList female = new DataList();
    private final DataList male = new DataList();
    private final TreeMap<String, Data> outfitToData = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    private final TreeMap<String, Data> outfitToFemale = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    private final TreeMap<String, Data> outfitToMale = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    private static final ItemVisuals tempItemVisuals = new ItemVisuals();

    public void init() {
        this.all.clear();
        this.female.clear();
        this.male.clear();
        this.outfitToData.clear();
        this.outfitToFemale.clear();
        this.outfitToMale.clear();
        this.outfitNames.clear();
        if (!GameClient.client) {
            for (int i = 0; i < 500; ++i) {
                this.seeds[i] = Rand.Next(Integer.MAX_VALUE);
            }
        }
        this.initOutfitList(OutfitManager.instance.femaleOutfits, true);
        this.initOutfitList(OutfitManager.instance.maleOutfits, false);
        this.registerCustomOutfits();
        if (GameClient.client) {
            return;
        }
        this.load();
        this.save();
    }

    private void initOutfitList(ArrayList<Outfit> outfitList, boolean female) {
        ArrayList<Outfit> outfits = new ArrayList<Outfit>(outfitList);
        outfits.sort(Comparator.comparing(o -> o.name));
        for (Outfit outfit : outfits) {
            this.initOutfit(outfit.name, female, true, PersistentOutfits::ApplyOutfit);
        }
    }

    private void initOutfit(String outfitName, boolean female, boolean useSeed, IOutfitter outfitter) {
        TreeMap<String, Data> outfitTo = female ? this.outfitToFemale : this.outfitToMale;
        Data data = this.outfitToData.get(outfitName);
        if (data == null) {
            data = new Data();
            data.index = (short)this.all.size();
            data.outfitName = outfitName;
            data.useSeed = useSeed;
            data.outfitter = outfitter;
            this.outfitNames.add(outfitName);
            this.outfitToData.put(outfitName, data);
            this.all.add(data);
        }
        DataList mf = female ? this.female : this.male;
        mf.add(data);
        outfitTo.put(outfitName, data);
    }

    private void registerCustomOutfits() {
        ArrayList<RandomizedVehicleStoryBase> storyList = IsoWorld.instance.getRandomizedVehicleStoryList();
        for (int i = 0; i < storyList.size(); ++i) {
            RandomizedVehicleStoryBase rvsb = storyList.get(i);
            rvsb.registerCustomOutfits();
        }
        ZombiesZoneDefinition.registerCustomOutfits();
        if (GameServer.server || GameClient.client) {
            this.registerOutfitter("ReanimatedPlayer", false, SharedDescriptors::ApplyReanimatedPlayerOutfit);
        }
    }

    public ArrayList<String> getOutfitNames() {
        return this.outfitNames;
    }

    public int pickRandomFemale() {
        if (this.female.isEmpty()) {
            return 0;
        }
        String outfitName = PZArrayUtil.pickRandom(this.female).outfitName;
        return this.pickOutfitFemale(outfitName);
    }

    public int pickRandomMale() {
        if (this.male.isEmpty()) {
            return 0;
        }
        String outfitName = PZArrayUtil.pickRandom(this.male).outfitName;
        return this.pickOutfitMale(outfitName);
    }

    public int pickOutfitFemale(String outfitName) {
        Data data = this.outfitToFemale.get(outfitName);
        if (data == null) {
            return 0;
        }
        short outfitIndex = (short)data.index;
        int variant = data.useSeed ? (int)Rand.Next(500) : 0;
        return Integer.MIN_VALUE | outfitIndex << 16 | variant + 1;
    }

    public int pickOutfitMale(String outfitName) {
        Data data = this.outfitToMale.get(outfitName);
        if (data == null) {
            return 0;
        }
        short outfitIndex = (short)data.index;
        int variant = data.useSeed ? (int)Rand.Next(500) : 0;
        return outfitIndex << 16 | variant + 1;
    }

    public int pickOutfit(String outfitName, boolean female) {
        String newOutfitName = ZombiesStageDefinitions.instance.getAdvancedOutfitName(outfitName);
        if (newOutfitName != null) {
            outfitName = newOutfitName;
        }
        return female ? this.pickOutfitFemale(outfitName) : this.pickOutfitMale(outfitName);
    }

    public int getOutfit(int id) {
        if (id == 0) {
            return 0;
        }
        int femaleBit = id & Integer.MIN_VALUE;
        int noHatBit = (id &= Integer.MAX_VALUE) & 0x8000;
        short outfitIndex = (short)((id &= 0xFFFF7FFF) >> 16);
        short variant = (short)(id & 0xFFFF);
        if (outfitIndex < 0 || outfitIndex >= this.all.size()) {
            return 0;
        }
        Data data = (Data)this.all.get(outfitIndex);
        if (data.useSeed && (variant < 1 || variant > 500)) {
            variant = (short)(Rand.Next(500) + 1);
        }
        return femaleBit | noHatBit | outfitIndex << 16 | variant;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void save() {
        if (Core.getInstance().isNoSave()) {
            return;
        }
        File outFile = ZomboidFileSystem.instance.getFileInCurrentSave("z_outfits.bin");
        try (FileOutputStream fos = new FileOutputStream(outFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);){
            Object object = SliceY.SliceBufferLock;
            synchronized (object) {
                SliceY.SliceBuffer.clear();
                ByteBuffer output = SliceY.SliceBuffer;
                this.save(output);
                bos.write(output.array(), 0, output.position());
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    public void save(ByteBuffer output) {
        output.put(FILE_MAGIC);
        output.putInt(1);
        output.putShort((short)500);
        for (int i = 0; i < 500; ++i) {
            output.putLong(this.seeds[i]);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void load() {
        File inFile = ZomboidFileSystem.instance.getFileInCurrentSave("z_outfits.bin");
        try (FileInputStream fis2 = new FileInputStream(inFile);
             BufferedInputStream bis = new BufferedInputStream(fis2);){
            Object object = SliceY.SliceBufferLock;
            synchronized (object) {
                SliceY.SliceBuffer.clear();
                ByteBuffer input = SliceY.SliceBuffer;
                int numBytes = bis.read(input.array());
                input.limit(numBytes);
                this.load(input);
            }
        }
        catch (FileNotFoundException fis2) {
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    public void load(ByteBuffer input) throws IOException {
        byte[] magic = new byte[4];
        input.get(magic);
        if (!Arrays.equals(magic, FILE_MAGIC)) {
            throw new IOException("not magic");
        }
        int version = input.getInt();
        if (version < 1 || version > 1) {
            return;
        }
        int numVariants = input.getShort();
        for (int i = 0; i < numVariants; ++i) {
            if (i >= 500) continue;
            this.seeds[i] = input.getLong();
        }
    }

    public void registerOutfitter(String id, boolean useSeed, IOutfitter outfitter) {
        this.initOutfit(id, true, useSeed, outfitter);
        this.initOutfit(id, false, useSeed, outfitter);
    }

    private static void ApplyOutfit(int outfitID, String outfitName, IsoGameCharacter chr) {
        instance.applyOutfit(outfitID, outfitName, chr);
    }

    private void applyOutfit(int outfitID, String outfitName, IsoGameCharacter chr) {
        boolean female = (outfitID & Integer.MIN_VALUE) != 0;
        short outfitIndex = (short)((outfitID &= Integer.MAX_VALUE) >> 16);
        Data data = (Data)this.all.get(outfitIndex);
        IsoZombie zombie = Type.tryCastTo(chr, IsoZombie.class);
        if (zombie != null) {
            zombie.setFemaleEtc(female);
        }
        chr.dressInNamedOutfit(data.outfitName);
        if (zombie != null && chr.doDirtBloodEtc) {
            AttachedWeaponDefinitions.instance.addRandomAttachedWeapon(zombie);
            zombie.addRandomBloodDirtHolesEtc();
        }
        this.removeFallenHat(outfitID, chr);
    }

    public boolean isHatFallen(IsoGameCharacter chr) {
        return this.isHatFallen(chr.getPersistentOutfitID());
    }

    public boolean isHatFallen(int outfitID) {
        return (outfitID & 0x8000) != 0;
    }

    public void setFallenHat(IsoGameCharacter chr, boolean fallen) {
        int outfitID = chr.getPersistentOutfitID();
        if (outfitID == 0) {
            return;
        }
        outfitID = fallen ? (outfitID |= 0x8000) : (outfitID &= 0xFFFF7FFF);
        chr.setPersistentOutfitID(outfitID, chr.isPersistentOutfitInit());
    }

    public boolean removeFallenHat(int outfitID, IsoGameCharacter chr) {
        if ((outfitID & 0x8000) == 0) {
            return false;
        }
        if (chr.isUsingWornItems()) {
            return false;
        }
        boolean removed = false;
        chr.getItemVisuals(tempItemVisuals);
        for (int i = 0; i < tempItemVisuals.size(); ++i) {
            ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem == null || scriptItem.getChanceToFall() <= 0) continue;
            chr.getItemVisuals().remove(itemVisual);
            removed = true;
        }
        return removed;
    }

    public InventoryItem processFallingHat(IsoGameCharacter character, boolean hitHead) {
        if (instance.isHatFallen(character)) {
            return null;
        }
        InventoryItem item = null;
        IsoZombie zombie = Type.tryCastTo(character, IsoZombie.class);
        if (zombie != null && !zombie.isUsingWornItems()) {
            zombie.getItemVisuals(tempItemVisuals);
            for (int i = 0; i < tempItemVisuals.size(); ++i) {
                ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
                Item scriptItem = itemVisual.getScriptItem();
                if (scriptItem == null || !scriptItem.isItemType(ItemType.CLOTHING) || scriptItem.getChanceToFall() <= 0) continue;
                int chanceToFall = scriptItem.getChanceToFall();
                if (hitHead) {
                    chanceToFall += 40;
                }
                if (Rand.Next(100) > chanceToFall || (item = (InventoryItem)InventoryItemFactory.CreateItem(scriptItem.getFullName())) == null) continue;
                if (item.getVisual() == null) break;
                item.getVisual().copyFrom(itemVisual);
                item.synchWithVisual();
                break;
            }
        } else if (character.getWornItems() != null && !character.getWornItems().isEmpty()) {
            for (int i = 0; i < character.getWornItems().size(); ++i) {
                WornItem wornItem = character.getWornItems().get(i);
                InventoryItem characterItem = wornItem.getItem();
                if (!(characterItem instanceof Clothing)) continue;
                Clothing clothing = (Clothing)characterItem;
                int chanceToFall = clothing.getChanceToFall();
                if (hitHead) {
                    chanceToFall += 40;
                }
                if (clothing.getChanceToFall() <= 0 || Rand.Next(100) > chanceToFall) continue;
                item = characterItem;
                break;
            }
        }
        if (item != null) {
            instance.setFallenHat(character, true);
        }
        return item;
    }

    public void dressInOutfit(IsoGameCharacter chr, int outfitID) {
        if ((outfitID = this.getOutfit(outfitID)) == 0) {
            return;
        }
        int outfitID2 = outfitID;
        short outfitIndex = (short)((outfitID2 &= 0x7FFF7FFF) >> 16);
        short variant = (short)(outfitID2 & 0xFFFF);
        Data data = (Data)this.all.get(outfitIndex);
        if (data.useSeed) {
            OutfitRNG.setSeed(this.seeds[variant - 1]);
        }
        data.outfitter.accept(outfitID, data.outfitName, chr);
    }

    private static final class DataList
    extends ArrayList<Data> {
        private DataList() {
        }
    }

    public static interface IOutfitter {
        public void accept(int var1, String var2, IsoGameCharacter var3);
    }

    private static final class Data {
        int index;
        String outfitName;
        boolean useSeed = true;
        IOutfitter outfitter;

        private Data() {
        }
    }
}


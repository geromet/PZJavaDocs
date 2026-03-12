/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.population;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import zombie.characters.HairOutfitDefinitions;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.model.CharacterMask;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.ClothingItemReference;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.debug.DebugType;
import zombie.util.list.PZArrayUtil;

@XmlType(name="Outfit")
public class Outfit
implements Cloneable {
    @XmlElement(name="m_Name")
    public String name = "Outfit";
    @XmlElement(name="m_Top")
    public boolean top = true;
    @XmlElement(name="m_Pants")
    public boolean pants = true;
    @XmlElement(name="m_TopTextures")
    public final ArrayList<String> topTextures = new ArrayList();
    @XmlElement(name="m_PantsTextures")
    public final ArrayList<String> pantsTextures = new ArrayList();
    @XmlElement(name="m_items")
    public final ArrayList<ClothingItemReference> items = new ArrayList();
    @XmlElement(name="m_AllowPantsHue")
    public boolean allowPantsHue = true;
    @XmlElement(name="m_AllowPantsTint")
    public boolean allowPantsTint;
    @XmlElement(name="m_AllowTopTint")
    public boolean allowTopTint = true;
    @XmlElement(name="m_AllowTShirtDecal")
    public boolean allowTshirtDecal = true;
    @XmlTransient
    public String modId;
    @XmlTransient
    public boolean immutable;
    @XmlTransient
    public final RandomData randomData = new RandomData();

    public void setModID(String modID) {
        this.modId = modID;
        for (ClothingItemReference clothingItemReference : this.items) {
            clothingItemReference.setModID(modID);
        }
    }

    public void AddItem(ClothingItemReference item) {
        this.items.add(item);
    }

    public void Randomize() {
        if (this.immutable) {
            throw new RuntimeException("trying to randomize an immutable Outfit");
        }
        for (int i = 0; i < this.items.size(); ++i) {
            ClothingItemReference itemRef = this.items.get(i);
            itemRef.randomize();
        }
        this.randomData.hairColor = HairOutfitDefinitions.instance.getRandomHaircutColor(this.name);
        this.randomData.femaleHairName = HairStyles.instance.getRandomFemaleStyle(this.name);
        this.randomData.maleHairName = HairStyles.instance.getRandomMaleStyle(this.name);
        this.randomData.beardName = BeardStyles.instance.getRandomStyle(this.name);
        this.randomData.topTint = OutfitRNG.randomImmutableColor();
        this.randomData.pantsTint = OutfitRNG.randomImmutableColor();
        this.randomData.pantsHue = OutfitRNG.Next(4) == 0 ? (float)OutfitRNG.Next(200) / 100.0f - 1.0f : 0.0f;
        this.randomData.hasTop = OutfitRNG.Next(16) != 0;
        this.randomData.hasTshirt = OutfitRNG.Next(2) == 0;
        boolean bl = this.randomData.hasTshirtDecal = OutfitRNG.Next(4) == 0;
        if (this.top) {
            this.randomData.hasTop = true;
        }
        this.randomData.topTexture = OutfitRNG.pickRandom(this.topTextures);
        this.randomData.pantsTexture = OutfitRNG.pickRandom(this.pantsTextures);
    }

    public void randomizeItem(String itemGuid) {
        ClothingItemReference itemRef = PZArrayUtil.find(this.items, item -> item.itemGuid.equals(itemGuid));
        if (itemRef != null) {
            itemRef.randomize();
        } else {
            DebugType.Clothing.warn("Outfit.randomizeItem> Could not find itemGuid: %s", itemGuid);
        }
    }

    public CharacterMask GetMask() {
        CharacterMask mask = new CharacterMask();
        for (int i = 0; i < this.items.size(); ++i) {
            ClothingItemReference itemRef = this.items.get(i);
            if (!itemRef.randomData.active) continue;
            ClothingItem.tryGetCombinedMask(itemRef, mask);
        }
        return mask;
    }

    public boolean containsItemGuid(String itemGuid) {
        boolean contains = false;
        for (int i = 0; i < this.items.size(); ++i) {
            ClothingItemReference itemRef = this.items.get(i);
            if (!itemRef.itemGuid.equals(itemGuid)) continue;
            contains = true;
            break;
        }
        return contains;
    }

    public ClothingItemReference findItemByGUID(String itemGuid) {
        for (int i = 0; i < this.items.size(); ++i) {
            ClothingItemReference itemRef = this.items.get(i);
            if (!itemRef.itemGuid.equals(itemGuid)) continue;
            return itemRef;
        }
        return null;
    }

    public Outfit clone() {
        try {
            Outfit clone = new Outfit();
            clone.name = this.name;
            clone.top = this.top;
            clone.pants = this.pants;
            PZArrayUtil.addAll(clone.pantsTextures, this.pantsTextures);
            PZArrayUtil.addAll(clone.topTextures, this.topTextures);
            PZArrayUtil.copy(clone.items, this.items, ClothingItemReference::clone);
            clone.allowPantsHue = this.allowPantsHue;
            clone.allowPantsTint = this.allowPantsTint;
            clone.allowTopTint = this.allowTopTint;
            clone.allowTshirtDecal = this.allowTshirtDecal;
            return clone;
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException("Outfit clone failed.", e);
        }
    }

    public ClothingItemReference findHat() {
        for (ClothingItemReference itemRef : this.items) {
            ClothingItem clothingItem;
            if (!itemRef.randomData.active || (clothingItem = itemRef.getClothingItem()) == null || !clothingItem.isHat()) continue;
            return itemRef;
        }
        return null;
    }

    public boolean isEmpty() {
        for (int i = 0; i < this.items.size(); ++i) {
            ClothingItemReference itemRef = this.items.get(i);
            ClothingItem clothingItem = OutfitManager.instance.getClothingItem(itemRef.itemGuid);
            if (clothingItem != null && clothingItem.isEmpty()) {
                return true;
            }
            for (int j = 0; j < itemRef.subItems.size(); ++j) {
                ClothingItemReference itemRef1 = itemRef.subItems.get(j);
                clothingItem = OutfitManager.instance.getClothingItem(itemRef1.itemGuid);
                if (clothingItem == null || !clothingItem.isEmpty()) continue;
                return true;
            }
        }
        return false;
    }

    public void loadItems() {
        for (int i = 0; i < this.items.size(); ++i) {
            ClothingItemReference itemRef = this.items.get(i);
            OutfitManager.instance.getClothingItem(itemRef.itemGuid);
            for (int j = 0; j < itemRef.subItems.size(); ++j) {
                ClothingItemReference itemRef1 = itemRef.subItems.get(j);
                OutfitManager.instance.getClothingItem(itemRef1.itemGuid);
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public static class RandomData {
        public ImmutableColor hairColor;
        public String maleHairName;
        public String femaleHairName;
        public String beardName;
        public ImmutableColor topTint;
        public ImmutableColor pantsTint;
        public float pantsHue;
        public boolean hasTop;
        public boolean hasTshirt;
        public boolean hasTshirtDecal;
        public String topTexture;
        public String pantsTexture;
    }
}


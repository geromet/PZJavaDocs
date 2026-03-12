/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.util.ArrayList;
import zombie.GameTime;
import zombie.ZomboidGlobals;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.BodyDamage.Thermoregulator;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.scripting.objects.ItemTag;

public final class ClothingWetness {
    private static final ItemVisuals itemVisuals = new ItemVisuals();
    private static final ArrayList<BloodBodyPartType> coveredParts = new ArrayList();
    private final IsoGameCharacter character;
    private final ItemList[] clothingList = new ItemList[BloodBodyPartType.MAX.index()];
    private final int[] perspiringParts = new int[BloodBodyPartType.MAX.index()];
    public boolean changed = true;

    public ClothingWetness(IsoGameCharacter character) {
        this.character = character;
        for (int i = 0; i < this.clothingList.length; ++i) {
            this.clothingList[i] = new ItemList();
        }
    }

    public void calculateExposedItems() {
        int i;
        for (i = 0; i < this.clothingList.length; ++i) {
            this.clothingList[i].clear();
        }
        this.character.getItemVisuals(itemVisuals);
        for (i = itemVisuals.size() - 1; i >= 0; --i) {
            ArrayList<BloodClothingType> types;
            ItemVisual itemVisual = (ItemVisual)itemVisuals.get(i);
            InventoryItem item = itemVisual.getInventoryItem();
            if (item == null || (types = item.getBloodClothingType()) == null) continue;
            coveredParts.clear();
            BloodClothingType.getCoveredParts(types, coveredParts);
            for (int j = 0; j < coveredParts.size(); ++j) {
                BloodBodyPartType part = coveredParts.get(j);
                this.clothingList[part.index()].add(item);
            }
        }
    }

    public void updateWetness(float outerWetnessInc, float outerWetnessDec) {
        float increaseMod;
        boolean dryBefore;
        boolean soakThrough;
        Thermoregulator.ThermalNode thermalNode;
        BodyPart bodyPart;
        BodyPartType bodyPartType;
        BloodBodyPartType part;
        int i;
        boolean umbrella = false;
        InventoryItem umbrellaItem = this.character.getPrimaryHandItem();
        if (umbrellaItem != null && umbrellaItem.isProtectFromRainWhileEquipped()) {
            umbrella = true;
        }
        if ((umbrellaItem = this.character.getSecondaryHandItem()) != null && umbrellaItem.isProtectFromRainWhileEquipped()) {
            umbrella = true;
        }
        if (this.changed) {
            this.changed = false;
            this.calculateExposedItems();
        }
        this.character.getItemVisuals(itemVisuals);
        for (int i2 = 0; i2 < itemVisuals.size(); ++i2) {
            InventoryItem item = ((ItemVisual)itemVisuals.get(i2)).getInventoryItem();
            if (!(item instanceof Clothing)) continue;
            Clothing clothing = (Clothing)item;
            if (item.hasTag(ItemTag.BREAK_WHEN_WET) && item.getWetness() >= 100.0f) {
                item.setCondition(0);
                this.character.onWornItemsChanged();
            }
            if (item.getBloodClothingType() == null) {
                clothing.updateWetness(true);
                continue;
            }
            clothing.flushWetness();
        }
        float baseIncrease = (float)ZomboidGlobals.wetnessIncrease * GameTime.instance.getMultiplier();
        float baseDecrease = (float)ZomboidGlobals.wetnessDecrease * GameTime.instance.getMultiplier();
        block1: for (i = 0; i < this.clothingList.length; ++i) {
            part = BloodBodyPartType.FromIndex(i);
            bodyPartType = BodyPartType.FromIndex(i);
            if (bodyPartType == BodyPartType.MAX) continue;
            bodyPart = this.character.getBodyDamage().getBodyPart(bodyPartType);
            thermalNode = this.character.getBodyDamage().getThermoregulator().getNodeForBloodType(part);
            if (bodyPart == null || thermalNode == null) continue;
            float baseDelta = 0.0f;
            float perspiration = PZMath.clamp(thermalNode.getSecondaryDelta(), 0.0f, 1.0f);
            perspiration *= perspiration;
            if ((perspiration *= 0.2f + 0.8f * (1.0f - thermalNode.getDistToCore())) > 0.1f) {
                baseDelta += perspiration;
            } else {
                float bodyHeat = (thermalNode.getSkinCelcius() - 20.0f) / 22.0f;
                bodyHeat *= bodyHeat;
                bodyHeat -= outerWetnessInc;
                bodyHeat = Math.max(0.0f, bodyHeat);
                baseDelta -= bodyHeat;
                if (outerWetnessInc > 0.0f) {
                    baseDelta = 0.0f;
                }
            }
            int n = this.perspiringParts[i] = baseDelta > 0.0f ? 1 : 0;
            if (baseDelta == 0.0f) continue;
            baseDelta = baseDelta > 0.0f ? (baseDelta *= baseIncrease) : (baseDelta *= baseDecrease);
            bodyPart.setWetness(bodyPart.getWetness() + baseDelta);
            if (baseDelta > 0.0f && bodyPart.getWetness() < 25.0f || baseDelta < 0.0f && bodyPart.getWetness() > 50.0f) continue;
            if (baseDelta > 0.0f) {
                float extTemp = this.character.getBodyDamage().getThermoregulator().getExternalAirTemperature();
                extTemp += 10.0f;
                extTemp = PZMath.clamp(extTemp, 0.0f, 20.0f) / 20.0f;
                baseDelta *= 0.4f + 0.6f * extTemp;
            }
            boolean holeBefore = false;
            soakThrough = false;
            dryBefore = false;
            for (int j = this.clothingList[i].size() - 1; j >= 0; --j) {
                InventoryItem item;
                if (baseDelta > 0.0f) {
                    int n2 = i;
                    this.perspiringParts[n2] = this.perspiringParts[n2] + 1;
                }
                if (!((item = (InventoryItem)this.clothingList[i].get(j)) instanceof Clothing)) continue;
                Clothing clothing = (Clothing)item;
                increaseMod = 1.0f;
                ItemVisual itemVisual = clothing.getVisual();
                if (itemVisual != null) {
                    if (itemVisual.getHole(part) > 0.0f) {
                        holeBefore = true;
                        continue;
                    }
                    if (baseDelta > 0.0f && clothing.getWetness() >= 100.0f) {
                        soakThrough = true;
                        continue;
                    }
                    if (baseDelta < 0.0f && clothing.getWetness() <= 0.0f) {
                        dryBefore = true;
                        continue;
                    }
                    if (baseDelta > 0.0f && clothing.getWaterResistance() > 0.0f && (increaseMod = PZMath.max(0.0f, 1.0f - clothing.getWaterResistance())) <= 0.0f) {
                        int n3 = i;
                        this.perspiringParts[n3] = this.perspiringParts[n3] - 1;
                        continue block1;
                    }
                }
                coveredParts.clear();
                BloodClothingType.getCoveredParts(item.getBloodClothingType(), coveredParts);
                float delta = baseDelta;
                if (delta > 0.0f) {
                    delta *= increaseMod;
                }
                if (holeBefore || soakThrough || dryBefore) {
                    delta /= 2.0f;
                }
                clothing.setWetness(clothing.getWetness() + delta);
                continue block1;
            }
        }
        for (i = 0; i < this.clothingList.length; ++i) {
            part = BloodBodyPartType.FromIndex(i);
            bodyPartType = BodyPartType.FromIndex(i);
            if (bodyPartType == BodyPartType.MAX) continue;
            bodyPart = this.character.getBodyDamage().getBodyPart(bodyPartType);
            thermalNode = this.character.getBodyDamage().getThermoregulator().getNodeForBloodType(part);
            if (bodyPart == null || thermalNode == null) continue;
            float maxWetness = 100.0f;
            if (umbrella) {
                maxWetness = 100.0f * BodyPartType.GetUmbrellaMod(bodyPartType);
            }
            float baseDelta = 0.0f;
            baseDelta = outerWetnessInc > 0.0f ? outerWetnessInc * baseIncrease : (baseDelta -= outerWetnessDec * baseDecrease);
            boolean holeAbove = false;
            soakThrough = false;
            dryBefore = false;
            float layerDivider = 2.0f;
            for (int j = 0; j < this.clothingList[i].size(); ++j) {
                int partCount = 1 + this.clothingList[i].size() - j;
                increaseMod = 1.0f;
                InventoryItem item = (InventoryItem)this.clothingList[i].get(j);
                if (!(item instanceof Clothing)) continue;
                Clothing clothing = (Clothing)item;
                ItemVisual itemVisual = clothing.getVisual();
                if (itemVisual != null) {
                    if (itemVisual.getHole(part) > 0.0f) {
                        holeAbove = true;
                        continue;
                    }
                    if (baseDelta > 0.0f && clothing.getWetness() >= 100.0f) {
                        soakThrough = true;
                        continue;
                    }
                    if (baseDelta < 0.0f && clothing.getWetness() <= 0.0f) {
                        dryBefore = true;
                        continue;
                    }
                    if (baseDelta > 0.0f && clothing.getWaterResistance() > 0.0f && (increaseMod = PZMath.max(0.0f, 1.0f - clothing.getWaterResistance())) <= 0.0f) break;
                }
                coveredParts.clear();
                BloodClothingType.getCoveredParts(item.getBloodClothingType(), coveredParts);
                int numParts = coveredParts.size();
                float delta = baseDelta;
                if (delta > 0.0f) {
                    delta *= increaseMod;
                }
                delta /= (float)numParts;
                if (holeAbove || soakThrough || dryBefore) {
                    delta /= layerDivider;
                }
                if (baseDelta < 0.0f && partCount > this.perspiringParts[i] || baseDelta > 0.0f && clothing.getWetness() <= maxWetness) {
                    clothing.setWetness(clothing.getWetness() + delta);
                }
                if (baseDelta > 0.0f) break;
                if (!dryBefore) continue;
                layerDivider *= 2.0f;
            }
            if (this.clothingList[i].isEmpty()) {
                if (!(baseDelta < 0.0f && this.perspiringParts[i] == 0) && !(bodyPart.getWetness() <= maxWetness)) continue;
                bodyPart.setWetness(bodyPart.getWetness() + baseDelta);
                continue;
            }
            InventoryItem bottomItem = (InventoryItem)this.clothingList[i].get(this.clothingList[i].size() - 1);
            if (!(bottomItem instanceof Clothing)) continue;
            Clothing clothing = (Clothing)bottomItem;
            if (baseDelta > 0.0f && this.perspiringParts[i] == 0 && clothing.getWetness() >= 50.0f && bodyPart.getWetness() <= maxWetness) {
                bodyPart.setWetness(bodyPart.getWetness() + baseDelta / 2.0f);
            }
            if (!(baseDelta < 0.0f) || this.perspiringParts[i] != 0 || !(clothing.getWetness() <= 50.0f)) continue;
            bodyPart.setWetness(bodyPart.getWetness() + baseDelta / 2.0f);
        }
    }

    private static final class ItemList
    extends ArrayList<InventoryItem> {
        private ItemList() {
        }
    }
}


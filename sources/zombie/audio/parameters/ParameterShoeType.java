/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;

public final class ParameterShoeType
extends FMODLocalParameter {
    private static final ItemVisuals tempItemVisuals = new ItemVisuals();
    private final IsoGameCharacter character;
    private ShoeType shoeType;

    public ParameterShoeType(IsoGameCharacter character) {
        super("ShoeType");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        if (this.shoeType == null) {
            this.shoeType = this.getShoeType();
        }
        return this.shoeType.label;
    }

    private ShoeType getShoeType() {
        this.character.getItemVisuals(tempItemVisuals);
        Item shoes = null;
        for (int i = 0; i < tempItemVisuals.size(); ++i) {
            ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem == null || !scriptItem.isBodyLocation(ItemBodyLocation.SHOES)) continue;
            shoes = scriptItem;
            break;
        }
        if (shoes == null) {
            return ShoeType.Barefoot;
        }
        String type = shoes.getName();
        if (type.contains("Boots") || type.contains("Wellies")) {
            return ShoeType.Boots;
        }
        if (type.contains("FlipFlop")) {
            return ShoeType.FlipFlops;
        }
        if (type.contains("Slippers")) {
            return ShoeType.Slippers;
        }
        if (type.contains("Trainer")) {
            return ShoeType.Sneakers;
        }
        return ShoeType.Shoes;
    }

    public void setShoeType(ShoeType shoeType) {
        this.shoeType = shoeType;
    }

    private static enum ShoeType {
        Barefoot(0),
        Boots(1),
        FlipFlops(2),
        Shoes(3),
        Slippers(4),
        Sneakers(5);

        final int label;

        private ShoeType(int label) {
            this.label = label;
        }
    }
}


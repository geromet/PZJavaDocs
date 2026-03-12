/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoBrokenGlass;
import zombie.iso.sprite.IsoSprite;

public final class ParameterFootstepMaterial2
extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterFootstepMaterial2(IsoGameCharacter character) {
        super("FootstepMaterial2");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.getMaterial().label;
    }

    private FootstepMaterial2 getMaterial() {
        IsoGridSquare square = this.character.getCurrentSquare();
        if (square == null) {
            return FootstepMaterial2.None;
        }
        IsoBrokenGlass brokenGlass = square.getBrokenGlass();
        if (brokenGlass != null) {
            return FootstepMaterial2.BrokenGlass;
        }
        for (int i = 0; i < square.getObjects().size(); ++i) {
            IsoObject object = square.getObjects().get(i);
            IsoSprite sprite = object.getSprite();
            if (sprite == null || !"d_trash_1".equals(sprite.tilesetName) && !"trash_01".equals(sprite.tilesetName)) continue;
            return FootstepMaterial2.Garbage;
        }
        float puddles = square.getPuddlesInGround();
        if (puddles > 0.5f) {
            return FootstepMaterial2.PuddleDeep;
        }
        if (puddles > 0.1f) {
            return FootstepMaterial2.PuddleShallow;
        }
        return FootstepMaterial2.None;
    }

    static enum FootstepMaterial2 {
        None(0),
        BrokenGlass(1),
        PuddleShallow(2),
        PuddleDeep(3),
        Garbage(4);

        final int label;

        private FootstepMaterial2(int label) {
            this.label = label;
        }
    }
}


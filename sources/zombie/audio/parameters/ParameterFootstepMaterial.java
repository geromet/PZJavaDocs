/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import fmod.fmod.FMODManager;
import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.properties.IsoPropertyType;
import zombie.core.properties.PropertyContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.scripting.objects.CharacterTrait;
import zombie.util.list.PZArrayList;

public final class ParameterFootstepMaterial
extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterFootstepMaterial(IsoGameCharacter character) {
        super("FootstepMaterial");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.getMaterial().label;
    }

    private FootstepMaterial getMaterial() {
        if (FMODManager.instance.getNumListeners() == 1) {
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player == null || player == this.character || player.hasTrait(CharacterTrait.DEAF)) continue;
                if (PZMath.fastfloor(player.getZ()) >= PZMath.fastfloor(this.character.getZ())) break;
                return FootstepMaterial.Upstairs;
            }
        }
        IsoObject staircase = null;
        IsoObject withMaterial = null;
        IsoGridSquare square = this.character.getCurrentSquare();
        if (square != null) {
            if (IsoWorld.instance.currentCell.gridSquareIsSnow(square.x, square.y, square.z)) {
                return FootstepMaterial.Snow;
            }
            PZArrayList<IsoObject> objects = square.getObjects();
            for (int i = 0; i < objects.size(); ++i) {
                PropertyContainer props;
                IsoObject object = objects.get(i);
                if (object instanceof IsoWorldInventoryObject || (props = object.getProperties()) == null) continue;
                if (object.isStairsObject()) {
                    staircase = object;
                }
                if (!props.has(IsoPropertyType.FOOTSTEP_MATERIAL)) continue;
                withMaterial = object;
            }
        }
        if (withMaterial != null) {
            try {
                String material = withMaterial.getProperties().get(IsoPropertyType.FOOTSTEP_MATERIAL);
                return FootstepMaterial.valueOf(material);
            }
            catch (IllegalArgumentException ex) {
                boolean bl = true;
            }
        }
        if (staircase != null) {
            return FootstepMaterial.Wood;
        }
        return FootstepMaterial.Concrete;
    }

    static enum FootstepMaterial {
        Upstairs(0),
        BrokenGlass(1),
        Concrete(2),
        Grass(3),
        Gravel(4),
        Puddle(5),
        Snow(6),
        Wood(7),
        Carpet(8),
        Dirt(9),
        Sand(10),
        Ceramic(11),
        Metal(12);

        final int label;

        private FootstepMaterial(int label) {
            this.label = label;
        }
    }
}


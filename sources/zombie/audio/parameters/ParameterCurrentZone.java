/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;

public final class ParameterCurrentZone
extends FMODLocalParameter {
    private final IsoObject object;
    private zombie.iso.zones.Zone metaZone;
    private Zone zone = Zone.None;

    public ParameterCurrentZone(IsoObject object) {
        super("CurrentZone");
        this.object = object;
    }

    @Override
    public float calculateCurrentValue() {
        IsoGridSquare square = this.object.getSquare();
        if (square == null) {
            this.zone = Zone.None;
            return this.zone.label;
        }
        if (square.zone == this.metaZone) {
            return this.zone.label;
        }
        this.metaZone = square.zone;
        if (this.metaZone == null || this.metaZone.type == null) {
            this.zone = Zone.None;
            return this.zone.label;
        }
        this.zone = switch (this.metaZone.type) {
            case "DeepForest" -> Zone.DeepForest;
            case "Farm" -> Zone.Farm;
            case "Forest" -> Zone.Forest;
            case "Nav" -> Zone.Nav;
            case "TownZone" -> Zone.Town;
            case "TrailerPark" -> Zone.TrailerPark;
            case "Vegitation" -> Zone.Vegetation;
            default -> this.metaZone.type.endsWith("Forest") ? Zone.Forest : Zone.None;
        };
        return this.zone.label;
    }

    static enum Zone {
        None(0),
        DeepForest(1),
        Farm(2),
        Forest(3),
        Nav(4),
        Town(5),
        TrailerPark(6),
        Vegetation(7);

        final int label;

        private Zone(int label) {
            this.label = label;
        }
    }
}


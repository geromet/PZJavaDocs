/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.vehicles.BaseVehicle;

public final class ParameterVehicleRoadMaterial
extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleRoadMaterial(BaseVehicle vehicle) {
        super("VehicleRoadMaterial");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        if (!this.vehicle.isEngineRunning()) {
            return Float.isNaN(this.getCurrentValue()) ? 0.0f : this.getCurrentValue();
        }
        return this.getMaterial().label;
    }

    private Material getMaterial() {
        IsoGridSquare square = this.vehicle.getCurrentSquare();
        if (square == null) {
            return Material.Concrete;
        }
        if (IsoWorld.instance.currentCell.gridSquareIsSnow(square.x, square.y, square.z)) {
            return Material.Snow;
        }
        IsoObject floor = null;
        for (int n = 0; n < square.getObjects().size(); ++n) {
            IsoSprite sprite;
            IsoObject obj = square.getObjects().get(n);
            IsoSprite isoSprite = sprite = obj == null ? null : obj.getSprite();
            if (sprite == null) continue;
            if (floor == null && sprite.getProperties().has(IsoFlagType.solidfloor)) {
                floor = obj;
            }
            if (sprite.getName() == null || !sprite.getName().startsWith("industry_railroad_")) continue;
            return Material.Railroad;
        }
        if (floor == null || floor.getSprite() == null || floor.getSprite().getName() == null) {
            return Material.Concrete;
        }
        String floorName = floor.getSprite().getName();
        if (floorName.endsWith("blends_natural_01_5") || floorName.endsWith("blends_natural_01_6") || floorName.endsWith("blends_natural_01_7") || floorName.endsWith("blends_natural_01_0")) {
            return Material.Sand;
        }
        if (floorName.endsWith("blends_natural_01_64") || floorName.endsWith("blends_natural_01_69") || floorName.endsWith("blends_natural_01_70") || floorName.endsWith("blends_natural_01_71")) {
            return Material.Dirt;
        }
        if (floorName.startsWith("blends_natural_01")) {
            return Material.Grass;
        }
        if (floorName.endsWith("blends_street_01_48") || floorName.endsWith("blends_street_01_53") || floorName.endsWith("blends_street_01_54") || floorName.endsWith("blends_street_01_55")) {
            return Material.Gravel;
        }
        if (floorName.startsWith("floors_interior_tilesandwood_01_")) {
            int index = Integer.parseInt(floorName.replaceFirst("floors_interior_tilesandwood_01_", ""));
            if (index > 40 && index < 48) {
                return Material.Wood;
            }
            return Material.Concrete;
        }
        if (floorName.startsWith("carpentry_02_")) {
            return Material.Wood;
        }
        if (floorName.contains("interior_carpet_")) {
            return Material.Carpet;
        }
        float puddles = square.getPuddlesInGround();
        if ((double)puddles > 0.1) {
            return Material.Puddle;
        }
        return Material.Concrete;
    }

    static enum Material {
        Concrete(0),
        Grass(1),
        Gravel(2),
        Puddle(3),
        Snow(4),
        Wood(5),
        Carpet(6),
        Dirt(7),
        Sand(8),
        Railroad(9);

        final int label;

        private Material(int label) {
            this.label = label;
        }
    }
}


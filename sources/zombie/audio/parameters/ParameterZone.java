/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import java.util.ArrayList;
import zombie.audio.FMODGlobalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;

public final class ParameterZone
extends FMODGlobalParameter {
    private final String zoneName;
    private final ArrayList<Zone> zones = new ArrayList();

    public ParameterZone(String name, String zoneName) {
        super(name);
        this.zoneName = zoneName;
    }

    @Override
    public float calculateCurrentValue() {
        IsoGameCharacter player = FMODParameterUtils.getFirstListener();
        if (player == null) {
            return 40.0f;
        }
        boolean z = false;
        this.zones.clear();
        IsoWorld.instance.metaGrid.getZonesIntersecting(PZMath.fastfloor(player.getX()) - 40, PZMath.fastfloor(player.getY()) - 40, 0, 80, 80, this.zones);
        float closestDistSq = Float.MAX_VALUE;
        for (int i = 0; i < this.zones.size(); ++i) {
            boolean bForestZone;
            Zone zone = this.zones.get(i);
            boolean bl = bForestZone = "Forest".equalsIgnoreCase(this.zoneName) && !"DeepForest".equalsIgnoreCase(zone.getType()) && zone.getType().endsWith("Forest");
            if (!bForestZone && !this.zoneName.equalsIgnoreCase(zone.getType())) continue;
            if (zone.contains(PZMath.fastfloor(player.getX()), PZMath.fastfloor(player.getY()), 0)) {
                return 0.0f;
            }
            float centerX = (float)zone.x + (float)zone.w / 2.0f;
            float centerY = (float)zone.y + (float)zone.h / 2.0f;
            float dx = PZMath.max(PZMath.abs(player.getX() - centerX) - (float)zone.w / 2.0f, 0.0f);
            float dy = PZMath.max(PZMath.abs(player.getY() - centerY) - (float)zone.h / 2.0f, 0.0f);
            closestDistSq = PZMath.min(closestDistSq, dx * dx + dy * dy);
        }
        return (int)PZMath.clamp(PZMath.sqrt(closestDistSq), 0.0f, 40.0f);
    }
}


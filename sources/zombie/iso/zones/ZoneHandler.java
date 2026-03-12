/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.zones;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import zombie.debug.DebugType;
import zombie.iso.zones.Zone;

public class ZoneHandler<U extends Zone> {
    private final HashMap<UUID, U> zones = new HashMap();

    public void Dispose() {
        this.zones.clear();
    }

    public void addZone(U zone) {
        DebugType.Zone.debugln(zone);
        this.zones.put(((Zone)zone).id, zone);
    }

    public U getZone(UUID id) {
        return (U)((Zone)this.zones.get(id));
    }

    public Collection<U> getZones() {
        return this.zones.values();
    }
}


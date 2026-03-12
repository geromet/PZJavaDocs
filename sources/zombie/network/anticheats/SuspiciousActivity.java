/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import java.util.EnumMap;
import java.util.Map;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.network.anticheats.AntiCheat;

public class SuspiciousActivity {
    private final EnumMap<AntiCheat, Integer> counters = new EnumMap(AntiCheat.class);
    private final UpdateLimit ulDecreaseInterval = new UpdateLimit(150000L);
    protected final UdpConnection connection;

    public SuspiciousActivity(UdpConnection connection) {
        this.connection = connection;
    }

    public void update() {
        boolean doDecrease = this.ulDecreaseInterval.Check();
        for (Map.Entry<AntiCheat, Integer> c : this.counters.entrySet()) {
            if (!doDecrease) continue;
            if (c.getValue() > 0) {
                c.setValue(c.getValue() - 1);
                AntiCheat.log(this.connection, c.getKey(), c.getValue(), null);
                continue;
            }
            c.setValue(0);
        }
    }

    public int report(AntiCheat antiCheat) {
        if (AntiCheat.None == antiCheat) {
            return 0;
        }
        int counter = this.counters.getOrDefault((Object)antiCheat, 0) + 1;
        this.counters.put(antiCheat, counter);
        return counter;
    }

    public EnumMap<AntiCheat, Integer> getCounters() {
        return this.counters;
    }

    public void resetCounters() {
        for (Map.Entry<AntiCheat, Integer> entry : this.counters.entrySet()) {
            entry.setValue(0);
        }
    }
}


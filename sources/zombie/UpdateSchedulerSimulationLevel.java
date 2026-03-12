/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.util.Comparator;

public enum UpdateSchedulerSimulationLevel {
    SIXTEENTH,
    EIGHTH,
    HALF,
    QUARTER,
    FULL;

    private static final UpdateSchedulerSimulationLevel[] VALUES;
    private static final Comparator<UpdateSchedulerSimulationLevel> COMPARATOR;

    public UpdateSchedulerSimulationLevel less() {
        return VALUES[Math.max(this.ordinal() - 1, 0)];
    }

    public UpdateSchedulerSimulationLevel more() {
        return VALUES[Math.min(this.ordinal() + 1, VALUES.length - 1)];
    }

    public UpdateSchedulerSimulationLevel max(UpdateSchedulerSimulationLevel rhs) {
        return COMPARATOR.compare(this, rhs) > 0 ? this : rhs;
    }

    static {
        VALUES = UpdateSchedulerSimulationLevel.values();
        COMPARATOR = Comparator.comparingInt(Enum::ordinal);
    }
}


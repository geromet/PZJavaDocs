/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.zombie;

import zombie.iso.worldgen.zombie.ClosestSelection;
import zombie.iso.worldgen.zombie.ClosestSelectionType;

public record ZombieVoronoiEntry(int numberPoints, ClosestSelection closestPoint, double scale, double cutoff) {
    public ZombieVoronoiEntry(int numberPoints, String closestPoint, double scale, double cutoff) {
        this(numberPoints, ClosestSelectionType.valueOf(closestPoint), scale, cutoff);
    }
}


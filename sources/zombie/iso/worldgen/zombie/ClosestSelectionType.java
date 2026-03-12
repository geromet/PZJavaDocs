/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.zombie;

import java.util.List;
import zombie.iso.worldgen.zombie.ClosestSelection;
import zombie.iso.worldgen.zombie.Coord;

public enum ClosestSelectionType implements ClosestSelection
{
    FIRST{

        @Override
        public double getClosest(double x, double y, List<Coord> points) {
            return points.stream().map(c -> (x - c.x()) * (x - c.x()) + (y - c.y()) * (y - c.y())).sorted().toList().get(0);
        }
    }
    ,
    SECOND{

        @Override
        public double getClosest(double x, double y, List<Coord> points) {
            return points.stream().map(c -> (x - c.x()) * (x - c.x()) + (y - c.y()) * (y - c.y())).sorted().toList().get(1);
        }
    }
    ,
    SECOND_MINUS_FIRST{

        @Override
        public double getClosest(double x, double y, List<Coord> points) {
            List<Double> l = points.stream().map(c -> (x - c.x()) * (x - c.x()) + (y - c.y()) * (y - c.y())).sorted().toList();
            return l.get(1) - l.get(0);
        }
    };

}


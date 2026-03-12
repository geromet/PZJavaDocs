/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.roads;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joml.Vector2i;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.roads.Road;

public class RoadEdge {
    public final Vector2i a;
    public final Vector2i b;
    public final Vector2i subnexus;
    public final List<Road> roads;

    public RoadEdge(Vector2i a, Vector2i b, List<TileGroup> tiles, double probability) {
        boolean test = a.lengthSquared() > b.lengthSquared();
        this.a = test ? a : b;
        this.b = test ? b : a;
        this.subnexus = new Vector2i(this.a.x, this.b.y);
        this.roads = new ArrayList<Road>();
        this.roads.add(new Road(this.a, this.subnexus, tiles, probability));
        this.roads.add(new Road(this.subnexus, this.b, tiles, probability));
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        RoadEdge roadEdge = (RoadEdge)o;
        return Objects.equals(this.a, roadEdge.a) && Objects.equals(this.b, roadEdge.b);
    }

    public int hashCode() {
        return Objects.hash(this.a, this.b);
    }
}


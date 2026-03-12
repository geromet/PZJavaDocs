/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.roads;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector2i;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.roads.RoadEdge;

public class RoadNexus {
    private final Vector2i delaunayPoint;
    private final List<Vector2i> delaunayRemotes;
    private final List<RoadEdge> roadEdges;

    public RoadNexus(Vector2i delaunayPoint, List<Vector2i> delaunayRemotes, List<TileGroup> tileGroups, double probability) {
        this.delaunayPoint = delaunayPoint;
        this.delaunayRemotes = delaunayRemotes;
        this.roadEdges = new ArrayList<RoadEdge>();
        for (Vector2i remote : delaunayRemotes) {
            this.roadEdges.add(new RoadEdge(this.delaunayPoint, remote, tileGroups, probability));
        }
    }

    public Vector2i getDelaunayPoint() {
        return this.delaunayPoint;
    }

    public List<Vector2i> getDelaunayRemotes() {
        return this.delaunayRemotes;
    }

    public List<RoadEdge> getRoadEdges() {
        return this.roadEdges;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen;

import java.util.List;
import org.joml.Vector2i;
import zombie.core.math.PZMath;
import zombie.iso.IsoWorld;
import zombie.iso.worldgen.roads.RoadEdge;
import zombie.iso.worldgen.roads.RoadGenerator;
import zombie.iso.worldgen.roads.RoadNexus;
import zombie.worldMap.UIWorldMap;

public class WorldGenDebug {
    private static final WorldGenDebug INSTANCE = new WorldGenDebug();

    private WorldGenDebug() {
    }

    public static WorldGenDebug getInstance() {
        return INSTANCE;
    }

    public void renderRoads(UIWorldMap ui) {
        for (int nGen = 0; nGen < IsoWorld.instance.getWgChunk().getRoadGenerators().size(); ++nGen) {
            RoadGenerator generator = IsoWorld.instance.getWgChunk().getRoadGenerators().get(nGen);
            for (RoadNexus nexus : generator.getRoadNexus()) {
                float uiY2;
                float uiX2;
                float uiY1;
                float uiX1;
                Vector2i origin = nexus.getDelaunayPoint();
                List<Vector2i> remotes = nexus.getDelaunayRemotes();
                List<RoadEdge> edges = nexus.getRoadEdges();
                double r = 0.1 * (double)nGen;
                double g = 1.0;
                double b = 0.0;
                double a = 1.0;
                float uiX = PZMath.floor(ui.getAPI().worldToUIX(origin.x, origin.y));
                float uiY = PZMath.floor(ui.getAPI().worldToUIY(origin.x, origin.y));
                ui.DrawTextureScaledColor(null, (double)uiX - 3.0, (double)uiY - 3.0, 6.0, 6.0, r, 1.0, 0.0, 1.0);
                for (Vector2i remote : remotes) {
                    uiX1 = PZMath.floor(ui.getAPI().worldToUIX(remote.x, remote.y));
                    uiY1 = PZMath.floor(ui.getAPI().worldToUIY(remote.x, remote.y));
                    uiX2 = PZMath.floor(ui.getAPI().worldToUIX(origin.x, origin.y));
                    uiY2 = PZMath.floor(ui.getAPI().worldToUIY(origin.x, origin.y));
                    ui.DrawLine(null, uiX1, uiY1, uiX2, uiY2, 0.5f, r, 1.0, 0.0, 1.0);
                }
                for (RoadEdge edge : edges) {
                    float uiX3 = PZMath.floor(ui.getAPI().worldToUIX(edge.subnexus.x, edge.subnexus.y));
                    float uiY3 = PZMath.floor(ui.getAPI().worldToUIY(edge.subnexus.x, edge.subnexus.y));
                    double r2 = 0.1 * (double)nGen;
                    double g2 = 0.0;
                    double b2 = 1.0;
                    double a2 = 1.0;
                    ui.DrawTextureScaledColor(null, (double)uiX3 - 3.0, (double)uiY3 - 3.0, 6.0, 6.0, r2, 0.0, 1.0, 1.0);
                    uiX1 = PZMath.floor(ui.getAPI().worldToUIX(edge.a.x, edge.a.y));
                    uiY1 = PZMath.floor(ui.getAPI().worldToUIY(edge.a.x, edge.a.y));
                    uiX2 = PZMath.floor(ui.getAPI().worldToUIX(edge.subnexus.x, edge.subnexus.y));
                    uiY2 = PZMath.floor(ui.getAPI().worldToUIY(edge.subnexus.x, edge.subnexus.y));
                    float uiX32 = PZMath.floor(ui.getAPI().worldToUIX(edge.b.x, edge.b.y));
                    float uiY32 = PZMath.floor(ui.getAPI().worldToUIY(edge.b.x, edge.b.y));
                    ui.DrawLine(null, uiX1, uiY1, uiX2, uiY2, 0.5f, r2, 0.0, 1.0, 1.0);
                    ui.DrawLine(null, uiX2, uiY2, uiX32, uiY32, 0.5f, r2, 0.0, 1.0, 1.0);
                }
            }
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals.pathfind;

import java.util.ArrayList;
import java.util.HashMap;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.characters.animals.AnimalZone;
import zombie.characters.animals.pathfind.IPathRenderer;
import zombie.characters.animals.pathfind.LowLevelAStar;
import zombie.characters.animals.pathfind.Mesh;
import zombie.characters.animals.pathfind.MeshList;
import zombie.characters.animals.pathfind.MeshWanderer;
import zombie.core.SpriteRenderer;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;
import zombie.popman.ObjectPool;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.UIWorldMapV1;
import zombie.worldMap.WorldMapRenderer;

public class AnimalPathfind
implements IPathRenderer {
    private static AnimalPathfind instance;
    final ObjectPool<Vector2f> vector2fObjectPool = new ObjectPool<Vector2f>(Vector2f::new);
    final ObjectPool<Vector3f> vector3fObjectPool = new ObjectPool<Vector3f>(Vector3f::new);
    final MeshList meshList = new MeshList();
    final LowLevelAStar cdAStar = new LowLevelAStar(this.meshList);
    UIWorldMap uiWorldMap;
    UIWorldMapV1 uiWorldMapV1;
    final HashMap<Mesh, Zone> meshZoneHashMap = new HashMap();
    final HashMap<Zone, Mesh> zoneMeshHashMap = new HashMap();
    MeshWanderer meshWanderer = new MeshWanderer();

    public static AnimalPathfind getInstance() {
        if (instance == null) {
            instance = new AnimalPathfind();
        }
        return instance;
    }

    public void renderPath(UIWorldMap ui, Zone zone, float x1, float y1, float x2, float y2) {
        this.uiWorldMap = ui;
        this.uiWorldMapV1 = ui.getAPIv1();
        this.cdAStar.renderer = this;
        this.meshList.meshes.clear();
        this.createMeshesFromZonesInArea((int)x1 - 300, (int)y1 - 300, 600, 600);
        for (Mesh mesh : this.meshList.meshes) {
            this.cdAStar.initOffMeshConnections(mesh);
        }
        boolean z1 = false;
        Mesh mesh1 = this.meshList.getMeshAt(x1, y1, 0);
        if (mesh1 == null) {
            return;
        }
        this.meshList.meshes.clear();
        mesh1.gatherConnectedMeshes(this.meshList.meshes);
    }

    private void createMeshesFromZonesInArea(int x, int y, int w, int h) {
        int i;
        int cellX = (x + 300) / 300;
        int cellY = (y + 300) / 300;
        IsoMetaCell metaCell = IsoWorld.instance.metaGrid.getCellData(cellX, cellY);
        if (metaCell == null) {
            return;
        }
        ArrayList<AnimalZone> zones = new ArrayList<AnimalZone>();
        for (i = 0; i < metaCell.getAnimalZonesSize(); ++i) {
            zones.add(metaCell.getAnimalZone(i));
        }
        for (i = 0; i < zones.size(); ++i) {
            Zone zone1 = (Zone)zones.get(i);
            Mesh mesh = this.zoneMeshHashMap.get(zone1);
            if (mesh != null) {
                this.meshList.meshes.add(mesh);
                continue;
            }
            if (zone1.isRectangle()) {
                // empty if block
            }
            if (zone1.getPolygonTriangles() == null) continue;
            mesh = new Mesh();
            mesh.meshList = this.meshList;
            mesh.initFromZone(zone1);
            this.meshList.meshes.add(mesh);
            this.meshZoneHashMap.put(mesh, zone1);
            this.zoneMeshHashMap.put(zone1, mesh);
        }
    }

    @Override
    public void drawTriangleCentroid(Mesh mesh, int tri, float r, float g, float b, float a) {
        Vector2f t1 = this.meshWanderer.mesh.triangles.get(tri);
        Vector2f t2 = this.meshWanderer.mesh.triangles.get(tri + 1);
        Vector2f t3 = this.meshWanderer.mesh.triangles.get(tri + 2);
        float cx = (t1.x + t2.x + t3.x) / 3.0f;
        float cy = (t1.y + t2.y + t3.y) / 3.0f;
        this.drawRect(cx - 1.0f, cy - 1.0f, 2.0f, 2.0f, r, g, b, a);
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        WorldMapRenderer rr = this.uiWorldMapV1.getRenderer();
        int uiX1 = (int)rr.worldToUIX(x1, y1, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        int uiY1 = (int)rr.worldToUIY(x1, y1, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        int uiX2 = (int)rr.worldToUIX(x2, y2, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        int uiY2 = (int)rr.worldToUIY(x2, y2, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        SpriteRenderer.instance.renderline(null, uiX1, uiY1, uiX2, uiY2, r, g, b, a, 1.0f);
    }

    @Override
    public void drawRect(float x1, float y1, float w, float h, float r, float g, float b, float a) {
        this.drawLine(x1, y1, x1 + w, y1, r, g, b, a);
        this.drawLine(x1 + w, y1, x1 + w, y1 + h, r, g, b, a);
        this.drawLine(x1, y1 + h, x1 + w, y1 + h, r, g, b, a);
        this.drawLine(x1, y1, x1, y1 + h, r, g, b, a);
    }
}


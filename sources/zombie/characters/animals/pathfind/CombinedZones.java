/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals.pathfind;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import org.joml.Vector2f;
import zombie.characters.animals.AnimalZone;
import zombie.characters.animals.VirtualAnimal;
import zombie.characters.animals.VirtualAnimalState;
import zombie.characters.animals.pathfind.AnimalPathfind;
import zombie.characters.animals.pathfind.IPathRenderer;
import zombie.characters.animals.pathfind.LowLevelAStar;
import zombie.characters.animals.pathfind.Mesh;
import zombie.characters.animals.pathfind.MeshList;
import zombie.core.math.PZMath;
import zombie.iso.zones.Zone;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.Clipper;
import zombie.worldMap.UIWorldMap;

public final class CombinedZones {
    private static Clipper clipper;
    private static ByteBuffer clipperBuffer;
    static final HashMap<Zone, CombinedZones> combinedZonesMap;
    final HashMap<Zone, MeshList> zoneMeshListMap = new HashMap();
    public final MeshList meshList = new MeshList();
    public final MeshList combinedMeshList = new MeshList();
    final LowLevelAStar cdAStar = new LowLevelAStar(null);
    final VirtualAnimal virtualAnimal = new VirtualAnimal();

    public void init(MeshList meshList) {
        for (Mesh mesh1 : meshList.meshes) {
            Mesh mesh2 = new Mesh();
            mesh2.initFrom(mesh1);
            mesh2.meshList = new MeshList();
            mesh2.meshList.meshes.add(mesh2);
            this.zoneMeshListMap.put(mesh1.zone, mesh2.meshList);
            this.meshList.meshes.add(mesh2);
        }
        this.cdAStar.renderer = AnimalPathfind.getInstance();
        if (clipper == null) {
            clipper = new Clipper();
        }
        clipper.clear();
        for (int i = 0; i < meshList.size(); ++i) {
            Mesh mesh = meshList.get(i);
            if (clipperBuffer == null || clipperBuffer.capacity() < mesh.polygon.size() * 8 * 4) {
                clipperBuffer = ByteBuffer.allocateDirect(mesh.polygon.size() * 8 * 4);
            }
            clipperBuffer.clear();
            if (this.isClockwise(mesh.polygon)) {
                for (j = mesh.polygon.size() - 1; j >= 0; --j) {
                    p = mesh.polygon.get(j);
                    clipperBuffer.putFloat(p.x);
                    clipperBuffer.putFloat(p.y);
                }
            } else {
                for (j = 0; j < mesh.polygon.size(); ++j) {
                    p = mesh.polygon.get(j);
                    clipperBuffer.putFloat(p.x);
                    clipperBuffer.putFloat(p.y);
                }
            }
            clipper.addPath(mesh.polygon.size(), clipperBuffer, false);
        }
        int numPolys = clipper.generatePolygons();
        if (numPolys <= 0) {
            return;
        }
        for (int i = 0; i < numPolys; ++i) {
            float y;
            float x;
            int j;
            clipperBuffer.clear();
            clipper.getPolygon(i, clipperBuffer);
            int numPoints = clipperBuffer.getShort();
            if (numPoints < 3) continue;
            Mesh mesh = new Mesh();
            mesh.meshList = this.combinedMeshList;
            for (j = 0; j < numPoints; ++j) {
                x = clipperBuffer.getFloat();
                y = clipperBuffer.getFloat();
                mesh.polygon.add(new Vector2f(x, y));
            }
            clipperBuffer.clear();
            if (clipperBuffer.capacity() < numPoints * 8 * 4) {
                clipperBuffer = ByteBuffer.allocateDirect(numPoints * 8 * 4);
            }
            numPoints = clipper.triangulate(i, clipperBuffer);
            for (j = 0; j < numPoints; ++j) {
                x = clipperBuffer.getFloat();
                y = clipperBuffer.getFloat();
                mesh.triangles.add(new Vector2f(x, y));
            }
            mesh.initEdges();
            mesh.initAdjacentTriangles();
            this.combinedMeshList.meshes.add(mesh);
        }
        Mesh mesh = this.meshList.meshes.get(0);
        AnimalZone animalZone = (AnimalZone)mesh.zone;
        if ("Eat".equals(animalZone.getAction())) {
            this.virtualAnimal.setState(new StateEat(this.virtualAnimal));
        }
        if ("Follow".equals(animalZone.getAction())) {
            this.virtualAnimal.setState(new StateFollow(this.virtualAnimal));
        }
        if ("Sleep".equals(animalZone.getAction())) {
            this.virtualAnimal.setState(new StateSleep(this.virtualAnimal));
        }
        BaseState state1 = (BaseState)this.virtualAnimal.getState();
        state1.combinedZones = this;
        state1.mesh = mesh;
        Vector2f pos = state1.mesh.pickRandomPoint(new Vector2f());
        this.virtualAnimal.setX(pos.x);
        this.virtualAnimal.setY(pos.y);
    }

    boolean isClockwise(ArrayList<Vector2f> points) {
        float sum = 0.0f;
        for (int i = 0; i < points.size(); ++i) {
            float p1x = points.get((int)i).x;
            float p1y = points.get((int)i).y;
            float p2x = points.get((int)((i + 1) % points.size())).x;
            float p2y = points.get((int)((i + 1) % points.size())).y;
            sum += (p2x - p1x) * (p2y + p1y);
        }
        return (double)sum > 0.0;
    }

    float getLength(float[] points) {
        float length = 0.0f;
        for (int i = 0; i < points.length; i += 2) {
            float x1 = points[i];
            float y1 = points[i + 1];
            float x2 = points[(i + 2) % points.length];
            float y2 = points[(i + 3) % points.length];
            length += Vector2f.length(x2 - x1, y2 - y1);
        }
        return length;
    }

    public static void renderPath(UIWorldMap ui, Zone zone, float x1, float y1, float x2, float y2) {
        AnimalPathfind animalPathfind = AnimalPathfind.getInstance();
        if (animalPathfind.meshList.meshes.isEmpty()) {
            return;
        }
        CombinedZones combinedZones = combinedZonesMap.get(zone);
        if (combinedZones == null) {
            combinedZones = new CombinedZones();
            combinedZones.init(animalPathfind.meshList);
            for (Mesh mesh : AnimalPathfind.getInstance().meshList.meshes) {
                combinedZonesMap.put(mesh.zone, combinedZones);
            }
        }
        combinedZones.render(animalPathfind, x1, y1, x2, y2);
        float uiX = ui.getAPI().worldToUIX(combinedZones.virtualAnimal.getX(), combinedZones.virtualAnimal.getY());
        float uiY = ui.getAPI().worldToUIY(combinedZones.virtualAnimal.getX(), combinedZones.virtualAnimal.getY());
        uiX = PZMath.floor(uiX);
        uiY = PZMath.floor(uiY);
        BaseState state1 = (BaseState)combinedZones.virtualAnimal.getState();
        ui.DrawTextCentre(state1.getClass().getSimpleName() + " / " + ((AnimalZone)state1.mesh.zone).getAction(), uiX, (double)uiY + 4.0, 0.0, 0.0, 0.0, 1.0);
    }

    public void render(IPathRenderer renderer, float x1, float y1, float x2, float y2) {
        for (Mesh mesh : this.combinedMeshList.meshes) {
            mesh.renderTriangles(renderer, 1.0f, 1.0f, 1.0f, 1.0f);
            for (int i = 0; i < mesh.edgesOnBoundaries.size(); ++i) {
                short triIdx = mesh.trianglesOnBoundaries.get(i);
                short edges = mesh.edgesOnBoundaries.get(i);
                float meshX1 = mesh.triangles.get(triIdx).x();
                float meshY1 = mesh.triangles.get(triIdx).y();
                float meshX2 = mesh.triangles.get(triIdx + 1).x();
                float meshY2 = mesh.triangles.get(triIdx + 1).y();
                float meshX3 = mesh.triangles.get(triIdx + 2).x();
                float meshY3 = mesh.triangles.get(triIdx + 2).y();
                if ((edges & 1) != 0) {
                    renderer.drawLine(meshX1, meshY1, meshX2, meshY2, 0.0f, 0.0f, 1.0f, 1.0f);
                }
                if ((edges & 2) != 0) {
                    renderer.drawLine(meshX2, meshY2, meshX3, meshY3, 0.0f, 0.0f, 1.0f, 1.0f);
                }
                if ((edges & 4) == 0) continue;
                renderer.drawLine(meshX1, meshY1, meshX3, meshY3, 0.0f, 0.0f, 1.0f, 1.0f);
            }
            mesh.renderOffMeshConnections(renderer, 1.0f, 0.0f, 0.0f, 1.0f);
            mesh.renderPoints(renderer, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        BaseState state1 = (BaseState)this.virtualAnimal.getState();
        state1.mesh.renderOutline(renderer, 0.0f, 1.0f, 0.0f, 1.0f);
        boolean z = false;
        this.cdAStar.setMeshList(this.combinedMeshList);
        this.cdAStar.findPath(x1, y1, 0, x2, y2, 0, null);
        this.virtualAnimal.getState().update();
        float box = 1.0f;
        renderer.drawRect(this.virtualAnimal.getX() - 0.5f, this.virtualAnimal.getY() - 0.5f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f);
    }

    static {
        combinedZonesMap = new HashMap();
    }

    public static class StateEat
    extends BaseState {
        public StateEat(VirtualAnimal animal) {
            super(animal);
        }

        @Override
        public void reachedEnd() {
            super.reachedEnd();
            if (this.counter < 5) {
                return;
            }
            for (Mesh mesh : this.combinedZones.meshList.meshes) {
                AnimalZone animalZone;
                Zone zone = mesh.zone;
                if (!(zone instanceof AnimalZone) || !(animalZone = (AnimalZone)zone).getAction().equals("Sleep")) continue;
                StateSleep state = new StateSleep(this.combinedZones.virtualAnimal);
                state.combinedZones = this.combinedZones;
                state.mesh = mesh;
                this.combinedZones.virtualAnimal.setState(state);
                break;
            }
        }
    }

    public static class StateFollow
    extends BaseState {
        public StateFollow(VirtualAnimal animal) {
            super(animal);
        }

        @Override
        public void reachedEnd() {
            super.reachedEnd();
            if (this.counter < 5) {
                return;
            }
            for (Mesh mesh : this.combinedZones.meshList.meshes) {
                AnimalZone animalZone;
                Zone zone = mesh.zone;
                if (!(zone instanceof AnimalZone) || !(animalZone = (AnimalZone)zone).getAction().equals("Eat")) continue;
                StateEat state = new StateEat(this.combinedZones.virtualAnimal);
                state.combinedZones = this.combinedZones;
                state.mesh = mesh;
                this.combinedZones.virtualAnimal.setState(state);
                break;
            }
        }
    }

    public static class StateSleep
    extends BaseState {
        public StateSleep(VirtualAnimal animal) {
            super(animal);
        }

        @Override
        public void reachedEnd() {
            super.reachedEnd();
            if (this.counter < 5) {
                return;
            }
            for (Mesh mesh : this.combinedZones.meshList.meshes) {
                AnimalZone animalZone;
                Zone zone = mesh.zone;
                if (!(zone instanceof AnimalZone) || !(animalZone = (AnimalZone)zone).getAction().equals("Follow")) continue;
                StateFollow state = new StateFollow(this.combinedZones.virtualAnimal);
                state.combinedZones = this.combinedZones;
                state.mesh = mesh;
                this.combinedZones.virtualAnimal.setState(state);
                break;
            }
        }
    }

    public static class BaseState
    extends VirtualAnimalState {
        CombinedZones combinedZones;
        Mesh mesh;
        ArrayList<Vector2f> path = new ArrayList();
        float pathLength;
        float distanceAlongPath;
        int counter = 5;

        public BaseState(VirtualAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            if (this.mesh == null) {
                return;
            }
            if (this.path.isEmpty()) {
                Vector2f pos = this.mesh.pickRandomPoint(new Vector2f());
                AnimalPathfind.getInstance().drawRect(pos.x - 0.5f, pos.y - 0.5f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f);
                Mesh mesh1 = this.combinedZones.meshList.getMeshAt(this.animal.getX(), this.animal.getY(), 0);
                Mesh mesh2 = this.combinedZones.meshList.getMeshAt(pos.x, pos.y, 0);
                if (mesh1 != mesh2) {
                    this.combinedZones.cdAStar.setMeshList(this.combinedZones.combinedMeshList);
                } else {
                    MeshList meshList = this.combinedZones.zoneMeshListMap.get(mesh1.zone);
                    this.combinedZones.cdAStar.setMeshList(meshList);
                }
                this.combinedZones.cdAStar.findPath(this.animal.getX(), this.animal.getY(), 0, pos.x, pos.y, 0, this.path);
                this.pathLength = this.getPolylineLength(this.path);
                this.distanceAlongPath = 0.0f;
            }
            for (int i = 0; i < this.path.size() - 1; ++i) {
                Vector2f p1 = this.path.get(i);
                Vector2f p2 = this.path.get(i + 1);
                AnimalPathfind.getInstance().drawLine(p1.x, p1.y, p2.x, p2.y, 0.0f, 1.0f, 1.0f, 1.0f);
            }
            Vector2f pos = new Vector2f();
            if (this.getPointOnPath(this.distanceAlongPath + 1.0f / this.pathLength, pos)) {
                this.animal.setX(pos.x);
                this.animal.setY(pos.y);
                this.distanceAlongPath += 1.0f / this.pathLength;
                if (this.distanceAlongPath >= 1.0f) {
                    this.reachedEnd();
                }
            }
        }

        @Override
        public void reachedEnd() {
            this.path.clear();
            if (--this.counter <= 0) {
                this.counter = 5;
                this.mesh = PZArrayUtil.pickRandom(this.combinedZones.meshList.meshes);
            }
        }

        float getPolylineLength(ArrayList<Vector2f> points) {
            float length = 0.0f;
            for (int i = 0; i < points.size() - 1; ++i) {
                float x1 = points.get((int)i).x;
                float y1 = points.get((int)i).y;
                float x2 = points.get((int)(i + 1)).x;
                float y2 = points.get((int)(i + 1)).y;
                length += Vector2f.length(x2 - x1, y2 - y1);
            }
            return length;
        }

        boolean getPointOnPath(float t, Vector2f out) {
            t = PZMath.clampFloat(t, 0.0f, 1.0f);
            out.set(0.0f);
            float length = this.pathLength;
            if (length <= 0.0f) {
                return false;
            }
            float distanceFromStart = length * t;
            float segmentStart = 0.0f;
            for (int i = 0; i < this.path.size() - 1; ++i) {
                float x2 = this.path.get((int)(i + 1)).x;
                float x1 = this.path.get((int)i).x;
                float y2 = this.path.get((int)(i + 1)).y;
                float y1 = this.path.get((int)i).y;
                float segmentLength = Vector2f.length(x2 - x1, y2 - y1);
                if (segmentStart + segmentLength >= distanceFromStart) {
                    float f = (distanceFromStart - segmentStart) / segmentLength;
                    out.set(x1 + (x2 - x1) * f, y1 + (y2 - y1) * f);
                    return true;
                }
                segmentStart += segmentLength;
            }
            return false;
        }
    }
}


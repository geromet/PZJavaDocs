/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import org.joml.Vector2f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.characters.animals.AnimalZoneJunction;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.IsoUtils;
import zombie.iso.zones.Zone;
import zombie.util.SharedStrings;

public final class AnimalZone
extends Zone {
    String animalType;
    public String action;
    public ArrayList<AnimalZoneJunction> junctions;
    boolean spawnedAnimals;
    boolean spawnAnimal = true;

    public AnimalZone() {
    }

    public AnimalZone(String name, String type, int x, int y, int z, int w, int h, KahluaTable properties) {
        super(name, type, x, y, z, w, h);
        if (properties != null) {
            Object o = properties.rawget("Action");
            if (o instanceof String) {
                String string;
                this.action = string = (String)o;
            }
            if ((o = properties.rawget("AnimalType")) instanceof String) {
                String s;
                this.animalType = s = (String)o;
            }
            if ((o = properties.rawget("SpawnAnimals")) instanceof Boolean) {
                Boolean b = (Boolean)o;
                this.spawnAnimal = b;
            }
        }
    }

    public AnimalZone(String name, String type, int x, int y, int z, int w, int h, String action, String animalType, boolean spawnAnimal) {
        super(name, type, x, y, z, w, h);
        this.action = action;
        this.animalType = animalType;
        this.spawnAnimal = spawnAnimal;
    }

    @Override
    public void save(ByteBuffer output) {
        super.save(output);
        GameWindow.WriteString(output, this.action);
        GameWindow.WriteString(output, this.animalType);
        output.put((byte)(this.spawnAnimal ? 1 : 0));
        output.put((byte)(this.spawnedAnimals ? 1 : 0));
    }

    @Override
    public void save(ByteBuffer output, Map<String, Integer> stringMap) {
        super.save(output, stringMap);
        GameWindow.WriteString(output, this.action);
        GameWindow.WriteString(output, this.animalType);
        output.put((byte)(this.spawnAnimal ? 1 : 0));
        output.put((byte)(this.spawnedAnimals ? 1 : 0));
    }

    @Override
    public AnimalZone load(ByteBuffer input, int worldVersion, Map<Integer, String> stringMap, SharedStrings sharedStrings) {
        super.load(input, worldVersion, stringMap, sharedStrings);
        this.action = GameWindow.ReadString(input);
        this.animalType = GameWindow.ReadString(input);
        this.spawnAnimal = input.get() != 0;
        this.spawnedAnimals = input.get() != 0;
        return this;
    }

    @Override
    public AnimalZone load(ByteBuffer input, int worldVersion) {
        super.load(input, worldVersion);
        this.action = GameWindow.ReadString(input);
        this.animalType = GameWindow.ReadString(input);
        this.spawnAnimal = input.get() != 0;
        this.spawnedAnimals = input.get() != 0;
        return this;
    }

    @Override
    public void Dispose() {
        super.Dispose();
        this.animalType = null;
        this.action = null;
        this.junctions = null;
        this.spawnedAnimals = false;
        this.spawnAnimal = true;
    }

    public int getIndexOfPoint(int x, int y) {
        for (int i = 0; i < this.points.size(); i += 2) {
            int x1 = this.points.get(i);
            int y1 = this.points.get(i + 1);
            if (x1 != x || y1 != y) continue;
            return i / 2;
        }
        return -1;
    }

    public String getAction() {
        return this.action;
    }

    public void addJunctionsWithOtherZone(AnimalZone other) {
        for (int i = 0; i < this.points.size(); i += 2) {
            int y;
            int x = this.points.get(i);
            int index = other.getIndexOfPoint(x, y = this.points.get(i + 1));
            if (index == -1) continue;
            this.addJunction(i / 2, other, index);
            other.addJunction(index, this, i / 2);
        }
    }

    boolean hasJunction(int pointIndexSelf, AnimalZone other, int pointIndexOther) {
        if (this.junctions == null) {
            return false;
        }
        for (int i = 0; i < this.junctions.size(); ++i) {
            AnimalZoneJunction junction = this.junctions.get(i);
            if (junction.pointIndexSelf != pointIndexSelf || junction.zoneOther != other || junction.pointIndexOther != pointIndexOther) continue;
            return true;
        }
        return false;
    }

    public void addJunction(int pointIndexSelf, AnimalZone other, int pointIndexOther) {
        if (this.hasJunction(pointIndexSelf, other, pointIndexOther)) {
            return;
        }
        if (pointIndexSelf < 0 || pointIndexSelf >= this.points.size() / 2) {
            return;
        }
        if (other == null || other == this || pointIndexOther < 0 || pointIndexOther >= other.points.size() / 2) {
            return;
        }
        if (this.junctions == null) {
            this.junctions = new ArrayList();
        }
        this.junctions.add(new AnimalZoneJunction(this, pointIndexSelf, other, pointIndexOther));
    }

    public void addJunction(AnimalZoneJunction junction) {
        if (this.junctions == null) {
            this.junctions = new ArrayList();
        }
        if (this.junctions.contains(junction)) {
            return;
        }
        this.junctions.add(junction);
    }

    public void getJunctionsBetween(float t1, float t2, ArrayList<AnimalZoneJunction> junctions) {
        junctions.clear();
        float length = this.getPolylineLength();
        for (int i = 0; i < this.junctions.size(); ++i) {
            AnimalZoneJunction junction = this.junctions.get(i);
            if (!(junction.distanceFromStart / length >= t1) || !(junction.distanceFromStart / length <= t2)) continue;
            junctions.add(junction);
        }
    }

    public float getClosedPolylineLength() {
        if (!this.isPolyline() || this.points.size() < 6) {
            return 0.0f;
        }
        float length = 0.0f;
        for (int i = 0; i < this.points.size(); i += 2) {
            float x1 = (float)this.points.get(i) + 0.5f;
            float y1 = (float)this.points.get(i + 1) + 0.5f;
            float x2 = (float)this.points.get((i + 2) % this.points.size()) + 0.5f;
            float y2 = (float)this.points.get((i + 3) % this.points.size()) + 0.5f;
            length += Vector2f.length(x2 - x1, y2 - y1);
        }
        return length;
    }

    int getPolylineSegment(float t) {
        t = PZMath.clampFloat(t, 0.0f, 1.0f);
        float length = this.getPolylineLength();
        if (length <= 0.0f) {
            return -1;
        }
        float distanceFromStart = length * t;
        float segmentStart = 0.0f;
        for (int i = 0; i < this.points.size() - 2; i += 2) {
            float y2;
            float x1 = (float)this.points.get(i) + 0.5f;
            float y1 = (float)this.points.get(i + 1) + 0.5f;
            float x2 = (float)this.points.get((i + 2) % this.points.size()) + 0.5f;
            float segmentLength = Vector2f.length(x2 - x1, (y2 = (float)this.points.get((i + 3) % this.points.size()) + 0.5f) - y1);
            if (segmentStart + segmentLength >= distanceFromStart) {
                return i;
            }
            segmentStart += segmentLength;
        }
        return -1;
    }

    public boolean getPointOnPolyline(float t, Vector2f out) {
        t = PZMath.clampFloat(t, 0.0f, 1.0f);
        out.set(0.0f);
        float length = this.getPolylineLength();
        if (length <= 0.0f) {
            return false;
        }
        float distanceFromStart = length * t;
        float segmentStart = 0.0f;
        for (int i = 0; i < this.points.size() - 2; i += 2) {
            float y2;
            float x1 = (float)this.points.get(i) + 0.5f;
            float y1 = (float)this.points.get(i + 1) + 0.5f;
            float x2 = (float)this.points.get((i + 2) % this.points.size()) + 0.5f;
            float segmentLength = Vector2f.length(x2 - x1, (y2 = (float)this.points.get((i + 3) % this.points.size()) + 0.5f) - y1);
            if (segmentStart + segmentLength >= distanceFromStart) {
                float f = (distanceFromStart - segmentStart) / segmentLength;
                out.set(x1 + (x2 - x1) * f, y1 + (y2 - y1) * f);
                return true;
            }
            segmentStart += segmentLength;
        }
        return false;
    }

    boolean pickRandomPointOnPolyline(Vector2f out) {
        return this.getPointOnPolyline(Rand.Next(0.0f, 1.0f), out);
    }

    public float getClosestPointOnPolyline(float px, float py, Vector2f out) {
        if (!this.isPolyline() && this.points.size() < 6) {
            return -1.0f;
        }
        float closestDist = Float.MAX_VALUE;
        float distanceFromStart = 0.0f;
        float length = 0.0f;
        for (int i = 0; i < this.points.size() - 2; i += 2) {
            float x1 = (float)this.points.get(i) + 0.5f;
            float y1 = (float)this.points.get(i + 1) + 0.5f;
            float x2 = (float)this.points.get((i + 2) % this.points.size()) + 0.5f;
            float y2 = (float)this.points.get((i + 3) % this.points.size()) + 0.5f;
            float segmentLength = Vector2f.distance(x1, y1, x2, y2);
            double u = (double)((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / (Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0));
            double xu = (double)x1 + u * (double)(x2 - x1);
            double yu = (double)y1 + u * (double)(y2 - y1);
            if (u <= 0.0) {
                xu = x1;
                yu = y1;
                u = 0.0;
            } else if (u >= 1.0) {
                xu = x2;
                yu = y2;
                u = 1.0;
            }
            float dist = IsoUtils.DistanceToSquared(px, py, (float)xu, (float)yu);
            if (dist < closestDist) {
                closestDist = dist;
                out.set(xu, yu);
                distanceFromStart = length + (float)(u * (double)segmentLength);
            }
            length += segmentLength;
        }
        return distanceFromStart / length;
    }

    public float getDistanceOfPointFromStart(int pointIndex) {
        float length = 0.0f;
        for (int i = 0; i < pointIndex * 2; i += 2) {
            float x1 = (float)this.points.get(i) + 0.5f;
            float y1 = (float)this.points.get(i + 1) + 0.5f;
            float x2 = (float)this.points.get((i + 2) % this.points.size()) + 0.5f;
            float y2 = (float)this.points.get((i + 3) % this.points.size()) + 0.5f;
            length += Vector2f.length(x2 - x1, y2 - y1);
        }
        return length;
    }

    public boolean getDirectionOnPolyline(float t, Vector2f out) {
        int i = this.getPolylineSegment(t);
        if (i == -1) {
            return false;
        }
        float x1 = (float)this.points.get(i) + 0.5f;
        float y1 = (float)this.points.get(i + 1) + 0.5f;
        float x2 = (float)this.points.get((i + 2) % this.points.size()) + 0.5f;
        float y2 = (float)this.points.get((i + 3) % this.points.size()) + 0.5f;
        out.set(x2 - x1, y2 - y1).normalize();
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("{");
        sb.append("action='").append(this.action).append('\'');
        sb.append(", animal_type=").append(this.animalType).append('\'');
        sb.append("}");
        return sb.toString();
    }
}


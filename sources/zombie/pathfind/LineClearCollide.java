/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.iso.IsoDirections;
import zombie.iso.Vector2;
import zombie.pathfind.Chunk;
import zombie.pathfind.ChunkDataZ;
import zombie.pathfind.LiangBarsky;
import zombie.pathfind.Obstacle;
import zombie.pathfind.Point;
import zombie.pathfind.PointPool;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Square;
import zombie.pathfind.Vehicle;
import zombie.pathfind.VehiclePoly;
import zombie.pathfind.VehicleRect;

final class LineClearCollide {
    final Vector2 perp = new Vector2();
    final ArrayList<Point> pts = new ArrayList();
    final VehicleRect sweepAabb = new VehicleRect();
    final VehicleRect vehicleAabb = new VehicleRect();
    final Vector2[] polyVec = new Vector2[4];
    final Vector2[] vehicleVec = new Vector2[4];
    final PointPool pointPool = new PointPool();
    final LiangBarsky lb = new LiangBarsky();

    LineClearCollide() {
        for (int i = 0; i < 4; ++i) {
            this.polyVec[i] = new Vector2();
            this.vehicleVec[i] = new Vector2();
        }
    }

    private float clamp(float f1, float min, float max) {
        if (f1 < min) {
            f1 = min;
        }
        if (f1 > max) {
            f1 = max;
        }
        return f1;
    }

    @Deprecated
    boolean canStandAt(PolygonalMap2 map, float fromX, float fromY, float x, float y, float z, Vehicle ignoreVehicle) {
        if ((PZMath.fastfloor(fromX) != PZMath.fastfloor(x) || PZMath.fastfloor(fromY) != PZMath.fastfloor(y)) && map.isBlockedInAllDirections(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z))) {
            return false;
        }
        int minX = PZMath.fastfloor(x - 0.3f);
        int minY = PZMath.fastfloor(y - 0.3f);
        int maxX = (int)Math.ceil(x + 0.3f);
        int maxY = (int)Math.ceil(y + 0.3f);
        for (int sy = minY; sy < maxY; ++sy) {
            for (int sx = minX; sx < maxX; ++sx) {
                boolean bTargetSquare;
                Square square = map.getSquare(sx, sy, PZMath.fastfloor(z));
                boolean bl = bTargetSquare = x >= (float)sx && y >= (float)sy && x < (float)(sx + 1) && y < (float)(sy + 1);
                if (square == null || square.isReallySolid() || !square.has(512)) {
                    if (!bTargetSquare) continue;
                    return false;
                }
                if (!square.has(504) && !square.hasSlopedSurface()) continue;
            }
        }
        for (int i = 0; i < map.vehicles.size(); ++i) {
            Vehicle vehicle = map.vehicles.get(i);
            if (vehicle == ignoreVehicle || PZMath.fastfloor(vehicle.polyPlusRadius.z) != PZMath.fastfloor(z) || !vehicle.polyPlusRadius.containsPoint(x, y)) continue;
            return false;
        }
        return true;
    }

    boolean canStandAtClipper(PolygonalMap2 map, float fromX, float fromY, float x, float y, float z, Vehicle ignoreVehicle, int flags) {
        if ((PZMath.fastfloor(fromX) != PZMath.fastfloor(x) || PZMath.fastfloor(fromY) != PZMath.fastfloor(y)) && map.isBlockedInAllDirections(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z))) {
            return false;
        }
        Chunk chunk = map.getChunkFromSquarePos(PZMath.fastfloor(x), PZMath.fastfloor(y));
        if (chunk == null) {
            return false;
        }
        ChunkDataZ chunkDataZ = chunk.collision.init(chunk, PZMath.fastfloor(z));
        for (int i = 0; i < chunkDataZ.obstacles.size(); ++i) {
            Obstacle obstacle = chunkDataZ.obstacles.get(i);
            if (ignoreVehicle != null && obstacle.vehicle == ignoreVehicle || !obstacle.bounds.containsPoint(x, y) || !obstacle.isPointInside(x, y, flags)) continue;
            return false;
        }
        return true;
    }

    float isLeft(float x0, float y0, float x1, float y1, float x2, float y2) {
        return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
    }

    boolean isPointInPolygon_WindingNumber(float x, float y, VehiclePoly poly) {
        this.polyVec[0].set(poly.x1, poly.y1);
        this.polyVec[1].set(poly.x2, poly.y2);
        this.polyVec[2].set(poly.x3, poly.y3);
        this.polyVec[3].set(poly.x4, poly.y4);
        int wn = 0;
        for (int i = 0; i < 4; ++i) {
            Vector2 v2;
            Vector2 v1 = this.polyVec[i];
            Vector2 vector2 = v2 = i == 3 ? this.polyVec[0] : this.polyVec[i + 1];
            if (v1.y <= y) {
                if (!(v2.y > y) || !(this.isLeft(v1.x, v1.y, v2.x, v2.y, x, y) > 0.0f)) continue;
                ++wn;
                continue;
            }
            if (!(v2.y <= y) || !(this.isLeft(v1.x, v1.y, v2.x, v2.y, x, y) < 0.0f)) continue;
            --wn;
        }
        return wn != 0;
    }

    @Deprecated
    boolean isNotClearOld(PolygonalMap2 map, float fromX, float fromY, float toX, float toY, int z, int flags) {
        int i;
        boolean checkCost;
        boolean bl = checkCost = (flags & 4) != 0;
        if (!this.canStandAt(map, fromX, fromY, toX, toY, z, null)) {
            return true;
        }
        float perpX = toY - fromY;
        float perpY = -(toX - fromX);
        this.perp.set(perpX, perpY);
        this.perp.normalize();
        float x0 = fromX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
        float y0 = fromY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
        float x1 = toX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
        float y1 = toY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
        this.perp.set(-perpX, -perpY);
        this.perp.normalize();
        float x2 = fromX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
        float y2 = fromY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
        float x3 = toX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
        float y3 = toY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
        for (i = 0; i < this.pts.size(); ++i) {
            this.pointPool.release(this.pts.get(i));
        }
        this.pts.clear();
        this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(fromX), PZMath.fastfloor(fromY)));
        if (PZMath.fastfloor(fromX) != PZMath.fastfloor(toX) || PZMath.fastfloor(fromY) != PZMath.fastfloor(toY)) {
            this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(toX), PZMath.fastfloor(toY)));
        }
        map.supercover(x0, y0, x1, y1, z, this.pointPool, this.pts);
        map.supercover(x2, y2, x3, y3, z, this.pointPool, this.pts);
        for (i = 0; i < this.pts.size(); ++i) {
            Point pt = this.pts.get(i);
            Square square = map.getSquare(pt.x, pt.y, z);
            if (checkCost && square != null && square.cost > 0) {
                return true;
            }
            if (square == null || square.isReallySolid() || !square.has(512)) {
                float w = 0.3f;
                float n = 0.3f;
                float e = 0.3f;
                float s = 0.3f;
                if (fromX < (float)pt.x && toX < (float)pt.x) {
                    w = 0.0f;
                } else if (fromX >= (float)(pt.x + 1) && toX >= (float)(pt.x + 1)) {
                    e = 0.0f;
                }
                if (fromY < (float)pt.y && toY < (float)pt.y) {
                    n = 0.0f;
                } else if (fromY >= (float)(pt.y + 1) && toY >= (float)(pt.y + 1)) {
                    s = 0.0f;
                }
                if (!this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, (float)pt.x - w, (float)pt.y - n, (float)pt.x + 1.0f + e, (float)pt.y + 1.0f + s)) continue;
                return true;
            }
            if (square.has(504)) {
                if (square.has(448)) {
                    if (this.testCollisionVertical(fromX, fromY, toX, toY, pt.x, pt.y, false, true)) {
                        return true;
                    }
                    if (this.testCollisionVertical(fromX, fromY, toX, toY, pt.x + 1, pt.y, true, false)) {
                        return true;
                    }
                    if (square.has(64) && this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y, false, true)) {
                        return true;
                    }
                }
                if (!square.has(56)) continue;
                if (this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y, false, true)) {
                    return true;
                }
                if (this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x + 1, pt.y, true, false)) {
                    return true;
                }
                if (!square.has(8) || !this.testCollisionVertical(fromX, fromY, toX, toY, pt.x, pt.y, false, true)) continue;
                return true;
            }
            if (square.hasSlopedSurface()) {
                IsoDirections dir = square.getSlopedSurfaceDirection();
                if (dir == IsoDirections.N || dir == IsoDirections.S) {
                    if ((square.getAdjacentSquare(IsoDirections.W) == null || !square.hasIdenticalSlopedSurface(square.getAdjacentSquare(IsoDirections.W))) && this.testCollisionVertical(fromX, fromY, toX, toY, pt.x, pt.y, false, true)) {
                        return true;
                    }
                    if ((square.getAdjacentSquare(IsoDirections.E) == null || !square.hasIdenticalSlopedSurface(square.getAdjacentSquare(IsoDirections.E))) && this.testCollisionVertical(fromX, fromY, toX, toY, pt.x + 1, pt.y, true, false)) {
                        return true;
                    }
                    Square squareN = square.getAdjacentSquare(IsoDirections.N);
                    if (dir == IsoDirections.N && (squareN == null || square.getSlopedSurfaceHeightMax() != squareN.getSlopedSurfaceHeight(0.5f, 1.0f)) && this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y, false, true)) {
                        return true;
                    }
                    Square squareS = square.getAdjacentSquare(IsoDirections.S);
                    if (dir == IsoDirections.S && (squareS == null || square.getSlopedSurfaceHeightMax() != squareS.getSlopedSurfaceHeight(0.5f, 0.0f)) && this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y + 1, true, false)) {
                        return true;
                    }
                }
                if (dir != IsoDirections.W && dir != IsoDirections.E) continue;
                Square squareN = square.getAdjacentSquare(IsoDirections.N);
                if ((squareN == null || !square.hasIdenticalSlopedSurface(squareN)) && this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y, false, true)) {
                    return true;
                }
                Square squareS = square.getAdjacentSquare(IsoDirections.S);
                if ((squareS == null || !square.hasIdenticalSlopedSurface(squareS)) && this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y + 1, true, false)) {
                    return true;
                }
                Square squareW = square.getAdjacentSquare(IsoDirections.W);
                if (dir == IsoDirections.W && (squareW == null || square.getSlopedSurfaceHeightMax() != squareW.getSlopedSurfaceHeight(1.0f, 0.5f)) && this.testCollisionVertical(fromX, fromY, toX, toY, pt.x, pt.y, false, true)) {
                    return true;
                }
                Square squareE = square.getAdjacentSquare(IsoDirections.E);
                if (dir != IsoDirections.E || squareE != null && square.getSlopedSurfaceHeightMax() == squareE.getSlopedSurfaceHeight(0.0f, 0.5f) || !this.testCollisionVertical(fromX, fromY, toX, toY, pt.x + 1, pt.y, true, false)) continue;
                return true;
            }
            if (square.isCollideW()) {
                float w = 0.3f;
                float n = 0.3f;
                float e = 0.3f;
                float s = 0.3f;
                if (fromX < (float)pt.x && toX < (float)pt.x) {
                    w = 0.0f;
                } else if (fromX >= (float)pt.x && toX >= (float)pt.x) {
                    e = 0.0f;
                }
                if (fromY < (float)pt.y && toY < (float)pt.y) {
                    n = 0.0f;
                } else if (fromY >= (float)(pt.y + 1) && toY >= (float)(pt.y + 1)) {
                    s = 0.0f;
                }
                if (this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, (float)pt.x - w, (float)pt.y - n, (float)pt.x + e, (float)pt.y + 1.0f + s)) {
                    return true;
                }
            }
            if (!square.isCollideN()) continue;
            float w = 0.3f;
            float n = 0.3f;
            float e = 0.3f;
            float s = 0.3f;
            if (fromX < (float)pt.x && toX < (float)pt.x) {
                w = 0.0f;
            } else if (fromX >= (float)(pt.x + 1) && toX >= (float)(pt.x + 1)) {
                e = 0.0f;
            }
            if (fromY < (float)pt.y && toY < (float)pt.y) {
                n = 0.0f;
            } else if (fromY >= (float)pt.y && toY >= (float)pt.y) {
                s = 0.0f;
            }
            if (!this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, (float)pt.x - w, (float)pt.y - n, (float)pt.x + 1.0f + e, (float)pt.y + s)) continue;
            return true;
        }
        float radius = 0.15f;
        this.perp.set(perpX, perpY);
        this.perp.normalize();
        x0 = fromX + this.perp.x * 0.15f;
        y0 = fromY + this.perp.y * 0.15f;
        x1 = toX + this.perp.x * 0.15f;
        y1 = toY + this.perp.y * 0.15f;
        this.perp.set(-perpX, -perpY);
        this.perp.normalize();
        x2 = fromX + this.perp.x * 0.15f;
        y2 = fromY + this.perp.y * 0.15f;
        x3 = toX + this.perp.x * 0.15f;
        y3 = toY + this.perp.y * 0.15f;
        float minX = Math.min(x0, Math.min(x1, Math.min(x2, x3)));
        float minY = Math.min(y0, Math.min(y1, Math.min(y2, y3)));
        float maxX = Math.max(x0, Math.max(x1, Math.max(x2, x3)));
        float maxY = Math.max(y0, Math.max(y1, Math.max(y2, y3)));
        this.sweepAabb.init(PZMath.fastfloor(minX), PZMath.fastfloor(minY), (int)Math.ceil(maxX) - PZMath.fastfloor(minX), (int)Math.ceil(maxY) - PZMath.fastfloor(minY), z);
        this.polyVec[0].set(x0, y0);
        this.polyVec[1].set(x1, y1);
        this.polyVec[2].set(x3, y3);
        this.polyVec[3].set(x2, y2);
        for (int i2 = 0; i2 < map.vehicles.size(); ++i2) {
            Vehicle vehicle = map.vehicles.get(i2);
            VehicleRect rect = vehicle.poly.getAABB(this.vehicleAabb);
            if (!rect.intersects(this.sweepAabb) || !this.polyVehicleIntersect(vehicle.poly)) continue;
            return true;
        }
        return false;
    }

    boolean testCollisionHorizontal(float fromX, float fromY, float toX, float toY, float ptX, float ptY, boolean bSkipN, boolean bSkipS) {
        float w = 0.3f;
        float n = 0.3f;
        float e = 0.3f;
        float s = 0.3f;
        if (fromX < ptX && toX < ptX) {
            w = 0.0f;
        } else if (fromX >= ptX + 1.0f && toX >= ptX + 1.0f) {
            e = 0.0f;
        }
        if (fromY < ptY && toY < ptY) {
            n = 0.0f;
        } else if (fromY >= ptY && toY >= ptY) {
            s = 0.0f;
        }
        if (bSkipN) {
            n = 0.0f;
        }
        if (bSkipS) {
            s = 0.0f;
        }
        return this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, ptX - w, ptY - n, ptX + 1.0f + e, ptY + s);
    }

    boolean testCollisionVertical(float fromX, float fromY, float toX, float toY, float ptX, float ptY, boolean bSkipW, boolean bSkipE) {
        float w = 0.3f;
        float n = 0.3f;
        float e = 0.3f;
        float s = 0.3f;
        if (fromX < ptX && toX < ptX) {
            w = 0.0f;
        } else if (fromX >= ptX && toX >= ptX) {
            e = 0.0f;
        }
        if (fromY < ptY && toY < ptY) {
            n = 0.0f;
        } else if (fromY >= ptY + 1.0f && toY >= ptY + 1.0f) {
            s = 0.0f;
        }
        if (bSkipW) {
            w = 0.0f;
        }
        if (bSkipE) {
            e = 0.0f;
        }
        return this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, ptX - w, ptY - n, ptX + e, ptY + 1.0f + s);
    }

    boolean isNotClearClipper(PolygonalMap2 map, float fromX, float fromY, float toX, float toY, int z, int flags) {
        int yInc;
        double error;
        int xInc;
        boolean checkCost = (flags & 4) != 0;
        Square square = map.getSquare(PZMath.fastfloor(fromX), PZMath.fastfloor(fromY), z);
        if (square != null && square.has(504)) {
            return true;
        }
        if (!this.canStandAtClipper(map, fromX, fromY, toX, toY, z, null, flags)) {
            return true;
        }
        float x0 = fromX / 8.0f;
        float y0 = fromY / 8.0f;
        float x1 = toX / 8.0f;
        float y1 = toY / 8.0f;
        double dx = Math.abs(x1 - x0);
        double dy = Math.abs(y1 - y0);
        int x = PZMath.fastfloor(x0);
        int y = PZMath.fastfloor(y0);
        int n = 1;
        if (dx == 0.0) {
            xInc = 0;
            error = Double.POSITIVE_INFINITY;
        } else if (x1 > x0) {
            xInc = 1;
            n += PZMath.fastfloor(x1) - x;
            error = (double)((float)(PZMath.fastfloor(x0) + 1) - x0) * dy;
        } else {
            xInc = -1;
            n += x - PZMath.fastfloor(x1);
            error = (double)(x0 - (float)PZMath.fastfloor(x0)) * dy;
        }
        if (dy == 0.0) {
            yInc = 0;
            error -= Double.POSITIVE_INFINITY;
        } else if (y1 > y0) {
            yInc = 1;
            n += PZMath.fastfloor(y1) - y;
            error -= (double)((float)(PZMath.fastfloor(y0) + 1) - y0) * dx;
        } else {
            yInc = -1;
            n += y - PZMath.fastfloor(y1);
            error -= (double)(y0 - (float)PZMath.fastfloor(y0)) * dx;
        }
        while (n > 0) {
            Chunk chunk = PolygonalMap2.instance.getChunkFromChunkPos(x, y);
            if (chunk != null) {
                ChunkDataZ chunkDataZ = chunk.collision.init(chunk, z);
                ArrayList<Obstacle> obstacles = chunkDataZ.obstacles;
                for (int i = 0; i < obstacles.size(); ++i) {
                    Obstacle obstacle = obstacles.get(i);
                    if (!obstacle.lineSegmentIntersects(fromX, fromY, toX, toY)) continue;
                    return true;
                }
            }
            if (error > 0.0) {
                y += yInc;
                error -= dx;
            } else {
                x += xInc;
                error += dy;
            }
            --n;
        }
        return checkCost && this.isNotClearCost(fromX, fromY, toX, toY, z);
    }

    boolean isNotClearCost(float fromX, float fromY, float toX, float toY, int z) {
        int i;
        float perpX = toY - fromY;
        float perpY = -(toX - fromX);
        this.perp.set(perpX, perpY);
        this.perp.normalize();
        float x0 = fromX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
        float y0 = fromY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
        float x1 = toX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
        float y1 = toY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
        this.perp.set(-perpX, -perpY);
        this.perp.normalize();
        float x2 = fromX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
        float y2 = fromY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
        float x3 = toX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
        float y3 = toY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
        for (i = 0; i < this.pts.size(); ++i) {
            this.pointPool.release(this.pts.get(i));
        }
        this.pts.clear();
        this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(fromX), PZMath.fastfloor(fromY)));
        if (PZMath.fastfloor(fromX) != PZMath.fastfloor(toX) || PZMath.fastfloor(fromY) != PZMath.fastfloor(toY)) {
            this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(toX), PZMath.fastfloor(toY)));
        }
        PolygonalMap2.instance.supercover(x0, y0, x1, y1, z, this.pointPool, this.pts);
        PolygonalMap2.instance.supercover(x2, y2, x3, y3, z, this.pointPool, this.pts);
        for (i = 0; i < this.pts.size(); ++i) {
            Point pt = this.pts.get(i);
            Square square = PolygonalMap2.instance.getSquare(pt.x, pt.y, z);
            if (square == null || square.cost <= 0) continue;
            return true;
        }
        return false;
    }

    boolean isNotClear(PolygonalMap2 map, float fromX, float fromY, float toX, float toY, int z, int flags) {
        return this.isNotClearOld(map, fromX, fromY, toX, toY, z, flags);
    }

    boolean polyVehicleIntersect(VehiclePoly poly) {
        this.vehicleVec[0].set(poly.x1, poly.y1);
        this.vehicleVec[1].set(poly.x2, poly.y2);
        this.vehicleVec[2].set(poly.x3, poly.y3);
        this.vehicleVec[3].set(poly.x4, poly.y4);
        boolean intersect = false;
        for (int i = 0; i < 4; ++i) {
            Vector2 a = this.polyVec[i];
            Vector2 b = i == 3 ? this.polyVec[0] : this.polyVec[i + 1];
            for (int j = 0; j < 4; ++j) {
                Vector2 d;
                Vector2 c = this.vehicleVec[j];
                Vector2 vector2 = d = j == 3 ? this.vehicleVec[0] : this.vehicleVec[j + 1];
                if (!Line2D.linesIntersect(a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y)) continue;
                intersect = true;
            }
        }
        return intersect;
    }
}


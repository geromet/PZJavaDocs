/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.debug.LineDrawer;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.Vector2;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.pathfind.LiangBarsky;
import zombie.pathfind.Point;
import zombie.pathfind.PointPool;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.SquareUpdateTask;
import zombie.pathfind.VehiclePoly;
import zombie.pathfind.VehicleRect;
import zombie.vehicles.BaseVehicle;

final class LineClearCollideMain {
    final Vector2 perp = new Vector2();
    final ArrayList<Point> pts = new ArrayList();
    final VehicleRect sweepAabb = new VehicleRect();
    final VehicleRect vehicleAabb = new VehicleRect();
    final VehiclePoly vehiclePoly = new VehiclePoly();
    final Vector2[] polyVec = new Vector2[4];
    final Vector2[] vehicleVec = new Vector2[4];
    final PointPool pointPool = new PointPool();
    final LiangBarsky lb = new LiangBarsky();

    LineClearCollideMain() {
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
    boolean canStandAtOld(PolygonalMap2 map, float x, float y, float z, BaseVehicle ignoreVehicle, int flags) {
        boolean ignoreDoors = (flags & 1) != 0;
        boolean closeToWalls = (flags & 2) != 0;
        int minX = PZMath.fastfloor(x - 0.3f);
        int minY = PZMath.fastfloor(y - 0.3f);
        int maxX = (int)Math.ceil(x + 0.3f);
        int maxY = (int)Math.ceil(y + 0.3f);
        for (int sy = minY; sy < maxY; ++sy) {
            for (int sx = minX; sx < maxX; ++sx) {
                float closestX;
                float closestX2;
                float closestY;
                float distanceY;
                float distanceX;
                float distanceSquared;
                IsoGridSquare below;
                boolean bHasFloor;
                boolean bTargetSquare = x >= (float)sx && y >= (float)sy && x < (float)(sx + 1) && y < (float)(sy + 1);
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(sx, sy, PZMath.fastfloor(z));
                boolean bl = square != null && (square.solidFloorCached ? square.solidFloor : square.TreatAsSolidFloor()) ? true : (bHasFloor = false);
                if ((square == null || square.getObjects().isEmpty()) && (below = IsoWorld.instance.currentCell.getGridSquare(sx, sy, PZMath.fastfloor(z) - 1)) != null && below.getSlopedSurfaceHeight(0.5f, 0.5f) > 0.9f) {
                    bHasFloor = true;
                }
                if (square == null || square.isSolid() || square.isSolidTrans() && !square.isAdjacentToWindow() && !square.isAdjacentToHoppable() || !bHasFloor) {
                    float closestX3;
                    if (!(closeToWalls ? bTargetSquare : (distanceSquared = (distanceX = x - (closestX3 = this.clamp(x, sx, sx + 1))) * distanceX + (distanceY = y - (closestY = this.clamp(y, sy, sy + 1))) * distanceY) < 0.09f)) continue;
                    return false;
                }
                if (square.HasStairs()) {
                    float closestX4;
                    float closestX5;
                    float closestX6;
                    float closestX7;
                    if (closeToWalls || bTargetSquare) continue;
                    if (square.isStairsEdgeBlocked(IsoDirections.N) && (distanceSquared = (distanceX = x - (closestX7 = this.clamp(x, sx, sx + 1))) * distanceX + (distanceY = y - (closestY = (float)sy)) * distanceY) < 0.09f) {
                        return false;
                    }
                    if (square.isStairsEdgeBlocked(IsoDirections.S) && (distanceSquared = (distanceX = x - (closestX6 = this.clamp(x, sx, sx + 1))) * distanceX + (distanceY = y - (closestY = (float)(sy + 1))) * distanceY) < 0.09f) {
                        return false;
                    }
                    if (square.isStairsEdgeBlocked(IsoDirections.W) && (distanceSquared = (distanceX = x - (closestX5 = (float)sx)) * distanceX + (distanceY = y - (closestY = this.clamp(y, sy, sy + 1))) * distanceY) < 0.09f) {
                        return false;
                    }
                    if (!square.isStairsEdgeBlocked(IsoDirections.E) || !((distanceSquared = (distanceX = x - (closestX4 = (float)(sx + 1))) * distanceX + (distanceY = y - (closestY = this.clamp(y, sy, sy + 1))) * distanceY) < 0.09f)) continue;
                    return false;
                }
                if (square.hasSlopedSurface()) {
                    float closestX8;
                    float closestX9;
                    float closestX10;
                    float closestX11;
                    if (closeToWalls || bTargetSquare) continue;
                    if (square.isSlopedSurfaceEdgeBlocked(IsoDirections.N) && (distanceSquared = (distanceX = x - (closestX11 = this.clamp(x, sx, sx + 1))) * distanceX + (distanceY = y - (closestY = (float)sy)) * distanceY) < 0.09f) {
                        return false;
                    }
                    if (square.isSlopedSurfaceEdgeBlocked(IsoDirections.S) && (distanceSquared = (distanceX = x - (closestX10 = this.clamp(x, sx, sx + 1))) * distanceX + (distanceY = y - (closestY = (float)(sy + 1))) * distanceY) < 0.09f) {
                        return false;
                    }
                    if (square.isSlopedSurfaceEdgeBlocked(IsoDirections.W) && (distanceSquared = (distanceX = x - (closestX9 = (float)sx)) * distanceX + (distanceY = y - (closestY = this.clamp(y, sy, sy + 1))) * distanceY) < 0.09f) {
                        return false;
                    }
                    if (!square.isSlopedSurfaceEdgeBlocked(IsoDirections.E) || !((distanceSquared = (distanceX = x - (closestX8 = (float)(sx + 1))) * distanceX + (distanceY = y - (closestY = this.clamp(y, sy, sy + 1))) * distanceY) < 0.09f)) continue;
                    return false;
                }
                if (closeToWalls) continue;
                if ((square.has(IsoFlagType.collideW) || !ignoreDoors && square.hasBlockedDoor(false)) && (distanceSquared = (distanceX = x - (closestX2 = (float)sx)) * distanceX + (distanceY = y - (closestY = this.clamp(y, sy, sy + 1))) * distanceY) < 0.09f) {
                    return false;
                }
                if (!square.has(IsoFlagType.collideN) && (ignoreDoors || !square.hasBlockedDoor(true)) || !((distanceSquared = (distanceX = x - (closestX = this.clamp(x, sx, sx + 1))) * distanceX + (distanceY = y - (closestY = (float)sy)) * distanceY) < 0.09f)) continue;
                return false;
            }
        }
        int chunkMinX = (PZMath.fastfloor(x) - 4) / 8 - 1;
        int chunkMinY = (PZMath.fastfloor(y) - 4) / 8 - 1;
        int chunkMaxX = (int)Math.ceil((x + 4.0f) / 8.0f) + 1;
        int chunkMaxY = (int)Math.ceil((y + 4.0f) / 8.0f) + 1;
        for (int cy = chunkMinY; cy < chunkMaxY; ++cy) {
            for (int cx = chunkMinX; cx < chunkMaxX; ++cx) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunkForGridSquare(cx * 8, cy * 8, 0);
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    if (vehicle == ignoreVehicle || !vehicle.addedToWorld || PZMath.fastfloor(vehicle.getZ()) != PZMath.fastfloor(z) || !vehicle.getPolyPlusRadius().containsPoint(x, y)) continue;
                    return false;
                }
            }
        }
        return true;
    }

    boolean canStandAtClipper(PolygonalMap2 map, float x, float y, float z, BaseVehicle ignoreVehicle, int flags) {
        return PolygonalMap2.instance.collideWithObstaclesPoly.canStandAt(x, y, z, ignoreVehicle, flags);
    }

    public void drawCircle(float x, float y, float z, float radius, float r, float g, float b, float a) {
        LineDrawer.DrawIsoCircle(x, y, z, radius, 16, r, g, b, a);
    }

    boolean isNotClearOld(PolygonalMap2 map, float fromX, float fromY, float toX, float toY, int z, BaseVehicle ignoreVehicle, int flags) {
        int i;
        boolean render;
        boolean ignoreDoors = (flags & 1) != 0;
        boolean checkCost = (flags & 4) != 0;
        boolean bl = render = (flags & 8) != 0;
        if (!this.canStandAtOld(map, toX, toY, z, ignoreVehicle, flags)) {
            if (render) {
                this.drawCircle(toX, toY, z, 0.3f, 1.0f, 0.0f, 0.0f, 1.0f);
            }
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
        if (render) {
            for (i = 0; i < this.pts.size(); ++i) {
                Point pt = this.pts.get(i);
                LineDrawer.addLine(pt.x, pt.y, z, (float)pt.x + 1.0f, (float)pt.y + 1.0f, z, 1.0f, 1.0f, 0.0f, null, false);
            }
        }
        boolean collided = false;
        for (int i2 = 0; i2 < this.pts.size(); ++i2) {
            IsoGridSquare below;
            boolean bHasFloor;
            Point pt = this.pts.get(i2);
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(pt.x, pt.y, z);
            if (checkCost && square != null && SquareUpdateTask.getCost(square) > 0) {
                return true;
            }
            boolean bl2 = square != null && (square.solidFloorCached ? square.solidFloor : square.TreatAsSolidFloor()) ? true : (bHasFloor = false);
            if ((square == null || square.getObjects().isEmpty()) && (below = IsoWorld.instance.currentCell.getGridSquare(pt.x, pt.y, z - 1)) != null && below.getSlopedSurfaceHeight(0.5f, 0.5f) > 0.9f) {
                bHasFloor = true;
            }
            if (square == null || square.isSolid() || square.isSolidTrans() && !square.isAdjacentToWindow() && !square.isAdjacentToHoppable() || !bHasFloor) {
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
                if (render) {
                    LineDrawer.addLine((float)pt.x - w, (float)pt.y - n, z, (float)pt.x + 1.0f + e, (float)pt.y + 1.0f + s, z, 1.0f, 0.0f, 0.0f, null, false);
                    collided = true;
                    continue;
                }
                return true;
            }
            if (square.HasStairs()) {
                if (square.HasStairsNorth()) {
                    if ((collided |= this.testCollisionVertical(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render)) && !render) {
                        return true;
                    }
                    if ((collided |= this.testCollisionVertical(fromX, fromY, toX, toY, pt.x + 1, pt.y, true, false, z, render)) && !render) {
                        return true;
                    }
                    if (square.has(IsoObjectType.stairsTN) && (collided |= this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render)) && !render) {
                        return true;
                    }
                }
                if (!square.HasStairsWest()) continue;
                if ((collided |= this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render)) && !render) {
                    return true;
                }
                if ((collided |= this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y + 1, true, false, z, render)) && !render) {
                    return true;
                }
                if (!square.has(IsoObjectType.stairsTW) || !(collided |= this.testCollisionVertical(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render)) || render) continue;
                return true;
            }
            if (square.hasSlopedSurface()) {
                IsoDirections dir = square.getSlopedSurfaceDirection();
                if (dir == IsoDirections.N || dir == IsoDirections.S) {
                    if (!(square.getAdjacentSquare(IsoDirections.W) != null && square.hasIdenticalSlopedSurface(square.getAdjacentSquare(IsoDirections.W)) || !(collided |= this.testCollisionVertical(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render)) || render)) {
                        return true;
                    }
                    if (!(square.getAdjacentSquare(IsoDirections.E) != null && square.hasIdenticalSlopedSurface(square.getAdjacentSquare(IsoDirections.E)) || !(collided |= this.testCollisionVertical(fromX, fromY, toX, toY, pt.x + 1, pt.y, true, false, z, render)) || render)) {
                        return true;
                    }
                    IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
                    if (dir == IsoDirections.N && (squareN == null || square.getSlopedSurfaceHeightMax() != squareN.getSlopedSurfaceHeight(0.5f, 1.0f)) && (collided |= this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render)) && !render) {
                        return true;
                    }
                    IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
                    if (dir == IsoDirections.S && (squareS == null || square.getSlopedSurfaceHeightMax() != squareS.getSlopedSurfaceHeight(0.5f, 0.0f)) && (collided |= this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y + 1, true, false, z, render)) && !render) {
                        return true;
                    }
                }
                if (dir != IsoDirections.W && dir != IsoDirections.E) continue;
                IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
                if (!(squareN != null && square.hasIdenticalSlopedSurface(squareN) || !(collided |= this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render)) || render)) {
                    return true;
                }
                IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
                if (!(squareS != null && square.hasIdenticalSlopedSurface(squareS) || !(collided |= this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y + 1, true, false, z, render)) || render)) {
                    return true;
                }
                IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
                if (dir == IsoDirections.W && (squareW == null || square.getSlopedSurfaceHeightMax() != squareW.getSlopedSurfaceHeight(1.0f, 0.5f)) && (collided |= this.testCollisionVertical(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render)) && !render) {
                    return true;
                }
                IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
                if (dir != IsoDirections.E || squareE != null && square.getSlopedSurfaceHeightMax() == squareE.getSlopedSurfaceHeight(0.0f, 0.5f) || !(collided |= this.testCollisionVertical(fromX, fromY, toX, toY, pt.x + 1, pt.y, true, false, z, render)) || render) continue;
                return true;
            }
            if (square.has(IsoFlagType.collideW) || !ignoreDoors && square.hasBlockedDoor(false)) {
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
                    if (render) {
                        LineDrawer.addLine((float)pt.x - w, (float)pt.y - n, z, (float)pt.x + e, (float)pt.y + 1.0f + s, z, 1.0f, 0.0f, 0.0f, null, false);
                        collided = true;
                    } else {
                        return true;
                    }
                }
            }
            if (!square.has(IsoFlagType.collideN) && (ignoreDoors || !square.hasBlockedDoor(true))) continue;
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
            if (render) {
                LineDrawer.addLine((float)pt.x - w, (float)pt.y - n, z, (float)pt.x + 1.0f + e, (float)pt.y + s, z, 1.0f, 0.0f, 0.0f, null, false);
                collided = true;
                continue;
            }
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
        int chunkMinX = this.sweepAabb.left() / 8 - 1;
        int chunkMinY = this.sweepAabb.top() / 8 - 1;
        int chunkMaxX = (int)Math.ceil((float)this.sweepAabb.right() / 8.0f) + 1;
        int chunkMaxY = (int)Math.ceil((float)this.sweepAabb.bottom() / 8.0f) + 1;
        for (int cy = chunkMinY; cy < chunkMaxY; ++cy) {
            for (int cx = chunkMinX; cx < chunkMaxX; ++cx) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunkForGridSquare(cx * 8, cy * 8, 0);
                if (chunk == null) continue;
                for (int i3 = 0; i3 < chunk.vehicles.size(); ++i3) {
                    BaseVehicle vehicle = chunk.vehicles.get(i3);
                    if (vehicle == ignoreVehicle || vehicle.vehicleId == -1) continue;
                    this.vehiclePoly.init(vehicle.getPoly());
                    this.vehiclePoly.getAABB(this.vehicleAabb);
                    if (!this.vehicleAabb.intersects(this.sweepAabb) || !this.polyVehicleIntersect(this.vehiclePoly, render)) continue;
                    collided = true;
                    if (render) continue;
                    return true;
                }
            }
        }
        return collided;
    }

    boolean testCollisionHorizontal(float fromX, float fromY, float toX, float toY, float ptX, float ptY, boolean bSkipN, boolean bSkipS, float z, boolean render) {
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
        if (this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, ptX - w, ptY - n, ptX + 1.0f + e, ptY + s)) {
            if (render) {
                LineDrawer.addLine(ptX - w, ptY - n, z, ptX + 1.0f + e, ptY + s, z, 1.0f, 0.0f, 0.0f, null, false);
            }
            return true;
        }
        return false;
    }

    boolean testCollisionVertical(float fromX, float fromY, float toX, float toY, float ptX, float ptY, boolean bSkipW, boolean bSkipE, float z, boolean render) {
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
        if (this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, ptX - w, ptY - n, ptX + e, ptY + 1.0f + s)) {
            if (render) {
                LineDrawer.addLine(ptX - w, ptY - n, z, ptX + e, ptY + 1.0f + s, z, 1.0f, 0.0f, 0.0f, null, false);
            }
            return true;
        }
        return false;
    }

    boolean isNotClearClipper(PolygonalMap2 map, float fromX, float fromY, float toX, float toY, int z, BaseVehicle ignoreVehicle, int flags) {
        boolean ignoreDoors = (flags & 1) != 0;
        boolean closeToWalls = (flags & 2) != 0;
        boolean checkCost = (flags & 4) != 0;
        boolean render = (flags & 8) != 0;
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(PZMath.fastfloor(fromX), PZMath.fastfloor(fromY), z);
        if (square != null && square.HasStairs()) {
            return !square.isSameStaircase(PZMath.fastfloor(toX), PZMath.fastfloor(toY), z);
        }
        if (!this.canStandAtClipper(map, toX, toY, z, ignoreVehicle, flags)) {
            if (render) {
                this.drawCircle(toX, toY, z, 0.3f, 1.0f, 0.0f, 0.0f, 1.0f);
            }
            return true;
        }
        return PolygonalMap2.instance.collideWithObstaclesPoly.isNotClear(fromX, fromY, toX, toY, z, render, ignoreVehicle, ignoreDoors, closeToWalls);
    }

    boolean isNotClear(PolygonalMap2 map, float fromX, float fromY, float toX, float toY, int z, BaseVehicle ignoreVehicle, int flags) {
        return this.isNotClearOld(map, fromX, fromY, toX, toY, z, ignoreVehicle, flags);
    }

    Vector2 getCollidepoint(PolygonalMap2 map, float fromX, float fromY, float toX, float toY, int z, BaseVehicle ignoreVehicle, int flags) {
        Point pt;
        int i;
        boolean ignoreDoors = (flags & 1) != 0;
        boolean closeToWalls = (flags & 2) != 0;
        boolean checkCost = (flags & 4) != 0;
        boolean render = (flags & 8) != 0;
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
        this.pts.sort((pt1, pt2) -> PZMath.fastfloor(IsoUtils.DistanceManhatten(fromX, fromY, pt1.x, pt1.y) - IsoUtils.DistanceManhatten(fromX, fromY, pt2.x, pt2.y)));
        if (render) {
            for (i = 0; i < this.pts.size(); ++i) {
                pt = this.pts.get(i);
                LineDrawer.addLine(pt.x, pt.y, z, (float)pt.x + 1.0f, (float)pt.y + 1.0f, z, 1.0f, 1.0f, 0.0f, null, false);
            }
        }
        for (i = 0; i < this.pts.size(); ++i) {
            float s;
            float e;
            float n;
            float w;
            pt = this.pts.get(i);
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(pt.x, pt.y, z);
            if (checkCost && square != null && SquareUpdateTask.getCost(square) > 0) {
                return PolygonalMap2.temp.set((float)pt.x + 0.5f, (float)pt.y + 0.5f);
            }
            if (square == null || square.isSolid() || square.isSolidTrans() && !square.isAdjacentToWindow() && !square.isAdjacentToHoppable() || square.HasStairs() || !(!square.solidFloorCached ? square.TreatAsSolidFloor() : square.solidFloor)) {
                w = 0.3f;
                n = 0.3f;
                e = 0.3f;
                s = 0.3f;
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
                if (render) {
                    LineDrawer.addLine((float)pt.x - w, (float)pt.y - n, z, (float)pt.x + 1.0f + e, (float)pt.y + 1.0f + s, z, 1.0f, 0.0f, 0.0f, null, false);
                }
                return PolygonalMap2.temp.set((float)pt.x + 0.5f, (float)pt.y + 0.5f);
            }
            if (square.has(IsoFlagType.collideW) || !ignoreDoors && square.hasBlockedDoor(false)) {
                w = 0.3f;
                n = 0.3f;
                e = 0.3f;
                s = 0.3f;
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
                    if (render) {
                        LineDrawer.addLine((float)pt.x - w, (float)pt.y - n, z, (float)pt.x + e, (float)pt.y + 1.0f + s, z, 1.0f, 0.0f, 0.0f, null, false);
                    }
                    return PolygonalMap2.temp.set((float)pt.x + (fromX - toX < 0.0f ? -0.5f : 0.5f), (float)pt.y + 0.5f);
                }
            }
            if (!square.has(IsoFlagType.collideN) && (ignoreDoors || !square.hasBlockedDoor(true))) continue;
            w = 0.3f;
            n = 0.3f;
            e = 0.3f;
            s = 0.3f;
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
            if (render) {
                LineDrawer.addLine((float)pt.x - w, (float)pt.y - n, z, (float)pt.x + 1.0f + e, (float)pt.y + s, z, 1.0f, 0.0f, 0.0f, null, false);
            }
            return PolygonalMap2.temp.set((float)pt.x + 0.5f, (float)pt.y + (fromY - toY < 0.0f ? -0.5f : 0.5f));
        }
        return PolygonalMap2.temp.set(toX, toY);
    }

    boolean polyVehicleIntersect(VehiclePoly poly, boolean render) {
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
                if (render) {
                    LineDrawer.addLine(a.x, a.y, 0.0f, b.x, b.y, 0.0f, 1.0f, 0.0f, 0.0f, null, true);
                    LineDrawer.addLine(c.x, c.y, 0.0f, d.x, d.y, 0.0f, 1.0f, 0.0f, 0.0f, null, true);
                }
                intersect = true;
            }
        }
        return intersect;
    }
}


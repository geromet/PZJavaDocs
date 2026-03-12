/*
 * Decompiled with CFR 0.152.
 */
package zombie.vispoly;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjglx.BufferUtils;
import org.lwjglx.opengl.Display;
import zombie.GameProfiler;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.Styles.FloatList;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.VBORenderer;
import zombie.core.rendering.RenderTarget;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.input.GameKeyboard;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LosUtil;
import zombie.iso.PlayerCamera;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.objects.IsoTree;
import zombie.popman.ObjectPool;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;
import zombie.vehicles.ClipperPolygon;

public final class VisibilityPolygon2 {
    private static VisibilityPolygon2 instance;
    boolean useCircle;
    private final Drawer[][] drawers = new Drawer[4][3];
    private final ObjectPool<VisibilityWall> visibilityWallPool = new ObjectPool<VisibilityWall>(VisibilityWall::new);
    int dirtyObstacleCounter;

    public static VisibilityPolygon2 getInstance() {
        if (instance == null) {
            instance = new VisibilityPolygon2();
        }
        return instance;
    }

    private VisibilityPolygon2() {
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.drawers[i][j] = new Drawer();
            }
        }
    }

    public void renderMain(int playerIndex) {
        if (!DebugOptions.instance.fboRenderChunk.renderVisionPolygon.getValue()) {
            return;
        }
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        Drawer drawer = this.drawers[playerIndex][stateIndex];
        drawer.calculateVisibilityPolygon(playerIndex);
        SpriteRenderer.instance.drawGeneric(drawer);
    }

    public void addChunkToWorld(IsoChunk chunk) {
        ++this.dirtyObstacleCounter;
    }

    private static final class Drawer
    extends TextureDraw.GenericDrawer {
        int playerIndex;
        float px;
        float py;
        int pz;
        float visionCone;
        float lookAngleRadians;
        final Vector2 lookVector = new Vector2();
        float px1;
        float py1;
        float px2;
        float py2;
        float dirX1;
        float dirY1;
        float dirX2;
        float dirY2;
        float circleRadius = 40.0f;
        static final ArrayList<Vector3f> circlePoints = new ArrayList();
        final Partition[] partitions = new Partition[8];
        final TFloatArrayList shadows = new TFloatArrayList();
        boolean insideTree;
        int dirtyObstacleCounter = -1;
        boolean clipperPolygonsDirty = true;
        final ArrayList<ClipperPolygon> clipperPolygons = new ArrayList();
        static Texture blurTex;
        static Texture blurDepthTex;
        static TextureFBO blurFBO;
        static Shader blurShader;
        static Shader polygonShader;
        static Shader blitShader;
        static int downscale;
        static float shadowBlurRamp;
        float zoom = 1.0f;
        private final PolygonData polygonData = new PolygonData();
        private final FloatList shadowVerts = new FloatList(FloatList.ExpandStyle.Normal, 10000);
        private static final Vector2 edge;
        private static final Vector3[] angles;

        Drawer() {
            for (int i = 0; i < 8; ++i) {
                this.partitions[i] = new Partition();
                this.partitions[i].minAngle = i * 45;
                this.partitions[i].maxAngle = (i + 1) * 45;
            }
            this.initCirclePoints(this.circleRadius, 32);
        }

        void initCirclePoints(float radius, int segments) {
            this.circleRadius = radius;
            circlePoints.clear();
            float x = 0.0f;
            float y = 0.0f;
            for (int i = 0; i < segments; ++i) {
                double angle = Math.toRadians((double)i * 360.0 / (double)segments);
                double cx = 0.0 + (double)radius * Math.cos(angle);
                double cy = 0.0 + (double)radius * Math.sin(angle);
                circlePoints.add(new Vector3f((float)cx, (float)cy, Vector2.getDirection((float)cx, (float)cy) + (float)Math.PI));
            }
        }

        boolean isDirty(IsoPlayer player) {
            if (this.clipperPolygonsDirty) {
                return true;
            }
            if (this.dirtyObstacleCounter != VisibilityPolygon2.instance.dirtyObstacleCounter) {
                return true;
            }
            if (this.zoom != IsoCamera.frameState.zoom) {
                return true;
            }
            if (this.px != player.getX() || this.py != player.getY() || this.pz != PZMath.fastfloor(player.getZ())) {
                return true;
            }
            float visionCone = LightingJNI.calculateVisionCone(player);
            if (visionCone != this.visionCone) {
                return true;
            }
            float lookAngleRadians = player.getLookAngleRadians();
            BaseVehicle vehicle = player.getVehicle();
            if (vehicle != null && !player.isAiming() && !player.isLookingWhileInVehicle() && vehicle.isDriver(player) && vehicle.getCurrentSpeedKmHour() < -1.0f) {
                lookAngleRadians += (float)Math.PI;
            }
            return lookAngleRadians != this.lookAngleRadians;
        }

        void calculateVisibilityPolygon(int playerIndex) {
            if (DebugOptions.instance.useNewVisibility.getValue()) {
                this.calculateVisibilityPolygonNew(playerIndex);
            } else {
                this.calculateVisibilityPolygonOld(playerIndex);
            }
        }

        void calculateVisibilityPolygonNew(int playerIndex) {
            IsoGridSquare playerSquare;
            this.playerIndex = playerIndex;
            IsoPlayer player = IsoPlayer.players[playerIndex];
            if (!this.isDirty(player)) {
                return;
            }
            this.insideTree = false;
            this.dirtyObstacleCounter = VisibilityPolygon2.instance.dirtyObstacleCounter;
            this.shadowVerts.clear();
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[playerIndex];
            int chunkMapMinX = chunkMap.getWorldXMinTiles();
            int chunkMapMinY = chunkMap.getWorldYMinTiles();
            int chunkMapMaxX = chunkMap.getWorldXMaxTiles();
            int chunkMapMaxY = chunkMap.getWorldYMaxTiles();
            this.px = IsoCamera.frameState.camCharacterX;
            this.py = IsoCamera.frameState.camCharacterY;
            this.pz = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
            this.zoom = IsoCamera.frameState.zoom;
            this.visionCone = LightingJNI.calculateVisionCone(player);
            this.lookAngleRadians = player.getLookAngleRadians();
            player.getLookVector(this.lookVector);
            this.polygonData.update(playerIndex);
            BaseVehicle vehicle = player.getVehicle();
            if (vehicle != null && !player.isAiming() && !player.isLookingWhileInVehicle() && vehicle.isDriver(player) && vehicle.getCurrentSpeedKmHour() < -1.0f) {
                this.lookAngleRadians += (float)Math.PI;
                this.lookVector.rotate((float)Math.PI);
            }
            if ((playerSquare = IsoPlayer.players[playerIndex].getCurrentSquare()) == null) {
                return;
            }
            IsoTree tree = playerSquare.getTree();
            if (tree != null && tree.getProperties() != null && tree.getProperties().has(IsoFlagType.blocksight)) {
                this.insideTree = true;
            }
            if (this.insideTree) {
                this.addFullScreenShadow();
                this.clipperPolygonsDirty = true;
                return;
            }
            this.addViewShadow();
            IsoChunk[] chunks = chunkMap.getChunks();
            for (int c = 0; c < chunks.length; ++c) {
                int i;
                ChunkLevelData chunkLevelData;
                FBORenderLevels renderLevels;
                IsoChunk chunk = chunks[c];
                if (chunk == null || !chunk.loaded || chunk.lightingNeverDone[playerIndex] || !(renderLevels = chunk.getRenderLevels(playerIndex)).isOnScreen(this.pz) || (chunkLevelData = chunk.getVispolyDataForLevel(this.pz)) == null || !chunk.IsOnScreen(false)) continue;
                if (chunkLevelData.adjacentChunkLoadedCounter != chunk.adjacentChunkLoadedCounter) {
                    chunkLevelData.adjacentChunkLoadedCounter = chunk.adjacentChunkLoadedCounter;
                    chunkLevelData.recreate();
                }
                for (i = 0; i < chunkLevelData.allWalls.size(); ++i) {
                    VisibilityWall wall = chunkLevelData.allWalls.get(i);
                    if (!wall.isHorizontal() ? wall.x1 == chunkMapMinX || wall.x1 == chunkMapMaxX : wall.y1 == chunkMapMinY || wall.y1 == chunkMapMaxY) continue;
                    if (!this.isInViewCone(wall.x1, wall.y1, wall.x2, wall.y2)) continue;
                    this.addWallShadow(wall);
                }
                for (i = 0; i < chunkLevelData.solidSquares.size(); ++i) {
                    IsoGridSquare square = chunkLevelData.solidSquares.get(i);
                    if (!this.isInViewCone(square)) continue;
                    this.addSquareShadow(square);
                }
            }
            this.clipperPolygonsDirty = true;
        }

        void calculateVisibilityPolygonOld(int playerIndex) {
            int i;
            this.playerIndex = playerIndex;
            IsoPlayer player = IsoPlayer.players[playerIndex];
            if (!(Core.debug && GameKeyboard.isKeyDown(28) || this.isDirty(player))) {
                return;
            }
            this.clipperPolygonsDirty = true;
            this.dirtyObstacleCounter = VisibilityPolygon2.instance.dirtyObstacleCounter;
            this.visionCone = LightingJNI.calculateVisionCone(player);
            this.lookAngleRadians = player.getLookAngleRadians();
            player.getLookVector(this.lookVector);
            BaseVehicle vehicle = player.getVehicle();
            if (vehicle != null && !player.isAiming() && !player.isLookingWhileInVehicle() && vehicle.isDriver(player) && vehicle.getCurrentSpeedKmHour() < -1.0f) {
                this.lookAngleRadians += (float)Math.PI;
                this.lookVector.rotate((float)Math.PI);
            }
            this.shadows.clear();
            this.insideTree = false;
            HashSet<Segment> segmentHashSet = L_calculateVisibilityPolygon.segmentHashSet;
            segmentHashSet.clear();
            for (int i2 = 0; i2 < this.partitions.length; ++i2) {
                segmentHashSet.addAll(this.partitions[i2].segments);
                this.partitions[i2].segments.clear();
            }
            L_calculateVisibilityPolygon.segmentList.clear();
            L_calculateVisibilityPolygon.segmentList.addAll(segmentHashSet);
            L_calculateVisibilityPolygon.m_segmentPool.releaseAll((List<Segment>)L_calculateVisibilityPolygon.segmentList);
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[playerIndex];
            int chunkMapMinX = chunkMap.getWorldXMinTiles();
            int chunkMapMinY = chunkMap.getWorldYMinTiles();
            int chunkMapMaxX = chunkMap.getWorldXMaxTiles();
            int chunkMapMaxY = chunkMap.getWorldYMaxTiles();
            this.px = IsoCamera.frameState.camCharacterX;
            this.py = IsoCamera.frameState.camCharacterY;
            this.pz = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
            float cone = -this.visionCone;
            float lookAngleRadians = this.lookAngleRadians;
            float direction1 = lookAngleRadians + (float)Math.acos(cone);
            float direction2 = lookAngleRadians - (float)Math.acos(cone);
            this.dirX1 = (float)Math.cos(direction2);
            this.dirY1 = (float)Math.sin(direction2);
            this.dirX2 = (float)Math.cos(direction1);
            this.dirY2 = (float)Math.sin(direction1);
            this.px1 = this.px + this.dirX1 * 1500.0f;
            this.py1 = this.py + this.dirY1 * 1500.0f;
            this.px2 = this.px + this.dirX2 * 1500.0f;
            this.py2 = this.py + this.dirY2 * 1500.0f;
            IsoGridSquare playerSquare = IsoPlayer.players[playerIndex].getCurrentSquare();
            if (playerSquare == null) {
                return;
            }
            IsoTree tree = playerSquare.getTree();
            if (tree != null && tree.getProperties() != null && tree.getProperties().has(IsoFlagType.blocksight)) {
                this.insideTree = true;
                return;
            }
            GameProfiler profiler = GameProfiler.getInstance();
            ArrayList<VisibilityWall> sortedWalls = L_calculateVisibilityPolygon.sortedWalls;
            ArrayList<IsoGridSquare> solidSquares = L_calculateVisibilityPolygon.solidSquares;
            try (GameProfiler.ProfileArea profileArea = profiler.profile("Collect");){
                sortedWalls.clear();
                solidSquares.clear();
                for (int y = 0; y < IsoChunkMap.chunkGridWidth; ++y) {
                    for (int x = 0; x < IsoChunkMap.chunkGridWidth; ++x) {
                        int i3;
                        ChunkLevelData chunkLevelData;
                        FBORenderLevels renderLevels;
                        IsoChunk chunk = chunkMap.getChunk(x, y);
                        if (chunk == null || chunk.lightingNeverDone[playerIndex] || !chunk.loaded || !chunk.IsOnScreen(true) || !(renderLevels = chunk.getRenderLevels(playerIndex)).isOnScreen(this.pz) || (chunkLevelData = chunk.getVispolyDataForLevel(this.pz)) == null) continue;
                        if (Core.debug && GameKeyboard.isKeyDown(28)) {
                            chunkLevelData.invalidate();
                        }
                        if (chunkLevelData.adjacentChunkLoadedCounter != chunk.adjacentChunkLoadedCounter) {
                            chunkLevelData.adjacentChunkLoadedCounter = chunk.adjacentChunkLoadedCounter;
                            chunkLevelData.recreate();
                        }
                        for (i3 = 0; i3 < chunkLevelData.allWalls.size(); ++i3) {
                            VisibilityWall wall = chunkLevelData.allWalls.get(i3);
                            if (!wall.isHorizontal() ? wall.x1 == chunkMapMinX || wall.x1 == chunkMapMaxX : wall.y1 == chunkMapMinY || wall.y1 == chunkMapMaxY) continue;
                            if (!this.isInViewCone(wall.x1, wall.y1, wall.x2, wall.y2)) continue;
                            sortedWalls.add(wall);
                        }
                        for (i3 = 0; i3 < chunkLevelData.solidSquares.size(); ++i3) {
                            IsoGridSquare square = chunkLevelData.solidSquares.get(i3);
                            if (!this.isInViewCone(square)) continue;
                            solidSquares.add(square);
                        }
                    }
                }
            }
            profileArea = profiler.profile("Walls");
            try {
                sortedWalls.sort((o1, o2) -> {
                    float d1 = IsoUtils.DistanceToSquared((float)o1.x1 + (float)(o1.x2 - o1.x1) * 0.5f, (float)o1.y1 + (float)(o1.y2 - o1.y1) * 0.5f, this.px, this.py);
                    float d2 = IsoUtils.DistanceToSquared((float)o2.x1 + (float)(o2.x2 - o2.x1) * 0.5f, (float)o2.y1 + (float)(o2.y2 - o2.y1) * 0.5f, this.px, this.py);
                    return Float.compare(d1, d2);
                });
                for (i = 0; i < sortedWalls.size(); ++i) {
                    VisibilityWall wall = sortedWalls.get(i);
                    float d = 0.0f;
                    if (wall.y1 == wall.y2) {
                        this.addPolygonForLineSegment(this.px, this.py, (float)wall.x1 - 0.0f, wall.y1, (float)wall.x2 + 0.0f, wall.y2);
                        continue;
                    }
                    this.addPolygonForLineSegment(this.px, this.py, wall.x1, (float)wall.y1 - 0.0f, wall.x2, (float)wall.y2 + 0.0f);
                }
            }
            finally {
                if (profileArea != null) {
                    profileArea.close();
                }
            }
            profileArea = profiler.profile("Squares");
            try {
                solidSquares.sort((o1, o2) -> {
                    float d1 = IsoUtils.DistanceToSquared((float)o1.x + 0.5f, (float)o1.y + 0.5f, this.px, this.py);
                    float d2 = IsoUtils.DistanceToSquared((float)o2.x + 0.5f, (float)o2.y + 0.5f, this.px, this.py);
                    return Float.compare(d1, d2);
                });
                for (i = 0; i < solidSquares.size(); ++i) {
                    IsoGridSquare square = solidSquares.get(i);
                    if (Vector2.dot((float)square.x + 0.5f - this.px, (float)square.y - this.py, 0.0f, -1.0f) < 0.0f) {
                        this.addPolygonForLineSegment(this.px, this.py, square.x, square.y, square.x + 1, square.y);
                    }
                    if (Vector2.dot((float)(square.x + 1) - this.px, (float)square.y + 0.5f - this.py, 1.0f, 0.0f) < 0.0f) {
                        this.addPolygonForLineSegment(this.px, this.py, square.x + 1, square.y, square.x + 1, square.y + 1);
                    }
                    if (Vector2.dot((float)square.x + 0.5f - this.px, (float)(square.y + 1) - this.py, 0.0f, 1.0f) < 0.0f) {
                        this.addPolygonForLineSegment(this.px, this.py, square.x + 1, square.y + 1, square.x, square.y + 1);
                    }
                    if (!(Vector2.dot((float)square.x - this.px, (float)square.y + 0.5f - this.py, -1.0f, 0.0f) < 0.0f)) continue;
                    this.addPolygonForLineSegment(this.px, this.py, square.x, square.y + 1, square.x, square.y);
                }
            }
            finally {
                if (profileArea != null) {
                    profileArea.close();
                }
            }
        }

        boolean isCollinear(float ax, float ay, float bx, float by, float cx, float cy) {
            float f = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay);
            return f >= -0.05f && f < 0.05f;
        }

        float getDotWithLookVector(float x, float y) {
            Vector2 v = L_calculateVisibilityPolygon.m_tempVector2_1.set(x - this.px, y - this.py);
            v.normalize();
            return v.dot(this.lookVector);
        }

        boolean lineSegmentsIntersects(float sx, float sy, float dirX, float dirY, float aX, float aY, float bX, float bY) {
            float t2;
            float lineSegmentLength = 1500.0f;
            float dbaX = bX - aX;
            float doaY = sy - aY;
            float dbaY = bY - aY;
            float doaX = sx - aX;
            float invDbaDir = 1.0f / (dbaY * dirX - dbaX * dirY);
            float t = (dbaX * doaY - dbaY * doaX) * invDbaDir;
            return t >= 0.0f && t <= 1500.0f && (t2 = (doaY * dirX - doaX * dirY) * invDbaDir) >= 0.0f && t2 <= 1.0f;
        }

        boolean isInViewCone(float x1, float y1, float x2, float y2) {
            if (VisibilityPolygon2.instance.useCircle && (IsoUtils.DistanceToSquared(x1, y1, this.px, this.py) >= this.circleRadius * this.circleRadius || IsoUtils.DistanceToSquared(x2, y2, this.px, this.py) >= this.circleRadius * this.circleRadius)) {
                return false;
            }
            float dot1 = this.getDotWithLookVector(x1, y1);
            float dot2 = this.getDotWithLookVector(x2, y2);
            float dot3 = this.getDotWithLookVector(x1 + (x2 - x1) * 0.5f, y1 + (y2 - y1) * 0.5f);
            if (Float.compare(dot1, -this.visionCone) >= 0 || Float.compare(dot2, -this.visionCone) >= 0 || Float.compare(dot3, -this.visionCone) >= 0) {
                return true;
            }
            return this.lineSegmentsIntersects(this.px, this.py, this.dirX1, this.dirY1, x1, y1, x2, y2) || this.lineSegmentsIntersects(this.px, this.py, this.dirX2, this.dirY2, x1, y1, x2, y2);
        }

        boolean isInViewCone(IsoGridSquare square) {
            return this.isInViewCone(square.x, square.y, square.x + 1, square.y) || this.isInViewCone(square.x + 1, square.y, square.x + 1, square.y + 1) || this.isInViewCone(square.x + 1, square.y + 1, square.x, square.y + 1) || this.isInViewCone(square.x, square.y + 1, square.x, square.y);
        }

        void addPolygonForLineSegment(float px, float py, float x1, float y1, float x2, float y2) {
            this.addPolygonForLineSegment(px, py, x1, y1, x2, y2, false);
        }

        void addPolygonForLineSegment(float px, float py, float x1, float y1, float x2, float y2, boolean bCollinearHack) {
            boolean clockwise;
            if (Core.debug) {
                // empty if block
            }
            boolean bDrawLines = false;
            float angle1 = Vector2.getDirection(x1 - px, y1 - py) * 57.295776f;
            float angle2 = Vector2.getDirection(x2 - px, y2 - py) * 57.295776f;
            if (!bCollinearHack && this.isCollinear(px, py, x1, y1, x2, y2) && Math.abs(angle1 - angle2) < 10.0f) {
                if (bDrawLines) {
                    LineDrawer.addLine(x1, y1, (float)this.pz, x2, y2, (float)this.pz, 1.0f, 1.0f, 0.0f, 1.0f);
                }
                Vector2 perp = L_calculateVisibilityPolygon.m_tempVector2_1.set(x2 - x1, y2 - y1);
                perp.rotate(1.5707964f);
                perp.normalize();
                float d = 0.025f;
                if (IsoUtils.DistanceToSquared(px, py, x1, y1) < IsoUtils.DistanceToSquared(px, py, x2, y2)) {
                    this.addPolygonForLineSegment(px, py, x1 - perp.x * 0.025f, y1 - perp.y * 0.025f, x1 + perp.x * 0.025f, y1 + perp.y * 0.025f, true);
                } else {
                    this.addPolygonForLineSegment(px, py, x2 - perp.x * 0.025f, y2 - perp.y * 0.025f, x2 + perp.x * 0.025f, y2 + perp.y * 0.025f, true);
                }
                return;
            }
            Segment segment = L_calculateVisibilityPolygon.m_segmentPool.alloc();
            segment.a.set(x1, y1, x1 - px, y1 - py, (Vector2.getDirection(x1 - px, y1 - py) + (float)Math.PI) * 57.295776f, IsoUtils.DistanceToSquared(x1, y1, px, py));
            segment.b.set(x2, y2, x2 - px, y2 - py, (Vector2.getDirection(x2 - px, y2 - py) + (float)Math.PI) * 57.295776f, IsoUtils.DistanceToSquared(x2, y2, px, py));
            int partitionMin = PZMath.fastfloor(segment.minAngle() / 45.0f);
            int partitionMax = PZMath.fastfloor(segment.maxAngle() / 45.0f);
            if (segment.maxAngle() - segment.minAngle() > 180.0f) {
                partitionMin = PZMath.fastfloor(segment.maxAngle() / 45.0f);
                partitionMax = PZMath.fastfloor((segment.minAngle() + 360.0f) / 45.0f);
            }
            boolean added = true;
            for (int i = partitionMin; i <= partitionMax; ++i) {
                int modI = i % 8;
                if (this.partitions[modI].addSegment(segment)) continue;
                --i;
                while (i >= partitionMin) {
                    modI = i % 8;
                    this.partitions[modI].segments.remove(segment);
                    --i;
                }
                added = false;
                break;
            }
            if (!added) {
                if (bDrawLines) {
                    LineDrawer.addLine(segment.a.x, segment.a.y, (float)this.pz, segment.b.x, segment.b.y, (float)this.pz, 1.0f, 0.0f, 0.0f, 1.0f);
                }
                L_calculateVisibilityPolygon.m_segmentPool.release(segment);
                return;
            }
            if (bDrawLines) {
                LineDrawer.addLine(segment.a.x, segment.a.y, (float)this.pz, segment.b.x, segment.b.y, (float)this.pz, 1.0f, 1.0f, 1.0f, 1.0f);
            }
            float radius = 1500.0f;
            float x3 = x1 + segment.a.dirX * 1500.0f;
            float y3 = y1 + segment.a.dirY * 1500.0f;
            float x4 = x2 + segment.b.dirX * 1500.0f;
            float y4 = y2 + segment.b.dirY * 1500.0f;
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(this.playerIndex);
            int xMin = chunkMap.getWorldXMinTiles();
            int yMin = chunkMap.getWorldYMinTiles();
            int xMax = chunkMap.getWorldXMaxTiles();
            int yMax = chunkMap.getWorldYMaxTiles();
            Vector2 intersection = L_calculateVisibilityPolygon.m_tempVector2_1;
            if (VisibilityPolygon2.instance.useCircle) {
                Drawer.intersectLineSegmentWithCircle(x3, y3, px, py, this.circleRadius, intersection);
                x3 = intersection.x;
                y3 = intersection.y;
                Drawer.intersectLineSegmentWithCircle(x4, y4, px, py, this.circleRadius, intersection);
                x4 = intersection.x;
                y4 = intersection.y;
            } else {
                if (Drawer.intersectLineSegmentWithAABB(x1, y1, x3, y3, xMin, yMin, xMax, yMax, intersection)) {
                    x3 = intersection.x;
                    y3 = intersection.y;
                }
                if (Drawer.intersectLineSegmentWithAABB(x2, y2, x4, y4, xMin, yMin, xMax, yMax, intersection)) {
                    x4 = intersection.x;
                    y4 = intersection.y;
                }
            }
            float sum = 0.0f;
            sum += (x2 - x1) * (y2 + y1);
            sum += (x4 - x2) * (y4 + y2);
            sum += (x3 - x4) * (y3 + y4);
            boolean bl = clockwise = (sum += (x1 - x3) * (y1 + y3)) > 0.0f;
            if (clockwise) {
                this.shadows.add(x2);
                this.shadows.add(y2);
                this.shadows.add(x1);
                this.shadows.add(y1);
                this.shadows.add(x3);
                this.shadows.add(y3);
                this.shadows.add(x4);
                this.shadows.add(y4);
            } else {
                this.shadows.add(x1);
                this.shadows.add(y1);
                this.shadows.add(x2);
                this.shadows.add(y2);
                this.shadows.add(x4);
                this.shadows.add(y4);
                this.shadows.add(x3);
                this.shadows.add(y3);
            }
            if (bDrawLines) {
                LineDrawer.addLine(px, py, (float)this.pz, x3, y3, (float)this.pz, 1.0f, 1.0f, 1.0f, 0.5f);
            }
            if (bDrawLines) {
                LineDrawer.addLine(px, py, (float)this.pz, x4, y4, (float)this.pz, 1.0f, 1.0f, 1.0f, 0.5f);
            }
        }

        static boolean intersectLineSegments(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, Vector2 intersection) {
            float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
            if (d == 0.0f) {
                return false;
            }
            float yd = y1 - y3;
            float xd = x1 - x3;
            float ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d;
            if (ua < 0.0f || ua > 1.0f) {
                return false;
            }
            float ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d;
            if (ub < 0.0f || ub > 1.0f) {
                return false;
            }
            if (intersection != null) {
                intersection.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua);
            }
            return true;
        }

        static boolean intersectLineSegmentWithAABB(float x1, float y1, float x2, float y2, float rx1, float ry1, float rx2, float ry2, Vector2 intersection) {
            if (Drawer.intersectLineSegments(x1, y1, x2, y2, rx1, ry1, rx2, ry1, intersection)) {
                return true;
            }
            if (Drawer.intersectLineSegments(x1, y1, x2, y2, rx2, ry1, rx2, ry2, intersection)) {
                return true;
            }
            if (Drawer.intersectLineSegments(x1, y1, x2, y2, rx2, ry2, rx1, ry2, intersection)) {
                return true;
            }
            return Drawer.intersectLineSegments(x1, y1, x2, y2, rx1, ry2, rx1, ry1, intersection);
        }

        static int intersectLineSegmentWithAABBEdge(float x1, float y1, float x2, float y2, float rx1, float ry1, float rx2, float ry2, Vector2 intersection) {
            if (Drawer.intersectLineSegments(x1, y1, x2, y2, rx1, ry1, rx2, ry1, intersection)) {
                return 1;
            }
            if (Drawer.intersectLineSegments(x1, y1, x2, y2, rx2, ry1, rx2, ry2, intersection)) {
                return 2;
            }
            if (Drawer.intersectLineSegments(x1, y1, x2, y2, rx2, ry2, rx1, ry2, intersection)) {
                return 3;
            }
            if (Drawer.intersectLineSegments(x1, y1, x2, y2, rx1, ry2, rx1, ry1, intersection)) {
                return 4;
            }
            return 0;
        }

        static void intersectLineSegmentWithCircle(float x2, float y2, float cx, float cy, float radius, Vector2 intersection) {
            intersection.set(x2 - cx, y2 - cy);
            intersection.setLength(radius);
            intersection.x += cx;
            intersection.y += cy;
        }

        private void initBlur() {
            int w = Core.width / downscale;
            int h = Core.height / downscale;
            if (blurTex == null || blurTex.getWidth() != w || blurTex.getHeight() != h) {
                if (blurTex != null) {
                    blurTex.destroy();
                }
                if (blurDepthTex != null) {
                    blurDepthTex.destroy();
                }
                int format = 6403;
                int internalFormat = Display.capabilities.OpenGL30 ? 33321 : 6403;
                blurTex = new Texture(w, h, 16, 6403, internalFormat);
                blurTex.setNameOnly("visBlur");
                blurDepthTex = new Texture(w, h, 512);
                blurDepthTex.setNameOnly("visBlurDepth");
                if (blurFBO == null) {
                    blurFBO = new TextureFBO(blurTex, blurDepthTex, false);
                } else {
                    blurFBO.startDrawing(false, false);
                    blurFBO.attach(blurTex, 36064);
                    blurFBO.attach(blurDepthTex, 36096);
                    blurFBO.endDrawing();
                }
            }
            if (blurShader == null) {
                blurShader = new Shader("visibilityBlur");
            }
            if (polygonShader == null) {
                polygonShader = new Shader("visPolygon");
            }
            if (blitShader == null) {
                blitShader = new Shader("blitSimple");
            }
            this.polygonData.init();
        }

        private void addFullScreenShadow() {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(this.playerIndex);
            float xMin = chunkMap.getWorldXMinTiles();
            float yMin = chunkMap.getWorldYMinTiles();
            float xMax = chunkMap.getWorldXMaxTiles();
            float yMax = chunkMap.getWorldYMaxTiles();
            this.shadowVerts.add(xMin);
            this.shadowVerts.add(yMin);
            this.shadowVerts.add(0.0f);
            this.shadowVerts.add(xMax);
            this.shadowVerts.add(yMin);
            this.shadowVerts.add(0.0f);
            this.shadowVerts.add(xMax);
            this.shadowVerts.add(yMax);
            this.shadowVerts.add(0.0f);
            this.shadowVerts.add(xMin);
            this.shadowVerts.add(yMin);
            this.shadowVerts.add(0.0f);
            this.shadowVerts.add(xMax);
            this.shadowVerts.add(yMax);
            this.shadowVerts.add(0.0f);
            this.shadowVerts.add(xMin);
            this.shadowVerts.add(yMax);
            this.shadowVerts.add(0.0f);
        }

        private float getLen(float x1, float y1, float x2, float y2) {
            float x = x2 - x1;
            float y = y2 - y1;
            return PZMath.sqrt(x * x + y * y);
        }

        private void addViewShadow() {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(this.playerIndex);
            int xMin = chunkMap.getWorldXMinTiles();
            int yMin = chunkMap.getWorldYMinTiles();
            int xMax = chunkMap.getWorldXMaxTiles();
            int yMax = chunkMap.getWorldYMaxTiles();
            float offset = (float)Math.acos(-this.visionCone);
            float dirL = this.lookAngleRadians - offset;
            float dirR = this.lookAngleRadians + offset;
            float angleXL = (float)Math.cos(dirL);
            float angleYL = (float)Math.sin(dirL);
            float angleXR = (float)Math.cos(dirR);
            float angleYR = (float)Math.sin(dirR);
            float pxL = this.px + angleXL * 1500.0f;
            float pyL = this.py + angleYL * 1500.0f;
            float pxR = this.px + angleXR * 1500.0f;
            float pyR = this.py + angleYR * 1500.0f;
            Drawer.intersectLineSegmentWithAABBEdge(this.px, this.py, pxR, pyR, xMin, yMin, xMax, yMax, edge);
            pxR = Drawer.edge.x;
            pyR = Drawer.edge.y;
            Drawer.intersectLineSegmentWithAABBEdge(this.px, this.py, pxL, pyL, xMin, yMin, xMax, yMax, edge);
            pxL = Drawer.edge.x;
            pyL = Drawer.edge.y;
            float angleL = Vector2.getDirection(pxL - this.px, pyL - this.py);
            float angleR = Vector2.getDirection(pxR - this.px, pyR - this.py);
            float angleTL = Vector2.getDirection((float)xMin - this.px, (float)yMin - this.py);
            float angleTR = Vector2.getDirection((float)xMax - this.px, (float)yMin - this.py);
            float angleBR = Vector2.getDirection((float)xMax - this.px, (float)yMax - this.py);
            float angleBL = Vector2.getDirection((float)xMin - this.px, (float)yMax - this.py);
            angles[0].set(xMin, yMin, angleTL);
            angles[1].set(xMax, yMin, angleTR);
            angles[2].set(xMax, yMax, angleBR);
            angles[3].set(xMin, yMax, angleBL);
            this.shadowVerts.add(this.px);
            this.shadowVerts.add(this.py);
            this.shadowVerts.add(0.0f);
            this.shadowVerts.add(pxR);
            this.shadowVerts.add(pyR);
            this.shadowVerts.add(this.getLen(this.px, this.py, pxR, pyR) * shadowBlurRamp);
            if (angleL > angleR) {
                for (int i = 0; i < 4; ++i) {
                    if (!(Drawer.angles[i].z > angleR) || !(Drawer.angles[i].z < angleL)) continue;
                    float len = this.getLen(this.px, this.py, Drawer.angles[i].x, Drawer.angles[i].y) * shadowBlurRamp;
                    this.shadowVerts.add(Drawer.angles[i].x);
                    this.shadowVerts.add(Drawer.angles[i].y);
                    this.shadowVerts.add(len);
                    this.shadowVerts.add(this.px);
                    this.shadowVerts.add(this.py);
                    this.shadowVerts.add(0.0f);
                    this.shadowVerts.add(Drawer.angles[i].x);
                    this.shadowVerts.add(Drawer.angles[i].y);
                    this.shadowVerts.add(len);
                }
            } else {
                float len;
                int i;
                for (i = 0; i < 4; ++i) {
                    if (!(Drawer.angles[i].z > angleR)) continue;
                    len = this.getLen(this.px, this.py, Drawer.angles[i].x, Drawer.angles[i].y) * shadowBlurRamp;
                    this.shadowVerts.add(Drawer.angles[i].x);
                    this.shadowVerts.add(Drawer.angles[i].y);
                    this.shadowVerts.add(len);
                    this.shadowVerts.add(this.px);
                    this.shadowVerts.add(this.py);
                    this.shadowVerts.add(0.0f);
                    this.shadowVerts.add(Drawer.angles[i].x);
                    this.shadowVerts.add(Drawer.angles[i].y);
                    this.shadowVerts.add(len);
                }
                for (i = 0; i < 4; ++i) {
                    if (!(Drawer.angles[i].z < angleL)) continue;
                    len = this.getLen(this.px, this.py, Drawer.angles[i].x, Drawer.angles[i].y) * shadowBlurRamp;
                    this.shadowVerts.add(Drawer.angles[i].x);
                    this.shadowVerts.add(Drawer.angles[i].y);
                    this.shadowVerts.add(len);
                    this.shadowVerts.add(this.px);
                    this.shadowVerts.add(this.py);
                    this.shadowVerts.add(0.0f);
                    this.shadowVerts.add(Drawer.angles[i].x);
                    this.shadowVerts.add(Drawer.angles[i].y);
                    this.shadowVerts.add(len);
                }
            }
            this.shadowVerts.add(pxL);
            this.shadowVerts.add(pyL);
            this.shadowVerts.add(this.getLen(this.px, this.py, pxL, pyL) * shadowBlurRamp);
        }

        private void addLineShadow(float x1, float y1, float x2, float y2) {
            float x1d = x1 - this.px;
            float y1d = y1 - this.py;
            float x2d = x2 - this.px;
            float y2d = y2 - this.py;
            float sqr1 = x1d * x1d + y1d * y1d;
            float mag1 = PZMath.sqrt(PZMath.max(0.01f, sqr1));
            float sqr2 = x2d * x2d + y2d * y2d;
            float mag2 = PZMath.sqrt(PZMath.max(0.01f, sqr2));
            float x1n = x1d / mag1;
            float y1n = y1d / mag1;
            float x2n = x2d / mag2;
            float y2n = y2d / mag2;
            float offsetDist = 70.0f;
            float len = 70.0f * shadowBlurRamp;
            this.shadowVerts.add(x1);
            this.shadowVerts.add(y1);
            this.shadowVerts.add(0.0f);
            this.shadowVerts.add(x2);
            this.shadowVerts.add(y2);
            this.shadowVerts.add(0.0f);
            this.shadowVerts.add(x1 + x1n * 70.0f);
            this.shadowVerts.add(y1 + y1n * 70.0f);
            this.shadowVerts.add(len);
            this.shadowVerts.add(x2);
            this.shadowVerts.add(y2);
            this.shadowVerts.add(0.0f);
            this.shadowVerts.add(x1 + x1n * 70.0f);
            this.shadowVerts.add(y1 + y1n * 70.0f);
            this.shadowVerts.add(len);
            this.shadowVerts.add(x2 + x2n * 70.0f);
            this.shadowVerts.add(y2 + y2n * 70.0f);
            this.shadowVerts.add(len);
        }

        private void addWallShadow(VisibilityWall wall) {
            this.addLineShadow(wall.x1, wall.y1, wall.x2, wall.y2);
        }

        private void addSquareShadow(IsoGridSquare square) {
            if ((float)square.x < this.px) {
                this.addLineShadow(square.x + 1, square.y, square.x + 1, square.y + 1);
            } else {
                this.addLineShadow(square.x, square.y, square.x, square.y + 1);
            }
            if ((float)square.y < this.py) {
                this.addLineShadow(square.x, square.y + 1, square.x + 1, square.y + 1);
            } else {
                this.addLineShadow(square.x, square.y, square.x + 1, square.y);
            }
        }

        @Override
        public void render() {
            if (DebugOptions.instance.useNewVisibility.getValue()) {
                try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("renderNew");){
                    this.renderNew();
                }
            } else {
                this.renderOld();
            }
        }

        public void renderNew() {
            this.initBlur();
            this.clipperPolygonsDirty = false;
            this.polygonData.camera = SpriteRenderer.instance.getRenderingPlayerCamera(this.playerIndex);
            GL11.glDepthFunc(515);
            GL11.glDepthMask(true);
            GL11.glEnable(3042);
            GL11.glEnable(2929);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(2884);
            GL11.glDisable(3008);
            this.renderPolygons();
            GL11.glDepthMask(false);
            if (!DebugOptions.instance.displayVisibilityPolygon.getValue()) {
                this.renderToScreen();
            }
            GL11.glEnable(3008);
            GL11.glDepthFunc(519);
            GLStateRenderThread.restore();
            SpriteRenderer.ringBuffer.restoreVbos = true;
            int screenLeft = IsoCamera.getScreenLeft(this.playerIndex);
            int screenTop = IsoCamera.getScreenTop(this.playerIndex);
            int screenWidth = IsoCamera.getScreenWidth(this.playerIndex);
            int screenHeight = IsoCamera.getScreenHeight(this.playerIndex);
            GL11.glViewport(screenLeft, screenTop, screenWidth, screenHeight);
        }

        public void renderOld() {
            Clipper clipper = L_render.m_clipper;
            ByteBuffer clipperBuf = L_render.clipperBuf;
            GameProfiler profiler = GameProfiler.getInstance();
            if (this.clipperPolygons.isEmpty() || this.clipperPolygonsDirty) {
                try (GameProfiler.ProfileArea profileArea = profiler.profile("Update Dirty");){
                    this.releaseClipperPolygons(this.clipperPolygons);
                    IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(this.playerIndex);
                    int xMin = chunkMap.getWorldXMinTiles();
                    int yMin = chunkMap.getWorldYMinTiles();
                    int xMax = chunkMap.getWorldXMaxTiles();
                    int yMax = chunkMap.getWorldYMaxTiles();
                    L_addShadowToClipper.angle1cm = Vector2.getDirection((float)xMin - this.px, (float)yMin - this.py) + (float)Math.PI;
                    L_addShadowToClipper.angle2cm = Vector2.getDirection((float)xMin - this.px, (float)yMax - this.py) + (float)Math.PI;
                    L_addShadowToClipper.angle3cm = Vector2.getDirection((float)xMax - this.px, (float)yMax - this.py) + (float)Math.PI;
                    L_addShadowToClipper.angle4cm = Vector2.getDirection((float)xMax - this.px, (float)yMin - this.py) + (float)Math.PI;
                    clipper.clear();
                    if (this.insideTree) {
                        this.addShadowToClipper(clipper, clipperBuf, xMin, yMin, xMax, yMin, xMax, yMax, xMin, yMax);
                    } else {
                        this.addViewConeToClipper(clipper, clipperBuf);
                        for (int i = 0; i < this.shadows.size(); i += 8) {
                            int n = i;
                            float x1 = this.shadows.getQuick(n++);
                            float y1 = this.shadows.getQuick(n++);
                            float x2 = this.shadows.getQuick(n++);
                            float y2 = this.shadows.getQuick(n++);
                            float x3 = this.shadows.getQuick(n++);
                            float y3 = this.shadows.getQuick(n++);
                            float x4 = this.shadows.getQuick(n++);
                            float y4 = this.shadows.getQuick(n);
                            this.addShadowToClipper(clipper, clipperBuf, x1, y1, x2, y2, x3, y3, x4, y4);
                        }
                    }
                    int numPolys = clipper.generatePolygons();
                    this.releaseClipperPolygons(L_render.clipperPolygons1);
                    this.getClipperPolygons(clipper, numPolys, L_render.clipperPolygons1);
                    for (int i = 0; i < L_render.clipperPolygons1.size(); ++i) {
                        ClipperPolygon clipperPolygon1 = L_render.clipperPolygons1.get(i);
                        ClipperPolygon copy = clipperPolygon1.makeCopy(L_render.clipperPolygonPool, L_render.floatArrayListPool);
                        this.clipperPolygons.add(copy);
                    }
                    this.clipperPolygonsDirty = false;
                }
            } else {
                this.releaseClipperPolygons(L_render.clipperPolygons1);
                for (int i = 0; i < this.clipperPolygons.size(); ++i) {
                    ClipperPolygon clipperPolygon1 = this.clipperPolygons.get(i);
                    ClipperPolygon copy = clipperPolygon1.makeCopy(L_render.clipperPolygonPool, L_render.floatArrayListPool);
                    L_render.clipperPolygons1.add(copy);
                }
            }
            PlayerCamera camera = SpriteRenderer.instance.getRenderingPlayerCamera(this.playerIndex);
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.startRun(vbor.formatPositionColorUvDepth);
            boolean triangulate = true;
            vbor.setMode(4);
            vbor.setDepthTest(true);
            GL11.glDepthFunc(515);
            GL11.glDepthMask(false);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            if (DebugOptions.instance.displayVisibilityPolygon.getValue()) {
                GL11.glPolygonMode(1032, 6913);
            }
            vbor.setTextureID(Texture.getWhite().getTextureId());
            int numSteps = 5;
            ArrayList<ClipperPolygon> polygons = L_render.clipperPolygons3;
            float alphaPerStep = 0.0125f;
            L_render.triangleIndex = 0;
            for (int step = 0; step < 5; ++step) {
                boolean bBlurEdges = true;
                try (GameProfiler.ProfileArea profileArea = profiler.profile("Blur");){
                    ClipperPolygon polygon1;
                    int i;
                    if (step == 4) {
                        for (i = 0; i < L_render.clipperPolygons1.size(); ++i) {
                            polygon1 = L_render.clipperPolygons1.get(i);
                            clipper.clear();
                            this.addClipperPolygon(clipper, clipperBuf, polygon1, false);
                            int numPolys3 = clipper.generatePolygons();
                            this.triangulatePolygons(numPolys3, clipper, clipperBuf, camera, 0.0125f * (float)(step + 1));
                        }
                        continue;
                    }
                    polygons.clear();
                    for (i = 0; i < L_render.clipperPolygons1.size(); ++i) {
                        polygon1 = L_render.clipperPolygons1.get(i);
                        clipper.clear();
                        this.addClipperPolygon(clipper, clipperBuf, polygon1, false);
                        int numPolys2 = clipper.generatePolygons(-0.06999999999999999, 3);
                        this.releaseClipperPolygons(L_render.clipperPolygons2);
                        this.getClipperPolygons(clipper, numPolys2, L_render.clipperPolygons2);
                        clipper.clear();
                        this.addClipperPolygon(clipper, clipperBuf, polygon1, false);
                        this.addClipperPolygons(clipper, clipperBuf, L_render.clipperPolygons2, true);
                        int numPolys3 = clipper.generatePolygons();
                        this.triangulatePolygons(numPolys3, clipper, clipperBuf, camera, 0.0125f * (float)(step + 1));
                        polygons.addAll(L_render.clipperPolygons2);
                        L_render.clipperPolygons2.clear();
                    }
                    this.releaseClipperPolygons(L_render.clipperPolygons1);
                    L_render.clipperPolygons1.addAll(polygons);
                    continue;
                }
            }
            vbor.endRun();
            vbor.flush();
            GL11.glDepthFunc(519);
            GL11.glPolygonMode(1032, 6914);
            GLStateRenderThread.restore();
        }

        private void updatePolygons(int start, int end) {
            this.polygonData.reset();
            float[] vertices = this.shadowVerts.array();
            for (int i = start; i < end; i += 3) {
                float x = vertices[i];
                float y = vertices[i + 1];
                float b = vertices[i + 2];
                this.polygonData.addVertex(x, y, b);
            }
            this.polygonData.data.flip();
        }

        private void renderPolygons() {
            GL11.glPushClientAttrib(2);
            if (DebugOptions.instance.displayVisibilityPolygon.getValue()) {
                GL11.glPolygonMode(1032, 6913);
            }
            polygonShader.Start();
            if (!DebugOptions.instance.displayVisibilityPolygon.getValue()) {
                GL11.glBlendFunc(1, 0);
                GL11.glViewport(0, 0, blurFBO.getWidth(), blurFBO.getHeight());
                blurFBO.startDrawing(true, true);
            }
            VertexBufferObject.setModelViewProjection(polygonShader.getProgram());
            int maxSlots = 9216;
            for (int i = 0; i < this.shadowVerts.size(); i += 9216) {
                int count = PZMath.min(9216, this.shadowVerts.size() - i);
                this.updatePolygons(i, i + count);
                this.polygonData.draw();
            }
            if (!DebugOptions.instance.displayVisibilityPolygon.getValue()) {
                blurFBO.endDrawing();
                GL11.glViewport(0, 0, Core.width, Core.height);
                GL11.glBlendFunc(770, 771);
            }
            polygonShader.End();
            if (DebugOptions.instance.displayVisibilityPolygon.getValue()) {
                GL11.glPolygonMode(1032, 6914);
            }
            GL11.glPopClientAttrib();
        }

        private void renderToScreen() {
            if (DebugOptions.instance.previewTiles.getValue()) {
                GL11.glDepthFunc(519);
                blitShader.Start();
                blitShader.getProgram().setValue("Tex", blurTex, 0);
                RenderTarget.DrawFullScreenQuad();
                blitShader.End();
            } else {
                GameProfiler profiler = GameProfiler.getInstance();
                try (GameProfiler.ProfileArea profileArea = profiler.profile("BlurShader");){
                    TextureFBO osBuffer = Core.getInstance().getOffscreenBuffer(this.playerIndex);
                    int osWidth = osBuffer == null ? Core.width : osBuffer.getWidth();
                    int osHeight = osBuffer == null ? Core.height : osBuffer.getHeight();
                    int divW = IsoPlayer.numPlayers > 1 ? 2 : 1;
                    int divH = IsoPlayer.numPlayers > 2 ? 2 : 1;
                    int offscreenWidth = osWidth / divW;
                    int offscreenHeight = osHeight / divH;
                    int screenLeft = IsoCamera.getScreenLeft(this.playerIndex);
                    int screenTop = IsoCamera.getScreenTop(this.playerIndex);
                    int screenWidth = IsoCamera.getScreenWidth(this.playerIndex);
                    int screenHeight = IsoCamera.getScreenHeight(this.playerIndex);
                    GL11.glViewport(screenLeft, screenTop, screenWidth, screenHeight);
                    blurShader.Start();
                    ShaderProgram blurProgram = blurShader.getProgram();
                    L_render.vector2.set(screenWidth, screenHeight);
                    blurProgram.setValue("screenSize", L_render.vector2);
                    L_render.vector2.set(screenLeft, screenTop);
                    blurProgram.setValue("displayOrigin", L_render.vector2);
                    L_render.vector2.set(offscreenWidth, offscreenHeight);
                    blurProgram.setValue("displaySize", L_render.vector2);
                    L_render.vector2.set(blurTex.getWidth(), blurTex.getHeight());
                    blurProgram.setValue("texSize", L_render.vector2);
                    L_render.vector2.set(blurTex.getWidthHW(), blurTex.getHeightHW());
                    blurProgram.setValue("TextureSize", L_render.vector2);
                    blurProgram.setValue("tex", blurTex, 0);
                    blurProgram.setValue("depth", blurDepthTex, 1);
                    blurProgram.setValue("zoom", this.zoom);
                    blurProgram.setValue("opacityScale", PerformanceSettings.viewConeOpacity);
                    try (GameProfiler.ProfileArea profileArea2 = profiler.profile("Render Quad");){
                        RenderTarget.DrawFullScreenQuad();
                    }
                    blurShader.End();
                }
            }
        }

        void addViewConeToClipper(Clipper clipper, ByteBuffer clipperBuf) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(this.playerIndex);
            int xMin = chunkMap.getWorldXMinTiles();
            int yMin = chunkMap.getWorldYMinTiles();
            int xMax = chunkMap.getWorldXMaxTiles();
            int yMax = chunkMap.getWorldYMaxTiles();
            float cone = -this.visionCone;
            float lookAngleRadians = this.lookAngleRadians;
            float direction1 = lookAngleRadians + (float)Math.acos(cone);
            float direction2 = lookAngleRadians - (float)Math.acos(cone);
            float px1 = this.px + (float)Math.cos(direction2) * 1500.0f;
            float py1 = this.py + (float)Math.sin(direction2) * 1500.0f;
            float px2 = this.px + (float)Math.cos(direction1) * 1500.0f;
            float py2 = this.py + (float)Math.sin(direction1) * 1500.0f;
            Vector2 vector2 = L_render.vector2;
            if (VisibilityPolygon2.instance.useCircle) {
                Drawer.intersectLineSegmentWithCircle(px1, py1, this.px, this.py, this.circleRadius, vector2);
                px1 = vector2.x;
                py1 = vector2.y;
                Drawer.intersectLineSegmentWithCircle(px2, py2, this.px, this.py, this.circleRadius, vector2);
                px2 = vector2.x;
                py2 = vector2.y;
            } else {
                Drawer.intersectLineSegmentWithAABB(this.px, this.py, px1, py1, xMin, yMin, xMax, yMax, vector2);
                px1 = vector2.x;
                py1 = vector2.y;
                Drawer.intersectLineSegmentWithAABB(this.px, this.py, px2, py2, xMin, yMin, xMax, yMax, vector2);
                px2 = vector2.x;
                py2 = vector2.y;
            }
            float angle1 = Vector2.getDirection(px1 - this.px, py1 - this.py) + (float)Math.PI;
            float angle2 = Vector2.getDirection(px2 - this.px, py2 - this.py) + (float)Math.PI;
            Vector3f v1 = L_render.v1.set(px1, py1, angle1);
            Vector3f v2 = L_render.v2.set(px2, py2, angle2);
            if (VisibilityPolygon2.instance.useCircle) {
                Vector3f v3;
                int i;
                for (int i2 = 0; i2 < circlePoints.size(); ++i2) {
                    Vector3f v32 = circlePoints.get(i2);
                    v32.x += this.px;
                    v32.y += this.py;
                }
                ArrayList<Vector3f> sorted2 = L_render.vs;
                sorted2.clear();
                sorted2.add(v1);
                sorted2.add(v2);
                if (angle1 > angle2) {
                    for (i = 0; i < circlePoints.size(); ++i) {
                        v3 = circlePoints.get(i);
                        if (Float.compare(v3.z, angle1) >= 0 || Float.compare(v3.z, angle2) <= 0) continue;
                        sorted2.add(v3);
                    }
                } else {
                    for (i = 0; i < circlePoints.size(); ++i) {
                        v3 = circlePoints.get(i);
                        if (Float.compare(v3.z, angle1) >= 0 && Float.compare(v3.z, angle2) <= 0) continue;
                        sorted2.add(v3);
                    }
                }
                sorted2.sort((o1, o2) -> Float.compare(o1.z, o2.z));
                sorted2.add(sorted2.indexOf(v2), L_addShadowToClipper.v1.set(this.px, this.py, 0.0f));
                clipperBuf.clear();
                for (i = 0; i < sorted2.size(); ++i) {
                    clipperBuf.putFloat(sorted2.get((int)i).x);
                    clipperBuf.putFloat(sorted2.get((int)i).y);
                }
                clipper.addPath(sorted2.size(), clipperBuf, false);
                for (i = 0; i < circlePoints.size(); ++i) {
                    v3 = circlePoints.get(i);
                    v3.x -= this.px;
                    v3.y -= this.py;
                }
                return;
            }
            Vector3f v3 = L_render.v3.set(xMin, yMin, L_addShadowToClipper.angle1cm);
            Vector3f v4 = L_render.v4.set(xMin, yMax, L_addShadowToClipper.angle2cm);
            Vector3f v5 = L_render.v5.set(xMax, yMax, L_addShadowToClipper.angle3cm);
            Vector3f v6 = L_render.v6.set(xMax, yMin, L_addShadowToClipper.angle4cm);
            ArrayList<Vector3f> sorted3 = L_render.vs;
            sorted3.clear();
            sorted3.add(v1);
            sorted3.add(v2);
            if (angle1 > angle2) {
                if (Float.compare(v3.z, angle1) < 0 && Float.compare(v3.z, angle2) > 0) {
                    sorted3.add(v3);
                }
                if (Float.compare(v4.z, angle1) < 0 && Float.compare(v4.z, angle2) > 0) {
                    sorted3.add(v4);
                }
                if (Float.compare(v5.z, angle1) < 0 && Float.compare(v5.z, angle2) > 0) {
                    sorted3.add(v5);
                }
                if (Float.compare(v6.z, angle1) < 0 && Float.compare(v6.z, angle2) > 0) {
                    sorted3.add(v6);
                }
            } else {
                if (Float.compare(v3.z, angle1) < 0 || Float.compare(v3.z, angle2) > 0) {
                    sorted3.add(v3);
                }
                if (Float.compare(v4.z, angle1) < 0 || Float.compare(v4.z, angle2) > 0) {
                    sorted3.add(v4);
                }
                if (Float.compare(v5.z, angle1) < 0 || Float.compare(v5.z, angle2) > 0) {
                    sorted3.add(v5);
                }
                if (Float.compare(v6.z, angle1) < 0 || Float.compare(v6.z, angle2) > 0) {
                    sorted3.add(v6);
                }
            }
            sorted3.sort((o1, o2) -> Float.compare(o1.z, o2.z));
            sorted3.add(sorted3.indexOf(v2), L_addShadowToClipper.v1.set(this.px, this.py, 0.0f));
            clipperBuf.clear();
            for (int i = 0; i < sorted3.size(); ++i) {
                clipperBuf.putFloat(sorted3.get((int)i).x);
                clipperBuf.putFloat(sorted3.get((int)i).y);
            }
            clipper.addPath(sorted3.size(), clipperBuf, false);
        }

        void addShadowToClipper(Clipper clipper, ByteBuffer clipperBuf, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(this.playerIndex);
            int xMin = chunkMap.getWorldXMinTiles();
            int yMin = chunkMap.getWorldYMinTiles();
            int xMax = chunkMap.getWorldXMaxTiles();
            int yMax = chunkMap.getWorldYMaxTiles();
            float angle2 = Vector2.getDirection(x3 - this.px, y3 - this.py) + (float)Math.PI;
            float angle1 = Vector2.getDirection(x4 - this.px, y4 - this.py) + (float)Math.PI;
            Vector3f v1 = L_render.v1.set(x3, y3, angle2);
            Vector3f v2 = L_render.v2.set(x4, y4, angle1);
            if (VisibilityPolygon2.instance.useCircle) {
                Vector3f v3;
                int i;
                float angleMin = PZMath.min(angle1, angle2);
                float angleMax = PZMath.max(angle1, angle2);
                for (int i2 = 0; i2 < circlePoints.size(); ++i2) {
                    Vector3f v32 = circlePoints.get(i2);
                    v32.x += this.px;
                    v32.y += this.py;
                }
                ArrayList<Vector3f> sorted2 = L_render.vs;
                sorted2.clear();
                sorted2.add(v1);
                sorted2.add(v2);
                clipperBuf.clear();
                if (angleMin < 1.5707964f && angleMax >= 4.712389f) {
                    for (i = 0; i < circlePoints.size(); ++i) {
                        v3 = circlePoints.get(i);
                        if (Float.compare(v3.z, angleMax) <= 0 && Float.compare(v3.z, angleMin) >= 0) continue;
                        sorted2.add(v3);
                    }
                    sorted2.sort((o1, o2) -> Float.compare(o1.z, o2.z));
                    for (i = 0; i < sorted2.size(); ++i) {
                        v = sorted2.get(i);
                        if (!(v.z >= 4.712389f)) continue;
                        clipperBuf.putFloat(v.x);
                        clipperBuf.putFloat(v.y);
                    }
                    for (i = 0; i < sorted2.size(); ++i) {
                        v = sorted2.get(i);
                        if (!(v.z < 1.5707964f)) continue;
                        clipperBuf.putFloat(v.x);
                        clipperBuf.putFloat(v.y);
                    }
                    clipperBuf.putFloat(x1);
                    clipperBuf.putFloat(y1);
                    clipperBuf.putFloat(x2);
                    clipperBuf.putFloat(y2);
                } else {
                    for (i = 0; i < circlePoints.size(); ++i) {
                        v3 = circlePoints.get(i);
                        if (Float.compare(v3.z, angleMax) >= 0 || Float.compare(v3.z, angleMin) <= 0) continue;
                        sorted2.add(v3);
                    }
                    sorted2.sort((o1, o2) -> Float.compare(o1.z, o2.z));
                    for (i = 0; i < sorted2.size(); ++i) {
                        v = sorted2.get(i);
                        clipperBuf.putFloat(v.x);
                        clipperBuf.putFloat(v.y);
                    }
                    clipperBuf.putFloat(x1);
                    clipperBuf.putFloat(y1);
                    clipperBuf.putFloat(x2);
                    clipperBuf.putFloat(y2);
                }
                clipper.addPath(sorted2.size() + 2, clipperBuf, false);
                for (i = 0; i < circlePoints.size(); ++i) {
                    v3 = circlePoints.get(i);
                    v3.x -= this.px;
                    v3.y -= this.py;
                }
                return;
            }
            Vector3f v3 = L_render.v3.set(xMin, yMin, L_addShadowToClipper.angle1cm);
            Vector3f v4 = L_render.v4.set(xMin, yMax, L_addShadowToClipper.angle2cm);
            Vector3f v5 = L_render.v5.set(xMax, yMax, L_addShadowToClipper.angle3cm);
            Vector3f v6 = L_render.v6.set(xMax, yMin, L_addShadowToClipper.angle4cm);
            ArrayList<Vector3f> sorted3 = L_render.vs;
            sorted3.clear();
            sorted3.add(v1);
            sorted3.add(v2);
            if (angle1 > angle2) {
                if (Float.compare(v3.z, angle1) < 0 && Float.compare(v3.z, angle2) > 0) {
                    sorted3.add(v3);
                }
                if (Float.compare(v4.z, angle1) < 0 && Float.compare(v4.z, angle2) > 0) {
                    sorted3.add(v4);
                }
                if (Float.compare(v5.z, angle1) < 0 && Float.compare(v5.z, angle2) > 0) {
                    sorted3.add(v5);
                }
                if (Float.compare(v6.z, angle1) < 0 && Float.compare(v6.z, angle2) > 0) {
                    sorted3.add(v6);
                }
                sorted3.sort((o1, o2) -> Float.compare(o1.z, o2.z));
                sorted3.add(L_addShadowToClipper.v1.set(x1, y1, 0.0f));
                sorted3.add(L_addShadowToClipper.v2.set(x2, y2, 0.0f));
                clipperBuf.clear();
                for (int i = 0; i < sorted3.size(); ++i) {
                    clipperBuf.putFloat(sorted3.get((int)i).x);
                    clipperBuf.putFloat(sorted3.get((int)i).y);
                }
                clipper.addPath(sorted3.size(), clipperBuf, false);
            } else {
                if (Float.compare(v3.z, angle1) < 0 || Float.compare(v3.z, angle2) > 0) {
                    sorted3.add(v3);
                }
                if (Float.compare(v4.z, angle1) < 0 || Float.compare(v4.z, angle2) > 0) {
                    sorted3.add(v4);
                }
                if (Float.compare(v5.z, angle1) < 0 || Float.compare(v5.z, angle2) > 0) {
                    sorted3.add(v5);
                }
                if (Float.compare(v6.z, angle1) < 0 || Float.compare(v6.z, angle2) > 0) {
                    sorted3.add(v6);
                }
                sorted3.sort((o1, o2) -> Float.compare(o1.z, o2.z));
                int index = sorted3.indexOf(v1);
                sorted3.add(index, L_addShadowToClipper.v1.set(x1, y1, 0.0f));
                sorted3.add(index + 1, L_addShadowToClipper.v2.set(x2, y2, 0.0f));
                clipperBuf.clear();
                for (int i = 0; i < sorted3.size(); ++i) {
                    clipperBuf.putFloat(sorted3.get((int)i).x);
                    clipperBuf.putFloat(sorted3.get((int)i).y);
                }
                clipper.addPath(sorted3.size(), clipperBuf, false);
            }
        }

        void triangulatePolygons(int numPolys3, Clipper clipper, ByteBuffer clipperBuf, PlayerCamera camera, float alpha) {
            for (int j = 0; j < numPolys3; ++j) {
                int numPoints;
                clipperBuf.clear();
                try {
                    numPoints = clipper.triangulate(j, clipperBuf);
                }
                catch (BufferOverflowException ex) {
                    clipperBuf = L_render.clipperBuf = ByteBuffer.allocateDirect(clipperBuf.capacity() + 1024);
                    --j;
                    continue;
                }
                for (int k = 0; k < numPoints; k += 3) {
                    float x1 = clipperBuf.getFloat();
                    float y1 = clipperBuf.getFloat();
                    float x2 = clipperBuf.getFloat();
                    float y2 = clipperBuf.getFloat();
                    float x3 = clipperBuf.getFloat();
                    float y3 = clipperBuf.getFloat();
                    if (VisibilityPolygon2.instance.useCircle) {
                        Vector2 split1 = L_render.vector2;
                        Vector2 split2 = L_render.vector2_2;
                        Vector2 split3 = L_render.vector2_3;
                        float dist1 = this.closestPointOnLineSegment(x1, y1, x2, y2, this.px, this.py, split1);
                        float dist2 = this.closestPointOnLineSegment(x2, y2, x3, y3, this.px, this.py, split2);
                        float dist3 = this.closestPointOnLineSegment(x3, y3, x1, y1, this.px, this.py, split3);
                        if (dist1 < 0.001f) {
                            dist1 = Float.MAX_VALUE;
                        }
                        if (dist2 < 0.001f) {
                            dist2 = Float.MAX_VALUE;
                        }
                        if (dist3 < 0.001f) {
                            dist3 = Float.MAX_VALUE;
                        }
                        if (dist1 < dist2 && dist1 < dist3) {
                            this.renderOneTriangle(camera, x1, y1, split1.x, split1.y, x3, y3, alpha);
                            this.renderOneTriangle(camera, split1.x, split1.y, x2, y2, x3, y3, alpha);
                            continue;
                        }
                        if (dist2 < dist1 && dist2 < dist3) {
                            this.renderOneTriangle(camera, x2, y2, split2.x, split2.y, x1, y1, alpha);
                            this.renderOneTriangle(camera, split2.x, split2.y, x3, y3, x1, y1, alpha);
                            continue;
                        }
                        if (dist3 < dist1 && dist3 < dist2) {
                            this.renderOneTriangle(camera, x3, y3, split3.x, split3.y, x2, y2, alpha);
                            this.renderOneTriangle(camera, split3.x, split3.y, x1, y1, x2, y2, alpha);
                            continue;
                        }
                    }
                    this.renderOneTriangle(camera, x1, y1, x2, y2, x3, y3, alpha);
                }
            }
        }

        float closestPointOnLineSegment(float x1, float y1, float x2, float y2, float x3, float y3, Vector2 closest) {
            double u = (double)((x3 - x1) * (x2 - x1) + (y3 - y1) * (y2 - y1)) / (Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0));
            if (Double.compare(u, 0.001f) <= 0) {
                closest.set(x1, y1);
                return IsoUtils.DistanceToSquared(x3, y3, x1, y1);
            }
            if (Double.compare(u, 0.9989999999525025) >= 0) {
                closest.set(x2, y2);
                return IsoUtils.DistanceToSquared(x3, y3, x2, y2);
            }
            double xu = (double)x1 + u * (double)(x2 - x1);
            double yu = (double)y1 + u * (double)(y2 - y1);
            closest.set((float)xu, (float)yu);
            return IsoUtils.DistanceToSquared(x3, y3, (float)xu, (float)yu);
        }

        void renderOneTriangle(PlayerCamera camera, float x1, float y1, float x2, float y2, float x3, float y3, float alpha) {
            VBORenderer vbor = VBORenderer.getInstance();
            float sx1 = camera.XToScreenExact(x1, y1, this.pz, 0);
            float sy1 = camera.YToScreenExact(x1, y1, this.pz, 0);
            float sx2 = camera.XToScreenExact(x2, y2, this.pz, 0);
            float sy2 = camera.YToScreenExact(x2, y2, this.pz, 0);
            float sx3 = camera.XToScreenExact(x3, y3, this.pz, 0);
            float sy3 = camera.YToScreenExact(x3, y3, this.pz, 0);
            float u = 0.0f;
            float v = 0.0f;
            float depthAdjust = -1.0E-4f;
            float depth1 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)this.px), (int)PZMath.fastfloor((float)this.py), (float)x1, (float)y1, (float)((float)this.pz)).depthStart + -1.0E-4f;
            float depth2 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)this.px), (int)PZMath.fastfloor((float)this.py), (float)x2, (float)y2, (float)((float)this.pz)).depthStart + -1.0E-4f;
            float depth3 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)this.px), (int)PZMath.fastfloor((float)this.py), (float)x3, (float)y3, (float)((float)this.pz)).depthStart + -1.0E-4f;
            float r = 0.0f;
            float g = 0.0f;
            float b = 0.0f;
            if (DebugOptions.instance.displayVisibilityPolygon.getValue()) {
                int c = L_render.triangleIndex++ % Model.debugDrawColours.length;
                r = Model.debugDrawColours[c].r;
                g = Model.debugDrawColours[c].g;
                b = Model.debugDrawColours[c].b;
                alpha = 1.0f;
            }
            if (VisibilityPolygon2.instance.useCircle) {
                float alpha1 = 1.0f - PZMath.clamp(IsoUtils.DistanceToSquared(x1, y1, this.px, this.py) / (this.circleRadius * this.circleRadius), 0.0f, 1.0f);
                float alpha2 = 1.0f - PZMath.clamp(IsoUtils.DistanceToSquared(x2, y2, this.px, this.py) / (this.circleRadius * this.circleRadius), 0.0f, 1.0f);
                float alpha3 = 1.0f - PZMath.clamp(IsoUtils.DistanceToSquared(x3, y3, this.px, this.py) / (this.circleRadius * this.circleRadius), 0.0f, 1.0f);
                vbor.addTriangleDepth(sx1, sy1, 0.0f, 0.0f, 0.0f, depth1, alpha1, sx2, sy2, 0.0f, 0.5f, 0.5f, depth2, alpha2, sx3, sy3, 0.0f, 1.0f, 1.0f, depth3, alpha3, r, g, b, alpha);
                return;
            }
            vbor.addTriangleDepth(sx1, sy1, 0.0f, 0.0f, 0.0f, depth1, sx2, sy2, 0.0f, 0.5f, 0.5f, depth2, sx3, sy3, 0.0f, 1.0f, 1.0f, depth3, r, g, b, alpha);
        }

        void addClipperPolygon(Clipper clipper, ByteBuffer clipperBuf, ClipperPolygon clipperPolygon, boolean bClip) {
            this.addClipperPolygon(clipper, clipperBuf, clipperPolygon.outer, bClip);
            for (int i = 0; i < clipperPolygon.holes.size(); ++i) {
                this.addClipperPolygon(clipper, clipperBuf, clipperPolygon.holes.get(i), bClip);
            }
        }

        void addClipperPolygon(Clipper clipper, ByteBuffer clipperBuf, TFloatArrayList xy, boolean bClip) {
            clipperBuf.clear();
            if (bClip) {
                // empty if block
            }
            for (int i = 0; i < xy.size(); ++i) {
                clipperBuf.putFloat(xy.get(i));
            }
            clipper.addPath(xy.size() / 2, clipperBuf, bClip);
        }

        void addClipperPolygons(Clipper clipper, ByteBuffer clipperBuf, ArrayList<ClipperPolygon> polygons, boolean bClip) {
            for (int i = 0; i < polygons.size(); ++i) {
                this.addClipperPolygon(clipper, clipperBuf, polygons.get(i), bClip);
            }
        }

        void getClipperPolygons(Clipper clipper, int numPolys, ArrayList<ClipperPolygon> polygons) {
            ByteBuffer clipperBuf = L_render.clipperBuf;
            for (int i = 0; i < numPolys; ++i) {
                clipperBuf.clear();
                clipper.getPolygon(i, clipperBuf);
                int numPoints = clipperBuf.getShort();
                if (numPoints < 3) continue;
                ClipperPolygon clipperPolygon = L_render.clipperPolygonPool.alloc();
                for (int j = 0; j < numPoints; ++j) {
                    float x1 = clipperBuf.getFloat();
                    float y1 = clipperBuf.getFloat();
                    clipperPolygon.outer.add(x1);
                    clipperPolygon.outer.add(y1);
                }
                int holeCount = clipperBuf.getShort();
                for (int j = 0; j < holeCount; ++j) {
                    numPoints = clipperBuf.getShort();
                    if (numPoints < 3) continue;
                    TFloatArrayList hole = L_render.floatArrayListPool.alloc();
                    hole.clear();
                    for (int k = 0; k < numPoints; ++k) {
                        float x1 = clipperBuf.getFloat();
                        float y1 = clipperBuf.getFloat();
                        hole.add(x1);
                        hole.add(y1);
                    }
                    clipperPolygon.holes.add(hole);
                }
                polygons.add(clipperPolygon);
            }
        }

        void releaseClipperPolygons(ArrayList<ClipperPolygon> polygons) {
            for (int i = 0; i < polygons.size(); ++i) {
                ClipperPolygon polygon = polygons.get(i);
                polygon.outer.clear();
                L_render.floatArrayListPool.releaseAll((List<TFloatArrayList>)polygon.holes);
                polygon.holes.clear();
            }
            L_render.clipperPolygonPool.releaseAll((List<ClipperPolygon>)polygons);
            polygons.clear();
        }

        static {
            downscale = 2;
            shadowBlurRamp = 0.015f;
            edge = new Vector2();
            angles = new Vector3[]{new Vector3(), new Vector3(), new Vector3(), new Vector3()};
        }

        private static class PolygonData {
            public static final int MAX_VERTS = 3072;
            public static final int NUM_ELEMENTS = 5;
            private final FloatBuffer data = BufferUtils.createFloatBuffer(15360);
            private int bufferId = -1;
            private PlayerCamera camera;
            private int sx;
            private int sy;
            private float z;

            private PolygonData() {
            }

            public void init() {
                if (this.bufferId == -1) {
                    this.bufferId = GL20.glGenBuffers();
                    GL20.glBindBuffer(34962, this.bufferId);
                    GL20.glBufferData(34962, this.data, 35048);
                    GL20.glBindBuffer(34962, 0);
                }
            }

            public void update(int playerIndex) {
                this.sx = PZMath.fastfloor(IsoCamera.frameState.camCharacterX);
                this.sy = PZMath.fastfloor(IsoCamera.frameState.camCharacterY);
                this.z = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
            }

            public void draw() {
                GL20.glBindBuffer(34962, this.bufferId);
                GL20.glBufferSubData(34962, 0L, this.data);
                GL20.glEnableVertexAttribArray(0);
                GL20.glEnableVertexAttribArray(1);
                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(0, 3, 5126, false, 20, 0L);
                GL20.glVertexAttribPointer(1, 1, 5126, false, 20, 12L);
                GL20.glVertexAttribPointer(2, 1, 5126, false, 20, 16L);
                GL11.glDrawArrays(4, 0, this.data.limit() / 5);
                GL20.glDisableVertexAttribArray(0);
                GL20.glDisableVertexAttribArray(1);
                GL20.glDisableVertexAttribArray(2);
                GL20.glBindBuffer(34962, 0);
            }

            public void addVertex(float x, float y, float b) {
                float depthAdjust = -1.0E-4f;
                float cx = this.camera.XToScreenExact(x, y, this.z, 0);
                float cy = this.camera.YToScreenExact(x, y, this.z, 0);
                float d = IsoDepthHelper.getSquareDepthData((int)this.sx, (int)this.sy, (float)x, (float)y, (float)this.z).depthStart;
                this.addTransformed(cx, cy, 0.0f, b, d + -1.0E-4f);
            }

            public void addTransformed(float x, float y, float z, float b, float d) {
                this.data.put(x);
                this.data.put(y);
                this.data.put(z);
                this.data.put(b);
                this.data.put(d);
            }

            public void reset() {
                this.data.rewind();
                this.data.limit(this.data.capacity());
            }
        }
    }

    static final class L_addShadowToClipper {
        static float angle1cm;
        static float angle2cm;
        static float angle3cm;
        static float angle4cm;
        static final Vector3f v1;
        static final Vector3f v2;

        L_addShadowToClipper() {
        }

        static {
            v1 = new Vector3f();
            v2 = new Vector3f();
        }
    }

    static final class L_render {
        static final Vector3f v1 = new Vector3f();
        static final Vector3f v2 = new Vector3f();
        static final Vector3f v3 = new Vector3f();
        static final Vector3f v4 = new Vector3f();
        static final Vector3f v5 = new Vector3f();
        static final Vector3f v6 = new Vector3f();
        static final ArrayList<Vector3f> vs = new ArrayList();
        static final Vector2 vector2 = new Vector2();
        static final Vector2 vector2_2 = new Vector2();
        static final Vector2 vector2_3 = new Vector2();
        static final Clipper m_clipper = new Clipper();
        static ByteBuffer clipperBuf = ByteBuffer.allocateDirect(10240);
        static final ArrayList<ClipperPolygon> clipperPolygons1 = new ArrayList();
        static final ArrayList<ClipperPolygon> clipperPolygons2 = new ArrayList();
        static final ArrayList<ClipperPolygon> clipperPolygons3 = new ArrayList();
        static final ObjectPool<ClipperPolygon> clipperPolygonPool = new ObjectPool<ClipperPolygon>(ClipperPolygon::new);
        static final ObjectPool<TFloatArrayList> floatArrayListPool = new ObjectPool<TFloatArrayList>(TFloatArrayList::new);
        static int triangleIndex;

        L_render() {
        }
    }

    static final class L_calculateVisibilityPolygon {
        static final ObjectPool<Segment> m_segmentPool = new ObjectPool<Segment>(Segment::new);
        static final Vector2 m_tempVector2_1 = new Vector2();
        static final HashSet<Segment> segmentHashSet = new HashSet();
        static final ArrayList<Segment> segmentList = new ArrayList();
        static final ArrayList<VisibilityWall> sortedWalls = new ArrayList();
        static final ArrayList<IsoGridSquare> solidSquares = new ArrayList();

        L_calculateVisibilityPolygon() {
        }
    }

    public static final class ChunkData {
        final IsoChunk chunk;
        final TIntObjectHashMap<ChunkLevelData> levelData = new TIntObjectHashMap();

        public ChunkData(IsoChunk chunk) {
            this.chunk = chunk;
        }

        public ChunkLevelData getDataForLevel(int level) {
            if (level < -32 || level > 31) {
                return null;
            }
            int index = level + 32;
            ChunkLevelData levelData = this.levelData.get(index);
            if (levelData == null) {
                levelData = new ChunkLevelData(this, level);
                this.levelData.put(index, levelData);
            }
            return levelData;
        }

        public void removeFromWorld() {
            for (ChunkLevelData levelData : this.levelData.valueCollection()) {
                levelData.removeFromWorld();
            }
        }
    }

    public static final class ChunkLevelData {
        final ChunkData chunkData;
        final int level;
        int adjacentChunkLoadedCounter = -1;
        final ArrayList<VisibilityWall> allWalls = new ArrayList();
        final ArrayList<IsoGridSquare> solidSquares = new ArrayList();

        ChunkLevelData(ChunkData chunkData, int level) {
            this.chunkData = chunkData;
            this.level = level;
        }

        public void invalidate() {
            this.adjacentChunkLoadedCounter = -1;
            ++VisibilityPolygon2.getInstance().dirtyObstacleCounter;
        }

        public void recreate() {
            IsoGridSquare square;
            VisibilityWall wall;
            int level = this.level;
            IsoChunk chunk = this.chunkData.chunk;
            ObjectPool<VisibilityWall> exteriorWallsPool = VisibilityPolygon2.getInstance().visibilityWallPool;
            exteriorWallsPool.releaseAll((List<VisibilityWall>)this.allWalls);
            this.allWalls.clear();
            this.solidSquares.clear();
            if (level < chunk.minLevel || level > chunk.maxLevel) {
                return;
            }
            IsoGridSquare[] squares = chunk.squares[chunk.squaresIndexOfLevel(level)];
            int chunksPerWidth = 8;
            for (int y = 0; y < 8; ++y) {
                wall = null;
                for (int x = 0; x < 8; ++x) {
                    square = squares[x + y * 8];
                    if (this.hasNorthWall(square)) {
                        if (wall == null) {
                            wall = exteriorWallsPool.alloc();
                            wall.chunkLevelData = this;
                            wall.x1 = square.x;
                            wall.y1 = square.y;
                        }
                    } else if (wall != null) {
                        wall.x2 = chunk.wx * 8 + x;
                        wall.y2 = chunk.wy * 8 + y;
                        this.allWalls.add(wall);
                        wall = null;
                    }
                    if (square == null) continue;
                    if (square.has(IsoObjectType.tree)) {
                        IsoTree tree = square.getTree();
                        if (tree == null || tree.getProperties() == null || !tree.getProperties().has(IsoFlagType.blocksight)) continue;
                        this.solidSquares.add(square);
                        continue;
                    }
                    if (!square.isSolid()) continue;
                    this.solidSquares.add(square);
                }
                if (wall == null) continue;
                wall.x2 = chunk.wx * 8 + 8;
                wall.y2 = chunk.wy * 8 + y;
                this.allWalls.add(wall);
            }
            for (int x = 0; x < 8; ++x) {
                wall = null;
                for (int y = 0; y < 8; ++y) {
                    square = squares[x + y * 8];
                    if (this.hasWestWall(square)) {
                        if (wall != null) continue;
                        wall = exteriorWallsPool.alloc();
                        wall.chunkLevelData = this;
                        wall.x1 = square.x;
                        wall.y1 = square.y;
                        continue;
                    }
                    if (wall == null) continue;
                    wall.x2 = chunk.wx * 8 + x;
                    wall.y2 = chunk.wy * 8 + y;
                    this.allWalls.add(wall);
                    wall = null;
                }
                if (wall == null) continue;
                wall.x2 = chunk.wx * 8 + x;
                wall.y2 = chunk.wy * 8 + 8;
                this.allWalls.add(wall);
            }
        }

        boolean hasNorthWall(IsoGridSquare square) {
            if (square == null) {
                return false;
            }
            if (square.isSolid()) {
                return false;
            }
            if (square.has(IsoObjectType.tree)) {
                return false;
            }
            return square.testVisionAdjacent(0, -1, 0, false, false) == LosUtil.TestResults.Blocked;
        }

        boolean hasWestWall(IsoGridSquare square) {
            if (square == null) {
                return false;
            }
            if (square.isSolid()) {
                return false;
            }
            if (square.has(IsoObjectType.tree)) {
                return false;
            }
            return square.testVisionAdjacent(-1, 0, 0, false, false) == LosUtil.TestResults.Blocked;
        }

        void removeFromWorld() {
            this.adjacentChunkLoadedCounter = -1;
            VisibilityPolygon2.instance.visibilityWallPool.releaseAll((List<VisibilityWall>)this.allWalls);
            this.allWalls.clear();
            this.solidSquares.clear();
        }
    }

    public static final class VisibilityWall {
        ChunkLevelData chunkLevelData;
        public int x1;
        public int y1;
        public int x2;
        public int y2;

        boolean isHorizontal() {
            return this.y1 == this.y2;
        }
    }

    private static final class Partition {
        float minAngle;
        float maxAngle;
        final ArrayList<Segment> segments = new ArrayList();

        private Partition() {
        }

        boolean addSegment(Segment segment) {
            int i;
            int index = this.segments.size();
            for (i = 0; i < this.segments.size(); ++i) {
                if (Float.compare(segment.minDist(), this.segments.get(i).minDist()) >= 0) continue;
                index = i;
                break;
            }
            for (i = 0; i < index; ++i) {
                if (!segment.isInShadowOf(this.segments.get(i))) continue;
                return false;
            }
            this.segments.add(index, segment);
            return true;
        }
    }

    private static final class Segment {
        final EndPoint a = new EndPoint();
        final EndPoint b = new EndPoint();

        private Segment() {
        }

        float minAngle() {
            return PZMath.min(this.a.angle, this.b.angle);
        }

        float maxAngle() {
            return PZMath.max(this.a.angle, this.b.angle);
        }

        float minDist() {
            return PZMath.min(this.a.dist, this.b.dist);
        }

        float maxDist() {
            return PZMath.max(this.a.dist, this.b.dist);
        }

        boolean isInShadowOf(Segment other) {
            float minAngle1 = this.minAngle();
            float maxAngle1 = this.maxAngle();
            if (maxAngle1 - minAngle1 > 180.0f) {
                float temp = minAngle1 += 360.0f;
                minAngle1 = maxAngle1;
                maxAngle1 = temp;
            }
            float minAngle2 = other.minAngle();
            float maxAngle2 = other.maxAngle();
            if (maxAngle2 - minAngle2 > 180.0f) {
                float temp = minAngle2 += 360.0f;
                minAngle2 = maxAngle2;
                maxAngle2 = temp;
            }
            if (maxAngle1 > 360.0f && maxAngle2 < 180.0f) {
                if (minAngle2 < 180.0f) {
                    minAngle2 += 360.0f;
                }
                maxAngle2 += 360.0f;
            }
            if (maxAngle2 > 360.0f && maxAngle1 < 180.0f) {
                if (minAngle1 < 180.0f) {
                    minAngle1 += 360.0f;
                }
                maxAngle1 += 360.0f;
            }
            return Float.compare(minAngle1, minAngle2) >= 0 && Float.compare(maxAngle1, maxAngle2) <= 0 && Float.compare(this.minDist(), other.maxDist()) >= 0;
        }
    }

    private static final class EndPoint {
        float x;
        float y;
        float dirX;
        float dirY;
        float angle;
        float dist;

        private EndPoint() {
        }

        EndPoint set(float x, float y, float dirX, float dirY, float angle, float dist) {
            this.x = x;
            this.y = y;
            Vector2 v = L_calculateVisibilityPolygon.m_tempVector2_1.set(dirX, dirY);
            v.normalize();
            this.dirX = v.x;
            this.dirY = v.y;
            this.angle = angle;
            this.dist = dist;
            return this;
        }
    }
}


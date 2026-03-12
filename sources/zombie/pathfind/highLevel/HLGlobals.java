/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind.highLevel;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.pathfind.PMMover;
import zombie.pathfind.Path;
import zombie.pathfind.highLevel.FloodFill;
import zombie.pathfind.highLevel.HLAStar;
import zombie.pathfind.highLevel.HLChunkLevel;
import zombie.pathfind.highLevel.HLChunkRegion;
import zombie.pathfind.highLevel.HLLevelTransition;
import zombie.pathfind.highLevel.HLSearchNode;
import zombie.pathfind.highLevel.HLSlopedSurface;
import zombie.pathfind.highLevel.HLStaircase;
import zombie.pathfind.highLevel.HLSuccessor;
import zombie.popman.ObjectPool;
import zombie.vehicles.Clipper;

public final class HLGlobals {
    static final ObjectPool<HLChunkRegion> chunkRegionPool = new ObjectPool<HLChunkRegion>(HLChunkRegion::new);
    static final ObjectPool<HLStaircase> staircasePool = new ObjectPool<HLStaircase>(HLStaircase::new);
    static final ObjectPool<HLSlopedSurface> slopedSurfacePool = new ObjectPool<HLSlopedSurface>(HLSlopedSurface::new);
    static final ObjectPool<HLSuccessor> successorPool = new ObjectPool<HLSuccessor>(HLSuccessor::new);
    static final FloodFill floodFill = new FloodFill();
    static final Clipper clipper = new Clipper();
    static ByteBuffer clipperBuffer = ByteBuffer.allocateDirect(512);
    public static final HLAStar astar = new HLAStar();
    static final ObjectPool<HLSearchNode> searchNodePool = new ObjectPool<HLSearchNode>(HLSearchNode::new);
    public static int debugTargetLevel;
    public static final PMMover mover;
    public static final ArrayList<HLChunkLevel> chunkLevelList;
    public static final ArrayList<HLLevelTransition> levelTransitionList;
    public static final ArrayList<HLStaircase> staircaseList2;
    public static final ArrayList<Boolean> bottomOfLevelTransition;
    public static final Path path;

    static {
        mover = new PMMover();
        chunkLevelList = new ArrayList();
        levelTransitionList = new ArrayList();
        staircaseList2 = new ArrayList();
        bottomOfLevelTransition = new ArrayList();
        path = new Path();
    }
}


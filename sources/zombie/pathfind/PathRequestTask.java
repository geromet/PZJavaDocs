/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.ai.astar.Mover;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.pathfind.PathFindRequest;
import zombie.pathfind.PolygonalMap2;

final class PathRequestTask {
    PolygonalMap2 map;
    PathFindRequest request;
    static final ArrayDeque<PathRequestTask> pool = new ArrayDeque();

    PathRequestTask() {
    }

    PathRequestTask init(PolygonalMap2 map, PathFindRequest request) {
        this.map = map;
        this.request = request;
        return this;
    }

    /*
     * Enabled aggressive block sorting
     */
    void execute() {
        if (this.request.mover instanceof IsoPlayer) {
            this.map.requests.playerQ.add(this.request);
            return;
        }
        Mover mover = this.request.mover;
        if (mover instanceof IsoZombie) {
            IsoZombie isoZombie = (IsoZombie)mover;
            if (isoZombie.target != null) {
                this.map.requests.aggroZombieQ.add(this.request);
                return;
            }
        }
        this.map.requests.otherQ.add(this.request);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static PathRequestTask alloc() {
        ArrayDeque<PathRequestTask> arrayDeque = pool;
        synchronized (arrayDeque) {
            return pool.isEmpty() ? new PathRequestTask() : pool.pop();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void release() {
        ArrayDeque<PathRequestTask> arrayDeque = pool;
        synchronized (arrayDeque) {
            assert (!pool.contains(this));
            pool.push(this);
        }
    }
}


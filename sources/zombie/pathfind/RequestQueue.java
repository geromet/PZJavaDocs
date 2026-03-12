/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.pathfind.PathFindRequest;

final class RequestQueue {
    final ArrayDeque<PathFindRequest> playerQ = new ArrayDeque();
    final ArrayDeque<PathFindRequest> aggroZombieQ = new ArrayDeque();
    final ArrayDeque<PathFindRequest> otherQ = new ArrayDeque();

    RequestQueue() {
    }

    boolean isEmpty() {
        return this.playerQ.isEmpty() && this.aggroZombieQ.isEmpty() && this.otherQ.isEmpty();
    }

    PathFindRequest removeFirst() {
        if (!this.playerQ.isEmpty()) {
            return this.playerQ.removeFirst();
        }
        if (!this.aggroZombieQ.isEmpty()) {
            return this.aggroZombieQ.removeFirst();
        }
        return this.otherQ.removeFirst();
    }

    PathFindRequest removeLast() {
        if (!this.otherQ.isEmpty()) {
            return this.otherQ.removeLast();
        }
        if (!this.aggroZombieQ.isEmpty()) {
            return this.aggroZombieQ.removeLast();
        }
        return this.playerQ.removeLast();
    }
}


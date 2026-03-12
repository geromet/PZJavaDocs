/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import org.joml.Vector2f;
import zombie.pathfind.Edge;
import zombie.pathfind.Node;

final class ClosestPointOnEdge {
    Edge edge;
    Node node;
    final Vector2f point = new Vector2f();
    double distSq;

    ClosestPointOnEdge() {
    }
}


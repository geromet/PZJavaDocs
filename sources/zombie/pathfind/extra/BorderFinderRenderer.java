/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind.extra;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.pathfind.extra.BorderStatus;
import zombie.pathfind.extra.Direction;
import zombie.pathfind.extra.Position;

public class BorderFinderRenderer {
    public static final BorderFinderRenderer instance = new BorderFinderRenderer();
    private final Set<Position> path = new HashSet<Position>();
    private final Object renderLock = new Object();

    private BorderFinderRenderer() {
    }

    public void addAllPath(Collection<Position> positions) {
        this.path.addAll(positions);
    }

    public void render() {
        if (DebugOptions.instance.pathfindBorderFinder.getValue()) {
            for (Position position : this.path) {
                LineDrawer.addLine((float)position.coords().x() + 0.45f, (float)position.coords().y() + 0.45f, position.coords().z(), (float)position.coords().x() + 0.55f, (float)position.coords().y() + 0.55f, position.coords().z(), 0.5f, 1.0f, 0.5f, null, false);
                for (Direction direction : Direction.values()) {
                    float r = 0.0f;
                    float g = 0.0f;
                    float b = 0.0f;
                    if (position.walls().get((Object)direction) == BorderStatus.OUT_OF_RANGE) {
                        r = 1.0f;
                    } else {
                        b = 1.0f;
                    }
                    if (position.walls().get((Object)direction) == BorderStatus.OPEN) continue;
                    float xmin = switch (direction) {
                        case Direction.NORTH, Direction.SOUTH -> 0.0f;
                        case Direction.WEST -> 0.2f;
                        case Direction.EAST -> 0.8f;
                        default -> 0.5f;
                    };
                    float xmax = switch (direction) {
                        case Direction.NORTH, Direction.SOUTH -> 1.0f;
                        case Direction.WEST -> 0.2f;
                        case Direction.EAST -> 0.8f;
                        default -> 0.5f;
                    };
                    float ymin = switch (direction) {
                        case Direction.NORTH -> 0.2f;
                        case Direction.SOUTH -> 0.8f;
                        case Direction.WEST, Direction.EAST -> 0.0f;
                        default -> 0.5f;
                    };
                    float ymax = switch (direction) {
                        case Direction.NORTH -> 0.2f;
                        case Direction.SOUTH -> 0.8f;
                        case Direction.WEST, Direction.EAST -> 1.0f;
                        default -> 0.5f;
                    };
                    LineDrawer.addLine((float)position.coords().x() + xmin, (float)position.coords().y() + ymin, position.coords().z(), (float)position.coords().x() + xmax, (float)position.coords().y() + ymax, position.coords().z(), r, 0.0f, b, null, false);
                }
            }
        }
    }
}


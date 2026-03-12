/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals.pathfind;

import org.joml.Vector2f;
import zombie.characters.animals.pathfind.NestedPath;
import zombie.characters.animals.pathfind.NestedPaths;
import zombie.core.SpriteRenderer;
import zombie.core.random.Rand;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMapRenderer;

public final class NestedPathWanderer {
    public NestedPaths paths;
    public NestedPath path;
    public float x;
    public float y;
    boolean moveForwardOnPath = true;
    float switchPathTimer;

    void pickAnotherPath() {
        boolean moveOut = Rand.NextBool(2);
        int index = this.paths.paths.indexOf(this.path);
        Vector2f pos = new Vector2f();
        if (moveOut) {
            for (int i = index - 1; i >= 0; --i) {
                NestedPath path = this.paths.paths.get(i);
                if (path.inset != this.path.inset - 5) continue;
                float t = path.getClosestPointOn(this.x, this.y, pos);
                if (!(Vector2f.distance(this.x, this.y, pos.x, pos.y) < 10.0f)) continue;
                this.path = path;
                return;
            }
        } else {
            for (int i = index + 1; i < this.paths.paths.size(); ++i) {
                NestedPath path = this.paths.paths.get(i);
                if (path.inset != this.path.inset + 5) continue;
                float t = path.getClosestPointOn(this.x, this.y, pos);
                if (!(Vector2f.distance(this.x, this.y, pos.x, pos.y) < 10.0f)) continue;
                this.path = path;
                return;
            }
        }
    }

    void moveAlongPath(float distance) {
        float t2;
        Vector2f pos = new Vector2f();
        float t = this.path.getClosestPointOn(this.x, this.y, pos);
        float zoneLength = this.path.getLength();
        if (this.moveForwardOnPath) {
            t2 = t + distance / zoneLength;
            if (t2 >= 1.0f) {
                t2 %= 1.0f;
            }
        } else {
            t2 = t - distance / zoneLength;
            if (t2 <= 0.0f) {
                t2 = (t2 + 1.0f) % 1.0f;
            }
        }
        this.path.getPointOn(t2, pos);
        this.x = pos.x;
        this.y = pos.y;
    }

    public void render(UIWorldMap ui) {
        float f;
        this.switchPathTimer += 1.0f;
        if (f >= 90.0f) {
            this.pickAnotherPath();
            this.switchPathTimer = 0.0f;
        }
        this.moveAlongPath(1.0f);
        this.drawRect(ui, this.x - 1.0f, this.y - 1.0f, 2.0f, 2.0f, 0.0f, 1.0f, 0.0f, 1.0f);
    }

    public void drawLine(UIWorldMap ui, float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        WorldMapRenderer rr = ui.getAPIv1().getRenderer();
        int uiX1 = (int)rr.worldToUIX(x1, y1, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        int uiY1 = (int)rr.worldToUIY(x1, y1, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        int uiX2 = (int)rr.worldToUIX(x2, y2, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        int uiY2 = (int)rr.worldToUIY(x2, y2, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        SpriteRenderer.instance.renderline(null, uiX1, uiY1, uiX2, uiY2, r, g, b, a, 1.0f);
    }

    public void drawRect(UIWorldMap ui, float x1, float y1, float w, float h, float r, float g, float b, float a) {
        this.drawLine(ui, x1, y1, x1 + w, y1, r, g, b, a);
        this.drawLine(ui, x1 + w, y1, x1 + w, y1 + h, r, g, b, a);
        this.drawLine(ui, x1, y1 + h, x1 + w, y1 + h, r, g, b, a);
        this.drawLine(ui, x1, y1, x1, y1 + h, r, g, b, a);
    }
}


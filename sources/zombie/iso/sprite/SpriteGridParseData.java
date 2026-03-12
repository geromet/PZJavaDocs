/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import zombie.iso.sprite.IsoSprite;
import zombie.popman.ObjectPool;

public final class SpriteGridParseData {
    final ObjectPool<Level> levelPool = new ObjectPool<Level>(Level::new);
    public final ArrayList<Level> levels = new ArrayList();
    public int width;
    public int height;

    public Level getOrCreateLevel(int z) {
        int z1 = this.levels.size();
        while (z1 <= z) {
            Level level = this.levelPool.alloc();
            level.width = 0;
            level.height = 0;
            level.z = z1++;
            level.xyToSprite.clear();
            this.levels.add(level);
        }
        return this.levels.get(z);
    }

    public boolean isValid() {
        if (this.width <= 0 || this.height <= 0) {
            return false;
        }
        if (this.levels.isEmpty()) {
            return false;
        }
        for (Level level : this.levels) {
            if (level.isValid()) continue;
            return false;
        }
        return true;
    }

    public void clear() {
        this.levelPool.releaseAll((List<Level>)this.levels);
        this.levels.clear();
        this.width = 0;
        this.height = 0;
    }

    public static final class Level {
        public int width;
        public int height;
        public int z;
        public HashMap<String, IsoSprite> xyToSprite = new HashMap();

        boolean isValid() {
            if (this.width <= 0 || this.height <= 0) {
                return false;
            }
            return !this.xyToSprite.isEmpty();
        }
    }
}


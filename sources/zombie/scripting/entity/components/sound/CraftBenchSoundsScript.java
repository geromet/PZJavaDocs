/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity.components.sound;

import java.util.ArrayList;
import zombie.entity.ComponentType;
import zombie.entity.components.sounds.CraftBenchSound;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;

public class CraftBenchSoundsScript
extends ComponentScript {
    private final ArrayList<CraftBenchSound> sounds = new ArrayList();

    protected CraftBenchSoundsScript() {
        super(ComponentType.CraftBenchSounds);
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        super.load(block);
        for (ScriptParser.Value value : block.values) {
            String[] ss;
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.isEmpty() || val.isEmpty() || (ss = val.split("\\s+")).length == 0) continue;
            CraftBenchSound sound = new CraftBenchSound();
            sound.id = key;
            sound.gameSound = ss[0];
            if (ss.length > 1) {
                sound.param1 = ss[1];
            }
            if (ss.length > 2) {
                sound.param2 = ss[2];
            }
            if (ss.length > 3) {
                sound.param3 = ss[2];
            }
            this.sounds.add(sound);
        }
    }

    @Override
    protected <T extends ComponentScript> void copyFrom(T source2) {
    }

    public ArrayList<CraftBenchSound> getSounds() {
        return this.sounds;
    }
}


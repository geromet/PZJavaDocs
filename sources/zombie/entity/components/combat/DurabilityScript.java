/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.combat;

import zombie.entity.ComponentType;
import zombie.iso.enums.MaterialType;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;

public class DurabilityScript
extends ComponentScript {
    private float maxHitPoints;
    private float currentHitPoints;
    private MaterialType material;

    private DurabilityScript() {
        super(ComponentType.Durability);
    }

    public float getMaxHitPoints() {
        return this.maxHitPoints;
    }

    public float getCurrentHitPoints() {
        return this.currentHitPoints;
    }

    public MaterialType getMaterial() {
        return this.material;
    }

    protected void copyFrom(ComponentScript componentScript) {
        DurabilityScript other = (DurabilityScript)componentScript;
        this.currentHitPoints = other.currentHitPoints;
        this.maxHitPoints = other.maxHitPoints;
        this.material = other.material;
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        super.load(block);
        for (ScriptParser.BlockElement element : block.elements) {
            String s;
            if (element.asValue() == null || (s = element.asValue().string).trim().isEmpty() || !s.contains("=")) continue;
            String[] split = s.split("=");
            String k = split[0].trim();
            String v = split[1].trim();
            if (k.equalsIgnoreCase("MaxHitPoints")) {
                this.currentHitPoints = this.maxHitPoints = Float.parseFloat(v);
                continue;
            }
            if (!k.equalsIgnoreCase("Material")) continue;
            this.material = MaterialType.valueOf(v);
        }
    }
}


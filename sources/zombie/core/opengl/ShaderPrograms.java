/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.opengl;

import gnu.trove.map.hash.TIntObjectHashMap;
import zombie.core.opengl.ShaderProgram;

public final class ShaderPrograms {
    private static ShaderPrograms instance;
    private final TIntObjectHashMap<ShaderProgram> programById = new TIntObjectHashMap();

    public static ShaderPrograms getInstance() {
        if (instance == null) {
            instance = new ShaderPrograms();
        }
        return instance;
    }

    private ShaderPrograms() {
        this.programById.setAutoCompactionFactor(0.0f);
    }

    public void registerProgram(ShaderProgram shaderProgram) {
        if (shaderProgram.getShaderID() == 0) {
            return;
        }
        this.programById.put(shaderProgram.getShaderID(), shaderProgram);
    }

    public void unregisterProgram(ShaderProgram shaderProgram) {
        if (shaderProgram.getShaderID() == 0) {
            return;
        }
        this.programById.remove(shaderProgram.getShaderID());
    }

    public ShaderProgram getProgramByID(int shaderID) {
        return this.programById.get(shaderID);
    }
}


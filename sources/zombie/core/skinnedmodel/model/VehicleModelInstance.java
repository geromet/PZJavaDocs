/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import java.util.Arrays;
import org.joml.Vector3f;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.textures.Texture;

public final class VehicleModelInstance
extends ModelInstance {
    public Texture textureRust;
    public Texture textureMask;
    public Texture textureLights;
    public Texture textureDamage1Overlay;
    public Texture textureDamage1Shell;
    public Texture textureDamage2Overlay;
    public Texture textureDamage2Shell;
    public final float[] textureUninstall1 = new float[16];
    public final float[] textureUninstall2 = new float[16];
    public final float[] textureLightsEnables1 = new float[16];
    public final float[] textureLightsEnables2 = new float[16];
    public final float[] textureDamage1Enables1 = new float[16];
    public final float[] textureDamage1Enables2 = new float[16];
    public final float[] textureDamage2Enables1 = new float[16];
    public final float[] textureDamage2Enables2 = new float[16];
    public final float[] matrixBlood1Enables1 = new float[16];
    public final float[] matrixBlood1Enables2 = new float[16];
    public final float[] matrixBlood2Enables1 = new float[16];
    public final float[] matrixBlood2Enables2 = new float[16];
    public float textureRustA;
    public float refWindows = 0.5f;
    public float refBody = 0.4f;
    public final Vector3f painColor = new Vector3f(0.0f, 0.5f, 0.5f);
    private final ModelInstance.EffectLight[] lights = new ModelInstance.EffectLight[5];
    public final Object lightsLock = "Model Lights Lock";

    public VehicleModelInstance() {
        for (int i = 0; i < this.lights.length; ++i) {
            this.lights[i] = new ModelInstance.EffectLight();
        }
    }

    @Override
    public void reset() {
        super.reset();
        Arrays.fill(this.textureUninstall1, 0.0f);
        Arrays.fill(this.textureUninstall2, 0.0f);
        Arrays.fill(this.textureLightsEnables1, 0.0f);
        Arrays.fill(this.textureLightsEnables2, 0.0f);
        Arrays.fill(this.textureDamage1Enables1, 0.0f);
        Arrays.fill(this.textureDamage1Enables2, 0.0f);
        Arrays.fill(this.textureDamage2Enables1, 0.0f);
        Arrays.fill(this.textureDamage2Enables2, 0.0f);
        Arrays.fill(this.matrixBlood1Enables1, 0.0f);
        Arrays.fill(this.matrixBlood1Enables2, 0.0f);
        Arrays.fill(this.matrixBlood2Enables1, 0.0f);
        Arrays.fill(this.matrixBlood2Enables2, 0.0f);
        this.textureRustA = 0.0f;
        this.refWindows = 0.5f;
        this.refBody = 0.4f;
        this.painColor.set(0.0f, 0.5f, 0.5f);
        for (int i = 0; i < this.lights.length; ++i) {
            this.lights[i].clear();
        }
    }

    public ModelInstance.EffectLight[] getLights() {
        return this.lights;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void UpdateLights(int playerIndex) {
        Object object = this.lightsLock;
        synchronized (object) {
            ModelManager.instance.getSquareLighting(playerIndex, this.object, this.lights);
        }
    }
}


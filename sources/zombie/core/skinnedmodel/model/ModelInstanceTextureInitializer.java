/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import zombie.characters.EquippedTextureCreator;
import zombie.core.Color;
import zombie.core.ImmutableColor;
import zombie.core.SpriteRenderer;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.popman.ObjectPool;
import zombie.util.Type;

public final class ModelInstanceTextureInitializer {
    private int stateIndex;
    private boolean rendered;
    private ModelInstance modelInstance;
    private InventoryItem item;
    private float bloodLevel;
    private float fluidLevel;
    private final Color fluidTint = new Color();
    private int changeNumberMain;
    private int changeNumberThread;
    private final RenderData[] renderData = new RenderData[3];
    private static final ObjectPool<ModelInstanceTextureInitializer> pool = new ObjectPool<ModelInstanceTextureInitializer>(ModelInstanceTextureInitializer::new);

    public void init(ModelInstance modelInstance, InventoryItem item) {
        this.stateIndex = SpriteRenderer.instance.getMainStateIndex();
        this.item = item;
        this.modelInstance = modelInstance;
        HandWeapon weapon = Type.tryCastTo(item, HandWeapon.class);
        this.bloodLevel = weapon == null ? 0.0f : weapon.getBloodLevel();
        this.fluidLevel = 0.0f;
        FluidContainer fluidContainer = item.getFluidContainer();
        if (fluidContainer != null) {
            this.fluidLevel = fluidContainer.getFilledRatio();
            this.fluidTint.set(fluidContainer.getColor());
        }
        this.setDirty();
    }

    public void init(ModelInstance modelInstance, float bloodLevel) {
        this.stateIndex = SpriteRenderer.instance.getMainStateIndex();
        this.item = null;
        this.modelInstance = modelInstance;
        this.bloodLevel = bloodLevel;
        this.fluidLevel = 0.0f;
        this.setDirty();
    }

    public void setDirty() {
        ++this.changeNumberMain;
        this.rendered = false;
    }

    public boolean isDirty() {
        return !this.rendered;
    }

    public void renderMain() {
        if (this.rendered) {
            return;
        }
        int stateIndex = this.stateIndex;
        if (this.renderData[stateIndex] == null) {
            this.renderData[stateIndex] = new RenderData();
        }
        RenderData renderData = this.renderData[stateIndex];
        if (renderData.textureCreator != null) {
            return;
        }
        renderData.changeNumber = this.changeNumberMain;
        renderData.textureCreator = EquippedTextureCreator.alloc();
        if (this.item == null) {
            renderData.textureCreator.init(this.modelInstance, this.bloodLevel, ImmutableColor.white, this.fluidLevel, this.fluidTint);
        } else {
            renderData.textureCreator.init(this.modelInstance, this.item);
        }
        renderData.rendered = false;
    }

    public void render() {
        int stateIndex = SpriteRenderer.instance.getRenderStateIndex();
        RenderData renderData = this.renderData[stateIndex];
        if (renderData == null) {
            return;
        }
        if (renderData.textureCreator == null) {
            return;
        }
        if (renderData.rendered) {
            return;
        }
        if (renderData.changeNumber == this.changeNumberThread) {
            renderData.rendered = true;
            return;
        }
        renderData.textureCreator.render();
        if (renderData.textureCreator.isRendered()) {
            this.changeNumberThread = renderData.changeNumber;
            renderData.rendered = true;
        }
    }

    public void postRender() {
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        RenderData renderData = this.renderData[stateIndex];
        if (renderData == null) {
            return;
        }
        if (renderData.textureCreator == null) {
            return;
        }
        if (renderData.textureCreator.isRendered() && renderData.changeNumber == this.changeNumberMain) {
            this.rendered = true;
        }
        if (renderData.rendered) {
            renderData.textureCreator.postRender();
            renderData.textureCreator = null;
        }
    }

    public boolean isRendered() {
        int stateIndex = SpriteRenderer.instance.getRenderStateIndex();
        RenderData renderData = this.renderData[stateIndex];
        if (renderData == null) {
            return true;
        }
        if (renderData.textureCreator == null) {
            return true;
        }
        return renderData.rendered;
    }

    public static ModelInstanceTextureInitializer alloc() {
        return pool.alloc();
    }

    public void release() {
        this.item = null;
        this.modelInstance = null;
        pool.release(this);
    }

    private static final class RenderData {
        int changeNumber;
        boolean rendered;
        EquippedTextureCreator textureCreator;

        private RenderData() {
        }
    }
}


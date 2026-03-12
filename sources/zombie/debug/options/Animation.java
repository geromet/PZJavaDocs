/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;
import zombie.debug.options.OptionGroup;

public final class Animation
extends OptionGroup {
    public final AnimLayerOG animLayer = this.newOptionGroup(new AnimLayerOG());
    public final SharedSkelesOG sharedSkeles = this.newOptionGroup(new SharedSkelesOG());
    public final BooleanDebugOption dancingDoors = this.newDebugOnlyOption("DancingDoors", false);
    public final BooleanDebugOption debug = this.newDebugOnlyOption("Debug", false);
    public final BooleanDebugOption disableRagdolls = this.newDebugOnlyOption("DisableRagdolls", false);
    public final BooleanDebugOption allowEarlyTransitionOut = this.newDebugOnlyOption("AllowEarlyTransitionOut", true);
    public final BooleanDebugOption animRenderPicker = this.newDebugOnlyOption("Render.Picker", false);
    public final BooleanDebugOption blendUseFbx = this.newDebugOnlyOption("BlendUseFbx", false);
    public final BooleanDebugOption disableAnimationBlends = this.newDebugOnlyOption("DisableAnimationBlends", false);

    public static final class AnimLayerOG
    extends OptionGroup {
        public final BooleanDebugOption logStateChanges = this.newDebugOnlyOption("Debug.LogStateChanges", false);
        public final BooleanDebugOption allowAnimNodeOverride = this.newDebugOnlyOption("Debug.AllowAnimNodeOverride", false);
        public final BooleanDebugOption logNodeConditions = this.newDebugOnlyOption("Debug.LogNodeConditions", false);
    }

    public static final class SharedSkelesOG
    extends OptionGroup {
        public final BooleanDebugOption enabled = this.newDebugOnlyOption("Enabled", true);
        public final BooleanDebugOption allowLerping = this.newDebugOnlyOption("AllowLerping", true);
    }
}


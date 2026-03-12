/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import zombie.characterTextures.BloodBodyPartType;
import zombie.debug.DebugType;
import zombie.util.Pool;

public final class CharacterMask {
    private final boolean[] visibleFlags = CharacterMask.createFlags(Part.values().length, true);

    public boolean isBloodBodyPartVisible(BloodBodyPartType bpt) {
        for (Part cmp : bpt.getCharacterMaskParts()) {
            if (!this.isPartVisible(cmp)) continue;
            return true;
        }
        return false;
    }

    private static boolean[] createFlags(int length, boolean val) {
        boolean[] flags = new boolean[length];
        for (int i = 0; i < length; ++i) {
            flags[i] = val;
        }
        return flags;
    }

    public void setAllVisible(boolean isVisible) {
        Arrays.fill(this.visibleFlags, isVisible);
    }

    public void copyFrom(CharacterMask rhs) {
        System.arraycopy(rhs.visibleFlags, 0, this.visibleFlags, 0, this.visibleFlags.length);
    }

    public void setPartVisible(Part part, boolean isVisible) {
        if (part.hasSubdivisions()) {
            for (Part sub : part.subDivisions()) {
                this.setPartVisible(sub, isVisible);
            }
        } else {
            this.visibleFlags[part.getValue()] = isVisible;
        }
    }

    public void setPartsVisible(ArrayList<Integer> parts, boolean isVisible) {
        for (int i = 0; i < parts.size(); ++i) {
            int maskIndex = parts.get(i);
            Part part = Part.fromInt(maskIndex);
            if (part == null) {
                DebugType.Clothing.warn("MaskValue out of bounds: %s", maskIndex);
                continue;
            }
            this.setPartVisible(part, isVisible);
        }
    }

    public boolean isPartVisible(Part part) {
        if (part == null) {
            return false;
        }
        if (part.hasSubdivisions()) {
            boolean allSubdivsSet = true;
            for (int i = 0; allSubdivsSet && i < part.subDivisions().length; ++i) {
                Part sub = part.subDivisions()[i];
                allSubdivsSet = this.visibleFlags[sub.getValue()];
            }
            return allSubdivsSet;
        }
        return this.visibleFlags[part.getValue()];
    }

    public boolean isTorsoVisible() {
        return this.isPartVisible(Part.Torso);
    }

    public String toString() {
        return this.getClass().getSimpleName() + "{VisibleFlags:(" + this.contentsToString() + ")}";
    }

    public String contentsToString() {
        if (this.isAllVisible()) {
            return "All Visible";
        }
        if (this.isNothingVisible()) {
            return "Nothing Visible";
        }
        StringBuilder builder = new StringBuilder();
        int maskCount = 0;
        for (int i = 0; i < Part.leaves().length; ++i) {
            Part part = Part.leaves()[i];
            if (!this.isPartVisible(part)) continue;
            if (maskCount > 0) {
                builder.append(',');
            }
            builder.append((Object)part);
            ++maskCount;
        }
        return builder.toString();
    }

    private boolean isAll(boolean val) {
        boolean isAll = true;
        int count = Part.leaves().length;
        for (int i = 0; isAll && i < count; ++i) {
            Part part = Part.leaves()[i];
            isAll = this.isPartVisible(part) == val;
        }
        return isAll;
    }

    public boolean isNothingVisible() {
        return this.isAll(false);
    }

    public boolean isAllVisible() {
        return this.isAll(true);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void forEachVisible(Consumer<Part> action) {
        try {
            for (int i = 0; i < Part.leaves().length; ++i) {
                Part part = Part.leaves()[i];
                if (!this.isPartVisible(part)) continue;
                action.accept(part);
            }
        }
        finally {
            Pool.tryRelease(action);
        }
    }

    public static enum Part {
        Head(0),
        Torso(1, true),
        Pelvis(2, true),
        LeftArm(3),
        LeftHand(4),
        RightArm(5),
        RightHand(6),
        LeftLeg(7),
        LeftFoot(8),
        RightLeg(9),
        RightFoot(10),
        Dress(11),
        Chest(12, Torso),
        Waist(13, Torso),
        Belt(14, Pelvis),
        Crotch(15, Pelvis);

        private final int value;
        private final Part parent;
        private final boolean isSubdivided;
        private Part[] subDivisions;
        private BloodBodyPartType[] bloodBodyPartTypes;
        private static final Part[] s_leaves;

        private Part(int value) {
            this.value = value;
            this.parent = null;
            this.isSubdivided = false;
        }

        private Part(int value, Part parent) {
            this.value = value;
            this.parent = parent;
            this.isSubdivided = false;
        }

        private Part(int value, boolean isSubdivided) {
            this.value = value;
            this.parent = null;
            this.isSubdivided = isSubdivided;
        }

        public static int count() {
            return Part.values().length;
        }

        public static Part[] leaves() {
            return s_leaves;
        }

        public static Part fromInt(int index) {
            return index >= 0 && index < Part.count() ? Part.values()[index] : null;
        }

        public int getValue() {
            return this.value;
        }

        public Part getParent() {
            return this.parent;
        }

        public boolean isSubdivision() {
            return this.parent != null;
        }

        public boolean hasSubdivisions() {
            return this.isSubdivided;
        }

        public Part[] subDivisions() {
            if (this.subDivisions != null) {
                return this.subDivisions;
            }
            if (!this.isSubdivided) {
                this.subDivisions = new Part[0];
            }
            ArrayList<Part> subDivsList = new ArrayList<Part>();
            for (Part part : Part.values()) {
                if (part.parent != this) continue;
                subDivsList.add(part);
            }
            this.subDivisions = subDivsList.toArray(new Part[0]);
            return this.subDivisions;
        }

        private static Part[] leavesInternal() {
            ArrayList<Part> leavesList = new ArrayList<Part>();
            for (Part part : Part.values()) {
                if (part.hasSubdivisions()) continue;
                leavesList.add(part);
            }
            return leavesList.toArray(new Part[0]);
        }

        public BloodBodyPartType[] getBloodBodyPartTypes() {
            if (this.bloodBodyPartTypes != null) {
                return this.bloodBodyPartTypes;
            }
            ArrayList<BloodBodyPartType> types = new ArrayList<BloodBodyPartType>();
            switch (this.ordinal()) {
                case 0: {
                    types.add(BloodBodyPartType.Head);
                    break;
                }
                case 1: {
                    types.add(BloodBodyPartType.Torso_Upper);
                    types.add(BloodBodyPartType.Torso_Lower);
                    break;
                }
                case 2: {
                    types.add(BloodBodyPartType.UpperLeg_L);
                    types.add(BloodBodyPartType.UpperLeg_R);
                    types.add(BloodBodyPartType.Groin);
                    break;
                }
                case 3: {
                    types.add(BloodBodyPartType.UpperArm_L);
                    types.add(BloodBodyPartType.ForeArm_L);
                    break;
                }
                case 4: {
                    types.add(BloodBodyPartType.Hand_L);
                    break;
                }
                case 5: {
                    types.add(BloodBodyPartType.UpperArm_R);
                    types.add(BloodBodyPartType.ForeArm_R);
                    break;
                }
                case 6: {
                    types.add(BloodBodyPartType.Hand_R);
                    break;
                }
                case 7: {
                    types.add(BloodBodyPartType.UpperLeg_L);
                    types.add(BloodBodyPartType.LowerLeg_L);
                    break;
                }
                case 8: {
                    types.add(BloodBodyPartType.Foot_L);
                    break;
                }
                case 9: {
                    types.add(BloodBodyPartType.UpperLeg_R);
                    types.add(BloodBodyPartType.LowerLeg_R);
                    break;
                }
                case 10: {
                    types.add(BloodBodyPartType.Foot_R);
                    break;
                }
                case 11: {
                    break;
                }
                case 12: {
                    types.add(BloodBodyPartType.Torso_Upper);
                    break;
                }
                case 13: {
                    types.add(BloodBodyPartType.Torso_Lower);
                    break;
                }
                case 14: {
                    types.add(BloodBodyPartType.UpperLeg_L);
                    types.add(BloodBodyPartType.UpperLeg_R);
                    break;
                }
                case 15: {
                    types.add(BloodBodyPartType.Groin);
                }
            }
            this.bloodBodyPartTypes = new BloodBodyPartType[types.size()];
            for (int i = 0; i < types.size(); ++i) {
                this.bloodBodyPartTypes[i] = (BloodBodyPartType)((Object)types.get(i));
            }
            return this.bloodBodyPartTypes;
        }

        static {
            s_leaves = Part.leavesInternal();
        }
    }
}


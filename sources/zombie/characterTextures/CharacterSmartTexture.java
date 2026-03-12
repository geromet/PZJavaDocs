/*
 * Decompiled with CFR 0.152.
 */
package zombie.characterTextures;

import org.lwjgl.opengl.GL11;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.core.textures.SmartTexture;
import zombie.core.textures.TextureCombinerCommand;
import zombie.core.textures.TextureCombinerShaderParam;

public final class CharacterSmartTexture
extends SmartTexture {
    public static final int BODY_CATEGORY = 0;
    public static final int CLOTHING_BOTTOM_CATEGORY = 1;
    public static final int CLOTHING_TOP_CATEGORY = 2;
    public static final int CLOTHING_ITEM_CATEGORY = 3;
    public static final int DECAL_OVERLAY_CATEGORY = 300;
    public static final int DIRT_OVERLAY_CATEGORY = 400;
    public static final String[] MaskFiles = new String[]{"BloodMaskHandL", "BloodMaskHandR", "BloodMaskLArmL", "BloodMaskLArmR", "BloodMaskUArmL", "BloodMaskUArmR", "BloodMaskChest", "BloodMaskStomach", "BloodMaskHead", "BloodMaskNeck", "BloodMaskGroin", "BloodMaskULegL", "BloodMaskULegR", "BloodMaskLLegL", "BloodMaskLLegR", "BloodMaskFootL", "BloodMaskFootR", "BloodMaskBack"};
    public static final String[] BasicPatchesMaskFiles = new String[]{"patches_left_hand_sheet", "patches_right_hand_sheet", "patches_left_lower_arm_sheet", "patches_right_lower_arm_sheet", "patches_left_upper_arm_sheet", "patches_right_upper_arm_sheet", "patches_chest_sheet", "patches_abdomen_sheet", "", "", "patches_groin_sheet", "patches_left_upper_leg_sheet", "patches_right_upper_leg_sheet", "patches_left_lower_leg_sheet", "patches_right_lower_leg_sheet", "", "", "patches_back_sheet"};
    public static final String[] DenimPatchesMaskFiles = new String[]{"patches_left_hand_denim", "patches_right_hand_denim", "patches_left_lower_arm_denim", "patches_right_lower_arm_denim", "patches_left_upper_arm_denim", "patches_right_upper_arm_denim", "patches_chest_denim", "patches_abdomen_denim", "", "", "patches_groin_denim", "patches_left_upper_leg_denim", "patches_right_upper_leg_denim", "patches_left_lower_leg_denim", "patches_right_lower_leg_denim", "", "", "patches_back_denim"};
    public static final String[] LeatherPatchesMaskFiles = new String[]{"patches_left_hand_leather", "patches_right_hand_leather", "patches_left_lower_arm_leather", "patches_right_lower_arm_leather", "patches_left_upper_arm_leather", "patches_right_upper_arm_leather", "patches_chest_leather", "patches_abdomen_leather", "", "", "patches_groin_leather", "patches_left_upper_leg_leather", "patches_right_upper_leg_leather", "patches_left_lower_leg_leather", "patches_right_lower_leg_leather", "", "", "patches_back_leather"};

    public void setBlood(BloodBodyPartType bodyPart, float intensity) {
        intensity = Math.max(0.0f, Math.min(1.0f, intensity));
        int category = 300 + bodyPart.index();
        TextureCombinerCommand com = this.getFirstFromCategory(category);
        if (com != null) {
            for (int i = 0; i < com.shaderParams.size(); ++i) {
                TextureCombinerShaderParam shaderParam = com.shaderParams.get(i);
                if (!shaderParam.name.equals("intensity") || shaderParam.min == intensity && shaderParam.max == intensity) continue;
                shaderParam.min = shaderParam.max = intensity;
                this.setDirty();
            }
        } else if (intensity > 0.0f) {
            String mask = "media/textures/BloodTextures/" + MaskFiles[bodyPart.index()] + ".png";
            this.addOverlay("media/textures/BloodTextures/BloodOverlay.png", mask, intensity, category);
        }
    }

    public void setDirt(BloodBodyPartType bodyPart, float intensity) {
        intensity = Math.max(0.0f, Math.min(1.0f, intensity));
        int category = 400 + bodyPart.index();
        TextureCombinerCommand com = this.getFirstFromCategory(category);
        if (com != null) {
            for (int i = 0; i < com.shaderParams.size(); ++i) {
                TextureCombinerShaderParam shaderParam = com.shaderParams.get(i);
                if (!shaderParam.name.equals("intensity") || shaderParam.min == intensity && shaderParam.max == intensity) continue;
                shaderParam.min = shaderParam.max = intensity;
                this.setDirty();
            }
        } else if (intensity > 0.0f) {
            String mask = "media/textures/BloodTextures/" + MaskFiles[bodyPart.index()] + ".png";
            this.addDirtOverlay("media/textures/BloodTextures/GrimeOverlay.png", mask, intensity, category);
        }
    }

    public void removeBlood() {
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            this.removeBlood(BloodBodyPartType.FromIndex(i));
        }
    }

    public void removeBlood(BloodBodyPartType bodyPart) {
        TextureCombinerCommand com = this.getFirstFromCategory(300 + bodyPart.index());
        if (com != null) {
            for (int i = 0; i < com.shaderParams.size(); ++i) {
                TextureCombinerShaderParam shaderParam = com.shaderParams.get(i);
                if (!shaderParam.name.equals("intensity") || shaderParam.min == 0.0f && shaderParam.max == 0.0f) continue;
                shaderParam.max = 0.0f;
                shaderParam.min = 0.0f;
                this.setDirty();
            }
        }
    }

    public float addBlood(BloodBodyPartType bodyPart, float intensity, IsoGameCharacter chr) {
        int category = 300 + bodyPart.index();
        TextureCombinerCommand com = this.getFirstFromCategory(category);
        if (bodyPart == BloodBodyPartType.Head && chr != null) {
            if (chr.hair != null) {
                chr.hair.tintR -= 0.022f;
                if (chr.hair.tintR < 0.0f) {
                    chr.hair.tintR = 0.0f;
                }
                chr.hair.tintG -= 0.03f;
                if (chr.hair.tintG < 0.0f) {
                    chr.hair.tintG = 0.0f;
                }
                chr.hair.tintB -= 0.03f;
                if (chr.hair.tintB < 0.0f) {
                    chr.hair.tintB = 0.0f;
                }
            }
            if (chr.beard != null) {
                chr.beard.tintR -= 0.022f;
                if (chr.beard.tintR < 0.0f) {
                    chr.beard.tintR = 0.0f;
                }
                chr.beard.tintG -= 0.03f;
                if (chr.beard.tintG < 0.0f) {
                    chr.beard.tintG = 0.0f;
                }
                chr.beard.tintB -= 0.03f;
                if (chr.beard.tintB < 0.0f) {
                    chr.beard.tintB = 0.0f;
                }
            }
        }
        if (com != null) {
            for (int i = 0; i < com.shaderParams.size(); ++i) {
                TextureCombinerShaderParam shaderParam = com.shaderParams.get(i);
                if (!shaderParam.name.equals("intensity")) continue;
                float in = shaderParam.min;
                in += intensity;
                if (shaderParam.min != (in = Math.min(1.0f, in)) || shaderParam.max != in) {
                    shaderParam.min = shaderParam.max = in;
                    this.setDirty();
                }
                return in;
            }
        } else {
            String mask = "media/textures/BloodTextures/" + MaskFiles[bodyPart.index()] + ".png";
            this.addOverlay("media/textures/BloodTextures/BloodOverlay.png", mask, intensity, category);
        }
        return intensity;
    }

    public float addDirt(BloodBodyPartType bodyPart, float intensity, IsoGameCharacter chr) {
        int category = 400 + bodyPart.index();
        TextureCombinerCommand com = this.getFirstFromCategory(category);
        if (bodyPart == BloodBodyPartType.Head && chr != null) {
            if (chr.hair != null) {
                chr.hair.tintR -= 0.022f;
                if (chr.hair.tintR < 0.0f) {
                    chr.hair.tintR = 0.0f;
                }
                chr.hair.tintG -= 0.03f;
                if (chr.hair.tintG < 0.0f) {
                    chr.hair.tintG = 0.0f;
                }
                chr.hair.tintB -= 0.03f;
                if (chr.hair.tintB < 0.0f) {
                    chr.hair.tintB = 0.0f;
                }
            }
            if (chr.beard != null) {
                chr.beard.tintR -= 0.022f;
                if (chr.beard.tintR < 0.0f) {
                    chr.beard.tintR = 0.0f;
                }
                chr.beard.tintG -= 0.03f;
                if (chr.beard.tintG < 0.0f) {
                    chr.beard.tintG = 0.0f;
                }
                chr.beard.tintB -= 0.03f;
                if (chr.beard.tintB < 0.0f) {
                    chr.beard.tintB = 0.0f;
                }
            }
        }
        if (com != null) {
            for (int i = 0; i < com.shaderParams.size(); ++i) {
                TextureCombinerShaderParam shaderParam = com.shaderParams.get(i);
                if (!shaderParam.name.equals("intensity")) continue;
                float in = shaderParam.min;
                in += intensity;
                if (shaderParam.min != (in = Math.min(1.0f, in)) || shaderParam.max != in) {
                    shaderParam.min = shaderParam.max = in;
                    this.setDirty();
                }
                return in;
            }
        } else {
            String mask = "media/textures/BloodTextures/" + MaskFiles[bodyPart.index()] + ".png";
            this.addDirtOverlay("media/textures/BloodTextures/GrimeOverlay.png", mask, intensity, category);
        }
        return intensity;
    }

    public void addShirtDecal(String dec) {
        GL11.glTexParameteri(3553, 10241, 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        this.addRect(dec, 102, 118, 52, 52);
    }
}


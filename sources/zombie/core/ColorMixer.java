/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import zombie.core.Color;

public class ColorMixer {
    public static Color LerpLCH(Color col1, Color col2, float delta, Color dest) {
        if (col1.r == col1.g && col1.r == col1.b || col2.r == col2.g && col2.r == col2.b) {
            col1.interp(col2, delta, dest);
            return dest;
        }
        float[] xyz1 = new float[3];
        float[] lab1 = new float[3];
        float[] lch1 = new float[3];
        float[] xyz2 = new float[3];
        float[] lab2 = new float[3];
        float[] lch2 = new float[3];
        ColorMixer.ColorToXYZ(col1, xyz1);
        ColorMixer.XYZToLab(xyz1, lab1);
        ColorMixer.LabToLCH(lab1, lch1);
        ColorMixer.ColorToXYZ(col2, xyz2);
        ColorMixer.XYZToLab(xyz2, lab2);
        ColorMixer.LabToLCH(lab2, lch2);
        float[] lch = new float[3];
        float[] lab = new float[3];
        float[] xyz = new float[3];
        if (lch2[2] > lch1[2]) {
            if (lch2[2] - lch1[2] > lch1[2] + 360.0f - lch2[2]) {
                lch1[2] = lch1[2] + 360.0f;
            }
        } else if (lch1[2] - lch2[2] > lch2[2] + 360.0f - lch1[2]) {
            lch2[2] = lch2[2] + 360.0f;
        }
        lch[0] = lch2[0] * delta + lch1[0] * (1.0f - delta);
        lch[1] = lch2[1] * delta + lch1[1] * (1.0f - delta);
        lch[2] = lch2[2] * delta + lch1[2] * (1.0f - delta);
        if (lch[2] > 360.0f) {
            lch[2] = lch[2] - 360.0f;
        }
        ColorMixer.LCHToLab(lch, lab);
        ColorMixer.LabToXYZ(lab, xyz);
        return ColorMixer.XYZToRGB(xyz[0], xyz[1], xyz[2], dest);
    }

    public static void ColorToXYZ(Color color, float[] outXYZ) {
        float[] rgb = new float[3];
        float[] xyz = new float[3];
        rgb[0] = color.getRedFloat();
        rgb[1] = color.getGreenFloat();
        rgb[2] = color.getBlueFloat();
        rgb[0] = rgb[0] > 0.04045f ? (float)Math.pow(((double)rgb[0] + 0.055) / 1.055, 2.4) : rgb[0] / 12.92f;
        rgb[1] = rgb[1] > 0.04045f ? (float)Math.pow(((double)rgb[1] + 0.055) / 1.055, 2.4) : rgb[1] / 12.92f;
        rgb[2] = rgb[2] > 0.04045f ? (float)Math.pow(((double)rgb[2] + 0.055) / 1.055, 2.4) : rgb[2] / 12.92f;
        rgb[0] = rgb[0] * 100.0f;
        rgb[1] = rgb[1] * 100.0f;
        rgb[2] = rgb[2] * 100.0f;
        xyz[0] = rgb[0] * 0.412453f + rgb[1] * 0.35758f + rgb[2] * 0.180423f;
        xyz[1] = rgb[0] * 0.212671f + rgb[1] * 0.71516f + rgb[2] * 0.072169f;
        xyz[2] = rgb[0] * 0.019334f + rgb[1] * 0.119193f + rgb[2] * 0.950227f;
        outXYZ[0] = xyz[0];
        outXYZ[1] = xyz[1];
        outXYZ[2] = xyz[2];
    }

    private static void XYZToLab(float[] xyz, float[] outLAB) {
        xyz[0] = xyz[0] / 95.047f;
        xyz[1] = xyz[1] / 100.0f;
        xyz[2] = xyz[2] / 108.883f;
        xyz[0] = xyz[0] > 0.008856f ? (float)Math.pow(xyz[0], 0.3333333333333333) : xyz[0] * 7.787f + 0.13793103f;
        xyz[1] = xyz[1] > 0.008856f ? (float)Math.pow(xyz[1], 0.3333333333333333) : xyz[1] * 7.787f + 0.13793103f;
        xyz[2] = xyz[2] > 0.008856f ? (float)Math.pow(xyz[2], 0.3333333333333333) : xyz[2] * 7.787f + 0.13793103f;
        outLAB[0] = 116.0f * xyz[1] - 16.0f;
        outLAB[1] = 500.0f * (xyz[0] - xyz[1]);
        outLAB[2] = 200.0f * (xyz[1] - xyz[2]);
    }

    private static void LabToLCH(float[] lab, float[] outLCH) {
        outLCH[0] = lab[0];
        outLCH[1] = (float)Math.sqrt(lab[1] * lab[1] + lab[2] * lab[2]);
        outLCH[2] = (float)Math.atan2(lab[2], lab[1]);
        outLCH[2] = (float)((double)outLCH[2] * 57.29577951308232);
        if (outLCH[2] < 0.0f) {
            outLCH[2] = outLCH[2] + 360.0f;
        }
    }

    private static void LCHToLab(float[] lch, float[] outLAB) {
        outLAB[0] = lch[0];
        outLAB[1] = lch[1] * (float)Math.cos((double)lch[2] * (Math.PI / 180));
        outLAB[2] = lch[1] * (float)Math.sin((double)lch[2] * (Math.PI / 180));
    }

    private static void LabToXYZ(float[] lab, float[] outXYZ) {
        float[] xyz = new float[3];
        xyz[1] = (lab[0] + 16.0f) / 116.0f;
        xyz[0] = lab[1] / 500.0f + xyz[1];
        xyz[2] = xyz[1] - lab[2] / 200.0f;
        for (int i = 0; i < 3; ++i) {
            float pow = xyz[i] * xyz[i] * xyz[i];
            float ratio = 0.20689656f;
            xyz[i] = xyz[i] > 0.20689656f ? pow : 0.12841856f * (xyz[i] - 0.13793103f);
        }
        outXYZ[0] = xyz[0] * 95.047f;
        outXYZ[1] = xyz[1] * 100.0f;
        outXYZ[2] = xyz[2] * 108.883f;
    }

    public static Color XYZToRGB(float x, float y, float z, Color target) {
        int i;
        float[] xyz = new float[]{x, y, z};
        float[] rgb = new float[3];
        for (i = 0; i < 3; ++i) {
            xyz[i] = xyz[i] / 100.0f;
        }
        rgb[0] = xyz[0] * 3.240479f + xyz[1] * -1.53715f + xyz[2] * -0.498535f;
        rgb[1] = xyz[0] * -0.969256f + xyz[1] * 1.875992f + xyz[2] * 0.041556f;
        rgb[2] = xyz[0] * 0.055648f + xyz[1] * -0.204043f + xyz[2] * 1.057311f;
        for (i = 0; i < 3; ++i) {
            rgb[i] = rgb[i] > 0.0031308f ? 1.055f * (float)Math.pow(rgb[i], 0.4166666567325592) - 0.055f : rgb[i] * 12.92f;
        }
        rgb[0] = Math.min(Math.max(rgb[0] * 255.0f, 0.0f), 255.0f);
        rgb[1] = Math.min(Math.max(rgb[1] * 255.0f, 0.0f), 255.0f);
        rgb[2] = Math.min(Math.max(rgb[2] * 255.0f, 0.0f), 255.0f);
        target.set(rgb[0] / 255.0f, rgb[1] / 255.0f, rgb[2] / 255.0f);
        return target;
    }
}


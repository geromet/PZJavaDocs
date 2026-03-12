/*
 * Decompiled with CFR 0.152.
 */
package zombie.creative.creativerects;

public class OpenSimplexNoise {
    private static final double STRETCH_CONSTANT_2D = -0.211324865405187;
    private static final double SQUISH_CONSTANT_2D = 0.366025403784439;
    private static final double STRETCH_CONSTANT_3D = -0.16666666666666666;
    private static final double SQUISH_CONSTANT_3D = 0.3333333333333333;
    private static final double STRETCH_CONSTANT_4D = -0.138196601125011;
    private static final double SQUISH_CONSTANT_4D = 0.309016994374947;
    private static final double NORM_CONSTANT_2D = 47.0;
    private static final double NORM_CONSTANT_3D = 103.0;
    private static final double NORM_CONSTANT_4D = 30.0;
    private static final long DEFAULT_SEED = 0L;
    private final short[] perm;
    private final short[] permGradIndex3d;
    private static final byte[] gradients2D = new byte[]{5, 2, 2, 5, -5, 2, -2, 5, 5, -2, 2, -5, -5, -2, -2, -5};
    private static final byte[] gradients3D = new byte[]{-11, 4, 4, -4, 11, 4, -4, 4, 11, 11, 4, 4, 4, 11, 4, 4, 4, 11, -11, -4, 4, -4, -11, 4, -4, -4, 11, 11, -4, 4, 4, -11, 4, 4, -4, 11, -11, 4, -4, -4, 11, -4, -4, 4, -11, 11, 4, -4, 4, 11, -4, 4, 4, -11, -11, -4, -4, -4, -11, -4, -4, -4, -11, 11, -4, -4, 4, -11, -4, 4, -4, -11};
    private static final byte[] gradients4D = new byte[]{3, 1, 1, 1, 1, 3, 1, 1, 1, 1, 3, 1, 1, 1, 1, 3, -3, 1, 1, 1, -1, 3, 1, 1, -1, 1, 3, 1, -1, 1, 1, 3, 3, -1, 1, 1, 1, -3, 1, 1, 1, -1, 3, 1, 1, -1, 1, 3, -3, -1, 1, 1, -1, -3, 1, 1, -1, -1, 3, 1, -1, -1, 1, 3, 3, 1, -1, 1, 1, 3, -1, 1, 1, 1, -3, 1, 1, 1, -1, 3, -3, 1, -1, 1, -1, 3, -1, 1, -1, 1, -3, 1, -1, 1, -1, 3, 3, -1, -1, 1, 1, -3, -1, 1, 1, -1, -3, 1, 1, -1, -1, 3, -3, -1, -1, 1, -1, -3, -1, 1, -1, -1, -3, 1, -1, -1, -1, 3, 3, 1, 1, -1, 1, 3, 1, -1, 1, 1, 3, -1, 1, 1, 1, -3, -3, 1, 1, -1, -1, 3, 1, -1, -1, 1, 3, -1, -1, 1, 1, -3, 3, -1, 1, -1, 1, -3, 1, -1, 1, -1, 3, -1, 1, -1, 1, -3, -3, -1, 1, -1, -1, -3, 1, -1, -1, -1, 3, -1, -1, -1, 1, -3, 3, 1, -1, -1, 1, 3, -1, -1, 1, 1, -3, -1, 1, 1, -1, -3, -3, 1, -1, -1, -1, 3, -1, -1, -1, 1, -3, -1, -1, 1, -1, -3, 3, -1, -1, -1, 1, -3, -1, -1, 1, -1, -3, -1, 1, -1, -1, -3, -3, -1, -1, -1, -1, -3, -1, -1, -1, -1, -3, -1, -1, -1, -1, -3};

    public OpenSimplexNoise() {
        this(0L);
    }

    public OpenSimplexNoise(short[] perm) {
        this.perm = perm;
        this.permGradIndex3d = new short[256];
        for (int i = 0; i < 256; ++i) {
            this.permGradIndex3d[i] = (short)(perm[i] % (gradients3D.length / 3) * 3);
        }
    }

    public OpenSimplexNoise(long seed) {
        int i;
        this.perm = new short[256];
        this.permGradIndex3d = new short[256];
        short[] source2 = new short[256];
        for (i = 0; i < 256; i = (int)((short)(i + 1))) {
            source2[i] = i;
        }
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        for (i = 255; i >= 0; --i) {
            int r = (int)(((seed = seed * 6364136223846793005L + 1442695040888963407L) + 31L) % (long)(i + 1));
            if (r < 0) {
                r += i + 1;
            }
            this.perm[i] = source2[r];
            this.permGradIndex3d[i] = (short)(this.perm[i] % (gradients3D.length / 3) * 3);
            source2[r] = source2[i];
        }
    }

    public double eval(double x, double y) {
        double attnExt;
        double dyExt;
        double dxExt;
        int ysvExt;
        int xsvExt;
        double dy2;
        double dx2;
        double attn2;
        double stretchOffset = (x + y) * -0.211324865405187;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;
        int xsb = OpenSimplexNoise.fastFloor(xs);
        int ysb = OpenSimplexNoise.fastFloor(ys);
        double squishOffset = (double)(xsb + ysb) * 0.366025403784439;
        double xb = (double)xsb + squishOffset;
        double yb = (double)ysb + squishOffset;
        double xins = xs - (double)xsb;
        double yins = ys - (double)ysb;
        double inSum = xins + yins;
        double dx0 = x - xb;
        double dy0 = y - yb;
        double value = 0.0;
        double dx1 = dx0 - 1.0 - 0.366025403784439;
        double dy1 = dy0 - 0.0 - 0.366025403784439;
        double attn1 = 2.0 - dx1 * dx1 - dy1 * dy1;
        if (attn1 > 0.0) {
            attn1 *= attn1;
            value += attn1 * attn1 * this.extrapolate(xsb + 1, ysb + 0, dx1, dy1);
        }
        if ((attn2 = 2.0 - (dx2 = dx0 - 0.0 - 0.366025403784439) * dx2 - (dy2 = dy0 - 1.0 - 0.366025403784439) * dy2) > 0.0) {
            attn2 *= attn2;
            value += attn2 * attn2 * this.extrapolate(xsb + 0, ysb + 1, dx2, dy2);
        }
        if (inSum <= 1.0) {
            zins = 1.0 - inSum;
            if (zins > xins || zins > yins) {
                if (xins > yins) {
                    xsvExt = xsb + 1;
                    ysvExt = ysb - 1;
                    dxExt = dx0 - 1.0;
                    dyExt = dy0 + 1.0;
                } else {
                    xsvExt = xsb - 1;
                    ysvExt = ysb + 1;
                    dxExt = dx0 + 1.0;
                    dyExt = dy0 - 1.0;
                }
            } else {
                xsvExt = xsb + 1;
                ysvExt = ysb + 1;
                dxExt = dx0 - 1.0 - 0.732050807568878;
                dyExt = dy0 - 1.0 - 0.732050807568878;
            }
        } else {
            zins = 2.0 - inSum;
            if (zins < xins || zins < yins) {
                if (xins > yins) {
                    xsvExt = xsb + 2;
                    ysvExt = ysb + 0;
                    dxExt = dx0 - 2.0 - 0.732050807568878;
                    dyExt = dy0 + 0.0 - 0.732050807568878;
                } else {
                    xsvExt = xsb + 0;
                    ysvExt = ysb + 2;
                    dxExt = dx0 + 0.0 - 0.732050807568878;
                    dyExt = dy0 - 2.0 - 0.732050807568878;
                }
            } else {
                dxExt = dx0;
                dyExt = dy0;
                xsvExt = xsb;
                ysvExt = ysb;
            }
            ++xsb;
            ++ysb;
            dx0 = dx0 - 1.0 - 0.732050807568878;
            dy0 = dy0 - 1.0 - 0.732050807568878;
        }
        double attn0 = 2.0 - dx0 * dx0 - dy0 * dy0;
        if (attn0 > 0.0) {
            attn0 *= attn0;
            value += attn0 * attn0 * this.extrapolate(xsb, ysb, dx0, dy0);
        }
        if ((attnExt = 2.0 - dxExt * dxExt - dyExt * dyExt) > 0.0) {
            attnExt *= attnExt;
            value += attnExt * attnExt * this.extrapolate(xsvExt, ysvExt, dxExt, dyExt);
        }
        return value / 47.0;
    }

    public double eval(double x, double y, double z) {
        double attnExt1;
        double dzExt1;
        double dzExt0;
        int zsvExt1;
        int zsvExt0;
        double dyExt0;
        double dyExt1;
        int ysvExt0;
        int ysvExt1;
        double dxExt1;
        double dxExt0;
        int xsvExt1;
        int xsvExt0;
        double stretchOffset = (x + y + z) * -0.16666666666666666;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;
        double zs = z + stretchOffset;
        int xsb = OpenSimplexNoise.fastFloor(xs);
        int ysb = OpenSimplexNoise.fastFloor(ys);
        int zsb = OpenSimplexNoise.fastFloor(zs);
        double squishOffset = (double)(xsb + ysb + zsb) * 0.3333333333333333;
        double xb = (double)xsb + squishOffset;
        double yb = (double)ysb + squishOffset;
        double zb = (double)zsb + squishOffset;
        double xins = xs - (double)xsb;
        double yins = ys - (double)ysb;
        double zins = zs - (double)zsb;
        double inSum = xins + yins + zins;
        double dx0 = x - xb;
        double dy0 = y - yb;
        double dz0 = z - zb;
        double value = 0.0;
        if (inSum <= 1.0) {
            double dz3;
            double dy3;
            double dx3;
            double attn3;
            double dz2;
            double dy2;
            double dx2;
            double attn2;
            double dz1;
            double dy1;
            double dx1;
            double attn1;
            int aPoint = 1;
            double aScore = xins;
            int bPoint = 2;
            double bScore = yins;
            if (aScore >= bScore && zins > bScore) {
                bScore = zins;
                bPoint = 4;
            } else if (aScore < bScore && zins > aScore) {
                aScore = zins;
                aPoint = 4;
            }
            double wins = 1.0 - inSum;
            if (wins > aScore || wins > bScore) {
                int n = c = bScore > aScore ? bPoint : aPoint;
                if ((c & 1) == 0) {
                    xsvExt0 = xsb - 1;
                    xsvExt1 = xsb;
                    dxExt0 = dx0 + 1.0;
                    dxExt1 = dx0;
                } else {
                    xsvExt0 = xsvExt1 = xsb + 1;
                    dxExt0 = dxExt1 = dx0 - 1.0;
                }
                if ((c & 2) == 0) {
                    ysvExt1 = ysb;
                    ysvExt0 = ysvExt1--;
                    dyExt0 = dyExt1 = dy0;
                    if ((c & 1) == 0) {
                        dyExt1 += 1.0;
                    } else {
                        --ysvExt0;
                        dyExt0 += 1.0;
                    }
                } else {
                    ysvExt0 = ysvExt1 = ysb + 1;
                    dyExt0 = dyExt1 = dy0 - 1.0;
                }
                if ((c & 4) == 0) {
                    zsvExt0 = zsb;
                    zsvExt1 = zsb - 1;
                    dzExt0 = dz0;
                    dzExt1 = dz0 + 1.0;
                } else {
                    zsvExt0 = zsvExt1 = zsb + 1;
                    dzExt0 = dzExt1 = dz0 - 1.0;
                }
            } else {
                c = (byte)(aPoint | bPoint);
                if ((c & 1) == 0) {
                    xsvExt0 = xsb;
                    xsvExt1 = xsb - 1;
                    dxExt0 = dx0 - 0.6666666666666666;
                    dxExt1 = dx0 + 1.0 - 0.3333333333333333;
                } else {
                    xsvExt0 = xsvExt1 = xsb + 1;
                    dxExt0 = dx0 - 1.0 - 0.6666666666666666;
                    dxExt1 = dx0 - 1.0 - 0.3333333333333333;
                }
                if ((c & 2) == 0) {
                    ysvExt0 = ysb;
                    ysvExt1 = ysb - 1;
                    dyExt0 = dy0 - 0.6666666666666666;
                    dyExt1 = dy0 + 1.0 - 0.3333333333333333;
                } else {
                    ysvExt0 = ysvExt1 = ysb + 1;
                    dyExt0 = dy0 - 1.0 - 0.6666666666666666;
                    dyExt1 = dy0 - 1.0 - 0.3333333333333333;
                }
                if ((c & 4) == 0) {
                    zsvExt0 = zsb;
                    zsvExt1 = zsb - 1;
                    dzExt0 = dz0 - 0.6666666666666666;
                    dzExt1 = dz0 + 1.0 - 0.3333333333333333;
                } else {
                    zsvExt0 = zsvExt1 = zsb + 1;
                    dzExt0 = dz0 - 1.0 - 0.6666666666666666;
                    dzExt1 = dz0 - 1.0 - 0.3333333333333333;
                }
            }
            double attn0 = 2.0 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0;
            if (attn0 > 0.0) {
                attn0 *= attn0;
                value += attn0 * attn0 * this.extrapolate(xsb + 0, ysb + 0, zsb + 0, dx0, dy0, dz0);
            }
            if ((attn1 = 2.0 - (dx1 = dx0 - 1.0 - 0.3333333333333333) * dx1 - (dy1 = dy0 - 0.0 - 0.3333333333333333) * dy1 - (dz1 = dz0 - 0.0 - 0.3333333333333333) * dz1) > 0.0) {
                attn1 *= attn1;
                value += attn1 * attn1 * this.extrapolate(xsb + 1, ysb + 0, zsb + 0, dx1, dy1, dz1);
            }
            if ((attn2 = 2.0 - (dx2 = dx0 - 0.0 - 0.3333333333333333) * dx2 - (dy2 = dy0 - 1.0 - 0.3333333333333333) * dy2 - (dz2 = dz1) * dz2) > 0.0) {
                attn2 *= attn2;
                value += attn2 * attn2 * this.extrapolate(xsb + 0, ysb + 1, zsb + 0, dx2, dy2, dz2);
            }
            if ((attn3 = 2.0 - (dx3 = dx2) * dx3 - (dy3 = dy1) * dy3 - (dz3 = dz0 - 1.0 - 0.3333333333333333) * dz3) > 0.0) {
                attn3 *= attn3;
                value += attn3 * attn3 * this.extrapolate(xsb + 0, ysb + 0, zsb + 1, dx3, dy3, dz3);
            }
        } else if (inSum >= 2.0) {
            double attn0;
            double dz1;
            double dy1;
            double dx1;
            double attn1;
            double dz2;
            double dy2;
            double dx2;
            double attn2;
            int aPoint = 6;
            double aScore = xins;
            int bPoint = 5;
            double bScore = yins;
            if (aScore <= bScore && zins < bScore) {
                bScore = zins;
                bPoint = 3;
            } else if (aScore > bScore && zins < aScore) {
                aScore = zins;
                aPoint = 3;
            }
            double wins = 3.0 - inSum;
            if (wins < aScore || wins < bScore) {
                int n = c = bScore < aScore ? bPoint : aPoint;
                if ((c & 1) != 0) {
                    xsvExt0 = xsb + 2;
                    xsvExt1 = xsb + 1;
                    dxExt0 = dx0 - 2.0 - 1.0;
                    dxExt1 = dx0 - 1.0 - 1.0;
                } else {
                    xsvExt0 = xsvExt1 = xsb;
                    dxExt0 = dxExt1 = dx0 - 1.0;
                }
                if ((c & 2) != 0) {
                    ysvExt1 = ysb + 1;
                    ysvExt0 = ysvExt1++;
                    dyExt0 = dyExt1 = dy0 - 1.0 - 1.0;
                    if ((c & 1) != 0) {
                        dyExt1 -= 1.0;
                    } else {
                        ++ysvExt0;
                        dyExt0 -= 1.0;
                    }
                } else {
                    ysvExt0 = ysvExt1 = ysb;
                    dyExt0 = dyExt1 = dy0 - 1.0;
                }
                if ((c & 4) != 0) {
                    zsvExt0 = zsb + 1;
                    zsvExt1 = zsb + 2;
                    dzExt0 = dz0 - 1.0 - 1.0;
                    dzExt1 = dz0 - 2.0 - 1.0;
                } else {
                    zsvExt0 = zsvExt1 = zsb;
                    dzExt0 = dzExt1 = dz0 - 1.0;
                }
            } else {
                c = (byte)(aPoint & bPoint);
                if ((c & 1) != 0) {
                    xsvExt0 = xsb + 1;
                    xsvExt1 = xsb + 2;
                    dxExt0 = dx0 - 1.0 - 0.3333333333333333;
                    dxExt1 = dx0 - 2.0 - 0.6666666666666666;
                } else {
                    xsvExt0 = xsvExt1 = xsb;
                    dxExt0 = dx0 - 0.3333333333333333;
                    dxExt1 = dx0 - 0.6666666666666666;
                }
                if ((c & 2) != 0) {
                    ysvExt0 = ysb + 1;
                    ysvExt1 = ysb + 2;
                    dyExt0 = dy0 - 1.0 - 0.3333333333333333;
                    dyExt1 = dy0 - 2.0 - 0.6666666666666666;
                } else {
                    ysvExt0 = ysvExt1 = ysb;
                    dyExt0 = dy0 - 0.3333333333333333;
                    dyExt1 = dy0 - 0.6666666666666666;
                }
                if ((c & 4) != 0) {
                    zsvExt0 = zsb + 1;
                    zsvExt1 = zsb + 2;
                    dzExt0 = dz0 - 1.0 - 0.3333333333333333;
                    dzExt1 = dz0 - 2.0 - 0.6666666666666666;
                } else {
                    zsvExt0 = zsvExt1 = zsb;
                    dzExt0 = dz0 - 0.3333333333333333;
                    dzExt1 = dz0 - 0.6666666666666666;
                }
            }
            double dx3 = dx0 - 1.0 - 0.6666666666666666;
            double dy3 = dy0 - 1.0 - 0.6666666666666666;
            double dz3 = dz0 - 0.0 - 0.6666666666666666;
            double attn3 = 2.0 - dx3 * dx3 - dy3 * dy3 - dz3 * dz3;
            if (attn3 > 0.0) {
                attn3 *= attn3;
                value += attn3 * attn3 * this.extrapolate(xsb + 1, ysb + 1, zsb + 0, dx3, dy3, dz3);
            }
            if ((attn2 = 2.0 - (dx2 = dx3) * dx2 - (dy2 = dy0 - 0.0 - 0.6666666666666666) * dy2 - (dz2 = dz0 - 1.0 - 0.6666666666666666) * dz2) > 0.0) {
                attn2 *= attn2;
                value += attn2 * attn2 * this.extrapolate(xsb + 1, ysb + 0, zsb + 1, dx2, dy2, dz2);
            }
            if ((attn1 = 2.0 - (dx1 = dx0 - 0.0 - 0.6666666666666666) * dx1 - (dy1 = dy3) * dy1 - (dz1 = dz2) * dz1) > 0.0) {
                attn1 *= attn1;
                value += attn1 * attn1 * this.extrapolate(xsb + 0, ysb + 1, zsb + 1, dx1, dy1, dz1);
            }
            if ((attn0 = 2.0 - (dx0 = dx0 - 1.0 - 1.0) * dx0 - (dy0 = dy0 - 1.0 - 1.0) * dy0 - (dz0 = dz0 - 1.0 - 1.0) * dz0) > 0.0) {
                attn0 *= attn0;
                value += attn0 * attn0 * this.extrapolate(xsb + 1, ysb + 1, zsb + 1, dx0, dy0, dz0);
            }
        } else {
            double dz6;
            double dy6;
            double dx6;
            double attn6;
            double dz5;
            double dy5;
            double dx5;
            double attn5;
            double dz4;
            double dy4;
            double dx4;
            double attn4;
            double dz3;
            double dy3;
            double dx3;
            double attn3;
            double dz2;
            double dy2;
            double dx2;
            double attn2;
            boolean bIsFurtherSide;
            int bPoint;
            double bScore;
            int aPoint;
            double aScore;
            boolean aIsFurtherSide;
            double p1 = xins + yins;
            boolean bl = aIsFurtherSide = p1 > 1.0;
            if (aIsFurtherSide) {
                aScore = p1 - 1.0;
                aPoint = 3;
            } else {
                aScore = 1.0 - p1;
                aPoint = 4;
            }
            double p2 = xins + zins;
            if (p2 > 1.0) {
                bScore = p2 - 1.0;
                bPoint = 5;
                bIsFurtherSide = true;
            } else {
                bScore = 1.0 - p2;
                bPoint = 2;
                bIsFurtherSide = false;
            }
            double p3 = yins + zins;
            if (p3 > 1.0) {
                score = p3 - 1.0;
                if (aScore <= bScore && aScore < score) {
                    aPoint = 6;
                    aIsFurtherSide = true;
                } else if (aScore > bScore && bScore < score) {
                    bPoint = 6;
                    bIsFurtherSide = true;
                }
            } else {
                score = 1.0 - p3;
                if (aScore <= bScore && aScore < score) {
                    aPoint = 1;
                    aIsFurtherSide = false;
                } else if (aScore > bScore && bScore < score) {
                    bPoint = 1;
                    bIsFurtherSide = false;
                }
            }
            if (aIsFurtherSide == bIsFurtherSide) {
                if (aIsFurtherSide) {
                    dxExt0 = dx0 - 1.0 - 1.0;
                    dyExt0 = dy0 - 1.0 - 1.0;
                    dzExt0 = dz0 - 1.0 - 1.0;
                    xsvExt0 = xsb + 1;
                    ysvExt0 = ysb + 1;
                    zsvExt0 = zsb + 1;
                    c = (byte)(aPoint & bPoint);
                    if ((c & 1) != 0) {
                        dxExt1 = dx0 - 2.0 - 0.6666666666666666;
                        dyExt1 = dy0 - 0.6666666666666666;
                        dzExt1 = dz0 - 0.6666666666666666;
                        xsvExt1 = xsb + 2;
                        ysvExt1 = ysb;
                        zsvExt1 = zsb;
                    } else if ((c & 2) != 0) {
                        dxExt1 = dx0 - 0.6666666666666666;
                        dyExt1 = dy0 - 2.0 - 0.6666666666666666;
                        dzExt1 = dz0 - 0.6666666666666666;
                        xsvExt1 = xsb;
                        ysvExt1 = ysb + 2;
                        zsvExt1 = zsb;
                    } else {
                        dxExt1 = dx0 - 0.6666666666666666;
                        dyExt1 = dy0 - 0.6666666666666666;
                        dzExt1 = dz0 - 2.0 - 0.6666666666666666;
                        xsvExt1 = xsb;
                        ysvExt1 = ysb;
                        zsvExt1 = zsb + 2;
                    }
                } else {
                    dxExt0 = dx0;
                    dyExt0 = dy0;
                    dzExt0 = dz0;
                    xsvExt0 = xsb;
                    ysvExt0 = ysb;
                    zsvExt0 = zsb;
                    c = (byte)(aPoint | bPoint);
                    if ((c & 1) == 0) {
                        dxExt1 = dx0 + 1.0 - 0.3333333333333333;
                        dyExt1 = dy0 - 1.0 - 0.3333333333333333;
                        dzExt1 = dz0 - 1.0 - 0.3333333333333333;
                        xsvExt1 = xsb - 1;
                        ysvExt1 = ysb + 1;
                        zsvExt1 = zsb + 1;
                    } else if ((c & 2) == 0) {
                        dxExt1 = dx0 - 1.0 - 0.3333333333333333;
                        dyExt1 = dy0 + 1.0 - 0.3333333333333333;
                        dzExt1 = dz0 - 1.0 - 0.3333333333333333;
                        xsvExt1 = xsb + 1;
                        ysvExt1 = ysb - 1;
                        zsvExt1 = zsb + 1;
                    } else {
                        dxExt1 = dx0 - 1.0 - 0.3333333333333333;
                        dyExt1 = dy0 - 1.0 - 0.3333333333333333;
                        dzExt1 = dz0 + 1.0 - 0.3333333333333333;
                        xsvExt1 = xsb + 1;
                        ysvExt1 = ysb + 1;
                        zsvExt1 = zsb - 1;
                    }
                }
            } else {
                int c2;
                int c1;
                if (aIsFurtherSide) {
                    c1 = aPoint;
                    c2 = bPoint;
                } else {
                    c1 = bPoint;
                    c2 = aPoint;
                }
                if ((c1 & 1) == 0) {
                    dxExt0 = dx0 + 1.0 - 0.3333333333333333;
                    dyExt0 = dy0 - 1.0 - 0.3333333333333333;
                    dzExt0 = dz0 - 1.0 - 0.3333333333333333;
                    xsvExt0 = xsb - 1;
                    ysvExt0 = ysb + 1;
                    zsvExt0 = zsb + 1;
                } else if ((c1 & 2) == 0) {
                    dxExt0 = dx0 - 1.0 - 0.3333333333333333;
                    dyExt0 = dy0 + 1.0 - 0.3333333333333333;
                    dzExt0 = dz0 - 1.0 - 0.3333333333333333;
                    xsvExt0 = xsb + 1;
                    ysvExt0 = ysb - 1;
                    zsvExt0 = zsb + 1;
                } else {
                    dxExt0 = dx0 - 1.0 - 0.3333333333333333;
                    dyExt0 = dy0 - 1.0 - 0.3333333333333333;
                    dzExt0 = dz0 + 1.0 - 0.3333333333333333;
                    xsvExt0 = xsb + 1;
                    ysvExt0 = ysb + 1;
                    zsvExt0 = zsb - 1;
                }
                dxExt1 = dx0 - 0.6666666666666666;
                dyExt1 = dy0 - 0.6666666666666666;
                dzExt1 = dz0 - 0.6666666666666666;
                xsvExt1 = xsb;
                ysvExt1 = ysb;
                zsvExt1 = zsb;
                if ((c2 & 1) != 0) {
                    dxExt1 -= 2.0;
                    xsvExt1 += 2;
                } else if ((c2 & 2) != 0) {
                    dyExt1 -= 2.0;
                    ysvExt1 += 2;
                } else {
                    dzExt1 -= 2.0;
                    zsvExt1 += 2;
                }
            }
            double dx1 = dx0 - 1.0 - 0.3333333333333333;
            double dy1 = dy0 - 0.0 - 0.3333333333333333;
            double dz1 = dz0 - 0.0 - 0.3333333333333333;
            double attn1 = 2.0 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1;
            if (attn1 > 0.0) {
                attn1 *= attn1;
                value += attn1 * attn1 * this.extrapolate(xsb + 1, ysb + 0, zsb + 0, dx1, dy1, dz1);
            }
            if ((attn2 = 2.0 - (dx2 = dx0 - 0.0 - 0.3333333333333333) * dx2 - (dy2 = dy0 - 1.0 - 0.3333333333333333) * dy2 - (dz2 = dz1) * dz2) > 0.0) {
                attn2 *= attn2;
                value += attn2 * attn2 * this.extrapolate(xsb + 0, ysb + 1, zsb + 0, dx2, dy2, dz2);
            }
            if ((attn3 = 2.0 - (dx3 = dx2) * dx3 - (dy3 = dy1) * dy3 - (dz3 = dz0 - 1.0 - 0.3333333333333333) * dz3) > 0.0) {
                attn3 *= attn3;
                value += attn3 * attn3 * this.extrapolate(xsb + 0, ysb + 0, zsb + 1, dx3, dy3, dz3);
            }
            if ((attn4 = 2.0 - (dx4 = dx0 - 1.0 - 0.6666666666666666) * dx4 - (dy4 = dy0 - 1.0 - 0.6666666666666666) * dy4 - (dz4 = dz0 - 0.0 - 0.6666666666666666) * dz4) > 0.0) {
                attn4 *= attn4;
                value += attn4 * attn4 * this.extrapolate(xsb + 1, ysb + 1, zsb + 0, dx4, dy4, dz4);
            }
            if ((attn5 = 2.0 - (dx5 = dx4) * dx5 - (dy5 = dy0 - 0.0 - 0.6666666666666666) * dy5 - (dz5 = dz0 - 1.0 - 0.6666666666666666) * dz5) > 0.0) {
                attn5 *= attn5;
                value += attn5 * attn5 * this.extrapolate(xsb + 1, ysb + 0, zsb + 1, dx5, dy5, dz5);
            }
            if ((attn6 = 2.0 - (dx6 = dx0 - 0.0 - 0.6666666666666666) * dx6 - (dy6 = dy4) * dy6 - (dz6 = dz5) * dz6) > 0.0) {
                attn6 *= attn6;
                value += attn6 * attn6 * this.extrapolate(xsb + 0, ysb + 1, zsb + 1, dx6, dy6, dz6);
            }
        }
        double attnExt0 = 2.0 - dxExt0 * dxExt0 - dyExt0 * dyExt0 - dzExt0 * dzExt0;
        if (attnExt0 > 0.0) {
            attnExt0 *= attnExt0;
            value += attnExt0 * attnExt0 * this.extrapolate(xsvExt0, ysvExt0, zsvExt0, dxExt0, dyExt0, dzExt0);
        }
        if ((attnExt1 = 2.0 - dxExt1 * dxExt1 - dyExt1 * dyExt1 - dzExt1 * dzExt1) > 0.0) {
            attnExt1 *= attnExt1;
            value += attnExt1 * attnExt1 * this.extrapolate(xsvExt1, ysvExt1, zsvExt1, dxExt1, dyExt1, dzExt1);
        }
        return value / 103.0;
    }

    public double eval(double x, double y, double z, double w) {
        double attnExt2;
        double attnExt1;
        double dwExt2;
        double dwExt0;
        double dwExt1;
        int wsvExt2;
        int wsvExt0;
        int wsvExt1;
        double dzExt0;
        double dzExt1;
        double dzExt2;
        int zsvExt0;
        int zsvExt1;
        int zsvExt2;
        double dyExt0;
        double dyExt1;
        double dyExt2;
        int ysvExt0;
        int ysvExt1;
        int ysvExt2;
        double dxExt1;
        double dxExt2;
        double dxExt0;
        int xsvExt1;
        int xsvExt2;
        int xsvExt0;
        double stretchOffset = (x + y + z + w) * -0.138196601125011;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;
        double zs = z + stretchOffset;
        double ws = w + stretchOffset;
        int xsb = OpenSimplexNoise.fastFloor(xs);
        int ysb = OpenSimplexNoise.fastFloor(ys);
        int zsb = OpenSimplexNoise.fastFloor(zs);
        int wsb = OpenSimplexNoise.fastFloor(ws);
        double squishOffset = (double)(xsb + ysb + zsb + wsb) * 0.309016994374947;
        double xb = (double)xsb + squishOffset;
        double yb = (double)ysb + squishOffset;
        double zb = (double)zsb + squishOffset;
        double wb = (double)wsb + squishOffset;
        double xins = xs - (double)xsb;
        double yins = ys - (double)ysb;
        double zins = zs - (double)zsb;
        double wins = ws - (double)wsb;
        double inSum = xins + yins + zins + wins;
        double dx0 = x - xb;
        double dy0 = y - yb;
        double dz0 = z - zb;
        double dw0 = w - wb;
        double value = 0.0;
        if (inSum <= 1.0) {
            double dw4;
            double dz4;
            double dy4;
            double dx4;
            double attn4;
            double dw3;
            double dz3;
            double dy3;
            double dx3;
            double attn3;
            double dw2;
            double dz2;
            double dy2;
            double dx2;
            double attn2;
            double dw1;
            double dz1;
            double dy1;
            double dx1;
            double attn1;
            int aPoint = 1;
            double aScore = xins;
            int bPoint = 2;
            bScore = yins;
            if (aScore >= bScore && zins > bScore) {
                bScore = zins;
                bPoint = 4;
            } else if (aScore < bScore && zins > aScore) {
                aScore = zins;
                aPoint = 4;
            }
            if (aScore >= bScore && wins > bScore) {
                bScore = wins;
                bPoint = 8;
            } else if (aScore < bScore && wins > aScore) {
                aScore = wins;
                aPoint = 8;
            }
            double uins = 1.0 - inSum;
            if (uins > aScore || uins > bScore) {
                int n = c = bScore > aScore ? bPoint : aPoint;
                if ((c & 1) == 0) {
                    xsvExt0 = xsb - 1;
                    xsvExt1 = xsvExt2 = xsb;
                    dxExt0 = dx0 + 1.0;
                    dxExt1 = dxExt2 = dx0;
                } else {
                    xsvExt1 = xsvExt2 = xsb + 1;
                    xsvExt0 = xsvExt2;
                    dxExt1 = dxExt2 = dx0 - 1.0;
                    dxExt0 = dxExt2;
                }
                if ((c & 2) == 0) {
                    ysvExt1 = ysvExt2 = ysb;
                    ysvExt0 = ysvExt2;
                    dyExt1 = dyExt2 = dy0;
                    dyExt0 = dyExt2;
                    if ((c & 1) == 1) {
                        --ysvExt0;
                        dyExt0 += 1.0;
                    } else {
                        --ysvExt1;
                        dyExt1 += 1.0;
                    }
                } else {
                    ysvExt1 = ysvExt2 = ysb + 1;
                    ysvExt0 = ysvExt2;
                    dyExt1 = dyExt2 = dy0 - 1.0;
                    dyExt0 = dyExt2;
                }
                if ((c & 4) == 0) {
                    zsvExt1 = zsvExt2 = zsb;
                    zsvExt0 = zsvExt2--;
                    dzExt1 = dzExt2 = dz0;
                    dzExt0 = dzExt2;
                    if ((c & 3) != 0) {
                        if ((c & 3) == 3) {
                            --zsvExt0;
                            dzExt0 += 1.0;
                        } else {
                            --zsvExt1;
                            dzExt1 += 1.0;
                        }
                    } else {
                        dzExt2 += 1.0;
                    }
                } else {
                    zsvExt1 = zsvExt2 = zsb + 1;
                    zsvExt0 = zsvExt2;
                    dzExt1 = dzExt2 = dz0 - 1.0;
                    dzExt0 = dzExt2;
                }
                if ((c & 8) == 0) {
                    wsvExt0 = wsvExt1 = wsb;
                    wsvExt2 = wsb - 1;
                    dwExt0 = dwExt1 = dw0;
                    dwExt2 = dw0 + 1.0;
                } else {
                    wsvExt1 = wsvExt2 = wsb + 1;
                    wsvExt0 = wsvExt2;
                    dwExt1 = dwExt2 = dw0 - 1.0;
                    dwExt0 = dwExt2;
                }
            } else {
                c = (byte)(aPoint | bPoint);
                if ((c & 1) == 0) {
                    xsvExt0 = xsvExt2 = xsb;
                    xsvExt1 = xsb - 1;
                    dxExt0 = dx0 - 0.618033988749894;
                    dxExt1 = dx0 + 1.0 - 0.309016994374947;
                    dxExt2 = dx0 - 0.309016994374947;
                } else {
                    xsvExt1 = xsvExt2 = xsb + 1;
                    xsvExt0 = xsvExt2;
                    dxExt0 = dx0 - 1.0 - 0.618033988749894;
                    dxExt1 = dxExt2 = dx0 - 1.0 - 0.309016994374947;
                }
                if ((c & 2) == 0) {
                    ysvExt1 = ysvExt2 = ysb;
                    ysvExt0 = ysvExt2--;
                    dyExt0 = dy0 - 0.618033988749894;
                    dyExt1 = dyExt2 = dy0 - 0.309016994374947;
                    if ((c & 1) == 1) {
                        --ysvExt1;
                        dyExt1 += 1.0;
                    } else {
                        dyExt2 += 1.0;
                    }
                } else {
                    ysvExt1 = ysvExt2 = ysb + 1;
                    ysvExt0 = ysvExt2;
                    dyExt0 = dy0 - 1.0 - 0.618033988749894;
                    dyExt1 = dyExt2 = dy0 - 1.0 - 0.309016994374947;
                }
                if ((c & 4) == 0) {
                    zsvExt1 = zsvExt2 = zsb;
                    zsvExt0 = zsvExt2--;
                    dzExt0 = dz0 - 0.618033988749894;
                    dzExt1 = dzExt2 = dz0 - 0.309016994374947;
                    if ((c & 3) == 3) {
                        --zsvExt1;
                        dzExt1 += 1.0;
                    } else {
                        dzExt2 += 1.0;
                    }
                } else {
                    zsvExt1 = zsvExt2 = zsb + 1;
                    zsvExt0 = zsvExt2;
                    dzExt0 = dz0 - 1.0 - 0.618033988749894;
                    dzExt1 = dzExt2 = dz0 - 1.0 - 0.309016994374947;
                }
                if ((c & 8) == 0) {
                    wsvExt0 = wsvExt1 = wsb;
                    wsvExt2 = wsb - 1;
                    dwExt0 = dw0 - 0.618033988749894;
                    dwExt1 = dw0 - 0.309016994374947;
                    dwExt2 = dw0 + 1.0 - 0.309016994374947;
                } else {
                    wsvExt1 = wsvExt2 = wsb + 1;
                    wsvExt0 = wsvExt2;
                    dwExt0 = dw0 - 1.0 - 0.618033988749894;
                    dwExt1 = dwExt2 = dw0 - 1.0 - 0.309016994374947;
                }
            }
            double attn0 = 2.0 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0 - dw0 * dw0;
            if (attn0 > 0.0) {
                attn0 *= attn0;
                value += attn0 * attn0 * this.extrapolate(xsb + 0, ysb + 0, zsb + 0, wsb + 0, dx0, dy0, dz0, dw0);
            }
            if ((attn1 = 2.0 - (dx1 = dx0 - 1.0 - 0.309016994374947) * dx1 - (dy1 = dy0 - 0.0 - 0.309016994374947) * dy1 - (dz1 = dz0 - 0.0 - 0.309016994374947) * dz1 - (dw1 = dw0 - 0.0 - 0.309016994374947) * dw1) > 0.0) {
                attn1 *= attn1;
                value += attn1 * attn1 * this.extrapolate(xsb + 1, ysb + 0, zsb + 0, wsb + 0, dx1, dy1, dz1, dw1);
            }
            if ((attn2 = 2.0 - (dx2 = dx0 - 0.0 - 0.309016994374947) * dx2 - (dy2 = dy0 - 1.0 - 0.309016994374947) * dy2 - (dz2 = dz1) * dz2 - (dw2 = dw1) * dw2) > 0.0) {
                attn2 *= attn2;
                value += attn2 * attn2 * this.extrapolate(xsb + 0, ysb + 1, zsb + 0, wsb + 0, dx2, dy2, dz2, dw2);
            }
            if ((attn3 = 2.0 - (dx3 = dx2) * dx3 - (dy3 = dy1) * dy3 - (dz3 = dz0 - 1.0 - 0.309016994374947) * dz3 - (dw3 = dw1) * dw3) > 0.0) {
                attn3 *= attn3;
                value += attn3 * attn3 * this.extrapolate(xsb + 0, ysb + 0, zsb + 1, wsb + 0, dx3, dy3, dz3, dw3);
            }
            if ((attn4 = 2.0 - (dx4 = dx2) * dx4 - (dy4 = dy1) * dy4 - (dz4 = dz1) * dz4 - (dw4 = dw0 - 1.0 - 0.309016994374947) * dw4) > 0.0) {
                attn4 *= attn4;
                value += attn4 * attn4 * this.extrapolate(xsb + 0, ysb + 0, zsb + 0, wsb + 1, dx4, dy4, dz4, dw4);
            }
        } else if (inSum >= 3.0) {
            double attn0;
            double dw1;
            double dz1;
            double dy1;
            double dx1;
            double attn1;
            double dw2;
            double dz2;
            double dy2;
            double dx2;
            double attn2;
            double dw3;
            double dz3;
            double dy3;
            double dx3;
            double attn3;
            int aPoint = 14;
            double aScore = xins;
            int bPoint = 13;
            bScore = yins;
            if (aScore <= bScore && zins < bScore) {
                bScore = zins;
                bPoint = 11;
            } else if (aScore > bScore && zins < aScore) {
                aScore = zins;
                aPoint = 11;
            }
            if (aScore <= bScore && wins < bScore) {
                bScore = wins;
                bPoint = 7;
            } else if (aScore > bScore && wins < aScore) {
                aScore = wins;
                aPoint = 7;
            }
            double uins = 4.0 - inSum;
            if (uins < aScore || uins < bScore) {
                int n = c = bScore < aScore ? bPoint : aPoint;
                if ((c & 1) != 0) {
                    xsvExt0 = xsb + 2;
                    xsvExt1 = xsvExt2 = xsb + 1;
                    dxExt0 = dx0 - 2.0 - 1.236067977499788;
                    dxExt1 = dxExt2 = dx0 - 1.0 - 1.236067977499788;
                } else {
                    xsvExt1 = xsvExt2 = xsb;
                    xsvExt0 = xsvExt2;
                    dxExt1 = dxExt2 = dx0 - 1.236067977499788;
                    dxExt0 = dxExt2;
                }
                if ((c & 2) != 0) {
                    ysvExt1 = ysvExt2 = ysb + 1;
                    ysvExt0 = ysvExt2;
                    dyExt1 = dyExt2 = dy0 - 1.0 - 1.236067977499788;
                    dyExt0 = dyExt2;
                    if ((c & 1) != 0) {
                        ++ysvExt1;
                        dyExt1 -= 1.0;
                    } else {
                        ++ysvExt0;
                        dyExt0 -= 1.0;
                    }
                } else {
                    ysvExt1 = ysvExt2 = ysb;
                    ysvExt0 = ysvExt2;
                    dyExt1 = dyExt2 = dy0 - 1.236067977499788;
                    dyExt0 = dyExt2;
                }
                if ((c & 4) != 0) {
                    zsvExt1 = zsvExt2 = zsb + 1;
                    zsvExt0 = zsvExt2++;
                    dzExt1 = dzExt2 = dz0 - 1.0 - 1.236067977499788;
                    dzExt0 = dzExt2;
                    if ((c & 3) != 3) {
                        if ((c & 3) == 0) {
                            ++zsvExt0;
                            dzExt0 -= 1.0;
                        } else {
                            ++zsvExt1;
                            dzExt1 -= 1.0;
                        }
                    } else {
                        dzExt2 -= 1.0;
                    }
                } else {
                    zsvExt1 = zsvExt2 = zsb;
                    zsvExt0 = zsvExt2;
                    dzExt1 = dzExt2 = dz0 - 1.236067977499788;
                    dzExt0 = dzExt2;
                }
                if ((c & 8) != 0) {
                    wsvExt0 = wsvExt1 = wsb + 1;
                    wsvExt2 = wsb + 2;
                    dwExt0 = dwExt1 = dw0 - 1.0 - 1.236067977499788;
                    dwExt2 = dw0 - 2.0 - 1.236067977499788;
                } else {
                    wsvExt1 = wsvExt2 = wsb;
                    wsvExt0 = wsvExt2;
                    dwExt1 = dwExt2 = dw0 - 1.236067977499788;
                    dwExt0 = dwExt2;
                }
            } else {
                c = (byte)(aPoint & bPoint);
                if ((c & 1) != 0) {
                    xsvExt0 = xsvExt2 = xsb + 1;
                    xsvExt1 = xsb + 2;
                    dxExt0 = dx0 - 1.0 - 0.618033988749894;
                    dxExt1 = dx0 - 2.0 - 0.927050983124841;
                    dxExt2 = dx0 - 1.0 - 0.927050983124841;
                } else {
                    xsvExt1 = xsvExt2 = xsb;
                    xsvExt0 = xsvExt2;
                    dxExt0 = dx0 - 0.618033988749894;
                    dxExt1 = dxExt2 = dx0 - 0.927050983124841;
                }
                if ((c & 2) != 0) {
                    ysvExt1 = ysvExt2 = ysb + 1;
                    ysvExt0 = ysvExt2++;
                    dyExt0 = dy0 - 1.0 - 0.618033988749894;
                    dyExt1 = dyExt2 = dy0 - 1.0 - 0.927050983124841;
                    if ((c & 1) != 0) {
                        dyExt2 -= 1.0;
                    } else {
                        ++ysvExt1;
                        dyExt1 -= 1.0;
                    }
                } else {
                    ysvExt1 = ysvExt2 = ysb;
                    ysvExt0 = ysvExt2;
                    dyExt0 = dy0 - 0.618033988749894;
                    dyExt1 = dyExt2 = dy0 - 0.927050983124841;
                }
                if ((c & 4) != 0) {
                    zsvExt1 = zsvExt2 = zsb + 1;
                    zsvExt0 = zsvExt2++;
                    dzExt0 = dz0 - 1.0 - 0.618033988749894;
                    dzExt1 = dzExt2 = dz0 - 1.0 - 0.927050983124841;
                    if ((c & 3) != 0) {
                        dzExt2 -= 1.0;
                    } else {
                        ++zsvExt1;
                        dzExt1 -= 1.0;
                    }
                } else {
                    zsvExt1 = zsvExt2 = zsb;
                    zsvExt0 = zsvExt2;
                    dzExt0 = dz0 - 0.618033988749894;
                    dzExt1 = dzExt2 = dz0 - 0.927050983124841;
                }
                if ((c & 8) != 0) {
                    wsvExt0 = wsvExt1 = wsb + 1;
                    wsvExt2 = wsb + 2;
                    dwExt0 = dw0 - 1.0 - 0.618033988749894;
                    dwExt1 = dw0 - 1.0 - 0.927050983124841;
                    dwExt2 = dw0 - 2.0 - 0.927050983124841;
                } else {
                    wsvExt1 = wsvExt2 = wsb;
                    wsvExt0 = wsvExt2;
                    dwExt0 = dw0 - 0.618033988749894;
                    dwExt1 = dwExt2 = dw0 - 0.927050983124841;
                }
            }
            double dx4 = dx0 - 1.0 - 0.927050983124841;
            double dy4 = dy0 - 1.0 - 0.927050983124841;
            double dz4 = dz0 - 1.0 - 0.927050983124841;
            double dw4 = dw0 - 0.927050983124841;
            double attn4 = 2.0 - dx4 * dx4 - dy4 * dy4 - dz4 * dz4 - dw4 * dw4;
            if (attn4 > 0.0) {
                attn4 *= attn4;
                value += attn4 * attn4 * this.extrapolate(xsb + 1, ysb + 1, zsb + 1, wsb + 0, dx4, dy4, dz4, dw4);
            }
            if ((attn3 = 2.0 - (dx3 = dx4) * dx3 - (dy3 = dy4) * dy3 - (dz3 = dz0 - 0.927050983124841) * dz3 - (dw3 = dw0 - 1.0 - 0.927050983124841) * dw3) > 0.0) {
                attn3 *= attn3;
                value += attn3 * attn3 * this.extrapolate(xsb + 1, ysb + 1, zsb + 0, wsb + 1, dx3, dy3, dz3, dw3);
            }
            if ((attn2 = 2.0 - (dx2 = dx4) * dx2 - (dy2 = dy0 - 0.927050983124841) * dy2 - (dz2 = dz4) * dz2 - (dw2 = dw3) * dw2) > 0.0) {
                attn2 *= attn2;
                value += attn2 * attn2 * this.extrapolate(xsb + 1, ysb + 0, zsb + 1, wsb + 1, dx2, dy2, dz2, dw2);
            }
            if ((attn1 = 2.0 - (dx1 = dx0 - 0.927050983124841) * dx1 - (dy1 = dy4) * dy1 - (dz1 = dz4) * dz1 - (dw1 = dw3) * dw1) > 0.0) {
                attn1 *= attn1;
                value += attn1 * attn1 * this.extrapolate(xsb + 0, ysb + 1, zsb + 1, wsb + 1, dx1, dy1, dz1, dw1);
            }
            if ((attn0 = 2.0 - (dx0 = dx0 - 1.0 - 1.236067977499788) * dx0 - (dy0 = dy0 - 1.0 - 1.236067977499788) * dy0 - (dz0 = dz0 - 1.0 - 1.236067977499788) * dz0 - (dw0 = dw0 - 1.0 - 1.236067977499788) * dw0) > 0.0) {
                attn0 *= attn0;
                value += attn0 * attn0 * this.extrapolate(xsb + 1, ysb + 1, zsb + 1, wsb + 1, dx0, dy0, dz0, dw0);
            }
        } else if (inSum <= 2.0) {
            double dw10;
            double dz10;
            double dy10;
            double dx10;
            double attn10;
            double dw9;
            double dz9;
            double dy9;
            double dx9;
            double attn9;
            double dw8;
            double dz8;
            double dy8;
            double dx8;
            double attn8;
            double dw7;
            double dz7;
            double dy7;
            double dx7;
            double attn7;
            double dw6;
            double dz6;
            double dy6;
            double dx6;
            double attn6;
            double dw5;
            double dz5;
            double dy5;
            double dx5;
            double attn5;
            double dw4;
            double dz4;
            double dy4;
            double dx4;
            double attn4;
            double dw3;
            double dz3;
            double dy3;
            double dx3;
            double attn3;
            double dw2;
            double dz2;
            double dy2;
            double dx2;
            double attn2;
            int bPoint;
            double aScore;
            boolean aIsBiggerSide = true;
            boolean bIsBiggerSide = true;
            if (xins + yins > zins + wins) {
                aScore = xins + yins;
                aPoint = 3;
            } else {
                aScore = zins + wins;
                aPoint = 12;
            }
            if (xins + zins > yins + wins) {
                bScore = xins + zins;
                bPoint = 5;
            } else {
                bScore = yins + wins;
                bPoint = 10;
            }
            if (xins + wins > yins + zins) {
                score = xins + wins;
                if (aScore >= bScore && score > bScore) {
                    bScore = score;
                    bPoint = 9;
                } else if (aScore < bScore && score > aScore) {
                    aScore = score;
                    aPoint = 9;
                }
            } else {
                score = yins + zins;
                if (aScore >= bScore && score > bScore) {
                    bScore = score;
                    bPoint = 6;
                } else if (aScore < bScore && score > aScore) {
                    aScore = score;
                    aPoint = 6;
                }
            }
            double p1 = 2.0 - inSum + xins;
            if (aScore >= bScore && p1 > bScore) {
                bScore = p1;
                bPoint = 1;
                bIsBiggerSide = false;
            } else if (aScore < bScore && p1 > aScore) {
                aScore = p1;
                aPoint = 1;
                aIsBiggerSide = false;
            }
            double p2 = 2.0 - inSum + yins;
            if (aScore >= bScore && p2 > bScore) {
                bScore = p2;
                bPoint = 2;
                bIsBiggerSide = false;
            } else if (aScore < bScore && p2 > aScore) {
                aScore = p2;
                aPoint = 2;
                aIsBiggerSide = false;
            }
            double p3 = 2.0 - inSum + zins;
            if (aScore >= bScore && p3 > bScore) {
                bScore = p3;
                bPoint = 4;
                bIsBiggerSide = false;
            } else if (aScore < bScore && p3 > aScore) {
                aScore = p3;
                aPoint = 4;
                aIsBiggerSide = false;
            }
            double p4 = 2.0 - inSum + wins;
            if (aScore >= bScore && p4 > bScore) {
                bPoint = 8;
                bIsBiggerSide = false;
            } else if (aScore < bScore && p4 > aScore) {
                aPoint = 8;
                aIsBiggerSide = false;
            }
            if (aIsBiggerSide == bIsBiggerSide) {
                if (aIsBiggerSide) {
                    c1 = (byte)(aPoint | bPoint);
                    byte c2 = (byte)(aPoint & bPoint);
                    if ((c1 & 1) == 0) {
                        xsvExt0 = xsb;
                        xsvExt1 = xsb - 1;
                        dxExt0 = dx0 - 0.927050983124841;
                        dxExt1 = dx0 + 1.0 - 0.618033988749894;
                    } else {
                        xsvExt0 = xsvExt1 = xsb + 1;
                        dxExt0 = dx0 - 1.0 - 0.927050983124841;
                        dxExt1 = dx0 - 1.0 - 0.618033988749894;
                    }
                    if ((c1 & 2) == 0) {
                        ysvExt0 = ysb;
                        ysvExt1 = ysb - 1;
                        dyExt0 = dy0 - 0.927050983124841;
                        dyExt1 = dy0 + 1.0 - 0.618033988749894;
                    } else {
                        ysvExt0 = ysvExt1 = ysb + 1;
                        dyExt0 = dy0 - 1.0 - 0.927050983124841;
                        dyExt1 = dy0 - 1.0 - 0.618033988749894;
                    }
                    if ((c1 & 4) == 0) {
                        zsvExt0 = zsb;
                        zsvExt1 = zsb - 1;
                        dzExt0 = dz0 - 0.927050983124841;
                        dzExt1 = dz0 + 1.0 - 0.618033988749894;
                    } else {
                        zsvExt0 = zsvExt1 = zsb + 1;
                        dzExt0 = dz0 - 1.0 - 0.927050983124841;
                        dzExt1 = dz0 - 1.0 - 0.618033988749894;
                    }
                    if ((c1 & 8) == 0) {
                        wsvExt0 = wsb;
                        wsvExt1 = wsb - 1;
                        dwExt0 = dw0 - 0.927050983124841;
                        dwExt1 = dw0 + 1.0 - 0.618033988749894;
                    } else {
                        wsvExt0 = wsvExt1 = wsb + 1;
                        dwExt0 = dw0 - 1.0 - 0.927050983124841;
                        dwExt1 = dw0 - 1.0 - 0.618033988749894;
                    }
                    xsvExt2 = xsb;
                    ysvExt2 = ysb;
                    zsvExt2 = zsb;
                    wsvExt2 = wsb;
                    dxExt2 = dx0 - 0.618033988749894;
                    dyExt2 = dy0 - 0.618033988749894;
                    dzExt2 = dz0 - 0.618033988749894;
                    dwExt2 = dw0 - 0.618033988749894;
                    if ((c2 & 1) != 0) {
                        xsvExt2 += 2;
                        dxExt2 -= 2.0;
                    } else if ((c2 & 2) != 0) {
                        ysvExt2 += 2;
                        dyExt2 -= 2.0;
                    } else if ((c2 & 4) != 0) {
                        zsvExt2 += 2;
                        dzExt2 -= 2.0;
                    } else {
                        wsvExt2 += 2;
                        dwExt2 -= 2.0;
                    }
                } else {
                    xsvExt2 = xsb;
                    ysvExt2 = ysb;
                    zsvExt2 = zsb;
                    wsvExt2 = wsb;
                    dxExt2 = dx0;
                    dyExt2 = dy0;
                    dzExt2 = dz0;
                    dwExt2 = dw0;
                    byte c = (byte)(aPoint | bPoint);
                    if ((c & 1) == 0) {
                        xsvExt0 = xsb - 1;
                        xsvExt1 = xsb;
                        dxExt0 = dx0 + 1.0 - 0.309016994374947;
                        dxExt1 = dx0 - 0.309016994374947;
                    } else {
                        xsvExt0 = xsvExt1 = xsb + 1;
                        dxExt0 = dxExt1 = dx0 - 1.0 - 0.309016994374947;
                    }
                    if ((c & 2) == 0) {
                        ysvExt1 = ysb;
                        ysvExt0 = ysvExt1--;
                        dyExt0 = dyExt1 = dy0 - 0.309016994374947;
                        if ((c & 1) == 1) {
                            --ysvExt0;
                            dyExt0 += 1.0;
                        } else {
                            dyExt1 += 1.0;
                        }
                    } else {
                        ysvExt0 = ysvExt1 = ysb + 1;
                        dyExt0 = dyExt1 = dy0 - 1.0 - 0.309016994374947;
                    }
                    if ((c & 4) == 0) {
                        zsvExt1 = zsb;
                        zsvExt0 = zsvExt1--;
                        dzExt0 = dzExt1 = dz0 - 0.309016994374947;
                        if ((c & 3) == 3) {
                            --zsvExt0;
                            dzExt0 += 1.0;
                        } else {
                            dzExt1 += 1.0;
                        }
                    } else {
                        zsvExt0 = zsvExt1 = zsb + 1;
                        dzExt0 = dzExt1 = dz0 - 1.0 - 0.309016994374947;
                    }
                    if ((c & 8) == 0) {
                        wsvExt0 = wsb;
                        wsvExt1 = wsb - 1;
                        dwExt0 = dw0 - 0.309016994374947;
                        dwExt1 = dw0 + 1.0 - 0.309016994374947;
                    } else {
                        wsvExt0 = wsvExt1 = wsb + 1;
                        dwExt0 = dwExt1 = dw0 - 1.0 - 0.309016994374947;
                    }
                }
            } else {
                int c2;
                if (aIsBiggerSide) {
                    c1 = aPoint;
                    c2 = bPoint;
                } else {
                    c1 = bPoint;
                    c2 = aPoint;
                }
                if ((c1 & 1) == 0) {
                    xsvExt0 = xsb - 1;
                    xsvExt1 = xsb;
                    dxExt0 = dx0 + 1.0 - 0.309016994374947;
                    dxExt1 = dx0 - 0.309016994374947;
                } else {
                    xsvExt0 = xsvExt1 = xsb + 1;
                    dxExt0 = dxExt1 = dx0 - 1.0 - 0.309016994374947;
                }
                if ((c1 & 2) == 0) {
                    ysvExt1 = ysb;
                    ysvExt0 = ysvExt1--;
                    dyExt0 = dyExt1 = dy0 - 0.309016994374947;
                    if ((c1 & 1) == 1) {
                        --ysvExt0;
                        dyExt0 += 1.0;
                    } else {
                        dyExt1 += 1.0;
                    }
                } else {
                    ysvExt0 = ysvExt1 = ysb + 1;
                    dyExt0 = dyExt1 = dy0 - 1.0 - 0.309016994374947;
                }
                if ((c1 & 4) == 0) {
                    zsvExt1 = zsb;
                    zsvExt0 = zsvExt1--;
                    dzExt0 = dzExt1 = dz0 - 0.309016994374947;
                    if ((c1 & 3) == 3) {
                        --zsvExt0;
                        dzExt0 += 1.0;
                    } else {
                        dzExt1 += 1.0;
                    }
                } else {
                    zsvExt0 = zsvExt1 = zsb + 1;
                    dzExt0 = dzExt1 = dz0 - 1.0 - 0.309016994374947;
                }
                if ((c1 & 8) == 0) {
                    wsvExt0 = wsb;
                    wsvExt1 = wsb - 1;
                    dwExt0 = dw0 - 0.309016994374947;
                    dwExt1 = dw0 + 1.0 - 0.309016994374947;
                } else {
                    wsvExt0 = wsvExt1 = wsb + 1;
                    dwExt0 = dwExt1 = dw0 - 1.0 - 0.309016994374947;
                }
                xsvExt2 = xsb;
                ysvExt2 = ysb;
                zsvExt2 = zsb;
                wsvExt2 = wsb;
                dxExt2 = dx0 - 0.618033988749894;
                dyExt2 = dy0 - 0.618033988749894;
                dzExt2 = dz0 - 0.618033988749894;
                dwExt2 = dw0 - 0.618033988749894;
                if ((c2 & 1) != 0) {
                    xsvExt2 += 2;
                    dxExt2 -= 2.0;
                } else if ((c2 & 2) != 0) {
                    ysvExt2 += 2;
                    dyExt2 -= 2.0;
                } else if ((c2 & 4) != 0) {
                    zsvExt2 += 2;
                    dzExt2 -= 2.0;
                } else {
                    wsvExt2 += 2;
                    dwExt2 -= 2.0;
                }
            }
            double dx1 = dx0 - 1.0 - 0.309016994374947;
            double dy1 = dy0 - 0.0 - 0.309016994374947;
            double dz1 = dz0 - 0.0 - 0.309016994374947;
            double dw1 = dw0 - 0.0 - 0.309016994374947;
            double attn1 = 2.0 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1 - dw1 * dw1;
            if (attn1 > 0.0) {
                attn1 *= attn1;
                value += attn1 * attn1 * this.extrapolate(xsb + 1, ysb + 0, zsb + 0, wsb + 0, dx1, dy1, dz1, dw1);
            }
            if ((attn2 = 2.0 - (dx2 = dx0 - 0.0 - 0.309016994374947) * dx2 - (dy2 = dy0 - 1.0 - 0.309016994374947) * dy2 - (dz2 = dz1) * dz2 - (dw2 = dw1) * dw2) > 0.0) {
                attn2 *= attn2;
                value += attn2 * attn2 * this.extrapolate(xsb + 0, ysb + 1, zsb + 0, wsb + 0, dx2, dy2, dz2, dw2);
            }
            if ((attn3 = 2.0 - (dx3 = dx2) * dx3 - (dy3 = dy1) * dy3 - (dz3 = dz0 - 1.0 - 0.309016994374947) * dz3 - (dw3 = dw1) * dw3) > 0.0) {
                attn3 *= attn3;
                value += attn3 * attn3 * this.extrapolate(xsb + 0, ysb + 0, zsb + 1, wsb + 0, dx3, dy3, dz3, dw3);
            }
            if ((attn4 = 2.0 - (dx4 = dx2) * dx4 - (dy4 = dy1) * dy4 - (dz4 = dz1) * dz4 - (dw4 = dw0 - 1.0 - 0.309016994374947) * dw4) > 0.0) {
                attn4 *= attn4;
                value += attn4 * attn4 * this.extrapolate(xsb + 0, ysb + 0, zsb + 0, wsb + 1, dx4, dy4, dz4, dw4);
            }
            if ((attn5 = 2.0 - (dx5 = dx0 - 1.0 - 0.618033988749894) * dx5 - (dy5 = dy0 - 1.0 - 0.618033988749894) * dy5 - (dz5 = dz0 - 0.0 - 0.618033988749894) * dz5 - (dw5 = dw0 - 0.0 - 0.618033988749894) * dw5) > 0.0) {
                attn5 *= attn5;
                value += attn5 * attn5 * this.extrapolate(xsb + 1, ysb + 1, zsb + 0, wsb + 0, dx5, dy5, dz5, dw5);
            }
            if ((attn6 = 2.0 - (dx6 = dx0 - 1.0 - 0.618033988749894) * dx6 - (dy6 = dy0 - 0.0 - 0.618033988749894) * dy6 - (dz6 = dz0 - 1.0 - 0.618033988749894) * dz6 - (dw6 = dw0 - 0.0 - 0.618033988749894) * dw6) > 0.0) {
                attn6 *= attn6;
                value += attn6 * attn6 * this.extrapolate(xsb + 1, ysb + 0, zsb + 1, wsb + 0, dx6, dy6, dz6, dw6);
            }
            if ((attn7 = 2.0 - (dx7 = dx0 - 1.0 - 0.618033988749894) * dx7 - (dy7 = dy0 - 0.0 - 0.618033988749894) * dy7 - (dz7 = dz0 - 0.0 - 0.618033988749894) * dz7 - (dw7 = dw0 - 1.0 - 0.618033988749894) * dw7) > 0.0) {
                attn7 *= attn7;
                value += attn7 * attn7 * this.extrapolate(xsb + 1, ysb + 0, zsb + 0, wsb + 1, dx7, dy7, dz7, dw7);
            }
            if ((attn8 = 2.0 - (dx8 = dx0 - 0.0 - 0.618033988749894) * dx8 - (dy8 = dy0 - 1.0 - 0.618033988749894) * dy8 - (dz8 = dz0 - 1.0 - 0.618033988749894) * dz8 - (dw8 = dw0 - 0.0 - 0.618033988749894) * dw8) > 0.0) {
                attn8 *= attn8;
                value += attn8 * attn8 * this.extrapolate(xsb + 0, ysb + 1, zsb + 1, wsb + 0, dx8, dy8, dz8, dw8);
            }
            if ((attn9 = 2.0 - (dx9 = dx0 - 0.0 - 0.618033988749894) * dx9 - (dy9 = dy0 - 1.0 - 0.618033988749894) * dy9 - (dz9 = dz0 - 0.0 - 0.618033988749894) * dz9 - (dw9 = dw0 - 1.0 - 0.618033988749894) * dw9) > 0.0) {
                attn9 *= attn9;
                value += attn9 * attn9 * this.extrapolate(xsb + 0, ysb + 1, zsb + 0, wsb + 1, dx9, dy9, dz9, dw9);
            }
            if ((attn10 = 2.0 - (dx10 = dx0 - 0.0 - 0.618033988749894) * dx10 - (dy10 = dy0 - 0.0 - 0.618033988749894) * dy10 - (dz10 = dz0 - 1.0 - 0.618033988749894) * dz10 - (dw10 = dw0 - 1.0 - 0.618033988749894) * dw10) > 0.0) {
                attn10 *= attn10;
                value += attn10 * attn10 * this.extrapolate(xsb + 0, ysb + 0, zsb + 1, wsb + 1, dx10, dy10, dz10, dw10);
            }
        } else {
            double dw10;
            double dz10;
            double dy10;
            double dx10;
            double attn10;
            double dw9;
            double dz9;
            double dy9;
            double dx9;
            double attn9;
            double dw8;
            double dz8;
            double dy8;
            double dx8;
            double attn8;
            double dw7;
            double dz7;
            double dy7;
            double dx7;
            double attn7;
            double dw6;
            double dz6;
            double dy6;
            double dx6;
            double attn6;
            double dw5;
            double dz5;
            double dy5;
            double dx5;
            double attn5;
            double dw1;
            double dz1;
            double dy1;
            double dx1;
            double attn1;
            double dw2;
            double dz2;
            double dy2;
            double dx2;
            double attn2;
            double dw3;
            double dz3;
            double dy3;
            double dx3;
            double attn3;
            int bPoint;
            double aScore;
            boolean aIsBiggerSide = true;
            boolean bIsBiggerSide = true;
            if (xins + yins < zins + wins) {
                aScore = xins + yins;
                aPoint = 12;
            } else {
                aScore = zins + wins;
                aPoint = 3;
            }
            if (xins + zins < yins + wins) {
                bScore = xins + zins;
                bPoint = 10;
            } else {
                bScore = yins + wins;
                bPoint = 5;
            }
            if (xins + wins < yins + zins) {
                score = xins + wins;
                if (aScore <= bScore && score < bScore) {
                    bScore = score;
                    bPoint = 6;
                } else if (aScore > bScore && score < aScore) {
                    aScore = score;
                    aPoint = 6;
                }
            } else {
                score = yins + zins;
                if (aScore <= bScore && score < bScore) {
                    bScore = score;
                    bPoint = 9;
                } else if (aScore > bScore && score < aScore) {
                    aScore = score;
                    aPoint = 9;
                }
            }
            double p1 = 3.0 - inSum + xins;
            if (aScore <= bScore && p1 < bScore) {
                bScore = p1;
                bPoint = 14;
                bIsBiggerSide = false;
            } else if (aScore > bScore && p1 < aScore) {
                aScore = p1;
                aPoint = 14;
                aIsBiggerSide = false;
            }
            double p2 = 3.0 - inSum + yins;
            if (aScore <= bScore && p2 < bScore) {
                bScore = p2;
                bPoint = 13;
                bIsBiggerSide = false;
            } else if (aScore > bScore && p2 < aScore) {
                aScore = p2;
                aPoint = 13;
                aIsBiggerSide = false;
            }
            double p3 = 3.0 - inSum + zins;
            if (aScore <= bScore && p3 < bScore) {
                bScore = p3;
                bPoint = 11;
                bIsBiggerSide = false;
            } else if (aScore > bScore && p3 < aScore) {
                aScore = p3;
                aPoint = 11;
                aIsBiggerSide = false;
            }
            double p4 = 3.0 - inSum + wins;
            if (aScore <= bScore && p4 < bScore) {
                bPoint = 7;
                bIsBiggerSide = false;
            } else if (aScore > bScore && p4 < aScore) {
                aPoint = 7;
                aIsBiggerSide = false;
            }
            if (aIsBiggerSide == bIsBiggerSide) {
                if (aIsBiggerSide) {
                    c1 = (byte)(aPoint & bPoint);
                    byte c2 = (byte)(aPoint | bPoint);
                    xsvExt0 = xsvExt1 = xsb;
                    ysvExt0 = ysvExt1 = ysb;
                    zsvExt0 = zsvExt1 = zsb;
                    wsvExt0 = wsvExt1 = wsb;
                    dxExt0 = dx0 - 0.309016994374947;
                    dyExt0 = dy0 - 0.309016994374947;
                    dzExt0 = dz0 - 0.309016994374947;
                    dwExt0 = dw0 - 0.309016994374947;
                    dxExt1 = dx0 - 0.618033988749894;
                    dyExt1 = dy0 - 0.618033988749894;
                    dzExt1 = dz0 - 0.618033988749894;
                    dwExt1 = dw0 - 0.618033988749894;
                    if ((c1 & 1) != 0) {
                        ++xsvExt0;
                        dxExt0 -= 1.0;
                        xsvExt1 += 2;
                        dxExt1 -= 2.0;
                    } else if ((c1 & 2) != 0) {
                        ++ysvExt0;
                        dyExt0 -= 1.0;
                        ysvExt1 += 2;
                        dyExt1 -= 2.0;
                    } else if ((c1 & 4) != 0) {
                        ++zsvExt0;
                        dzExt0 -= 1.0;
                        zsvExt1 += 2;
                        dzExt1 -= 2.0;
                    } else {
                        ++wsvExt0;
                        dwExt0 -= 1.0;
                        wsvExt1 += 2;
                        dwExt1 -= 2.0;
                    }
                    xsvExt2 = xsb + 1;
                    ysvExt2 = ysb + 1;
                    zsvExt2 = zsb + 1;
                    wsvExt2 = wsb + 1;
                    dxExt2 = dx0 - 1.0 - 0.618033988749894;
                    dyExt2 = dy0 - 1.0 - 0.618033988749894;
                    dzExt2 = dz0 - 1.0 - 0.618033988749894;
                    dwExt2 = dw0 - 1.0 - 0.618033988749894;
                    if ((c2 & 1) == 0) {
                        xsvExt2 -= 2;
                        dxExt2 += 2.0;
                    } else if ((c2 & 2) == 0) {
                        ysvExt2 -= 2;
                        dyExt2 += 2.0;
                    } else if ((c2 & 4) == 0) {
                        zsvExt2 -= 2;
                        dzExt2 += 2.0;
                    } else {
                        wsvExt2 -= 2;
                        dwExt2 += 2.0;
                    }
                } else {
                    xsvExt2 = xsb + 1;
                    ysvExt2 = ysb + 1;
                    zsvExt2 = zsb + 1;
                    wsvExt2 = wsb + 1;
                    dxExt2 = dx0 - 1.0 - 1.236067977499788;
                    dyExt2 = dy0 - 1.0 - 1.236067977499788;
                    dzExt2 = dz0 - 1.0 - 1.236067977499788;
                    dwExt2 = dw0 - 1.0 - 1.236067977499788;
                    byte c = (byte)(aPoint & bPoint);
                    if ((c & 1) != 0) {
                        xsvExt0 = xsb + 2;
                        xsvExt1 = xsb + 1;
                        dxExt0 = dx0 - 2.0 - 0.927050983124841;
                        dxExt1 = dx0 - 1.0 - 0.927050983124841;
                    } else {
                        xsvExt0 = xsvExt1 = xsb;
                        dxExt0 = dxExt1 = dx0 - 0.927050983124841;
                    }
                    if ((c & 2) != 0) {
                        ysvExt1 = ysb + 1;
                        ysvExt0 = ysvExt1++;
                        dyExt0 = dyExt1 = dy0 - 1.0 - 0.927050983124841;
                        if ((c & 1) == 0) {
                            ++ysvExt0;
                            dyExt0 -= 1.0;
                        } else {
                            dyExt1 -= 1.0;
                        }
                    } else {
                        ysvExt0 = ysvExt1 = ysb;
                        dyExt0 = dyExt1 = dy0 - 0.927050983124841;
                    }
                    if ((c & 4) != 0) {
                        zsvExt1 = zsb + 1;
                        zsvExt0 = zsvExt1++;
                        dzExt0 = dzExt1 = dz0 - 1.0 - 0.927050983124841;
                        if ((c & 3) == 0) {
                            ++zsvExt0;
                            dzExt0 -= 1.0;
                        } else {
                            dzExt1 -= 1.0;
                        }
                    } else {
                        zsvExt0 = zsvExt1 = zsb;
                        dzExt0 = dzExt1 = dz0 - 0.927050983124841;
                    }
                    if ((c & 8) != 0) {
                        wsvExt0 = wsb + 1;
                        wsvExt1 = wsb + 2;
                        dwExt0 = dw0 - 1.0 - 0.927050983124841;
                        dwExt1 = dw0 - 2.0 - 0.927050983124841;
                    } else {
                        wsvExt0 = wsvExt1 = wsb;
                        dwExt0 = dwExt1 = dw0 - 0.927050983124841;
                    }
                }
            } else {
                int c2;
                if (aIsBiggerSide) {
                    c1 = aPoint;
                    c2 = bPoint;
                } else {
                    c1 = bPoint;
                    c2 = aPoint;
                }
                if ((c1 & 1) != 0) {
                    xsvExt0 = xsb + 2;
                    xsvExt1 = xsb + 1;
                    dxExt0 = dx0 - 2.0 - 0.927050983124841;
                    dxExt1 = dx0 - 1.0 - 0.927050983124841;
                } else {
                    xsvExt0 = xsvExt1 = xsb;
                    dxExt0 = dxExt1 = dx0 - 0.927050983124841;
                }
                if ((c1 & 2) != 0) {
                    ysvExt1 = ysb + 1;
                    ysvExt0 = ysvExt1++;
                    dyExt0 = dyExt1 = dy0 - 1.0 - 0.927050983124841;
                    if ((c1 & 1) == 0) {
                        ++ysvExt0;
                        dyExt0 -= 1.0;
                    } else {
                        dyExt1 -= 1.0;
                    }
                } else {
                    ysvExt0 = ysvExt1 = ysb;
                    dyExt0 = dyExt1 = dy0 - 0.927050983124841;
                }
                if ((c1 & 4) != 0) {
                    zsvExt1 = zsb + 1;
                    zsvExt0 = zsvExt1++;
                    dzExt0 = dzExt1 = dz0 - 1.0 - 0.927050983124841;
                    if ((c1 & 3) == 0) {
                        ++zsvExt0;
                        dzExt0 -= 1.0;
                    } else {
                        dzExt1 -= 1.0;
                    }
                } else {
                    zsvExt0 = zsvExt1 = zsb;
                    dzExt0 = dzExt1 = dz0 - 0.927050983124841;
                }
                if ((c1 & 8) != 0) {
                    wsvExt0 = wsb + 1;
                    wsvExt1 = wsb + 2;
                    dwExt0 = dw0 - 1.0 - 0.927050983124841;
                    dwExt1 = dw0 - 2.0 - 0.927050983124841;
                } else {
                    wsvExt0 = wsvExt1 = wsb;
                    dwExt0 = dwExt1 = dw0 - 0.927050983124841;
                }
                xsvExt2 = xsb + 1;
                ysvExt2 = ysb + 1;
                zsvExt2 = zsb + 1;
                wsvExt2 = wsb + 1;
                dxExt2 = dx0 - 1.0 - 0.618033988749894;
                dyExt2 = dy0 - 1.0 - 0.618033988749894;
                dzExt2 = dz0 - 1.0 - 0.618033988749894;
                dwExt2 = dw0 - 1.0 - 0.618033988749894;
                if ((c2 & 1) == 0) {
                    xsvExt2 -= 2;
                    dxExt2 += 2.0;
                } else if ((c2 & 2) == 0) {
                    ysvExt2 -= 2;
                    dyExt2 += 2.0;
                } else if ((c2 & 4) == 0) {
                    zsvExt2 -= 2;
                    dzExt2 += 2.0;
                } else {
                    wsvExt2 -= 2;
                    dwExt2 += 2.0;
                }
            }
            double dx4 = dx0 - 1.0 - 0.927050983124841;
            double dy4 = dy0 - 1.0 - 0.927050983124841;
            double dz4 = dz0 - 1.0 - 0.927050983124841;
            double dw4 = dw0 - 0.927050983124841;
            double attn4 = 2.0 - dx4 * dx4 - dy4 * dy4 - dz4 * dz4 - dw4 * dw4;
            if (attn4 > 0.0) {
                attn4 *= attn4;
                value += attn4 * attn4 * this.extrapolate(xsb + 1, ysb + 1, zsb + 1, wsb + 0, dx4, dy4, dz4, dw4);
            }
            if ((attn3 = 2.0 - (dx3 = dx4) * dx3 - (dy3 = dy4) * dy3 - (dz3 = dz0 - 0.927050983124841) * dz3 - (dw3 = dw0 - 1.0 - 0.927050983124841) * dw3) > 0.0) {
                attn3 *= attn3;
                value += attn3 * attn3 * this.extrapolate(xsb + 1, ysb + 1, zsb + 0, wsb + 1, dx3, dy3, dz3, dw3);
            }
            if ((attn2 = 2.0 - (dx2 = dx4) * dx2 - (dy2 = dy0 - 0.927050983124841) * dy2 - (dz2 = dz4) * dz2 - (dw2 = dw3) * dw2) > 0.0) {
                attn2 *= attn2;
                value += attn2 * attn2 * this.extrapolate(xsb + 1, ysb + 0, zsb + 1, wsb + 1, dx2, dy2, dz2, dw2);
            }
            if ((attn1 = 2.0 - (dx1 = dx0 - 0.927050983124841) * dx1 - (dy1 = dy4) * dy1 - (dz1 = dz4) * dz1 - (dw1 = dw3) * dw1) > 0.0) {
                attn1 *= attn1;
                value += attn1 * attn1 * this.extrapolate(xsb + 0, ysb + 1, zsb + 1, wsb + 1, dx1, dy1, dz1, dw1);
            }
            if ((attn5 = 2.0 - (dx5 = dx0 - 1.0 - 0.618033988749894) * dx5 - (dy5 = dy0 - 1.0 - 0.618033988749894) * dy5 - (dz5 = dz0 - 0.0 - 0.618033988749894) * dz5 - (dw5 = dw0 - 0.0 - 0.618033988749894) * dw5) > 0.0) {
                attn5 *= attn5;
                value += attn5 * attn5 * this.extrapolate(xsb + 1, ysb + 1, zsb + 0, wsb + 0, dx5, dy5, dz5, dw5);
            }
            if ((attn6 = 2.0 - (dx6 = dx0 - 1.0 - 0.618033988749894) * dx6 - (dy6 = dy0 - 0.0 - 0.618033988749894) * dy6 - (dz6 = dz0 - 1.0 - 0.618033988749894) * dz6 - (dw6 = dw0 - 0.0 - 0.618033988749894) * dw6) > 0.0) {
                attn6 *= attn6;
                value += attn6 * attn6 * this.extrapolate(xsb + 1, ysb + 0, zsb + 1, wsb + 0, dx6, dy6, dz6, dw6);
            }
            if ((attn7 = 2.0 - (dx7 = dx0 - 1.0 - 0.618033988749894) * dx7 - (dy7 = dy0 - 0.0 - 0.618033988749894) * dy7 - (dz7 = dz0 - 0.0 - 0.618033988749894) * dz7 - (dw7 = dw0 - 1.0 - 0.618033988749894) * dw7) > 0.0) {
                attn7 *= attn7;
                value += attn7 * attn7 * this.extrapolate(xsb + 1, ysb + 0, zsb + 0, wsb + 1, dx7, dy7, dz7, dw7);
            }
            if ((attn8 = 2.0 - (dx8 = dx0 - 0.0 - 0.618033988749894) * dx8 - (dy8 = dy0 - 1.0 - 0.618033988749894) * dy8 - (dz8 = dz0 - 1.0 - 0.618033988749894) * dz8 - (dw8 = dw0 - 0.0 - 0.618033988749894) * dw8) > 0.0) {
                attn8 *= attn8;
                value += attn8 * attn8 * this.extrapolate(xsb + 0, ysb + 1, zsb + 1, wsb + 0, dx8, dy8, dz8, dw8);
            }
            if ((attn9 = 2.0 - (dx9 = dx0 - 0.0 - 0.618033988749894) * dx9 - (dy9 = dy0 - 1.0 - 0.618033988749894) * dy9 - (dz9 = dz0 - 0.0 - 0.618033988749894) * dz9 - (dw9 = dw0 - 1.0 - 0.618033988749894) * dw9) > 0.0) {
                attn9 *= attn9;
                value += attn9 * attn9 * this.extrapolate(xsb + 0, ysb + 1, zsb + 0, wsb + 1, dx9, dy9, dz9, dw9);
            }
            if ((attn10 = 2.0 - (dx10 = dx0 - 0.0 - 0.618033988749894) * dx10 - (dy10 = dy0 - 0.0 - 0.618033988749894) * dy10 - (dz10 = dz0 - 1.0 - 0.618033988749894) * dz10 - (dw10 = dw0 - 1.0 - 0.618033988749894) * dw10) > 0.0) {
                attn10 *= attn10;
                value += attn10 * attn10 * this.extrapolate(xsb + 0, ysb + 0, zsb + 1, wsb + 1, dx10, dy10, dz10, dw10);
            }
        }
        double attnExt0 = 2.0 - dxExt0 * dxExt0 - dyExt0 * dyExt0 - dzExt0 * dzExt0 - dwExt0 * dwExt0;
        if (attnExt0 > 0.0) {
            attnExt0 *= attnExt0;
            value += attnExt0 * attnExt0 * this.extrapolate(xsvExt0, ysvExt0, zsvExt0, wsvExt0, dxExt0, dyExt0, dzExt0, dwExt0);
        }
        if ((attnExt1 = 2.0 - dxExt1 * dxExt1 - dyExt1 * dyExt1 - dzExt1 * dzExt1 - dwExt1 * dwExt1) > 0.0) {
            attnExt1 *= attnExt1;
            value += attnExt1 * attnExt1 * this.extrapolate(xsvExt1, ysvExt1, zsvExt1, wsvExt1, dxExt1, dyExt1, dzExt1, dwExt1);
        }
        if ((attnExt2 = 2.0 - dxExt2 * dxExt2 - dyExt2 * dyExt2 - dzExt2 * dzExt2 - dwExt2 * dwExt2) > 0.0) {
            attnExt2 *= attnExt2;
            value += attnExt2 * attnExt2 * this.extrapolate(xsvExt2, ysvExt2, zsvExt2, wsvExt2, dxExt2, dyExt2, dzExt2, dwExt2);
        }
        return value / 30.0;
    }

    private double extrapolate(int xsb, int ysb, double dx, double dy) {
        int index = this.perm[this.perm[xsb & 0xFF] + ysb & 0xFF] & 0xE;
        return (double)gradients2D[index] * dx + (double)gradients2D[index + 1] * dy;
    }

    private double extrapolate(int xsb, int ysb, int zsb, double dx, double dy, double dz) {
        short index = this.permGradIndex3d[this.perm[this.perm[xsb & 0xFF] + ysb & 0xFF] + zsb & 0xFF];
        return (double)gradients3D[index] * dx + (double)gradients3D[index + 1] * dy + (double)gradients3D[index + 2] * dz;
    }

    private double extrapolate(int xsb, int ysb, int zsb, int wsb, double dx, double dy, double dz, double dw) {
        int index = this.perm[this.perm[this.perm[this.perm[xsb & 0xFF] + ysb & 0xFF] + zsb & 0xFF] + wsb & 0xFF] & 0xFC;
        return (double)gradients4D[index] * dx + (double)gradients4D[index + 1] * dy + (double)gradients4D[index + 2] * dz + (double)gradients4D[index + 3] * dw;
    }

    private static int fastFloor(double x) {
        int xi = (int)x;
        return x < (double)xi ? xi - 1 : xi;
    }

    public double evalOct(float v, float v1, int i) {
        double res = this.eval(v, v1, i);
        for (int x = 2; x <= 64; ++x) {
            res += this.eval(v * (float)x * v, v1 * (float)x * v1, i * x * i);
        }
        return res;
    }
}


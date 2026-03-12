/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.population;

import java.util.List;
import zombie.SandboxOptions;
import zombie.core.Color;
import zombie.core.ImmutableColor;
import zombie.util.LocationRNG;

public final class OutfitRNG {
    private static final ThreadLocal<LocationRNG> RNG = ThreadLocal.withInitial(LocationRNG::new);

    public static void setSeed(long seed) {
        RNG.get().setSeed(seed);
    }

    public static long getSeed() {
        return RNG.get().getSeed();
    }

    public static int Next(int max) {
        return RNG.get().nextInt(max);
    }

    public static int Next(int min, int max) {
        if (max == min) {
            return min;
        }
        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }
        int n = RNG.get().nextInt(max - min);
        return n + min;
    }

    public static float Next(float min, float max) {
        if (max == min) {
            return min;
        }
        if (min > max) {
            float temp = min;
            min = max;
            max = temp;
        }
        return min + RNG.get().nextFloat() * (max - min);
    }

    public static boolean NextBool(int invProbability) {
        return OutfitRNG.Next(invProbability) == 0;
    }

    public static <E> E pickRandom(List<E> collection) {
        if (collection.isEmpty()) {
            return null;
        }
        if (collection.size() == 1) {
            return collection.get(0);
        }
        int randomIndex = OutfitRNG.Next(collection.size());
        return collection.get(randomIndex);
    }

    public static ImmutableColor randomImmutableColor() {
        return OutfitRNG.randomImmutableColor(false);
    }

    public static ImmutableColor randomImmutableColor(boolean noBlack) {
        float colorHue = OutfitRNG.Next(0.0f, 1.0f);
        float colorSaturation = OutfitRNG.Next(0.0f, 0.6f);
        float minimumBrightness = 0.1f;
        if (SandboxOptions.instance.noBlackClothes.getValue() || noBlack) {
            minimumBrightness = 0.2f;
        }
        float colorBrightness = OutfitRNG.Next(minimumBrightness, 0.9f);
        Color newC = Color.HSBtoRGB(colorHue, colorSaturation, colorBrightness);
        return new ImmutableColor(newC);
    }
}


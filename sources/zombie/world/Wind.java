/*
 * Decompiled with CFR 0.152.
 */
package zombie.world;

import zombie.core.Translator;

public class Wind {
    public static float getWindKnots(float windKph) {
        return windKph * 19.0f / 36.0f;
    }

    public static int getWindsockSegments(float windKph) {
        return Math.max(0, Math.min(5, (int)Math.floor(windKph * 19.0f / 108.0f)));
    }

    public static int getBeaufortNumber(float windKph) {
        if (windKph < 4.0f) {
            return 0;
        }
        if (windKph < 9.0f) {
            return 1;
        }
        if (windKph < 16.0f) {
            return 2;
        }
        if (windKph < 23.0f) {
            return 3;
        }
        if (windKph < 31.0f) {
            return 4;
        }
        if (windKph < 40.0f) {
            return 5;
        }
        if (windKph < 50.0f) {
            return 6;
        }
        if (windKph < 60.0f) {
            return 7;
        }
        if (windKph < 72.0f) {
            return 8;
        }
        if (windKph < 84.0f) {
            return 9;
        }
        if (windKph < 97.0f) {
            return 10;
        }
        return 11;
    }

    public static String getName(int beaufortNumber) {
        return String.format(Translator.getText("UI_GameLoad_windName" + beaufortNumber), new Object[0]);
    }

    public static String getDescription(int beaufortNumber) {
        return String.format(Translator.getText("UI_GameLoad_windDescription" + beaufortNumber), new Object[0]);
    }
}


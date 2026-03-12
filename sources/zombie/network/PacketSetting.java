/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import zombie.characters.Capability;
import zombie.network.anticheats.AntiCheat;

@Target(value={ElementType.TYPE})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface PacketSetting {
    public int priority();

    public int reliability();

    public byte ordering();

    public Capability requiredCapability();

    public int handlingType();

    public AntiCheat[] anticheats() default {AntiCheat.None};

    public static class HandlingType {
        public static final int None = 0;
        public static final int Server = 1;
        public static final int Client = 2;
        public static final int ClientLoading = 4;
        public static final int All = 7;

        static int getType(boolean server, boolean client, boolean clientLoading) {
            int type = 0;
            if (server) {
                type |= 1;
            }
            if (client) {
                type |= 2;
            }
            if (clientLoading) {
                type |= 4;
            }
            return type;
        }
    }
}


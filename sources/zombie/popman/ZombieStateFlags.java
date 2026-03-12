/*
 * Decompiled with CFR 0.152.
 */
package zombie.popman;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.popman.ZombieStateFlag;
import zombie.util.list.PZArrayUtil;

public final class ZombieStateFlags {
    private int flags;

    public ZombieStateFlags() {
    }

    public ZombieStateFlags(int flags) {
        this.flags = flags;
    }

    public ZombieStateFlags(ZombieStateFlag ... flags) {
        for (ZombieStateFlag flag : flags) {
            this.setFlag(flag);
        }
    }

    public static ZombieStateFlags fromInt(int stateFlags) {
        return new ZombieStateFlags(stateFlags);
    }

    public static int intFromZombie(IsoZombie fromZombie) {
        int state = 0;
        state = ZombieStateFlags.setFlag(state, ZombieStateFlag.Initialized);
        state = ZombieStateFlags.setFlag(state, ZombieStateFlag.Crawling, fromZombie.isCrawling());
        state = ZombieStateFlags.setFlag(state, ZombieStateFlag.CanWalk, fromZombie.isCanWalk());
        state = ZombieStateFlags.setFlag(state, ZombieStateFlag.FakeDead, fromZombie.isFakeDead());
        state = ZombieStateFlags.setFlag(state, ZombieStateFlag.CanCrawlUnderVehicle, fromZombie.isCanCrawlUnderVehicle());
        state = ZombieStateFlags.setFlag(state, ZombieStateFlag.ReanimatedForGrappleOnly, fromZombie.isReanimatedForGrappleOnly());
        return state;
    }

    public static ZombieStateFlags fromZombie(IsoZombie fromZombie) {
        return ZombieStateFlags.fromInt(ZombieStateFlags.intFromZombie(fromZombie));
    }

    public void setFlag(ZombieStateFlag flag) {
        this.flags = ZombieStateFlags.setFlag(this.flags, flag);
    }

    public void clearFlag(ZombieStateFlag flag) {
        this.flags = ZombieStateFlags.clearFlag(this.flags, flag);
    }

    public void setFlag(ZombieStateFlag flag, boolean isTrue) {
        this.flags = ZombieStateFlags.setFlag(this.flags, flag, isTrue);
    }

    public boolean checkFlag(ZombieStateFlag flag) {
        return ZombieStateFlags.checkFlag(this.flags, flag);
    }

    public static int setFlag(int state, ZombieStateFlag flag) {
        return state | flag.flag;
    }

    public static int clearFlag(int state, ZombieStateFlag flag) {
        return state & ~flag.flag;
    }

    public static int setFlag(int state, ZombieStateFlag flag, boolean isTrue) {
        if (isTrue) {
            return ZombieStateFlags.setFlag(state, flag);
        }
        return ZombieStateFlags.clearFlag(state, flag);
    }

    public static boolean checkFlag(int state, ZombieStateFlag flag) {
        return (state & flag.flag) != 0;
    }

    public int asInt() {
        return this.flags;
    }

    public boolean isInitialized() {
        return this.checkFlag(ZombieStateFlag.Initialized);
    }

    public boolean isCrawling() {
        return this.checkFlag(ZombieStateFlag.Crawling);
    }

    public boolean isCanWalk() {
        return this.checkFlag(ZombieStateFlag.CanWalk);
    }

    public boolean isFakeDead() {
        return this.checkFlag(ZombieStateFlag.FakeDead);
    }

    public boolean isCanCrawlUnderVehicle() {
        return this.checkFlag(ZombieStateFlag.CanCrawlUnderVehicle);
    }

    public boolean isReanimatedForGrappleOnly() {
        return this.checkFlag(ZombieStateFlag.ReanimatedForGrappleOnly);
    }

    public ZombieStateFlag[] toArray() {
        ArrayList<ZombieStateFlag> array = new ArrayList<ZombieStateFlag>();
        for (ZombieStateFlag flag : ZombieStateFlag.values()) {
            if (!this.checkFlag(flag)) continue;
            array.add(flag);
        }
        return array.toArray(new ZombieStateFlag[0]);
    }

    public String toString() {
        String contentsStr = PZArrayUtil.arrayToString(this.toArray(), Enum::toString, "{ ", " }", ", ");
        return this.getClass().getName() + "{ " + contentsStr + "}";
    }

    public static String intToString(int state) {
        return ZombieStateFlags.fromInt(state).toString();
    }
}


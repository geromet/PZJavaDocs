/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.statistics;

import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.network.GameClient;
import zombie.network.statistics.PingManager;

public class StatusManager {
    private static final StatusManager instance = new StatusManager();
    private static final KahluaTable statusTable = LuaManager.platform.newTable();

    public static StatusManager getInstance() {
        return instance;
    }

    public KahluaTable getTable() {
        statusTable.wipe();
        if (GameClient.client) {
            statusTable.rawset("serverTime", (Object)NumberFormat.getNumberInstance().format(TimeUnit.NANOSECONDS.toSeconds(GameTime.getServerTime())));
            statusTable.rawset("svnRevision", (Object)"62e0c8afb10b8dd38c0cbb2f95d49897058ed073");
            statusTable.rawset("buildDate", (Object)"2026-03-10");
            statusTable.rawset("buildTime", (Object)"12:36:37");
            statusTable.rawset("position", (Object)String.format("( %.3f ; %.3f ; %.3f )", Float.valueOf(IsoPlayer.getInstance().getX()), Float.valueOf(IsoPlayer.getInstance().getY()), Float.valueOf(IsoPlayer.getInstance().getZ())));
            statusTable.rawset("version", (Object)Core.getInstance().getVersion());
            statusTable.rawset("lastPing", (Object)String.format("%03d", PingManager.getPing()));
        }
        return statusTable;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.util.ArrayList;
import java.util.Objects;
import zombie.characters.IsoPlayer;
import zombie.characters.VisibilityData;
import zombie.core.math.PZMath;
import zombie.core.textures.ColorInfo;
import zombie.iso.IsoGridSquare;
import zombie.iso.LosUtil;
import zombie.network.ServerMap;

public class ServerLOS {
    public static ServerLOS instance;
    private LOSThread thread;
    private final ArrayList<PlayerData> playersMain = new ArrayList();
    private final ArrayList<PlayerData> playersLos = new ArrayList();
    private boolean mapLoading;
    private boolean suspended;
    private static final int PD_SIZE_IN_CHUNKS = 12;
    private static final int PD_SIZE_IN_SQUARES = 96;
    boolean wasSuspended;

    private void noise(String str) {
    }

    public static void init() {
        instance = new ServerLOS();
        instance.start();
    }

    public void start() {
        this.thread = new LOSThread(this);
        this.thread.setName("LOS");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addPlayer(IsoPlayer player) {
        ArrayList<PlayerData> arrayList = this.playersMain;
        synchronized (arrayList) {
            if (this.findData(player) != null) {
                return;
            }
            PlayerData data = new PlayerData(player);
            this.playersMain.add(data);
            Object object = this.thread.notifier;
            synchronized (object) {
                this.thread.notifier.notify();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removePlayer(IsoPlayer player) {
        ArrayList<PlayerData> arrayList = this.playersMain;
        synchronized (arrayList) {
            PlayerData data = this.findData(player);
            this.playersMain.remove(data);
            Object object = this.thread.notifier;
            synchronized (object) {
                this.thread.notifier.notify();
            }
        }
    }

    public boolean isCouldSee(IsoPlayer player, IsoGridSquare sq) {
        PlayerData data = this.findData(player);
        if (data != null) {
            int minX = data.px - 48;
            int minY = data.py - 48;
            int minZ = data.pz - LosUtil.sizeZ / 2;
            int x = sq.x - minX;
            int y = sq.y - minY;
            int z = sq.z - minZ;
            if (x >= 0 && x < 96 && y >= 0 && y < 96 && z >= 0 && z < LosUtil.sizeZ) {
                return data.visible[x][y][z];
            }
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void doServerZombieLOS(IsoPlayer player) {
        if (!ServerMap.instance.updateLosThisFrame) {
            return;
        }
        PlayerData data = this.findData(player);
        if (data == null) {
            return;
        }
        if (data.status == UpdateStatus.NeverDone) {
            data.status = UpdateStatus.ReadyInMain;
        }
        if (data.status == UpdateStatus.ReadyInMain) {
            data.status = UpdateStatus.WaitingInLOS;
            this.noise("WaitingInLOS playerID=" + player.onlineId);
            Object object = this.thread.notifier;
            synchronized (object) {
                this.thread.notifier.notify();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void updateLOS(IsoPlayer player) {
        PlayerData data = this.findData(player);
        if (data == null) {
            return;
        }
        if (data.status == UpdateStatus.ReadyInLOS || data.status == UpdateStatus.ReadyInMain) {
            if (data.status == UpdateStatus.ReadyInLOS) {
                this.noise("BusyInMain playerID=" + player.onlineId);
            }
            data.status = UpdateStatus.BusyInMain;
            player.updateLOS();
            data.status = UpdateStatus.ReadyInMain;
            Object object = this.thread.notifier;
            synchronized (object) {
                this.thread.notifier.notify();
            }
        }
    }

    private PlayerData findData(IsoPlayer player) {
        for (int i = 0; i < this.playersMain.size(); ++i) {
            if (this.playersMain.get((int)i).player != player) continue;
            return this.playersMain.get(i);
        }
        return null;
    }

    public void suspend() {
        this.mapLoading = true;
        this.wasSuspended = this.suspended;
        while (!this.suspended) {
            try {
                Thread.sleep(1L);
            }
            catch (InterruptedException interruptedException) {}
        }
        if (!this.wasSuspended) {
            this.noise("suspend **********");
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void resume() {
        this.mapLoading = false;
        Object object = this.thread.notifier;
        synchronized (object) {
            this.thread.notifier.notify();
        }
        if (!this.wasSuspended) {
            this.noise("resume **********");
        }
    }

    private class LOSThread
    extends Thread {
        public final Object notifier;
        final /* synthetic */ ServerLOS this$0;

        private LOSThread(ServerLOS serverLOS) {
            ServerLOS serverLOS2 = serverLOS;
            Objects.requireNonNull(serverLOS2);
            this.this$0 = serverLOS2;
            this.notifier = new Object();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    while (true) {
                        this.runInner();
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    continue;
                }
                break;
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private void runInner() {
            ArrayList<PlayerData> arrayList = this.this$0.playersMain;
            synchronized (arrayList) {
                this.this$0.playersLos.clear();
                this.this$0.playersLos.addAll(this.this$0.playersMain);
            }
            for (int i = 0; i < this.this$0.playersLos.size(); ++i) {
                PlayerData data = this.this$0.playersLos.get(i);
                if (data.status == UpdateStatus.WaitingInLOS) {
                    data.status = UpdateStatus.BusyInLOS;
                    this.this$0.noise("BusyInLOS playerID=" + data.player.onlineId);
                    this.calcLOS(data);
                    data.status = UpdateStatus.ReadyInLOS;
                }
                if (this.this$0.mapLoading) break;
            }
            while (this.shouldWait()) {
                this.this$0.suspended = true;
                Object object = this.notifier;
                synchronized (object) {
                    try {
                        this.notifier.wait();
                    }
                    catch (InterruptedException interruptedException) {
                        // empty catch block
                    }
                }
            }
            this.this$0.suspended = false;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private void calcLOS(PlayerData data) {
            boolean skip = data.px == PZMath.fastfloor(data.player.getX()) && data.py == PZMath.fastfloor(data.player.getY()) && data.pz == PZMath.fastfloor(data.player.getZ());
            data.px = PZMath.fastfloor(data.player.getX());
            data.py = PZMath.fastfloor(data.player.getY());
            data.pz = PZMath.fastfloor(data.player.getZ());
            data.player.initLightInfo2();
            if (skip) {
                return;
            }
            boolean playerIndex = false;
            LosUtil.PerPlayerData ppd = LosUtil.cachedresults[0];
            ppd.checkSize();
            for (int x = 0; x < LosUtil.sizeX; ++x) {
                for (int y = 0; y < LosUtil.sizeY; ++y) {
                    for (int z = 0; z < LosUtil.sizeZ; ++z) {
                        ppd.cachedresults[x][y][z] = 0;
                    }
                }
            }
            try {
                IsoPlayer.players[0] = data.player;
                int playerX = data.px;
                int playerY = data.py;
                int playerZ = data.pz;
                int minX = playerX - 48;
                int maxX = minX + 96;
                int minY = playerY - 48;
                int maxY = minY + 96;
                int minZ = playerZ - LosUtil.sizeZ / 2;
                int maxZ = minZ + LosUtil.sizeZ;
                IsoPlayer isoGameCharacter = data.player;
                VisibilityData visibilityData = isoGameCharacter.calculateVisibilityData();
                for (int x = minX; x < maxX; ++x) {
                    for (int y = minY; y < maxY; ++y) {
                        for (int z = minZ; z < maxZ; ++z) {
                            IsoGridSquare sq = ServerMap.instance.getGridSquare(x, y, z);
                            if (sq != null) {
                                sq.CalcVisibility(0, isoGameCharacter, visibilityData);
                                data.visible[x - minX][y - minY][z - minZ] = sq.isCouldSee(0);
                                sq.checkRoomSeen(0);
                                continue;
                            }
                            data.visible[x - minX][y - minY][z - minZ] = false;
                        }
                    }
                }
            }
            finally {
                IsoPlayer.players[0] = null;
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private boolean shouldWait() {
            if (this.this$0.mapLoading) {
                return true;
            }
            for (int i = 0; i < this.this$0.playersLos.size(); ++i) {
                PlayerData data = this.this$0.playersLos.get(i);
                if (data.status != UpdateStatus.WaitingInLOS) continue;
                return false;
            }
            ArrayList<PlayerData> arrayList = this.this$0.playersMain;
            synchronized (arrayList) {
                if (this.this$0.playersLos.size() != this.this$0.playersMain.size()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class PlayerData {
        public IsoPlayer player;
        public UpdateStatus status = UpdateStatus.NeverDone;
        public int px;
        public int py;
        public int pz;
        public boolean[][][] visible = new boolean[96][96][LosUtil.sizeZ];

        public PlayerData(IsoPlayer player) {
            this.player = player;
        }
    }

    static enum UpdateStatus {
        NeverDone,
        WaitingInLOS,
        BusyInLOS,
        ReadyInLOS,
        BusyInMain,
        ReadyInMain;

    }

    public static final class ServerLighting
    implements IsoGridSquare.ILighting {
        private static final byte LOS_SEEN = 1;
        private static final byte LOS_COULD_SEE = 2;
        private static final byte LOS_CAN_SEE = 4;
        private static final ColorInfo lightInfo = new ColorInfo();
        private byte los;

        @Override
        public int lightverts(int i) {
            return 0;
        }

        @Override
        public float lampostTotalR() {
            return 0.0f;
        }

        @Override
        public float lampostTotalG() {
            return 0.0f;
        }

        @Override
        public float lampostTotalB() {
            return 0.0f;
        }

        @Override
        public boolean bSeen() {
            return (this.los & 1) != 0;
        }

        @Override
        public boolean bCanSee() {
            return (this.los & 4) != 0;
        }

        @Override
        public boolean bCouldSee() {
            return (this.los & 2) != 0;
        }

        @Override
        public float darkMulti() {
            return 0.0f;
        }

        @Override
        public float targetDarkMulti() {
            return 0.0f;
        }

        @Override
        public ColorInfo lightInfo() {
            ServerLighting.lightInfo.r = 1.0f;
            ServerLighting.lightInfo.g = 1.0f;
            ServerLighting.lightInfo.b = 1.0f;
            return lightInfo;
        }

        @Override
        public void lightverts(int i, int value) {
        }

        @Override
        public void lampostTotalR(float r) {
        }

        @Override
        public void lampostTotalG(float g) {
        }

        @Override
        public void lampostTotalB(float b) {
        }

        @Override
        public void bSeen(boolean seen) {
            this.los = seen ? (byte)(this.los | 1) : (byte)(this.los & 0xFFFFFFFE);
        }

        @Override
        public void bCanSee(boolean canSee) {
            this.los = canSee ? (byte)(this.los | 4) : (byte)(this.los & 0xFFFFFFFB);
        }

        @Override
        public void bCouldSee(boolean couldSee) {
            this.los = couldSee ? (byte)(this.los | 2) : (byte)(this.los & 0xFFFFFFFD);
        }

        @Override
        public void darkMulti(float f) {
        }

        @Override
        public void targetDarkMulti(float f) {
        }

        @Override
        public int resultLightCount() {
            return 0;
        }

        @Override
        public IsoGridSquare.ResultLight getResultLight(int index) {
            return null;
        }

        @Override
        public void reset() {
            this.los = 0;
        }
    }
}


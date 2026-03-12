/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import zombie.core.logger.ExceptionLogger;
import zombie.savefile.ServerPlayerDB;
import zombie.vehicles.VehiclesDB2;

public class ServerPlayersVehicles {
    public static final ServerPlayersVehicles instance = new ServerPlayersVehicles();
    private SPVThread thread;

    public void init() {
        this.thread = new SPVThread();
        this.thread.setName("ServerPlayersVehicles");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public void stop() {
        if (this.thread != null) {
            this.thread.stop = true;
            while (this.thread.isAlive()) {
                try {
                    Thread.sleep(100L);
                }
                catch (InterruptedException interruptedException) {}
            }
            this.thread = null;
        }
    }

    private static final class SPVThread
    extends Thread {
        boolean stop;

        private SPVThread() {
        }

        @Override
        public void run() {
            while (!this.stop) {
                try {
                    this.runInner();
                }
                catch (Throwable t) {
                    ExceptionLogger.logException(t);
                }
            }
        }

        void runInner() {
            ServerPlayerDB.getInstance().process();
            VehiclesDB2.instance.updateWorldStreamer();
            try {
                Thread.sleep(500L);
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
        }
    }
}


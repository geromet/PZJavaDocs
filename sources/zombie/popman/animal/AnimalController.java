/*
 * Decompiled with CFR 0.152.
 */
package zombie.popman.animal;

import zombie.network.GameServer;
import zombie.popman.animal.AnimalSynchronizationManager;

public class AnimalController {
    private static final AnimalController instance = new AnimalController();

    public static AnimalController getInstance() {
        return instance;
    }

    private AnimalController() {
    }

    public void update() {
        if (GameServer.server) {
            AnimalSynchronizationManager.getInstance().update();
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.MovingObjectUpdateSchedulerUpdateBucket;
import zombie.UpdateSchedulerSimulationLevel;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;

public final class MovingObjectUpdateScheduler {
    public static final MovingObjectUpdateScheduler instance = new MovingObjectUpdateScheduler();
    final MovingObjectUpdateSchedulerUpdateBucket fullSimulation = new MovingObjectUpdateSchedulerUpdateBucket(1);
    final MovingObjectUpdateSchedulerUpdateBucket halfSimulation = new MovingObjectUpdateSchedulerUpdateBucket(2);
    final MovingObjectUpdateSchedulerUpdateBucket quarterSimulation = new MovingObjectUpdateSchedulerUpdateBucket(4);
    final MovingObjectUpdateSchedulerUpdateBucket eighthSimulation = new MovingObjectUpdateSchedulerUpdateBucket(8);
    final MovingObjectUpdateSchedulerUpdateBucket sixteenthSimulation = new MovingObjectUpdateSchedulerUpdateBucket(16);
    long frameCounter;
    private boolean isEnabled = true;

    public long getFrameCounter() {
        return this.frameCounter;
    }

    public void startFrame() {
        ++this.frameCounter;
        this.fullSimulation.clear();
        this.halfSimulation.clear();
        this.quarterSimulation.clear();
        this.eighthSimulation.clear();
        this.sixteenthSimulation.clear();
        float averageFps = GameWindow.averageFPS;
        ArrayList<IsoMovingObject> objectList = IsoWorld.instance.getCell().getObjectList();
        block7: for (int i = 0; i < objectList.size(); ++i) {
            UpdateSchedulerSimulationLevel sim;
            IsoMovingObject isoMovingObject = objectList.get(i);
            if (GameServer.server && isoMovingObject instanceof IsoZombie) {
                IsoZombie isoZombie = (IsoZombie)isoMovingObject;
                if (!GameServer.guiCommandline) continue;
                isoZombie.updateForServerGui();
                continue;
            }
            if (isoMovingObject.getCurrentSquare() == null) {
                isoMovingObject.setCurrentSquareFromPosition();
            }
            UpdateSchedulerSimulationLevel updateSchedulerSimulationLevel = sim = this.getUpdateSchedulerSimulationLevelForObject(isoMovingObject, averageFps);
            int n = 0;
            switch (SwitchBootstraps.enumSwitch("enumSwitch", new Object[]{"FULL", "HALF", "QUARTER", "EIGHTH", "SIXTEENTH"}, (UpdateSchedulerSimulationLevel)updateSchedulerSimulationLevel, n)) {
                case 0: {
                    this.fullSimulation.add(isoMovingObject);
                    continue block7;
                }
                case 1: {
                    this.halfSimulation.add(isoMovingObject);
                    continue block7;
                }
                case 2: {
                    this.quarterSimulation.add(isoMovingObject);
                    continue block7;
                }
                case 3: {
                    this.eighthSimulation.add(isoMovingObject);
                    continue block7;
                }
                case 4: {
                    this.sixteenthSimulation.add(isoMovingObject);
                    continue block7;
                }
            }
        }
    }

    private UpdateSchedulerSimulationLevel getUpdateSchedulerSimulationLevelForObject(IsoMovingObject isoMovingObject, float averageFps) {
        if (!this.isEnabled || GameServer.server) {
            return UpdateSchedulerSimulationLevel.FULL;
        }
        UpdateSchedulerSimulationLevel minSim = isoMovingObject.getMinimumSimulationLevel();
        if (minSim == UpdateSchedulerSimulationLevel.FULL) {
            return minSim;
        }
        if (!isoMovingObject.getDoRender() || isoMovingObject.isSceneCulled()) {
            return minSim;
        }
        float distance = 1.0E8f;
        int levelSeparation = Integer.MAX_VALUE;
        float alpha = 0.0f;
        float targetAlpha = 0.0f;
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; ++playerIndex) {
            IsoPlayer player = IsoPlayer.players[playerIndex];
            if (player == null) continue;
            if (player == isoMovingObject) {
                return UpdateSchedulerSimulationLevel.FULL;
            }
            distance = PZMath.min(isoMovingObject.DistTo(player), distance);
            levelSeparation = PZMath.min(PZMath.abs(isoMovingObject.getZi() - player.getZi()), levelSeparation);
            alpha = PZMath.max(isoMovingObject.getAlpha(playerIndex), alpha);
            targetAlpha = PZMath.max(isoMovingObject.getTargetAlpha(playerIndex), targetAlpha);
        }
        UpdateSchedulerSimulationLevel sim = UpdateSchedulerSimulationLevel.FULL;
        float minAlpha = 0.25f;
        if (alpha < 0.25f && targetAlpha < 0.25f) {
            sim = sim.less();
            if (distance > 10.0f) {
                sim = sim.less();
            }
            if (levelSeparation > 1) {
                sim = minSim;
            }
        }
        if (distance > 30.0f) {
            sim = sim.less();
        }
        if (distance > 60.0f) {
            sim = sim.less();
            if (averageFps < 20.0f) {
                sim = sim.less();
            }
            if (averageFps < 10.0f) {
                sim = sim.less();
            }
        }
        if (distance > 80.0f) {
            sim = sim.less();
            if (averageFps < 20.0f) {
                sim = sim.less();
            }
        }
        if (averageFps > 25.0f) {
            sim = sim.more();
        }
        if (averageFps > 35.0f) {
            sim = sim.more();
        }
        if (averageFps > 45.0f) {
            sim = sim.more();
        }
        if (averageFps > 55.0f) {
            sim = sim.more();
        }
        sim = sim.max(minSim);
        return sim;
    }

    public void update() {
        GameTime.getInstance().perObjectMultiplier = 1.0f;
        this.fullSimulation.update((int)this.frameCounter);
        this.halfSimulation.update((int)this.frameCounter);
        this.quarterSimulation.update((int)this.frameCounter);
        this.eighthSimulation.update((int)this.frameCounter);
        this.sixteenthSimulation.update((int)this.frameCounter);
    }

    public void postupdate() {
        GameTime.getInstance().perObjectMultiplier = 1.0f;
        this.fullSimulation.postupdate((int)this.frameCounter);
        this.halfSimulation.postupdate((int)this.frameCounter);
        this.quarterSimulation.postupdate((int)this.frameCounter);
        this.eighthSimulation.postupdate((int)this.frameCounter);
        this.sixteenthSimulation.postupdate((int)this.frameCounter);
    }

    public void updateAnimation() {
        GameTime.getInstance().perObjectMultiplier = 1.0f;
        this.fullSimulation.updateAnimation((int)this.frameCounter);
        this.halfSimulation.updateAnimation((int)this.frameCounter);
        this.quarterSimulation.updateAnimation((int)this.frameCounter);
        this.eighthSimulation.updateAnimation((int)this.frameCounter);
        this.sixteenthSimulation.updateAnimation((int)this.frameCounter);
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public void removeObject(IsoMovingObject object) {
        this.fullSimulation.removeObject(object);
        this.halfSimulation.removeObject(object);
        this.quarterSimulation.removeObject(object);
        this.eighthSimulation.removeObject(object);
        this.sixteenthSimulation.removeObject(object);
    }

    public ArrayList<IsoMovingObject> getBucket() {
        return this.fullSimulation.getBucket((int)this.frameCounter);
    }
}


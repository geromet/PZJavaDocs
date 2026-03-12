/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai;

import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoUtils;
import zombie.network.GameServer;
import zombie.ui.SpeedControls;

public final class WalkingOnTheSpot {
    private float x;
    private float y;
    private float time;
    private float seconds;

    public boolean check(IsoGameCharacter chr) {
        float timeCheck = 400.0f;
        if (chr.isAnimal()) {
            timeCheck = 30.0f;
            if (!GameServer.server && SpeedControls.instance.getCurrentGameSpeed() == 4) {
                timeCheck = 150.0f;
            }
        }
        if (IsoUtils.DistanceToSquared(this.x, this.y, chr.getX(), chr.getY()) < 0.010000001f) {
            this.time += GameTime.getInstance().getMultiplier();
            this.seconds += GameTime.getInstance().getThirtyFPSMultiplier() / 30.0f;
        } else {
            this.x = chr.getX();
            this.y = chr.getY();
            this.time = 0.0f;
            this.seconds = 0.0f;
        }
        return this.time > timeCheck;
    }

    public boolean check(float x1, float y1) {
        if (IsoUtils.DistanceToSquared(this.x, this.y, x1, y1) < 0.010000001f) {
            this.time += GameTime.getInstance().getMultiplier();
            this.seconds += GameTime.getInstance().getThirtyFPSMultiplier() / 30.0f;
        } else {
            this.x = x1;
            this.y = y1;
            this.time = 0.0f;
            this.seconds = 0.0f;
        }
        return this.time > 400.0f;
    }

    public void reset(float x1, float y1) {
        this.x = x1;
        this.y = y1;
        this.time = 0.0f;
        this.seconds = 0.0f;
    }
}


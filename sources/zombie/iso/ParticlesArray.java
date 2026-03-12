/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import zombie.debug.DebugLog;

public final class ParticlesArray<E>
extends ArrayList<E> {
    private boolean needToUpdate = true;
    private int particleSystemsCount = 0;
    private int particleSystemsLast = 0;

    public synchronized int addParticle(E p) {
        int i;
        if (p == null) {
            return -1;
        }
        if (this.size() == this.particleSystemsCount) {
            this.add(p);
            ++this.particleSystemsCount;
            this.needToUpdate = true;
            return this.size() - 1;
        }
        for (i = this.particleSystemsLast; i < this.size(); ++i) {
            if (this.get(i) != null) continue;
            this.particleSystemsLast = i;
            this.set(i, p);
            ++this.particleSystemsCount;
            this.needToUpdate = true;
            return i;
        }
        for (i = 0; i < this.particleSystemsLast; ++i) {
            if (this.get(i) != null) continue;
            this.particleSystemsLast = i;
            this.set(i, p);
            ++this.particleSystemsCount;
            this.needToUpdate = true;
            return i;
        }
        DebugLog.log("ERROR: ParticlesArray.addParticle has unknown error");
        return -1;
    }

    public synchronized boolean deleteParticle(int k) {
        if (k >= 0 && k < this.size() && this.get(k) != null) {
            this.set(k, null);
            --this.particleSystemsCount;
            this.needToUpdate = true;
            return true;
        }
        return false;
    }

    public synchronized void defragmentParticle() {
        int i;
        this.needToUpdate = false;
        if (this.particleSystemsCount == this.size() || this.size() == 0) {
            return;
        }
        int pointernull = -1;
        for (i = 0; i < this.size(); ++i) {
            if (this.get(i) != null) continue;
            pointernull = i;
            break;
        }
        for (i = this.size() - 1; i >= 0; --i) {
            if (this.get(i) == null) continue;
            this.set(pointernull, this.get(i));
            this.set(i, null);
            for (int k = pointernull; k < this.size(); ++k) {
                if (this.get(k) != null) continue;
                pointernull = k;
                break;
            }
            if (pointernull + 1 < i) continue;
            this.particleSystemsLast = pointernull;
            break;
        }
    }

    public synchronized int getCount() {
        return this.particleSystemsCount;
    }

    public synchronized boolean getNeedToUpdate() {
        return this.needToUpdate;
    }
}


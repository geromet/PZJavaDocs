/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.joml.Quaternionf;
import zombie.GameTime;
import zombie.core.physics.WorldSimulation;
import zombie.network.statistics.PingManager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleInterpolationData;

public class VehicleInterpolation {
    private static final ArrayDeque<VehicleInterpolationData> pool = new ArrayDeque();
    private static final List<VehicleInterpolationData> outdated = new ArrayList<VehicleInterpolationData>();
    private static final Quaternionf tempQuaternionA = new Quaternionf();
    private static final Quaternionf tempQuaternionB = new Quaternionf();
    private static final VehicleInterpolationData temp = new VehicleInterpolationData();
    final TreeSet<VehicleInterpolationData> buffer = new TreeSet();
    int delay;
    int history;
    int delayTarget;
    boolean buffering;
    float[] lastBuf1;
    boolean wasNull;
    long lastTime = -1L;
    long lastTimeA = -1L;
    long recountedTime;
    boolean highPing;
    boolean wasHighPing;
    private byte getPointUpdateTimeout;
    private final byte getPointUpdatePer = (byte)10;

    VehicleInterpolation() {
        this.reset();
        this.delay = 500;
        this.history = 800;
        this.delayTarget = this.delay;
    }

    public void reset() {
        this.buffering = true;
        this.clear();
    }

    public void clear() {
        if (!this.buffer.isEmpty()) {
            pool.addAll(this.buffer);
            this.buffer.clear();
            outdated.clear();
        }
    }

    public void update(long time) {
        VehicleInterpolation.temp.time = time - (long)this.delay;
        VehicleInterpolationData dataA = this.buffer.floor(temp);
        for (VehicleInterpolationData data : this.buffer) {
            if (time - data.time <= (long)this.history || data == dataA) continue;
            outdated.add(data);
        }
        outdated.forEach(this.buffer::remove);
        pool.addAll(outdated);
        outdated.clear();
        if (this.buffer.isEmpty()) {
            this.buffering = true;
        }
    }

    void getPointUpdate() {
        boolean isHighPing;
        byte by = this.getPointUpdateTimeout;
        this.getPointUpdateTimeout = (byte)(by + 1);
        if (by < 10) {
            return;
        }
        float dt = WorldSimulation.instance.periodSec * 10.0f;
        this.getPointUpdateTimeout = 0;
        int lastPing = PingManager.getPing();
        boolean bl = isHighPing = lastPing > 290;
        if (this.highPing && !isHighPing) {
            this.wasHighPing = true;
        }
        if (isHighPing) {
            this.wasHighPing = false;
        }
        this.highPing = isHighPing;
        if (this.delay != this.delayTarget) {
            int delayStep = Math.max(1, (int)(500.0f * dt / 3.0f));
            if (this.delay < this.delayTarget) {
                if (this.wasHighPing) {
                    this.wasHighPing = false;
                }
                if (this.highPing) {
                    delayStep = this.delayTarget - this.delay;
                }
                int d = Math.min(this.delayTarget - this.delay, delayStep);
                this.delay += d;
                this.history += d;
            } else {
                if (this.wasHighPing && (delayStep = (int)((float)delayStep / 8.0f)) < 1) {
                    delayStep = 1;
                }
                int d = Math.min(this.delay - this.delayTarget, delayStep);
                this.delay -= d;
                this.history -= d;
            }
        }
        int delayCap = 500;
        if (this.highPing) {
            delayCap = (int)((float)delayCap * 3.0f);
        }
        if (this.delayTarget != delayCap) {
            if (this.delayTarget < delayCap) {
                this.delayTarget += Math.max(1, (int)((float)delayCap * dt / 10.0f));
                if (this.highPing) {
                    this.delayTarget = delayCap;
                }
            } else {
                this.delayTarget -= Math.max(1, (int)((float)delayCap * dt / 10.0f));
            }
        }
        if (this.wasHighPing && !this.highPing && Math.abs(this.delay - this.delayTarget) < 10 && Math.abs(delayCap - this.delayTarget) < 10) {
            this.wasHighPing = false;
            this.delayTarget = 500;
        }
    }

    private void interpolationDataCurrentAdd(BaseVehicle vehicle) {
        VehicleInterpolationData d = pool.isEmpty() ? new VehicleInterpolationData() : pool.pop();
        d.time = GameTime.getServerTimeMills() - (long)this.delay;
        d.x = vehicle.jniTransform.origin.x + WorldSimulation.instance.offsetX;
        d.y = vehicle.jniTransform.origin.z + WorldSimulation.instance.offsetY;
        d.z = vehicle.jniTransform.origin.y;
        Quaternionf q = vehicle.jniTransform.getRotation(new Quaternionf());
        d.qx = q.x;
        d.qy = q.y;
        d.qz = q.z;
        d.qw = q.w;
        d.vx = vehicle.jniLinearVelocity.x;
        d.vy = vehicle.jniLinearVelocity.y;
        d.vz = vehicle.jniLinearVelocity.z;
        d.engineSpeed = (float)vehicle.engineSpeed;
        d.throttle = vehicle.throttle;
        d.setNumWheels((short)vehicle.wheelInfo.length);
        for (int i = 0; i < d.wheelsCount; ++i) {
            d.wheelSteering[i] = vehicle.wheelInfo[i].steering;
            d.wheelRotation[i] = vehicle.wheelInfo[i].rotation;
            d.wheelSkidInfo[i] = vehicle.wheelInfo[i].skidInfo;
            d.wheelSuspensionLength[i] = vehicle.wheelInfo[i].suspensionLength > 0.0f ? vehicle.wheelInfo[i].suspensionLength : 0.3f;
        }
        this.buffer.add(d);
    }

    public void interpolationDataAdd(BaseVehicle vehicle, VehicleInterpolationData data, long currentTime) {
        if (this.buffer.isEmpty()) {
            this.interpolationDataCurrentAdd(vehicle);
        }
        VehicleInterpolationData d = pool.isEmpty() ? new VehicleInterpolationData() : pool.pop();
        d.copy(data);
        this.buffer.add(d);
        this.update(currentTime);
    }

    public boolean interpolationDataGet(float[] buf1, float[] buf2) {
        long time = WorldSimulation.instance.time - (long)this.delay;
        return this.interpolationDataGet(buf1, buf2, time);
    }

    public VehicleInterpolationData getLastAddedInterpolationPoint() {
        try {
            return this.buffer.last();
        }
        catch (Exception e) {
            return null;
        }
    }

    public void setDelayLength(float d) {
        this.delayTarget = (int)(d * 500.0f);
    }

    public boolean isDelayLengthIncreased() {
        return this.delayTarget > 500;
    }

    public boolean interpolationDataGet(float[] buf1, float[] buf2, long time) {
        float tempTimeM;
        this.getPointUpdate();
        VehicleInterpolation.temp.time = time;
        VehicleInterpolationData dataB = this.buffer.higher(temp);
        VehicleInterpolationData dataA = this.buffer.floor(temp);
        if (this.buffering) {
            if (this.buffer.size() < 2 || dataB == null || dataA == null) {
                return false;
            }
            this.buffering = false;
        } else if (this.buffer.isEmpty()) {
            this.reset();
            return false;
        }
        int n = 0;
        if (dataB == null) {
            if (dataA != null) {
                int i;
                this.wasNull = true;
                this.lastTimeA = -1L;
                this.recountedTime = this.lastTime = dataA.time;
                buf2[0] = dataA.engineSpeed;
                buf2[1] = dataA.throttle;
                buf1[n++] = dataA.x;
                buf1[n++] = dataA.y;
                buf1[n++] = dataA.z;
                buf1[n++] = dataA.qx;
                buf1[n++] = dataA.qy;
                buf1[n++] = dataA.qz;
                buf1[n++] = dataA.qw;
                buf1[n++] = dataA.vx;
                buf1[n++] = dataA.vy;
                buf1[n++] = dataA.vz;
                buf1[n++] = dataA.wheelsCount;
                for (i = 0; i < dataA.wheelsCount; ++i) {
                    buf1[n++] = dataA.wheelSteering[i];
                    buf1[n++] = dataA.wheelRotation[i];
                    buf1[n++] = dataA.wheelSkidInfo[i];
                    buf1[n++] = dataA.wheelSuspensionLength[i];
                }
                this.lastBuf1 = new float[buf1.length];
                for (i = 0; i < buf1.length; ++i) {
                    this.lastBuf1[i] = buf1[i];
                }
                this.reset();
                return true;
            }
            this.reset();
            return false;
        }
        if (dataA == null || Math.abs(dataB.time - dataA.time) < 10L && !this.wasNull || this.wasNull && dataB.time - this.lastTime < 10L) {
            return false;
        }
        if (this.lastTimeA == -1L) {
            this.lastTimeA = dataA.time;
        }
        if (this.lastTimeA != dataA.time && this.wasNull) {
            this.lastTimeA = dataA.time;
            this.wasNull = false;
        }
        if (this.wasNull) {
            long timeChunk;
            this.recountedTime = time - this.recountedTime > 20L ? (this.recountedTime += (timeChunk = (dataB.time - this.lastTime) / 7L) > 20L ? timeChunk : 20L) : time;
            tempTimeM = (float)(this.recountedTime - this.lastTime) / (float)(dataB.time - this.lastTime);
        } else {
            tempTimeM = (float)(time - dataA.time) / (float)(dataB.time - dataA.time);
        }
        float timeM = tempTimeM;
        buf2[0] = (dataB.engineSpeed - dataA.engineSpeed) * timeM + dataA.engineSpeed;
        buf2[1] = (dataB.throttle - dataA.throttle) * timeM + dataA.throttle;
        if (this.wasNull) {
            buf1[n] = (dataB.x - this.lastBuf1[n]) * timeM + this.lastBuf1[n];
            buf1[++n] = (dataB.y - this.lastBuf1[n]) * timeM + this.lastBuf1[n];
            buf1[++n] = (dataB.z - this.lastBuf1[n]) * timeM + this.lastBuf1[n];
            tempQuaternionA.set(dataA.qx, dataA.qy, dataA.qz, dataA.qw);
            tempQuaternionB.set(dataB.qx, dataB.qy, dataB.qz, dataB.qw);
            tempQuaternionA.nlerp(tempQuaternionB, timeM);
            int n2 = ++n;
            buf1[n2] = VehicleInterpolation.tempQuaternionA.x;
            int n3 = ++n;
            buf1[n3] = VehicleInterpolation.tempQuaternionA.y;
            int n4 = ++n;
            buf1[n4] = VehicleInterpolation.tempQuaternionA.z;
            int n5 = ++n;
            buf1[n5] = VehicleInterpolation.tempQuaternionA.w;
            buf1[++n] = (dataB.vx - this.lastBuf1[n]) * timeM + this.lastBuf1[n];
            buf1[++n] = (dataB.vy - this.lastBuf1[n]) * timeM + this.lastBuf1[n];
            buf1[++n] = (dataB.vz - this.lastBuf1[n]) * timeM + this.lastBuf1[n];
            int n6 = ++n;
            ++n;
            buf1[n6] = dataB.wheelsCount;
            for (int i = 0; i < dataB.wheelsCount; ++i) {
                buf1[n++] = (dataB.wheelSteering[i] - dataA.wheelSteering[i]) * timeM + dataA.wheelSteering[i];
                buf1[n++] = (dataB.wheelRotation[i] - dataA.wheelRotation[i]) * timeM + dataA.wheelRotation[i];
                buf1[n++] = (dataB.wheelSkidInfo[i] - dataA.wheelSkidInfo[i]) * timeM + dataA.wheelSkidInfo[i];
                buf1[n++] = (dataB.wheelSuspensionLength[i] - dataA.wheelSuspensionLength[i]) * timeM + dataA.wheelSuspensionLength[i];
            }
        } else {
            buf1[n++] = (dataB.x - dataA.x) * timeM + dataA.x;
            buf1[n++] = (dataB.y - dataA.y) * timeM + dataA.y;
            buf1[n++] = (dataB.z - dataA.z) * timeM + dataA.z;
            tempQuaternionA.set(dataA.qx, dataA.qy, dataA.qz, dataA.qw);
            tempQuaternionB.set(dataB.qx, dataB.qy, dataB.qz, dataB.qw);
            tempQuaternionA.nlerp(tempQuaternionB, timeM);
            buf1[n++] = VehicleInterpolation.tempQuaternionA.x;
            buf1[n++] = VehicleInterpolation.tempQuaternionA.y;
            buf1[n++] = VehicleInterpolation.tempQuaternionA.z;
            buf1[n++] = VehicleInterpolation.tempQuaternionA.w;
            buf1[n++] = (dataB.vx - dataA.vx) * timeM + dataA.vx;
            buf1[n++] = (dataB.vy - dataA.vy) * timeM + dataA.vy;
            buf1[n++] = (dataB.vz - dataA.vz) * timeM + dataA.vz;
            buf1[n++] = dataB.wheelsCount;
            for (int i = 0; i < dataB.wheelsCount; ++i) {
                buf1[n++] = (dataB.wheelSteering[i] - dataA.wheelSteering[i]) * timeM + dataA.wheelSteering[i];
                buf1[n++] = (dataB.wheelRotation[i] - dataA.wheelRotation[i]) * timeM + dataA.wheelRotation[i];
                buf1[n++] = (dataB.wheelSkidInfo[i] - dataA.wheelSkidInfo[i]) * timeM + dataA.wheelSkidInfo[i];
                buf1[n++] = (dataB.wheelSuspensionLength[i] - dataA.wheelSuspensionLength[i]) * timeM + dataA.wheelSuspensionLength[i];
            }
        }
        this.wasNull = false;
        return true;
    }
}


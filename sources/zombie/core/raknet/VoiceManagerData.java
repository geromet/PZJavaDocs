/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.raknet;

import java.util.ArrayList;
import zombie.radio.devices.DeviceData;

public class VoiceManagerData {
    public static ArrayList<VoiceManagerData> data = new ArrayList();
    public long userplaychannel = 0L;
    public long userplaysound = 0L;
    public boolean userplaymute = false;
    public long voicetimeout = 0L;
    public final ArrayList<RadioData> radioData = new ArrayList();
    public boolean isCanHearAll;
    short index;

    public VoiceManagerData(short index) {
        this.index = index;
    }

    public static VoiceManagerData get(short onlineId) {
        VoiceManagerData d;
        if (data.size() <= onlineId) {
            for (short i = (short)data.size(); i <= onlineId; i = (short)(i + 1)) {
                VoiceManagerData td = new VoiceManagerData(i);
                data.add(td);
            }
        }
        if ((d = data.get(onlineId)) == null) {
            d = new VoiceManagerData(onlineId);
            data.set(onlineId, d);
        }
        return d;
    }

    public static class RadioData {
        DeviceData deviceData;
        public int freq;
        public float distance;
        public short x;
        public short y;
        float lastReceiveDistance;

        public RadioData(float distance, float x, float y) {
            this(null, 0, distance, x, y);
        }

        public RadioData(int freq, float distance, float x, float y) {
            this(null, freq, distance, x, y);
        }

        public RadioData(DeviceData data, float x, float y) {
            this(data, data.getChannel(), data.getMicIsMuted() ? 0.0f : (float)data.getTransmitRange(), x, y);
        }

        private RadioData(DeviceData deviceData, int freq, float distance, float x, float y) {
            this.deviceData = deviceData;
            this.freq = freq;
            this.distance = distance;
            this.x = (short)x;
            this.y = (short)y;
        }

        public boolean isTransmissionAvailable() {
            return this.freq != 0 && this.deviceData != null && this.deviceData.getIsTurnedOn() && this.deviceData.getIsTwoWay() && !this.deviceData.isNoTransmit() && !this.deviceData.getMicIsMuted();
        }

        public boolean isReceivingAvailable(int channel) {
            return this.freq != 0 && this.deviceData != null && this.deviceData.getIsTurnedOn() && this.deviceData.getChannel() == channel && this.deviceData.getDeviceVolume() != 0.0f && !this.deviceData.isPlayingMedia();
        }

        public DeviceData getDeviceData() {
            return this.deviceData;
        }
    }

    public static enum VoiceDataSource {
        Unknown,
        Voice,
        Radio,
        Cheat;

    }
}


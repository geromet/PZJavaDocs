/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.service;

import java.util.List;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.popman.MPDebugInfo;

@PacketSetting(ordering=0, priority=1, reliability=0, requiredCapability=Capability.ConnectWithDebug, handlingType=3)
public class ServerDebugInfo
implements INetworkPacket {
    public static final byte PKT_LOADED = 1;
    public static final byte PKT_REPOP = 2;
    public byte packetType;
    public short repopEpoch;

    public void setRequestServerInfo() {
        this.packetType = 1;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.packetType = b.getByte();
        if (GameServer.server) {
            ByteBufferWriter bbw;
            if (this.packetType == 1) {
                MPDebugInfo.instance.requestTime = System.currentTimeMillis();
                MPDebugInfo.instance.requestPacketReceived = true;
                this.repopEpoch = b.getShort();
                bbw = connection.startPacket();
                PacketTypes.PacketType.ServerDebugInfo.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.ServerDebugInfo.send(connection);
                if (this.repopEpoch != MPDebugInfo.instance.repopEpoch) {
                    this.packetType = (byte)2;
                }
            }
            if (this.packetType == 2) {
                bbw = connection.startPacket();
                PacketTypes.PacketType.ServerDebugInfo.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.ServerDebugInfo.send(connection);
            }
        } else {
            int i;
            if (this.packetType == 1) {
                MPDebugInfo.instance.cellPool.release((List<MPDebugInfo.MPCell>)MPDebugInfo.instance.loadedCells);
                MPDebugInfo.instance.loadedCells.clear();
                int numCells = b.getShort();
                for (i = 0; i < numCells; ++i) {
                    MPDebugInfo.MPCell cell = MPDebugInfo.instance.cellPool.alloc();
                    cell.cx = b.getShort();
                    cell.cy = b.getShort();
                    cell.currentPopulation = b.getShort();
                    cell.desiredPopulation = b.getShort();
                    cell.lastRepopTime = b.getFloat();
                    MPDebugInfo.instance.loadedCells.add(cell);
                }
                MPDebugInfo.instance.loadedAreas.clear();
                int numAreas = b.getShort();
                for (int i2 = 0; i2 < numAreas; ++i2) {
                    short ax = b.getShort();
                    short ay = b.getShort();
                    short aw = b.getShort();
                    short ah = b.getShort();
                    MPDebugInfo.instance.loadedAreas.add(ax, ay, aw, ah);
                }
            }
            if (this.packetType == 2) {
                MPDebugInfo.instance.repopEventPool.release((List<MPDebugInfo.MPRepopEvent>)MPDebugInfo.instance.repopEvents);
                MPDebugInfo.instance.repopEvents.clear();
                MPDebugInfo.instance.repopEpoch = b.getShort();
                int numEvents = b.getShort();
                for (i = 0; i < numEvents; ++i) {
                    MPDebugInfo.MPRepopEvent re = MPDebugInfo.instance.repopEventPool.alloc();
                    re.wx = b.getShort();
                    re.wy = b.getShort();
                    re.worldAge = b.getFloat();
                    MPDebugInfo.instance.repopEvents.add(re);
                }
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.packetType);
        if (GameClient.client) {
            if (this.packetType == 1) {
                b.putShort(MPDebugInfo.instance.repopEpoch);
            }
            if (this.packetType == 2) {
                // empty if block
            }
        } else {
            int i;
            if (this.packetType == 1) {
                b.putShort(MPDebugInfo.instance.loadedCells.size());
                for (i = 0; i < MPDebugInfo.instance.loadedCells.size(); ++i) {
                    MPDebugInfo.MPCell cell = MPDebugInfo.instance.loadedCells.get(i);
                    b.putShort(cell.cx);
                    b.putShort(cell.cy);
                    b.putShort(cell.currentPopulation);
                    b.putShort(cell.desiredPopulation);
                    b.putFloat(cell.lastRepopTime);
                }
                b.putShort(MPDebugInfo.instance.loadedAreas.count);
                for (i = 0; i < MPDebugInfo.instance.loadedAreas.count; ++i) {
                    int n = i * 4;
                    b.putShort(MPDebugInfo.instance.loadedAreas.areas[n++]);
                    b.putShort(MPDebugInfo.instance.loadedAreas.areas[n++]);
                    b.putShort(MPDebugInfo.instance.loadedAreas.areas[n++]);
                    b.putShort(MPDebugInfo.instance.loadedAreas.areas[n++]);
                }
            }
            if (this.packetType == 2) {
                b.putShort(MPDebugInfo.instance.repopEpoch);
                b.putShort(MPDebugInfo.instance.repopEvents.size());
                for (i = 0; i < MPDebugInfo.instance.repopEvents.size(); ++i) {
                    MPDebugInfo.MPRepopEvent evt = MPDebugInfo.instance.repopEvents.get(i);
                    b.putShort(evt.wx);
                    b.putShort(evt.wy);
                    b.putFloat(evt.worldAge);
                }
            }
        }
    }
}


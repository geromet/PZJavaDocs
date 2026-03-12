/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.PacketValidator;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=2, reliability=2, requiredCapability=Capability.ReadUserLog, handlingType=3)
public class RequestUserLogPacket
implements INetworkPacket {
    @JSONField
    String username;
    @JSONField
    ArrayList<Userlog> userLog = new ArrayList();
    @JSONField
    EnumMap<AntiCheat, Integer> suspiciousActivity = new EnumMap(AntiCheat.class);

    @Override
    public void setData(Object ... values2) {
        this.username = (String)values2[0];
        if (GameServer.server) {
            this.userLog = ServerWorldDatabase.instance.getUserlog(this.username);
            PacketValidator validator = (PacketValidator)values2[1];
            if (validator != null) {
                this.suspiciousActivity = validator.getCounters();
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.username);
        if (GameServer.server) {
            b.putInt(this.userLog.size());
            for (Userlog userlog : this.userLog) {
                userlog.write(b);
            }
            b.putInt(this.suspiciousActivity.size());
            for (Map.Entry entry : this.suspiciousActivity.entrySet()) {
                b.putUTF(((AntiCheat)((Object)entry.getKey())).name());
                b.putInt((Integer)entry.getValue());
            }
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.username = b.getUTF();
    }

    @Override
    public void parseClient(ByteBufferReader b, UdpConnection connection) {
        this.parse(b, connection);
        this.userLog.clear();
        int userLogSize = b.getInt();
        for (int i = 0; i < userLogSize; ++i) {
            this.userLog.add(new Userlog(b));
        }
        this.suspiciousActivity.clear();
        int suspiciousActivitySize = b.getInt();
        for (int i = 0; i < suspiciousActivitySize; ++i) {
            this.suspiciousActivity.put(AntiCheat.valueOf(b.getUTF()), b.getInt());
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (!connection.getRole().hasCapability(Capability.ReadUserLog)) {
            return;
        }
        if (this.suspiciousActivity.isEmpty()) {
            LuaEventManager.triggerEvent("OnReceiveUserlog", this.username, this.userLog, null);
        } else {
            KahluaTable tbl = LuaManager.platform.newTable();
            for (Map.Entry<AntiCheat, Integer> point : this.suspiciousActivity.entrySet()) {
                tbl.rawset(point.getKey().name(), (Object)point.getValue().toString());
            }
            LuaEventManager.triggerEvent("OnReceiveUserlog", this.username, this.userLog, tbl);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (connection.getRole().hasCapability(Capability.ReadUserLog)) {
            UdpConnection c;
            PacketValidator validator = null;
            IsoPlayer player = GameServer.getPlayerByUserName(this.username);
            if (player != null && (c = GameServer.getConnectionFromPlayer(player)) != null) {
                validator = c.getValidator();
            }
            INetworkPacket.send(connection, PacketTypes.PacketType.RequestUserLog, this.username, validator);
        }
    }
}


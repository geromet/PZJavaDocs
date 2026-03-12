/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.chat.ChatManager;
import zombie.core.Translator;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.gameStates.IngameState;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@PacketSetting(ordering=0, priority=0, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=6)
public class KickedPacket
implements INetworkPacket {
    @JSONField
    public String description;
    @JSONField
    public String reason;

    @Override
    public void setData(Object ... values2) {
        this.description = (String)values2[0];
        this.reason = (String)values2[1];
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.description);
        b.putUTF(this.reason);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.description = b.getUTF();
        this.reason = b.getUTF();
    }

    public String getMessage() {
        Object message = Translator.getText(this.description);
        if (!StringUtils.isNullOrEmpty(this.reason)) {
            message = (String)message + " ";
            String text = Translator.getTextOrNull(this.reason);
            message = text != null ? (String)message + text : (String)message + this.reason;
        }
        return message;
    }

    @Override
    public void processClient(UdpConnection connection) {
        String message = this.getMessage();
        if (GameWindow.states.current == IngameState.instance) {
            if (!StringUtils.isNullOrEmpty(message)) {
                ChatManager.getInstance().showServerChatMessage(message);
            }
        } else {
            LuaEventManager.triggerEvent("OnConnectFailed", message);
        }
        connection.setUserName(null);
        GameWindow.kickReason = message;
        GameWindow.serverDisconnected = true;
        connection.forceDisconnect(message);
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        this.processClient(connection);
    }
}


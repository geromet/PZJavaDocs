/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.io.IOException;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.MovingObject;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=1, priority=2, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class ObjectModDataPacket
implements INetworkPacket {
    @JSONField
    protected final MovingObject movingObject = new MovingObject();

    @Override
    public void setData(Object ... values2) {
        this.movingObject.set((IsoObject)values2[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.movingObject.write(b);
        if (b.putBoolean(!this.movingObject.getObject().getModData().isEmpty())) {
            try {
                this.movingObject.getObject().getModData().save(b.bb);
            }
            catch (IOException e) {
                DebugLog.Multiplayer.printException(e, "ObjectModDataPacket write error", LogSeverity.Error);
            }
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.movingObject.parse(b, connection);
        boolean hasData = b.getBoolean();
        IsoObject object = this.movingObject.getObject();
        if (object == null) {
            DebugLog.Multiplayer.warn("ObjectModDataPacket.parse: object is null (%s)", this.movingObject.getDescription());
            return;
        }
        if (hasData) {
            int waterAmount = (int)object.getFluidAmount();
            try {
                object.getModData().load(b.bb, 244);
            }
            catch (IOException e) {
                DebugLog.Multiplayer.printException(e, "ObjectModDataPacket parse error", LogSeverity.Error);
                return;
            }
            if ((float)waterAmount != object.getFluidAmount()) {
                LuaEventManager.triggerEvent("OnWaterAmountChange", object, waterAmount);
            }
        } else if (object.hasModData()) {
            object.getModData().wipe();
        }
        IsoObject isoObject = this.movingObject.getObject();
        if (isoObject instanceof IsoAnimal) {
            IsoAnimal isoAnimal = (IsoAnimal)isoObject;
            if (isoAnimal.isOnHook()) {
                isoAnimal.getHook().onReceivedNetUpdate();
            }
        } else {
            isoObject = this.movingObject.getObject();
            if (isoObject instanceof IsoDeadBody) {
                IsoDeadBody isoDeadBody = (IsoDeadBody)isoObject;
                isoDeadBody.invalidateCorpse();
            }
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.movingObject.isConsistent(connection);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.sendToRelativeClients(PacketTypes.PacketType.ObjectModData, connection, this.movingObject.getObject().getX(), this.movingObject.getObject().getY());
    }
}


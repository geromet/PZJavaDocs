/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.world;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.Square;
import zombie.network.packets.INetworkPacket;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.util.StringUtils;

@PacketSetting(ordering=0, priority=2, reliability=2, requiredCapability=Capability.CreateStory, handlingType=1)
public class DebugStoryPacket
implements INetworkPacket {
    @JSONField
    protected final Square square = new Square();
    @JSONField
    protected int type;
    @JSONField
    protected String name;

    @Override
    public void setData(Object ... values2) {
        this.square.set((IsoGridSquare)values2[0]);
        this.type = (Integer)values2[1];
        this.name = (String)values2[2];
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.square.write(b);
        b.putInt(this.type);
        b.putUTF(this.name);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.square.parse(b, connection);
        this.type = b.getInt();
        this.name = b.getUTF();
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.square.isConsistent(connection) && !StringUtils.isNullOrEmpty(this.name);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        RandomizedZoneStoryBase rzs;
        if (this.type == 0) {
            RandomizedVehicleStoryBase rvs = IsoWorld.instance.getRandomizedVehicleStoryByName(this.name);
            if (rvs != null) {
                rvs.randomizeVehicleStory(this.square.getSquare().getZone(), this.square.getSquare().getChunk());
            }
        } else if (this.type == 1 && (rzs = IsoWorld.instance.getRandomizedZoneStoryByName(this.name)) != null) {
            Zone zone = new Zone("debugstoryzone", "debugstoryzone", this.square.getSquare().getX() - 20, this.square.getSquare().getY() - 20, this.square.getSquare().getZ(), this.square.getSquare().getX() + 20, this.square.getSquare().getX() + 20);
            zone.setPickedXForZoneStory(this.square.getSquare().getX());
            zone.setPickedYForZoneStory(this.square.getSquare().getY());
            zone.setX(this.square.getSquare().getX() - rzs.getMinimumWidth() / 2);
            zone.setY(this.square.getSquare().getY() - rzs.getMinimumHeight() / 2);
            zone.setW(rzs.getMinimumWidth() + 2);
            zone.setH(rzs.getMinimumHeight() + 2);
            rzs.randomizeZoneStory(zone);
        }
    }
}


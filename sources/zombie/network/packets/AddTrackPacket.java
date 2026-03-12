/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.io.IOException;
import zombie.characters.Capability;
import zombie.characters.animals.AnimalTracks;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoAnimalTrack;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class AddTrackPacket
implements INetworkPacket {
    int x;
    int y;
    byte z;
    AnimalTracks tracks;

    @Override
    public void setData(Object ... values2) {
        if (values2.length == 2) {
            this.set((IsoGridSquare)values2[0], (AnimalTracks)values2[1]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(IsoGridSquare sq, AnimalTracks tracks) {
        this.x = sq.getX();
        this.y = sq.getY();
        this.z = (byte)sq.getZ();
        this.tracks = tracks;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte(this.z);
        try {
            this.tracks.save(b.bb);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.getByte();
        this.tracks = new AnimalTracks();
        try {
            this.tracks.load(b.bb, 0);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        if (sq != null) {
            new IsoAnimalTrack(sq, this.tracks.getTrackSprite(), this.tracks);
        }
    }
}


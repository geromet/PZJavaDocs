/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.sound;

import zombie.GameSounds;
import zombie.SoundManager;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoTrap;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class PlayWorldSoundPacket
implements INetworkPacket {
    String name;
    int x;
    int y;
    byte z;
    int index;

    public void set(String name, int x, int y, byte z, int index) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.index = index;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!this.isConsistent(connection)) {
            return;
        }
        int radius = 70;
        GameSound gameSound = GameSounds.getSound(this.getName());
        if (gameSound != null) {
            for (int i = 0; i < gameSound.clips.size(); ++i) {
                GameSoundClip clip = gameSound.clips.get(i);
                if (!clip.hasMaxDistance()) continue;
                radius = Math.max(radius, (int)clip.distanceMax);
            }
        }
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            IsoPlayer p;
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID() || !c.isFullyConnected() || (p = GameServer.getAnyPlayerFromConnection(c)) == null || !c.RelevantTo(this.getX(), this.getY(), radius)) continue;
            ByteBufferWriter b2 = c.startPacket();
            PacketTypes.PacketType.PlayWorldSound.doPacket(b2);
            this.write(b2);
            PacketTypes.PacketType.PlayWorldSound.send(c);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.index == -1) {
            SoundManager.instance.PlayWorldSoundImpl(this.name, false, this.x, this.y, this.z, 1.0f, 20.0f, 2.0f, false);
        } else {
            IsoObject isoObject;
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
            if (square != null && (isoObject = square.getObjects().get(this.index)) instanceof IsoTrap) {
                IsoTrap trap = (IsoTrap)isoObject;
                trap.playExplosionSound();
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.getByte();
        this.name = b.getUTF();
        this.index = b.getInt();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte(this.z);
        b.putUTF(this.name);
        b.putInt(this.index);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.name != null && !this.name.isEmpty();
    }

    @Override
    public int getPacketSizeBytes() {
        return 12 + this.name.length();
    }

    @Override
    public String getDescription() {
        return "\n\tPlayWorldSoundPacket [name=" + this.name + " | x=" + this.x + " | y=" + this.y + " | z=" + this.z + " ]";
    }
}


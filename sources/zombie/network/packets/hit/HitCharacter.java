/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.hit;

import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public interface HitCharacter
extends INetworkPacket {
    public boolean isRelevant(UdpConnection var1);

    default public void attack() {
    }

    default public void react() {
    }

    default public void update() {
    }

    public void preProcess();

    public void process();

    public void postProcess();

    default public void log(UdpConnection connection) {
    }

    @Override
    default public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.log(connection);
        this.update();
        GameServer.sendHitCharacter(this, packetType, connection);
        this.processClient(connection);
    }

    @Override
    default public void processClient(UdpConnection connection) {
        this.preProcess();
        this.process();
        this.postProcess();
        this.attack();
        this.react();
    }
}


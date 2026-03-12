/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.character;

import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.ServerMap;
import zombie.network.fields.IDShort;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.IPositional;

public class ZombieID
extends IDShort
implements INetworkPacketField,
IPositional {
    protected IsoZombie zombie;

    public void set(IsoZombie zombie) {
        this.setID(zombie.getOnlineID());
        this.zombie = zombie;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        if (GameServer.server) {
            this.zombie = ServerMap.instance.zombieMap.get(this.getID());
        } else if (GameClient.client) {
            this.zombie = GameClient.IDToZombieMap.get(this.getID());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return super.isConsistent(connection) && this.getZombie() != null;
    }

    @Override
    public float getX() {
        if (this.zombie != null) {
            return this.zombie.getX();
        }
        return 0.0f;
    }

    @Override
    public float getY() {
        if (this.zombie != null) {
            return this.zombie.getY();
        }
        return 0.0f;
    }

    @Override
    public float getZ() {
        if (this.zombie != null) {
            return this.zombie.getZ();
        }
        return 0.0f;
    }

    public IsoZombie getZombie() {
        return this.zombie;
    }
}


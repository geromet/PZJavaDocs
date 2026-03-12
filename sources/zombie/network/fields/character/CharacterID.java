/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields.character;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.IPositional;
import zombie.network.fields.character.AnimalID;
import zombie.network.fields.character.PlayerID;
import zombie.network.fields.character.ZombieID;

public class CharacterID
implements INetworkPacketField,
IPositional {
    public static final byte UNKNOWN = 0;
    public static final byte PLAYER = 1;
    public static final byte ZOMBIE = 2;
    public static final byte ANIMAL = 3;
    @JSONField
    private final PlayerID playerId = new PlayerID();
    @JSONField
    private final ZombieID zombieId = new ZombieID();
    @JSONField
    private final AnimalID animalId = new AnimalID();
    @JSONField
    private byte type = 0;

    public void set(short id, byte type) {
        this.type = type;
        switch (type) {
            case 3: {
                this.animalId.setID(id);
                break;
            }
            case 1: {
                this.playerId.setID(id);
                break;
            }
            case 2: {
                this.zombieId.setID(id);
            }
        }
    }

    public void set(IsoGameCharacter character) {
        if (character instanceof IsoAnimal) {
            IsoAnimal isoAnimal = (IsoAnimal)character;
            this.type = (byte)3;
            this.animalId.set(isoAnimal);
        } else if (character instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)character;
            this.type = 1;
            this.playerId.set(isoPlayer);
        } else if (character instanceof IsoZombie) {
            IsoZombie isoZombie = (IsoZombie)character;
            this.type = (byte)2;
            this.zombieId.set(isoZombie);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.type = b.getByte();
        switch (this.type) {
            case 3: {
                this.animalId.parse(b, connection);
                break;
            }
            case 1: {
                this.playerId.parse(b, connection);
                break;
            }
            case 2: {
                this.zombieId.parse(b, connection);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.write(b.bb);
    }

    public void write(ByteBuffer b) {
        b.put(this.type);
        switch (this.type) {
            case 3: {
                this.animalId.write(b);
                break;
            }
            case 1: {
                this.playerId.write(b);
                break;
            }
            case 2: {
                this.zombieId.write(b);
            }
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        boolean isConsistent = false;
        switch (this.type) {
            case 3: {
                isConsistent = this.animalId.isConsistent(connection);
                break;
            }
            case 1: {
                isConsistent = this.playerId.isConsistent(connection);
                break;
            }
            case 2: {
                isConsistent = this.zombieId.isConsistent(connection);
            }
        }
        return isConsistent;
    }

    @Override
    public float getX() {
        switch (this.type) {
            case 3: {
                this.animalId.getX();
                break;
            }
            case 1: {
                this.playerId.getX();
                break;
            }
            case 2: {
                this.zombieId.getX();
            }
        }
        return 0.0f;
    }

    @Override
    public float getY() {
        switch (this.type) {
            case 3: {
                this.animalId.getY();
                break;
            }
            case 1: {
                this.playerId.getY();
                break;
            }
            case 2: {
                this.zombieId.getY();
            }
        }
        return 0.0f;
    }

    @Override
    public float getZ() {
        switch (this.type) {
            case 3: {
                this.animalId.getZ();
                break;
            }
            case 1: {
                this.playerId.getZ();
                break;
            }
            case 2: {
                this.zombieId.getZ();
            }
        }
        return 0.0f;
    }

    public IsoGameCharacter getCharacter() {
        IsoGameCharacter character = null;
        switch (this.type) {
            case 3: {
                character = this.animalId.getAnimal();
                break;
            }
            case 1: {
                character = this.playerId.getPlayer();
                break;
            }
            case 2: {
                character = this.zombieId.getZombie();
            }
        }
        return character;
    }

    public short getID() {
        short id = -1;
        switch (this.type) {
            case 3: {
                id = this.animalId.getID();
                break;
            }
            case 1: {
                id = this.playerId.getID();
                break;
            }
            case 2: {
                id = this.zombieId.getID();
            }
        }
        return id;
    }
}


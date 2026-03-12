/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.core.Transaction;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.character.PlayerID;
import zombie.network.server.AnimEventEmulator;

abstract class Action
implements INetworkPacketField {
    protected static int timeoutForInfinitiveActions = 1000000;
    protected static byte lastId;
    @JSONField
    protected byte id;
    @JSONField
    protected Transaction.TransactionState state;
    @JSONField
    PlayerID playerId = new PlayerID();
    @JSONField
    public long duration;
    protected long startTime;
    protected long endTime;

    Action() {
    }

    public void setTimeData() {
        this.startTime = GameTime.getServerTimeMills();
        this.duration = (long)this.getDuration();
        this.endTime = this.duration < 0L ? this.startTime + AnimEventEmulator.getInstance().getDurationMax() : this.startTime + this.duration;
    }

    public void set(IsoPlayer player) {
        this.state = Transaction.TransactionState.Request;
        if (lastId == 0) {
            lastId = (byte)(lastId + 1);
        }
        byte by = lastId;
        lastId = (byte)(by + 1);
        this.id = by;
        this.playerId.set(player);
        this.setTimeData();
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.id = b.getByte();
        this.state = b.getEnum(Transaction.TransactionState.class);
        if (this.state == Transaction.TransactionState.Request || this.state == Transaction.TransactionState.Reject) {
            this.playerId.parse(b, connection);
        }
        if (this.state == Transaction.TransactionState.Accept) {
            this.duration = b.getLong();
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.id);
        b.putEnum(this.state);
        if (this.state == Transaction.TransactionState.Request || this.state == Transaction.TransactionState.Reject) {
            this.playerId.write(b);
        }
        if (this.state == Transaction.TransactionState.Accept) {
            b.putLong(this.duration);
        }
    }

    public void setState(Transaction.TransactionState state) {
        this.state = state;
    }

    public void setDuration(long duration) {
        this.endTime = this.startTime + duration;
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (this.state == Transaction.TransactionState.Request) {
            return this.playerId.isConsistent(connection);
        }
        return true;
    }

    public float getProgress() {
        if (this.endTime == this.startTime) {
            return 1.0f;
        }
        return (float)(GameTime.getServerTimeMills() - this.startTime) / (float)(this.endTime - this.startTime);
    }

    abstract float getDuration();

    abstract void start();

    abstract void stop();

    abstract boolean isValid();

    abstract void update();

    abstract boolean perform();

    abstract boolean isUsingTimeout();
}


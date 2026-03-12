/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.core.Transaction;
import zombie.core.raknet.UdpConnection;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;

public class AntiCheatTransaction
extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        Transaction.TransactionState state = field.getState();
        if (Transaction.TransactionState.Accept == state || Transaction.TransactionState.Done == state) {
            return "invalid state";
        }
        if (Transaction.TransactionState.Request == state && field.getConsistent() == -1) {
            return "invalid request";
        }
        return result;
    }

    public static interface IAntiCheat {
        public Transaction.TransactionState getState();

        public byte getConsistent();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.savefile.ServerPlayerDB;

public class TradingManager {
    private static TradingManager instance;
    final ArrayList<Trading> tradings = new ArrayList();
    final ArrayList<Trading> finalisingTradings = new ArrayList();
    final ArrayList<Trading> finalisedTradings = new ArrayList();

    public static TradingManager getInstance() {
        if (instance == null) {
            instance = new TradingManager();
        }
        return instance;
    }

    public void addNewTrading(IsoPlayer playerA, IsoPlayer playerB) {
        this.tradings.add(new Trading(this, playerA, playerB));
    }

    public void addItem(IsoPlayer player, InventoryItem item) {
        for (Trading t : this.tradings) {
            if (t.playerA == player) {
                t.playerAItems.add(item);
                break;
            }
            if (t.playerB != player) continue;
            t.playerBItems.add(item);
            break;
        }
    }

    public void removeItem(IsoPlayer player, int itemId) {
        for (Trading t : this.tradings) {
            if (t.playerA == player) {
                t.playerAItems.removeIf(i -> i.getID() == itemId);
                break;
            }
            if (t.playerB != player) continue;
            t.playerAItems.removeIf(i -> i.getID() == itemId);
            break;
        }
    }

    public void dealSealStatusChanged(IsoPlayer player, boolean isSealed) {
        for (Trading t : this.tradings) {
            if (t.playerA == player) {
                t.playerAAgreement = isSealed;
                break;
            }
            if (t.playerB != player) continue;
            t.playerBAgreement = isSealed;
            break;
        }
    }

    public void cancelTrading(IsoPlayer player) {
        this.tradings.removeIf(t -> t.playerA == player || t.playerB == player);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void finishTrading(IsoPlayer player) {
        Optional<Trading> trading = this.tradings.stream().filter(t -> t.playerA == player || t.playerB == player).findFirst();
        if (trading.isPresent()) {
            ArrayList<Trading> arrayList = this.finalisingTradings;
            synchronized (arrayList) {
                this.finalisingTradings.add(trading.get());
            }
            this.tradings.remove(trading.get());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void update() {
        for (Trading trading : this.finalisingTradings) {
            trading.time = System.currentTimeMillis();
            UdpConnection connectionA = GameServer.getConnectionFromPlayer(trading.playerA);
            byte playerAIndex = connectionA.getPlayerIndex(trading.playerA);
            UdpConnection connectionB = GameServer.getConnectionFromPlayer(trading.playerB);
            byte playerBIndex = connectionB.getPlayerIndex(trading.playerB);
            if (connectionA == null || !connectionA.isFullyConnected() || connectionB == null || !connectionB.isFullyConnected() || playerAIndex == -1 || playerBIndex == -1) {
                this.finalisedTradings.add(trading);
                continue;
            }
            trading.time = System.currentTimeMillis();
            for (InventoryItem item : trading.playerAItems) {
                trading.playerA.getInventory().Remove(item);
            }
            INetworkPacket.send(connectionA, PacketTypes.PacketType.RemoveInventoryItemFromContainer, trading.playerA.getInventory(), trading.playerAItems);
            trading.time = System.currentTimeMillis();
            for (InventoryItem item : trading.playerBItems) {
                trading.playerB.getInventory().Remove(item);
            }
            INetworkPacket.send(connectionB, PacketTypes.PacketType.RemoveInventoryItemFromContainer, trading.playerB.getInventory(), trading.playerBItems);
            trading.time = System.currentTimeMillis();
            for (InventoryItem item : trading.playerBItems) {
                trading.playerA.getInventory().AddItem(item);
            }
            INetworkPacket.send(connectionA, PacketTypes.PacketType.AddInventoryItemToContainer, trading.playerA.getInventory(), trading.playerBItems);
            trading.time = System.currentTimeMillis();
            for (InventoryItem item : trading.playerAItems) {
                trading.playerB.getInventory().AddItem(item);
            }
            INetworkPacket.send(connectionB, PacketTypes.PacketType.AddInventoryItemToContainer, trading.playerB.getInventory(), trading.playerAItems);
            ServerPlayerDB.getInstance().serverUpdateNetworkCharacter(trading.playerA, playerAIndex, connectionA);
            ServerPlayerDB.getInstance().serverUpdateNetworkCharacter(trading.playerB, playerBIndex, connectionB);
            this.finalisedTradings.add(trading);
        }
        if (!this.finalisedTradings.isEmpty()) {
            ArrayList<Trading> arrayList = this.finalisingTradings;
            synchronized (arrayList) {
                this.finalisingTradings.removeAll(this.finalisedTradings);
            }
        }
    }

    private class Trading {
        long time;
        IsoPlayer playerA;
        IsoPlayer playerB;
        ArrayList<InventoryItem> playerAItems;
        ArrayList<InventoryItem> playerBItems;
        boolean playerAAgreement;
        boolean playerBAgreement;

        public Trading(TradingManager tradingManager, IsoPlayer playerA, IsoPlayer playerB) {
            Objects.requireNonNull(tradingManager);
            this.time = System.currentTimeMillis();
            this.playerA = playerA;
            this.playerB = playerB;
            this.playerAItems = new ArrayList();
            this.playerBItems = new ArrayList();
            this.playerAAgreement = false;
            this.playerBAgreement = false;
        }
    }
}


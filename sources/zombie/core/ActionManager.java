/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.Action;
import zombie.core.BuildAction;
import zombie.core.NetTimedAction;
import zombie.core.Transaction;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.BuildActionPacket;
import zombie.network.packets.FishingActionPacket;
import zombie.network.packets.GeneralActionPacket;
import zombie.network.packets.NetTimedActionPacket;
import zombie.network.server.AnimEventEmulator;

public class ActionManager {
    private static ActionManager instance;
    private static final ConcurrentLinkedQueue<Action> actions;

    public static ActionManager getInstance() {
        if (instance == null) {
            instance = new ActionManager();
        }
        return instance;
    }

    public static void start(Action action) {
        DebugLog.Action.debugln("ActionManager start action %s", action.getDescription());
        action.start();
        ActionManager.add(action);
    }

    public static void add(Action action) {
        DebugLog.Action.debugln("ActionManager add action %s", action.getDescription());
        actions.add(action);
    }

    public static void stop(Action action) {
        DebugLog.Action.debugln("ActionManager stop action %s", action.getDescription());
        ActionManager.remove(action.id, true);
    }

    public static void update() {
        block7: {
            block6: {
                if (!GameServer.server) break block6;
                for (Action action : actions) {
                    if (action.state == Transaction.TransactionState.Accept && action.endTime <= GameTime.getServerTimeMills()) {
                        ByteBufferWriter bbw2;
                        UdpConnection connection;
                        DebugLog.Action.debugln("ActionManager complete %s", action.getDescription());
                        if (action.perform()) {
                            action.state = Transaction.TransactionState.Done;
                            connection = GameServer.getConnectionFromPlayer(action.playerId.getPlayer());
                            if (connection == null || !connection.isFullyConnected()) continue;
                            if (action instanceof BuildAction) {
                                bbw2 = connection.startPacket();
                                PacketTypes.PacketType.BuildAction.doPacket(bbw2);
                                action.write(bbw2);
                                PacketTypes.PacketType.BuildAction.send(connection);
                            }
                            if (!(action instanceof NetTimedAction)) continue;
                            bbw2 = connection.startPacket();
                            PacketTypes.PacketType.NetTimedAction.doPacket(bbw2);
                            action.write(bbw2);
                            PacketTypes.PacketType.NetTimedAction.send(connection);
                            continue;
                        }
                        action.state = Transaction.TransactionState.Reject;
                        DebugLog.Action.noise("ActionManager reject %s", action.getDescription());
                        connection = GameServer.getConnectionFromPlayer(action.playerId.getPlayer());
                        if (connection == null || !connection.isFullyConnected() || !(action instanceof NetTimedAction)) continue;
                        bbw2 = connection.startPacket();
                        PacketTypes.PacketType.NetTimedAction.doPacket(bbw2);
                        action.write(bbw2);
                        PacketTypes.PacketType.NetTimedAction.send(connection);
                        continue;
                    }
                    action.update();
                }
                List transactionForDelete = actions.stream().filter(r -> r.state == Transaction.TransactionState.Done || r.state == Transaction.TransactionState.Reject).collect(Collectors.toList());
                actions.removeAll(transactionForDelete);
                for (Action action : transactionForDelete) {
                    DebugLog.Action.debugln("ActionManager clear action %s", action.getDescription());
                    if (!(action instanceof NetTimedAction)) continue;
                    NetTimedAction netTimedAction = (NetTimedAction)action;
                    AnimEventEmulator.getInstance().remove(netTimedAction);
                }
                break block7;
            }
            if (!GameClient.client) break block7;
            actions.forEach(Action::update);
            List transactionForDelete = actions.stream().filter(t -> t.isUsingTimeout() && GameTime.getServerTimeMills() > t.startTime + AnimEventEmulator.getInstance().getDurationMax()).collect(Collectors.toList());
            actions.removeAll(transactionForDelete);
            for (Action action : transactionForDelete) {
                DebugLog.Action.debugln("ActionManager clear action %s", action.getDescription());
            }
        }
    }

    public static boolean isRejected(byte id) {
        return !actions.isEmpty() && (actions.stream().filter(r -> id == r.id).allMatch(r -> r.state == Transaction.TransactionState.Reject) || actions.stream().noneMatch(r -> id == r.id));
    }

    public static boolean isDone(byte id) {
        return !actions.isEmpty() && actions.stream().filter(r -> id == r.id).allMatch(r -> r.state == Transaction.TransactionState.Done);
    }

    public static boolean isLooped(byte id) {
        Optional<Action> res = actions.stream().filter(r -> id == r.id).findFirst();
        if (res.isEmpty()) {
            return false;
        }
        Action t = res.get();
        return t.duration == -1L;
    }

    public static int getDuration(byte id) {
        Optional<Action> res = actions.stream().filter(r -> id == r.id).findFirst();
        if (res.isEmpty()) {
            return -1;
        }
        Action t = res.get();
        return (int)(t.endTime - t.startTime);
    }

    public static IsoPlayer getPlayer(byte id) {
        Optional<Action> res = actions.stream().filter(r -> id == r.id).findFirst();
        if (res.isEmpty()) {
            return null;
        }
        Action t = res.get();
        return t.playerId.getPlayer();
    }

    public static void remove(byte id, boolean isCanceled) {
        block5: {
            block4: {
                if (!GameClient.client) break block4;
                if (id == 0) break block5;
                if (isCanceled) {
                    GeneralActionPacket generalAction = new GeneralActionPacket();
                    generalAction.setReject(id);
                    ByteBufferWriter bbw2 = GameClient.connection.startPacket();
                    PacketTypes.PacketType.GeneralAction.doPacket(bbw2);
                    generalAction.write(bbw2);
                    PacketTypes.PacketType.GeneralAction.send(GameClient.connection);
                }
                List transactionForDelete = actions.stream().filter(t -> t.id == id).collect(Collectors.toList());
                actions.removeAll(transactionForDelete);
                for (Action action : transactionForDelete) {
                    DebugLog.Action.debugln("ActionManager remove action %s", action.getDescription());
                }
                break block5;
            }
            if (GameServer.server) {
                List transactionForDelete = actions.stream().filter(t -> t.id == id).collect(Collectors.toList());
                actions.removeAll(transactionForDelete);
                for (Action action : transactionForDelete) {
                    DebugLog.Action.debugln("ActionManager remove action %s", action.getDescription());
                    action.stop();
                    if (!(action instanceof NetTimedAction)) continue;
                    NetTimedAction netTimedAction = (NetTimedAction)action;
                    AnimEventEmulator.getInstance().remove(netTimedAction);
                }
            }
        }
    }

    private byte sendAction(Action action, PacketTypes.PacketType packetType) {
        if (action.isConsistent(GameClient.connection)) {
            ActionManager.add(action);
            try {
                ByteBufferWriter bbw2 = GameClient.connection.startPacket();
                packetType.doPacket(bbw2);
                action.write(bbw2);
                packetType.send(GameClient.connection);
                DebugLog.Action.noise("ActionManager send %s", action.getDescription());
                return action.id;
            }
            catch (Exception e) {
                GameClient.connection.cancelPacket();
                DebugLog.Multiplayer.printException(e, "SendAction: failed", LogSeverity.Error);
                return 0;
            }
        }
        DebugLog.Action.error("ActionManager send FAIL %s", action.getDescription());
        return 0;
    }

    public byte createNetTimedAction(IsoPlayer player, KahluaTable actionTable) {
        NetTimedActionPacket action = new NetTimedActionPacket();
        action.set(player, actionTable);
        return this.sendAction(action, PacketTypes.PacketType.NetTimedAction);
    }

    public byte createBuildAction(IsoPlayer player, float x, float y, float z, boolean north, String spriteName, KahluaTable item) {
        BuildActionPacket action = new BuildActionPacket();
        action.set(player, x, y, z, north, spriteName, item);
        return this.sendAction(action, PacketTypes.PacketType.BuildAction);
    }

    public byte createFishingAction(IsoPlayer player, InventoryItem item, IsoGridSquare sq, KahluaTable bobber) {
        FishingActionPacket action = new FishingActionPacket();
        action.setStartFishing(player, item, sq, bobber);
        return this.sendAction(action, PacketTypes.PacketType.FishingAction);
    }

    public void setStateFromPacket(Action packet) {
        for (Action action : actions) {
            if (packet.id != action.id) continue;
            action.setState(packet.state);
            if (packet.state != Transaction.TransactionState.Accept) break;
            action.setDuration(packet.duration);
            break;
        }
    }

    public void disconnectPlayer(UdpConnection connection) {
        for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
            IsoPlayer player = connection.players[playerIndex];
            if (player == null) continue;
            for (Action action : actions) {
                if (action.playerId.getID() != player.getOnlineID()) continue;
                action.state = Transaction.TransactionState.Reject;
            }
        }
    }

    public void replaceObjectInQueuedActions(IsoPlayer player, Object oldItem, Object newItem) {
        KahluaTable table = (KahluaTable)LuaManager.env.rawget("ISTimedActionQueue");
        KahluaTable queues = (KahluaTable)table.rawget("queues");
        KahluaTable queue = (KahluaTable)queues.rawget(player);
        if (queue != null) {
            KahluaTableIterator queueIt = queue.iterator();
            while (queueIt.advance()) {
                if (!queueIt.getKey().equals("queue")) continue;
                KahluaTable actions = (KahluaTable)queueIt.getValue();
                KahluaTableIterator actionsIt = actions.iterator();
                while (actionsIt.advance()) {
                    KahluaTable action = (KahluaTable)actionsIt.getValue();
                    KahluaTableIterator actionIt = action.iterator();
                    while (actionIt.advance()) {
                        if (oldItem == null && this.isItemWithSameID(actionIt.getValue(), newItem)) {
                            action.rawset(actionIt.getKey(), newItem);
                        }
                        if (oldItem == null || actionIt.getValue() != oldItem) continue;
                        action.rawset(actionIt.getKey(), newItem);
                    }
                }
            }
        }
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private boolean isItemWithSameID(Object actionObject, Object newObject) {
        if (!(actionObject instanceof InventoryItem)) return false;
        InventoryItem actionItem = (InventoryItem)actionObject;
        if (!(newObject instanceof InventoryItem)) return false;
        InventoryItem newItem = (InventoryItem)newObject;
        if (actionItem.getID() != newItem.getID()) return false;
        return true;
    }

    static {
        actions = new ConcurrentLinkedQueue();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.popman.animal;

import java.util.ArrayList;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.IsoObjectID;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.popman.animal.AnimalSynchronizationManager;
import zombie.util.Type;

public class AnimalInstanceManager {
    private static final AnimalInstanceManager instance = new AnimalInstanceManager();
    private static final Vector2 movement = new Vector2();
    private static final IsoObjectID<IsoAnimal> AnimalMap = new IsoObjectID<IsoAnimal>(IsoAnimal.class);

    public static AnimalInstanceManager getInstance() {
        return instance;
    }

    public short allocateID() {
        return AnimalMap.allocateID();
    }

    public void add(IsoAnimal animal, short onlineID) {
        DebugLog.Animal.noise("Animal add id=%d", onlineID);
        AnimalMap.remove(animal);
        animal.setOnlineID(onlineID);
        AnimalMap.put(onlineID, animal);
    }

    public void remove(IsoAnimal animal) {
        DebugLog.Animal.noise("Animal remove id=%d", animal.getOnlineID());
        AnimalMap.remove(animal.getOnlineID());
    }

    public IsoAnimal get(short id) {
        return AnimalMap.get(id);
    }

    public IsoObjectID<IsoAnimal> getAnimals() {
        return AnimalMap;
    }

    private void updateLocal(IsoAnimal animal) {
        AnimalSynchronizationManager.getInstance().setSendToClients(animal.getOnlineID());
    }

    private void updateRemote(IsoAnimal animal) {
    }

    public void update(IsoAnimal animal) {
        if (animal.isLocalPlayer()) {
            this.updateLocal(animal);
        } else {
            this.updateRemote(animal);
        }
    }

    public static void removeAnimals(UdpConnection connection) {
        int radius = (IsoChunkMap.chunkGridWidth / 2 + 2) * 8;
        for (IsoPlayer player : connection.players) {
            if (player == null) continue;
            int x = (int)player.getX();
            int y = (int)player.getY();
            ArrayList<IsoAnimal> removed = new ArrayList<IsoAnimal>();
            for (IsoAnimal animal : AnimalInstanceManager.getInstance().getAnimals()) {
                if (animal == null) continue;
                removed.add(animal);
            }
            removed.forEach(IsoAnimal::remove);
            for (int z = 0; z < 64; ++z) {
                for (int y1 = y - radius; y1 <= y + radius; ++y1) {
                    for (int x1 = x - radius; x1 <= x + radius; ++x1) {
                        int i;
                        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x1, y1, z);
                        if (square == null) continue;
                        for (i = square.getStaticMovingObjects().size() - 1; i >= 0; --i) {
                            IsoDeadBody deadBody = Type.tryCastTo(square.getStaticMovingObjects().get(i), IsoDeadBody.class);
                            if (deadBody == null || !deadBody.isAnimal()) continue;
                            INetworkPacket.sendToAll(PacketTypes.PacketType.RemoveCorpseFromMap, deadBody);
                            deadBody.removeFromWorld();
                            deadBody.removeFromSquare();
                        }
                        for (i = square.getMovingObjects().size() - 1; i >= 0; --i) {
                            IsoMovingObject isoMovingObject = square.getMovingObjects().get(i);
                            if (!(isoMovingObject instanceof IsoAnimal)) continue;
                            IsoAnimal animal = (IsoAnimal)isoMovingObject;
                            animal.remove();
                        }
                    }
                }
            }
        }
    }

    public void stop() {
        ArrayList<IsoAnimal> removed = new ArrayList<IsoAnimal>();
        for (IsoAnimal animal : AnimalInstanceManager.getInstance().getAnimals()) {
            removed.add(animal);
        }
        removed.forEach(IsoAnimal::remove);
    }
}


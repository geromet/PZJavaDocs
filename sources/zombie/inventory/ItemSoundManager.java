/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.SoundInstanceLimiter;
import zombie.audio.SoundLimiterParams;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;

public final class ItemSoundManager {
    private static final ArrayList<InventoryItem> items = new ArrayList();
    private static final ArrayList<BaseSoundEmitter> emitters = new ArrayList();
    private static final ArrayList<InventoryItem> toAdd = new ArrayList();
    private static final ArrayList<InventoryItem> toRemove = new ArrayList();
    private static final ArrayList<InventoryItem> toStopItems = new ArrayList();
    private static final ArrayList<BaseSoundEmitter> toStopEmitters = new ArrayList();
    private static final SoundInstanceLimiter instanceLimiter = new SoundInstanceLimiter();
    private static final List<String> activeLimiterGroups = new ArrayList<String>();
    private static final Map<String, BaseSoundEmitter> limiterEmitters = new HashMap<String, BaseSoundEmitter>();
    private static final Map<String, SoundLimiterParams> limiterParams = new HashMap<String, SoundLimiterParams>();

    public static void addItem(InventoryItem item) {
        if (GameServer.server) {
            return;
        }
        if (item == null || items.contains(item)) {
            return;
        }
        toRemove.remove(item);
        int index = toStopItems.indexOf(item);
        if (index != -1) {
            toStopItems.remove(index);
            BaseSoundEmitter emitter = toStopEmitters.remove(index);
            items.add(item);
            emitters.add(emitter);
            return;
        }
        if (toAdd.contains(item)) {
            return;
        }
        toAdd.add(item);
    }

    public static void removeItem(InventoryItem item) {
        if (GameServer.server) {
            return;
        }
        toAdd.remove(item);
        int index = items.indexOf(item);
        if (item == null || index == -1) {
            return;
        }
        if (toRemove.contains(item)) {
            return;
        }
        toRemove.add(item);
    }

    public static void removeItems(ArrayList<InventoryItem> items) {
        for (int i = 0; i < items.size(); ++i) {
            ItemSoundManager.removeItem(items.get(i));
        }
    }

    public static void update() {
        SoundLimiterParams params;
        BaseSoundEmitter emitter;
        InventoryItem item;
        int i;
        if (!toStopItems.isEmpty()) {
            for (i = 0; i < toStopItems.size(); ++i) {
                item = toStopItems.get(i);
                item.stopSoundOnPlayer();
                emitter = toStopEmitters.get(i);
                emitter.stopAll();
                IsoWorld.instance.returnOwnershipOfEmitter(emitter);
            }
            toStopItems.clear();
            toStopEmitters.clear();
        }
        if (!toAdd.isEmpty()) {
            for (i = 0; i < toAdd.size(); ++i) {
                item = toAdd.get(i);
                assert (!items.contains(item));
                items.add(item);
                emitter = IsoWorld.instance.getFreeEmitter();
                IsoWorld.instance.takeOwnershipOfEmitter(emitter);
                emitters.add(emitter);
            }
            toAdd.clear();
        }
        if (!toRemove.isEmpty()) {
            for (i = 0; i < toRemove.size(); ++i) {
                item = toRemove.get(i);
                assert (items.contains(item));
                int index = items.indexOf(item);
                items.remove(index);
                BaseSoundEmitter emitter2 = emitters.get(index);
                emitters.remove(index);
                toStopItems.add(item);
                toStopEmitters.add(emitter2);
            }
            toRemove.clear();
        }
        instanceLimiter.startFrame();
        for (i = 0; i < items.size(); ++i) {
            item = items.get(i);
            BaseSoundEmitter emitter3 = emitters.get(i);
            ItemContainer container = ItemSoundManager.getExistingContainer(item);
            if (container == null && (item.getWorldItem() == null || item.getWorldItem().getWorldObjectIndex() == -1)) {
                ItemSoundManager.removeItem(item);
                continue;
            }
            if (item.getSoundLimiterGroupID() != null) {
                item.registerWithSoundLimiter(instanceLimiter);
                continue;
            }
            item.updateSound(emitter3);
            emitter3.tick();
        }
        instanceLimiter.getActiveGroups(activeLimiterGroups);
        for (i = 0; i < items.size(); ++i) {
            item = items.get(i);
            if (item.getSoundLimiterGroupID() == null || !instanceLimiter.isClosest(item, item.getSoundLimiterGroupID())) continue;
            BaseSoundEmitter emitter4 = ItemSoundManager.getEmitterForSoundLimiterGroup(item.getSoundLimiterGroupID());
            params = ItemSoundManager.getParamsForSoundLimiterGroup(item.getSoundLimiterGroupID());
            item.updateSound(emitter4, params);
            emitter4.tick();
        }
        Iterator<Map.Entry<String, BaseSoundEmitter>> it = limiterEmitters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, BaseSoundEmitter> entry = it.next();
            if (activeLimiterGroups.contains(entry.getKey())) continue;
            BaseSoundEmitter emitter5 = entry.getValue();
            emitter5.stopAll();
            SoundLimiterParams soundLimiterParams = limiterParams.get(entry.getKey());
            if (soundLimiterParams instanceof SoundLimiterParams) {
                params = soundLimiterParams;
                params.clear();
            }
            it.remove();
            IsoWorld.instance.returnOwnershipOfEmitter(emitter5);
        }
    }

    private static ItemContainer getExistingContainer(InventoryItem item) {
        ItemContainer container = item.getOutermostContainer();
        if (container != null) {
            if (container.containingItem != null && container.containingItem.getWorldItem() != null) {
                if (container.containingItem.getWorldItem().getWorldObjectIndex() == -1) {
                    container = null;
                }
            } else if (container.parent != null) {
                if (container.parent.getObjectIndex() == -1 && container.parent.getMovingObjectIndex() == -1 && container.parent.getStaticMovingObjectIndex() == -1) {
                    container = null;
                }
            } else {
                container = null;
            }
        }
        return container;
    }

    static BaseSoundEmitter getEmitterForSoundLimiterGroup(String groupID) {
        BaseSoundEmitter emitter = limiterEmitters.get(groupID);
        if (emitter == null) {
            emitter = IsoWorld.instance.getFreeEmitter();
            IsoWorld.instance.takeOwnershipOfEmitter(emitter);
            limiterEmitters.put(groupID, emitter);
        }
        return emitter;
    }

    static SoundLimiterParams getParamsForSoundLimiterGroup(String groupID) {
        return limiterParams.computeIfAbsent(groupID, s -> new SoundLimiterParams());
    }

    public static void Reset() {
        items.clear();
        emitters.clear();
        toAdd.clear();
        toRemove.clear();
        toStopItems.clear();
        toStopEmitters.clear();
    }
}


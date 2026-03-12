/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.id;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import zombie.ZomboidFileSystem;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.id.IIdentifiable;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDType;

public class ObjectIDManager {
    private static final ObjectIDManager instance = new ObjectIDManager();
    private static final int saveLastIDNumber = 100;
    private static int objectIdManagerCheckLimiter;

    public static ObjectIDManager getInstance() {
        return instance;
    }

    private ObjectIDManager() {
    }

    public void clear() {
        for (ObjectIDType type : ObjectIDType.values()) {
            type.lastId = 0L;
            type.countNewId = 0L;
        }
    }

    public void load(DataInputStream input, int worldVersion) throws IOException {
        byte size = input.readByte();
        for (byte i = 0; i < size; i = (byte)(i + 1)) {
            byte index = input.readByte();
            long lastID = input.readLong();
            ObjectIDType.valueOf((byte)index).lastId = lastID + 100L;
            ObjectIDType.valueOf((byte)index).countNewId = 0L;
            DebugLog.General.println((Object)ObjectIDType.valueOf(index));
        }
    }

    private void save(DataOutputStream output) throws IOException {
        output.write(ObjectIDType.permanentObjectIDTypes);
        for (ObjectIDType type : ObjectIDType.values()) {
            if (type.isPermanent) {
                output.writeByte(type.index);
                output.writeLong(type.lastId);
            }
            type.countNewId = 0L;
            DebugLog.General.println((Object)type);
        }
    }

    private boolean isNeedToSave() {
        for (ObjectIDType type : ObjectIDType.values()) {
            if (type.countNewId < 100L) continue;
            return true;
        }
        return false;
    }

    public void checkForSaveDataFile(boolean force) {
        if (GameClient.client) {
            return;
        }
        if (force || ++objectIdManagerCheckLimiter > 300) {
            objectIdManagerCheckLimiter = 0;
            if (force || this.isNeedToSave()) {
                DebugLog.General.println("The id_manager_data.bin file is saved");
                File outFile = ZomboidFileSystem.instance.getFileInCurrentSave("id_manager_data.bin");
                try (FileOutputStream fos = new FileOutputStream(outFile);
                     DataOutputStream output = new DataOutputStream(fos);){
                    output.writeInt(IsoWorld.getWorldVersion());
                    this.save(output);
                }
                catch (IOException e) {
                    DebugLog.General.printException(e, "Save failed", LogSeverity.Error);
                }
            }
        }
    }

    public static IIdentifiable get(ObjectID id) {
        return id.getType().idToObjectMap.get(id.getObjectID());
    }

    public void remove(ObjectID id) {
        IIdentifiable obj = id.getType().idToObjectMap.get(id.getObjectID());
        if (id.getType().idToObjectMap.contains(id.getObjectID())) {
            id.getType().idToObjectMap.remove(id.getObjectID(), obj);
        }
    }

    public void addObject(IIdentifiable object) {
        if (object == null) {
            DebugLog.General.warn("%s ObjectID: is null");
            return;
        }
        long id = object.getObjectID().getObjectID();
        ObjectIDType type = object.getObjectID().getType();
        if (id == -1L) {
            if (GameClient.client) {
                return;
            }
            id = (short)type.allocateID();
            while (type.idToObjectMap.get(id) != null) {
                id = (short)type.allocateID();
            }
        }
        type.idToObjectMap.add(id, object);
        object.getObjectID().set(id, type);
    }

    public static ObjectID createObjectID(ObjectIDType type) {
        try {
            Constructor<?> ctr = type.type.getDeclaredConstructor(ObjectIDType.class);
            return (ObjectID)ctr.newInstance(new Object[]{type});
        }
        catch (Exception ex) {
            DebugLog.General.printException(ex, "ObjectID creation failed", LogSeverity.Error);
            throw new RuntimeException();
        }
    }
}


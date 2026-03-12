/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.io.IOException;
import java.util.HashSet;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoDirections;

public final class TableNetworkUtils {
    private static final byte SBYT_NO_SAVE = -1;
    private static final byte SBYT_STRING = 0;
    private static final byte SBYT_DOUBLE = 1;
    private static final byte SBYT_TABLE = 2;
    private static final byte SBYT_BOOLEAN = 3;
    private static final byte SBYT_ITEM = 4;
    private static final byte SBYT_DIRECTION = 5;

    public static void save(KahluaTable tbl, ByteBufferWriter output) throws IOException {
        KahluaTableIterator it = tbl.iterator();
        int count = 0;
        while (it.advance()) {
            if (!TableNetworkUtils.canSave(it.getKey(), it.getValue())) continue;
            ++count;
        }
        it = tbl.iterator();
        output.putInt(count);
        while (it.advance()) {
            byte keyByte = TableNetworkUtils.getKeyByte(it.getKey());
            byte valueByte = TableNetworkUtils.getValueByte(it.getValue());
            if (keyByte == -1 || valueByte == -1) continue;
            TableNetworkUtils.save(output, keyByte, it.getKey());
            TableNetworkUtils.save(output, valueByte, it.getValue());
        }
    }

    public static void saveSome(KahluaTable tbl, ByteBufferWriter output, HashSet<? extends Object> keys2) throws IOException {
        KahluaTableIterator it = tbl.iterator();
        int count = 0;
        while (it.advance()) {
            if (!keys2.contains(it.getKey()) || !TableNetworkUtils.canSave(it.getKey(), it.getValue())) continue;
            ++count;
        }
        it = tbl.iterator();
        output.putInt(count);
        while (it.advance()) {
            if (!keys2.contains(it.getKey())) continue;
            byte keyByte = TableNetworkUtils.getKeyByte(it.getKey());
            byte valueByte = TableNetworkUtils.getValueByte(it.getValue());
            if (keyByte == -1 || valueByte == -1) continue;
            TableNetworkUtils.save(output, keyByte, it.getKey());
            TableNetworkUtils.save(output, valueByte, it.getValue());
        }
    }

    private static void save(ByteBufferWriter output, byte sbyt, Object o) throws IOException, RuntimeException {
        output.putByte(sbyt);
        if (sbyt == 0) {
            output.putUTF((String)o);
        } else if (sbyt == 1) {
            output.putDouble((Double)o);
        } else if (sbyt == 3) {
            output.putBoolean((Boolean)o);
        } else if (sbyt == 2) {
            TableNetworkUtils.save((KahluaTable)o, output);
        } else if (sbyt == 4) {
            ((InventoryItem)o).saveWithSize(output.bb, false);
        } else if (sbyt == 5) {
            output.putEnum((IsoDirections)((Object)o));
        } else {
            throw new RuntimeException("invalid lua table type " + sbyt);
        }
    }

    public static void load(KahluaTable tbl, ByteBufferReader input) throws IOException {
        int count = input.getInt();
        tbl.wipe();
        for (int n = 0; n < count; ++n) {
            byte keyByte = input.getByte();
            Object key = TableNetworkUtils.load(input, keyByte);
            byte valueByte = input.getByte();
            Object value = TableNetworkUtils.load(input, valueByte);
            tbl.rawset(key, value);
        }
    }

    public static Object load(ByteBufferReader input, byte sbyt) throws IOException, RuntimeException {
        if (sbyt == 0) {
            return input.getUTF();
        }
        if (sbyt == 1) {
            return input.getDouble();
        }
        if (sbyt == 3) {
            return input.getBoolean();
        }
        if (sbyt == 2) {
            KahluaTableImpl v = (KahluaTableImpl)LuaManager.platform.newTable();
            TableNetworkUtils.load(v, input);
            return v;
        }
        if (sbyt == 4) {
            InventoryItem item = null;
            try {
                item = InventoryItem.loadItem(input.bb, 244);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            return item;
        }
        if (sbyt == 5) {
            return input.getEnum(IsoDirections.class);
        }
        throw new RuntimeException("invalid lua table type " + sbyt);
    }

    private static byte getKeyByte(Object o) {
        if (o instanceof String) {
            return 0;
        }
        if (o instanceof Double) {
            return 1;
        }
        return -1;
    }

    private static byte getValueByte(Object o) {
        if (o instanceof String) {
            return 0;
        }
        if (o instanceof Double) {
            return 1;
        }
        if (o instanceof Boolean) {
            return 3;
        }
        if (o instanceof KahluaTableImpl) {
            return 2;
        }
        if (o instanceof InventoryItem) {
            return 4;
        }
        if (o instanceof IsoDirections) {
            return 5;
        }
        return -1;
    }

    public static boolean canSave(Object key, Object value) {
        return TableNetworkUtils.getKeyByte(key) != -1 && TableNetworkUtils.getValueByte(value) != -1;
    }
}


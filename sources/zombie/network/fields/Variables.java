/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import zombie.GameWindow;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;

public class Variables
implements INetworkPacketField {
    @JSONField
    protected final HashMap<String, String> variables = new HashMap();

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        int size = b.getInt();
        for (int i = 0; i < size; ++i) {
            String key = b.getUTF();
            String value = b.getUTF();
            this.variables.put(key, value);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.write(b.bb);
    }

    public void write(ByteBuffer b) {
        int size = this.variables.size();
        b.putInt(size);
        for (Map.Entry<String, String> variable : this.variables.entrySet()) {
            GameWindow.WriteString(b, variable.getKey());
            GameWindow.WriteString(b, variable.getValue());
        }
    }

    public void clear() {
        this.variables.clear();
    }

    public HashMap<String, String> get() {
        return this.variables;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.statistics.tools;

import java.io.FileWriter;
import java.io.IOException;
import zombie.core.random.RandLua;
import zombie.core.random.RandStandard;
import zombie.network.PacketTypes;

public class WiresharkPluginUpdater {
    static void main(String[] args2) {
        RandStandard.INSTANCE.init();
        RandLua.INSTANCE.init();
        PacketTypes.PacketType[] values2 = PacketTypes.PacketType.values();
        StringBuilder content = new StringBuilder();
        content.append("pzPacketType = {\n");
        for (int i = 0; i < values2.length; ++i) {
            content.append(String.format("\t[%d] = \"%s\",\n", i, values2[i].name()));
        }
        content.append("}\n");
        try (FileWriter writer = new FileWriter("packet_types.txt");){
            writer.write(content.toString());
            System.out.println("Successfully wrote to file.");
        }
        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}


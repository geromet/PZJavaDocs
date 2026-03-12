/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import generation.ScriptFileGenerator;
import zombie.core.Core;
import zombie.network.GameServer;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.Registry;

public class DevGameServer {
    static void main(String[] args2) throws Exception {
        Core.IS_DEV = true;
        Registry<Registry<?>> registry = Registries.REGISTRY;
        ScriptFileGenerator.main(args2);
        GameServer.main(args2);
    }
}


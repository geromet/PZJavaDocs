/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import zombie.network.server.IEventController;
import zombie.util.PublicServerUtil;

public class StackBot
implements IEventController {
    private final String webhookUrl;

    public StackBot(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void process(String event) {
        PublicServerUtil.callPostJson(this.webhookUrl, "{ \"text\": \"" + event.replace("\"", "\\\"").replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n") + "\" }");
    }
}


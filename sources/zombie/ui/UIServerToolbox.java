/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import java.util.ArrayList;
import java.util.Objects;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.znet.SteamUtils;
import zombie.network.CoopMaster;
import zombie.network.ICoopServerMessageListener;
import zombie.ui.DialogButton;
import zombie.ui.NewWindow;
import zombie.ui.ScrollBar;
import zombie.ui.UIEventHandler;
import zombie.ui.UIFont;
import zombie.ui.UITextBox2;

public final class UIServerToolbox
extends NewWindow
implements ICoopServerMessageListener,
UIEventHandler {
    public static UIServerToolbox instance;
    ScrollBar scrollBarV;
    UITextBox2 outputLog;
    private final ArrayList<String> incomingConnections = new ArrayList();
    DialogButton buttonAccept;
    DialogButton buttonReject;
    private String externalAddress;
    private String steamId;
    public boolean autoAccept;

    public UIServerToolbox(int x, int y) {
        super(x, y, 10, 10, true);
        this.resizeToFitY = false;
        this.visible = true;
        if (instance != null) {
            instance.shutdown();
        }
        instance = this;
        this.width = 340.0f;
        this.height = 325.0f;
        int cx = 6;
        int px = 37;
        this.outputLog = new UITextBox2(UIFont.Small, 5, 33, 330, 260, Translator.getText("IGUI_ServerToolBox_Status"), true);
        this.outputLog.multipleLine = true;
        this.scrollBarV = new ScrollBar("ServerToolboxScrollbar", this, (int)(this.outputLog.getX() + this.outputLog.getWidth()) - 14, this.outputLog.getY().intValue() + 4, this.outputLog.getHeight().intValue() - 8, true);
        this.scrollBarV.SetParentTextBox(this.outputLog);
        this.AddChild(this.outputLog);
        this.AddChild(this.scrollBarV);
        this.buttonAccept = new DialogButton(this, 30, 225, Translator.getText("IGUI_ServerToolBox_acccept"), "accept");
        this.buttonReject = new DialogButton(this, 80, 225, Translator.getText("IGUI_ServerToolBox_reject"), "reject");
        this.AddChild(this.buttonAccept);
        this.AddChild(this.buttonReject);
        this.buttonAccept.setVisible(false);
        this.buttonReject.setVisible(false);
        this.PrintLine("\n");
        if (CoopMaster.instance.isRunning()) {
            CoopMaster.instance.addListener(this);
            CoopMaster.instance.invokeServer("get-parameter", "external-ip", new ICoopServerMessageListener(this){
                final /* synthetic */ UIServerToolbox this$0;
                {
                    UIServerToolbox uIServerToolbox = this$0;
                    Objects.requireNonNull(uIServerToolbox);
                    this.this$0 = uIServerToolbox;
                }

                @Override
                public void OnCoopServerMessage(String tag, String cookie, String payload) {
                    this.this$0.externalAddress = payload;
                    String address = "null".equals(this.this$0.externalAddress) ? Translator.getText("IGUI_ServerToolBox_IPUnknown") : this.this$0.externalAddress;
                    this.this$0.PrintLine(Translator.getText("IGUI_ServerToolBox_ServerAddress", address));
                    this.this$0.PrintLine("");
                }
            });
            if (SteamUtils.isSteamModeEnabled()) {
                CoopMaster.instance.invokeServer("get-parameter", "steam-id", new ICoopServerMessageListener(this){
                    final /* synthetic */ UIServerToolbox this$0;
                    {
                        UIServerToolbox uIServerToolbox = this$0;
                        Objects.requireNonNull(uIServerToolbox);
                        this.this$0 = uIServerToolbox;
                    }

                    @Override
                    public void OnCoopServerMessage(String tag, String cookie, String payload) {
                        this.this$0.steamId = payload;
                        if (!Core.getInstance().getOptionStreamerMode()) {
                            this.this$0.PrintLine(Translator.getText("IGUI_ServerToolBox_SteamID", this.this$0.steamId));
                        }
                        this.this$0.PrintLine("");
                        this.this$0.PrintLine(Translator.getText("IGUI_ServerToolBox_Invite1"));
                        this.this$0.PrintLine("");
                        this.this$0.PrintLine(Translator.getText("IGUI_ServerToolBox_Invite2"));
                        this.this$0.PrintLine(Translator.getText("IGUI_ServerToolBox_Invite3"));
                        this.this$0.PrintLine("");
                        this.this$0.PrintLine(Translator.getText("IGUI_ServerToolBox_Invite4"));
                        this.this$0.PrintLine("");
                        this.this$0.PrintLine(Translator.getText("IGUI_ServerToolBox_Invite5"));
                    }
                });
            }
        }
    }

    @Override
    public void render() {
        String username;
        String address;
        if (!this.isVisible().booleanValue()) {
            return;
        }
        super.render();
        this.DrawTextCentre(Translator.getText("IGUI_ServerToolBox_Title"), this.getWidth() / 2.0, 2.0, 1.0, 1.0, 1.0, 1.0);
        String string = address = "null".equals(this.externalAddress) ? Translator.getText("IGUI_ServerToolBox_IPUnknown") : this.externalAddress;
        if (!Core.getInstance().getOptionStreamerMode()) {
            this.DrawText(Translator.getText("IGUI_ServerToolBox_ExternalIP", address), 7.0, 19.0, 0.7f, 0.7f, 1.0, 1.0);
        }
        if (!this.incomingConnections.isEmpty() && (username = this.incomingConnections.get(0)) != null) {
            this.DrawText(Translator.getText("IGUI_ServerToolBox_UserConnecting", username), 10.0, 205.0, 0.7f, 0.7f, 1.0, 1.0);
        }
    }

    @Override
    public void update() {
        if (!this.isVisible().booleanValue()) {
            return;
        }
        if (this.incomingConnections.isEmpty()) {
            this.buttonReject.setVisible(false);
            this.buttonAccept.setVisible(false);
        } else {
            this.buttonReject.setVisible(true);
            this.buttonAccept.setVisible(true);
        }
        super.update();
    }

    void UpdateViewPos() {
        this.outputLog.topLineIndex = this.outputLog.lines.size() - this.outputLog.numVisibleLines;
        if (this.outputLog.topLineIndex < 0) {
            this.outputLog.topLineIndex = 0;
        }
        this.scrollBarV.scrollToBottom();
    }

    @Override
    public synchronized void OnCoopServerMessage(String tag, String cookie, String payload) {
        if (Objects.equals(tag, "login-attempt")) {
            this.PrintLine(payload + " is connecting");
            if (this.autoAccept) {
                this.PrintLine("Accepted connection from " + payload);
                CoopMaster.instance.sendMessage("approve-login-attempt", payload);
            } else {
                this.incomingConnections.add(payload);
                this.setVisible(true);
            }
        }
    }

    void PrintLine(String string) {
        this.outputLog.SetText(this.outputLog.text + string + "\n");
        this.UpdateViewPos();
    }

    public void shutdown() {
        if (CoopMaster.instance != null) {
            CoopMaster.instance.removeListener(this);
        }
    }

    @Override
    public void DoubleClick(String name, int x, int y) {
    }

    @Override
    public void ModalClick(String name, String chosen) {
    }

    @Override
    public void Selected(String name, int selected, int lastSelected) {
        String username;
        if (Objects.equals(name, "accept")) {
            username = this.incomingConnections.get(0);
            this.incomingConnections.remove(0);
            this.PrintLine("Accepted connection from " + username);
            CoopMaster.instance.sendMessage("approve-login-attempt", username);
        }
        if (Objects.equals(name, "reject")) {
            username = this.incomingConnections.get(0);
            this.incomingConnections.remove(0);
            this.PrintLine("Rejected connection from " + username);
            CoopMaster.instance.sendMessage("reject-login-attempt", username);
        }
    }
}


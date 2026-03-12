/*
 * Decompiled with CFR 0.152.
 */
package zombie.commands.serverCommands;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.ServerOptions;

@CommandName(name="help")
@CommandArgs(optional="(\\w+)")
@CommandHelp(helpText="UI_ServerOptionDesc_Help")
@RequiredCapability(requiredCapability=Capability.LoginOnServer)
public class HelpCommand
extends CommandBase {
    public HelpCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String command = this.getCommandArg(0);
        if (command != null) {
            Class<?> cls = HelpCommand.findCommandCls(command);
            if (cls != null) {
                return HelpCommand.getHelp(cls);
            }
            return "Unknown command /" + command;
        }
        String carriageReturn = " <LINE> ";
        StringBuilder result = new StringBuilder();
        if (this.connection == null) {
            carriageReturn = "\n";
        }
        if (!GameServer.server) {
            ArrayList<String> clientOption = ServerOptions.getClientCommandList(this.connection != null);
            for (String string : clientOption) {
                result.append(string);
            }
        }
        result.append("List of ").append("server").append(" commands : ");
        TreeMap<String, String> commandsHelp = new TreeMap<String, String>();
        for (Class<?> cls : HelpCommand.getSubClasses()) {
            String help;
            if (HelpCommand.isDisabled(cls) || (help = HelpCommand.getHelp(cls)) == null) continue;
            commandsHelp.put(HelpCommand.getCommandName(cls), help);
        }
        for (Map.Entry entry : commandsHelp.entrySet()) {
            result.append(carriageReturn).append("* ").append((String)entry.getKey()).append(" : ").append((String)entry.getValue());
        }
        return result.toString();
    }
}


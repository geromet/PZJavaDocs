/*
 * Decompiled with CFR 0.152.
 */
package zombie.commands.serverCommands;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.network.BanSystem;

@CommandName(name="unbanip")
@CommandArgs(required={"((?:\\d{1,3}\\.){3}\\d{1,3})"})
@CommandHelp(helpText="UI_ServerOptionDesc_UnBanIp")
@RequiredCapability(requiredCapability=Capability.BanUnbanUser)
public class UnbanIPCommand
extends CommandBase {
    public UnbanIPCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() throws SQLException {
        String ip = this.getCommandArg(0);
        if (SteamUtils.isSteamModeEnabled()) {
            return "Server is in Steam mode";
        }
        return BanSystem.BanIP(ip, this.connection, "", false);
    }
}


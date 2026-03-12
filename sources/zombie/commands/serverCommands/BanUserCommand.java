/*
 * Decompiled with CFR 0.152.
 */
package zombie.commands.serverCommands;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.AltCommandArgs;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.network.BanSystem;
import zombie.network.GameServer;
import zombie.network.ServerOptions;

@CommandName(name="banuser")
@AltCommandArgs(value={@CommandArgs(required={"(.+)"}, argName="Ban User Only"), @CommandArgs(required={"(.+)", "-ip"}, argName="Ban User And IP"), @CommandArgs(required={"(.+)", "-r", "(.+)"}, argName="Ban User And Supply Reason"), @CommandArgs(required={"(.+)", "-ip", "-r", "(.+)"}, argName="Ban User And IP And Supply Reason")})
@CommandHelp(helpText="UI_ServerOptionDesc_BanUser")
@RequiredCapability(requiredCapability=Capability.BanUnbanUser)
public class BanUserCommand
extends CommandBase {
    private String reason = "";
    public static final String banUser = "Ban User Only";
    public static final String banWithIP = "Ban User And IP";
    public static final String banWithReason = "Ban User And Supply Reason";
    public static final String banWithReasonIP = "Ban User And IP And Supply Reason";

    public BanUserCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() throws SQLException {
        boolean found;
        String banTarget = this.getCommandArg(0);
        if (this.hasOptionalArg(1)) {
            this.reason = this.getCommandArg(1);
        }
        boolean doIPBan = false;
        switch (this.argsName) {
            case "Ban User And IP": 
            case "Ban User And IP And Supply Reason": {
                doIPBan = true;
            }
        }
        if (SteamUtils.isSteamModeEnabled() && doIPBan) {
            return "Server is in Steam mode, you can't ban IP";
        }
        String message = doIPBan ? BanSystem.BanUserByIP(banTarget, this.connection, this.reason, true) : BanSystem.BanUser(banTarget, this.connection, this.reason, true);
        boolean bl = found = GameServer.getPlayerByUserName(banTarget) != null;
        if (found && ServerOptions.instance.banKickGlobalSound.getValue()) {
            GameServer.PlaySoundAtEveryPlayer("Thunder");
        }
        return message;
    }
}


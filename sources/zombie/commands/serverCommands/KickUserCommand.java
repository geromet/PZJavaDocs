/*
 * Decompiled with CFR 0.152.
 */
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.AltCommandArgs;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.CommandNames;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;

@CommandNames(value={@CommandName(name="kick"), @CommandName(name="kickuser")})
@AltCommandArgs(value={@CommandArgs(required={"(.+)"}), @CommandArgs(required={"(.+)", "-r", "(.+)"})})
@CommandHelp(helpText="UI_ServerOptionDesc_Kick")
@RequiredCapability(requiredCapability=Capability.KickUser)
public class KickUserCommand
extends CommandBase {
    private String reason = "";

    public KickUserCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String kickedUser = this.getCommandArg(0);
        if (this.hasOptionalArg(1)) {
            this.reason = this.getCommandArg(1);
        }
        LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " kicked user " + kickedUser);
        ServerWorldDatabase.instance.addUserlog(kickedUser, Userlog.UserlogType.Kicked, this.reason, this.getExecutorUsername(), 1);
        boolean found = false;
        block0: for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                if (!kickedUser.equals(c.usernames[playerIndex])) continue;
                if (!c.getRole().hasCapability(Capability.CantBeKickedByUser)) {
                    found = true;
                    if ("".equals(this.reason)) {
                        GameServer.kick(c, "UI_Policy_Kick", null);
                    } else {
                        GameServer.kick(c, "UI_Policy_KickReason", this.reason);
                    }
                    c.forceDisconnect("command-kick");
                    continue block0;
                }
                return "This user can't be kicked.";
            }
        }
        if (found && ServerOptions.instance.banKickGlobalSound.getValue()) {
            GameServer.PlaySoundAtEveryPlayer("RumbleThunder");
        }
        if (found) {
            return "User " + kickedUser + " kicked.";
        }
        return "User " + kickedUser + " doesn't exist.";
    }
}


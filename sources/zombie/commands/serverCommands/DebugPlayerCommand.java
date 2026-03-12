/*
 * Decompiled with CFR 0.152.
 */
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;

@CommandName(name="debugplayer")
@CommandArgs(required={"(.+)"})
@RequiredCapability(requiredCapability=Capability.ConnectWithDebug)
public class DebugPlayerCommand
extends CommandBase {
    public DebugPlayerCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        if (this.getCommandArgsCount() != 1) {
            return "/debugplayer \"username\"";
        }
        String debugPlayerName = this.getCommandArg(0);
        IsoPlayer player = GameServer.getPlayerByUserNameForCommand(debugPlayerName);
        if (player == null) {
            return "no such user";
        }
        UdpConnection playerConnection = GameServer.getConnectionByPlayerOnlineID(player.onlineId);
        if (playerConnection == null) {
            return "no connection for user";
        }
        if (GameServer.DebugPlayer.contains(playerConnection)) {
            GameServer.DebugPlayer.remove(playerConnection);
            return "debug off";
        }
        GameServer.DebugPlayer.add(playerConnection);
        return "debug on";
    }
}


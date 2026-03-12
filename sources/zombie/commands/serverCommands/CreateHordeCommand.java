/*
 * Decompiled with CFR 0.152.
 */
package zombie.commands.serverCommands;

import zombie.VirtualZombieManager;
import zombie.ZombieSpawnRecorder;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;

@CommandName(name="createhorde")
@CommandArgs(required={"(\\d+)"}, optional="(.+)")
@CommandHelp(helpText="UI_ServerOptionDesc_CreateHorde")
@RequiredCapability(requiredCapability=Capability.CreateHorde)
public class CreateHordeCommand
extends CommandBase {
    public CreateHordeCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        Integer count = Integer.parseInt(this.getCommandArg(0));
        String user = this.getCommandArg(1);
        IsoPlayer player = null;
        if (this.getCommandArgsCount() == 2) {
            player = GameServer.getPlayerByUserNameForCommand(user);
            if (player == null) {
                return "User \"" + user + "\" not found";
            }
        } else if (this.connection != null) {
            player = GameServer.getAnyPlayerFromConnection(this.connection);
        }
        if (count == null) {
            return this.getHelp();
        }
        count = Math.min(count, 500);
        if (player != null) {
            for (int i = 0; i < count; ++i) {
                VirtualZombieManager.instance.choices.clear();
                IsoGridSquare g = IsoWorld.instance.currentCell.getGridSquare(Rand.Next(player.getX() - 10.0f, player.getX() + 10.0f), Rand.Next(player.getY() - 10.0f, player.getY() + 10.0f), player.getZ());
                VirtualZombieManager.instance.choices.add(g);
                IsoZombie zombie = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.getRandom(), false);
                if (zombie == null) continue;
                ZombieSpawnRecorder.instance.record(zombie, this.getClass().getSimpleName());
            }
            LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " created a horde of " + count + " zombies near " + player.getX() + "," + player.getY(), "IMPORTANT");
            return "Horde spawned.";
        }
        return "Specify a player to create the horde near to.";
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.commands.serverCommands;

import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.util.StringUtils;

@CommandName(name="createhorde2")
@CommandArgs(varArgs=true)
@CommandHelp(helpText="UI_ServerOptionDesc_CreateHorde2")
@RequiredCapability(requiredCapability=Capability.CreateHorde)
public class CreateHorde2Command
extends CommandBase {
    public CreateHorde2Command(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        int count = -1;
        int radius = -1;
        int x = -1;
        int y = -1;
        int z = -1;
        boolean crawler = false;
        boolean isFallOnFront = false;
        boolean isFakeDead = false;
        boolean isKnockedDown = false;
        boolean isInvulnerable = false;
        float health = 1.0f;
        String outfitName = null;
        block28: for (int i = 0; i < this.getCommandArgsCount() - 1; i += 2) {
            String option = this.getCommandArg(i);
            String value = this.getCommandArg(i + 1);
            switch (option) {
                case "-count": {
                    count = PZMath.tryParseInt(value, -1);
                    continue block28;
                }
                case "-radius": {
                    radius = PZMath.tryParseInt(value, -1);
                    continue block28;
                }
                case "-x": {
                    x = PZMath.tryParseInt(value, -1);
                    continue block28;
                }
                case "-y": {
                    y = PZMath.tryParseInt(value, -1);
                    continue block28;
                }
                case "-z": {
                    z = PZMath.tryParseInt(value, -1);
                    continue block28;
                }
                case "-outfit": {
                    outfitName = StringUtils.discardNullOrWhitespace(value);
                    continue block28;
                }
                case "-crawler": {
                    crawler = !"false".equals(value);
                    continue block28;
                }
                case "-isFallOnFront": {
                    isFallOnFront = !"false".equals(value);
                    continue block28;
                }
                case "-isFakeDead": {
                    isFakeDead = !"false".equals(value);
                    continue block28;
                }
                case "-knockedDown": {
                    isKnockedDown = !"false".equals(value);
                    continue block28;
                }
                case "-isInvulnerable": {
                    isInvulnerable = !"false".equals(value);
                    continue block28;
                }
                case "-health": {
                    health = Float.parseFloat(value);
                    continue block28;
                }
                default: {
                    return this.getHelp();
                }
            }
        }
        if (count > 500) {
            System.out.println("Zombie spawn commands are capped at 500 maximum zombies per command");
        }
        count = PZMath.clamp(count, 1, 500);
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (square == null) {
            return "invalid location";
        }
        if (outfitName != null && OutfitManager.instance.FindMaleOutfit(outfitName) == null && OutfitManager.instance.FindFemaleOutfit(outfitName) == null) {
            return "invalid outfit";
        }
        Integer femaleChance = null;
        if (outfitName != null) {
            if (OutfitManager.instance.FindFemaleOutfit(outfitName) == null) {
                femaleChance = Integer.MIN_VALUE;
            } else if (OutfitManager.instance.FindMaleOutfit(outfitName) == null) {
                femaleChance = Integer.MAX_VALUE;
            }
        }
        for (int i = 0; i < count; ++i) {
            int x1 = radius <= 0 ? x : Rand.Next(x - radius, x + radius + 1);
            int y1 = radius <= 0 ? y : Rand.Next(y - radius, y + radius + 1);
            LuaManager.GlobalObject.addZombiesInOutfit(x1, y1, z, 1, outfitName, femaleChance, crawler, isFallOnFront, isFakeDead, isKnockedDown, isInvulnerable, false, health);
        }
        LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " created a horde of " + count + " zombies near " + x + "," + y, "IMPORTANT");
        return "Horde spawned.";
    }
}


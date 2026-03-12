/*
 * Decompiled with CFR 0.152.
 */
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.characters.skills.PerkFactory;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;

@CommandName(name="addxp")
@CommandArgs(required={"(.+)", "(\\S+)"}, optional="(-true|-false)")
@CommandHelp(helpText="UI_ServerOptionDesc_AddXp")
@RequiredCapability(requiredCapability=Capability.AddXP)
public class AddXPCommand
extends CommandBase {
    private static final String MULTIPLIER_ENABLED = "-true";

    public AddXPCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        float xp;
        String user = this.getCommandArg(0);
        String perks = this.getCommandArg(1);
        String isMultiplierEnabled = this.getCommandArg(2);
        IsoPlayer p = GameServer.getPlayerByUserNameForCommand(user);
        if (p == null) {
            return "No such user";
        }
        String username = p.getDisplayName();
        String[] perkAndXp = perks.split("=", 2);
        if (perkAndXp.length != 2) {
            return this.getHelp();
        }
        String perk = perkAndXp[0].trim();
        if (PerkFactory.Perks.FromString(perk) == PerkFactory.Perks.MAX) {
            String nl = this.connection == null ? "\n" : " LINE ";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < PerkFactory.PerkList.size(); ++i) {
                if (PerkFactory.PerkList.get(i) == PerkFactory.Perks.Passiv) continue;
                sb.append(PerkFactory.PerkList.get(i));
                if (i >= PerkFactory.PerkList.size()) continue;
                sb.append(nl);
            }
            return "List of available perks :" + nl + String.valueOf(sb);
        }
        try {
            xp = Float.parseFloat(perkAndXp[1]);
        }
        catch (NumberFormatException ex) {
            return this.getHelp();
        }
        UdpConnection c = GameServer.getConnectionFromPlayer(p);
        if (c != null) {
            p.getXp().AddXP(PerkFactory.Perks.FromString(perk), xp, false, isMultiplierEnabled != null && isMultiplierEnabled.equals(MULTIPLIER_ENABLED), false, false);
            if (perk.equals(PerkFactory.Perks.Fitness)) {
                p.getStats().set(CharacterStat.FITNESS, (float)p.getPerkLevel(PerkFactory.Perks.Fitness) / 5.0f - 1.0f);
            }
            LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " added " + xp + " " + perk + " xp's to " + username);
            return "Added " + xp + " " + perk + " xp's to " + username;
        }
        return "User " + username + " not found.";
    }
}


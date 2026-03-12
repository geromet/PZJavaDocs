/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatXP;
import zombie.network.anticheats.AntiCheatXPPlayer;
import zombie.network.fields.Perk;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.AddXP, handlingType=1, anticheats={AntiCheat.XPPlayer, AntiCheat.XP})
public class AddXpPacket
implements INetworkPacket,
AntiCheatXP.IAntiCheat,
AntiCheatXPPlayer.IAntiCheat {
    @JSONField
    public final PlayerID target = new PlayerID();
    @JSONField
    protected Perk perk = new Perk();
    @JSONField
    protected float amount;
    @JSONField
    protected boolean noMultiplier;
    @JSONField
    protected boolean showXP;

    @Override
    public void setData(Object ... values2) {
        if (values2.length == 3) {
            this.set((IsoPlayer)values2[0], (PerkFactory.Perk)values2[1], ((Float)values2[2]).floatValue());
        } else if (values2.length == 4) {
            this.set((IsoPlayer)values2[0], (PerkFactory.Perk)values2[1], ((Float)values2[2]).floatValue(), (Boolean)values2[3]);
        } else if (values2.length == 5) {
            this.set((IsoPlayer)values2[0], (PerkFactory.Perk)values2[1], ((Float)values2[2]).floatValue(), (Boolean)values2[3], (Boolean)values2[4]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(IsoPlayer target, PerkFactory.Perk perk, float amount) {
        this.target.set(target);
        this.perk.set(perk);
        this.amount = amount;
        this.noMultiplier = false;
    }

    public void set(IsoPlayer target, PerkFactory.Perk perk, float amount, boolean noMultiplier) {
        this.target.set(target);
        this.perk.set(perk);
        this.amount = amount;
        this.noMultiplier = noMultiplier;
    }

    public void set(IsoPlayer target, PerkFactory.Perk perk, float amount, boolean noMultiplier, boolean showXP) {
        this.target.set(target);
        this.perk.set(perk);
        this.amount = amount;
        this.noMultiplier = noMultiplier;
        this.showXP = showXP;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.target.parse(b, connection);
        this.perk.parse(b, connection);
        this.amount = b.getFloat();
        this.noMultiplier = b.getBoolean();
        this.showXP = b.getBoolean();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.target.write(b);
        this.perk.write(b);
        b.putFloat(this.amount);
        b.putBoolean(this.noMultiplier);
        b.putBoolean(this.showXP);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        AddXpPacket.addXp(connection, this.target.getPlayer(), this.perk.getPerk(), this.amount, this.noMultiplier, this.showXP);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.target.isConsistent(connection) && this.perk.isConsistent(connection);
    }

    @Override
    public IsoPlayer getPlayer() {
        return this.target.getPlayer();
    }

    @Override
    public float getAmount() {
        return this.amount;
    }

    public static void addXp(UdpConnection connection, IsoPlayer player, PerkFactory.Perk perk, float amount, boolean noMultiplier, boolean showXP) {
        if (!GameServer.canModifyPlayerStats(connection, player)) {
            PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.AddXP);
            return;
        }
        if (player != null && !player.isDead()) {
            player.getXp().AddXP(perk, amount, false, !noMultiplier, true, showXP);
            if (GameServer.canModifyPlayerStats(connection, null)) {
                player.getXp().getGrowthRate();
            }
        }
        INetworkPacket.send(player, PacketTypes.PacketType.AddXP, player, perk, Float.valueOf(amount), noMultiplier, showXP);
    }
}


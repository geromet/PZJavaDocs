/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.packets.character.PlayerPacket;

@PacketSetting(ordering=5, priority=2, reliability=1, requiredCapability=Capability.LoginOnServer, handlingType=3, anticheats={AntiCheat.Power, AntiCheat.Speed, AntiCheat.NoClip})
public class PlayerPacketUnreliable
extends PlayerPacket {
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.network.PacketSetting;
import zombie.network.packets.character.ZombieSimulationPacket;

@PacketSetting(ordering=0, priority=0, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=1)
public class ZombieSimulationReliablePacket
extends ZombieSimulationPacket {
}


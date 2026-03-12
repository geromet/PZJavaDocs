/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.character.DeadCharacterPacket;

@PacketSetting(ordering=0, priority=0, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=2)
public class DeadAnimalPacket
extends DeadCharacterPacket
implements INetworkPacket {
}


/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.vehicle;

import zombie.characters.Capability;
import zombie.network.PacketSetting;
import zombie.network.packets.vehicle.VehiclePhysicsPacket;

@PacketSetting(ordering=8, priority=1, reliability=3, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class VehiclePhysicsReliablePacket
extends VehiclePhysicsPacket {
}


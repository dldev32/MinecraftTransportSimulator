package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.systems.CameraSystem;

/**
 * Packet sent from the server to apply a camera shake impulse to a nearby player.
 * The strength has already been scaled by the server config and distance.
 *
 * @author dldev32
 */
public class PacketCameraShake extends APacketBase {
    private final double strength;

    public PacketCameraShake(double strength) {
        super(null);
        this.strength = strength;
    }

    public PacketCameraShake(ByteBuf buf) {
        super(buf);
        this.strength = buf.readDouble();
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeDouble(strength);
    }

    @Override
    public void handle(AWrapperWorld world) {
        if (world.isClient()) {
            CameraSystem.startCameraShake(strength);
        }
    }
}

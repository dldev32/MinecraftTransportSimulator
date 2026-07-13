package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to send seat-specific control signals, including gun selection, optics selection,
 * and camera zoom.  Sent to servers when a player presses the corresponding control and sent back
 * to all clients for updating.
 *
 * @author don_bruce
 */
public class PacketPartSeat extends APacketEntity<PartSeat> {
    private final SeatAction packetType;

    public PacketPartSeat(PartSeat seat, SeatAction packetType) {
        super(seat);
        this.packetType = packetType;
    }

    public PacketPartSeat(ByteBuf buf) {
        super(buf);
        this.packetType = SeatAction.values()[buf.readByte()];
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(packetType.ordinal());
    }

    @Override
    public boolean handle(AWrapperWorld world, PartSeat seat) {
        switch (packetType) {
            case CHANGE_GUN: {
                seat.setNextActiveGun();
                return true;
            }
            case CHANGE_OPTIC: {
                seat.setNextOptic();
                return true;
            }
            case ZOOM_IN: {
                return seat.zoomIn();
            }
            case ZOOM_OUT: {
                return seat.zoomOut();
            }
        }
        return false;
    }

    public enum SeatAction {
        CHANGE_GUN,
        ZOOM_IN,
        ZOOM_OUT,
        CHANGE_OPTIC;
    }
}

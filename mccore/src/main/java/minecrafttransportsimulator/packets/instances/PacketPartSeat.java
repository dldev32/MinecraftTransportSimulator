package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to send control signals to seats.  Incremental actions may be sent by players to the server
 * and echoed to clients, while absolute gun selections are sent only by the server for AI synchronization.
 *
 * @author don_bruce
 */
public class PacketPartSeat extends APacketEntity<PartSeat> {
    private final SeatAction packetType;
    private final ItemPartGun gunItem;
    private final int gunIndex;
    private final UUID entityTargetID;
    private final UUID engineTargetID;

    public PacketPartSeat(PartSeat seat, SeatAction packetType) {
        super(seat);
        this.packetType = packetType;
        this.gunItem = null;
        this.gunIndex = 0;
        this.entityTargetID = null;
        this.engineTargetID = null;
    }

    public PacketPartSeat(PartSeat seat, ItemPartGun gunItem, int gunIndex) {
        this(seat, gunItem, gunIndex, null, null);
    }

    public PacketPartSeat(PartSeat seat, ItemPartGun gunItem, int gunIndex, IWrapperEntity entityTarget, PartEngine engineTarget) {
        super(seat);
        this.packetType = SeatAction.SET_GUN;
        this.gunItem = gunItem;
        this.gunIndex = gunIndex;
        this.entityTargetID = entityTarget != null ? entityTarget.getID() : null;
        this.engineTargetID = engineTarget != null ? engineTarget.uniqueUUID : null;
    }

    public PacketPartSeat(ByteBuf buf) {
        super(buf);
        this.packetType = SeatAction.values()[buf.readByte()];
        if (packetType == SeatAction.SET_GUN) {
            this.gunItem = readItemFromBuffer(buf);
            this.gunIndex = buf.readInt();
            this.entityTargetID = buf.readBoolean() ? readUUIDFromBuffer(buf) : null;
            this.engineTargetID = buf.readBoolean() ? readUUIDFromBuffer(buf) : null;
        } else {
            this.gunItem = null;
            this.gunIndex = 0;
            this.entityTargetID = null;
            this.engineTargetID = null;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(packetType.ordinal());
        if (packetType == SeatAction.SET_GUN) {
            writeItemToBuffer(gunItem, buf);
            buf.writeInt(gunIndex);
            buf.writeBoolean(entityTargetID != null);
            if (entityTargetID != null) {
                writeUUIDToBuffer(entityTargetID, buf);
            }
            buf.writeBoolean(engineTargetID != null);
            if (engineTargetID != null) {
                writeUUIDToBuffer(engineTargetID, buf);
            }
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, PartSeat seat) {
        switch (packetType) {
            case CHANGE_GUN: {
                seat.setNextActiveGun();
                return true;
            }
            case ZOOM_IN: {
                if (seat.zoomLevel > 0) {
                    --seat.zoomLevel;
                    return true;
                } else {
                    return false;
                }
            }
            case ZOOM_OUT: {
                ++seat.zoomLevel;
                return true;
            }
            case SET_GUN: {
                if (world.isClient()) {
                    seat.setActiveGunFromPacket(gunItem, gunIndex, entityTargetID, engineTargetID);
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public enum SeatAction {
        CHANGE_GUN,
        ZOOM_IN,
        ZOOM_OUT,
        SET_GUN;
    }
}

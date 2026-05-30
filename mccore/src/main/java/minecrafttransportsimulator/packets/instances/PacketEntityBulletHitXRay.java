package minecrafttransportsimulator.packets.instances;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.systems.DamageXRaySystem;
import minecrafttransportsimulator.systems.DamageXRaySystem.FragmentEvent;
import minecrafttransportsimulator.systems.DamageXRaySystem.HitEvent;
import minecrafttransportsimulator.systems.DamageXRaySystem.ResultType;

/**
 * Packet sent to clients to display a short x-ray replay for bullet damage.
 */
public class PacketEntityBulletHitXRay extends APacketBase {
    private final UUID targetID;
    private final UUID gunID;
    private final int bulletNumber;
    private final String bulletName;
    private final String targetName;
    private final String bulletModelLocation;
    private final String bulletTextureLocation;
    private final Point3D startPosition;
    private final Point3D endPosition;
    private final ResultType resultType;
    private final List<HitEvent> hitEvents;
    private final List<FragmentEvent> fragmentEvents;

    public PacketEntityBulletHitXRay(AEntityE_Interactable<?> targetEntity, UUID gunID, int bulletNumber, String bulletName, String targetName, String bulletModelLocation, String bulletTextureLocation, Point3D startPosition, Point3D endPosition, ResultType resultType, List<HitEvent> hitEvents, List<FragmentEvent> fragmentEvents) {
        super(null);
        this.targetID = targetEntity.uniqueUUID;
        this.gunID = gunID;
        this.bulletNumber = bulletNumber;
        this.bulletName = bulletName;
        this.targetName = targetName;
        this.bulletModelLocation = bulletModelLocation;
        this.bulletTextureLocation = bulletTextureLocation;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.resultType = resultType;
        this.hitEvents = hitEvents;
        this.fragmentEvents = fragmentEvents;
    }

    public PacketEntityBulletHitXRay(ByteBuf buf) {
        super(buf);
        this.targetID = readUUIDFromBuffer(buf);
        this.gunID = readUUIDFromBuffer(buf);
        this.bulletNumber = buf.readInt();
        this.bulletName = readStringFromBuffer(buf);
        this.targetName = readStringFromBuffer(buf);
        this.bulletModelLocation = readStringFromBuffer(buf);
        this.bulletTextureLocation = readStringFromBuffer(buf);
        this.startPosition = readPoint3dFromBuffer(buf);
        this.endPosition = readPoint3dFromBuffer(buf);
        this.resultType = ResultType.values()[buf.readByte()];
        int eventCount = buf.readInt();
        this.hitEvents = new ArrayList<>();
        for (int i = 0; i < eventCount; ++i) {
            Point3D hitPosition = readPoint3dFromBuffer(buf);
            int groupIndex = buf.readInt();
            int boxIndex = buf.readInt();
            String componentName = readStringFromBuffer(buf);
            double armorThickness = buf.readDouble();
            double penetrationPotential = buf.readDouble();
            double armorPenetrated = buf.readDouble();
            double collisionDamage = buf.readDouble();
            double entityDamage = buf.readDouble();
            boolean stopped = buf.readBoolean();
            boolean forwardedDamage = buf.readBoolean();
            if (i < DamageXRaySystem.MAX_EVENTS) {
                hitEvents.add(new HitEvent(hitPosition, groupIndex, boxIndex, componentName, armorThickness, penetrationPotential, armorPenetrated, collisionDamage, entityDamage, stopped, forwardedDamage));
            }
        }
        int fragmentEventCount = buf.readInt();
        this.fragmentEvents = new ArrayList<>();
        for (int i = 0; i < fragmentEventCount; ++i) {
            Point3D fragmentStartPosition = readPoint3dFromBuffer(buf);
            Point3D fragmentEndPosition = readPoint3dFromBuffer(buf);
            if (i < DamageXRaySystem.MAX_FRAGMENT_EVENTS) {
                fragmentEvents.add(new FragmentEvent(fragmentStartPosition, fragmentEndPosition));
            }
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(targetID, buf);
        writeUUIDToBuffer(gunID, buf);
        buf.writeInt(bulletNumber);
        writeStringToBuffer(bulletName, buf);
        writeStringToBuffer(targetName, buf);
        writeStringToBuffer(bulletModelLocation != null ? bulletModelLocation : "", buf);
        writeStringToBuffer(bulletTextureLocation != null ? bulletTextureLocation : "", buf);
        writePoint3dToBuffer(startPosition, buf);
        writePoint3dToBuffer(endPosition, buf);
        buf.writeByte(resultType.ordinal());
        int eventCount = Math.min(hitEvents.size(), DamageXRaySystem.MAX_EVENTS);
        buf.writeInt(eventCount);
        for (int i = 0; i < eventCount; ++i) {
            HitEvent event = hitEvents.get(i);
            writePoint3dToBuffer(event.hitPosition, buf);
            buf.writeInt(event.groupIndex);
            buf.writeInt(event.boxIndex);
            writeStringToBuffer(event.componentName != null ? event.componentName : "", buf);
            buf.writeDouble(event.armorThickness);
            buf.writeDouble(event.penetrationPotential);
            buf.writeDouble(event.armorPenetrated);
            buf.writeDouble(event.collisionDamage);
            buf.writeDouble(event.entityDamage);
            buf.writeBoolean(event.stopped);
            buf.writeBoolean(event.forwardedDamage);
        }
        int fragmentEventCount = fragmentEvents != null ? Math.min(fragmentEvents.size(), DamageXRaySystem.MAX_FRAGMENT_EVENTS) : 0;
        buf.writeInt(fragmentEventCount);
        for (int i = 0; i < fragmentEventCount; ++i) {
            FragmentEvent event = fragmentEvents.get(i);
            writePoint3dToBuffer(event.startPosition, buf);
            writePoint3dToBuffer(event.endPosition, buf);
        }
    }

    @Override
    public void handle(AWrapperWorld world) {
        AEntityE_Interactable<?> targetEntity = world.getEntity(targetID);
        DamageXRaySystem.displayAnalysis(targetEntity, gunID, bulletNumber, bulletName, targetName, bulletModelLocation.isEmpty() ? null : bulletModelLocation, bulletTextureLocation.isEmpty() ? null : bulletTextureLocation, startPosition, endPosition, resultType, hitEvents, fragmentEvents);
    }
}

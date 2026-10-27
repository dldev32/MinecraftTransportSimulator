package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.entities.instances.EntityBullet;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntityInteractHitbox;

/**
 * Packet sent when a bullet hits an entity.
 *
 * @author don_bruce
 */
public class PacketEntityBulletHitEntity extends APacketEntityInteractHitbox {
    private final UUID gunID;
    private final double amount;
    private final ItemBullet bulletItem;

    public PacketEntityBulletHitEntity(EntityBullet bullet, AEntityE_Interactable<?> entity, Damage damage) {
        super(entity, damage.box);
        this.gunID = bullet.gun.uniqueUUID;
        this.amount = damage.amount;
        this.bulletItem = (ItemBullet) bullet.cachedItem;
    }

    public PacketEntityBulletHitEntity(ByteBuf buf) {
        super(buf);
        this.gunID = readUUIDFromBuffer(buf);
        this.amount = buf.readDouble();
        this.bulletItem = readItemFromBuffer(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        writeUUIDToBuffer(gunID, buf);
        buf.writeDouble(amount);
        writeItemToBuffer(bulletItem, buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityE_Interactable<?> entity, BoundingBox hitBox) {
        PartGun gun = world.getBulletGun(gunID);
        if (gun != null && bulletItem != null) {
            EntityBullet.performEntityHitLogic(entity, new Damage(gun, bulletItem.definition.bullet, hitBox, amount));
        }
        //Don't send this back to clients.  The damage method in the call does this for us.
        return false;
    }
}

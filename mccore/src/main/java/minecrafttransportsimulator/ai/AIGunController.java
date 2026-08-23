package minecrafttransportsimulator.ai;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.entities.instances.PartGun.GunState;
import minecrafttransportsimulator.entities.instances.PartSeat;
import minecrafttransportsimulator.items.instances.ItemPartGun;
import minecrafttransportsimulator.jsondefs.JSONBullet.Bullet;
import minecrafttransportsimulator.jsondefs.JSONBullet.BulletType;
import minecrafttransportsimulator.jsondefs.JSONMuzzle;
import minecrafttransportsimulator.jsondefs.JSONPart.TargetType;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartSeat;

/**
 * Handles target acquisition, aiming, and fire requests for guns controlled by non-player riders.
 */
public class AIGunController extends AIBase {

    private static final double TARGET_SEARCH_RANGE = 128.0;
    private static final int RETARGET_INTERVAL_TICKS = 20;
    private static final int NO_TARGET_WEAPON_REEVALUATION_TICKS = 40;
    private static final double CLOSER_TARGET_HYSTERESIS = 5.0;
    private static final double SMALL_CALIBER_CUTOFF_MM = 20.0;
    private static final double AUTOCANNON_MAX_FIRE_DELAY_TICKS = 20.0;

    private final PartGun gun;
    private final RotationMatrix internalOrientation;
    private final double minYaw;
    private final double maxYaw;
    private final double yawSpeed;
    private final double minPitch;
    private final double maxPitch;
    private final double pitchSpeed;
    private final Runnable targetInvalidationHandler;

    //Temp helper variables for calculations.
    private final Point3D bulletPosition = new Point3D();
    private final Point3D targetVector = new Point3D();
    private final Point3D targetAngles = new Point3D();
    private final Point3D controllerAngles = new Point3D();

    public AIGunController(PartGun gun, RotationMatrix internalOrientation, double minYaw, double maxYaw, double yawSpeed, double minPitch, double maxPitch, double pitchSpeed, Runnable targetInvalidationHandler) {
        this.gun = gun;
        this.internalOrientation = internalOrientation;
        this.minYaw = minYaw;
        this.maxYaw = maxYaw;
        this.yawSpeed = yawSpeed;
        this.minPitch = minPitch;
        this.maxPitch = maxPitch;
        this.pitchSpeed = pitchSpeed;
        this.targetInvalidationHandler = targetInvalidationHandler;
    }

    /**
     * Updates this controller for the non-player rider currently controlling the gun.
     */
    public void updateControl(IWrapperEntity controller, PartSeat controllerSeat) {
        //Get new target if we don't have one, or if we've gone 1 second and we have a closer target by 5 blocks.
        boolean checkForCloser = gun.entityTarget != null && gun.ticksExisted % RETARGET_INTERVAL_TICKS == 0;
        if ((gun.entityTarget == null && gun.engineTarget == null) || checkForCloser) {
            for (IWrapperEntity entity : gun.world.getEntitiesHostile(controller, TARGET_SEARCH_RANGE)) {
                if (canAimAtTarget(entity)) {
                    if (gun.entityTarget != null) {
                        double distanceToBeat = gun.position.distanceTo(gun.entityTarget.getPosition());
                        if (checkForCloser) {
                            distanceToBeat -= CLOSER_TARGET_HYSTERESIS;
                        }
                        if (gun.position.distanceTo(entity.getPosition()) > distanceToBeat) {
                            continue;
                        }
                    }
                    gun.entityTarget = entity;
                }
            }
        }

        //Select once per seat tick so guns in the same group cannot fight over the active selection.
        if (!gun.world.isClient() && controllerSeat != null && !gun.isRunningInCoaxialMode) {
            IWrapperEntity selectionEntityTarget = gun.entityTarget;
            PartEngine selectionEngineTarget = gun.engineTarget;
            boolean currentWeaponUsable = isCurrentWeaponUsable(controllerSeat);
            if (selectionEntityTarget == null && selectionEngineTarget == null && currentWeaponUsable && controllerSeat.claimAIWeaponTargetSearch(NO_TARGET_WEAPON_REEVALUATION_TICKS)) {
                selectionEntityTarget = findAlternativeTarget(controller, controllerSeat);
            }
            AITargetType targetType = getTargetType(selectionEntityTarget, selectionEngineTarget);
            if ((targetType != AITargetType.NONE || !currentWeaponUsable) && controllerSeat.claimAIWeaponSelection() && selectBestWeaponWithFallback(controllerSeat, targetType, selectionEntityTarget, selectionEngineTarget)) {
                gun.state = gun.state.demote(GunState.CONTROLLED);
                return;
            }
        }

        //If we have a target, validate it and try to hit it.
        if (gun.entityTarget != null || gun.engineTarget != null) {
            boolean canAimAtTarget = gun.entityTarget != null ? canAimAtTarget(gun.entityTarget) : canAimAtTarget(gun.engineTarget);
            if (canAimAtTarget) {
                controllerAngles.set(targetVector).getAngles(true);
                controller.setYaw(controllerAngles.y);
                controller.setPitch(controllerAngles.x);

                //Only fire if we're within 1 movement increment of the target.
                if (Math.abs(targetAngles.y - internalOrientation.angles.y) < yawSpeed && Math.abs(targetAngles.x - internalOrientation.angles.x) < pitchSpeed) {
                    gun.state = gun.state.promote(GunState.FIRING_REQUESTED);
                } else {
                    gun.state = gun.state.demote(GunState.CONTROLLED);
                }
            } else {
                targetInvalidationHandler.run();
                gun.state = gun.state.demote(GunState.CONTROLLED);
            }
        } else {
            gun.state = gun.state.demote(GunState.CONTROLLED);
        }
    }

    /**
     * Ensures an NPC rider does not remain stuck on a disabled, reloading, or empty weapon.
     * This is called by the seat because a disabled gun does not run its own control logic.
     */
    public static void ensureWeaponAvailable(PartSeat seat) {
        if (!seat.world.isClient() && seat.rider != null && !(seat.rider instanceof IWrapperPlayer) && !isCurrentWeaponUsable(seat)) {
            PartGun targetSource = getSelectedTargetSource(seat);
            IWrapperEntity entityTarget = targetSource != null ? targetSource.entityTarget : null;
            PartEngine engineTarget = targetSource != null ? targetSource.engineTarget : null;
            if (seat.claimAIWeaponSelection()) {
                selectBestWeaponWithFallback(seat, getTargetType(entityTarget, engineTarget), entityTarget, engineTarget);
            }
        }
    }

    /**
     * Tries the current target first, then falls back to any operational weapon if the selected one is unusable.
     */
    private static boolean selectBestWeaponWithFallback(PartSeat seat, AITargetType targetType, IWrapperEntity entityTarget, PartEngine engineTarget) {
        if (selectBestWeapon(seat, targetType, entityTarget, engineTarget)) {
            return true;
        }
        return !isCurrentWeaponUsable(seat) && selectBestWeapon(seat, AITargetType.NONE, null, null);
    }

    /**
     * Selects the highest-priority usable weapon for the supplied target type.
     * Ties keep the current selection to prevent oscillation between equivalent weapons.
     */
    private static boolean selectBestWeapon(PartSeat seat, AITargetType targetType, IWrapperEntity entityTarget, PartEngine engineTarget) {
        if (seat.rider == null) {
            return false;
        }

        ItemPartGun bestGunItem = null;
        int bestGunIndex = 0;
        int bestPriority = Integer.MAX_VALUE;
        boolean bestIsCurrent = false;

        for (ItemPartGun gunItem : seat.gunGroups.keySet()) {
            List<PartGun> gunGroup = seat.gunGroups.get(gunItem);
            if (gunItem.definition.gun.fireSolo) {
                for (int gunIndex = 0; gunIndex < gunGroup.size(); ++gunIndex) {
                    PartGun candidate = gunGroup.get(gunIndex);
                    if (isGunUsable(candidate, seat, entityTarget, engineTarget)) {
                        int priority = getWeaponPriority(candidate, targetType);
                        boolean isCurrent = gunItem == seat.activeGunItem && gunIndex == seat.gunIndex;
                        if (bestGunItem == null || priority < bestPriority || (priority == bestPriority && isCurrent && !bestIsCurrent)) {
                            bestGunItem = gunItem;
                            bestGunIndex = gunIndex;
                            bestPriority = priority;
                            bestIsCurrent = isCurrent;
                        }
                    }
                }
            } else {
                int groupPriority = Integer.MAX_VALUE;
                for (PartGun candidate : gunGroup) {
                    if (isGunUsable(candidate, seat, entityTarget, engineTarget)) {
                        groupPriority = Math.min(groupPriority, getWeaponPriority(candidate, targetType));
                    }
                }
                if (groupPriority != Integer.MAX_VALUE) {
                    boolean isCurrent = gunItem == seat.activeGunItem;
                    if (bestGunItem == null || groupPriority < bestPriority || (groupPriority == bestPriority && isCurrent && !bestIsCurrent)) {
                        bestGunItem = gunItem;
                        bestGunIndex = 0;
                        bestPriority = groupPriority;
                        bestIsCurrent = isCurrent;
                    }
                }
            }
        }

        boolean alreadySelected = bestGunItem == seat.activeGunItem && (bestGunItem == null || !bestGunItem.definition.gun.fireSolo || bestGunIndex == seat.gunIndex);
        if (bestGunItem != null && !alreadySelected && seat.setActiveGun(bestGunItem, bestGunIndex)) {
            List<PartGun> selectedGunGroup = seat.gunGroups.get(bestGunItem);
            if (bestGunItem.definition.gun.fireSolo) {
                selectedGunGroup.get(bestGunIndex).setAITarget(entityTarget, engineTarget);
            } else {
                for (PartGun selectedGun : selectedGunGroup) {
                    if (isGunUsable(selectedGun, seat, entityTarget, engineTarget)) {
                        selectedGun.setAITarget(entityTarget, engineTarget);
                    }
                }
            }
            InterfaceManager.packetInterface.sendToAllClients(new PacketPartSeat(seat, bestGunItem, bestGunIndex, entityTarget, engineTarget));
            return true;
        }
        return false;
    }

    private static boolean isCurrentWeaponUsable(PartSeat seat) {
        List<PartGun> gunGroup = seat.gunGroups.get(seat.activeGunItem);
        if (gunGroup == null || gunGroup.isEmpty()) {
            return false;
        }
        if (seat.activeGunItem.definition.gun.fireSolo) {
            return seat.gunIndex >= 0 && seat.gunIndex < gunGroup.size() && isGunUsable(gunGroup.get(seat.gunIndex), seat, null, null);
        }
        for (PartGun candidate : gunGroup) {
            if (isGunUsable(candidate, seat, null, null)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGunUsable(PartGun candidate, PartSeat seat, IWrapperEntity entityTarget, PartEngine engineTarget) {
        if (!candidate.isValid || candidate.outOfHealth || !candidate.isActiveVar.isActive || candidate.isSpare || candidate.isReloading || candidate.getLoadedBulletCount() <= 0) {
            return false;
        }
        if (!isGunControlledBySeat(candidate, seat)) {
            return false;
        }
        if (entityTarget != null && entityTarget.isValid()) {
            return candidate.canAITarget(entityTarget);
        } else if (engineTarget != null && engineTarget.isValid) {
            return candidate.canAITarget(engineTarget);
        }
        return true;
    }

    private static boolean isGunControlledBySeat(PartGun candidate, PartSeat seat) {
        IWrapperEntity gunController = candidate.getGunController();
        return gunController != null && seat.rider.equals(gunController);
    }

    private static PartGun getSelectedTargetSource(PartSeat seat) {
        List<PartGun> gunGroup = seat.gunGroups.get(seat.activeGunItem);
        if (gunGroup == null || gunGroup.isEmpty()) {
            return null;
        }
        if (seat.activeGunItem.definition.gun.fireSolo) {
            if (seat.gunIndex >= 0 && seat.gunIndex < gunGroup.size()) {
                PartGun selectedGun = gunGroup.get(seat.gunIndex);
                return isGunControlledBySeat(selectedGun, seat) ? selectedGun : null;
            }
            return null;
        }
        for (PartGun selectedGun : gunGroup) {
            if (isGunControlledBySeat(selectedGun, seat) && (selectedGun.entityTarget != null || selectedGun.engineTarget != null)) {
                return selectedGun;
            }
        }
        return null;
    }

    /**
     * Finds the closest hostile that at least one weapon on this seat can engage.
     * This allows a gun with a restricted arc to hand control to another gun that can see the target.
     */
    private IWrapperEntity findAlternativeTarget(IWrapperEntity controller, PartSeat seat) {
        IWrapperEntity closestTarget = null;
        double closestDistance = Double.MAX_VALUE;
        for (IWrapperEntity entity : gun.world.getEntitiesHostile(controller, TARGET_SEARCH_RANGE)) {
            double distance = gun.position.distanceTo(entity.getPosition());
            if (distance < closestDistance && canAnyWeaponEngage(seat, entity)) {
                closestTarget = entity;
                closestDistance = distance;
            }
        }
        return closestTarget;
    }

    private static boolean canAnyWeaponEngage(PartSeat seat, IWrapperEntity target) {
        for (List<PartGun> gunGroup : seat.gunGroups.values()) {
            for (PartGun candidate : gunGroup) {
                if (isGunUsable(candidate, seat, target, null)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int getWeaponPriority(PartGun candidate, AITargetType targetType) {
        Bullet bullet = candidate.lastLoadedBullet != null ? candidate.lastLoadedBullet.definition.bullet : null;
        boolean smallCaliber = candidate.definition.gun.diameter < SMALL_CALIBER_CUTOFF_MM;
        boolean autocannon = !smallCaliber && !candidate.definition.gun.isSemiAuto && candidate.definition.gun.fireDelay <= AUTOCANNON_MAX_FIRE_DELAY_TICKS;
        boolean explosive = hasBulletType(bullet, BulletType.EXPLOSIVE);
        boolean armorPiercing = isArmorPiercing(bullet);
        boolean vehicleArmorPiercing = isVehicleArmorPiercing(bullet);

        switch (targetType) {
            case SOFT:
                if (smallCaliber) {
                    return 0;
                } else if (autocannon) {
                    return 1;
                } else if (explosive) {
                    return 2;
                } else if (armorPiercing) {
                    return 3;
                }
                return 4;
            case AIRCRAFT:
                if (candidate.definition.gun.targetType == TargetType.AIRCRAFT) {
                    if (isAntiAircraftAmmunition(bullet)) {
                        return 0;
                    } else if (explosive) {
                        return 1;
                    } else if (hasBulletType(bullet, BulletType.INCENDIARY)) {
                        return 2;
                    } else if (vehicleArmorPiercing) {
                        return 3;
                    }
                    return 4;
                } else if (isAntiAircraftAmmunition(bullet)) {
                    return 5;
                } else if (smallCaliber) {
                    return 10;
                }
                return 20;
            case GROUND_VEHICLE:
                if (vehicleArmorPiercing) {
                    return 0;
                } else if (explosive) {
                    return 1;
                }
                return 2;
            case NONE:
            default:
                return 0;
        }
    }

    private static boolean hasBulletType(Bullet bullet, BulletType type) {
        return bullet != null && bullet.types != null && bullet.types.contains(type);
    }

    private static boolean isArmorPiercing(Bullet bullet) {
        return bullet != null && (bullet.isHeat || bullet.armorPenetration > 0 || hasBulletType(bullet, BulletType.ARMOR_PIERCING));
    }

    private static boolean isVehicleArmorPiercing(Bullet bullet) {
        return bullet != null && (bullet.isHeat || bullet.armorPenetration > 0);
    }

    private static boolean isAntiAircraftAmmunition(Bullet bullet) {
        boolean explosive = hasBulletType(bullet, BulletType.EXPLOSIVE);
        return bullet != null && (bullet.turnRate > 0 || (explosive && (bullet.proximityFuze > 0 || bullet.airBurstDelay > 0 || bullet.blastDamageVsAircraft > 0)));
    }

    private static AITargetType getTargetType(IWrapperEntity entityTarget, PartEngine engineTarget) {
        if (entityTarget != null && entityTarget.isValid()) {
            AEntityB_Existing entityRiding = entityTarget.getEntityRiding();
            EntityVehicleF_Physics targetVehicle = null;
            if (entityRiding instanceof PartSeat) {
                targetVehicle = ((PartSeat) entityRiding).vehicleOn;
            } else if (entityRiding instanceof EntityVehicleF_Physics) {
                targetVehicle = (EntityVehicleF_Physics) entityRiding;
            }
            if (targetVehicle != null) {
                return targetVehicle.definition.motorized.isAircraft ? AITargetType.AIRCRAFT : AITargetType.GROUND_VEHICLE;
            }
            return AITargetType.SOFT;
        } else if (engineTarget != null && engineTarget.isValid && engineTarget.vehicleOn != null) {
            return engineTarget.vehicleOn.definition.motorized.isAircraft ? AITargetType.AIRCRAFT : AITargetType.GROUND_VEHICLE;
        }
        return AITargetType.NONE;
    }

    /**
     * Checks if the target is valid, inside the gun's movement bounds, and visible from the gun.
     * Also sets {@link #targetVector} and {@link #targetAngles} for aiming.
     */
    public boolean canAimAtTarget(IWrapperEntity target) {
        if (target.isValid()) {
            return canAimAtPosition(target.getPosition(), target.getBounds().heightRadius);
        }
        return false;
    }

    /**
     * Checks whether this gun can aim at a vehicle engine target.
     */
    public boolean canAimAtTarget(PartEngine target) {
        if (target != null && target.isValid) {
            return canAimAtPosition(target.position, 0);
        }
        return false;
    }

    private boolean canAimAtPosition(Point3D targetPosition, double targetHeightOffset) {
        //Get vector from gun center to target.
        //Target we aim for the middle, as it's more accurate.
        //We also take into account tracking for bullet speed.
        JSONMuzzle muzzleDef = gun.getActiveMuzzle();
        if (muzzleDef == null) {
            return false;
        }
        if (muzzleDef.center != null) {
            bulletPosition.set(muzzleDef.center);
        } else {
            bulletPosition.set(0, 0, 0);
        }
        bulletPosition.rotate(internalOrientation).add(gun.position);

        targetVector.set(targetPosition);
        targetVector.y += targetHeightOffset;
        targetVector.subtract(bulletPosition);

        //Transform vector to gun's coordinate system.
        //Get the angles the gun has to rotate to match the target.
        //If the are outside the gun's clamps, this isn't a valid target.
        targetAngles.set(targetVector).reOrigin(gun.zeroReferenceOrientation).getAngles(true);

        //Check yaw, if we need to.
        if (minYaw != -180 || maxYaw != 180) {
            if (targetAngles.y < minYaw || targetAngles.y > maxYaw) {
                return false;
            }
        }

        //Check pitch.
        if (targetAngles.x < minPitch || targetAngles.x > maxPitch) {
            return false;
        }

        //Check block raytracing.
        return gun.world.getBlockHit(bulletPosition, targetVector) == null;
    }

    private enum AITargetType {
        NONE,
        SOFT,
        AIRCRAFT,
        GROUND_VEHICLE
    }
}

package minecrafttransportsimulator.ai;

import minecrafttransportsimulator.entities.instances.AEntityVehicleE_Powered;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;

/**
 * Base class for AI controllers.  Contains common server-side vehicle controls
 * that mirror player inputs while keeping clients synchronized.
 */
public class AIBase {

    /**
     * Starts all stopped engines.  Ground vehicles are shifted into neutral first,
     * while aircraft retain their fixed forward gear so propellers stay connected.
     */
    protected final void startEngines(EntityVehicleF_Physics vehicle) {
        if (!canControl(vehicle)) {
            return;
        }

        boolean isAircraft = vehicle.definition.motorized.isAircraft;
        for (PartEngine engine : vehicle.engines) {
            if (!engine.running) {
                if (!isAircraft) {
                    clearGearRequests(engine);
                    engine.shiftNeutral();
                }

                boolean magnetoWasActive = engine.magnetoVar.isActive;
                boolean starterWasActive = engine.electricStarterVar.isActive;
                engine.autoStartEngine();
                if (magnetoWasActive != engine.magnetoVar.isActive || starterWasActive != engine.electricStarterVar.isActive) {
                    InterfaceManager.packetInterface.sendToAllClients(new PacketPartEngine(engine, Signal.AS_ON));
                }
            }
        }
    }

    /**Stops all engines and disengages any active electric starters.*/
    protected final void stopEngines(EntityVehicleF_Physics vehicle) {
        if (!canControl(vehicle)) {
            return;
        }

        for (PartEngine engine : vehicle.engines) {
            engine.magnetoVar.setActive(false, true);
            engine.electricStarterVar.setActive(false, true);
        }
    }

    /**Shifts every engine up one gear.  Aircraft do not have AI-controlled gears.*/
    protected final void shiftUp(EntityVehicleF_Physics vehicle) {
        if (!canShift(vehicle)) {
            return;
        }

        for (PartEngine engine : vehicle.engines) {
            clearGearRequests(engine);
            engine.shiftUp();
        }
    }

    /**Shifts every engine down one gear.  Aircraft do not have AI-controlled gears.*/
    protected final void shiftDown(EntityVehicleF_Physics vehicle) {
        if (!canShift(vehicle)) {
            return;
        }

        for (PartEngine engine : vehicle.engines) {
            clearGearRequests(engine);
            engine.shiftDown();
        }
    }

    /**Shifts every engine into neutral.  Aircraft retain their fixed forward gear.*/
    protected final void shiftNeutral(EntityVehicleF_Physics vehicle) {
        if (!canShift(vehicle)) {
            return;
        }

        for (PartEngine engine : vehicle.engines) {
            clearGearRequests(engine);
            engine.shiftNeutral();
        }
    }

    /**
     * Sets the throttle between zero and full power.  Ground AI should reset this
     * to zero when releasing the gas; aircraft retain the value until it is changed.
     */
    protected final void setThrottle(EntityVehicleF_Physics vehicle, double throttle) {
        if (canControl(vehicle)) {
            vehicle.throttleVar.setTo(clampControl(throttle, AEntityVehicleE_Powered.MAX_THROTTLE), true);
        }
    }

    /**Adjusts persistent throttle by the requested amount, clamped between zero and full power.*/
    protected final void adjustThrottle(EntityVehicleF_Physics vehicle, double adjustment) {
        if (canControl(vehicle) && !Double.isNaN(adjustment)) {
            vehicle.throttleVar.increment(adjustment, 0, AEntityVehicleE_Powered.MAX_THROTTLE, true);
        }
    }

    /**Sets service-brake pressure between zero and full braking.*/
    protected final void setBrake(EntityVehicleF_Physics vehicle, double brake) {
        if (canControl(vehicle)) {
            vehicle.brakeVar.setTo(clampControl(brake, EntityVehicleF_Physics.MAX_BRAKE), true);
        }
    }

    /**Sets the requested landing-gear state for aircraft equipped with retractable gear.*/
    protected final void setLandingGearRetracted(EntityVehicleF_Physics vehicle, boolean retracted) {
        if (canControl(vehicle) && vehicle.definition.motorized.isAircraft && vehicle.definition.motorized.gearSequenceDuration != 0) {
            vehicle.retractGearVar.setActive(retracted, true);
        }
    }

    /**Sets all standard light groups supported by the vehicle to the requested state.*/
    protected final void setLights(EntityVehicleF_Physics vehicle, boolean active) {
        if (!canControl(vehicle)) {
            return;
        }

        if (vehicle.definition.motorized.hasRunningLights) {
            vehicle.runningLightVar.setActive(active, true);
        }
        if (vehicle.definition.motorized.hasHeadlights) {
            vehicle.headLightVar.setActive(active, true);
        }
        if (vehicle.definition.motorized.hasNavLights) {
            vehicle.navigationLightVar.setActive(active, true);
        }
        if (vehicle.definition.motorized.hasStrobeLights) {
            vehicle.strobeLightVar.setActive(active, true);
        }
        if (vehicle.definition.motorized.hasTaxiLights) {
            vehicle.taxiLightVar.setActive(active, true);
        }
        if (vehicle.definition.motorized.hasLandingLights) {
            vehicle.landingLightVar.setActive(active, true);
        }
    }

    private static void clearGearRequests(PartEngine engine) {
        engine.shiftUpVar.setActive(false, true);
        engine.shiftDownVar.setActive(false, true);
        engine.shiftNeutralVar.setActive(false, true);
        engine.shiftSelectionVar.setTo(0, true);
    }

    private static boolean canShift(EntityVehicleF_Physics vehicle) {
        return canControl(vehicle) && !vehicle.definition.motorized.isAircraft;
    }

    private static boolean canControl(EntityVehicleF_Physics vehicle) {
        return vehicle != null && !vehicle.world.isClient();
    }

    private static double clampControl(double value, double maximum) {
        return Double.isNaN(value) ? 0 : Math.max(0, Math.min(value, maximum));
    }
}

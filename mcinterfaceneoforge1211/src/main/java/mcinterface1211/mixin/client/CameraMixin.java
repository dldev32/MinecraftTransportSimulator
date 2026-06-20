package mcinterface1211.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import minecrafttransportsimulator.baseclasses.Point3D;
import mcinterface1211.FMODWrapper;
import mcinterface1211.InterfaceEventsEntityRendering;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Invoker("setPosition")
    public abstract void invoke_setPosition(double pX, double pY, double pZ);

    private static final Point3D fmodListenerPosition = new Point3D();
    private static final Point3D fmodListenerVelocity = new Point3D();
    private static final Point3D fmodListenerForward = new Point3D();
    private static final Point3D fmodListenerUp = new Point3D();

    /**
     * In MC 1.21, Camera.setup() calls setPosition() AFTER the ComputeCameraAngles event,
     * overwriting the custom position MTS set during the event. We re-apply MTS's camera
     * position after setup() completes to fix the camera flying away from vehicles.
     */
    @Inject(method = "setup", at = @At("TAIL"))
    private void inject_ivCameraSetupTail(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (InterfaceEventsEntityRendering.adjustedCamera) {
            invoke_setPosition(
                InterfaceEventsEntityRendering.cameraAdjustedPosition.x,
                InterfaceEventsEntityRendering.cameraAdjustedPosition.y,
                InterfaceEventsEntityRendering.cameraAdjustedPosition.z
            );
        }

        if (entity != null) {
            Camera camera = (Camera) (Object) this;
            Vec3 position = camera.getPosition();
            Vec3 velocity = entity.getDeltaMovement();
            org.joml.Vector3f forward = camera.getLookVector();
            org.joml.Vector3f up = camera.getUpVector();
            fmodListenerPosition.set(position.x, position.y, position.z);
            fmodListenerVelocity.set(velocity.x, velocity.y, velocity.z);
            fmodListenerForward.set(forward.x(), forward.y(), forward.z()).normalize();
            fmodListenerUp.set(up.x(), up.y(), up.z()).normalize();
            FMODWrapper.updateRenderListener(fmodListenerPosition, fmodListenerVelocity, fmodListenerForward, fmodListenerUp);
        }
    }
}

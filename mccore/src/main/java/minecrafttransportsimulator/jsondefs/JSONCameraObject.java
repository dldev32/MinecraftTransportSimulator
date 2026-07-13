package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.baseclasses.RotationMatrix;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONCameraObject {
    @JSONRequired(dependentField = "cameraType", dependentValues = {"SIGHT"})
    @JSONDescription("The name of this optical sight.  Gun definitions use this value to select the sight associated with that weapon.  Multiple cameras may use the same name and will be cycled in definition order.")
    public String name;

    @JSONDescription("Defines how this camera is selected.  NORMAL cameras use the standard camera-cycle control.  SIGHT cameras are available only when the active gun references their name.  TRIPLEX cameras are always available through the optics-cycle control.")
    public CameraType cameraType;

    @JSONRequired
    @JSONDescription("An entry of x, y, and z coordinates that define the center point of where this camera will be located on the entity.  Note that FOV means this value may not be 100% accurate, so you may need to fudge this value to make things work.")
    public Point3D pos;

    @JSONDescription("This parameter is optional.  If included, it defines the x, y, and z rotations for this camera.")
    public RotationMatrix rot;

    @JSONDescription("This parameter is optional.  If included, MTS will set the player's FOV to this value when they are in this camera mode.  Useful for simulating zoom functions on scopes and sights.")
    public float fovOverride;

    @JSONDescription("This parameter is optional.  If included, MTS will set the player's mouse sensitivity to this value when they are in this camera mode.  Useful if you have a zoomed view and want to make aiming easier.")
    public float mouseSensitivityOverride;

    @JSONDescription("This parameter is optional.  If included, MTS will render the specified texture as an overlay when this camera is active.  This overlay will also disable the hotbar and cross-hair rendering.  The format is [packID:path/to/texture]")
    public String overlay;

    @JSONDescription("This parameter is optional.  If included, MTS will render the specified texture as a centered reticle over the camera overlay.  Unlike the overlay, this texture scales when a variable-magnification optic changes zoom.  The format is [packID:path/to/texture]")
    public String reticle;

    @JSONDescription("An optional ordered list of magnification values for variable-power optics.  Zoom controls move through this list.  The camera FOV is magnified relative to fovOverride (or the player's current FOV when no override is set), while only the separate reticle texture is scaled on-screen.")
    public List<Float> magnifications;

    @JSONDescription("If true, the player will have night vision with this overlay.")
    public boolean nightVision;

    @JSONDescription("If true, the camera will be considered to be interior.  Used in conjunction with sounds.")
    public boolean isInterior;

    @JSONDescription("A listing of one or more animation objects.  There are a few caveats with cameras, however:<br><br>Cameras do not support duration/delay, for obvious reasons.  This means that they do not support sounds, as those are tied to duration/delay code.<br><br>Cameras do not support the addPriorOffset flag, though they do support clamping.<br><br>Using the visibility animation will skip rendering the camera if the camera isn't 'visible'.  This can be used to dynamically enable cameras, such as those for active guns or vehicle components.  Note that the camera index will not increment during this, so if you stop rendering camera #1, then MTS will switch to camera #2 instead. This can be helpful if you want cameras to replace each other for specific actions.")
    public List<JSONAnimationDefinition> animations;

    public boolean isOptical() {
        return cameraType == CameraType.SIGHT || cameraType == CameraType.TRIPLEX;
    }

    public float getMagnification(int zoomIndex) {
        if (magnifications != null && !magnifications.isEmpty()) {
            float magnification = magnifications.get(Math.max(0, Math.min(zoomIndex, magnifications.size() - 1)));
            return magnification > 0 ? magnification : 1.0F;
        }
        return 1.0F;
    }

    public float getReticleScale(int zoomIndex) {
        return getMagnification(zoomIndex) / getMagnification(0);
    }

    public enum CameraType {
        NORMAL,
        SIGHT,
        TRIPLEX
    }
}

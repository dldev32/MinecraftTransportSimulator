package mcinterface1211.mixin.client;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import mcinterface1211.InterfaceRender;
import mcinterface1211.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    private RenderBuffers renderBuffers;

    /**
     * Need this to grab variables used in the level rendering routine for the rendering routine.
     * In 1.21, renderLevel signature changed: PoseStack removed, float partialTicks replaced by DeltaTracker,
     * and a second Matrix4f parameter was added.
     */
    @Inject(method = "renderLevel", at = @At(value = "HEAD"))
    public void inject_renderLevelDataGetter(DeltaTracker pDeltaTracker, boolean pDrawBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pModelViewMatrix, Matrix4f pProjection, CallbackInfo ci) {
        InterfaceRender.projectionMatrix = pProjection;
        InterfaceRender.viewMatrix = new Matrix4f(pModelViewMatrix);
    }

    /**
     * Need this to render solid entities if we aren't rendering the overriding one.
     */
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"))
    public void inject_renderLevelSolid(DeltaTracker pDeltaTracker, boolean pDrawBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pModelViewMatrix, Matrix4f pProjection, CallbackInfo ci) {
        if (ConfigSystem.settings.general.forceRenderLastSolid.value) {
            float pPartialTicks = pDeltaTracker.getGameTimeDeltaPartialTick(false);
            MultiBufferSource.BufferSource irendertypebuffer$impl = renderBuffers.bufferSource();
            //Set camera offset point for later.
            Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            InterfaceRender.renderCameraOffset.set(position.x, position.y, position.z);
            InterfaceRender.matrixStack = new PoseStack();
            InterfaceRender.renderBuffer = irendertypebuffer$impl;
            InterfaceRender.doRenderCall(false, pPartialTicks);
        }
    }

    /**
     * Render blended models and lights before translucent terrain and particles.
     * Drawing at TAIL would overlay nearer particles, which do not write depth.
     */
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;translucent()Lnet/minecraft/client/renderer/RenderType;"))
    public void inject_renderLevelBlended(DeltaTracker pDeltaTracker, boolean pDrawBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightmap, Matrix4f pModelViewMatrix, Matrix4f pProjection, CallbackInfo ci) {
        float pPartialTicks = pDeltaTracker.getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource irendertypebuffer$impl = renderBuffers.bufferSource();
        //Set camera offset point for later.
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        InterfaceRender.renderCameraOffset.set(position.x, position.y, position.z);
        InterfaceRender.matrixStack = new PoseStack();
        InterfaceRender.renderBuffer = irendertypebuffer$impl;

        //The camera view is already applied at this stage.  Fabulous's preceding depth
        //copy unbinds the framebuffer; bright blending must use the main scene as its destination.
        if (Minecraft.useShaderTransparency()) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }

        if (ConfigSystem.settings.general.forceRenderLastSolid.value) {
            InterfaceRender.doRenderCall(false, pPartialTicks);
        }
        InterfaceRender.doRenderCall(true, pPartialTicks);
        //Need to end batch after drawing translucents, otherwise they'll get other matrices applied.
        irendertypebuffer$impl.endBatch();
    }

    /**
     * This changes the heightmap of the rain checker to block rain from vehicles.
     * Better than trying to do block placement which has a host of issues.
     */
    @Redirect(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
    public int redirect_renderSnowAndRain(Level world, Heightmap.Types pHeightmapType, int pX, int pZ) {
        Point3D position = new Point3D(pX + 0.5, world.getHeight(Heightmap.Types.MOTION_BLOCKING, pX, pZ), pZ + 0.5);
        WrapperWorld.getWrapperFor(world).adjustHeightForRain(position);
        return (int) Math.ceil(position.y);
    }
}

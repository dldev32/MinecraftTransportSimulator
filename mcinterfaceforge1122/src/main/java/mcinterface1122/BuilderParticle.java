package mcinterface1122;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.particles.ParticleObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;

/** Lets Minecraft own the lifetime and rendering of pack-defined particles. */
public class BuilderParticle extends Particle {
    private final ParticleObject particle;
    private final TransformationMatrix transform = new TransformationMatrix();

    public BuilderParticle(ParticleObject particle) {
        super(((WrapperWorld) particle.world).world, particle.position.x, particle.position.y, particle.position.z);
        this.particle = particle;
    }

    @Override
    public void onUpdate() {
        if (Minecraft.getMinecraft().world != world) {
            setExpired();
            return;
        }
        particle.update();
        if (!particle.isValid) {
            setExpired();
            return;
        }
        prevPosX = particle.prevPosition.x;
        prevPosY = particle.prevPosition.y;
        prevPosZ = particle.prevPosition.z;
        setPosition(particle.position.x, particle.position.y, particle.position.z);
    }

    @Override
    public void setExpired() {
        super.setExpired();
        particle.remove();
    }

    @Override
    public int getFXLayer() {
        // Layer 3 gives custom particles ownership of their render state and vertices.
        return 3;
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity camera, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        if (!particle.isValid || Minecraft.getMinecraft().world != world) {
            return;
        }
        double cameraX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partialTicks;
        double cameraY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partialTicks;
        double cameraZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partialTicks;
        transform.resetTransforms().setTranslation(particle.position.x - cameraX, particle.position.y - cameraY, particle.position.z - cameraZ);
        float previousLightX = OpenGlHelper.lastBrightnessX;
        float previousLightY = OpenGlHelper.lastBrightnessY;
        boolean previousParticles = InterfaceRender.renderingParticles;
        try {
            InterfaceRender.renderingParticles = true;
            GlStateManager.alphaFunc(GL11.GL_GREATER, 1.0F / 255.0F);
            particle.render(transform, partialTicks);
        } finally {
            InterfaceRender.renderingParticles = previousParticles;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
            // Restore the same state expected after Minecraft's other layer-3 particles.
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.enableLighting();
        }
    }
}

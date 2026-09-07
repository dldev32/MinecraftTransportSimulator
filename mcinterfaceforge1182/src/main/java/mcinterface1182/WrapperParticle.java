package mcinterface1182;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;

import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.particles.ParticleObject;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.phys.Vec3;

/** Lets Minecraft own particle ticking, rendering, and lifetime limits. */
public class WrapperParticle extends Particle {
    private static final ParticleRenderType RENDER_TYPE = new ParticleRenderType() {
        private final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
        private final MultiBufferSource.BufferSource buffers = MultiBufferSource.immediateWithBuffers(builders, new BufferBuilder(256));
        private final MultiBufferSource particleBuffers = renderType -> {
            builders.computeIfAbsent(renderType, key -> new BufferBuilder(256));
            return buffers.getBuffer(renderType);
        };
        private final PoseStack particleMatrixStack = new PoseStack();
        private PoseStack previousMatrixStack;
        private MultiBufferSource previousRenderBuffer;
        private boolean previouslyRenderingParticles;

        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
            previousMatrixStack = InterfaceRender.matrixStack;
            previousRenderBuffer = InterfaceRender.renderBuffer;
            previouslyRenderingParticles = InterfaceRender.renderingParticles;
            //Minecraft already applies the camera view through RenderSystem's model-view matrix.
            InterfaceRender.matrixStack = particleMatrixStack;
            InterfaceRender.renderBuffer = particleBuffers;
            InterfaceRender.renderingParticles = true;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        @Override
        public void end(Tesselator tesselator) {
            try {
                //Solid particles must fill the depth buffer before translucent particles blend over them.
                for (RenderType renderType : builders.keySet()) {
                    if (!InterfaceRender.translucentParticleRenderTypes.contains(renderType)) {
                        buffers.endBatch(renderType);
                    }
                }
                for (RenderType renderType : builders.keySet()) {
                    if (InterfaceRender.translucentParticleRenderTypes.contains(renderType)) {
                        buffers.endBatch(renderType);
                    }
                }
            } finally {
                InterfaceRender.matrixStack = previousMatrixStack;
                InterfaceRender.renderBuffer = previousRenderBuffer;
                InterfaceRender.renderingParticles = previouslyRenderingParticles;
                //RenderType cleanup binds the main target; later native particles still need this target.
                if (Minecraft.useShaderTransparency()) {
                    Minecraft.getInstance().levelRenderer.getParticlesTarget().bindWrite(false);
                }
            }
        }

        @Override
        public String toString() {
            return "MTS_PARTICLES";
        }
    };

    private final ParticleObject particle;
    private final TransformationMatrix transform = new TransformationMatrix();

    public WrapperParticle(ParticleObject particle) {
        super((ClientLevel) ((WrapperWorld) particle.world).world, particle.position.x, particle.position.y, particle.position.z);
        this.particle = particle;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().level != level || !particle.isValid) {
            remove();
            return;
        }
        particle.update();
        if (!particle.isValid) {
            remove();
            return;
        }
        xo = particle.prevPosition.x;
        yo = particle.prevPosition.y;
        zo = particle.prevPosition.z;
        x = particle.position.x;
        y = particle.position.y;
        z = particle.position.z;
        age = (int) particle.ticksExisted;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        if (particle.isValid && Minecraft.getInstance().level == level) {
            Vec3 cameraPosition = camera.getPosition();
            transform.resetTransforms().setTranslation(particle.position.x - cameraPosition.x, particle.position.y - cameraPosition.y, particle.position.z - cameraPosition.z);
            particle.render(transform, partialTicks);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return RENDER_TYPE;
    }

    @Override
    public boolean shouldCull() {
        //Pack models and trails can extend well beyond the native particle bounding box.
        return false;
    }

    @Override
    public void remove() {
        super.remove();
        particle.remove();
    }
}

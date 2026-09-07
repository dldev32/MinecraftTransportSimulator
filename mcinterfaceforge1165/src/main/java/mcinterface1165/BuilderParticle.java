package mcinterface1165;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import minecrafttransportsimulator.baseclasses.TransformationMatrix;
import minecrafttransportsimulator.particles.ParticleObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Vector3d;

/** Lets Minecraft own the lifetime and rendering of pack-defined particles. */
public class BuilderParticle extends Particle {
    private static final MatrixStack PARTICLE_STACK = new MatrixStack();
    private static final Map<RenderType, BufferBuilder> PARTICLE_BUILDERS = new LinkedHashMap<>();
    private static final IRenderTypeBuffer.Impl PARTICLE_BUFFER = IRenderTypeBuffer.immediateWithBuffers(PARTICLE_BUILDERS, new BufferBuilder(256));
    private static final IRenderTypeBuffer PARTICLE_SOURCE = type -> {
        PARTICLE_BUILDERS.computeIfAbsent(type, key -> new BufferBuilder(256));
        return PARTICLE_BUFFER.getBuffer(type);
    };
    private static final IParticleRenderType RENDER_TYPE = new IParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
        }

        @Override
        public void end(Tessellator tessellator) {
            try {
                // Opaque particles must establish depth before smoke and other blended particles.
                for (RenderType type : PARTICLE_BUILDERS.keySet()) {
                    if (!InterfaceRender.translucentParticleRenderTypes.contains(type)) {
                        PARTICLE_BUFFER.endBatch(type);
                    }
                }
                for (RenderType type : PARTICLE_BUILDERS.keySet()) {
                    if (InterfaceRender.translucentParticleRenderTypes.contains(type)) {
                        PARTICLE_BUFFER.endBatch(type);
                    }
                }
            } finally {
                // RenderType cleanup binds the main target; later particles still need this target.
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

    public BuilderParticle(ParticleObject particle) {
        super((ClientWorld) ((WrapperWorld) particle.world).world, particle.position.x, particle.position.y, particle.position.z);
        this.particle = particle;
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().level != level) {
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
        setPos(particle.position.x, particle.position.y, particle.position.z);
    }

    @Override
    public void remove() {
        super.remove();
        particle.remove();
    }

    @Override
    public IParticleRenderType getRenderType() {
        return RENDER_TYPE;
    }

    @Override
    public boolean shouldCull() {
        // A trail or an animated model may extend well beyond the particle's origin.
        return false;
    }

    @Override
    public void render(IVertexBuilder buffer, ActiveRenderInfo camera, float partialTicks) {
        if (!particle.isValid || Minecraft.getInstance().level != level) {
            return;
        }
        Vector3d cameraPosition = camera.getPosition();
        transform.resetTransforms().setTranslation(particle.position.x - cameraPosition.x, particle.position.y - cameraPosition.y, particle.position.z - cameraPosition.z);
        MatrixStack previousStack = InterfaceRender.matrixStack;
        IRenderTypeBuffer previousBuffer = InterfaceRender.renderBuffer;
        boolean previousParticles = InterfaceRender.renderingParticles;
        try {
            // Minecraft has already applied the view matrix for this particle pass.
            InterfaceRender.matrixStack = PARTICLE_STACK;
            InterfaceRender.renderBuffer = PARTICLE_SOURCE;
            InterfaceRender.renderingParticles = true;
            particle.render(transform, partialTicks);
        } finally {
            InterfaceRender.matrixStack = previousStack;
            InterfaceRender.renderBuffer = previousBuffer;
            InterfaceRender.renderingParticles = previousParticles;
        }
    }
}

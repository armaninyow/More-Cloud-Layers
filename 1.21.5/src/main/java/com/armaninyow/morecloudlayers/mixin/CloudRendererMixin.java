package com.armaninyow.morecloudlayers.mixin;

import com.armaninyow.morecloudlayers.render.CloudLayerAlphaAccessor;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// vanilla's render() hardcodes alpha to 1.0F on setShaderColor, discarding our layer alpha - redirect it.
// Also redirects addVertex in buildFlatCell/buildExtrudedCell to scale X/Z per layer, since 1.21.5 has no transform-matrix seam like 1.21.6 does.
@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin implements CloudLayerAlphaAccessor {

	@Unique
	private float morecloudlayers$alpha = 1.0F;

	@Unique
	private float morecloudlayers$sizeMultiplier = 1.0F;

	@Override
	public void morecloudlayers$setAlpha(float alpha) {
		this.morecloudlayers$alpha = alpha;
	}

	@Override
	public void morecloudlayers$setSizeMultiplier(float sizeMultiplier) {
		this.morecloudlayers$sizeMultiplier = sizeMultiplier;
	}

	@Redirect(
		method = "render",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V")
	)
	private void morecloudlayers$redirectSetShaderColor(float r, float g, float b, float a) {
		RenderSystem.setShaderColor(r, g, b, this.morecloudlayers$alpha);
	}

	@Redirect(
		method = {"buildFlatCell", "buildExtrudedCell"},
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;")
	)
	private VertexConsumer morecloudlayers$redirectAddVertex(BufferBuilder builder, float x, float y, float z) {
		return builder.addVertex(x * this.morecloudlayers$sizeMultiplier, y, z * this.morecloudlayers$sizeMultiplier);
	}

	@Shadow
	private void draw(RenderPipeline pipeline, float p, float q, float r) {
		throw new AssertionError("Mixin shadow stub for draw should never actually run");
	}

	// keeps the in-cell (sub-rebuild) translation consistent with our scaled geometry - without this, movement between mesh rebuilds drifts/appears to follow the camera
	@Redirect(
		method = "render",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CloudRenderer;draw(Lcom/mojang/blaze3d/pipeline/RenderPipeline;FFF)V")
	)
	private void morecloudlayers$redirectDraw(CloudRenderer self, RenderPipeline pipeline, float p, float q, float r) {
		this.draw(pipeline, p * this.morecloudlayers$sizeMultiplier, q, r * this.morecloudlayers$sizeMultiplier);
	}
}
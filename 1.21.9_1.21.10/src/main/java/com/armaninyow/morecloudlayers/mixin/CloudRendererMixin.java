package com.armaninyow.morecloudlayers.mixin;

import com.armaninyow.morecloudlayers.render.CloudLayerAlphaAccessor;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.DynamicUniforms;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// vanilla's render() hardcodes alpha to 1.0F in the CloudInfo uniform - redirect it. Also redirects
// writeTransform to scale the model-view matrix per layer, since this is the only size-scale seam 1.21.6 exposes.
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
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putVec4(FFFF)Lcom/mojang/blaze3d/buffers/Std140Builder;")
	)
	private Std140Builder morecloudlayers$redirectPutVec4(Std140Builder builder, float r, float g, float b, float a) {
		return builder.putVec4(r, g, b, this.morecloudlayers$alpha);
	}

	@Redirect(
		method = "render",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;F)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;")
	)
	private GpuBufferSlice morecloudlayers$redirectWriteTransform(
		DynamicUniforms uniforms,
		Matrix4fc modelViewMatrix,
		Vector4fc colorModulator,
		Vector3fc modelOffset,
		Matrix4fc textureMatrix,
		float lineWidth
	) {
		Matrix4f scaledMatrix = new Matrix4f(modelViewMatrix);
		scaledMatrix.scale(this.morecloudlayers$sizeMultiplier, 1.0F, this.morecloudlayers$sizeMultiplier);
		return uniforms.writeTransform(scaledMatrix, colorModulator, modelOffset, textureMatrix, lineWidth);
	}
}
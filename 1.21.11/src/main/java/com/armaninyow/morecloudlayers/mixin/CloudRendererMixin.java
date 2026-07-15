package com.armaninyow.morecloudlayers.mixin;

import com.armaninyow.morecloudlayers.render.CloudHeightTracker;
import com.armaninyow.morecloudlayers.render.CloudLayerAlphaAccessor;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// vanilla's render() hardcodes alpha to 1.0F in the CloudInfo uniform - redirect it. Also redirects
// writeTransform to scale the model-view matrix per layer. 1.21.11 changed putVec4 to take a Vector4fc instead of 4 floats, and dropped lineWidth from writeTransform vs 1.21.6-1.21.9.
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

	// Captures the actual height this render() call received, regardless of whether
	// another mod (e.g. Sodium Extra) redirected the call site that produced it -
	// their redirect replaces the call to render() rather than reassigning a
	// variable, so this is the only reliable place to observe the true final value
	// without depending on that mod's classes. Read via CloudHeightTracker in
	// LevelRendererMixin, before our own extra layers render and overwrite it.
	@Inject(method = "render", at = @At("HEAD"))
	private void morecloudlayers$captureHeight(
		int color,
		CloudStatus cloudStatus,
		float cloudHeight,
		Vec3 camPos,
		long gameTime,
		float partialTick,
		CallbackInfo ci
	) {
		CloudHeightTracker.record(cloudHeight);
	}

	@Redirect(
		method = "render",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putVec4(Lorg/joml/Vector4fc;)Lcom/mojang/blaze3d/buffers/Std140Builder;")
	)
	private Std140Builder morecloudlayers$redirectPutVec4(Std140Builder builder, Vector4fc color) {
		return builder.putVec4(new Vector4f(color.x(), color.y(), color.z(), this.morecloudlayers$alpha));
	}

	@Redirect(
		method = "render",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;")
	)
	private GpuBufferSlice morecloudlayers$redirectWriteTransform(
		DynamicUniforms uniforms,
		Matrix4fc modelViewMatrix,
		Vector4fc colorModulator,
		Vector3fc modelOffset,
		Matrix4fc textureMatrix
	) {
		Matrix4f scaledMatrix = new Matrix4f(modelViewMatrix);
		scaledMatrix.scale(this.morecloudlayers$sizeMultiplier, 1.0F, this.morecloudlayers$sizeMultiplier);
		return uniforms.writeTransform(scaledMatrix, colorModulator, modelOffset, textureMatrix);
	}
}
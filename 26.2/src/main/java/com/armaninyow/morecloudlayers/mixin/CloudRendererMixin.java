package com.armaninyow.morecloudlayers.mixin;

import com.armaninyow.morecloudlayers.config.ModConfig;
import com.armaninyow.morecloudlayers.render.CloudHeightTracker;
import com.armaninyow.morecloudlayers.render.CloudLayerAlphaAccessor;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin implements CloudLayerAlphaAccessor {

	@Unique
	private float morecloudlayers$alpha = 1.0F;

	@Unique
	private float morecloudlayers$sizeMultiplier = 1.0F;

	@Unique
	private ModConfig.@Nullable CullPattern morecloudlayers$cullPattern = null;

	@Unique
	private int morecloudlayers$cullSlot = 0;

	@Shadow
	private CloudRenderer.TextureData texture;

	@Override
	public void morecloudlayers$setAlpha(float alpha) {
		this.morecloudlayers$alpha = alpha;
	}

	@Override
	public void morecloudlayers$setSizeMultiplier(float sizeMultiplier) {
		this.morecloudlayers$sizeMultiplier = sizeMultiplier;
	}

	@Override
	public void morecloudlayers$setCullPattern(ModConfig.@Nullable CullPattern pattern, int slot) {
		this.morecloudlayers$cullPattern = pattern;
		this.morecloudlayers$cullSlot = slot;
	}

	@Inject(method = "apply", at = @At("TAIL"))
	private void morecloudlayers$onApply(
		Optional<CloudRenderer.TextureData> preparations,
		ResourceManager manager,
		ProfilerFiller profiler,
		CallbackInfo ci
	) {
		if (this.morecloudlayers$cullPattern != null && this.texture != null) {
			this.texture = morecloudlayers$applyCullPattern(this.texture, this.morecloudlayers$cullPattern, this.morecloudlayers$cullSlot);
		}
	}

	@Unique
	private CloudRenderer.TextureData morecloudlayers$applyCullPattern(CloudRenderer.TextureData source, ModConfig.CullPattern pattern, int slot) {
		int width = source.width();
		int height = source.height();
		long[] cells = source.cells().clone();

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (morecloudlayers$isCulled(pattern, slot, x, y, width, height)) {
					cells[x + y * width] = 0L;
				}
			}
		}

		return new CloudRenderer.TextureData(cells, width, height);
	}

	@Unique
	private boolean morecloudlayers$isCulled(ModConfig.CullPattern pattern, int slot, int x, int y, int width, int height) {
		float u = (x + 0.5F) / width;
		float v = (y + 0.5F) / height;

		return switch (pattern) {
			case QUADRANT -> morecloudlayers$isCulledQuadrant(slot, u, v);
			case DIAGONAL_HALF -> morecloudlayers$isCulledDiagonalHalf(slot, u, v);
			case HALF -> morecloudlayers$isCulledHalf(slot, u, v);
			case EIGHTHS -> morecloudlayers$isCulledEighths(slot, x, y, width, height);
		};
	}

	@Unique
	private static boolean morecloudlayers$isCulledQuadrant(int slot, float u, float v) {
		boolean top = v < 0.5F;
		boolean left = u < 0.5F;

		return switch (((slot - 1) % 4) + 1) {
			case 1 -> top && !left;
			case 2 -> !top && !left;
			case 3 -> !top && left;
			default -> top && left;
		};
	}

	@Unique
	private static boolean morecloudlayers$isCulledDiagonalHalf(int slot, float u, float v) {
		return switch (((slot - 1) % 4) + 1) {
			case 1 -> v < u;
			case 2 -> u + v > 1.0F;
			case 3 -> v > u;
			default -> u + v < 1.0F;
		};
	}

	@Unique
	private static boolean morecloudlayers$isCulledHalf(int slot, float u, float v) {
		return switch (((slot - 1) % 4) + 1) {
			case 1 -> v < 0.5F;
			case 2 -> u >= 0.5F;
			case 3 -> v >= 0.5F;
			default -> u < 0.5F;
		};
	}

	@Unique
	private static boolean morecloudlayers$isCulledEighths(int slot, int x, int y, int width, int height) {
		boolean oddSlot = (slot % 2) != 0;
		if (oddSlot) {
			return y < height / 8;
		} else {
			return x < width / 8;
		}
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void morecloudlayers$captureHeight(
		int color,
		CloudStatus cloudStatus,
		float cloudHeight,
		int range,
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
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicUniforms;writeTransform(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;")
	)
	private GpuBufferSlice morecloudlayers$redirectWriteTransform(
		DynamicUniforms uniforms,
		Matrix4f modelViewMatrix
	) {
		Matrix4f scaledMatrix = new Matrix4f(modelViewMatrix);
		scaledMatrix.scale(this.morecloudlayers$sizeMultiplier, 1.0F, this.morecloudlayers$sizeMultiplier);
		return uniforms.writeTransform(scaledMatrix);
	}
}
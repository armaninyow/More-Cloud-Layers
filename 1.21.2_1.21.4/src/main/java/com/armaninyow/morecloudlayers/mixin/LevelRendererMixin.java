package com.armaninyow.morecloudlayers.mixin;

import com.armaninyow.morecloudlayers.config.CloudConfigManager;
import com.armaninyow.morecloudlayers.config.CloudLayerData;
import com.armaninyow.morecloudlayers.render.CloudLayerAlphaAccessor;
import com.armaninyow.morecloudlayers.render.CloudLayerDirtyMarker;
import com.mojang.blaze3d.resource.ResourceHandle;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// 1.21.2: hooks synthetic method_62205, draws extra CloudRenderer instances per layer
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements CloudLayerDirtyMarker {

	@Unique
	private final Map<Integer, CloudRenderer> morecloudlayers$layerRenderers = new HashMap<>();

	@Unique
	@Override
	public void morecloudlayers$markLayersDirty() {
		for (CloudRenderer renderer : morecloudlayers$layerRenderers.values()) {
			renderer.markForRebuild();
		}
	}

	@Inject(method = "method_62205", at = @At("TAIL"))
	private void morecloudlayers$renderExtraLayers(
		ResourceHandle<?> targetHandle,
		int color,
		CloudStatus cloudStatus,
		float cloudHeight,
		Matrix4f matrix4f,
		Matrix4f matrix4f2,
		Vec3 camPos,
		float time,
		CallbackInfo ci
	) {
		if (cloudStatus == CloudStatus.OFF) {
			return;
		}

		for (CloudLayerData layerData : CloudConfigManager.getInstance().getActiveLayers()) {
			morecloudlayers$renderLayer(color, cloudStatus, cloudHeight, matrix4f, matrix4f2, camPos, time, layerData);
		}
	}

	@Unique
	private void morecloudlayers$renderLayer(
		int baseColor,
		CloudStatus cloudStatus,
		float baseCloudHeight,
		Matrix4f matrix4f,
		Matrix4f matrix4f2,
		Vec3 camPos,
		float time,
		CloudLayerData layerData
	) {
		boolean isNew = !morecloudlayers$layerRenderers.containsKey(layerData.index());
		CloudRenderer renderer = morecloudlayers$layerRenderers.computeIfAbsent(layerData.index(), k -> new CloudRenderer());

		if (isNew) {
			morecloudlayers$loadTexture(renderer);
		}

		// alpha channel of color int is discarded by vanilla's render(), set via redirect mixin instead
		((CloudLayerAlphaAccessor) renderer).morecloudlayers$setAlpha(layerData.alpha());

		float layerHeight = baseCloudHeight + layerData.index() * 16.0F;

		// unverified approximation, watch for jitter/incorrect scroll speed
		float scaledTime = time * layerData.speedMultiplier();

		// shift x per layer so each CloudRenderer samples a different texture region - floorMod in buildMesh wraps this safely
		Vec3 offsetCamPos = camPos.add(384.0 * layerData.index(), 0.0, 0.0);

		// copy before scaling so vanilla's own matrix isn't mutated for later layers/rest of frame
		Matrix4f scaledMatrix = new Matrix4f(matrix4f);
		scaledMatrix.scale(layerData.sizeMultiplier(), 1.0F, layerData.sizeMultiplier());

		renderer.render(baseColor, cloudStatus, layerHeight, scaledMatrix, matrix4f2, offsetCamPos, scaledTime);
	}

	@Unique
	private void morecloudlayers$loadTexture(CloudRenderer renderer) {
		PreparableReloadListener.PreparationBarrier barrier = new PreparableReloadListener.PreparationBarrier() {
			@Override
			public <T> CompletableFuture<T> wait(T data) {
				return CompletableFuture.completedFuture(data);
			}
		};

		renderer.reload(barrier, Minecraft.getInstance().getResourceManager(), Runnable::run, Runnable::run);
	}
}
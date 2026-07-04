package com.armaninyow.morecloudlayers.mixin;

import com.armaninyow.morecloudlayers.config.CloudConfigManager;
import com.armaninyow.morecloudlayers.config.CloudLayerData;
import com.armaninyow.morecloudlayers.render.CloudLayerAlphaAccessor;
import com.armaninyow.morecloudlayers.render.CloudLayerDirtyMarker;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// 1.21.11: same as 1.21.9 but render()/method_62205 split time into (long gameTime, float partialTick) to avoid float precision loss over long play sessions
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
		int color,
		CloudStatus cloudStatus,
		float cloudHeight,
		Vec3 camPos,
		long gameTime,
		float partialTick,
		CallbackInfo ci
	) {
		if (cloudStatus == CloudStatus.OFF) {
			return;
		}

		for (CloudLayerData layerData : CloudConfigManager.getInstance().getActiveLayers()) {
			morecloudlayers$renderLayer(color, cloudStatus, cloudHeight, camPos, gameTime, partialTick, layerData);
		}
	}

	@Unique
	private void morecloudlayers$renderLayer(
		int baseColor,
		CloudStatus cloudStatus,
		float baseCloudHeight,
		Vec3 camPos,
		long gameTime,
		float partialTick,
		CloudLayerData layerData
	) {
		boolean isNew = !morecloudlayers$layerRenderers.containsKey(layerData.index());
		CloudRenderer renderer = morecloudlayers$layerRenderers.computeIfAbsent(layerData.index(), k -> new CloudRenderer());

		if (isNew) {
			morecloudlayers$loadTexture(renderer);
		}

		// alpha channel of color int is discarded by vanilla's render(), set via redirect mixin instead
		((CloudLayerAlphaAccessor) renderer).morecloudlayers$setAlpha(layerData.alpha());

		// no scale seam exists in this version's render pipeline - no-op on 1.21.5, applied via writeTransform redirect on 1.21.6+
		((CloudLayerAlphaAccessor) renderer).morecloudlayers$setSizeMultiplier(layerData.sizeMultiplier());

		float layerHeight = baseCloudHeight + layerData.index() * 16.0F;

		// unverified approximation - scaling gameTime through double then back to long reintroduces the precision loss this long/float split was meant to avoid, though negligible at realistic play-session lengths
		long scaledGameTime = (long) (gameTime * layerData.speedMultiplier());

		// shift x per layer so each CloudRenderer samples a different texture region - floorMod in buildMesh wraps this safely
		Vec3 offsetCamPos = camPos.add(384.0 * layerData.index(), 0.0, 0.0);

		renderer.render(baseColor, cloudStatus, layerHeight, offsetCamPos, scaledGameTime, partialTick);
	}

	@Unique
	private void morecloudlayers$loadTexture(CloudRenderer renderer) {
		PreparableReloadListener.PreparationBarrier barrier = new PreparableReloadListener.PreparationBarrier() {
			@Override
			public <T> CompletableFuture<T> wait(T data) {
				return CompletableFuture.completedFuture(data);
			}
		};

		PreparableReloadListener.SharedState sharedState = new PreparableReloadListener.SharedState(Minecraft.getInstance().getResourceManager());

		renderer.reload(sharedState, Runnable::run, barrier, Runnable::run);
	}
}
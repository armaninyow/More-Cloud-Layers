package com.armaninyow.morecloudlayers.mixin;

import com.armaninyow.morecloudlayers.config.CloudConfigManager;
import com.armaninyow.morecloudlayers.config.CloudLayerData;
import com.armaninyow.morecloudlayers.config.ModConfig;
import com.armaninyow.morecloudlayers.render.CloudHeightTracker;
import com.armaninyow.morecloudlayers.render.CloudLayerAlphaAccessor;
import com.armaninyow.morecloudlayers.render.CloudLayerDirtyMarker;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements CloudLayerDirtyMarker {

	@Unique
	private final Map<Integer, CloudRenderer> morecloudlayers$layerRenderers = new HashMap<>();

	@Inject(method = "endFrame", at = @At("TAIL"))
	private void morecloudlayers$endFrameExtraLayers(CallbackInfo ci) {
		for (CloudRenderer renderer : morecloudlayers$layerRenderers.values()) {
			renderer.endFrame();
		}
	}

	@Unique
	@Override
	public void morecloudlayers$markLayersDirty() {
		ModConfig config = CloudConfigManager.getInstance().getConfig();

		for (Map.Entry<Integer, CloudRenderer> entry : morecloudlayers$layerRenderers.entrySet()) {
			int slot = entry.getKey();
			CloudRenderer renderer = entry.getValue();

			((CloudLayerAlphaAccessor) renderer).morecloudlayers$setCullPattern(
				config.layerCullingEnabled ? config.layerCullingPattern : null, slot
			);
			morecloudlayers$loadTexture(renderer);

			renderer.markForRebuild();
		}
	}

	@Redirect(
		method = "addCloudsPass",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V"
		)
	)
	private void morecloudlayers$redirectCloudsExecutes(
		FramePass pass,
		Runnable vanillaTask,
		FrameGraphBuilder frame,
		CloudStatus cloudStatus,
		Vec3 camPos,
		long gameTime,
		float partialTick,
		int cloudColor,
		float cloudHeight,
		int cloudRange
	) {
		pass.executes(() -> {
			vanillaTask.run();

			if (cloudStatus == CloudStatus.OFF) {
				return;
			}

			float resolvedCloudHeight = CloudHeightTracker.getLastRenderedHeight(cloudHeight);

			for (CloudLayerData layerData : CloudConfigManager.getInstance().getActiveLayers()) {
				morecloudlayers$renderLayer(cloudColor, cloudStatus, resolvedCloudHeight, cloudRange, camPos, gameTime, partialTick, layerData);
			}
		});
	}

	@Unique
	private void morecloudlayers$renderLayer(
		int baseColor,
		CloudStatus cloudStatus,
		float baseCloudHeight,
		int cloudRange,
		Vec3 camPos,
		long gameTime,
		float partialTick,
		CloudLayerData layerData
	) {
		boolean isNew = !morecloudlayers$layerRenderers.containsKey(layerData.index());
		CloudRenderer renderer = morecloudlayers$layerRenderers.computeIfAbsent(layerData.index(), k -> new CloudRenderer());

		if (isNew) {
			ModConfig config = CloudConfigManager.getInstance().getConfig();
			((CloudLayerAlphaAccessor) renderer).morecloudlayers$setCullPattern(
				config.layerCullingEnabled ? config.layerCullingPattern : null, layerData.index()
			);
			morecloudlayers$loadTexture(renderer);
		}

		((CloudLayerAlphaAccessor) renderer).morecloudlayers$setAlpha(layerData.alpha());

		((CloudLayerAlphaAccessor) renderer).morecloudlayers$setSizeMultiplier(layerData.sizeMultiplier());

		float layerHeight = baseCloudHeight + layerData.index() * 16.0F;

		long scaledGameTime = (long) (gameTime * layerData.speedMultiplier());

		Vec3 offsetCamPos = camPos.add(384.0 * layerData.index(), 0.0, 0.0);

		renderer.render(baseColor, cloudStatus, layerHeight, cloudRange, offsetCamPos, scaledGameTime, partialTick);
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
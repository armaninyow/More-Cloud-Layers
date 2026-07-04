package com.armaninyow.morecloudlayers.mixin;

import com.armaninyow.morecloudlayers.config.CloudConfigManager;
import com.armaninyow.morecloudlayers.config.CloudLayerData;
import com.armaninyow.morecloudlayers.render.CloudLayerDirtyMarker;
import com.armaninyow.morecloudlayers.render.ExtraCloudLayerState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements CloudLayerDirtyMarker {

	@Shadow @Final private Minecraft minecraft;

	@Shadow @Nullable private ClientLevel level;

	@Shadow private int ticks;

	@Shadow
	private MeshData buildClouds(Tesselator tesselator, double d, double e, double f, Vec3 vec3) {
		throw new AssertionError("Mixin shadow stub for buildClouds should never actually run");
	}

	@Unique
	private final Map<Integer, ExtraCloudLayerState> morecloudlayers$layerStates = new HashMap<>();

	/**
	 * Marks every extra layer's cache dirty, forcing a rebuild (and, in Random mode,
	 * a fresh roll) on the next renderClouds call. Called from the three lifecycle
	 * triggers - never per-frame.
	 */
	@Unique
	@Override
	public void morecloudlayers$markLayersDirty() {
		for (ExtraCloudLayerState state : morecloudlayers$layerStates.values()) {
			state.dirty = true;
		}
	}

	@Inject(method = "renderClouds", at = @At("TAIL"))
	private void morecloudlayers$renderExtraLayers(
		PoseStack poseStack, Matrix4f matrix4f, Matrix4f matrix4f2, float partialTick, double camX, double camY, double camZ, CallbackInfo ci
	) {
		if (this.level == null) {
			return;
		}

		float baseHeight = this.level.effects().getCloudHeight();
		if (Float.isNaN(baseHeight)) {
			// No clouds in this dimension (Nether / End) - matches vanilla's own guard.
			return;
		}

		for (CloudLayerData layerData : CloudConfigManager.getInstance().getActiveLayers()) {
			morecloudlayers$renderLayer(poseStack, matrix4f, matrix4f2, partialTick, camX, camY, camZ, baseHeight, layerData);
		}
	}

	@Unique
	private void morecloudlayers$renderLayer(
		PoseStack poseStack,
		Matrix4f matrix4f,
		Matrix4f matrix4f2,
		float partialTick,
		double camX,
		double camY,
		double camZ,
		float baseHeight,
		CloudLayerData layerData
	) {
		float layerHeight = baseHeight + layerData.index() * 16.0F;
		float speedMult = layerData.speedMultiplier();
		float sizeMult = layerData.sizeMultiplier();

		// Straight port of vanilla LevelRenderer#renderClouds, parameterized only by
		// height/speed/size. Rotation was attempted here (twice) and dropped after it
		// reintroduced a follow bug both times. This offset approach is different in
		// kind, not just degree: it's a plain additive constant on the Z texture
		// coordinate only, applied AFTER scroll/translate is fully computed from the
		// untouched m/n/o - so it can only ever shift which slice of the repeating
		// 256x256 clouds.png each layer samples, never touch movement or precision.
		double l = (this.ticks + partialTick) * 0.03F * speedMult;
		double m = (camX + l) / 12.0;
		double n = layerHeight - camY + 0.33F;
		double o = camZ / 12.0 + 0.33F;
		m -= Mth.floor(m / 2048.0) * 2048;
		o -= Mth.floor(o / 2048.0) * 2048;
		float p = (float) (m - Mth.floor(m));
		float q = (float) (n / 4.0 - Mth.floor(n / 4.0)) * 4.0F;
		float r = (float) (o - Mth.floor(o));

		// One integer step here = one texel of the 256-wide texture (buildClouds turns
		// floor(f) directly into a UV offset via *1/256). 32 * layerIndex cycles
		// through 8 distinct slices before wrapping back to the same slice as vanilla
		// at layer 8. Only affects which pixels of clouds.png are sampled for the
		// mesh built below - p/q/r (the actual on-screen scroll) are already fixed
		// above and never reference this value.
		double textureO = (o + 32.0 * layerData.index()) % 256.0;

		Vec3 cloudColor = this.level.getCloudColor(partialTick);
		CloudStatus cloudsType = this.minecraft.options.getCloudsType();

		int s = (int) Math.floor(m);
		int t = (int) Math.floor(n / 4.0);
		int u = (int) Math.floor(textureO);

		ExtraCloudLayerState state = morecloudlayers$layerStates.computeIfAbsent(layerData.index(), k -> new ExtraCloudLayerState());

		boolean needsRebuild = state.dirty
			|| s != state.prevX
			|| t != state.prevY
			|| u != state.prevZ
			|| cloudsType != state.prevType
			|| state.prevColor == null
			|| state.prevColor.distanceToSqr(cloudColor) > 2.0E-4;

		if (needsRebuild) {
			state.dirty = false;
			state.prevX = s;
			state.prevY = t;
			state.prevZ = u;
			state.prevColor = cloudColor;
			state.prevType = cloudsType;

			state.close();
			state.buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
			state.buffer.bind();
			// Relies on vanilla's own prevCloudsType field (set earlier this same frame
			// by vanilla's renderClouds body) to pick the Fast/Fancy vertex layout -
			// keeps our layers in sync with the graphics-mode setting automatically.
			state.buffer.upload(this.buildClouds(Tesselator.getInstance(), m, n, textureO, cloudColor));
			VertexBuffer.unbind();
		}

		if (state.buffer == null) {
			return;
		}

		FogRenderer.levelFogColor();
		poseStack.pushPose();
		poseStack.mulPose(matrix4f);
		poseStack.scale(12.0F * sizeMult, 1.0F, 12.0F * sizeMult);
		poseStack.translate(-p, q, -r);

		state.buffer.bind();
		int startPass = cloudsType == CloudStatus.FANCY ? 0 : 1;
		// setShaderColor is a global multiplier applied on top of buildClouds' own baked
		// vertex alpha (0.8F) - same mechanism vanilla itself uses for sky/sun/moon
		// tinting. Reset to (1,1,1,1) immediately after so it can't affect anything
		// rendered later this frame.
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, layerData.alpha());
		for (int pass = startPass; pass < 2; pass++) {
			RenderType renderType = pass == 0 ? RenderType.cloudsDepthOnly() : RenderType.clouds();
			renderType.setupRenderState();
			ShaderInstance shaderInstance = RenderSystem.getShader();
			state.buffer.drawWithShader(poseStack.last().pose(), matrix4f2, shaderInstance);
			renderType.clearRenderState();
		}
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		VertexBuffer.unbind();

		poseStack.popPose();
	}
}
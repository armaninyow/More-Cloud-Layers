package com.armaninyow.morecloudlayers.mixin;

import com.armaninyow.morecloudlayers.render.CloudLayerAlphaAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// vanilla's render() always calls setShaderColor with alpha hardcoded to 1.0F, discarding our layer alpha entirely - redirect it
@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin implements CloudLayerAlphaAccessor {

	@Unique
	private float morecloudlayers$alpha = 1.0F;

	@Override
	public void morecloudlayers$setAlpha(float alpha) {
		this.morecloudlayers$alpha = alpha;
	}

	@Redirect(
		method = "render",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V")
	)
	private void morecloudlayers$redirectSetShaderColor(float r, float g, float b, float a) {
		RenderSystem.setShaderColor(r, g, b, this.morecloudlayers$alpha);
	}
}
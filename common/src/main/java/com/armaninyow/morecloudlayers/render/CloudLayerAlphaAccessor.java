package com.armaninyow.morecloudlayers.render;

import com.armaninyow.morecloudlayers.config.ModConfig;

public interface CloudLayerAlphaAccessor {
	void morecloudlayers$setAlpha(float alpha);

	default void morecloudlayers$setSizeMultiplier(float sizeMultiplier) {
	}

	default void morecloudlayers$setCullPattern(ModConfig.CullPattern pattern, int slot) {
	}
}
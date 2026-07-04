package com.armaninyow.morecloudlayers.render;

// implemented by CloudRendererMixin - lets us override values vanilla's render() hardcodes or has no per-call param for
public interface CloudLayerAlphaAccessor {
	void morecloudlayers$setAlpha(float alpha);

	// no-op default - only 1.21.6 has a matrix seam to actually apply this; 1.21.5 has no scale seam in its render pipeline
	default void morecloudlayers$setSizeMultiplier(float sizeMultiplier) {
	}
}
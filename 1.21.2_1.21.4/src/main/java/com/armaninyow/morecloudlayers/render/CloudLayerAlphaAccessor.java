package com.armaninyow.morecloudlayers.render;

// implemented by CloudRendererMixin - lets us override the alpha vanilla's render() hardcodes to 1.0F
public interface CloudLayerAlphaAccessor {
	void morecloudlayers$setAlpha(float alpha);
}
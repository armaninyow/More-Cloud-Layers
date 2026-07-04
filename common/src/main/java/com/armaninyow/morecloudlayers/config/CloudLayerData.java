package com.armaninyow.morecloudlayers.config;

/**
 * Cached, resolved values for a single extra cloud layer.
 * index: 1-based, since layer 0 is the vanilla cloud layer (not tracked here).
 * speedMultiplier / sizeMultiplier: 1.0F = 100%.
 * alpha: 1.0F = fully opaque (matches vanilla). When Random Transparency is enabled
 * in config, this is a random value between 0.25F and 0.75F, re-rolled only on the
 * three lifecycle triggers (world join, dimension change, wake up) - never per-frame.
 */
public record CloudLayerData(int index, float speedMultiplier, float sizeMultiplier, float alpha) {
}
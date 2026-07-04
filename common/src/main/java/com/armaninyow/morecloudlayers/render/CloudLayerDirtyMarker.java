package com.armaninyow.morecloudlayers.render;

/**
 * Implemented by LevelRendererMixin. Cast Minecraft.getInstance().levelRenderer
 * to this to force a re-roll/recalculation of extra cloud layers from outside
 * the mixin package (e.g. from the network payload receiver, world-join event,
 * or dimension-change detection).
 */
public interface CloudLayerDirtyMarker {
	void morecloudlayers$markLayersDirty();
}
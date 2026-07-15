package com.armaninyow.morecloudlayers.render;

public final class CloudHeightTracker {
	private static volatile float lastRenderedHeight = Float.NaN;

	private CloudHeightTracker() {
	}

	public static void record(float height) {
		lastRenderedHeight = height;
	}

	public static float getLastRenderedHeight(float fallback) {
		return Float.isNaN(lastRenderedHeight) ? fallback : lastRenderedHeight;
	}
}
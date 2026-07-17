package com.armaninyow.morecloudlayers.config;

public class ModConfig {
	public enum Mode {
		LINEAR,
		RANDOM
	}

	public enum CullPattern {
		QUADRANT,
		DIAGONAL_HALF,
		HALF,
		EIGHTHS
	}

	public int layerCount = 2;
	public Mode speedMode = Mode.LINEAR;
	public int speedDropPercent = 5;
	public Mode sizeMode = Mode.LINEAR;
	public int sizeDropPercent = 4;
	public boolean randomTransparency = true;

	public boolean randomLayerPositions = false;

	public boolean layerCullingEnabled = false;
	public CullPattern layerCullingPattern = CullPattern.QUADRANT;

	public ModConfig copy() {
		ModConfig c = new ModConfig();
		c.layerCount = this.layerCount;
		c.speedMode = this.speedMode;
		c.speedDropPercent = this.speedDropPercent;
		c.sizeMode = this.sizeMode;
		c.sizeDropPercent = this.sizeDropPercent;
		c.randomTransparency = this.randomTransparency;
		c.randomLayerPositions = this.randomLayerPositions;
		c.layerCullingEnabled = this.layerCullingEnabled;
		c.layerCullingPattern = this.layerCullingPattern;
		return c;
	}
}
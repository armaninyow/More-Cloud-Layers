package com.armaninyow.morecloudlayers.config;

public class ModConfig {
	public enum Mode {
		LINEAR,
		RANDOM
	}

	public int layerCount = 2;              // 1-8
	public Mode speedMode = Mode.LINEAR;
	public int speedDropPercent = 5;         // 1-10, ignored when speedMode == RANDOM
	public Mode sizeMode = Mode.LINEAR;
	public int sizeDropPercent = 4;          // 1-10, ignored when sizeMode == RANDOM
	public boolean randomTransparency = true; // when true, each layer gets a random 25%-75% alpha, re-rolled on the 3 lifecycle triggers

	public ModConfig copy() {
		ModConfig c = new ModConfig();
		c.layerCount = this.layerCount;
		c.speedMode = this.speedMode;
		c.speedDropPercent = this.speedDropPercent;
		c.sizeMode = this.sizeMode;
		c.sizeDropPercent = this.sizeDropPercent;
		c.randomTransparency = this.randomTransparency;
		return c;
	}
}
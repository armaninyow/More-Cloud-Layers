package com.armaninyow.morecloudlayers.config;

import com.armaninyow.morecloudlayers.MoreCloudLayers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Holds the persisted config and the cached, resolved per-layer values.
 * recalculate() is the only place layer values change, and it is only meant
 * to be called from the three lifecycle triggers (world join, dimension change, wake up).
 */
public final class CloudConfigManager {
	private static final CloudConfigManager INSTANCE = new CloudConfigManager();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = MoreCloudLayers.MOD_ID + ".json";

	private ModConfig config = new ModConfig();
	private List<CloudLayerData> activeLayers = Collections.emptyList();

	private CloudConfigManager() {
	}

	public static CloudConfigManager getInstance() {
		return INSTANCE;
	}

	public ModConfig getConfig() {
		return config;
	}

	/**
	 * Replaces the config (e.g. after the user saves the Cloth Config screen) and
	 * immediately recalculates, since layer count / mode changes should apply right away.
	 */
	public void setConfig(ModConfig newConfig) {
		this.config = newConfig;
		save();
		recalculate();
	}

	public List<CloudLayerData> getActiveLayers() {
		return activeLayers;
	}

	private Path getConfigPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	public void load() {
		Path path = getConfigPath();
		if (!Files.exists(path)) {
			save();
			recalculate();
			return;
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
			this.config = loaded != null ? loaded : new ModConfig();
		} catch (IOException e) {
			MoreCloudLayers.LOGGER.warn("Failed to load {} config, using defaults", MoreCloudLayers.MOD_ID, e);
			this.config = new ModConfig();
		}

		recalculate();
	}

	public void save() {
		Path path = getConfigPath();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException e) {
			MoreCloudLayers.LOGGER.warn("Failed to save {} config", MoreCloudLayers.MOD_ID, e);
		}
	}

	/**
	 * Recomputes the cached per-layer speed/size values.
	 * Must only be called from: world join, dimension change to Overworld, or the
	 * server's night-skip wake-up event - never per-frame.
	 */
	public void recalculate() {
		int count = Math.max(1, Math.min(8, config.layerCount));
		List<CloudLayerData> layers = new ArrayList<>(count);

		for (int i = 1; i <= count; i++) {
			float speed = resolveValue(config.speedMode, config.speedDropPercent, i);
			float size = resolveValue(config.sizeMode, config.sizeDropPercent, i);
			float alpha = config.randomTransparency
                ? 0.75F + ThreadLocalRandom.current().nextFloat() * 0.25F // 75%-100%
				: 1.0F;
			layers.add(new CloudLayerData(i, speed, size, alpha));
		}

		this.activeLayers = Collections.unmodifiableList(layers);
	}

	private float resolveValue(ModConfig.Mode mode, int dropPercent, int layerIndex) {
		if (mode == ModConfig.Mode.RANDOM) {
			// Random between 30% and 100%, re-rolled every recalculate() call.
			return 0.30F + ThreadLocalRandom.current().nextFloat() * 0.70F;
		}

		// Linear: 100% - (drop% * layerIndex), expressed as a 0.0-1.0 multiplier.
		float value = 1.0F - (dropPercent / 100.0F) * layerIndex;
		return Math.max(0.0F, value);
	}
}
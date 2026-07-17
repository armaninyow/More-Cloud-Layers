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

public final class CloudConfigManager {
	private static final CloudConfigManager INSTANCE = new CloudConfigManager();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = MoreCloudLayers.MOD_ID + ".json";
	private static final int MAX_SLOTS = 8;
	private static final int MAX_SHUFFLE_ATTEMPTS = 200;

	private ModConfig config = new ModConfig();
	private List<CloudLayerData> activeLayers = Collections.emptyList();

	private int[] previousSlots = null;

	private CloudConfigManager() {
	}

	public static CloudConfigManager getInstance() {
		return INSTANCE;
	}

	public ModConfig getConfig() {
		return config;
	}

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

		normalizeConfig();
		recalculate();
	}

	private void normalizeConfig() {
		if (config.speedMode == null) {
			config.speedMode = ModConfig.Mode.LINEAR;
		}
		if (config.sizeMode == null) {
			config.sizeMode = ModConfig.Mode.LINEAR;
		}
		if (config.layerCullingPattern == null) {
			config.layerCullingPattern = ModConfig.CullPattern.QUADRANT;
		}
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

	public void recalculate() {
		int count = Math.max(1, Math.min(MAX_SLOTS, config.layerCount));
		int[] slots = resolveSlotAssignment(count);
		List<CloudLayerData> layers = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			int slot = slots[i];
			float speed = resolveValue(config.speedMode, config.speedDropPercent, slot);
			float size = resolveValue(config.sizeMode, config.sizeDropPercent, slot);
			float alpha = config.randomTransparency
                ? 0.75F + ThreadLocalRandom.current().nextFloat() * 0.25F
				: 1.0F;
			layers.add(new CloudLayerData(slot, speed, size, alpha));
		}

		this.activeLayers = Collections.unmodifiableList(layers);
		this.previousSlots = slots;
	}

	private int[] resolveSlotAssignment(int count) {
		if (!config.randomLayerPositions || count >= MAX_SLOTS) {
			int[] sequential = new int[count];
			for (int i = 0; i < count; i++) {
				sequential[i] = i + 1;
			}
			return sequential;
		}

		int[] previous = (previousSlots != null && previousSlots.length == count) ? previousSlots : null;

		List<Integer> pool = new ArrayList<>(MAX_SLOTS);
		for (int slot = 1; slot <= MAX_SLOTS; slot++) {
			pool.add(slot);
		}

		int[] assignment = new int[count];

		for (int attempt = 0; attempt < MAX_SHUFFLE_ATTEMPTS; attempt++) {
			Collections.shuffle(pool, ThreadLocalRandom.current());
			for (int i = 0; i < count; i++) {
				assignment[i] = pool.get(i);
			}

			if (previous == null) {
				break;
			}

			boolean anyRepeat = false;
			for (int i = 0; i < count; i++) {
				if (assignment[i] == previous[i]) {
					anyRepeat = true;
					break;
				}
			}

			if (!anyRepeat) {
				break;
			}
		}

		return assignment;
	}

	private float resolveValue(ModConfig.Mode mode, int dropPercent, int layerIndex) {
		if (mode == ModConfig.Mode.RANDOM) {
			return 0.30F + ThreadLocalRandom.current().nextFloat() * 0.70F;
		}

		float value = 1.0F - (dropPercent / 100.0F) * layerIndex;
		return Math.max(0.0F, value);
	}
}
package com.armaninyow.morecloudlayers;

import com.armaninyow.morecloudlayers.config.CloudConfigManager;
import com.armaninyow.morecloudlayers.config.ModConfig;
import com.armaninyow.morecloudlayers.render.CloudLayerDirtyMarker;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			ModConfig current = CloudConfigManager.getInstance().getConfig();
			ModConfig working = current.copy();

			ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("morecloudlayers.config.title"))
				.setSavingRunnable(() -> {
					CloudConfigManager.getInstance().setConfig(working);
					if (Minecraft.getInstance().levelRenderer instanceof CloudLayerDirtyMarker marker) {
						marker.morecloudlayers$markLayersDirty();
					}
				});

			ConfigEntryBuilder entryBuilder = builder.entryBuilder();
			ConfigCategory general = builder.getOrCreateCategory(Component.translatable("morecloudlayers.config.category.general"));

			general.addEntry(entryBuilder.startIntSlider(
					Component.translatable("morecloudlayers.config.layerCount"), current.layerCount, 1, 8)
				.setDefaultValue(2)
				.setTooltip(Component.translatable("morecloudlayers.config.layerCount.tooltip"))
				.setSaveConsumer(v -> working.layerCount = v)
				.build());

			general.addEntry(entryBuilder.startEnumSelector(
					Component.translatable("morecloudlayers.config.speedMode"), ModConfig.Mode.class, current.speedMode)
				.setDefaultValue(ModConfig.Mode.LINEAR)
				.setSaveConsumer(v -> working.speedMode = v)
				.build());

			general.addEntry(entryBuilder.startIntSlider(
					Component.translatable("morecloudlayers.config.speedDropPercent"), current.speedDropPercent, 1, 10)
				.setDefaultValue(5)
				.setTooltip(Component.translatable("morecloudlayers.config.speedDropPercent.tooltip"))
				.setSaveConsumer(v -> working.speedDropPercent = v)
				.build());

			general.addEntry(entryBuilder.startEnumSelector(
					Component.translatable("morecloudlayers.config.sizeMode"), ModConfig.Mode.class, current.sizeMode)
				.setDefaultValue(ModConfig.Mode.LINEAR)
				.setSaveConsumer(v -> working.sizeMode = v)
				.build());

			general.addEntry(entryBuilder.startIntSlider(
					Component.translatable("morecloudlayers.config.sizeDropPercent"), current.sizeDropPercent, 1, 10)
				.setDefaultValue(4)
				.setTooltip(Component.translatable("morecloudlayers.config.sizeDropPercent.tooltip"))
				.setSaveConsumer(v -> working.sizeDropPercent = v)
				.build());

			general.addEntry(entryBuilder.startBooleanToggle(
					Component.translatable("morecloudlayers.config.randomTransparency"), current.randomTransparency)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("morecloudlayers.config.randomTransparency.tooltip"))
				.setSaveConsumer(v -> working.randomTransparency = v)
				.build());

			return builder.build();
		};
	}
}
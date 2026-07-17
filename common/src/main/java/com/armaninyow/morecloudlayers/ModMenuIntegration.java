package com.armaninyow.morecloudlayers;

import com.armaninyow.morecloudlayers.config.CloudConfigManager;
import com.armaninyow.morecloudlayers.config.ModConfig;
import com.armaninyow.morecloudlayers.render.CloudLayerDirtyMarker;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			ModConfig current = CloudConfigManager.getInstance().getConfig();
			ModConfig working = current.copy();

			Option<Integer> speedDropOption = Option.<Integer>createBuilder()
				.name(Component.translatable("morecloudlayers.config.speedDropPercent"))
				.description(OptionDescription.of(Component.translatable("morecloudlayers.config.speedDropPercent.tooltip")))
				.binding(5, () -> working.speedDropPercent, v -> working.speedDropPercent = v)
				.controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 10).step(1))
				.available(current.speedMode != ModConfig.Mode.RANDOM)
				.build();

			Option<ModConfig.Mode> speedModeOption = Option.<ModConfig.Mode>createBuilder()
				.name(Component.translatable("morecloudlayers.config.speedMode"))
				.binding(ModConfig.Mode.LINEAR, () -> working.speedMode, v -> working.speedMode = v)
				.controller(opt -> EnumControllerBuilder.create(opt).enumClass(ModConfig.Mode.class))
				.listener((opt, value) -> speedDropOption.setAvailable(value != ModConfig.Mode.RANDOM))
				.build();

			Option<Integer> sizeDropOption = Option.<Integer>createBuilder()
				.name(Component.translatable("morecloudlayers.config.sizeDropPercent"))
				.description(OptionDescription.of(Component.translatable("morecloudlayers.config.sizeDropPercent.tooltip")))
				.binding(4, () -> working.sizeDropPercent, v -> working.sizeDropPercent = v)
				.controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 10).step(1))
				.available(current.sizeMode != ModConfig.Mode.RANDOM)
				.build();

			Option<ModConfig.Mode> sizeModeOption = Option.<ModConfig.Mode>createBuilder()
				.name(Component.translatable("morecloudlayers.config.sizeMode"))
				.binding(ModConfig.Mode.LINEAR, () -> working.sizeMode, v -> working.sizeMode = v)
				.controller(opt -> EnumControllerBuilder.create(opt).enumClass(ModConfig.Mode.class))
				.listener((opt, value) -> sizeDropOption.setAvailable(value != ModConfig.Mode.RANDOM))
				.build();

			Option<Boolean> randomTransparencyOption = Option.<Boolean>createBuilder()
				.name(Component.translatable("morecloudlayers.config.randomTransparency"))
				.description(OptionDescription.of(Component.translatable("morecloudlayers.config.randomTransparency.tooltip")))
				.binding(true, () -> working.randomTransparency, v -> working.randomTransparency = v)
				.controller(TickBoxControllerBuilder::create)
				.build();

			Option<Boolean> randomLayerPositionsOption = Option.<Boolean>createBuilder()
				.name(Component.translatable("morecloudlayers.config.randomLayerPositions"))
				.description(OptionDescription.of(Component.translatable("morecloudlayers.config.randomLayerPositions.tooltip")))
				.binding(false, () -> working.randomLayerPositions, v -> working.randomLayerPositions = v)
				.controller(TickBoxControllerBuilder::create)
				.available(current.layerCount <= 7)
				.build();

			Option<Integer> layerCountOption = Option.<Integer>createBuilder()
				.name(Component.translatable("morecloudlayers.config.layerCount"))
				.description(OptionDescription.of(Component.translatable("morecloudlayers.config.layerCount.tooltip")))
				.binding(2, () -> working.layerCount, v -> working.layerCount = v)
				.controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 8).step(1))
				.listener((opt, value) -> randomLayerPositionsOption.setAvailable(value <= 7))
				.build();

			Option<ModConfig.CullPattern> layerCullingPatternOption = Option.<ModConfig.CullPattern>createBuilder()
				.name(Component.translatable("morecloudlayers.config.layerCullingPattern"))
				.description(OptionDescription.of(Component.translatable("morecloudlayers.config.layerCullingPattern.tooltip")))
				.binding(ModConfig.CullPattern.QUADRANT, () -> working.layerCullingPattern, v -> working.layerCullingPattern = v)
				.controller(opt -> EnumControllerBuilder.create(opt).enumClass(ModConfig.CullPattern.class))
				.available(current.layerCullingEnabled)
				.build();

			Option<Boolean> layerCullingEnabledOption = Option.<Boolean>createBuilder()
				.name(Component.translatable("morecloudlayers.config.layerCullingEnabled"))
				.description(OptionDescription.of(Component.translatable("morecloudlayers.config.layerCullingEnabled.tooltip")))
				.binding(false, () -> working.layerCullingEnabled, v -> working.layerCullingEnabled = v)
				.controller(TickBoxControllerBuilder::create)
				.listener((opt, value) -> layerCullingPatternOption.setAvailable(value))
				.build();

			YetAnotherConfigLib yacl = YetAnotherConfigLib.createBuilder()
				.title(Component.translatable("morecloudlayers.config.title"))
				.category(ConfigCategory.createBuilder()
					.name(Component.translatable("morecloudlayers.config.category.general"))
					.option(layerCountOption)
					.option(randomLayerPositionsOption)
					.option(speedModeOption)
					.option(speedDropOption)
					.option(sizeModeOption)
					.option(sizeDropOption)
					.option(randomTransparencyOption)
					.build())
				.category(ConfigCategory.createBuilder()
					.name(Component.translatable("morecloudlayers.config.category.others"))
					.option(layerCullingEnabledOption)
					.option(layerCullingPatternOption)
					.build())
				.save(() -> {
					CloudConfigManager.getInstance().setConfig(working);
					if (Minecraft.getInstance().levelRenderer instanceof CloudLayerDirtyMarker marker) {
						marker.morecloudlayers$markLayersDirty();
					}
				})
				.build();

			return yacl.generateScreen(parent);
		};
	}
}
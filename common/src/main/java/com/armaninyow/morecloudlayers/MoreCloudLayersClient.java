package com.armaninyow.morecloudlayers;

import com.armaninyow.morecloudlayers.config.CloudConfigManager;
import com.armaninyow.morecloudlayers.render.CloudLayerDirtyMarker;
import com.armaninyow.morecloudlayers.network.CloudRecalcPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class MoreCloudLayersClient implements ClientModInitializer {
	private ResourceKey<Level> lastDimension = null;

	@Override
	public void onInitializeClient() {
		CloudConfigManager.getInstance().load();

		ClientPlayNetworking.registerGlobalReceiver(CloudRecalcPayload.TYPE, (payload, context) -> {
			context.client().execute(this::triggerRecalculate);
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			lastDimension = client.level != null ? client.level.dimension() : null;
			triggerRecalculate();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.level == null) {
				return;
			}

			ResourceKey<Level> current = client.level.dimension();
			if (lastDimension != null && current != lastDimension && current == Level.OVERWORLD) {
				triggerRecalculate();
			}
			lastDimension = current;
		});

	}

	private void triggerRecalculate() {
		CloudConfigManager.getInstance().recalculate();

		Minecraft client = Minecraft.getInstance();
		if (client.levelRenderer instanceof CloudLayerDirtyMarker marker) {
			marker.morecloudlayers$markLayersDirty();
		}
	}
}
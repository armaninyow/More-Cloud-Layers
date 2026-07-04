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
	// Tracks the last dimension we saw so we only trigger on an actual transition,
	// specifically a transition INTO the Overworld (per spec: portal back to Overworld).
	private ResourceKey<Level> lastDimension = null;

	@Override
	public void onInitializeClient() {
		CloudConfigManager.getInstance().load();

		ClientPlayNetworking.registerGlobalReceiver(CloudRecalcPayload.TYPE, (payload, context) -> {
			context.client().execute(this::triggerRecalculate);
		});

		// Trigger 1: world join.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			lastDimension = client.level != null ? client.level.dimension() : null;
			triggerRecalculate();
		});

		// Trigger 2: dimension change back into the Overworld.
		// Polled each client tick rather than relying on an unverified dedicated
		// dimension-change event - cheap (one reference comparison) and reliable.
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

		// Trigger 3 (wake up / night skip) arrives via CloudRecalcPayload from ServerLevelMixin,
		// handled by the registerGlobalReceiver call above.
	}

	private void triggerRecalculate() {
		CloudConfigManager.getInstance().recalculate();

		Minecraft client = Minecraft.getInstance();
		if (client.levelRenderer instanceof CloudLayerDirtyMarker marker) {
			marker.morecloudlayers$markLayersDirty();
		}
	}
}
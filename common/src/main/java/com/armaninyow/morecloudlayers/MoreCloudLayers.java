package com.armaninyow.morecloudlayers;

import com.armaninyow.morecloudlayers.network.CloudRecalcPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoreCloudLayers implements ModInitializer {
	public static final String MOD_ID = "morecloudlayers";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(CloudRecalcPayload.TYPE, CloudRecalcPayload.CODEC);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
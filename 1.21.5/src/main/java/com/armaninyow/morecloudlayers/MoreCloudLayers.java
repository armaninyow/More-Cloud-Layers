package com.armaninyow.morecloudlayers;

import com.armaninyow.morecloudlayers.network.CloudRecalcPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoreCloudLayers implements ModInitializer {
	public static final String MOD_ID = "morecloudlayers";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Payload types must be registered on both client and server, before any
		// handler is registered - done here since onInitialize runs on both sides.
		PayloadTypeRegistry.playS2C().register(CloudRecalcPayload.TYPE, CloudRecalcPayload.CODEC);
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
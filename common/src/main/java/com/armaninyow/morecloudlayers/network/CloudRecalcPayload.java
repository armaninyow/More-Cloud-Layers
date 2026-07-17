package com.armaninyow.morecloudlayers.network;

import com.armaninyow.morecloudlayers.MoreCloudLayers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CloudRecalcPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CloudRecalcPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MoreCloudLayers.MOD_ID, "cloud_recalc"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CloudRecalcPayload> CODEC =
		StreamCodec.unit(new CloudRecalcPayload());

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
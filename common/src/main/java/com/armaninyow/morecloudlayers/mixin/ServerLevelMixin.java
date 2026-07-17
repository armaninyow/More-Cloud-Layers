package com.armaninyow.morecloudlayers.mixin;

import com.armaninyow.morecloudlayers.network.CloudRecalcPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

	@Inject(method = "wakeUpAllPlayers", at = @At("TAIL"))
	private void morecloudlayers$onWakeUpAllPlayers(CallbackInfo ci) {
		ServerLevel self = (ServerLevel) (Object) this;
		CloudRecalcPayload payload = new CloudRecalcPayload();

		for (ServerPlayer player : self.players()) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
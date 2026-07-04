package com.armaninyow.morecloudlayers.mixin;

import com.armaninyow.morecloudlayers.network.CloudRecalcPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * wakeUpAllPlayers() only runs when the server actually skips the night
 * (called right after the sleep-percentage check succeeds in tickDaylight/updateSleepingPlayerList)
 * - it does NOT run on a single player manually getting out of bed. That makes it
 * the correct hook for the "new day, new weather" trigger from the spec, as opposed
 * to EntitySleepEvents.STOP_SLEEPING which fires on every wake-up regardless of cause.
 */
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
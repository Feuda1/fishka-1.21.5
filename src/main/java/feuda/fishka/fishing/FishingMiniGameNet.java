package feuda.fishka.fishing;

import feuda.fishka.fishing.net.FishkaMinigameInputC2SPayload;
import feuda.fishka.fishing.net.FishkaMinigameLifecycleS2CPayload;
import feuda.fishka.fishing.net.FishkaMinigameStateS2CPayload;
import feuda.fishka.fishing.net.FishkaCatchFeedbackS2CPayload;
import feuda.fishka.fishing.net.FishkaFishingProfileS2CPayload;
import feuda.fishka.fishing.net.FishkaOpenRodModulesC2SPayload;
import feuda.fishka.fishing.net.FishkaSkillTreeActionC2SPayload;
import feuda.fishka.item.CustomFishingRodItem;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class FishingMiniGameNet {
	private static boolean initialized;

	private FishingMiniGameNet() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;

		PayloadTypeRegistry.playC2S().register(FishkaMinigameInputC2SPayload.ID, FishkaMinigameInputC2SPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FishkaSkillTreeActionC2SPayload.ID, FishkaSkillTreeActionC2SPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FishkaOpenRodModulesC2SPayload.ID, FishkaOpenRodModulesC2SPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(FishkaMinigameStateS2CPayload.ID, FishkaMinigameStateS2CPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(FishkaMinigameLifecycleS2CPayload.ID, FishkaMinigameLifecycleS2CPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(FishkaCatchFeedbackS2CPayload.ID, FishkaCatchFeedbackS2CPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(FishkaFishingProfileS2CPayload.ID, FishkaFishingProfileS2CPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(FishkaMinigameInputC2SPayload.ID, (payload, context) ->
			context.server().execute(() -> FishingMiniGameManager.handleInputPacket(context.player(), payload))
		);
		ServerPlayNetworking.registerGlobalReceiver(FishkaSkillTreeActionC2SPayload.ID, (payload, context) ->
			context.server().execute(() -> FishingSkillService.handleActionPacket(context.player(), payload))
		);
		ServerPlayNetworking.registerGlobalReceiver(FishkaOpenRodModulesC2SPayload.ID, (payload, context) ->
			context.server().execute(() -> {
				int slotIndex = payload.slotIndex();
				if (slotIndex < 0 || slotIndex >= context.player().getInventory().size()) {
					return;
				}
				CustomFishingRodItem.tryOpenModuleScreen(
					context.player(),
					context.player().getInventory().getStack(slotIndex),
					slotIndex
				);
			})
		);
	}
}

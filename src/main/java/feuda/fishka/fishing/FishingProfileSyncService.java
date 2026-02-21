package feuda.fishka.fishing;

import feuda.fishka.fishing.net.FishkaFishingProfileS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FishingProfileSyncService {
	private static boolean initialized;

	private FishingProfileSyncService() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			server.execute(() -> syncTo(handler.player, false))
		);
	}

	public static void syncTo(ServerPlayerEntity player, boolean levelUpEvent) {
		FishingProgressionService.FishingProfile profile = FishingProgressionService.getProfile(player);
		FishingDataState.FishingProfileData data = FishingProgressionService.getProfileData(player);
		int availableSkillPoints = FishingSkillService.availableSkillPoints(data);
		int level = Math.max(1, profile.level());
		int currentLevelXp = Math.max(0, profile.currentLevelXp());
		int nextLevelXp = Math.max(1, FishingProgressionService.xpForNextLevel(level));
		ServerPlayNetworking.send(
			player,
			new FishkaFishingProfileS2CPayload(
				level,
				currentLevelXp,
				nextLevelXp,
				levelUpEvent,
				Math.max(0, data.money()),
				Math.max(0, availableSkillPoints),
				Math.max(0, data.skillPointsSpent()),
				data.unlockedNodesMask()
			)
		);
	}
}

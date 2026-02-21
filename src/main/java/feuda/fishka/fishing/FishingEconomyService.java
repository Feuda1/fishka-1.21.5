package feuda.fishka.fishing;

import net.minecraft.server.network.ServerPlayerEntity;

public final class FishingEconomyService {
	private FishingEconomyService() {
	}

	public static int getMoney(ServerPlayerEntity player) {
		return Math.max(0, FishingDataState.get(player.getServer()).getOrCreateProfile(player.getUuid()).money());
	}

	public static int addMoney(ServerPlayerEntity player, int amount) {
		return mutateMoney(player, Math.max(0, amount));
	}

	public static int removeMoney(ServerPlayerEntity player, int amount) {
		return mutateMoney(player, -Math.max(0, amount));
	}

	public static int setMoney(ServerPlayerEntity player, int amount) {
		FishingDataState state = FishingDataState.get(player.getServer());
		FishingDataState.FishingProfileData current = state.getOrCreateProfile(player.getUuid());
		int clampedMoney = Math.max(0, amount);
		FishingDataState.FishingProfileData updated = new FishingDataState.FishingProfileData(
			current.level(),
			current.totalXp(),
			current.currentLevelXp(),
			current.totalCatches(),
			clampedMoney,
			current.skillPointsSpent(),
			current.unlockedNodesMask()
		);
		state.putProfile(player.getUuid(), FishingSkillService.sanitizeProgress(updated));
		state.markDirty();
		return clampedMoney;
	}

	public static boolean trySpendMoney(ServerPlayerEntity player, int amount) {
		int spend = Math.max(0, amount);
		FishingDataState state = FishingDataState.get(player.getServer());
		FishingDataState.FishingProfileData current = state.getOrCreateProfile(player.getUuid());
		if (current.money() < spend) {
			return false;
		}
		FishingDataState.FishingProfileData updated = new FishingDataState.FishingProfileData(
			current.level(),
			current.totalXp(),
			current.currentLevelXp(),
			current.totalCatches(),
			current.money() - spend,
			current.skillPointsSpent(),
			current.unlockedNodesMask()
		);
		state.putProfile(player.getUuid(), FishingSkillService.sanitizeProgress(updated));
		state.markDirty();
		return true;
	}

	private static int mutateMoney(ServerPlayerEntity player, int delta) {
		FishingDataState state = FishingDataState.get(player.getServer());
		FishingDataState.FishingProfileData current = state.getOrCreateProfile(player.getUuid());
		int clampedMoney = Math.max(0, current.money() + delta);
		FishingDataState.FishingProfileData updated = new FishingDataState.FishingProfileData(
			current.level(),
			current.totalXp(),
			current.currentLevelXp(),
			current.totalCatches(),
			clampedMoney,
			current.skillPointsSpent(),
			current.unlockedNodesMask()
		);
		state.putProfile(player.getUuid(), FishingSkillService.sanitizeProgress(updated));
		state.markDirty();
		return clampedMoney;
	}
}

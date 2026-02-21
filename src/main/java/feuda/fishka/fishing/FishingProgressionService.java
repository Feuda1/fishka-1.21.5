package feuda.fishka.fishing;

import net.minecraft.server.network.ServerPlayerEntity;

public final class FishingProgressionService {
	private FishingProgressionService() {
	}

	public static FishingProfile getProfile(ServerPlayerEntity player) {
		FishingDataState.FishingProfileData data = getRaw(player);
		return new FishingProfile(data.level(), data.totalXp(), data.currentLevelXp(), data.totalCatches());
	}

	public static FishingDataState.FishingProfileData getProfileData(ServerPlayerEntity player) {
		return getRaw(player);
	}

	public static ProgressResult addCatchXp(ServerPlayerEntity player, int xp) {
		FishingDataState state = FishingDataState.get(player.getServer());
		FishingDataState.FishingProfileData data = state.getOrCreateProfile(player.getUuid());

		int safeXp = Math.max(0, xp);
		int updatedTotalXp = Math.max(0, data.totalXp() + safeXp);
		int totalCatches = Math.max(0, data.totalCatches() + 1);
		LevelXp levelXp = levelXpFromTotalXp(updatedTotalXp);
		FishingDataState.FishingProfileData updated = new FishingDataState.FishingProfileData(
			levelXp.level(),
			updatedTotalXp,
			levelXp.currentLevelXp(),
			totalCatches,
			data.money(),
			data.skillPointsSpent(),
			data.unlockedNodesMask()
		);
		state.putProfile(player.getUuid(), FishingSkillService.sanitizeProgress(updated));
		state.markDirty();

		int levelUps = Math.max(0, updated.level() - data.level());
		return new ProgressResult(toProfile(updated), levelUps);
	}

	public static MutationResult addXp(ServerPlayerEntity player, int amount) {
		FishingDataState.FishingProfileData current = getRaw(player);
		int totalXp = Math.max(0, current.totalXp() + amount);
		FishingDataState.FishingProfileData updated = withTotalXp(current, totalXp);
		saveRaw(player, updated);
		return new MutationResult(toProfile(current), toProfile(updated), updated.level() - current.level());
	}

	public static MutationResult removeXp(ServerPlayerEntity player, int amount) {
		return addXp(player, -Math.max(0, amount));
	}

	public static MutationResult setCurrentLevelXp(ServerPlayerEntity player, int xp) {
		FishingDataState.FishingProfileData current = getRaw(player);
		int level = Math.max(1, current.level());
		int maxCurrent = Math.max(0, xpForNextLevel(level) - 1);
		int clampedXp = clampInt(xp, 0, maxCurrent);
		int totalXp = totalXpForLevelStart(level) + clampedXp;
		FishingDataState.FishingProfileData updated = new FishingDataState.FishingProfileData(
			level,
			totalXp,
			clampedXp,
			current.totalCatches(),
			current.money(),
			current.skillPointsSpent(),
			current.unlockedNodesMask()
		);
		saveRaw(player, updated);
		return new MutationResult(toProfile(current), toProfile(updated), updated.level() - current.level());
	}

	public static MutationResult setTotalXp(ServerPlayerEntity player, int totalXp) {
		FishingDataState.FishingProfileData current = getRaw(player);
		FishingDataState.FishingProfileData updated = withTotalXp(current, Math.max(0, totalXp));
		saveRaw(player, updated);
		return new MutationResult(toProfile(current), toProfile(updated), updated.level() - current.level());
	}

	public static MutationResult setLevel(ServerPlayerEntity player, int level) {
		FishingDataState.FishingProfileData current = getRaw(player);
		int safeLevel = Math.max(1, level);
		int totalXp = totalXpForLevelStart(safeLevel);
		FishingDataState.FishingProfileData updated = new FishingDataState.FishingProfileData(
			safeLevel,
			totalXp,
			0,
			current.totalCatches(),
			current.money(),
			current.skillPointsSpent(),
			current.unlockedNodesMask()
		);
		saveRaw(player, updated);
		return new MutationResult(toProfile(current), toProfile(updated), updated.level() - current.level());
	}

	public static MutationResult addLevels(ServerPlayerEntity player, int amount) {
		FishingDataState.FishingProfileData current = getRaw(player);
		return setLevel(player, Math.max(1, current.level() + amount));
	}

	public static MutationResult removeLevels(ServerPlayerEntity player, int amount) {
		return addLevels(player, -Math.max(0, amount));
	}

	public static MutationResult resetProgress(ServerPlayerEntity player) {
		FishingDataState.FishingProfileData current = getRaw(player);
		FishingDataState.FishingProfileData updated = new FishingDataState.FishingProfileData(
			1,
			0,
			0,
			0,
			current.money(),
			0,
			0
		);
		saveRaw(player, updated);
		return new MutationResult(toProfile(current), toProfile(updated), updated.level() - current.level());
	}

	public static int xpForNextLevel(int level) {
		return 100 + (level - 1) * 35;
	}

	public static int totalXpForLevelStart(int level) {
		int safeLevel = Math.max(1, level);
		int total = 0;
		for (int currentLevel = 1; currentLevel < safeLevel; currentLevel++) {
			total += xpForNextLevel(currentLevel);
		}
		return total;
	}

	private static LevelXp levelXpFromTotalXp(int totalXp) {
		int remaining = Math.max(0, totalXp);
		int level = 1;
		while (remaining >= xpForNextLevel(level)) {
			remaining -= xpForNextLevel(level);
			level++;
		}
		return new LevelXp(level, remaining);
	}

	private static FishingDataState.FishingProfileData withTotalXp(FishingDataState.FishingProfileData current, int totalXp) {
		LevelXp levelXp = levelXpFromTotalXp(totalXp);
		return new FishingDataState.FishingProfileData(
			levelXp.level(),
			totalXp,
			levelXp.currentLevelXp(),
			current.totalCatches(),
			current.money(),
			current.skillPointsSpent(),
			current.unlockedNodesMask()
		);
	}

	private static FishingDataState.FishingProfileData getRaw(ServerPlayerEntity player) {
		FishingDataState.FishingProfileData data = FishingDataState.get(player.getServer()).getOrCreateProfile(player.getUuid());
		return FishingSkillService.sanitizeProgress(data);
	}

	private static void saveRaw(ServerPlayerEntity player, FishingDataState.FishingProfileData data) {
		FishingDataState state = FishingDataState.get(player.getServer());
		state.putProfile(player.getUuid(), FishingSkillService.sanitizeProgress(data));
		state.markDirty();
	}

	private static FishingProfile toProfile(FishingDataState.FishingProfileData data) {
		return new FishingProfile(data.level(), data.totalXp(), data.currentLevelXp(), data.totalCatches());
	}

	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public record FishingProfile(int level, int totalXp, int currentLevelXp, int totalCatches) {
	}

	public record ProgressResult(FishingProfile profile, int levelUps) {
	}

	public record MutationResult(FishingProfile before, FishingProfile after, int levelDelta) {
	}

	private record LevelXp(int level, int currentLevelXp) {
	}
}

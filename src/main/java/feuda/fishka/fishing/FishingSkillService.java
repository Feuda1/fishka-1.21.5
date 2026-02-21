package feuda.fishka.fishing;

import feuda.fishka.fishing.net.FishkaSkillTreeActionC2SPayload;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FishingSkillService {
	private FishingSkillService() {
	}

	public static int totalEarnedSkillPoints(int level) {
		return Math.max(0, Math.max(1, level) - 1);
	}

	public static int availableSkillPoints(FishingDataState.FishingProfileData data) {
		return Math.max(0, totalEarnedSkillPoints(data.level()) - Math.max(0, data.skillPointsSpent()));
	}

	public static boolean hasNodeUnlocked(FishingDataState.FishingProfileData data, FishingSkillNode node) {
		return (data.unlockedNodesMask() & node.bitMask()) != 0;
	}

	public static boolean hasNodeUnlocked(ServerPlayerEntity player, FishingSkillNode node) {
		FishingDataState.FishingProfileData data = FishingDataState.get(player.getServer()).getOrCreateProfile(player.getUuid());
		return hasNodeUnlocked(data, node);
	}

	public static UnlockResult unlockNode(ServerPlayerEntity player, FishingSkillNode node) {
		if (node == null) {
			return UnlockResult.INVALID_NODE;
		}
		FishingDataState state = FishingDataState.get(player.getServer());
		FishingDataState.FishingProfileData current = sanitizeProgress(state.getOrCreateProfile(player.getUuid()));

		if (hasNodeUnlocked(current, node)) {
			return UnlockResult.ALREADY_UNLOCKED;
		}
		if (availableSkillPoints(current) < node.cost()) {
			return UnlockResult.NOT_ENOUGH_POINTS;
		}

		int updatedMask = current.unlockedNodesMask() | node.bitMask();
		FishingDataState.FishingProfileData updated = new FishingDataState.FishingProfileData(
			current.level(),
			current.totalXp(),
			current.currentLevelXp(),
			current.totalCatches(),
			current.money(),
			current.skillPointsSpent() + node.cost(),
			updatedMask
		);
		state.putProfile(player.getUuid(), sanitizeProgress(updated));
		state.markDirty();
		return UnlockResult.SUCCESS;
	}

	public static RespecResult respecAll(ServerPlayerEntity player) {
		int cost = Math.max(0, FishingConfig.get().skillTree.respecCostMoney);
		FishingDataState state = FishingDataState.get(player.getServer());
		FishingDataState.FishingProfileData current = sanitizeProgress(state.getOrCreateProfile(player.getUuid()));
		if (current.money() < cost) {
			return RespecResult.NOT_ENOUGH_MONEY;
		}

		FishingDataState.FishingProfileData updated = new FishingDataState.FishingProfileData(
			current.level(),
			current.totalXp(),
			current.currentLevelXp(),
			current.totalCatches(),
			Math.max(0, current.money() - cost),
			0,
			0
		);
		state.putProfile(player.getUuid(), sanitizeProgress(updated));
		state.markDirty();
		return RespecResult.SUCCESS;
	}

	public static void resetSkills(ServerPlayerEntity player) {
		FishingDataState state = FishingDataState.get(player.getServer());
		FishingDataState.FishingProfileData current = state.getOrCreateProfile(player.getUuid());
		FishingDataState.FishingProfileData updated = new FishingDataState.FishingProfileData(
			current.level(),
			current.totalXp(),
			current.currentLevelXp(),
			current.totalCatches(),
			current.money(),
			0,
			0
		);
		state.putProfile(player.getUuid(), sanitizeProgress(updated));
		state.markDirty();
	}

	public static void unlockDirect(ServerPlayerEntity player, FishingSkillNode node) {
		if (node == null) {
			return;
		}
		FishingDataState state = FishingDataState.get(player.getServer());
		FishingDataState.FishingProfileData current = sanitizeProgress(state.getOrCreateProfile(player.getUuid()));
		int updatedMask = current.unlockedNodesMask() | node.bitMask();
		int spent = Math.max(current.skillPointsSpent(), 1);
		FishingDataState.FishingProfileData updated = new FishingDataState.FishingProfileData(
			current.level(),
			current.totalXp(),
			current.currentLevelXp(),
			current.totalCatches(),
			current.money(),
			spent,
			updatedMask
		);
		state.putProfile(player.getUuid(), sanitizeProgress(updated));
		state.markDirty();
	}

	public static void handleActionPacket(ServerPlayerEntity player, FishkaSkillTreeActionC2SPayload payload) {
		switch (payload.action()) {
			case REQUEST_SYNC -> FishingProfileSyncService.syncTo(player, false);
			case UNLOCK_NODE -> {
				FishingSkillNode node = FishingSkillNode.fromId(payload.nodeId());
				unlockNode(player, node);
				FishingProfileSyncService.syncTo(player, false);
			}
			case RESPEC_ALL -> {
				respecAll(player);
				FishingProfileSyncService.syncTo(player, false);
			}
		}
	}

	public static FishingDataState.FishingProfileData sanitizeProgress(FishingDataState.FishingProfileData data) {
		int level = Math.max(1, data.level());
		int totalXp = Math.max(0, data.totalXp());
		int currentLevelXp = Math.max(0, Math.min(data.currentLevelXp(), Math.max(0, FishingProgressionService.xpForNextLevel(level) - 1)));
		int catches = Math.max(0, data.totalCatches());
		int money = Math.max(0, data.money());

		int mask = data.unlockedNodesMask() & FishingSkillNode.knownMask();
		int spent = Math.max(0, data.skillPointsSpent());
		int earned = totalEarnedSkillPoints(level);

		if ((mask & FishingSkillNode.UNLOCK_FISHER_CRAFTING.bitMask()) != 0 && earned < 1) {
			mask &= ~FishingSkillNode.UNLOCK_FISHER_CRAFTING.bitMask();
		}

		int minimumSpentFromMask = ((mask & FishingSkillNode.UNLOCK_FISHER_CRAFTING.bitMask()) != 0) ? 1 : 0;
		spent = Math.max(spent, minimumSpentFromMask);
		if (spent > earned) {
			mask &= ~FishingSkillNode.UNLOCK_FISHER_CRAFTING.bitMask();
			spent = Math.min(earned, ((mask & FishingSkillNode.UNLOCK_FISHER_CRAFTING.bitMask()) != 0) ? 1 : 0);
		}

		return new FishingDataState.FishingProfileData(level, totalXp, currentLevelXp, catches, money, spent, mask);
	}

	public enum UnlockResult {
		SUCCESS,
		ALREADY_UNLOCKED,
		NOT_ENOUGH_POINTS,
		INVALID_NODE
	}

	public enum RespecResult {
		SUCCESS,
		NOT_ENOUGH_MONEY
	}
}

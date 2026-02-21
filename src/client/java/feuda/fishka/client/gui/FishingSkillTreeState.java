package feuda.fishka.client.gui;

import feuda.fishka.fishing.FishingSkillNode;
import feuda.fishka.fishing.net.FishkaFishingProfileS2CPayload;

public final class FishingSkillTreeState {
	private static boolean initialized;
	private static int level;
	private static int money;
	private static int availableSkillPoints;
	private static int skillPointsSpent;
	private static int unlockedNodesMask;

	private FishingSkillTreeState() {
	}

	public static void apply(FishkaFishingProfileS2CPayload payload) {
		initialized = true;
		level = Math.max(1, payload.level());
		money = Math.max(0, payload.money());
		availableSkillPoints = Math.max(0, payload.availableSkillPoints());
		skillPointsSpent = Math.max(0, payload.skillPointsSpent());
		unlockedNodesMask = payload.unlockedNodesMask();
	}

	public static Snapshot snapshot() {
		return new Snapshot(
			initialized,
			level,
			money,
			availableSkillPoints,
			skillPointsSpent,
			unlockedNodesMask
		);
	}

	public static void reset() {
		initialized = false;
		level = 1;
		money = 0;
		availableSkillPoints = 0;
		skillPointsSpent = 0;
		unlockedNodesMask = 0;
	}

	public static boolean isUnlocked(FishingSkillNode node) {
		return (unlockedNodesMask & node.bitMask()) != 0;
	}

	public record Snapshot(
		boolean initialized,
		int level,
		int money,
		int availableSkillPoints,
		int skillPointsSpent,
		int unlockedNodesMask
	) {
		public boolean isUnlocked(FishingSkillNode node) {
			return (unlockedNodesMask & node.bitMask()) != 0;
		}
	}
}

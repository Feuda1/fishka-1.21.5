package feuda.fishka.client.gui;

import feuda.fishka.fishing.FishingConfig;
import feuda.fishka.fishing.net.FishkaFishingProfileS2CPayload;
import net.minecraft.client.MinecraftClient;

public final class FishingLevelHudState {
	private static boolean initialized;
	private static int level = 1;
	private static int currentLevelXp;
	private static int nextLevelXp = 100;
	private static int availableSkillPoints;
	private static float displayedProgress01;
	private static int levelUpFxTicksLeft;
	private static int levelUpFxDuration = 1;

	private FishingLevelHudState() {
	}

	public static void apply(FishkaFishingProfileS2CPayload payload) {
		int newLevel = Math.max(1, payload.level());
		int newNextLevelXp = Math.max(1, payload.nextLevelXp());
		int newCurrentLevelXp = clampInt(payload.currentLevelXp(), 0, newNextLevelXp);
		int newAvailableSkillPoints = Math.max(0, payload.availableSkillPoints());
		float targetProgress = newCurrentLevelXp / (float) newNextLevelXp;

		if (!initialized) {
			level = newLevel;
			currentLevelXp = newCurrentLevelXp;
			nextLevelXp = newNextLevelXp;
			availableSkillPoints = newAvailableSkillPoints;
			displayedProgress01 = targetProgress;
			levelUpFxTicksLeft = 0;
			levelUpFxDuration = 1;
			initialized = true;
			return;
		}

		boolean levelUp = payload.levelUpEvent() && newLevel > level;
		if (levelUp) {
			levelUpFxDuration = Math.max(1, FishingConfig.get().fishingLevelHud.levelUpPulseTicks);
			levelUpFxTicksLeft = levelUpFxDuration;
			displayedProgress01 = 1.0f;
		}

		level = newLevel;
		currentLevelXp = newCurrentLevelXp;
		nextLevelXp = newNextLevelXp;
		availableSkillPoints = newAvailableSkillPoints;
	}

	public static void tick(MinecraftClient client) {
		if (!initialized) {
			return;
		}

		FishingConfig.FishingLevelHudConfig cfg = FishingConfig.get().fishingLevelHud;
		float target = currentLevelXp / (float) Math.max(1, nextLevelXp);
		float speed = clamp(cfg.fillLerpSpeed, 0.02f, 1.0f);
		if (displayedProgress01 < target) {
			displayedProgress01 += (target - displayedProgress01) * speed;
			if (displayedProgress01 > target) {
				displayedProgress01 = target;
			}
		} else {
			displayedProgress01 -= (displayedProgress01 - target) * Math.max(0.06f, speed * 0.75f);
			if (displayedProgress01 < target) {
				displayedProgress01 = target;
			}
		}
		if (Math.abs(displayedProgress01 - target) < 0.0005f) {
			displayedProgress01 = target;
		}

		if (levelUpFxTicksLeft > 0) {
			levelUpFxTicksLeft--;
		}
	}

	public static Snapshot snapshot() {
		if (!initialized) {
			return Snapshot.inactive();
		}
		float targetProgress = currentLevelXp / (float) Math.max(1, nextLevelXp);
		float levelUpFx01 = 0.0f;
		if (levelUpFxTicksLeft > 0) {
			float elapsed01 = 1.0f - (levelUpFxTicksLeft / (float) Math.max(1, levelUpFxDuration));
			levelUpFx01 = (float) Math.sin(elapsed01 * Math.PI);
		}
		return new Snapshot(
			true,
			level,
			currentLevelXp,
			nextLevelXp,
			availableSkillPoints,
			clamp01(displayedProgress01),
			clamp01(targetProgress),
			clamp01(levelUpFx01),
			levelUpFxTicksLeft > 0
		);
	}

	public static void reset() {
		initialized = false;
		level = 1;
		currentLevelXp = 0;
		nextLevelXp = 100;
		availableSkillPoints = 0;
		displayedProgress01 = 0.0f;
		levelUpFxTicksLeft = 0;
		levelUpFxDuration = 1;
	}

	private static float clamp01(float value) {
		return clamp(value, 0.0f, 1.0f);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public record Snapshot(
		boolean active,
		int level,
		int currentLevelXp,
		int nextLevelXp,
		int availableSkillPoints,
		float displayedProgress01,
		float targetProgress01,
		float levelUpFx01,
		boolean levelUpActive
	) {
		private static Snapshot inactive() {
			return new Snapshot(false, 1, 0, 100, 0, 0.0f, 0.0f, 0.0f, false);
		}
	}
}

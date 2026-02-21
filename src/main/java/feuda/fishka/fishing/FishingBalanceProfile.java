package feuda.fishka.fishing;

import feuda.fishka.fishing.RodComponentBonusProvider.RodComponentBonus;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

public record FishingBalanceProfile(
	FishEncounterTier tier,
	int durationTicks,
	int stateSyncIntervalTicks,
	int softStartTicks,
	float softStartFishForceStartScale,
	float softStartReleasePenaltyStartScale,
	float softStartOutZoneLossStartScale,
	float holdAccelerationPerTick,
	float releaseAccelerationPerTick,
	float maxTensionVelocityPerTick,
	float velocitySlewPerTick,
	float tensionDamping,
	float tensionBounceFactor,
	float progressGainInZonePerTick,
	float progressLossOutZonePerTick,
	float safeZoneHalfWidth,
	float safeZoneMoveSpeedPerTick,
	float fishSpringK,
	float fishForcePerTick,
	boolean burstEnabled,
	int burstCount,
	float burstStartMin01,
	float burstStartMax01,
	int burstDurationMinTicks,
	int burstDurationMaxTicks,
	int burstImpulseEveryMinTicks,
	int burstImpulseEveryMaxTicks,
	float burstImpulseStrength,
	boolean failOnZeroProgress
) {
	public static FishingBalanceProfile forEncounter(
		ServerPlayerEntity player,
		Hand hand,
		ItemStack rodStack,
		FishingLootService.EncounterPlan encounterPlan
	) {
		FishEncounterTier tier = encounterPlan.tier();
		FishingConfig.MiniGameBalanceConfig cfg = FishingConfig.get().miniGameBalance;
		FishingConfig.LevelScalingConfig lvlCfg = cfg.levelScaling != null
			? cfg.levelScaling
			: FishingConfig.LevelScalingConfig.defaults();
		FishingConfig.TierDifficultyConfig tierCfg = FishingConfig.get().encounterBalance.forTier(tier);

		int playerLevel = FishingProgressionService.getProfile(player).level();
		int levelOverBase = Math.max(0, playerLevel - 1);

		float levelHoldGain = Math.min(levelOverBase * lvlCfg.holdGainPerLevel, lvlCfg.holdGainCap);
		float levelSafeZone = Math.min(levelOverBase * lvlCfg.safeZoneHalfWidthPerLevel, lvlCfg.safeZoneHalfWidthCap);
		float levelOutZoneReduction = Math.min(levelOverBase * lvlCfg.outZoneLossReductionPerLevel, lvlCfg.outZoneLossReductionCap);
		float levelProgressGain = Math.min(levelOverBase * lvlCfg.progressGainPerLevel, lvlCfg.progressGainCap);
		float levelForceReduction = Math.min(levelOverBase * lvlCfg.fishForceReductionPerLevel, lvlCfg.fishForceReductionCap);

		RodComponentBonus components = RodComponentBonusProvider.resolve(player, hand, rodStack);
		float componentProgressLossReduction = clamp01(components.progressLossReduction());
		float componentForceReduction = clamp01(components.fishForceReduction());

		float safeZoneHalfWidth = clamp(
			(cfg.safeZoneBaseHalfWidth + levelSafeZone + components.safeZoneWidthBonus()) * tierCfg.safeZoneMult,
			0.045f,
			0.34f
		);
		float holdAcceleration = clamp(
			cfg.holdAcceleration + levelHoldGain + components.holdGainBonus(),
			0.001f,
			0.060f
		);
		float releaseAcceleration = clamp(
			cfg.releaseAcceleration * (1.0f - levelOutZoneReduction),
			0.001f,
			0.060f
		);
		float progressGain = clamp(
			cfg.baseProgressGainInZone + levelProgressGain + components.progressGainBonus(),
			0.002f,
			0.080f
		);
		float progressLoss = clamp(
			cfg.baseProgressLossOutZone * (1.0f - levelOutZoneReduction * 0.7f) * (1.0f - componentProgressLossReduction),
			0.002f,
			0.090f
		);

		float forceReduction = clamp01(levelForceReduction + componentForceReduction);
		float fishForce = clamp(
			cfg.fishForceBase * tierCfg.fishForceMult * (1.0f - forceReduction),
			0.0005f,
			0.055f
		);
		float fishSpringK = clamp(
			cfg.fishSpringK * (1.0f + (tierCfg.fishForceMult - 1.0f) * 0.35f) * (1.0f - forceReduction * 0.35f),
			0.005f,
			0.22f
		);

		float tierTimerMult = timerMultForTier(tier);
		float tierProgressGainMult = progressGainMultForTier(tier);
		float tierProgressLossMult = progressLossMultForTier(tier);
		float tierZoneSpeedMult = zoneMoveMultForTier(tier);
		float tierForceMult = forceChaosMultForTier(tier);

		progressGain = clamp(progressGain * tierProgressGainMult, 0.002f, 0.080f);
		progressLoss = clamp(progressLoss * tierProgressLossMult, 0.002f, 0.090f);
		fishForce = clamp(fishForce * tierForceMult, 0.0005f, 0.065f);
		fishSpringK = clamp(fishSpringK * (0.92f + (tierForceMult - 1.0f) * 0.85f), 0.005f, 0.24f);
		float configDurationInfluence = clamp(0.82f + tierCfg.durationMult * 0.18f, 0.65f, 1.35f);
		int duration = Math.max(52, Math.round(cfg.baseDurationTicks * tierTimerMult * configDurationInfluence));

		return new FishingBalanceProfile(
			tier,
			duration,
			Math.max(1, cfg.stateSyncIntervalTicks),
			Math.max(0, cfg.softStartTicks),
			clamp(cfg.softStartFishForceStartScale, 0.05f, 1.0f),
			clamp(cfg.softStartReleasePenaltyStartScale, 0.05f, 1.0f),
			clamp(cfg.softStartOutZoneLossStartScale, 0.05f, 1.0f),
			holdAcceleration,
			releaseAcceleration,
			clamp(cfg.maxTensionVelocity, 0.03f, 0.26f),
			clamp(cfg.velocitySlewPerTick, 0.003f, 0.15f),
			clamp(cfg.tensionDamping, 0.10f, 0.99f),
			clamp(cfg.tensionBounceFactor, 0.05f, 0.95f),
			progressGain,
			progressLoss,
			safeZoneHalfWidth,
			clamp(cfg.safeZoneMoveSpeed * tierZoneSpeedMult, 0.0005f, 0.040f),
			fishSpringK,
			fishForce,
			cfg.burstEnabled,
			Math.max(0, tierCfg.burstCount),
			clamp(cfg.burstStartMin01, 0.0f, 1.0f),
			clamp(cfg.burstStartMax01, 0.0f, 1.0f),
			Math.max(4, cfg.burstDurationMinTicks),
			Math.max(5, cfg.burstDurationMaxTicks),
			Math.max(1, cfg.burstImpulseEveryMinTicks),
			Math.max(1, cfg.burstImpulseEveryMaxTicks),
			clamp(cfg.burstImpulseStrength * tierCfg.burstImpulseMult, 0.0f, 0.40f),
			cfg.failOnZeroProgress
		);
	}

	private static float clamp01(float value) {
		return clamp(value, 0.0f, 1.0f);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float timerMultForTier(FishEncounterTier tier) {
		return switch (tier) {
			case COMMON -> 1.0f;
			case UNCOMMON -> 1.00f;
			case RARE -> 1.00f;
			case EPIC -> 1.00f;
			case LEGENDARY -> 1.00f;
		};
	}

	private static float progressGainMultForTier(FishEncounterTier tier) {
		return switch (tier) {
			case COMMON -> 1.12f;
			case UNCOMMON -> 1.00f;
			case RARE -> 0.74f;
			case EPIC -> 0.58f;
			case LEGENDARY -> 0.44f;
		};
	}

	private static float progressLossMultForTier(FishEncounterTier tier) {
		return switch (tier) {
			case COMMON -> 0.84f;
			case UNCOMMON -> 1.00f;
			case RARE -> 1.42f;
			case EPIC -> 1.88f;
			case LEGENDARY -> 2.35f;
		};
	}

	private static float zoneMoveMultForTier(FishEncounterTier tier) {
		return switch (tier) {
			case COMMON -> 0.92f;
			case UNCOMMON -> 1.00f;
			case RARE -> 1.36f;
			case EPIC -> 1.74f;
			case LEGENDARY -> 2.10f;
		};
	}

	private static float forceChaosMultForTier(FishEncounterTier tier) {
		return switch (tier) {
			case COMMON -> 0.80f;
			case UNCOMMON -> 1.00f;
			case RARE -> 1.65f;
			case EPIC -> 2.15f;
			case LEGENDARY -> 2.85f;
		};
	}
}

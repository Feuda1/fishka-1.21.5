package feuda.fishka.fishing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import feuda.fishka.Fishka;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FishingConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("fishka-fishing.json");
	private static FishingConfigData cached;

	private FishingConfig() {
	}

	public static FishingConfigData get() {
		if (cached == null) {
			reload();
		}
		return cached;
	}

	public static void reload() {
		FishingConfigData defaults = FishingConfigData.defaults();
		try {
			if (!Files.exists(CONFIG_PATH)) {
				Files.createDirectories(CONFIG_PATH.getParent());
				Files.writeString(CONFIG_PATH, GSON.toJson(defaults));
				cached = defaults;
				return;
			}
			String raw = Files.readString(CONFIG_PATH);
			FishingConfigData loaded = GSON.fromJson(raw, FishingConfigData.class);
			cached = loaded != null ? loaded.mergeMissing(defaults) : defaults;
			try {
				Files.writeString(CONFIG_PATH, GSON.toJson(cached));
			} catch (IOException writeException) {
				Fishka.LOGGER.warn("Failed to persist merged fishing config", writeException);
			}
		} catch (IOException exception) {
			Fishka.LOGGER.error("Failed to load fishing config, fallback to defaults", exception);
			cached = defaults;
		}
	}

	public static final class FishingConfigData {
		public MiniGameConfig miniGame = MiniGameConfig.defaults();
		public MiniGameBalanceConfig miniGameBalance = MiniGameBalanceConfig.defaults();
		public EncounterBalanceConfig encounterBalance = EncounterBalanceConfig.defaults();
		public MiniGameHudConfig miniGameHud = MiniGameHudConfig.defaults();
		public FishingLevelHudConfig fishingLevelHud = FishingLevelHudConfig.defaults();
		public SkillTreeConfig skillTree = SkillTreeConfig.defaults();
		public MoneyHudConfig moneyHud = MoneyHudConfig.defaults();
		public RodModulesConfig rodModules = RodModulesConfig.defaults();
		public RewardHudConfig rewardHud = RewardHudConfig.defaults();
		public LootConfig loot = LootConfig.defaults();
		public List<SpeciesConfig> species = SpeciesConfig.defaults();

		public static FishingConfigData defaults() {
			return new FishingConfigData();
		}

		public FishingConfigData mergeMissing(FishingConfigData defaults) {
			if (miniGame == null) miniGame = defaults.miniGame;
			if (miniGameBalance == null) miniGameBalance = defaults.miniGameBalance;
			if (encounterBalance == null) encounterBalance = defaults.encounterBalance;
			if (miniGameHud == null) miniGameHud = defaults.miniGameHud;
			if (fishingLevelHud == null) fishingLevelHud = defaults.fishingLevelHud;
			if (skillTree == null) skillTree = defaults.skillTree;
			if (moneyHud == null) moneyHud = defaults.moneyHud;
			if (rodModules == null) rodModules = defaults.rodModules;
			if (rewardHud == null) rewardHud = defaults.rewardHud;
			if (loot == null) loot = defaults.loot;
			if (species == null || species.isEmpty()) species = defaults.species;

			miniGameBalance = miniGameBalance.mergeMissing(defaults.miniGameBalance);
			encounterBalance = encounterBalance.mergeMissing(defaults.encounterBalance);
			fishingLevelHud = fishingLevelHud.mergeMissing(defaults.fishingLevelHud);
			skillTree = skillTree.mergeMissing(defaults.skillTree);
			moneyHud = moneyHud.mergeMissing(defaults.moneyHud);
			rodModules = rodModules.mergeMissing(defaults.rodModules);
			rewardHud = rewardHud.mergeMissing(defaults.rewardHud);
			localizeSpeciesNames();
			return this;
		}

		private void localizeSpeciesNames() {
			for (SpeciesConfig cfg : species) {
				if (cfg == null || cfg.name == null) {
					continue;
				}
				cfg.name = switch (cfg.name.trim().toLowerCase()) {
					case "roach" -> "Плотва";
					case "perch" -> "Окунь";
					case "carp" -> "Карп";
					case "trout" -> "Форель";
					case "pike" -> "Щука";
					case "catfish" -> "Сом";
					case "sturgeon" -> "Осётр";
					default -> cfg.name;
				};
			}
		}
	}

	public static final class MiniGameConfig {
		public int minWaitAfterCastTicks = 40;
		public int sessionTimeoutTicks = 20 * 18;
		public float startProgress = 38.0f;
		public float passProgress = 100.0f;
		public int perfectWindowTicks = 2;
		public int beatMinIntervalTicks = 8;
		public int beatIntervalVarianceTicks = 9;
		public float resistanceMin = 0.8f;
		public float resistanceMax = 1.5f;
		public float perfectGain = 18.0f;
		public float offTimingGain = 4.0f;
		public float offTimingPenalty = 3.0f;
		public int hudIntervalTicks = 6;

		public static MiniGameConfig defaults() {
			return new MiniGameConfig();
		}
	}

	public static final class MiniGameBalanceConfig {
		public int baseDurationTicks = 20 * 7;
		public int stateSyncIntervalTicks = 2;
		public boolean failOnZeroProgress = false;
		public int inputToggleDebounceTicks = 1;

		public int softStartTicks = 30;
		public float softStartFishForceStartScale = 0.25f;
		public float softStartReleasePenaltyStartScale = 0.35f;
		public float softStartOutZoneLossStartScale = 0.40f;

		public float holdAcceleration = 0.0078f;
		public float releaseAcceleration = 0.0038f;
		public float tensionDamping = 0.945f;
		public float tensionBounceFactor = 0.42f;
		public float fishSpringK = 0.038f;
		public float maxTensionVelocity = 0.085f;
		public float velocitySlewPerTick = 0.010f;

		public float baseProgressGainInZone = 0.0105f;
		public float baseProgressLossOutZone = 0.0032f;
		public float safeZoneBaseHalfWidth = 0.11f;
		public float safeZoneMoveSpeed = 0.0054f;
		public float fishForceBase = 0.0065f;

		public boolean burstEnabled = true;
		public float burstStartMin01 = 0.35f;
		public float burstStartMax01 = 0.70f;
		public int burstDurationMinTicks = 14;
		public int burstDurationMaxTicks = 20;
		public int burstImpulseEveryMinTicks = 3;
		public int burstImpulseEveryMaxTicks = 4;
		public float burstImpulseStrength = 0.10f;
		public boolean burstHudShakeEnabled = false;
		public int burstHudShakeX = 1;
		public int burstHudShakeY = 1;

		public LevelScalingConfig levelScaling = LevelScalingConfig.defaults();

		public static MiniGameBalanceConfig defaults() {
			return new MiniGameBalanceConfig();
		}

		public MiniGameBalanceConfig mergeMissing(MiniGameBalanceConfig defaults) {
			if (levelScaling == null) {
				levelScaling = defaults.levelScaling;
			}
			return this;
		}
	}

	public static final class EncounterBalanceConfig {
		public TierDifficultyConfig common = TierDifficultyConfig.common();
		public TierDifficultyConfig uncommon = TierDifficultyConfig.uncommon();
		public TierDifficultyConfig rare = TierDifficultyConfig.rare();
		public TierDifficultyConfig epic = TierDifficultyConfig.epic();
		public TierDifficultyConfig legendary = TierDifficultyConfig.legendary();
		public TierWeightConfig materialTierWeights = TierWeightConfig.materialDefaults();
		public TierWeightConfig treasureTierWeights = TierWeightConfig.treasureDefaults();

		public static EncounterBalanceConfig defaults() {
			return new EncounterBalanceConfig();
		}

		public EncounterBalanceConfig mergeMissing(EncounterBalanceConfig defaults) {
			if (common == null) common = defaults.common;
			if (uncommon == null) uncommon = defaults.uncommon;
			if (rare == null) rare = defaults.rare;
			if (epic == null) epic = defaults.epic;
			if (legendary == null) legendary = defaults.legendary;
			if (materialTierWeights == null) materialTierWeights = defaults.materialTierWeights;
			if (treasureTierWeights == null) treasureTierWeights = defaults.treasureTierWeights;
			materialTierWeights = materialTierWeights.mergeMissing(defaults.materialTierWeights);
			treasureTierWeights = treasureTierWeights.mergeMissing(defaults.treasureTierWeights);
			return this;
		}

		public TierDifficultyConfig forTier(FishEncounterTier tier) {
			return switch (tier) {
				case COMMON -> common;
				case UNCOMMON -> uncommon;
				case RARE -> rare;
				case EPIC -> epic;
				case LEGENDARY -> legendary;
			};
		}
	}

	public static final class TierDifficultyConfig {
		public float durationMult;
		public float fishForceMult;
		public float safeZoneMult;
		public int burstCount;
		public float burstImpulseMult;

		public static TierDifficultyConfig common() {
			return of(0.95f, 0.80f, 1.08f, 0, 0.85f);
		}

		public static TierDifficultyConfig uncommon() {
			return of(1.00f, 1.00f, 1.00f, 1, 1.00f);
		}

		public static TierDifficultyConfig rare() {
			return of(1.15f, 1.20f, 0.92f, 1, 1.20f);
		}

		public static TierDifficultyConfig epic() {
			return of(1.30f, 1.40f, 0.84f, 2, 1.35f);
		}

		public static TierDifficultyConfig legendary() {
			return of(1.45f, 1.60f, 0.76f, 2, 1.50f);
		}

		private static TierDifficultyConfig of(float durationMult, float fishForceMult, float safeZoneMult, int burstCount, float burstImpulseMult) {
			TierDifficultyConfig cfg = new TierDifficultyConfig();
			cfg.durationMult = durationMult;
			cfg.fishForceMult = fishForceMult;
			cfg.safeZoneMult = safeZoneMult;
			cfg.burstCount = burstCount;
			cfg.burstImpulseMult = burstImpulseMult;
			return cfg;
		}
	}

	public static final class TierWeightConfig {
		public int common;
		public int uncommon;
		public int rare;
		public int epic;
		public int legendary;

		public static TierWeightConfig materialDefaults() {
			return of(52, 30, 13, 4, 1);
		}

		public static TierWeightConfig treasureDefaults() {
			return of(18, 34, 28, 14, 6);
		}

		public TierWeightConfig mergeMissing(TierWeightConfig defaults) {
			common = common <= 0 ? defaults.common : common;
			uncommon = uncommon <= 0 ? defaults.uncommon : uncommon;
			rare = rare <= 0 ? defaults.rare : rare;
			epic = epic <= 0 ? defaults.epic : epic;
			legendary = legendary <= 0 ? defaults.legendary : legendary;
			return this;
		}

		public int totalWeight() {
			return Math.max(1, common + uncommon + rare + epic + legendary);
		}

		private static TierWeightConfig of(int common, int uncommon, int rare, int epic, int legendary) {
			TierWeightConfig cfg = new TierWeightConfig();
			cfg.common = common;
			cfg.uncommon = uncommon;
			cfg.rare = rare;
			cfg.epic = epic;
			cfg.legendary = legendary;
			return cfg;
		}
	}

	public static final class LevelScalingConfig {
		public float holdGainPerLevel = 0.00020f;
		public float holdGainCap = 0.0035f;
		public float safeZoneHalfWidthPerLevel = 0.0007f;
		public float safeZoneHalfWidthCap = 0.03f;
		public float outZoneLossReductionPerLevel = 0.00042f;
		public float outZoneLossReductionCap = 0.25f;
		public float progressGainPerLevel = 0.00045f;
		public float progressGainCap = 0.011f;
		public float fishForceReductionPerLevel = 0.0022f;
		public float fishForceReductionCap = 0.20f;

		public static LevelScalingConfig defaults() {
			return new LevelScalingConfig();
		}
	}

	public static final class MiniGameHudConfig {
		public int yOffsetFromHotbar = 26;
		public int frameWidth = 300;
		public int frameHeight = 64;
		public int interpolationWindowMs = 100;

		public static MiniGameHudConfig defaults() {
			return new MiniGameHudConfig();
		}
	}

	public static final class FishingLevelHudConfig {
		public boolean enabled = true;
		public int offsetX = 10;
		public int offsetY = 10;
		public int width = 210;
		public int height = 34;
		public int barHeight = 8;
		public float fillLerpSpeed = 0.18f;
		public int levelUpPulseTicks = 24;
		public float shineSpeed = 0.10f;
		public boolean showShadow = true;

		public static FishingLevelHudConfig defaults() {
			return new FishingLevelHudConfig();
		}

		public FishingLevelHudConfig mergeMissing(FishingLevelHudConfig defaults) {
			width = width <= 0 ? defaults.width : width;
			height = height <= 0 ? defaults.height : height;
			barHeight = barHeight <= 0 ? defaults.barHeight : barHeight;
			fillLerpSpeed = fillLerpSpeed <= 0.0f ? defaults.fillLerpSpeed : fillLerpSpeed;
			levelUpPulseTicks = levelUpPulseTicks <= 0 ? defaults.levelUpPulseTicks : levelUpPulseTicks;
			shineSpeed = shineSpeed <= 0.0f ? defaults.shineSpeed : shineSpeed;
			return this;
		}
	}

	public static final class SkillTreeConfig {
		public int respecCostMoney = 500;
		public String openKeyDefault = "GLFW_KEY_GRAVE_ACCENT";

		public static SkillTreeConfig defaults() {
			return new SkillTreeConfig();
		}

		public SkillTreeConfig mergeMissing(SkillTreeConfig defaults) {
			respecCostMoney = Math.max(0, respecCostMoney);
			if (openKeyDefault == null || openKeyDefault.isBlank()) {
				openKeyDefault = defaults.openKeyDefault;
			}
			return this;
		}
	}

	public static final class MoneyHudConfig {
		public boolean enabled = true;
		public int offsetX = 10;
		public int offsetY = 10;
		public boolean showShadow = true;
		public String format = "$%s";

		public static MoneyHudConfig defaults() {
			return new MoneyHudConfig();
		}

		public MoneyHudConfig mergeMissing(MoneyHudConfig defaults) {
			if (format == null || format.isBlank()) {
				format = defaults.format;
			}
			return this;
		}
	}

	public static final class RodModulesConfig {
		public boolean enabled = true;
		public boolean openByInventoryRightClick = true;
		public boolean biteCountdownAccelerationEnabled = true;

		public float baseHandleProgressLossReduction = 0.09f;
		public float baseReelProgressGainBonus = 0.0016f;
		public float baseRodLengthBonus = 0.11f;
		public float baseLineWeightBonus = 0.12f;
		public float baseBobberBiteSpeedBonus = 0.09f;
		public float baseHookRarityLuckBonus = 0.10f;

		public float handleProgressLossReductionCap = 0.30f;
		public float reelProgressGainBonusCap = 0.0052f;
		public float rodLengthBonusCap = 0.30f;
		public float lineWeightBonusCap = 0.33f;
		public float bobberBiteSpeedBonusCap = 0.32f;
		public float hookRarityLuckBonusCap = 0.27f;

		public TierMultipliersConfig tierMultipliers = TierMultipliersConfig.defaults();

		public static RodModulesConfig defaults() {
			return new RodModulesConfig();
		}

		public RodModulesConfig mergeMissing(RodModulesConfig defaults) {
			if (tierMultipliers == null) {
				tierMultipliers = defaults.tierMultipliers;
			}
			tierMultipliers = tierMultipliers.mergeMissing(defaults.tierMultipliers);
			handleProgressLossReductionCap = Math.max(0.0f, handleProgressLossReductionCap <= 0.0f ? defaults.handleProgressLossReductionCap : handleProgressLossReductionCap);
			reelProgressGainBonusCap = Math.max(0.0f, reelProgressGainBonusCap <= 0.0f ? defaults.reelProgressGainBonusCap : reelProgressGainBonusCap);
			rodLengthBonusCap = Math.max(0.0f, rodLengthBonusCap <= 0.0f ? defaults.rodLengthBonusCap : rodLengthBonusCap);
			lineWeightBonusCap = Math.max(0.0f, lineWeightBonusCap <= 0.0f ? defaults.lineWeightBonusCap : lineWeightBonusCap);
			bobberBiteSpeedBonusCap = Math.max(0.0f, bobberBiteSpeedBonusCap <= 0.0f ? defaults.bobberBiteSpeedBonusCap : bobberBiteSpeedBonusCap);
			hookRarityLuckBonusCap = Math.max(0.0f, hookRarityLuckBonusCap <= 0.0f ? defaults.hookRarityLuckBonusCap : hookRarityLuckBonusCap);
			return this;
		}
	}

	public static final class TierMultipliersConfig {
		public float common = 1.00f;
		public float uncommon = 1.25f;
		public float rare = 1.55f;
		public float epic = 1.90f;
		public float legendary = 2.30f;

		public static TierMultipliersConfig defaults() {
			return new TierMultipliersConfig();
		}

		public TierMultipliersConfig mergeMissing(TierMultipliersConfig defaults) {
			common = common <= 0.0f ? defaults.common : common;
			uncommon = uncommon <= 0.0f ? defaults.uncommon : uncommon;
			rare = rare <= 0.0f ? defaults.rare : rare;
			epic = epic <= 0.0f ? defaults.epic : epic;
			legendary = legendary <= 0.0f ? defaults.legendary : legendary;
			return this;
		}

		public float forTier(FishEncounterTier tier) {
			return switch (tier) {
				case COMMON -> common;
				case UNCOMMON -> uncommon;
				case RARE -> rare;
				case EPIC -> epic;
				case LEGENDARY -> legendary;
			};
		}
	}

	public static final class RewardHudConfig {
		public int displayTicks = 70;
		public int fadeInTicks = 12;
		public int fadeOutTicks = 16;
		public int revealDelayTicks = 20;
		public int totemPhaseTicks = 24;
		public int bannerOffsetY = 72;
		public int maxBannerWidth = 320;
		public boolean rayEnabled = true;
		public boolean rayAroundBanner = true;
		public int rayOrbitRadiusX = 14;
		public int rayOrbitRadiusY = 10;
		public float rayRotationSpeed = 0.0f;
		public float rayPulseSpeed = 0.14f;
		public int rayCount = 14;
		public float rayIntensityCommon = 0.55f;
		public float rayIntensityUncommon = 0.62f;
		public float rayIntensityRare = 0.72f;
		public float rayIntensityEpic = 0.84f;
		public float rayIntensityLegendary = 1.00f;

		public static RewardHudConfig defaults() {
			return new RewardHudConfig();
		}

		public RewardHudConfig mergeMissing(RewardHudConfig defaults) {
			maxBannerWidth = Math.max(180, maxBannerWidth <= 0 ? defaults.maxBannerWidth : maxBannerWidth);
			fadeInTicks = Math.max(10, fadeInTicks <= 0 ? defaults.fadeInTicks : fadeInTicks);
			fadeOutTicks = Math.max(12, fadeOutTicks <= 0 ? defaults.fadeOutTicks : fadeOutTicks);
			totemPhaseTicks = Math.max(22, totemPhaseTicks <= 0 ? defaults.totemPhaseTicks : totemPhaseTicks);
			revealDelayTicks = Math.max(16, revealDelayTicks <= 0 ? defaults.revealDelayTicks : revealDelayTicks);
			rayOrbitRadiusX = Math.max(0, rayOrbitRadiusX);
			rayOrbitRadiusY = Math.max(0, rayOrbitRadiusY);
			rayCount = Math.max(4, rayCount <= 0 ? defaults.rayCount : rayCount);
			rayRotationSpeed = rayRotationSpeed <= 0.0f ? defaults.rayRotationSpeed : rayRotationSpeed;
			rayPulseSpeed = rayPulseSpeed <= 0.0f ? defaults.rayPulseSpeed : rayPulseSpeed;
			return this;
		}

		public float rayIntensityForTier(FishEncounterTier tier) {
			return switch (tier) {
				case COMMON -> rayIntensityCommon;
				case UNCOMMON -> rayIntensityUncommon;
				case RARE -> rayIntensityRare;
				case EPIC -> rayIntensityEpic;
				case LEGENDARY -> rayIntensityLegendary;
			};
		}
	}

	public static final class LootConfig {
		public float baseTreasureChance = 0.03f;
		public float treasureLevelBonus = 0.002f;
		public float maxTreasureChance = 0.14f;
		public float materialChance = 0.20f;

		public int commonXp = 12;
		public int uncommonXp = 20;
		public int rareXp = 34;
		public int epicXp = 52;
		public int legendaryXp = 85;

		public static LootConfig defaults() {
			return new LootConfig();
		}
	}

	public static final class SpeciesConfig {
		public String name;
		public float minKg;
		public float maxKg;
		public float minCm;
		public float maxCm;
		public String biomeTag;
		public String preferredTime;
		public float baseWeight;

		public static List<SpeciesConfig> defaults() {
			List<SpeciesConfig> list = new ArrayList<>();
			list.add(spec("Плотва", 0.2f, 1.2f, 12.0f, 28.0f, "river", "day", 1.0f));
			list.add(spec("Окунь", 0.3f, 2.4f, 15.0f, 35.0f, "river", "day", 1.0f));
			list.add(spec("Карп", 0.7f, 6.0f, 20.0f, 55.0f, "river", "day", 0.95f));
			list.add(spec("Форель", 0.6f, 4.4f, 22.0f, 50.0f, "river", "night", 0.9f));
			list.add(spec("Щука", 1.0f, 9.0f, 35.0f, 110.0f, "swamp", "night", 0.8f));
			list.add(spec("Сом", 2.0f, 20.0f, 45.0f, 160.0f, "swamp", "night", 0.7f));
			list.add(spec("Осётр", 3.0f, 35.0f, 55.0f, 220.0f, "ocean", "day", 0.5f));
			return list;
		}

		private static SpeciesConfig spec(String name, float minKg, float maxKg, float minCm, float maxCm, String biomeTag, String preferredTime, float baseWeight) {
			SpeciesConfig config = new SpeciesConfig();
			config.name = name;
			config.minKg = minKg;
			config.maxKg = maxKg;
			config.minCm = minCm;
			config.maxCm = maxCm;
			config.biomeTag = biomeTag;
			config.preferredTime = preferredTime;
			config.baseWeight = baseWeight;
			return config;
		}
	}
}

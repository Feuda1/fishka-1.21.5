package feuda.fishka.fishing;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;

public final class RodModuleCatalog {
	private RodModuleCatalog() {
	}

	public static String modulePath(RodModuleSlot slot, FishEncounterTier tier) {
		return "module_" + slot.key() + "_" + tier.name().toLowerCase(Locale.ROOT);
	}

	public static FishEncounterTier parseTier(String raw) {
		if (raw == null) {
			return null;
		}
		String token = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
		return switch (token) {
			case "common", "обычная", "обычный", "обычное" -> FishEncounterTier.COMMON;
			case "uncommon", "необычная", "необычный", "необычное" -> FishEncounterTier.UNCOMMON;
			case "rare", "редкая", "редкий", "редкое" -> FishEncounterTier.RARE;
			case "epic", "эпическая", "эпический", "эпическое" -> FishEncounterTier.EPIC;
			case "legendary", "легендарная", "легендарный", "легендарное" -> FishEncounterTier.LEGENDARY;
			default -> null;
		};
	}

	public static float tierMultiplier(FishingConfig.RodModulesConfig cfg, FishEncounterTier tier) {
		if (cfg == null || cfg.tierMultipliers == null) {
			return 1.0f;
		}
		return cfg.tierMultipliers.forTier(tier);
	}

	public static Formatting tierColor(FishEncounterTier tier) {
		return switch (tier) {
			case COMMON -> Formatting.WHITE;
			case UNCOMMON -> Formatting.GREEN;
			case RARE -> Formatting.BLUE;
			case EPIC -> Formatting.LIGHT_PURPLE;
			case LEGENDARY -> Formatting.GOLD;
		};
	}

	public static Text effectText(RodModuleSlot slot, FishEncounterTier tier) {
		FishingConfig.RodModulesConfig cfg = FishingConfig.get().rodModules;
		float mult = tierMultiplier(cfg, tier);
		return switch (slot) {
			case HANDLE -> Text.translatable(
				"fishka.module.effect.handle",
				percent(Math.min(cfg.handleProgressLossReductionCap, cfg.baseHandleProgressLossReduction * mult))
			).formatted(Formatting.AQUA);
			case REEL -> Text.translatable(
				"fishka.module.effect.reel",
				percent(Math.min(cfg.reelProgressGainBonusCap, cfg.baseReelProgressGainBonus * mult))
			).formatted(Formatting.AQUA);
			case ROD -> Text.translatable(
				"fishka.module.effect.rod",
				percent(Math.min(cfg.rodLengthBonusCap, cfg.baseRodLengthBonus * mult))
			).formatted(Formatting.AQUA);
			case LINE -> Text.translatable(
				"fishka.module.effect.line",
				percent(Math.min(cfg.lineWeightBonusCap, cfg.baseLineWeightBonus * mult))
			).formatted(Formatting.AQUA);
			case BOBBER -> Text.translatable(
				"fishka.module.effect.bobber",
				percent(Math.min(cfg.bobberBiteSpeedBonusCap, cfg.baseBobberBiteSpeedBonus * mult))
			).formatted(Formatting.AQUA);
			case HOOK -> Text.translatable(
				"fishka.module.effect.hook",
				percent(Math.min(cfg.hookRarityLuckBonusCap, cfg.baseHookRarityLuckBonus * mult))
			).formatted(Formatting.AQUA);
		};
	}

	private static String percent(float value) {
		return String.format(Locale.US, "%.1f%%", value * 100.0f).replace('.', ',');
	}
}

package feuda.fishka.fishing;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

public final class RodComponentBonusProvider {
	private static final RodComponentBonus EMPTY = new RodComponentBonus(
		0.0f,
		0.0f,
		0.0f,
		0.0f,
		0.0f,
		0.0f,
		0.0f,
		0.0f,
		0.0f
	);

	private RodComponentBonusProvider() {
	}

	public static RodComponentBonus resolve(ServerPlayerEntity player, Hand hand, ItemStack rodStack) {
		FishingConfig.RodModulesConfig cfg = FishingConfig.get().rodModules;
		if (cfg == null || !cfg.enabled || !RodModuleState.supports(rodStack)) {
			return EMPTY;
		}

		float progressLossReduction = 0.0f;
		float progressGainBonus = 0.0f;
		float biteSpeedBonus = 0.0f;
		float lengthBonus = 0.0f;
		float weightBonus = 0.0f;
		float rarityLuckBonus = 0.0f;

		for (RodModuleSlot slotType : RodModuleSlot.values()) {
			ItemStack moduleStack = RodModuleState.getInstalledModuleStack(rodStack, slotType);
			if (!(moduleStack.getItem() instanceof feuda.fishka.item.RodModuleItem moduleItem)) {
				continue;
			}
			float multiplier = RodModuleCatalog.tierMultiplier(cfg, moduleItem.tier());
			switch (slotType) {
				case HANDLE -> progressLossReduction = clamp(
					progressLossReduction + cfg.baseHandleProgressLossReduction * multiplier,
					0.0f,
					cfg.handleProgressLossReductionCap
				);
				case REEL -> progressGainBonus = clamp(
					progressGainBonus + cfg.baseReelProgressGainBonus * multiplier,
					0.0f,
					cfg.reelProgressGainBonusCap
				);
				case ROD -> lengthBonus = clamp(
					lengthBonus + cfg.baseRodLengthBonus * multiplier,
					0.0f,
					cfg.rodLengthBonusCap
				);
				case LINE -> weightBonus = clamp(
					weightBonus + cfg.baseLineWeightBonus * multiplier,
					0.0f,
					cfg.lineWeightBonusCap
				);
				case BOBBER -> biteSpeedBonus = clamp(
					biteSpeedBonus + cfg.baseBobberBiteSpeedBonus * multiplier,
					0.0f,
					cfg.bobberBiteSpeedBonusCap
				);
				case HOOK -> rarityLuckBonus = clamp(
					rarityLuckBonus + cfg.baseHookRarityLuckBonus * multiplier,
					0.0f,
					cfg.hookRarityLuckBonusCap
				);
			}
		}

		return new RodComponentBonus(
			0.0f,
			0.0f,
			progressLossReduction,
			0.0f,
			progressGainBonus,
			biteSpeedBonus,
			lengthBonus,
			weightBonus,
			rarityLuckBonus
		);
	}

	public record RodComponentBonus(
		float safeZoneWidthBonus,
		float holdGainBonus,
		float progressLossReduction,
		float fishForceReduction,
		float progressGainBonus,
		float biteSpeedBonus,
		float lengthBonus,
		float weightBonus,
		float rarityLuckBonus
	) {
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}

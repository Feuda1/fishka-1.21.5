package feuda.fishka.client.gui;

import feuda.fishka.fishing.FishingConfig;
import feuda.fishka.fishing.FishEncounterTier;
import feuda.fishka.fishing.net.FishkaCatchFeedbackS2CPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class CatchRewardHudState {
	private static ItemStack displayStack = ItemStack.EMPTY;
	private static Text title = Text.empty();
	private static Text statLine = Text.empty();
	private static int xp;
	private static int newLevel = -1;
	private static FishEncounterTier tier = FishEncounterTier.COMMON;
	private static boolean showRarityRays;

	private static Phase phase = Phase.INACTIVE;
	private static int phaseTicksLeft;
	private static int holdTicks;
	private static int revealDelayTicks;

	private CatchRewardHudState() {
	}

	public static void apply(FishkaCatchFeedbackS2CPayload payload) {
		FishingConfig.RewardHudConfig cfg = FishingConfig.get().rewardHud;
		displayStack = payload.displayStack().copy();
		title = payload.title();
		statLine = payload.statLine();
		xp = Math.max(0, payload.xp());
		newLevel = payload.newLevel();
		tier = payload.tier();
		showRarityRays = payload.showRarityRays();
		holdTicks = Math.max(1, payload.displayTicks());
		revealDelayTicks = Math.max(16, payload.revealDelayTicks());

		phase = Phase.TOTEM_PHASE;
		phaseTicksLeft = Math.max(22, cfg.totemPhaseTicks);
	}

	public static void tick(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			reset();
			return;
		}
		if (phase == Phase.INACTIVE) {
			return;
		}

		if (phaseTicksLeft > 0) {
			phaseTicksLeft--;
		}
		if (phaseTicksLeft > 0) {
			return;
		}

		FishingConfig.RewardHudConfig cfg = FishingConfig.get().rewardHud;
		int fadeInTicks = Math.max(10, cfg.fadeInTicks);
		int fadeOutTicks = Math.max(12, cfg.fadeOutTicks);
		switch (phase) {
			case TOTEM_PHASE -> {
				phase = Phase.WAIT_REVEAL_DELAY;
				int delay = revealDelayTicks > 0 ? revealDelayTicks : Math.max(0, cfg.revealDelayTicks);
				phaseTicksLeft = delay;
				if (phaseTicksLeft == 0) {
					phase = Phase.BANNER_FADE_IN;
					phaseTicksLeft = fadeInTicks;
				}
			}
			case WAIT_REVEAL_DELAY -> {
				phase = Phase.BANNER_FADE_IN;
				phaseTicksLeft = fadeInTicks;
			}
			case BANNER_FADE_IN -> {
				phase = Phase.BANNER_HOLD;
				phaseTicksLeft = holdTicks;
			}
			case BANNER_HOLD -> {
				phase = Phase.BANNER_FADE_OUT;
				phaseTicksLeft = fadeOutTicks;
			}
			case BANNER_FADE_OUT -> reset();
			case INACTIVE -> {
			}
		}
	}

	public static Snapshot snapshot() {
		if (phase == Phase.INACTIVE || displayStack.isEmpty()) {
			return Snapshot.inactive();
		}

		FishingConfig.RewardHudConfig cfg = FishingConfig.get().rewardHud;
		int fadeInTicks = Math.max(10, cfg.fadeInTicks);
		int fadeOutTicks = Math.max(12, cfg.fadeOutTicks);
		boolean bannerVisible = phase == Phase.BANNER_FADE_IN || phase == Phase.BANNER_HOLD || phase == Phase.BANNER_FADE_OUT;
		float bannerAlpha = switch (phase) {
			case BANNER_FADE_IN -> easeOutCubic(1.0f - (phaseTicksLeft / (float) fadeInTicks));
			case BANNER_HOLD -> 1.0f;
			case BANNER_FADE_OUT -> smoothStep(phaseTicksLeft / (float) fadeOutTicks);
			default -> 0.0f;
		};
		bannerAlpha = clamp01(bannerAlpha);

		boolean raysVisible = cfg.rayEnabled
			&& showRarityRays
			&& (phase == Phase.BANNER_FADE_IN || phase == Phase.BANNER_HOLD || phase == Phase.BANNER_FADE_OUT);
		float raysAlpha = raysVisible ? bannerAlpha : 0.0f;

		return new Snapshot(
			true,
			displayStack,
			title,
			statLine,
			xp,
			newLevel,
			tier,
			bannerVisible,
			bannerAlpha,
			raysVisible,
			raysAlpha,
			phase
		);
	}

	private static void reset() {
		displayStack = ItemStack.EMPTY;
		title = Text.empty();
		statLine = Text.empty();
		xp = 0;
		newLevel = -1;
		tier = FishEncounterTier.COMMON;
		showRarityRays = false;
		phase = Phase.INACTIVE;
		phaseTicksLeft = 0;
		holdTicks = 0;
		revealDelayTicks = 0;
	}

	private static float clamp01(float value) {
		return Math.max(0.0f, Math.min(1.0f, value));
	}

	private static float easeOutCubic(float t) {
		float x = clamp01(t);
		float inv = 1.0f - x;
		return 1.0f - inv * inv * inv;
	}

	private static float smoothStep(float t) {
		float x = clamp01(t);
		return x * x * (3.0f - 2.0f * x);
	}

	public enum Phase {
		INACTIVE,
		TOTEM_PHASE,
		WAIT_REVEAL_DELAY,
		BANNER_FADE_IN,
		BANNER_HOLD,
		BANNER_FADE_OUT
	}

	public record Snapshot(
		boolean active,
		ItemStack displayStack,
		Text title,
		Text statLine,
		int xp,
		int newLevel,
		FishEncounterTier tier,
		boolean bannerVisible,
		float bannerAlpha,
		boolean raysVisible,
		float raysAlpha,
		Phase phase
	) {
		private static Snapshot inactive() {
			return new Snapshot(
				false,
				ItemStack.EMPTY,
				Text.empty(),
				Text.empty(),
				0,
				-1,
				FishEncounterTier.COMMON,
				false,
				0.0f,
				false,
				0.0f,
				Phase.INACTIVE
			);
		}
	}
}

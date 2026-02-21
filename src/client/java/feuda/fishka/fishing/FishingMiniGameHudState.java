package feuda.fishka.fishing;

import feuda.fishka.fishing.net.FishkaMinigameLifecycleS2CPayload;
import feuda.fishka.fishing.net.FishkaMinigameStateS2CPayload;

public final class FishingMiniGameHudState {
	private static boolean active;
	private static int sessionId = -1;

	private static float previousCatchProgress;
	private static float previousTension;
	private static float previousTensionVelocity;
	private static float previousSafeZoneCenter;
	private static float previousSafeZoneHalfWidth;
	private static float previousFishForce;
	private static int previousTimeLeftTicks;
	private static int previousSoftStartTicksLeft;
	private static boolean previousBurstActive;
	private static FishEncounterTier previousTier = FishEncounterTier.COMMON;

	private static float catchProgress;
	private static float tension;
	private static float tensionVelocity;
	private static float safeZoneCenter;
	private static float safeZoneHalfWidth;
	private static float fishForce;
	private static int timeLeftTicks;
	private static int softStartTicksLeft;
	private static boolean burstActive;
	private static FishEncounterTier tier = FishEncounterTier.COMMON;

	private static long lastSnapshotNanos;

	private FishingMiniGameHudState() {
	}

	public static void applyState(FishkaMinigameStateS2CPayload payload) {
		if (!payload.active()) {
			active = false;
			sessionId = payload.sessionId();
			return;
		}

		if (payload.sessionId() != sessionId) {
			sessionId = payload.sessionId();
			previousCatchProgress = payload.catchProgress01();
			previousTension = payload.tension01();
			previousTensionVelocity = payload.tensionVelocity01();
			previousSafeZoneCenter = payload.safeZoneCenter01();
			previousSafeZoneHalfWidth = payload.safeZoneHalfWidth01();
			previousFishForce = payload.fishForce01();
			previousTimeLeftTicks = payload.timeLeftTicks();
			previousSoftStartTicksLeft = payload.softStartTicksLeft();
			previousBurstActive = payload.burstActive();
			previousTier = payload.tier();
		} else {
			previousCatchProgress = catchProgress;
			previousTension = tension;
			previousTensionVelocity = tensionVelocity;
			previousSafeZoneCenter = safeZoneCenter;
			previousSafeZoneHalfWidth = safeZoneHalfWidth;
			previousFishForce = fishForce;
			previousTimeLeftTicks = timeLeftTicks;
			previousSoftStartTicksLeft = softStartTicksLeft;
			previousBurstActive = burstActive;
			previousTier = tier;
		}

		catchProgress = payload.catchProgress01();
		tension = payload.tension01();
		tensionVelocity = payload.tensionVelocity01();
		safeZoneCenter = payload.safeZoneCenter01();
		safeZoneHalfWidth = payload.safeZoneHalfWidth01();
		fishForce = payload.fishForce01();
		timeLeftTicks = payload.timeLeftTicks();
		softStartTicksLeft = payload.softStartTicksLeft();
		burstActive = payload.burstActive();
		tier = payload.tier();
		lastSnapshotNanos = System.nanoTime();
		active = true;
	}

	public static void applyLifecycle(FishkaMinigameLifecycleS2CPayload payload) {
		sessionId = payload.sessionId();
		if (payload.event() != FishkaMinigameLifecycleS2CPayload.Event.START) {
			active = false;
		}
	}

	public static Snapshot snapshot() {
		if (!active) {
			return Snapshot.inactive();
		}

		int interpolationWindowMs = Math.max(1, FishingConfig.get().miniGameHud.interpolationWindowMs);
		float alpha = clamp01((System.nanoTime() - lastSnapshotNanos) / (interpolationWindowMs * 1_000_000.0f));

		float lerpCatch = lerp(previousCatchProgress, catchProgress, alpha);
		float lerpTension = lerp(previousTension, tension, alpha);
		float lerpTensionVelocity = lerp(previousTensionVelocity, tensionVelocity, alpha);
		float lerpSafeCenter = lerp(previousSafeZoneCenter, safeZoneCenter, alpha);
		float lerpSafeHalf = lerp(previousSafeZoneHalfWidth, safeZoneHalfWidth, alpha);
		float lerpForce = lerp(previousFishForce, fishForce, alpha);
		int lerpTimeLeft = Math.max(0, Math.round(lerp(previousTimeLeftTicks, timeLeftTicks, alpha)));
		int lerpSoftStartTicksLeft = Math.max(0, Math.round(lerp(previousSoftStartTicksLeft, softStartTicksLeft, alpha)));
		boolean lerpBurst = alpha < 0.5f ? previousBurstActive : burstActive;
		FishEncounterTier lerpTier = alpha < 0.5f ? previousTier : tier;

		return new Snapshot(
			true,
			sessionId,
			lerpCatch,
			lerpTension,
			lerpTensionVelocity,
			lerpSafeCenter,
			lerpSafeHalf,
			lerpForce,
			lerpTimeLeft,
			lerpSoftStartTicksLeft,
			lerpBurst,
			lerpTier
		);
	}

	private static float clamp01(float value) {
		return Math.max(0.0f, Math.min(1.0f, value));
	}

	private static float lerp(float start, float end, float alpha) {
		return start + (end - start) * alpha;
	}

	public record Snapshot(
		boolean active,
		int sessionId,
		float catchProgress01,
		float tension01,
		float tensionVelocity01,
		float safeZoneCenter01,
		float safeZoneHalfWidth01,
		float fishForce01,
		int timeLeftTicks,
		int softStartTicksLeft,
		boolean burstActive,
		FishEncounterTier tier
	) {
		private static Snapshot inactive() {
			return new Snapshot(false, -1, 0.0f, 0.0f, 0.5f, 0.5f, 0.1f, 0.5f, 0, 0, false, FishEncounterTier.COMMON);
		}
	}
}

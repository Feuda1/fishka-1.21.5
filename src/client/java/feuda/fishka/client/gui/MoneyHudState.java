package feuda.fishka.client.gui;

import feuda.fishka.fishing.net.FishkaFishingProfileS2CPayload;
import net.minecraft.client.MinecraftClient;

public final class MoneyHudState {
	private static boolean initialized;
	private static int targetMoney;
	private static float displayedMoney;

	private MoneyHudState() {
	}

	public static void apply(FishkaFishingProfileS2CPayload payload) {
		targetMoney = Math.max(0, payload.money());
		if (!initialized) {
			displayedMoney = targetMoney;
			initialized = true;
		}
	}

	public static void tick(MinecraftClient client) {
		if (!initialized) {
			return;
		}
		float target = targetMoney;
		displayedMoney += (target - displayedMoney) * 0.20f;
		if (Math.abs(displayedMoney - target) < 0.2f) {
			displayedMoney = target;
		}
	}

	public static Snapshot snapshot() {
		return new Snapshot(initialized, Math.max(0, Math.round(displayedMoney)));
	}

	public static void reset() {
		initialized = false;
		targetMoney = 0;
		displayedMoney = 0.0f;
	}

	public record Snapshot(boolean initialized, int money) {
	}
}

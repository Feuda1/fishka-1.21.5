package feuda.fishka.client.gui;

import feuda.fishka.fishing.FishEncounterTier;
import feuda.fishka.fishing.FishingConfig;
import feuda.fishka.fishing.FishingMiniGameHudState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class FishingMiniGameHudRenderer {
	private FishingMiniGameHudRenderer() {
	}

	public static void render(DrawContext context, RenderTickCounter tickCounter) {
		FishingMiniGameHudState.Snapshot snapshot = FishingMiniGameHudState.snapshot();
		if (!snapshot.active()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen instanceof FishingSkillTreeScreen) {
			return;
		}
		FishingConfig.MiniGameHudConfig cfg = FishingConfig.get().miniGameHud;
		FishingConfig.MiniGameBalanceConfig balanceCfg = FishingConfig.get().miniGameBalance;

		boolean burstActive = snapshot.burstActive();
		boolean softStart = snapshot.softStartTicksLeft() > 0;

		int panelW = clampInt(cfg.frameWidth, 280, 420);
		int panelH = clampInt(cfg.frameHeight, 60, 110);
		int x = (client.getWindow().getScaledWidth() - panelW) / 2;
		int y = client.getWindow().getScaledHeight() - cfg.yOffsetFromHotbar - panelH;

		if (burstActive && balanceCfg.burstHudShakeEnabled) {
			long time = client.world != null ? client.world.getTime() : 0L;
			int shakeX = Math.max(0, balanceCfg.burstHudShakeX);
			int shakeY = Math.max(0, balanceCfg.burstHudShakeY);
			x += ((time & 1L) == 0L ? -1 : 1) * shakeX;
			y += ((time & 2L) == 0L ? -1 : 1) * shakeY;
		}

		int tierColor = tierAccent(snapshot.tier());
		context.fill(x + 2, y + 2, x + panelW + 2, y + panelH + 2, 0x55000000);
		context.fill(x, y, x + panelW, y + panelH, 0xC0151A23);
		context.drawBorder(x, y, panelW, panelH, 0xFF7A6750);
		context.drawBorder(x + 1, y + 1, panelW - 2, panelH - 2, 0xFF2A3442);

		int barLeft = x + 18;
		int barRight = x + panelW - 18;
		int barWidth = barRight - barLeft;
		int barH = clampInt(Math.round(panelH * 0.16f), 10, 14);
		int contentTop = y + 10;
		int contentBottom = y + panelH - 10;
		int gap = Math.max(8, contentBottom - contentTop - barH * 2);
		int progressY = contentTop;
		int tensionY = progressY + barH + gap;

		context.fill(barLeft, progressY, barRight, progressY + barH, 0xAA0D1118);
		context.fill(barLeft, tensionY, barRight, tensionY + barH, 0xAA0D1118);

		float progress01 = clamp01(snapshot.catchProgress01());
		int progressW = Math.round(barWidth * progress01);
		if (progressW > 0) {
			context.fill(barLeft, progressY + 1, barLeft + progressW, progressY + 5, softStart ? 0xFF39A453 : 0xFF3BCB5D);
			context.fill(barLeft, progressY + 5, barLeft + progressW, progressY + barH - 1, softStart ? 0xFF26863F : 0xFF2DA84A);
		}
		context.drawBorder(barLeft - 1, progressY - 1, barWidth + 2, barH + 2, 0xFF4F3D2D);

		context.fill(barLeft, tensionY + 1, barRight, tensionY + 5, softStart ? 0xFF4EA4D8 : 0xFF5AB9F4);
		context.fill(barLeft, tensionY + 5, barRight, tensionY + barH - 1, softStart ? 0xFF2E79AB : 0xFF2A8BC8);
		context.drawBorder(barLeft - 1, tensionY - 1, barWidth + 2, barH + 2, 0xFF4F3D2D);

		float safeCenter = clamp01(snapshot.safeZoneCenter01());
		float safeHalf = Math.max(0.02f, Math.min(0.30f, snapshot.safeZoneHalfWidth01()));
		int safeStart = barLeft + Math.round((safeCenter - safeHalf) * barWidth);
		int safeEnd = barLeft + Math.round((safeCenter + safeHalf) * barWidth);
		safeStart = Math.max(barLeft, safeStart);
		safeEnd = Math.min(barRight, safeEnd);

		int safeFill = burstActive ? withAlpha(tierColor, 0x90) : withAlpha(tierColor, 0x66);
		context.fill(safeStart, tensionY - 1, safeEnd, tensionY + barH + 1, safeFill);
		context.drawBorder(safeStart, tensionY - 1, Math.max(1, safeEnd - safeStart), barH + 2, withAlpha(tierColor, 0xF8));

		int markerX = barLeft + Math.round(clamp01(snapshot.tension01()) * barWidth);
		context.fill(markerX - 2, tensionY - 3, markerX + 2, tensionY + barH + 3, 0x66FFFFFF);
		context.fill(markerX - 1, tensionY - 2, markerX + 1, tensionY + barH + 2, 0xFFFFFFFF);

		if (burstActive) {
			context.fill(x + 2, y + 2, x + panelW - 2, y + 5, withAlpha(tierColor, 0x72));
			context.fill(x + 2, y + panelH - 5, x + panelW - 2, y + panelH - 2, withAlpha(tierColor, 0x72));
		}
	}

	private static int tierAccent(FishEncounterTier tier) {
		return switch (tier) {
			case COMMON -> 0xEBCB3F;
			case UNCOMMON -> 0x6FDB7D;
			case RARE -> 0x5AA7FF;
			case EPIC -> 0xD681FF;
			case LEGENDARY -> 0xFFD35A;
		};
	}

	private static int withAlpha(int rgb, int alpha) {
		return (alpha << 24) | (rgb & 0x00FFFFFF);
	}

	private static float clamp01(float value) {
		return Math.max(0.0f, Math.min(1.0f, value));
	}

	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}

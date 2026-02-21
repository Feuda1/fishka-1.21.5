package feuda.fishka.client.gui;

import feuda.fishka.fishing.FishingConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class FishingLevelHudRenderer {
	private FishingLevelHudRenderer() {
	}

	public static void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.options.hudHidden) {
			return;
		}
		if (client.currentScreen instanceof FishingSkillTreeScreen) {
			return;
		}

		FishingConfig.FishingLevelHudConfig cfg = FishingConfig.get().fishingLevelHud;
		if (!cfg.enabled) {
			return;
		}

		FishingLevelHudState.Snapshot snapshot = FishingLevelHudState.snapshot();
		if (!snapshot.active()) {
			return;
		}

		int screenW = client.getWindow().getScaledWidth();
		int screenH = client.getWindow().getScaledHeight();
		int width = clampInt(cfg.width, 180, 300);
		int height = clampInt(cfg.height, 32, 70);
		boolean hasUnusedSkillPoints = snapshot.availableSkillPoints() > 0;
		int extraBottomSpace = hasUnusedSkillPoints ? 12 : 0;
		int x = clampInt(cfg.offsetX, 2, Math.max(2, screenW - width - 2));
		int y = clampInt(cfg.offsetY, 2, Math.max(2, screenH - height - extraBottomSpace - 2));
		int barHeight = clampInt(cfg.barHeight, 6, Math.max(6, height - 20));

		float fx = snapshot.levelUpFx01();
		if (cfg.showShadow) {
			context.fill(x + 2, y + 2, x + width + 2, y + height + 2, withAlpha(0x000000, 96));
		}

		if (fx > 0.001f) {
			int glowAlpha = Math.round(55.0f * fx);
			context.fill(x - 2, y - 2, x + width + 2, y + height + 2, withAlpha(0xFFD35A, glowAlpha));
		}

		context.fill(x, y, x + width, y + height, 0xCC131922);
		int borderColor = mixColor(0x667A8A, 0xFFD35A, fx * 0.85f);
		context.drawBorder(x, y, width, height, withAlpha(borderColor, 255));

		TextRenderer textRenderer = client.textRenderer;
		Text title = Text.translatable("fishka.hud.level.title", snapshot.level());
		Text xpText = Text.translatable("fishka.hud.level.xp", snapshot.currentLevelXp(), snapshot.nextLevelXp());
		context.drawText(textRenderer, title, x + 7, y + 4, 0xFFFFE7A8, true);

		int barLeft = x + 8;
		int barRight = x + width - 8;
		int barWidth = Math.max(20, barRight - barLeft);
		int barY = y + 14;
		context.fill(barLeft, barY, barRight, barY + barHeight, 0xB0070A12);
		context.drawBorder(barLeft - 1, barY - 1, barWidth + 2, barHeight + 2, withAlpha(0x3E4C5E, 220));

		int fillWidth = Math.round((barWidth - 2) * clamp01(snapshot.displayedProgress01()));
		if (fillWidth > 0) {
			int fillStart = barLeft + 1;
			int fillEnd = fillStart + fillWidth;
			int topColor = mixColor(0x2FC8FF, 0xFFE08A, fx * 0.35f);
			int botColor = mixColor(0x1D8CCF, 0xFFB84F, fx * 0.25f);
			int midY = barY + Math.max(1, barHeight / 2);
			context.fill(fillStart, barY + 1, fillEnd, midY, withAlpha(topColor, 240));
			context.fill(fillStart, midY, fillEnd, barY + barHeight - 1, withAlpha(botColor, 240));

			float worldTime = (client.world != null ? client.world.getTime() : 0.0f) + tickCounter.getTickProgress(false);
			float phase = (worldTime * clamp(cfg.shineSpeed, 0.01f, 2.0f)) % 1.0f;
			int shineX = fillStart + Math.round(phase * Math.max(1, fillWidth - 1));
			context.fill(shineX - 1, barY + 1, shineX + 2, barY + barHeight - 1, withAlpha(0xFFFFFF, 110));
		}

		context.drawText(textRenderer, xpText, x + 7, barY + barHeight + 3, 0xFFBFE8FF, false);
		if (hasUnusedSkillPoints) {
			Text skillPointsText = formatUnusedSkillPointsText(snapshot.availableSkillPoints());
			int pointsX = MathHelper.clamp(x + 2, 2, Math.max(2, screenW - textRenderer.getWidth(skillPointsText) - 2));
			context.drawText(textRenderer, skillPointsText, pointsX, y + height + 2, 0xFF9CF2A9, true);
		}

		if (snapshot.levelUpActive()) {
			Text levelUpText = Text.translatable("fishka.hud.level.up");
			int textWidth = textRenderer.getWidth(levelUpText);
			int textX = x + width - textWidth - 7;
			int textAlpha = Math.round(170.0f + 85.0f * fx);
			context.drawText(textRenderer, levelUpText, textX, y - 10, withAlpha(0xFFD35A, textAlpha), true);
		}
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

	private static int mixColor(int fromRgb, int toRgb, float alpha) {
		float t = clamp01(alpha);
		int fr = (fromRgb >> 16) & 0xFF;
		int fg = (fromRgb >> 8) & 0xFF;
		int fb = fromRgb & 0xFF;
		int tr = (toRgb >> 16) & 0xFF;
		int tg = (toRgb >> 8) & 0xFF;
		int tb = toRgb & 0xFF;
		int r = Math.round(fr + (tr - fr) * t);
		int g = Math.round(fg + (tg - fg) * t);
		int b = Math.round(fb + (tb - fb) * t);
		return (r << 16) | (g << 8) | b;
	}

	private static int withAlpha(int rgb, int alpha) {
		return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
	}

	private static Text formatUnusedSkillPointsText(int points) {
		int mod10 = points % 10;
		int mod100 = points % 100;
		if (mod10 == 1 && mod100 != 11) {
			return Text.translatable("fishka.hud.level.skill_points.one", points);
		}
		if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
			return Text.translatable("fishka.hud.level.skill_points.few", points);
		}
		return Text.translatable("fishka.hud.level.skill_points.many", points);
	}
}

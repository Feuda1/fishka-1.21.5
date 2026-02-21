package feuda.fishka.client.gui;

import feuda.fishka.fishing.FishingConfig;
import feuda.fishka.fishing.FishEncounterTier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class CatchRewardHudRenderer {
	private CatchRewardHudRenderer() {
	}

	public static void render(DrawContext context, RenderTickCounter tickCounter) {
		CatchRewardHudState.Snapshot snapshot = CatchRewardHudState.snapshot();
		if (!snapshot.active()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen instanceof FishingSkillTreeScreen) {
			return;
		}
		TextRenderer textRenderer = client.textRenderer;
		FishingConfig.RewardHudConfig cfg = FishingConfig.get().rewardHud;

		int screenW = client.getWindow().getScaledWidth();
		int screenH = client.getWindow().getScaledHeight();

		if (!snapshot.bannerVisible()) {
			return;
		}

		Text title = ellipsize(snapshot.title(), textRenderer, cfg.maxBannerWidth - 52);
		Text statLine = ellipsize(snapshot.statLine(), textRenderer, cfg.maxBannerWidth - 52);
		Text xpText = Text.translatable("fishka.reward.xp", snapshot.xp());
		Text levelText = snapshot.newLevel() > 0 ? Text.translatable("fishka.reward.new_level", snapshot.newLevel()) : Text.empty();

		int contentWidth = Math.max(textRenderer.getWidth(title), textRenderer.getWidth(statLine));
		contentWidth = Math.max(contentWidth, textRenderer.getWidth(xpText));
		if (snapshot.newLevel() > 0) {
			contentWidth = Math.max(contentWidth, textRenderer.getWidth(levelText));
		}

		int boxW = Math.max(190, Math.min(cfg.maxBannerWidth, contentWidth + 50));
		int boxH = snapshot.newLevel() > 0 ? 56 : 44;
		int x = (screenW - boxW) / 2;
		int y = (screenH / 2) + cfg.bannerOffsetY;
		if (snapshot.phase() == CatchRewardHudState.Phase.BANNER_FADE_IN) {
			int slidePx = Math.round((1.0f - snapshot.bannerAlpha()) * 10.0f);
			y += slidePx;
		}
		if (snapshot.raysVisible()) {
			renderRarityRays(context, x, y, boxW, boxH, snapshot, cfg, client);
		}

		int textColor = 0xFFFFFFFF;
		int subColor = 0xFFC8D0D6;
		int bg = 0xCC11161E;
		int border = 0xFF5A7A8F;

		context.fill(x, y, x + boxW, y + boxH, bg);
		context.drawBorder(x, y, boxW, boxH, border);

		context.drawItem(snapshot.displayStack(), x + 8, y + 13);

		context.drawText(textRenderer, title, x + 30, y + 6, textColor, false);
		context.drawText(textRenderer, statLine, x + 30, y + 18, subColor, false);
		context.drawText(textRenderer, xpText, x + 30, y + 31, 0xFF5FD7FF, false);

		if (snapshot.newLevel() > 0) {
			context.drawText(textRenderer, levelText, x + 30, y + 42, 0xFFFFD35A, false);
		}

		int overlayAlpha = Math.round((1.0f - clamp01(snapshot.bannerAlpha())) * 255.0f);
		if (overlayAlpha > 0) {
			context.fill(x, y, x + boxW, y + boxH, withAlpha(0x11161E, overlayAlpha));
		}
	}

	private static void renderRarityRays(
		DrawContext context,
		int boxX,
		int boxY,
		int boxW,
		int boxH,
		CatchRewardHudState.Snapshot snapshot,
		FishingConfig.RewardHudConfig cfg,
		MinecraftClient client
	) {
		float intensity = Math.max(0.0f, cfg.rayIntensityForTier(snapshot.tier()));
		if (intensity <= 0.01f) {
			return;
		}

		int baseRgb = colorForTier(snapshot.tier());
		float phaseTime = (float) (System.nanoTime() * 1.0e-9);
		float baseAlpha = clamp01(snapshot.raysAlpha()) * (0.58f + 0.42f * intensity);
		int rays = Math.max(4, cfg.rayCount);
		float pulseSpeed = Math.max(0.01f, cfg.rayPulseSpeed);
		float halfW = boxW * 0.5f;
		float halfH = boxH * 0.5f;
		float orbitX = halfW + cfg.rayOrbitRadiusX;
		float orbitY = halfH + cfg.rayOrbitRadiusY;
		float centerX = boxX + halfW;
		float centerY = boxY + halfH;
		if (!cfg.rayAroundBanner) {
			centerX = boxX + 16.0f;
			centerY = boxY + halfH;
			orbitX = 8.0f + cfg.rayOrbitRadiusX;
			orbitY = 8.0f + cfg.rayOrbitRadiusY;
		}

		for (int i = 0; i < rays; i++) {
			float baseAngle = (float) ((Math.PI * 2.0 / rays) * i);
			float drift = (float) (Math.sin(phaseTime * 0.26f + i * 0.83f) * 0.02f);
			float angle = baseAngle + drift;
			float pulse = (float) (0.58f + 0.42f * Math.sin(phaseTime * pulseSpeed + i * 0.91f));
			float anchorX = centerX + (float) Math.cos(angle) * orbitX;
			float anchorY = centerY + (float) Math.sin(angle) * orbitY;
			float length = (18.0f + 34.0f * pulse) * intensity;
			float dirX = (float) Math.cos(angle);
			float dirY = (float) Math.sin(angle);
			float rayThickness = 1.0f + 2.6f * pulse * intensity;

			for (float p = 0.0f; p <= length; p += 1.6f) {
				float fade = 1.0f - (p / (length + 1.0f));
				float glow = 0.45f + 0.55f * fade;
				int alpha = Math.round(255.0f * baseAlpha * fade * glow);
				if (alpha <= 3) {
					continue;
				}
				int px = Math.round(anchorX + dirX * p);
				int py = Math.round(anchorY + dirY * p);
				int radius = Math.max(1, Math.round(rayThickness * (0.5f + fade * 0.5f)));
				context.fill(px - radius, py - radius, px + radius + 1, py + radius + 1, withAlpha(baseRgb, alpha));
			}

			for (float p = 0.0f; p <= length * 0.4f; p += 1.8f) {
				float fade = 1.0f - (p / (length * 0.4f + 1.0f));
				int alpha = Math.round(255.0f * baseAlpha * 0.46f * fade);
				if (alpha <= 4) {
					continue;
				}
				int px = Math.round(anchorX + dirX * p);
				int py = Math.round(anchorY + dirY * p);
				context.fill(px - 1, py - 1, px + 2, py + 2, withAlpha(0xFFFFFF, alpha));
			}

			for (float p = 0.0f; p <= length * 0.9f; p += 2.8f) {
				float fade = 1.0f - (p / (length * 0.9f + 1.0f));
				int alpha = Math.round(255.0f * baseAlpha * 0.28f * fade);
				if (alpha <= 4) {
					continue;
				}
				int px = Math.round(anchorX + dirX * p);
				int py = Math.round(anchorY + dirY * p);
				context.fill(px - 2, py - 2, px + 3, py + 3, withAlpha(baseRgb, alpha));
			}
		}
	}

	private static Text ellipsize(Text source, TextRenderer textRenderer, int maxWidth) {
		if (textRenderer.getWidth(source) <= maxWidth) {
			return source;
		}
		String raw = source.getString();
		int dotsWidth = textRenderer.getWidth("...");
		String trimmed = textRenderer.trimToWidth(raw, Math.max(0, maxWidth - dotsWidth));
		if (trimmed.isEmpty()) {
			return Text.literal("...");
		}
		return Text.literal(trimmed + "...").setStyle(source.getStyle());
	}

	private static int colorForTier(FishEncounterTier tier) {
		return switch (tier) {
			case COMMON -> 0x9EC8FF;
			case UNCOMMON -> 0x69E79A;
			case RARE -> 0x56A5FF;
			case EPIC -> 0xCF78FF;
			case LEGENDARY -> 0xFFD35A;
		};
	}

	private static float clamp01(float value) {
		return Math.max(0.0f, Math.min(1.0f, value));
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int withAlpha(int rgb, int alpha) {
		return (alpha << 24) | (rgb & 0x00FFFFFF);
	}
}

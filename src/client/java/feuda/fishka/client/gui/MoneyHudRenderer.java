package feuda.fishka.client.gui;

import feuda.fishka.fishing.FishingConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class MoneyHudRenderer {
	private MoneyHudRenderer() {
	}

	public static void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.options.hudHidden) {
			return;
		}
		if (client.currentScreen instanceof FishingSkillTreeScreen) {
			return;
		}
		FishingConfig.MoneyHudConfig cfg = FishingConfig.get().moneyHud;
		if (!cfg.enabled) {
			return;
		}

		MoneyHudState.Snapshot snapshot = MoneyHudState.snapshot();
		if (!snapshot.initialized()) {
			return;
		}

		String format = cfg.format == null || cfg.format.isBlank() ? "$%s" : cfg.format;
		Text moneyText = Text.literal(format.formatted(snapshot.money()));

		int textW = client.textRenderer.getWidth(moneyText);
		int x = Math.max(2, client.getWindow().getScaledWidth() - textW - Math.max(2, cfg.offsetX));
		int y = Math.max(2, cfg.offsetY);

		if (cfg.showShadow) {
			context.fill(x - 4, y - 2, x + textW + 4, y + 11, 0x66000000);
		}
		context.drawText(client.textRenderer, moneyText, x, y, 0xFFFFD35A, true);
	}
}

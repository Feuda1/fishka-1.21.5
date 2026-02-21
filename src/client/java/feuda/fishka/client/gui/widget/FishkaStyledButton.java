package feuda.fishka.client.gui.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public final class FishkaStyledButton extends ClickableWidget {
	private final PressAction onPress;

	public FishkaStyledButton(int x, int y, int width, int height, Text message, PressAction onPress) {
		super(x, y, width, height, message);
		this.onPress = onPress;
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		if (!this.active || !this.visible) {
			return;
		}
		this.playDownSound(net.minecraft.client.MinecraftClient.getInstance().getSoundManager());
		this.onPress.onPress(this);
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		if (!this.visible) {
			return;
		}

		int x = this.getX();
		int y = this.getY();
		int w = this.getWidth();
		int h = this.getHeight();
		boolean hovered = this.isHovered();

		int bg = !this.active ? 0xAA1A1F28 : (hovered ? 0xCC1F2B3A : 0xCC16212E);
		int border = !this.active ? 0xFF505867 : (hovered ? 0xFFFFD980 : 0xFFBFA96B);
		int glow = !this.active ? 0x00000000 : (hovered ? 0x28FFD980 : 0x14000000);

		if (glow != 0) {
			context.fill(x - 1, y - 1, x + w + 1, y + h + 1, glow);
		}
		context.fill(x, y, x + w, y + h, bg);
		context.drawBorder(x, y, w, h, border);
		context.fill(x + 1, y + 1, x + w - 1, y + 2, hovered ? 0x44FFFFFF : 0x22FFFFFF);

		int textColor = !this.active ? 0xFF8A909A : (hovered ? 0xFFFFF2CE : 0xFFE2E8F0);
		int textX = x + w / 2;
		int textY = y + (h - 8) / 2;
		context.drawCenteredTextWithShadow(
			net.minecraft.client.MinecraftClient.getInstance().textRenderer,
			this.getMessage(),
			textX,
			textY,
			textColor
		);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		appendDefaultNarrations(builder);
	}

	@FunctionalInterface
	public interface PressAction {
		void onPress(FishkaStyledButton button);
	}
}

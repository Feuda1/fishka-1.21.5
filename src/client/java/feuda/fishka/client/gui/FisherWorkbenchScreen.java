package feuda.fishka.client.gui;

import feuda.fishka.client.gui.widget.FishkaStyledButton;
import feuda.fishka.fishing.FishingMiniGameClientController;
import feuda.fishka.screen.FisherWorkbenchScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

public final class FisherWorkbenchScreen extends HandledScreen<FisherWorkbenchScreenHandler> {
	private static final int LOCK_PANEL_WIDTH = 152;
	private static final int LOCK_PANEL_HEIGHT = 80;
	private FishkaStyledButton openTreeButton;

	public FisherWorkbenchScreen(FisherWorkbenchScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 176;
		this.backgroundHeight = 166;
	}

	@Override
	protected void init() {
		super.init();
		openTreeButton = new FishkaStyledButton(
			0,
			0,
			LOCK_PANEL_WIDTH - 20,
			20,
			Text.translatable("fishka.workbench.open_tree"),
			button -> {
				if (this.client != null && this.client.player != null) {
					this.client.player.closeHandledScreen();
				}
				FishingMiniGameClientController.openSkillTreeScreen(this.client);
			}
		);
		addDrawableChild(openTreeButton);
		updateButtonLayout();
		updateButtonState();
	}

	@Override
	protected void handledScreenTick() {
		super.handledScreenTick();
		updateButtonLayout();
		updateButtonState();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fillGradient(0, 0, this.width, this.height, 0xB60A111A, 0xC0101A28);
		context.fill(0, 0, this.width, this.height, 0x26000000);
		if (!handler.isUnlocked()) {
			boolean renderButton = openTreeButton != null && openTreeButton.visible;
			if (renderButton) {
				openTreeButton.visible = false;
			}
			super.render(context, -10_000, -10_000, delta);
			if (renderButton) {
				openTreeButton.visible = true;
			}
			context.getMatrices().push();
			context.getMatrices().translate(0.0F, 0.0F, 450.0F);
			renderLockedOverlay(context);
			if (renderButton) {
				openTreeButton.render(context, mouseX, mouseY, delta);
			}
			context.getMatrices().pop();
			return;
		}
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		context.fill(this.x - 3, this.y - 3, this.x + this.backgroundWidth + 3, this.y + this.backgroundHeight + 3, 0x40000000);
		context.fill(this.x, this.y, this.x + this.backgroundWidth, this.y + this.backgroundHeight, 0xCC151B26);
		context.drawBorder(this.x, this.y, this.backgroundWidth, this.backgroundHeight, 0xFF5A7290);
		context.fill(this.x + 1, this.y + 1, this.x + this.backgroundWidth - 1, this.y + 3, 0x24FFFFFF);

		if (!handler.isUnlocked()) {
			return;
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int slotX = this.x + 30 + col * 18;
				int slotY = this.y + 17 + row * 18;
				context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x88101824);
				context.drawBorder(slotX, slotY, 16, 16, 0xFF3E526A);
			}
		}
		int resultX = this.x + 124;
		int resultY = this.y + 35;
		context.fill(resultX, resultY, resultX + 16, resultY + 16, 0x88101824);
		context.drawBorder(resultX, resultY, 16, 16, 0xFF6D84A1);
		context.drawText(this.textRenderer, ">", this.x + 104, this.y + 40, 0xFFD9E7F7, false);
	}

	private void renderLockedOverlay(DrawContext context) {
		context.fill(this.x + 1, this.y + 1, this.x + this.backgroundWidth - 1, this.y + this.backgroundHeight - 1, 0xFF0F1824);
		context.drawBorder(this.x + 1, this.y + 1, this.backgroundWidth - 2, this.backgroundHeight - 2, 0xFF6E86A5);
		int lockW = LOCK_PANEL_WIDTH;
		int lockH = LOCK_PANEL_HEIGHT;
		int lockX = this.x + (this.backgroundWidth - lockW) / 2;
		int lockY = this.y + 24;
		context.fill(lockX, lockY, lockX + lockW, lockY + lockH, 0xFF111C2A);
		context.drawBorder(lockX, lockY, lockW, lockH, 0xFF8AA3C2);
		context.fill(lockX + 1, lockY + 1, lockX + lockW - 1, lockY + 2, 0x20FFFFFF);
		context.drawCenteredTextWithShadow(
			this.textRenderer,
			Text.translatable("fishka.workbench.locked.title"),
			this.x + this.backgroundWidth / 2,
			lockY + 10,
			0xFFECD7A4
		);
		List<OrderedText> wrappedDesc = this.textRenderer.wrapLines(Text.translatable("fishka.workbench.locked.desc"), lockW - 20);
		int descY = lockY + 26;
		for (int i = 0; i < wrappedDesc.size() && i < 2; i++) {
			context.drawText(this.textRenderer, wrappedDesc.get(i), lockX + 10, descY, 0xFFB9C7D5, false);
			descY += 10;
		}
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		if (handler.isUnlocked()) {
			context.drawText(this.textRenderer, this.title, 8, 6, 0xFFE6EEF8, false);
			context.drawText(this.textRenderer, this.playerInventoryTitle, 8, this.backgroundHeight - 94, 0xFF9FB3CB, false);
		}
	}

	private void updateButtonState() {
		if (openTreeButton != null) {
			openTreeButton.visible = !handler.isUnlocked();
			openTreeButton.active = !handler.isUnlocked();
		}
	}

	private void updateButtonLayout() {
		if (openTreeButton != null) {
			int lockX = this.x + (this.backgroundWidth - LOCK_PANEL_WIDTH) / 2;
			int lockY = this.y + 24;
			openTreeButton.setPosition(lockX + 10, lockY + 48);
		}
	}
}

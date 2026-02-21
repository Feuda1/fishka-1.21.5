package feuda.fishka.client.gui;

import feuda.fishka.fishing.RodModuleSlot;
import feuda.fishka.screen.RodModuleScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public final class RodModuleScreen extends HandledScreen<RodModuleScreenHandler> {
	private static final int MODULE_AREA_X = 16;
	private static final int MODULE_AREA_Y = 20;
	private static final int MODULE_AREA_W = 198;
	private static final int MODULE_AREA_H = 132;
	private static final int CENTER_X = 115;
	private static final int CENTER_Y = 76;

	public RodModuleScreen(RodModuleScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 230;
		this.backgroundHeight = 262;
		this.titleX = 10;
		this.titleY = 8;
		this.playerInventoryTitleX = 25;
		this.playerInventoryTitleY = 158;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fillGradient(0, 0, this.width, this.height, 0xEE0B1524, 0xF10E1A2A);
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
		renderModuleTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int x1 = this.x;
		int y1 = this.y;
		int x2 = x1 + this.backgroundWidth;
		int y2 = y1 + this.backgroundHeight;
		context.fill(x1 - 4, y1 - 4, x2 + 4, y2 + 4, 0x42000000);
		context.fill(x1, y1, x2, y2, 0xFF0E1827);
		context.drawBorder(x1, y1, this.backgroundWidth, this.backgroundHeight, 0xFF7A97BC);
		context.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 3, 0x2CFFFFFF);

		int moduleX = x1 + MODULE_AREA_X;
		int moduleY = y1 + MODULE_AREA_Y;
		context.fill(moduleX, moduleY, moduleX + MODULE_AREA_W, moduleY + MODULE_AREA_H, 0xCC101F32);
		context.drawBorder(moduleX, moduleY, MODULE_AREA_W, MODULE_AREA_H, 0xFF4D6788);

		int centerX = x1 + CENTER_X;
		int centerY = y1 + CENTER_Y;

		for (RodModuleSlot slot : RodModuleSlot.values()) {
			int slotX = x1 + RodModuleScreenHandler.moduleSlotX(slot);
			int slotY = y1 + RodModuleScreenHandler.moduleSlotY(slot);
			drawConnectionLine(context, centerX, centerY, slotX + 8, slotY + 8);
		}
		context.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, 0xFFE4EEFC);

		for (RodModuleSlot slot : RodModuleSlot.values()) {
			int slotX = x1 + RodModuleScreenHandler.moduleSlotX(slot);
			int slotY = y1 + RodModuleScreenHandler.moduleSlotY(slot);
			boolean hasModule = !handler.getModuleStack(slot).isEmpty();
			int fillColor = hasModule ? 0xCC1C324A : 0xA6101B2A;
			int borderColor = hasModule ? 0xFFD4A84F : 0xFF6A83A3;
			context.fill(slotX, slotY, slotX + 16, slotY + 16, fillColor);
			context.drawBorder(slotX, slotY, 16, 16, borderColor);
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				int slotX = x1 + 25 + col * 18;
				int slotY = y1 + 170 + row * 18;
				context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x7E0F1824);
				context.drawBorder(slotX, slotY, 16, 16, 0xFF3F5674);
			}
		}
		for (int col = 0; col < 9; col++) {
			int slotX = x1 + 25 + col * 18;
			int slotY = y1 + 228;
			context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x7E0F1824);
			context.drawBorder(slotX, slotY, 16, 16, 0xFF3F5674);
		}
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0xFFE8F0FA, false);
		if (!handler.getHostRodStack().isEmpty()) {
			context.drawItem(handler.getHostRodStack(), CENTER_X - 8, CENTER_Y - 8);
		}
		context.drawText(this.textRenderer, Text.translatable("fishka.rod_modules.inventory"), this.playerInventoryTitleX, this.playerInventoryTitleY, 0xFF9FB3CB, false);

		for (RodModuleSlot slot : RodModuleSlot.values()) {
			int slotX = RodModuleScreenHandler.moduleSlotX(slot);
			int slotY = RodModuleScreenHandler.moduleSlotY(slot);
			Text labelText = Text.translatable(slot.translationKey());
			int labelWidth = this.textRenderer.getWidth(labelText);
			int labelX = MathHelper.clamp(slotX + 8 - labelWidth / 2, 4, this.backgroundWidth - labelWidth - 4);
			int labelY = switch (slot) {
				case ROD -> slotY - 16;
				case BOBBER -> slotY + 20;
				default -> slotY + 18;
			};
			drawLabelWithBackdrop(context, labelText, labelX, labelY, labelWidth);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private void renderModuleTooltip(DrawContext context, int mouseX, int mouseY) {
		RodModuleSlot hovered = getHoveredModuleSlot(mouseX, mouseY);
		if (hovered == null || !handler.getModuleStack(hovered).isEmpty()) {
			return;
		}
		List<Text> lines = new ArrayList<>();
		lines.add(Text.translatable(hovered.translationKey()));
		lines.add(Text.translatable("fishka.rod_modules.empty_slot"));
		lines.add(Text.translatable("fishka.rod_modules.accepts", Text.translatable(hovered.translationKey())));
		context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
	}

	private RodModuleSlot getHoveredModuleSlot(int mouseX, int mouseY) {
		for (RodModuleSlot slot : RodModuleSlot.values()) {
			int slotX = this.x + RodModuleScreenHandler.moduleSlotX(slot);
			int slotY = this.y + RodModuleScreenHandler.moduleSlotY(slot);
			if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
				return slot;
			}
		}
		return null;
	}

	private void drawConnectionLine(DrawContext context, int x1, int y1, int x2, int y2) {
		int dx = x2 - x1;
		int dy = y2 - y1;
		int max = Math.max(Math.abs(dx), Math.abs(dy));
		if (max == 0) {
			return;
		}

		// Cut exactly on slot border (slot is 16x16 -> half size 8 around slot center).
		double borderOffset = 8.0 / max;
		double endX = x2 - dx * borderOffset;
		double endY = y2 - dy * borderOffset;

		int ex = Math.round((float) endX);
		int ey = Math.round((float) endY);

		// Safety: never draw into slot interior after rounding.
		int stepX = Integer.compare(x1, x2);
		int stepY = Integer.compare(y1, y2);
		while (Math.abs(ex - x2) < 8 && Math.abs(ey - y2) < 8) {
			ex += stepX;
			ey += stepY;
		}

		drawBresenhamLine(context, x1, y1, ex, ey, 0xFFC8DCF4);
	}

	private void drawLabelWithBackdrop(DrawContext context, Text label, int x, int y, int width) {
		int pad = 1;
		int h = this.textRenderer.fontHeight;
		context.fill(x - pad, y - pad, x + width + pad, y + h + pad, 0xCC0E1827);
		context.drawTextWithShadow(this.textRenderer, label, x, y, 0xFFD7E6F7);
	}

	private void drawBresenhamLine(DrawContext context, int x0, int y0, int x1, int y1, int color) {
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx - dy;

		while (true) {
			context.fill(x0, y0, x0 + 1, y0 + 1, color);
			if (x0 == x1 && y0 == y1) {
				break;
			}
			int e2 = err * 2;
			if (e2 > -dy) {
				err -= dy;
				x0 += sx;
			}
			if (e2 < dx) {
				err += dx;
				y0 += sy;
			}
		}
	}

}

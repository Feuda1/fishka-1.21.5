package feuda.fishka.client.gui;

import feuda.fishka.client.gui.widget.FishkaStyledButton;
import feuda.fishka.fishing.FishingConfig;
import feuda.fishka.fishing.FishingSkillNode;
import feuda.fishka.fishing.net.FishkaSkillTreeActionC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class FishingSkillTreeScreen extends Screen {
	private static final int CENTER_NODE_DIAMETER = 42;
	private static final int BRANCH_NODE_DIAMETER = 30;
	private static final int POPUP_WIDTH = 196;
	private static final int POPUP_HEIGHT = 88;
	private static final int TOOLTIP_MAX_WIDTH = 220;
	private static final int CHIP_HEIGHT = 16;
	private static final int CHIP_GAP = 6;

	private static final NodeVisual CENTER_NODE = new NodeVisual(
		FishingSkillNode.UNLOCK_FISHER_CRAFTING.id(),
		0,
		0,
		CENTER_NODE_DIAMETER,
		new ItemStack(Items.CRAFTING_TABLE),
		Text.translatable("fishka.skill_tree.node.crafting.title"),
		Text.translatable("fishka.skill_tree.node.crafting.desc")
	);

	private static final NodeVisual[] BRANCH_NODES = new NodeVisual[] {
		new NodeVisual(-2, -172, -110, BRANCH_NODE_DIAMETER, new ItemStack(Items.LEATHER_CHESTPLATE), Text.translatable("fishka.skill_tree.branch.equipment"), Text.translatable("fishka.skill_tree.node.soon.desc")),
		new NodeVisual(-3, 172, -110, BRANCH_NODE_DIAMETER, new ItemStack(Items.ANVIL), Text.translatable("fishka.skill_tree.branch.craft"), Text.translatable("fishka.skill_tree.node.soon.desc")),
		new NodeVisual(-4, -184, 118, BRANCH_NODE_DIAMETER, new ItemStack(Items.COD), Text.translatable("fishka.skill_tree.branch.catch"), Text.translatable("fishka.skill_tree.node.soon.desc")),
		new NodeVisual(-5, 184, 118, BRANCH_NODE_DIAMETER, new ItemStack(Items.BONE), Text.translatable("fishka.skill_tree.branch.pets"), Text.translatable("fishka.skill_tree.node.soon.desc"))
	};

	private FishkaStyledButton popupConfirmButton;
	private FishkaStyledButton popupCancelButton;
	private NodeVisual hoveredNode;
	private boolean respecPopupVisible;
	private int popupAnchorX;
	private int popupAnchorY;
	private int popupX;
	private int popupY;

	public FishingSkillTreeScreen() {
		super(Text.translatable("fishka.skill_tree.title"));
	}

	@Override
	protected void init() {
		this.popupConfirmButton = addDrawableChild(
			new FishkaStyledButton(
				0,
				0,
				88,
				20,
				Text.translatable("fishka.skill_tree.popup.confirm"),
				button -> {
					sendSkillAction(FishkaSkillTreeActionC2SPayload.Action.RESPEC_ALL, -1);
					closePopup();
				}
			)
		);
		this.popupCancelButton = addDrawableChild(
			new FishkaStyledButton(
				0,
				0,
				88,
				20,
				Text.translatable("fishka.skill_tree.popup.cancel"),
				button -> closePopup()
			)
		);
		closePopup();
	}

	@Override
	public void tick() {
		super.tick();
		FishingSkillTreeState.Snapshot snapshot = FishingSkillTreeState.snapshot();
		if (popupConfirmButton != null) {
			popupConfirmButton.active = snapshot.initialized()
				&& snapshot.skillPointsSpent() > 0
				&& snapshot.money() >= FishingConfig.get().skillTree.respecCostMoney;
		}
		if (respecPopupVisible && !snapshot.isUnlocked(FishingSkillNode.UNLOCK_FISHER_CRAFTING)) {
			closePopup();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		drawBackgroundLayer(context);

		FishingSkillTreeState.Snapshot snapshot = FishingSkillTreeState.snapshot();
		int centerX = width / 2;
		int centerY = height / 2 - 8;
		this.hoveredNode = findNodeAt(mouseX, mouseY, centerX, centerY);

		drawTopStatsChips(context, snapshot);
		drawBranches(context, centerX, centerY);
		drawNodes(context, snapshot, centerX, centerY);
		if (respecPopupVisible) {
			layoutPopup();
			drawPopup(context, snapshot);
		}

		renderWidgets(context, mouseX, mouseY, delta);
		drawNodeTooltip(context, snapshot, mouseX, mouseY, centerX, centerY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		if (button != 0 && button != 1) {
			return false;
		}

		int centerX = width / 2;
		int centerY = height / 2 - 8;
		NodeVisual clickedNode = findNodeAt((int) mouseX, (int) mouseY, centerX, centerY);
		if (clickedNode != null) {
			if (clickedNode.nodeId() == FishingSkillNode.UNLOCK_FISHER_CRAFTING.id()) {
				FishingSkillTreeState.Snapshot snapshot = FishingSkillTreeState.snapshot();
				boolean unlocked = snapshot.isUnlocked(FishingSkillNode.UNLOCK_FISHER_CRAFTING);
				if (button == 0) {
					if (!unlocked && snapshot.availableSkillPoints() >= 1) {
						sendSkillAction(FishkaSkillTreeActionC2SPayload.Action.UNLOCK_NODE, FishingSkillNode.UNLOCK_FISHER_CRAFTING.id());
					}
					return true;
				}
				if (button == 1) {
					if (unlocked) {
						openPopup(centerX, centerY);
					} else {
						closePopup();
					}
					return true;
				}
			}
			closePopup();
			return true;
		}

		if (respecPopupVisible) {
			if (isInsidePopup(mouseX, mouseY)) {
				return true;
			}
			closePopup();
			return true;
		}

		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_GRAVE_ACCENT) {
			closePopup();
			close();
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.respecPopupVisible) {
			closePopup();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private void drawBackgroundLayer(DrawContext context) {
		context.fillGradient(0, 0, width, height, 0xFF162131, 0xFF111926);
		context.fill(0, 0, width, height, 0x1A000000);
	}

	private void renderWidgets(DrawContext context, int mouseX, int mouseY, float delta) {
		if (popupConfirmButton != null && popupConfirmButton.visible) {
			popupConfirmButton.render(context, mouseX, mouseY, delta);
		}
		if (popupCancelButton != null && popupCancelButton.visible) {
			popupCancelButton.render(context, mouseX, mouseY, delta);
		}
	}

	private void drawTopStatsChips(DrawContext context, FishingSkillTreeState.Snapshot snapshot) {
		List<TopChip> chips = List.of(
			new TopChip(Text.translatable("fishka.skill_tree.chip.level", snapshot.level()), 0xC3152230, 0xFF7D9BBC, 0xFFEEDAA4),
			new TopChip(Text.translatable("fishka.skill_tree.chip.points", snapshot.availableSkillPoints()), 0xC3152230, 0xFF5B8EB5, 0xFF78D5FF),
			new TopChip(Text.translatable("fishka.skill_tree.chip.money", snapshot.money()), 0xC3152230, 0xFFB89A59, 0xFFFFD873)
		);

		int x = 6;
		int y = 6;
		int maxRight = width - 6;
		for (TopChip chip : chips) {
			int chipWidth = this.textRenderer.getWidth(chip.text()) + 16;
			if (x + chipWidth > maxRight) {
				x = 6;
				y += CHIP_HEIGHT + CHIP_GAP;
			}
			context.fill(x, y, x + chipWidth, y + CHIP_HEIGHT, chip.fillColor());
			context.drawBorder(x, y, chipWidth, CHIP_HEIGHT, chip.borderColor());
			context.drawText(this.textRenderer, chip.text(), x + 8, y + 4, chip.textColor(), false);
			x += chipWidth + CHIP_GAP;
		}
	}

	private void drawBranches(DrawContext context, int centerX, int centerY) {
		for (NodeVisual node : BRANCH_NODES) {
			int toX = centerX + node.offsetX();
			int toY = centerY + node.offsetY();
			// Slightly thicker than the previous stable version.
			drawSoftLine(context, centerX, centerY, toX, toY, 0x485B89B6, 3.6f);
			drawSoftLine(context, centerX, centerY, toX, toY, 0xFF8AB9EA, 2.0f);
		}
	}

	private void drawNodes(DrawContext context, FishingSkillTreeState.Snapshot snapshot, int centerX, int centerY) {
		boolean centerUnlocked = snapshot.isUnlocked(FishingSkillNode.UNLOCK_FISHER_CRAFTING);
		drawSingleNode(context, CENTER_NODE, centerX, centerY, hoveredNode == CENTER_NODE, centerUnlocked);

		for (NodeVisual node : BRANCH_NODES) {
			int nodeX = centerX + node.offsetX();
			int nodeY = centerY + node.offsetY();
			drawSingleNode(context, node, nodeX, nodeY, hoveredNode == node, false);
		}
	}

	private void drawSingleNode(DrawContext context, NodeVisual node, int centerX, int centerY, boolean hovered, boolean unlockedCenterNode) {
		int radius = node.diameter() / 2;
		boolean isCenter = node.nodeId() == FishingSkillNode.UNLOCK_FISHER_CRAFTING.id();
		int fillColor = isCenter
			? (unlockedCenterNode ? 0xFF1E6140 : 0xFF243140)
			: 0xFF1F2C3C;
		int ringColor = isCenter
			? (unlockedCenterNode ? 0xFF74D89A : 0xFF89A8C8)
			: 0xFF6D87A6;
		if (hovered) {
			fillColor = isCenter
				? (unlockedCenterNode ? 0xFF25734C : 0xFF2D3C4F)
				: 0xFF27384B;
			ringColor = 0xFFE5C47B;
		}

		drawSoftCircleFill(context, centerX, centerY, radius, fillColor);
		drawSoftCircleRing(context, centerX, centerY, radius - 0.3f, 2.0f, ringColor);
		if (hovered) {
			drawSoftCircleRing(context, centerX, centerY, radius + 2.0f, 1.6f, 0x66FFE0A8);
		}
		context.drawItem(node.icon(), centerX - 8, centerY - 8);

		Text label = ellipsize(node.label(), 94);
		context.drawCenteredTextWithShadow(this.textRenderer, label, centerX, centerY + radius + 7, 0xFFD4DFED);
	}

	private void drawPopup(DrawContext context, FishingSkillTreeState.Snapshot snapshot) {
		context.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, 0xF6152232);
		context.drawBorder(popupX, popupY, POPUP_WIDTH, POPUP_HEIGHT, 0xFFB79B60);
		context.fill(popupX + 1, popupY + 1, popupX + POPUP_WIDTH - 1, popupY + 2, 0x24FFFFFF);

		context.drawText(this.textRenderer, Text.translatable("fishka.skill_tree.popup.title"), popupX + 10, popupY + 10, 0xFFF2DDAD, false);
		List<OrderedText> wrapped = this.textRenderer.wrapLines(Text.translatable("fishka.skill_tree.popup.desc"), POPUP_WIDTH - 20);
		int y = popupY + 24;
		for (int i = 0; i < wrapped.size() && i < 2; i++) {
			context.drawText(this.textRenderer, wrapped.get(i), popupX + 10, y, 0xFFBFCBDD, false);
			y += 10;
		}
		context.drawText(
			this.textRenderer,
			Text.translatable("fishka.skill_tree.popup.cost", FishingConfig.get().skillTree.respecCostMoney),
			popupX + 10,
			popupY + 46,
			snapshot.money() >= FishingConfig.get().skillTree.respecCostMoney ? 0xFFFFD56E : 0xFFD98989,
			false
		);
	}

	private void drawNodeTooltip(
		DrawContext context,
		FishingSkillTreeState.Snapshot snapshot,
		int mouseX,
		int mouseY,
		int centerX,
		int centerY
	) {
		if (hoveredNode == null) {
			return;
		}

		List<TooltipLine> lines = new ArrayList<>();
		lines.add(new TooltipLine(hoveredNode.label(), 0xFFF0DFB5));
		if (hoveredNode.nodeId() == FishingSkillNode.UNLOCK_FISHER_CRAFTING.id()) {
			boolean unlocked = snapshot.isUnlocked(FishingSkillNode.UNLOCK_FISHER_CRAFTING);
			lines.add(new TooltipLine(
				unlocked ? Text.translatable("fishka.skill_tree.node.unlocked") : Text.translatable("fishka.skill_tree.node.locked"),
				unlocked ? 0xFF7CE7A5 : 0xFFD5DEEA
			));
			if (!unlocked) {
				lines.add(new TooltipLine(Text.translatable("fishka.skill_tree.node.cost", 1), 0xFFFFD56E));
				lines.add(new TooltipLine(
					snapshot.availableSkillPoints() >= 1
						? Text.translatable("fishka.skill_tree.tooltip.left_click_unlock")
						: Text.translatable("fishka.skill_tree.tooltip.no_points"),
					snapshot.availableSkillPoints() >= 1 ? 0xFF7DCBFF : 0xFFFF8C8C
				));
			} else {
				lines.add(new TooltipLine(Text.translatable("fishka.skill_tree.tooltip.right_click_respec"), 0xFF7DCBFF));
			}
		} else {
			lines.add(new TooltipLine(Text.translatable("fishka.skill_tree.soon"), 0xFFB8C4D3));
		}
		lines.add(new TooltipLine(hoveredNode.description(), 0xFFB8C4D3));
		drawTooltipPanel(context, lines, mouseX + 14, mouseY + 12, centerX, centerY);
	}

	private void drawTooltipPanel(DrawContext context, List<TooltipLine> lines, int preferredX, int preferredY, int centerX, int centerY) {
		List<WrappedTooltipLine> wrapped = new ArrayList<>();
		int maxWidth = 0;
		for (TooltipLine line : lines) {
			List<OrderedText> wrappedLine = this.textRenderer.wrapLines(line.text(), TOOLTIP_MAX_WIDTH - 14);
			for (OrderedText orderedText : wrappedLine) {
				wrapped.add(new WrappedTooltipLine(orderedText, line.color()));
				maxWidth = Math.max(maxWidth, this.textRenderer.getWidth(orderedText));
			}
		}

		int panelWidth = maxWidth + 14;
		int panelHeight = wrapped.size() * 10 + 8;
		int nodeCenterX = centerX + hoveredNode.offsetX();
		int nodeCenterY = centerY + hoveredNode.offsetY();
		int radius = hoveredNode.diameter() / 2;

		int drawX = MathHelper.clamp(preferredX, 8, this.width - panelWidth - 8);
		int drawY = MathHelper.clamp(preferredY, 8, this.height - panelHeight - 8);

		Text nodeLabel = ellipsize(hoveredNode.label(), 94);
		int labelWidth = this.textRenderer.getWidth(nodeLabel);
		int labelLeft = nodeCenterX - labelWidth / 2 - 2;
		int labelRight = nodeCenterX + labelWidth / 2 + 2;
		int labelTop = nodeCenterY + radius + 5;
		int labelBottom = labelTop + 10;
		if (rectanglesIntersect(drawX, drawY, panelWidth, panelHeight, labelLeft, labelTop, labelRight - labelLeft, labelBottom - labelTop)) {
			int aboveY = MathHelper.clamp(labelTop - panelHeight - 8, 8, this.height - panelHeight - 8);
			if (aboveY + panelHeight < labelTop) {
				drawY = aboveY;
			} else {
				drawY = MathHelper.clamp(labelBottom + 8, 8, this.height - panelHeight - 8);
			}
		}

		context.fill(drawX + 1, drawY + 1, drawX + panelWidth + 1, drawY + panelHeight + 1, 0x64000000);
		context.fill(drawX, drawY, drawX + panelWidth, drawY + panelHeight, 0xFF111B28);
		context.drawBorder(drawX, drawY, panelWidth, panelHeight, 0xFF6A86A8);
		context.fill(drawX + 1, drawY + 1, drawX + panelWidth - 1, drawY + 2, 0x22FFFFFF);
		if (!wrapped.isEmpty()) {
			context.fill(drawX + 6, drawY + 14, drawX + panelWidth - 6, drawY + 15, 0x335A7DA1);
		}
		int lineY = drawY + 5;
		for (int i = 0; i < wrapped.size(); i++) {
			context.drawText(this.textRenderer, wrapped.get(i).text(), drawX + 7, lineY, wrapped.get(i).color(), false);
			lineY += 10;
		}
	}

	private static boolean rectanglesIntersect(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
		return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
	}

	private void drawSoftCircleFill(DrawContext context, float centerX, float centerY, float radius, int color) {
		float outer = radius + 1.0f;
		int minX = MathHelper.floor(centerX - outer);
		int maxX = MathHelper.ceil(centerX + outer);
		int minY = MathHelper.floor(centerY - outer);
		int maxY = MathHelper.ceil(centerY + outer);

		for (int py = minY; py <= maxY; py++) {
			for (int px = minX; px <= maxX; px++) {
				float dx = (px + 0.5f) - centerX;
				float dy = (py + 0.5f) - centerY;
				float distance = (float) Math.sqrt(dx * dx + dy * dy);
				float alpha = 1.0f - clamp01(distance - radius);
				if (alpha <= 0.0f) {
					continue;
				}
				context.fill(px, py, px + 1, py + 1, withScaledAlpha(color, alpha));
			}
		}
	}

	private void drawSoftCircleRing(DrawContext context, float centerX, float centerY, float radius, float thickness, int color) {
		float halfThickness = Math.max(0.1f, thickness * 0.5f);
		float outer = radius + halfThickness + 1.0f;
		int minX = MathHelper.floor(centerX - outer);
		int maxX = MathHelper.ceil(centerX + outer);
		int minY = MathHelper.floor(centerY - outer);
		int maxY = MathHelper.ceil(centerY + outer);

		for (int py = minY; py <= maxY; py++) {
			for (int px = minX; px <= maxX; px++) {
				float dx = (px + 0.5f) - centerX;
				float dy = (py + 0.5f) - centerY;
				float distance = (float) Math.sqrt(dx * dx + dy * dy);
				float delta = Math.abs(distance - radius);
				float alpha = 1.0f - clamp01(delta - halfThickness);
				if (alpha <= 0.0f) {
					continue;
				}
				context.fill(px, py, px + 1, py + 1, withScaledAlpha(color, alpha));
			}
		}
	}

	private void drawSoftLine(DrawContext context, float x1, float y1, float x2, float y2, int color, float thickness) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float length = (float) Math.sqrt(dx * dx + dy * dy);
		if (length <= 0.001f) {
			drawSoftCircleFill(context, x1, y1, thickness * 0.5f, color);
			return;
		}
		float step = 0.7f;
		for (float t = 0.0f; t <= length; t += step) {
			float ratio = t / length;
			float px = x1 + dx * ratio;
			float py = y1 + dy * ratio;
			drawSoftCircleFill(context, px, py, thickness * 0.5f, color);
		}
		drawSoftCircleFill(context, x2, y2, thickness * 0.5f, color);
	}


	private NodeVisual findNodeAt(int mouseX, int mouseY, int centerX, int centerY) {
		if (isInsideNode(mouseX, mouseY, centerX + CENTER_NODE.offsetX(), centerY + CENTER_NODE.offsetY(), CENTER_NODE.diameter())) {
			return CENTER_NODE;
		}
		for (NodeVisual node : BRANCH_NODES) {
			if (isInsideNode(mouseX, mouseY, centerX + node.offsetX(), centerY + node.offsetY(), node.diameter())) {
				return node;
			}
		}
		return null;
	}

	private static boolean isInsideNode(double mouseX, double mouseY, int centerX, int centerY, int diameter) {
		int radius = diameter / 2;
		double dx = mouseX - centerX;
		double dy = mouseY - centerY;
		return dx * dx + dy * dy <= radius * radius;
	}

	private void openPopup(int centerX, int centerY) {
		this.respecPopupVisible = true;
		this.popupAnchorX = centerX;
		this.popupAnchorY = centerY;
		layoutPopup();
	}

	private void layoutPopup() {
		this.popupX = MathHelper.clamp(popupAnchorX + 38, 8, this.width - POPUP_WIDTH - 8);
		this.popupY = MathHelper.clamp(popupAnchorY - POPUP_HEIGHT / 2, 8, this.height - POPUP_HEIGHT - 8);
		this.popupConfirmButton.visible = true;
		this.popupCancelButton.visible = true;
		this.popupConfirmButton.setPosition(popupX + 10, popupY + POPUP_HEIGHT - 28);
		this.popupCancelButton.setPosition(popupX + POPUP_WIDTH - 98, popupY + POPUP_HEIGHT - 28);
	}

	private void closePopup() {
		this.respecPopupVisible = false;
		if (this.popupConfirmButton != null) {
			this.popupConfirmButton.visible = false;
		}
		if (this.popupCancelButton != null) {
			this.popupCancelButton.visible = false;
		}
	}

	private boolean isInsidePopup(double mouseX, double mouseY) {
		return mouseX >= popupX && mouseX <= popupX + POPUP_WIDTH && mouseY >= popupY && mouseY <= popupY + POPUP_HEIGHT;
	}

	private Text ellipsize(Text text, int maxWidth) {
		String value = text.getString();
		if (this.textRenderer.getWidth(value) <= maxWidth) {
			return text;
		}
		String suffix = "...";
		int cut = value.length();
		while (cut > 0 && this.textRenderer.getWidth(value.substring(0, cut) + suffix) > maxWidth) {
			cut--;
		}
		return Text.literal(value.substring(0, cut) + suffix);
	}

	private static int withScaledAlpha(int color, float alphaScale) {
		int baseAlpha = (color >>> 24) & 0xFF;
		int scaled = MathHelper.clamp(Math.round(baseAlpha * clamp01(alphaScale)), 0, 255);
		return (scaled << 24) | (color & 0x00FFFFFF);
	}

	private static float clamp01(float value) {
		return Math.max(0.0f, Math.min(1.0f, value));
	}

	private static void sendSkillAction(FishkaSkillTreeActionC2SPayload.Action action, int nodeId) {
		if (ClientPlayNetworking.canSend(FishkaSkillTreeActionC2SPayload.ID)) {
			ClientPlayNetworking.send(new FishkaSkillTreeActionC2SPayload(action, nodeId));
		}
	}

	private record NodeVisual(
		int nodeId,
		int offsetX,
		int offsetY,
		int diameter,
		ItemStack icon,
		Text label,
		Text description
	) {
	}

	private record TopChip(Text text, int fillColor, int borderColor, int textColor) {
	}

	private record TooltipLine(Text text, int color) {
	}

	private record WrappedTooltipLine(OrderedText text, int color) {
	}
}

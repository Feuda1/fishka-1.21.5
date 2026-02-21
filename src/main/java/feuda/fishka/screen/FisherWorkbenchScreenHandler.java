package feuda.fishka.screen;

import feuda.fishka.recipe.FisherRecipeRegistry;
import feuda.fishka.registry.ModBlocks;
import feuda.fishka.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;

public class FisherWorkbenchScreenHandler extends ScreenHandler {
	private static final int PROPERTY_UNLOCKED = 0;

	private final Inventory input;
	private final Inventory result;
	private final PropertyDelegate properties;
	private final ScreenHandlerContext context;

	public FisherWorkbenchScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new SimpleInventory(9), new SimpleInventory(1), new ArrayPropertyDelegate(1), ScreenHandlerContext.EMPTY);
	}

	private FisherWorkbenchScreenHandler(
		int syncId,
		PlayerInventory playerInventory,
		Inventory input,
		Inventory result,
		PropertyDelegate properties,
		ScreenHandlerContext context
	) {
		super(ModScreenHandlers.FISHER_WORKBENCH, syncId);
		checkSize(input, 9);
		checkSize(result, 1);
		checkDataCount(properties, 1);
		this.input = input;
		this.result = result;
		this.properties = properties;
		this.context = context;

		this.addSlot(new Slot(result, 0, 124, 35) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return false;
			}

			@Override
			public boolean canTakeItems(PlayerEntity playerEntity) {
				return isUnlocked() && super.canTakeItems(playerEntity);
			}
		});

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int index = col + row * 3;
				this.addSlot(new Slot(input, index, 30 + col * 18, 17 + row * 18) {
					@Override
					public boolean canInsert(ItemStack stack) {
						return isUnlocked() && super.canInsert(stack);
					}
				});
			}
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}

		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
		}

		this.addProperties(properties);
		updateResult();
	}

	public static FisherWorkbenchScreenHandler create(
		int syncId,
		PlayerInventory playerInventory,
		ScreenHandlerContext context,
		boolean unlocked
	) {
		SimpleInventory input = new SimpleInventory(9) {
			@Override
			public void markDirty() {
				super.markDirty();
			}
		};
		SimpleInventory result = new SimpleInventory(1);
		ArrayPropertyDelegate properties = new ArrayPropertyDelegate(1);
		properties.set(PROPERTY_UNLOCKED, unlocked ? 1 : 0);
		return new FisherWorkbenchScreenHandler(syncId, playerInventory, input, result, properties, context);
	}

	public boolean isUnlocked() {
		return properties.get(PROPERTY_UNLOCKED) > 0;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slotIndex) {
		ItemStack moved = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);
		if (slot == null || !slot.hasStack()) {
			return ItemStack.EMPTY;
		}

		ItemStack source = slot.getStack();
		moved = source.copy();

		if (slotIndex == 0) {
			if (!this.insertItem(source, 10, 46, true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickTransfer(source, moved);
		} else if (slotIndex >= 1 && slotIndex <= 9) {
			if (!this.insertItem(source, 10, 46, false)) {
				return ItemStack.EMPTY;
			}
		} else if (isUnlocked() && slotIndex >= 10 && slotIndex < 46) {
			if (!this.insertItem(source, 1, 10, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			return ItemStack.EMPTY;
		}

		if (source.isEmpty()) {
			slot.setStack(ItemStack.EMPTY);
		} else {
			slot.markDirty();
		}

		updateResult();
		return moved;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(context, player, ModBlocks.FISHER_WORKBENCH);
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		super.onContentChanged(inventory);
		updateResult();
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		this.context.run((world, pos) -> this.dropInventory(player, input));
	}

	private void updateResult() {
		if (!isUnlocked()) {
			result.setStack(0, ItemStack.EMPTY);
			return;
		}
		ItemStack stack = FisherRecipeRegistry.match(input);
		result.setStack(0, stack.copy());
	}
}

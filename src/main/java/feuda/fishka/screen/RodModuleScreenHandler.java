package feuda.fishka.screen;

import feuda.fishka.fishing.RodModuleSlot;
import feuda.fishka.fishing.RodModuleState;
import feuda.fishka.item.RodModuleItem;
import feuda.fishka.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public final class RodModuleScreenHandler extends ScreenHandler {
	private static final int MODULE_SLOT_COUNT = 6;
	private static final int PROPERTY_HOST_SLOT = 0;

	private final Inventory moduleInventory;
	private final PlayerInventory playerInventory;
	private final PropertyDelegate properties;
	private boolean loadingFromRod;

	public RodModuleScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new SimpleInventory(MODULE_SLOT_COUNT), new ArrayPropertyDelegate(1));
		this.properties.set(PROPERTY_HOST_SLOT, -1);
	}

	private RodModuleScreenHandler(int syncId, PlayerInventory playerInventory, Inventory moduleInventory, PropertyDelegate properties) {
		super(ModScreenHandlers.ROD_MODULE, syncId);
		checkSize(moduleInventory, MODULE_SLOT_COUNT);
		checkDataCount(properties, 1);
		this.moduleInventory = moduleInventory;
		this.playerInventory = playerInventory;
		this.properties = properties;

		addModuleSlots();
		addPlayerInventorySlots();
		addProperties(properties);

		if (!playerInventory.player.getWorld().isClient) {
			if (!isValidHostRod()) {
				if (playerInventory.player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
					serverPlayer.closeHandledScreen();
				}
				return;
			}
			loadFromRod();
		}
	}

	public static RodModuleScreenHandler create(int syncId, PlayerInventory playerInventory, int hostSlotIndex) {
		ArrayPropertyDelegate properties = new ArrayPropertyDelegate(1);
		properties.set(PROPERTY_HOST_SLOT, hostSlotIndex);
		return new RodModuleScreenHandler(syncId, playerInventory, new SimpleInventory(MODULE_SLOT_COUNT), properties);
	}

	public int getHostSlotIndex() {
		return properties.get(PROPERTY_HOST_SLOT);
	}

	public ItemStack getHostRodStack() {
		int hostSlot = getHostSlotIndex();
		if (hostSlot < 0 || hostSlot >= playerInventory.size()) {
			return ItemStack.EMPTY;
		}
		return playerInventory.getStack(hostSlot);
	}

	public ItemStack getModuleStack(RodModuleSlot slot) {
		return moduleInventory.getStack(slot.index());
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		if (player.getWorld().isClient) {
			return true;
		}
		return isValidHostRod();
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
		int playerStart = MODULE_SLOT_COUNT;
		int playerEnd = playerStart + 36;

		if (slotIndex < MODULE_SLOT_COUNT) {
			if (!this.insertItem(source, playerStart, playerEnd, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			if (slot.inventory == playerInventory && slot.getIndex() == getHostSlotIndex()) {
				return ItemStack.EMPTY;
			}
			if (!(source.getItem() instanceof RodModuleItem moduleItem)) {
				return ItemStack.EMPTY;
			}
			int targetSlot = moduleItem.slot().index();
			if (!this.insertItem(source, targetSlot, targetSlot + 1, false)) {
				return ItemStack.EMPTY;
			}
		}

		if (source.isEmpty()) {
			slot.setStack(ItemStack.EMPTY);
		} else {
			slot.markDirty();
		}
		return moved;
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		super.onContentChanged(inventory);
		if (inventory == moduleInventory) {
			saveToRod();
		}
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		if (player.getWorld().isClient) {
			return;
		}
		if (RodModuleState.supports(getHostRodStack())) {
			saveToRod();
			return;
		}
		for (int i = 0; i < MODULE_SLOT_COUNT; i++) {
			ItemStack stack = moduleInventory.getStack(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (!player.getInventory().insertStack(stack.copy())) {
				player.dropItem(stack.copy(), false);
			}
			moduleInventory.setStack(i, ItemStack.EMPTY);
		}
	}

	public static int moduleSlotX(RodModuleSlot slot) {
		return switch (slot) {
			case HANDLE -> 55;
			case REEL -> 159;
			case ROD -> 107;
			case LINE -> 79;
			case BOBBER -> 107;
			case HOOK -> 135;
		};
	}

	public static int moduleSlotY(RodModuleSlot slot) {
		return switch (slot) {
			case HANDLE -> 68;
			case REEL -> 68;
			case ROD -> 46;
			case LINE -> 102;
			case BOBBER -> 118;
			case HOOK -> 102;
		};
	}

	private void addModuleSlots() {
		for (RodModuleSlot slotType : RodModuleSlot.values()) {
			final RodModuleSlot required = slotType;
			this.addSlot(new Slot(moduleInventory, required.index(), moduleSlotX(required), moduleSlotY(required)) {
				@Override
				public boolean canInsert(ItemStack stack) {
					return stack.getItem() instanceof RodModuleItem moduleItem && moduleItem.slot() == required;
				}

				@Override
				public int getMaxItemCount() {
					return 1;
				}
			});
		}
	}

	private void addPlayerInventorySlots() {
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				int invIndex = col + row * 9 + 9;
				this.addSlot(createPlayerSlot(invIndex, 25 + col * 18, 170 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			int invIndex = col;
			this.addSlot(createPlayerSlot(invIndex, 25 + col * 18, 228));
		}
	}

	private Slot createPlayerSlot(int invIndex, int x, int y) {
		return new Slot(playerInventory, invIndex, x, y) {
			@Override
			public boolean canTakeItems(PlayerEntity playerEntity) {
				return invIndex != getHostSlotIndex() && super.canTakeItems(playerEntity);
			}

			@Override
			public boolean canInsert(ItemStack stack) {
				return invIndex != getHostSlotIndex() && super.canInsert(stack);
			}
		};
	}

	private void loadFromRod() {
		ItemStack rodStack = getHostRodStack();
		if (!RodModuleState.supports(rodStack)) {
			return;
		}
		loadingFromRod = true;
		for (RodModuleSlot slot : RodModuleSlot.values()) {
			moduleInventory.setStack(slot.index(), RodModuleState.getInstalledModuleStack(rodStack, slot));
		}
		loadingFromRod = false;
	}

	private void saveToRod() {
		if (loadingFromRod || playerInventory.player.getWorld().isClient) {
			return;
		}
		ItemStack rodStack = getHostRodStack();
		if (!RodModuleState.supports(rodStack)) {
			return;
		}
		for (RodModuleSlot slot : RodModuleSlot.values()) {
			ItemStack moduleStack = moduleInventory.getStack(slot.index());
			RodModuleState.setModule(rodStack, slot, moduleStack);
		}
		playerInventory.markDirty();
	}

	private boolean isValidHostRod() {
		int hostSlot = getHostSlotIndex();
		return hostSlot >= 0 && hostSlot < playerInventory.size() && RodModuleState.supports(playerInventory.getStack(hostSlot));
	}
}

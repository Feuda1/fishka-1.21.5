package feuda.fishka.fishing;

import feuda.fishka.registry.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class RodModuleState {
	private static final String ROOT_KEY = "fishka_rod_modules";

	private RodModuleState() {
	}

	public static boolean supports(ItemStack rodStack) {
		return !rodStack.isEmpty() && rodStack.getItem() == ModItems.APPRENTICE_FISHING_ROD;
	}

	public static ItemStack getInstalledModuleStack(ItemStack rodStack, RodModuleSlot slot) {
		Identifier id = getInstalledModuleId(rodStack, slot);
		if (id == null) {
			return ItemStack.EMPTY;
		}
		Item item = Registries.ITEM.get(id);
		if (!(item instanceof feuda.fishka.item.RodModuleItem moduleItem) || moduleItem.slot() != slot) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(item);
	}

	public static Identifier getInstalledModuleId(ItemStack rodStack, RodModuleSlot slot) {
		if (!supports(rodStack) || slot == null) {
			return null;
		}
		NbtCompound modules = readModulesNbt(rodStack);
		String rawId = modules.getString(slot.key(), "");
		if (rawId.isBlank()) {
			return null;
		}
		return Identifier.tryParse(rawId);
	}

	public static void setModule(ItemStack rodStack, RodModuleSlot slot, ItemStack moduleStack) {
		if (!supports(rodStack) || slot == null) {
			return;
		}
		NbtCompound modules = readModulesNbt(rodStack);
		if (
			moduleStack.isEmpty()
				|| !(moduleStack.getItem() instanceof feuda.fishka.item.RodModuleItem moduleItem)
				|| moduleItem.slot() != slot
		) {
			modules.remove(slot.key());
		} else {
			Identifier id = Registries.ITEM.getId(moduleStack.getItem());
			if (id == null) {
				modules.remove(slot.key());
			} else {
				modules.putString(slot.key(), id.toString());
			}
		}
		writeModulesNbt(rodStack, modules);
	}

	public static void clearAll(ItemStack rodStack) {
		if (!supports(rodStack)) {
			return;
		}
		writeModulesNbt(rodStack, new NbtCompound());
	}

	private static NbtCompound readModulesNbt(ItemStack rodStack) {
		NbtComponent customData = rodStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
		NbtCompound root = customData.copyNbt();
		return root.getCompoundOrEmpty(ROOT_KEY).copy();
	}

	private static void writeModulesNbt(ItemStack rodStack, NbtCompound modulesNbt) {
		NbtComponent customData = rodStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
		NbtCompound root = customData.copyNbt();
		if (modulesNbt.isEmpty()) {
			root.remove(ROOT_KEY);
		} else {
			root.put(ROOT_KEY, modulesNbt.copy());
		}
		if (root.isEmpty()) {
			rodStack.remove(DataComponentTypes.CUSTOM_DATA);
		} else {
			rodStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(root));
		}
	}
}

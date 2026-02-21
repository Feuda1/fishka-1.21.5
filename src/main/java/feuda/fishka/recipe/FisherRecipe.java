package feuda.fishka.recipe;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public interface FisherRecipe {
	boolean matches(Inventory inventory);

	ItemStack craft(Inventory inventory);
}

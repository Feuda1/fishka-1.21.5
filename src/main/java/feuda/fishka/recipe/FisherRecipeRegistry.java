package feuda.fishka.recipe;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class FisherRecipeRegistry {
	private static final List<FisherRecipe> RECIPES = new CopyOnWriteArrayList<>();

	private FisherRecipeRegistry() {
	}

	public static void register(FisherRecipe recipe) {
		if (recipe != null) {
			RECIPES.add(recipe);
		}
	}

	public static ItemStack match(Inventory inventory) {
		for (FisherRecipe recipe : RECIPES) {
			if (recipe.matches(inventory)) {
				return recipe.craft(inventory);
			}
		}
		return ItemStack.EMPTY;
	}
}

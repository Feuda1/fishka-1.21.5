package feuda.fishka.registry;

import feuda.fishka.Fishka;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroups {
	public static final ItemGroup RODS_AND_UPGRADES = Registry.register(
		Registries.ITEM_GROUP,
		Fishka.id("rods_upgrades"),
		FabricItemGroup.builder()
			.displayName(Text.translatable("itemGroup.fishka.rods_upgrades"))
			.icon(() -> new ItemStack(ModItems.APPRENTICE_FISHING_ROD))
			.entries((context, entries) -> {
				entries.add(ModItems.APPRENTICE_FISHING_ROD);
				for (net.minecraft.item.Item module : ModItems.allRodModuleItems()) {
					entries.add(module);
				}
			})
			.build()
	);

	public static final ItemGroup FISH = Registry.register(
		Registries.ITEM_GROUP,
		Fishka.id("fish"),
		FabricItemGroup.builder()
			.displayName(Text.translatable("itemGroup.fishka.fish"))
			.icon(() -> new ItemStack(Items.COD))
			.entries((context, entries) -> {
				entries.add(ModItems.RAW_FISHKA);
			})
			.build()
	);

	public static final ItemGroup TOOLS = Registry.register(
		Registries.ITEM_GROUP,
		Fishka.id("tools"),
		FabricItemGroup.builder()
			.displayName(Text.translatable("itemGroup.fishka.tools"))
			.icon(() -> new ItemStack(ModItems.DEBUG_FISHING_TOOL))
			.entries((context, entries) -> {
				entries.add(ModItems.APPRENTICE_FISHING_ROD);
				entries.add(ModItems.DEBUG_FISHING_TOOL);
			})
			.build()
	);

	public static final ItemGroup BLOCKS = Registry.register(
		Registries.ITEM_GROUP,
		Fishka.id("blocks"),
		FabricItemGroup.builder()
			.displayName(Text.translatable("itemGroup.fishka.blocks"))
			.icon(() -> new ItemStack(ModBlocks.FISHKA_BLOCK_ITEM))
			.entries((context, entries) -> {
				entries.add(ModBlocks.FISHKA_BLOCK_ITEM);
				entries.add(ModBlocks.FISHER_WORKBENCH_ITEM);
			})
			.build()
	);

	private ModItemGroups() {
	}

	public static void initialize() {
	}
}

package feuda.fishka.registry;

import feuda.fishka.Fishka;
import feuda.fishka.block.FisherWorkbenchBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public final class ModBlocks {
	public static final Block FISHKA_BLOCK = registerBlock("fishka_block");
	public static final Item FISHKA_BLOCK_ITEM = registerBlockItem("fishka_block", FISHKA_BLOCK);
	public static final Block FISHER_WORKBENCH = registerBlock(
		"fisher_workbench",
		settings -> new FisherWorkbenchBlock(settings.strength(2.5f))
	);
	public static final Item FISHER_WORKBENCH_ITEM = registerBlockItem("fisher_workbench", FISHER_WORKBENCH);

	private ModBlocks() {
	}

	private static Block registerBlock(String path) {
		return registerBlock(path, Block::new);
	}

	private static Block registerBlock(String path, Function<AbstractBlock.Settings, Block> factory) {
		Identifier id = Fishka.id(path);
		RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, id);
		Block block = factory.apply(AbstractBlock.Settings.create().registryKey(key).strength(1.5f));
		return Registry.register(Registries.BLOCK, id, block);
	}

	private static Item registerBlockItem(String path, Block block) {
		Identifier id = Fishka.id(path);
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
		return Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings().registryKey(key)));
	}

	public static void initialize() {
		Fishka.LOGGER.info("Registered mod blocks");
	}
}

package feuda.fishka.registry;

import feuda.fishka.Fishka;
import feuda.fishka.fishing.FishEncounterTier;
import feuda.fishka.fishing.RodModuleCatalog;
import feuda.fishka.fishing.RodModuleSlot;
import feuda.fishka.item.CustomFishingRodItem;
import feuda.fishka.item.DebugFishingToolItem;
import feuda.fishka.item.RodModuleItem;
import feuda.fishka.item.TestGeckoFishingRodItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.function.Function;

public final class ModItems {
	public static final Item RAW_FISHKA = register("raw_fishka", key -> new Item(new Item.Settings().registryKey(key)));
	public static final Item APPRENTICE_FISHING_ROD = register(
		"apprentice_fishing_rod",
		key -> new CustomFishingRodItem(new Item.Settings().registryKey(key).maxDamage(256).maxCount(1))
	);
	public static final Item TEST_FISHING_ROD = register(
		"test_fishing_rod",
		key -> createTestFishingRod(key)
	);
	public static final Item DEBUG_FISHING_TOOL = register(
		"debug_fishing_tool",
		key -> new DebugFishingToolItem(new Item.Settings().registryKey(key).maxCount(1))
	);
	private static final EnumMap<RodModuleSlot, EnumMap<FishEncounterTier, Item>> ROD_MODULES = registerRodModules();
	private static final List<Item> ALL_ROD_MODULE_ITEMS = collectRodModules();

	private ModItems() {
	}

	public static Item getRodModuleItem(RodModuleSlot slot, FishEncounterTier tier) {
		EnumMap<FishEncounterTier, Item> byTier = ROD_MODULES.get(slot);
		return byTier != null ? byTier.get(tier) : null;
	}

	public static Collection<Item> allRodModuleItems() {
		return ALL_ROD_MODULE_ITEMS;
	}

	private static Item register(String path, Function<RegistryKey<Item>, Item> factory) {
		Identifier id = Fishka.id(path);
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
		Item item = factory.apply(key);
		return Registry.register(Registries.ITEM, id, item);
	}

	private static EnumMap<RodModuleSlot, EnumMap<FishEncounterTier, Item>> registerRodModules() {
		EnumMap<RodModuleSlot, EnumMap<FishEncounterTier, Item>> result = new EnumMap<>(RodModuleSlot.class);
		for (RodModuleSlot slot : RodModuleSlot.values()) {
			EnumMap<FishEncounterTier, Item> byTier = new EnumMap<>(FishEncounterTier.class);
			for (FishEncounterTier tier : FishEncounterTier.values()) {
				String path = RodModuleCatalog.modulePath(slot, tier);
				Item item = register(
					path,
					key -> new RodModuleItem(slot, tier, new Item.Settings().registryKey(key).maxCount(1))
				);
				byTier.put(tier, item);
			}
			result.put(slot, byTier);
		}
		return result;
	}

	private static List<Item> collectRodModules() {
		List<Item> items = new ArrayList<>();
		for (RodModuleSlot slot : RodModuleSlot.values()) {
			EnumMap<FishEncounterTier, Item> byTier = ROD_MODULES.get(slot);
			if (byTier != null) {
				items.addAll(byTier.values());
			}
		}
		return Collections.unmodifiableList(items);
	}

	private static Item createTestFishingRod(RegistryKey<Item> key) {
		Item.Settings settings = new Item.Settings().registryKey(key).maxDamage(256).maxCount(1);
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			try {
				Class<?> clientClass = Class.forName("feuda.fishka.client.item.TestGeckoFishingRodItemClient");
				Constructor<?> constructor = clientClass.getConstructor(Item.Settings.class);
				return (Item) constructor.newInstance(settings);
			} catch (ReflectiveOperationException exception) {
				Fishka.LOGGER.warn("Не удалось загрузить GeckoLib-клиентскую тестовую удочку, используется базовая версия", exception);
			}
		}
		return new TestGeckoFishingRodItem(settings);
	}

	public static void initialize() {
		Fishka.LOGGER.info("Registered mod items");
	}
}

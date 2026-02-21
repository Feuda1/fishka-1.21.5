package feuda.fishka;

import feuda.fishka.fishing.FishingConfig;
import feuda.fishka.fishing.FishingMiniGameManager;
import feuda.fishka.fishing.FishingMiniGameNet;
import feuda.fishka.fishing.FishingProfileSyncService;
import feuda.fishka.registry.ModBlocks;
import feuda.fishka.registry.ModCommands;
import feuda.fishka.registry.ModItemGroups;
import feuda.fishka.registry.ModItems;
import feuda.fishka.registry.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fishka implements ModInitializer {
	public static final String MOD_ID = "fishka";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		FishingConfig.reload();
		ModItems.initialize();
		ModBlocks.initialize();
		ModItemGroups.initialize();
		ModScreenHandlers.initialize();
		ModCommands.initialize();
		FishingMiniGameNet.initialize();
		FishingProfileSyncService.initialize();
		FishingMiniGameManager.initialize();

		LOGGER.info("Fishka initialized");
	}
}

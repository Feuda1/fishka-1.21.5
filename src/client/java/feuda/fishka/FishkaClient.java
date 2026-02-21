package feuda.fishka;

import feuda.fishka.client.ModClientScreens;
import feuda.fishka.fishing.FishingMiniGameClientController;
import net.fabricmc.api.ClientModInitializer;

public class FishkaClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FishingMiniGameClientController.initialize();
		ModClientScreens.initialize();
		Fishka.LOGGER.info("Fishka client initialized");
	}
}

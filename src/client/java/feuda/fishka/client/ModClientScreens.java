package feuda.fishka.client;

import feuda.fishka.client.gui.FisherWorkbenchScreen;
import feuda.fishka.client.gui.RodModuleScreen;
import feuda.fishka.registry.ModScreenHandlers;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public final class ModClientScreens {
	private ModClientScreens() {
	}

	public static void initialize() {
		HandledScreens.register(ModScreenHandlers.FISHER_WORKBENCH, FisherWorkbenchScreen::new);
		HandledScreens.register(ModScreenHandlers.ROD_MODULE, RodModuleScreen::new);
	}
}

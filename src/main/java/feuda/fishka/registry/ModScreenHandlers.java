package feuda.fishka.registry;

import feuda.fishka.Fishka;
import feuda.fishka.screen.FisherWorkbenchScreenHandler;
import feuda.fishka.screen.RodModuleScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

public final class ModScreenHandlers {
	public static final ScreenHandlerType<FisherWorkbenchScreenHandler> FISHER_WORKBENCH = Registry.register(
		Registries.SCREEN_HANDLER,
		Fishka.id("fisher_workbench"),
		new ScreenHandlerType<>(FisherWorkbenchScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
	);
	public static final ScreenHandlerType<RodModuleScreenHandler> ROD_MODULE = Registry.register(
		Registries.SCREEN_HANDLER,
		Fishka.id("rod_module"),
		new ScreenHandlerType<>(RodModuleScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
	);

	private ModScreenHandlers() {
	}

	public static void initialize() {
		Fishka.LOGGER.info("Registered mod screen handlers");
	}
}

package feuda.fishka.fishing;

import feuda.fishka.client.gui.CatchRewardHudRenderer;
import feuda.fishka.client.gui.CatchRewardHudState;
import feuda.fishka.client.gui.FishingLevelHudRenderer;
import feuda.fishka.client.gui.FishingLevelHudState;
import feuda.fishka.client.gui.FishingMiniGameHudRenderer;
import feuda.fishka.client.gui.FishingSkillTreeScreen;
import feuda.fishka.client.gui.FishingSkillTreeState;
import feuda.fishka.client.gui.MoneyHudRenderer;
import feuda.fishka.client.gui.MoneyHudState;
import feuda.fishka.fishing.net.FishkaOpenRodModulesC2SPayload;
import feuda.fishka.fishing.net.FishkaCatchFeedbackS2CPayload;
import feuda.fishka.fishing.net.FishkaFishingProfileS2CPayload;
import feuda.fishka.fishing.net.FishkaMinigameInputC2SPayload;
import feuda.fishka.fishing.net.FishkaMinigameLifecycleS2CPayload;
import feuda.fishka.fishing.net.FishkaMinigameStateS2CPayload;
import feuda.fishka.fishing.net.FishkaSkillTreeActionC2SPayload;
import feuda.fishka.mixin.client.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

public final class FishingMiniGameClientController {
	private static boolean initialized;
	private static int trackedSessionId = -1;
	private static boolean lastHoldingState;
	private static int suppressUseTicks;
	private static float lineTonePitch = 0.72f;
	private static int lineToneCooldownTicks;
	private static KeyBinding openSkillTreeKey;

	private FishingMiniGameClientController() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;

		ClientPlayNetworking.registerGlobalReceiver(FishkaMinigameStateS2CPayload.ID, (payload, context) ->
			context.client().execute(() -> FishingMiniGameHudState.applyState(payload))
		);
		ClientPlayNetworking.registerGlobalReceiver(FishkaMinigameLifecycleS2CPayload.ID, (payload, context) ->
			context.client().execute(() -> {
				FishingMiniGameHudState.applyLifecycle(payload);
				if (payload.event() != FishkaMinigameLifecycleS2CPayload.Event.START) {
					suppressUseTicks = Math.max(suppressUseTicks, 7);
				}
			})
		);
		ClientPlayNetworking.registerGlobalReceiver(FishkaCatchFeedbackS2CPayload.ID, (payload, context) ->
			context.client().execute(() -> {
				CatchRewardHudState.apply(payload);
				MinecraftClient client = context.client();
				if (client.gameRenderer != null) {
					client.gameRenderer.showFloatingItem(payload.displayStack().copy());
				}
				if (client.player != null) {
					client.player.playSound(SoundEvents.BLOCK_BELL_USE, 0.72f, 1.46f);
				}
			})
		);
		ClientPlayNetworking.registerGlobalReceiver(FishkaFishingProfileS2CPayload.ID, (payload, context) ->
			context.client().execute(() -> {
				FishingLevelHudState.apply(payload);
				FishingSkillTreeState.apply(payload);
				MoneyHudState.apply(payload);
			})
		);

		openSkillTreeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.fishka.open_skill_tree",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_GRAVE_ACCENT,
			"category.fishka.general"
		));

		HudRenderCallback.EVENT.register(FishingLevelHudRenderer::render);
		HudRenderCallback.EVENT.register(MoneyHudRenderer::render);
		HudRenderCallback.EVENT.register(FishingMiniGameHudRenderer::render);
		HudRenderCallback.EVENT.register(CatchRewardHudRenderer::render);
		ClientTickEvents.END_CLIENT_TICK.register(FishingMiniGameClientController::onEndClientTick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			FishingLevelHudState.reset();
			FishingSkillTreeState.reset();
			MoneyHudState.reset();
			resetInputTracking();
			resetMiniGameAudio();
		});
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof CreativeInventoryScreen)) {
				return;
			}
			ScreenMouseEvents.allowMouseClick(screen).register((currentScreen, mouseX, mouseY, button) ->
				allowCreativeRodModuleClick(client, currentScreen, mouseX, mouseY, button)
			);
		});
	}

	private static void onEndClientTick(MinecraftClient client) {
		CatchRewardHudState.tick(client);
		FishingLevelHudState.tick(client);
		MoneyHudState.tick(client);

		if (client.player == null || client.world == null) {
			resetMiniGameAudio();
			resetInputTracking();
			return;
		}

		while (openSkillTreeKey != null && openSkillTreeKey.wasPressed()) {
			if (client.currentScreen instanceof FishingSkillTreeScreen) {
				client.setScreen(null);
			} else if (client.currentScreen == null || client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?>) {
				openSkillTreeScreen(client);
			}
		}

		if (suppressUseTicks > 0) {
			client.options.useKey.setPressed(false);
			while (client.options.useKey.wasPressed()) {
				// Drain queued presses so rod does not recast after duel end.
			}
			suppressUseTicks--;
		}

		FishingMiniGameHudState.Snapshot snapshot = FishingMiniGameHudState.snapshot();
		if (!snapshot.active()) {
			resetMiniGameAudio();
			resetInputTracking();
			return;
		}
		updateMiniGameAudio(client, snapshot);

		if (trackedSessionId != snapshot.sessionId()) {
			trackedSessionId = snapshot.sessionId();
			lastHoldingState = false;
		}

		boolean holding = client.currentScreen == null && client.options.useKey.isPressed();
		if (holding == lastHoldingState) {
			return;
		}

		if (ClientPlayNetworking.canSend(FishkaMinigameInputC2SPayload.ID)) {
			FishkaMinigameInputC2SPayload.Action action = holding
				? FishkaMinigameInputC2SPayload.Action.START_HOLD
				: FishkaMinigameInputC2SPayload.Action.STOP_HOLD;
			ClientPlayNetworking.send(new FishkaMinigameInputC2SPayload(
				action,
				snapshot.sessionId(),
				client.world.getTime()
			));
		}
		lastHoldingState = holding;
	}

	private static void resetInputTracking() {
		trackedSessionId = -1;
		lastHoldingState = false;
		suppressUseTicks = 0;
	}

	private static void updateMiniGameAudio(MinecraftClient client, FishingMiniGameHudState.Snapshot snapshot) {
		if (client.player == null || client.isPaused()) {
			return;
		}

		float safeHalf = Math.max(0.0001f, snapshot.safeZoneHalfWidth01());
		float distance = Math.abs(snapshot.tension01() - snapshot.safeZoneCenter01());
		boolean inZone = distance <= safeHalf;
		float closeness = clamp01(1.0f - (distance / (safeHalf * 1.35f)));

		float targetPitch = inZone ? 1.60f : (0.72f + closeness * 0.28f);
		float smooth = inZone ? 0.22f : 0.08f;
		lineTonePitch += (targetPitch - lineTonePitch) * smooth;
		lineTonePitch = clamp(lineTonePitch, 0.65f, 1.75f);

		lineToneCooldownTicks--;
		if (lineToneCooldownTicks > 0) {
			return;
		}

		float volume = 0.02f + closeness * 0.06f + (inZone ? 0.03f : 0.0f);
		client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value(), volume, lineTonePitch);
		lineToneCooldownTicks = inZone ? 4 : 6;
	}

	private static void resetMiniGameAudio() {
		lineTonePitch = 0.72f;
		lineToneCooldownTicks = 0;
	}

	public static void openSkillTreeScreen(MinecraftClient client) {
		if (client == null || client.player == null || client.world == null) {
			return;
		}
		requestSkillTreeSync();
		client.setScreen(new FishingSkillTreeScreen());
	}

	public static void requestSkillTreeSync() {
		if (ClientPlayNetworking.canSend(FishkaSkillTreeActionC2SPayload.ID)) {
			ClientPlayNetworking.send(new FishkaSkillTreeActionC2SPayload(
				FishkaSkillTreeActionC2SPayload.Action.REQUEST_SYNC,
				-1
			));
		}
	}

	private static float clamp01(float value) {
		return clamp(value, 0.0f, 1.0f);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static boolean allowCreativeRodModuleClick(
		MinecraftClient client,
		net.minecraft.client.gui.screen.Screen screen,
		double mouseX,
		double mouseY,
		int button
	) {
		if (
			button != GLFW.GLFW_MOUSE_BUTTON_RIGHT
				|| client.player == null
				|| !ClientPlayNetworking.canSend(FishkaOpenRodModulesC2SPayload.ID)
				|| !(screen instanceof HandledScreen<?> handledScreen)
				|| !(handledScreen instanceof HandledScreenAccessor accessor)
		) {
			return true;
		}

		Slot hoveredSlot = accessor.fishka$invokeGetSlotAt(mouseX, mouseY);
		if (hoveredSlot == null || !hoveredSlot.hasStack()) {
			return true;
		}
		if (!(hoveredSlot.inventory instanceof net.minecraft.entity.player.PlayerInventory)) {
			return true;
		}
		if (!handledScreen.getScreenHandler().getCursorStack().isEmpty()) {
			return true;
		}
		if (!RodModuleState.supports(hoveredSlot.getStack())) {
			return true;
		}

		int slotIndex = hoveredSlot.getIndex();
		if (slotIndex < 0 || slotIndex >= client.player.getInventory().size()) {
			return true;
		}

		ClientPlayNetworking.send(new FishkaOpenRodModulesC2SPayload(slotIndex));
		return false;
	}
}

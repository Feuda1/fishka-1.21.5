package feuda.fishka.item;

import feuda.fishka.fishing.FishingConfig;
import feuda.fishka.fishing.FishingMiniGameManager;
import feuda.fishka.fishing.FishEncounterTier;
import feuda.fishka.fishing.RodModuleCatalog;
import feuda.fishka.fishing.RodModuleSlot;
import feuda.fishka.fishing.RodModuleState;
import feuda.fishka.screen.RodModuleScreenHandler;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.inventory.StackReference;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class CustomFishingRodItem extends FishingRodItem {
	public CustomFishingRodItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		// Keep client behavior close to vanilla for hand animation and client prediction.
		if (world.isClient) {
			return super.use(world, user, hand);
		}

		if (!(user instanceof ServerPlayerEntity player)) {
			return ActionResult.SUCCESS;
		}

		FishingBobberEntity bobber = player.fishHook;
		if (bobber == null) {
			ActionResult result = super.use(world, user, hand);
			if (result.isAccepted()) {
				FishingMiniGameManager.onCast(player, hand);
			}
			return result;
		}

		// Prevent accidental instant retract from rapid use packets right after cast.
		if (FishingMiniGameManager.ticksSinceCast(player) < 10L) {
			return ActionResult.SUCCESS;
		}

		if (FishingMiniGameManager.hasActiveSession(player)) {
			if (player.isSneaking()) {
				FishingMiniGameManager.cancelSession(
					player,
					feuda.fishka.fishing.net.FishkaMinigameLifecycleS2CPayload.Reason.PLAYER_CANCEL
				);
				return super.use(world, user, hand);
			}
			return ActionResult.SUCCESS;
		}

		if (FishingMiniGameManager.hasWaitingSession(player) || bobber.isTouchingWater()) {
			FishingMiniGameManager.startIfPossible(player, hand, bobber);
		}

		// Optional manual cancel while waiting for fish bite.
		if (player.isSneaking()) {
			FishingMiniGameManager.cancelSession(
				player,
				feuda.fishka.fishing.net.FishkaMinigameLifecycleS2CPayload.Reason.PLAYER_CANCEL
			);
			return super.use(world, user, hand);
		}

		// During waiting/duel states we block vanilla retract and rely on minigame lifecycle.
		return ActionResult.SUCCESS;
	}

	@Override
	public boolean onClicked(
		ItemStack stack,
		ItemStack otherStack,
		Slot slot,
		ClickType clickType,
		PlayerEntity player,
		StackReference cursorStackReference
	) {
		FishingConfig.RodModulesConfig cfg = FishingConfig.get().rodModules;
		if (cfg == null || !cfg.enabled || !cfg.openByInventoryRightClick) {
			return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference);
		}
		if (clickType != ClickType.RIGHT || !otherStack.isEmpty()) {
			return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference);
		}
		if (!RodModuleState.supports(stack)) {
			return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference);
		}
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			// Consume client-side click so vanilla pickup logic does not move the rod to cursor.
			return true;
		}

		int preferredSlot = slot.inventory instanceof PlayerInventory ? slot.getIndex() : -1;
		return tryOpenModuleScreen(serverPlayer, stack, preferredSlot);
	}

	public static boolean tryOpenModuleScreen(ServerPlayerEntity serverPlayer, ItemStack clickedStack, int preferredSlotIndex) {
		FishingConfig.RodModulesConfig cfg = FishingConfig.get().rodModules;
		if (cfg == null || !cfg.enabled || !cfg.openByInventoryRightClick || !RodModuleState.supports(clickedStack)) {
			return false;
		}

		PlayerInventory inventory = serverPlayer.getInventory();
		int hostSlotIndex = -1;

		if (preferredSlotIndex >= 0 && preferredSlotIndex < inventory.size()) {
			ItemStack candidateStack = inventory.getStack(preferredSlotIndex);
			if (
				RodModuleState.supports(candidateStack)
					&& (candidateStack == clickedStack || ItemStack.areItemsAndComponentsEqual(candidateStack, clickedStack))
			) {
				hostSlotIndex = preferredSlotIndex;
			}
		}

		if (hostSlotIndex < 0) {
			for (int i = 0; i < inventory.size(); i++) {
				ItemStack invStack = inventory.getStack(i);
				if (invStack == clickedStack && RodModuleState.supports(invStack)) {
					hostSlotIndex = i;
					break;
				}
			}
		}

		if (hostSlotIndex < 0) {
			for (int i = 0; i < inventory.size(); i++) {
				ItemStack invStack = inventory.getStack(i);
				if (RodModuleState.supports(invStack) && ItemStack.areItemsAndComponentsEqual(invStack, clickedStack)) {
					hostSlotIndex = i;
					break;
				}
			}
		}

		if (hostSlotIndex < 0) {
			return false;
		}

		final int targetHostSlot = hostSlotIndex;
		serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
			(syncId, playerInventory, ignored) -> RodModuleScreenHandler.create(syncId, playerInventory, targetHostSlot),
			Text.translatable("fishka.rod_modules.title")
		));
		return true;
	}

	@Override
	public void appendTooltip(
		ItemStack stack,
		Item.TooltipContext context,
		TooltipDisplayComponent displayComponent,
		Consumer<Text> textConsumer,
		TooltipType type
	) {
		if (!RodModuleState.supports(stack)) {
			return;
		}

		textConsumer.accept(Text.empty());
		textConsumer.accept(Text.translatable("fishka.rod.tooltip.modules").formatted(Formatting.GOLD));

		int installedCount = 0;
		for (RodModuleSlot slot : RodModuleSlot.values()) {
			ItemStack moduleStack = RodModuleState.getInstalledModuleStack(stack, slot);
			if (moduleStack.isEmpty() || !(moduleStack.getItem() instanceof RodModuleItem moduleItem)) {
				continue;
			}
			installedCount++;
			FishEncounterTier tier = moduleItem.tier();
			textConsumer.accept(
				Text.literal("• ").formatted(Formatting.DARK_GRAY)
					.append(Text.translatable(slot.translationKey()).formatted(Formatting.GRAY))
					.append(Text.literal(": ").formatted(Formatting.DARK_GRAY))
					.append(moduleStack.getName().copy().formatted(RodModuleCatalog.tierColor(tier)))
			);
			textConsumer.accept(Text.literal("  ").append(RodModuleCatalog.effectText(slot, tier)));
		}

		if (installedCount == 0) {
			textConsumer.accept(Text.translatable("fishka.rod.tooltip.modules_empty").formatted(Formatting.DARK_GRAY));
		} else {
			textConsumer.accept(
				Text.translatable("fishka.rod.tooltip.installed_count", installedCount, RodModuleSlot.values().length)
					.formatted(Formatting.AQUA)
			);
		}
	}
}

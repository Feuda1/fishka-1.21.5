package feuda.fishka.item;

import feuda.fishka.fishing.FishingConfig;
import feuda.fishka.fishing.FishingLootService;
import feuda.fishka.fishing.FishingMiniGameManager;
import feuda.fishka.fishing.FishingProgressionService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class DebugFishingToolItem extends Item {
	public DebugFishingToolItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		if (!(user instanceof ServerPlayerEntity player)) {
			return ActionResult.SUCCESS;
		}

		FishingProgressionService.FishingProfile profile = FishingProgressionService.getProfile(player);
		FishingConfig.MiniGameBalanceConfig cfg = FishingConfig.get().miniGameBalance;

		player.sendMessage(Text.translatable("fishka.debug.title").formatted(Formatting.AQUA), false);
		player.sendMessage(Text.translatable("fishka.debug.level_catches", profile.level(), profile.totalCatches()).formatted(Formatting.GOLD), false);
		player.sendMessage(Text.translatable("fishka.debug.active_bobber", yesNo(player.fishHook != null)).formatted(Formatting.GRAY), false);
		player.sendMessage(Text.translatable("fishka.debug.waiting", yesNo(FishingMiniGameManager.hasWaitingSession(player))).formatted(Formatting.GRAY), false);
		player.sendMessage(Text.translatable("fishka.debug.active", yesNo(FishingMiniGameManager.hasActiveSession(player))).formatted(Formatting.GRAY), false);
		player.sendMessage(
			Text.translatable("fishka.debug.balance", cfg.softStartTicks, cfg.baseDurationTicks, yesNo(cfg.burstEnabled)).formatted(Formatting.DARK_AQUA),
			false
		);

		if (player.isSneaking()) {
			FishingLootService.CatchResult preview = FishingLootService.rollCatchDebug(player);
			ItemStack stack = preview.itemStack();
			if (!player.giveItemStack(stack)) {
				player.dropItem(stack, false);
			}
			player.sendMessage(Text.translatable("fishka.debug.drop_granted").formatted(Formatting.GREEN), true);
		}

		return ActionResult.SUCCESS;
	}

	private static Text yesNo(boolean value) {
		return Text.translatable(value ? "fishka.common.yes" : "fishka.common.no");
	}
}

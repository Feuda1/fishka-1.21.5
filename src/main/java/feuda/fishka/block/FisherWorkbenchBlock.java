package feuda.fishka.block;

import feuda.fishka.fishing.FishingSkillNode;
import feuda.fishka.fishing.FishingSkillService;
import feuda.fishka.screen.FisherWorkbenchScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FisherWorkbenchBlock extends Block {
	public FisherWorkbenchBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return ActionResult.PASS;
		}

		boolean unlocked = FishingSkillService.hasNodeUnlocked(serverPlayer, FishingSkillNode.UNLOCK_FISHER_CRAFTING);
		serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
			(syncId, playerInventory, ignored) -> FisherWorkbenchScreenHandler.create(
				syncId,
				playerInventory,
				ScreenHandlerContext.create(world, pos),
				unlocked
			),
			Text.translatable("block.fishka.fisher_workbench")
		));
		return ActionResult.CONSUME;
	}
}

package feuda.fishka.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {
	@Inject(method = "removeIfInvalid", at = @At("HEAD"), cancellable = true)
	private void fishka$allowCustomFishingRods(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
		ItemStack main = player.getMainHandStack();
		ItemStack off = player.getOffHandStack();
		boolean hasAnyFishingRod = main.getItem() instanceof FishingRodItem || off.getItem() instanceof FishingRodItem;

		FishingBobberEntity bobber = (FishingBobberEntity) (Object) this;
		boolean invalid = player.isRemoved()
			|| !player.isAlive()
			|| !hasAnyFishingRod
			|| bobber.squaredDistanceTo(player) > 1024.0;

		if (invalid) {
			bobber.discard();
			cir.setReturnValue(true);
			return;
		}

		cir.setReturnValue(false);
	}
}

package feuda.fishka.mixin;

import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FishingBobberEntity.class)
public interface FishingBobberEntityBiteAccessor {
	@Accessor("caughtFish")
	boolean fishka$hasCaughtFish();
}

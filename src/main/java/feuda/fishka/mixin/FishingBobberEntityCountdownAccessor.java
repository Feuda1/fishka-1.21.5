package feuda.fishka.mixin;

import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FishingBobberEntity.class)
public interface FishingBobberEntityCountdownAccessor {
	@Accessor("waitCountdown")
	int fishka$getWaitCountdown();

	@Accessor("waitCountdown")
	void fishka$setWaitCountdown(int value);

	@Accessor("fishTravelCountdown")
	int fishka$getFishTravelCountdown();

	@Accessor("fishTravelCountdown")
	void fishka$setFishTravelCountdown(int value);

	@Accessor("hookCountdown")
	int fishka$getHookCountdown();

	@Accessor("hookCountdown")
	void fishka$setHookCountdown(int value);
}

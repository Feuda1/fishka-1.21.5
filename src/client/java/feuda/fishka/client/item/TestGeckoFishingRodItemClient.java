package feuda.fishka.client.item;

import feuda.fishka.client.render.item.TestGeckoFishingRodRenderer;
import feuda.fishka.item.TestGeckoFishingRodItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animatable.processing.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public final class TestGeckoFishingRodItemClient extends TestGeckoFishingRodItem implements GeoItem {
	private static final String IDLE_CONTROLLER = "idle_loop";
	private static final String CAST_CONTROLLER = "cast_controller";
	private static final String CAST_ANIMATION = "cast_once";

	private static final RawAnimation IDLE_LOOP = RawAnimation.begin().thenLoop("animation.model.loop");
	private static final RawAnimation CAST_ONCE = RawAnimation.begin().thenPlay("animation.model.once");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public TestGeckoFishingRodItemClient(Settings settings) {
		super(settings);
		GeoItem.registerSyncedAnimatable(this);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		boolean hadHook = user instanceof ServerPlayerEntity serverPlayer && serverPlayer.fishHook != null;
		ActionResult result = super.use(world, user, hand);

		if (!world.isClient
			&& result.isAccepted()
			&& !hadHook
			&& user instanceof ServerPlayerEntity serverPlayer
			&& serverPlayer.fishHook != null
			&& world instanceof ServerWorld serverWorld
		) {
			long instanceId = GeoItem.getOrAssignId(serverPlayer.getStackInHand(hand), serverWorld);
			if (instanceId != Long.MAX_VALUE) {
				triggerAnim(serverPlayer, instanceId, CAST_CONTROLLER, CAST_ANIMATION);
			}
		}

		return result;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(
			IDLE_CONTROLLER,
			0,
			state -> {
				ItemDisplayContext perspective = state.hasData(DataTickets.ITEM_RENDER_PERSPECTIVE)
					? state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE)
					: null;
				if (isHandPerspective(perspective)) {
					state.controller().setAnimation(IDLE_LOOP);
					return PlayState.CONTINUE;
				}
				return PlayState.STOP;
			}
		));

		controllers.add(new AnimationController<>(CAST_CONTROLLER, 0, state -> PlayState.STOP)
			.triggerableAnim(CAST_ANIMATION, CAST_ONCE));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	@Override
	public boolean isPerspectiveAware() {
		return true;
	}

	@Override
	public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
		consumer.accept(new GeoRenderProvider() {
			private GeoItemRenderer<?> renderer;

			@Override
			public GeoItemRenderer<?> getGeoItemRenderer() {
				if (this.renderer == null) {
					this.renderer = new TestGeckoFishingRodRenderer();
				}
				return this.renderer;
			}
		});
	}

	private static boolean isHandPerspective(ItemDisplayContext context) {
		if (context == null) {
			return false;
		}
		return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
			|| context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
			|| context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
			|| context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}
}

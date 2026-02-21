package feuda.fishka.client.render.item;

import feuda.fishka.Fishka;
import feuda.fishka.client.item.TestGeckoFishingRodItemClient;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public final class TestGeckoFishingRodModel extends GeoModel<TestGeckoFishingRodItemClient> {
	@Override
	public Identifier getModelResource(GeoRenderState renderState) {
		return Fishka.id("test_fishing_rod");
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		return Fishka.id("textures/item/test_fishing_rod.png");
	}

	@Override
	public Identifier getAnimationResource(TestGeckoFishingRodItemClient animatable) {
		return Fishka.id("test_fishing_rod");
	}
}

package feuda.fishka.client.render.item;

import feuda.fishka.client.item.TestGeckoFishingRodItemClient;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class TestGeckoFishingRodRenderer extends GeoItemRenderer<TestGeckoFishingRodItemClient> {
	public TestGeckoFishingRodRenderer() {
		super(new TestGeckoFishingRodModel());
	}
}

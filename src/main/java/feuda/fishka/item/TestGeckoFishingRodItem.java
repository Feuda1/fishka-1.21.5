package feuda.fishka.item;

/**
 * Server-safe base class for the test rod.
 * Client-side GeckoLib behavior is attached by a client-only subclass.
 */
public class TestGeckoFishingRodItem extends CustomFishingRodItem {
	public TestGeckoFishingRodItem(Settings settings) {
		super(settings);
	}
}

package feuda.fishka;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FishkaCoreTest {
	@Test
	void modIdIsStable() {
		assertEquals("fishka", Fishka.MOD_ID);
	}
}

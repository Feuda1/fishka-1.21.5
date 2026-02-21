package feuda.fishka.fishing;

public enum FishEncounterTier {
	COMMON(0),
	UNCOMMON(1),
	RARE(2),
	EPIC(3),
	LEGENDARY(4);

	private final int id;

	FishEncounterTier(int id) {
		this.id = id;
	}

	public int id() {
		return id;
	}

	public static FishEncounterTier fromId(int id) {
		for (FishEncounterTier value : values()) {
			if (value.id == id) {
				return value;
			}
		}
		return COMMON;
	}
}

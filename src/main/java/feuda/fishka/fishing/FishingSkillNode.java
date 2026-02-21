package feuda.fishka.fishing;

public enum FishingSkillNode {
	UNLOCK_FISHER_CRAFTING(0, 1);

	private final int id;
	private final int cost;

	FishingSkillNode(int id, int cost) {
		this.id = id;
		this.cost = cost;
	}

	public int id() {
		return id;
	}

	public int cost() {
		return cost;
	}

	public int bitMask() {
		return 1 << id;
	}

	public static FishingSkillNode fromId(int id) {
		for (FishingSkillNode value : values()) {
			if (value.id == id) {
				return value;
			}
		}
		return null;
	}

	public static int knownMask() {
		int mask = 0;
		for (FishingSkillNode value : values()) {
			mask |= value.bitMask();
		}
		return mask;
	}
}

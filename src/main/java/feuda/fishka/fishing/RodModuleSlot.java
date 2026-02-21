package feuda.fishka.fishing;

import java.util.Locale;

public enum RodModuleSlot {
	HANDLE(0, "handle", "fishka.module.slot.handle"),
	REEL(1, "reel", "fishka.module.slot.reel"),
	ROD(2, "rod", "fishka.module.slot.rod"),
	LINE(3, "line", "fishka.module.slot.line"),
	BOBBER(4, "bobber", "fishka.module.slot.bobber"),
	HOOK(5, "hook", "fishka.module.slot.hook");

	private final int index;
	private final String key;
	private final String translationKey;

	RodModuleSlot(int index, String key, String translationKey) {
		this.index = index;
		this.key = key;
		this.translationKey = translationKey;
	}

	public int index() {
		return index;
	}

	public String key() {
		return key;
	}

	public String translationKey() {
		return translationKey;
	}

	public static RodModuleSlot fromIndex(int index) {
		for (RodModuleSlot value : values()) {
			if (value.index == index) {
				return value;
			}
		}
		return null;
	}

	public static RodModuleSlot fromToken(String raw) {
		if (raw == null) {
			return null;
		}
		String token = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
		return switch (token) {
			case "handle", "рукоять" -> HANDLE;
			case "reel", "катушка" -> REEL;
			case "rod", "удилище" -> ROD;
			case "line", "леска" -> LINE;
			case "bobber", "поплавок" -> BOBBER;
			case "hook", "крючок", "крюк" -> HOOK;
			default -> null;
		};
	}
}

package feuda.fishka.fishing.net;

import feuda.fishka.Fishka;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record FishkaMinigameLifecycleS2CPayload(
	int sessionId,
	Event event,
	Reason reason
) implements CustomPayload {
	public static final CustomPayload.Id<FishkaMinigameLifecycleS2CPayload> ID = new CustomPayload.Id<>(Fishka.id("minigame_lifecycle"));
	public static final PacketCodec<RegistryByteBuf, FishkaMinigameLifecycleS2CPayload> CODEC = PacketCodec.of(
		FishkaMinigameLifecycleS2CPayload::write,
		FishkaMinigameLifecycleS2CPayload::read
	);

	private static FishkaMinigameLifecycleS2CPayload read(RegistryByteBuf buf) {
		int sessionId = buf.readVarInt();
		Event event = Event.fromId(buf.readVarInt());
		Reason reason = Reason.fromId(buf.readVarInt());
		return new FishkaMinigameLifecycleS2CPayload(sessionId, event, reason);
	}

	private static void write(FishkaMinigameLifecycleS2CPayload payload, RegistryByteBuf buf) {
		buf.writeVarInt(payload.sessionId);
		buf.writeVarInt(payload.event.id);
		buf.writeVarInt(payload.reason.id);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}

	public enum Event {
		START(0),
		SUCCESS(1),
		FAIL(2),
		CANCEL(3);

		private final int id;

		Event(int id) {
			this.id = id;
		}

		private static Event fromId(int id) {
			for (Event value : values()) {
				if (value.id == id) {
					return value;
				}
			}
			return CANCEL;
		}
	}

	public enum Reason {
		NONE(0),
		TIMEOUT(1),
		TENSION_COLLAPSE(2),
		PLAYER_CANCEL(3),
		INVALID_STATE(4);

		private final int id;

		Reason(int id) {
			this.id = id;
		}

		private static Reason fromId(int id) {
			for (Reason value : values()) {
				if (value.id == id) {
					return value;
				}
			}
			return INVALID_STATE;
		}
	}
}

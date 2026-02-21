package feuda.fishka.fishing.net;

import feuda.fishka.Fishka;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record FishkaMinigameInputC2SPayload(
	Action action,
	int sessionId,
	long clientGameTime
) implements CustomPayload {
	public static final CustomPayload.Id<FishkaMinigameInputC2SPayload> ID = new CustomPayload.Id<>(Fishka.id("minigame_input"));
	public static final PacketCodec<RegistryByteBuf, FishkaMinigameInputC2SPayload> CODEC = PacketCodec.of(
		FishkaMinigameInputC2SPayload::write,
		FishkaMinigameInputC2SPayload::read
	);

	private static FishkaMinigameInputC2SPayload read(RegistryByteBuf buf) {
		Action action = Action.fromId(buf.readVarInt());
		int sessionId = buf.readVarInt();
		long clientGameTime = buf.readVarLong();
		return new FishkaMinigameInputC2SPayload(action, sessionId, clientGameTime);
	}

	private static void write(FishkaMinigameInputC2SPayload payload, RegistryByteBuf buf) {
		buf.writeVarInt(payload.action.id);
		buf.writeVarInt(payload.sessionId);
		buf.writeVarLong(payload.clientGameTime);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}

	public enum Action {
		START_HOLD(0),
		STOP_HOLD(1);

		private final int id;

		Action(int id) {
			this.id = id;
		}

		private static Action fromId(int id) {
			for (Action value : values()) {
				if (value.id == id) {
					return value;
				}
			}
			return STOP_HOLD;
		}
	}
}

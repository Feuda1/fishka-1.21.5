package feuda.fishka.fishing.net;

import feuda.fishka.Fishka;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record FishkaSkillTreeActionC2SPayload(
	Action action,
	int nodeId
) implements CustomPayload {
	public static final CustomPayload.Id<FishkaSkillTreeActionC2SPayload> ID = new CustomPayload.Id<>(Fishka.id("skill_tree_action"));
	public static final PacketCodec<RegistryByteBuf, FishkaSkillTreeActionC2SPayload> CODEC = PacketCodec.of(
		FishkaSkillTreeActionC2SPayload::write,
		FishkaSkillTreeActionC2SPayload::read
	);

	private static FishkaSkillTreeActionC2SPayload read(RegistryByteBuf buf) {
		Action action = Action.fromId(buf.readVarInt());
		int nodeId = buf.readVarInt();
		return new FishkaSkillTreeActionC2SPayload(action, nodeId);
	}

	private static void write(FishkaSkillTreeActionC2SPayload payload, RegistryByteBuf buf) {
		buf.writeVarInt(payload.action.id);
		buf.writeVarInt(payload.nodeId);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}

	public enum Action {
		REQUEST_SYNC(0),
		UNLOCK_NODE(1),
		RESPEC_ALL(2);

		private final int id;

		Action(int id) {
			this.id = id;
		}

		public static Action fromId(int id) {
			for (Action value : values()) {
				if (value.id == id) {
					return value;
				}
			}
			return REQUEST_SYNC;
		}
	}
}

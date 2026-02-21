package feuda.fishka.fishing.net;

import feuda.fishka.Fishka;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record FishkaOpenRodModulesC2SPayload(
	int slotIndex
) implements CustomPayload {
	public static final CustomPayload.Id<FishkaOpenRodModulesC2SPayload> ID = new CustomPayload.Id<>(Fishka.id("open_rod_modules"));
	public static final PacketCodec<RegistryByteBuf, FishkaOpenRodModulesC2SPayload> CODEC = PacketCodec.of(
		FishkaOpenRodModulesC2SPayload::write,
		FishkaOpenRodModulesC2SPayload::read
	);

	private static FishkaOpenRodModulesC2SPayload read(RegistryByteBuf buf) {
		return new FishkaOpenRodModulesC2SPayload(buf.readVarInt());
	}

	private static void write(FishkaOpenRodModulesC2SPayload payload, RegistryByteBuf buf) {
		buf.writeVarInt(payload.slotIndex);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

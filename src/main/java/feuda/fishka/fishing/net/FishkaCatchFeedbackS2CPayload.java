package feuda.fishka.fishing.net;

import feuda.fishka.Fishka;
import feuda.fishka.fishing.FishEncounterTier;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public record FishkaCatchFeedbackS2CPayload(
	ItemStack displayStack,
	Text title,
	Text statLine,
	int xp,
	int newLevel,
	int displayTicks,
	FishEncounterTier tier,
	int revealDelayTicks,
	boolean showRarityRays
) implements CustomPayload {
	public static final CustomPayload.Id<FishkaCatchFeedbackS2CPayload> ID = new CustomPayload.Id<>(Fishka.id("catch_feedback"));
	public static final PacketCodec<RegistryByteBuf, FishkaCatchFeedbackS2CPayload> CODEC = PacketCodec.of(
		FishkaCatchFeedbackS2CPayload::write,
		FishkaCatchFeedbackS2CPayload::read
	);

	private static FishkaCatchFeedbackS2CPayload read(RegistryByteBuf buf) {
		ItemStack displayStack = ItemStack.PACKET_CODEC.decode(buf);
		Text title = TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC.decode(buf);
		Text statLine = TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC.decode(buf);
		int xp = buf.readVarInt();
		int newLevel = buf.readVarInt();
		int displayTicks = buf.readVarInt();
		FishEncounterTier tier = FishEncounterTier.fromId(buf.readVarInt());
		int revealDelayTicks = buf.readVarInt();
		boolean showRarityRays = buf.readBoolean();
		return new FishkaCatchFeedbackS2CPayload(
			displayStack,
			title,
			statLine,
			xp,
			newLevel,
			displayTicks,
			tier,
			revealDelayTicks,
			showRarityRays
		);
	}

	private static void write(FishkaCatchFeedbackS2CPayload payload, RegistryByteBuf buf) {
		ItemStack.PACKET_CODEC.encode(buf, payload.displayStack);
		TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC.encode(buf, payload.title);
		TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC.encode(buf, payload.statLine);
		buf.writeVarInt(payload.xp);
		buf.writeVarInt(payload.newLevel);
		buf.writeVarInt(payload.displayTicks);
		buf.writeVarInt(payload.tier.id());
		buf.writeVarInt(payload.revealDelayTicks);
		buf.writeBoolean(payload.showRarityRays);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

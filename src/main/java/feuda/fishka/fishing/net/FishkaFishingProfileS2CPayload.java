package feuda.fishka.fishing.net;

import feuda.fishka.Fishka;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record FishkaFishingProfileS2CPayload(
	int level,
	int currentLevelXp,
	int nextLevelXp,
	boolean levelUpEvent,
	int money,
	int availableSkillPoints,
	int skillPointsSpent,
	int unlockedNodesMask
) implements CustomPayload {
	public static final CustomPayload.Id<FishkaFishingProfileS2CPayload> ID = new CustomPayload.Id<>(Fishka.id("fishing_profile"));
	public static final PacketCodec<RegistryByteBuf, FishkaFishingProfileS2CPayload> CODEC = PacketCodec.of(
		FishkaFishingProfileS2CPayload::write,
		FishkaFishingProfileS2CPayload::read
	);

	private static FishkaFishingProfileS2CPayload read(RegistryByteBuf buf) {
		int level = buf.readVarInt();
		int currentLevelXp = buf.readVarInt();
		int nextLevelXp = buf.readVarInt();
		boolean levelUpEvent = buf.readBoolean();
		int money = buf.readVarInt();
		int availableSkillPoints = buf.readVarInt();
		int skillPointsSpent = buf.readVarInt();
		int unlockedNodesMask = buf.readVarInt();
		return new FishkaFishingProfileS2CPayload(
			level,
			currentLevelXp,
			nextLevelXp,
			levelUpEvent,
			money,
			availableSkillPoints,
			skillPointsSpent,
			unlockedNodesMask
		);
	}

	private static void write(FishkaFishingProfileS2CPayload payload, RegistryByteBuf buf) {
		buf.writeVarInt(payload.level);
		buf.writeVarInt(payload.currentLevelXp);
		buf.writeVarInt(payload.nextLevelXp);
		buf.writeBoolean(payload.levelUpEvent);
		buf.writeVarInt(payload.money);
		buf.writeVarInt(payload.availableSkillPoints);
		buf.writeVarInt(payload.skillPointsSpent);
		buf.writeVarInt(payload.unlockedNodesMask);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

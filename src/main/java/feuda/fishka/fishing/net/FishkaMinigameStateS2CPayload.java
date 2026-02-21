package feuda.fishka.fishing.net;

import feuda.fishka.Fishka;
import feuda.fishka.fishing.FishEncounterTier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record FishkaMinigameStateS2CPayload(
	int sessionId,
	boolean active,
	float catchProgress01,
	float tension01,
	float tensionVelocity01,
	float safeZoneCenter01,
	float safeZoneHalfWidth01,
	float fishForce01,
	int timeLeftTicks,
	int softStartTicksLeft,
	boolean burstActive,
	FishEncounterTier tier
) implements CustomPayload {
	public static final CustomPayload.Id<FishkaMinigameStateS2CPayload> ID = new CustomPayload.Id<>(Fishka.id("minigame_state"));
	public static final PacketCodec<RegistryByteBuf, FishkaMinigameStateS2CPayload> CODEC = PacketCodec.of(
		FishkaMinigameStateS2CPayload::write,
		FishkaMinigameStateS2CPayload::read
	);

	private static FishkaMinigameStateS2CPayload read(RegistryByteBuf buf) {
		int sessionId = buf.readVarInt();
		boolean active = buf.readBoolean();
		float catchProgress01 = buf.readFloat();
		float tension01 = buf.readFloat();
		float tensionVelocity01 = buf.readFloat();
		float safeZoneCenter01 = buf.readFloat();
		float safeZoneHalfWidth01 = buf.readFloat();
		float fishForce01 = buf.readFloat();
		int timeLeftTicks = buf.readVarInt();
		int softStartTicksLeft = buf.readVarInt();
		boolean burstActive = buf.readBoolean();
		FishEncounterTier tier = FishEncounterTier.fromId(buf.readVarInt());
		return new FishkaMinigameStateS2CPayload(
			sessionId,
			active,
			catchProgress01,
			tension01,
			tensionVelocity01,
			safeZoneCenter01,
			safeZoneHalfWidth01,
			fishForce01,
			timeLeftTicks,
			softStartTicksLeft,
			burstActive,
			tier
		);
	}

	private static void write(FishkaMinigameStateS2CPayload payload, RegistryByteBuf buf) {
		buf.writeVarInt(payload.sessionId);
		buf.writeBoolean(payload.active);
		buf.writeFloat(payload.catchProgress01);
		buf.writeFloat(payload.tension01);
		buf.writeFloat(payload.tensionVelocity01);
		buf.writeFloat(payload.safeZoneCenter01);
		buf.writeFloat(payload.safeZoneHalfWidth01);
		buf.writeFloat(payload.fishForce01);
		buf.writeVarInt(payload.timeLeftTicks);
		buf.writeVarInt(payload.softStartTicksLeft);
		buf.writeBoolean(payload.burstActive);
		buf.writeVarInt(payload.tier.id());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

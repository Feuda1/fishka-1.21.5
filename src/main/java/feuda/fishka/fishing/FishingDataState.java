package feuda.fishka.fishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.PersistentStateManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FishingDataState extends PersistentState {
	private static final Codec<Map<UUID, FishingProfileData>> PROFILE_MAP_CODEC = Codec.unboundedMap(
		net.minecraft.util.Uuids.STRING_CODEC,
		FishingProfileData.CODEC
	);

	private static final Codec<FishingDataState> CODEC = RecordCodecBuilder.create(instance ->
		instance.group(
			PROFILE_MAP_CODEC.fieldOf("profiles").forGetter(state -> state.profiles)
		).apply(instance, FishingDataState::new)
	);

	public static final PersistentStateType<FishingDataState> TYPE = new PersistentStateType<>(
		"fishka_fishing_profiles",
		context -> new FishingDataState(),
		context -> CODEC,
		DataFixTypes.LEVEL
	);

	private final Map<UUID, FishingProfileData> profiles;

	private FishingDataState() {
		this(new HashMap<>());
	}

	private FishingDataState(Map<UUID, FishingProfileData> profiles) {
		this.profiles = new HashMap<>(profiles);
	}

	public static FishingDataState get(MinecraftServer server) {
		PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
		return manager.getOrCreate(TYPE);
	}

	public FishingProfileData getOrCreateProfile(UUID playerId) {
		return profiles.computeIfAbsent(playerId, ignored -> new FishingProfileData());
	}

	public void putProfile(UUID playerId, FishingProfileData data) {
		profiles.put(playerId, data);
	}

	public record FishingProfileData(
		int level,
		int totalXp,
		int currentLevelXp,
		int totalCatches,
		int money,
		int skillPointsSpent,
		int unlockedNodesMask
	) {
		public static final Codec<FishingProfileData> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
				Codec.INT.fieldOf("level").forGetter(FishingProfileData::level),
				Codec.INT.fieldOf("totalXp").forGetter(FishingProfileData::totalXp),
				Codec.INT.fieldOf("currentLevelXp").forGetter(FishingProfileData::currentLevelXp),
				Codec.INT.fieldOf("totalCatches").forGetter(FishingProfileData::totalCatches),
				Codec.INT.optionalFieldOf("money", 0).forGetter(FishingProfileData::money),
				Codec.INT.optionalFieldOf("skillPointsSpent", 0).forGetter(FishingProfileData::skillPointsSpent),
				Codec.INT.optionalFieldOf("unlockedNodesMask", 0).forGetter(FishingProfileData::unlockedNodesMask)
			).apply(instance, FishingProfileData::new)
		);

		public FishingProfileData() {
			this(1, 0, 0, 0, 0, 0, 0);
		}
	}
}

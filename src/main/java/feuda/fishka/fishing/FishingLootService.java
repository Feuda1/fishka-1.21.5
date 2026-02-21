package feuda.fishka.fishing;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FishingLootService {
	private FishingLootService() {
	}

	public static EncounterPlan prepareEncounter(ServerPlayerEntity player, FishingBobberEntity bobber) {
		return prepareEncounter(player, bobber, player.getMainHandStack(), Hand.MAIN_HAND);
	}

	public static EncounterPlan prepareEncounter(ServerPlayerEntity player, FishingBobberEntity bobber, ItemStack rodStack, Hand hand) {
		Random random = player.getRandom();
		int level = FishingProgressionService.getProfile(player).level();
		FishingConfig.LootConfig cfg = FishingConfig.get().loot;
		SpeciesContext context = buildContextFromBobber(player, bobber);
		RodComponentBonusProvider.RodComponentBonus bonuses = RodComponentBonusProvider.resolve(player, hand, rodStack);

		float treasureChance = Math.min(cfg.maxTreasureChance, cfg.baseTreasureChance + level * cfg.treasureLevelBonus);
		float materialChance = cfg.materialChance;
		float roll = random.nextFloat();

		if (roll < treasureChance) {
			return prepareTreasureEncounter(random);
		}
		if (roll < treasureChance + materialChance) {
			return prepareMaterialEncounter(random);
		}
		return prepareFishEncounter(
			random,
			level,
			context,
			bonuses.rarityLuckBonus(),
			bonuses.lengthBonus(),
			bonuses.weightBonus()
		);
	}

	public static CatchResult resolveCatch(ServerPlayerEntity player, EncounterPlan plan) {
		return switch (plan.type()) {
			case FISH -> resolveFishCatch(player.getRandom(), plan);
			case MATERIAL -> resolveMaterialCatch(plan);
			case TREASURE -> resolveTreasureCatch(plan);
		};
	}

	public static CatchResult rollCatchDebug(ServerPlayerEntity player) {
		SpeciesContext context = buildContext(player.getWorld(), player.getBlockPos(), player.getWorld().getTimeOfDay());
		EncounterPlan plan = prepareFishEncounter(
			player.getRandom(),
			FishingProgressionService.getProfile(player).level(),
			context,
			0.0f,
			0.0f,
			0.0f
		);
		return resolveCatch(player, plan);
	}

	private static EncounterPlan prepareFishEncounter(
		Random random,
		int level,
		SpeciesContext context,
		float rarityLuckBonus,
		float lengthBonus,
		float weightBonus
	) {
		FishEncounterTier tier = rollTier(random, level, rarityLuckBonus);
		FishingConfig.SpeciesConfig species = pickSpecies(random, context);
		return new EncounterPlan(
			EncounterType.FISH,
			tier,
			species,
			ItemStack.EMPTY,
			xpForTier(tier),
			true,
			lengthBonus,
			weightBonus,
			rarityLuckBonus
		);
	}

	private static EncounterPlan prepareMaterialEncounter(Random random) {
		int pick = random.nextInt(4);
		ItemStack stack = switch (pick) {
			case 0 -> new ItemStack(Items.STRING, 1 + random.nextInt(2));
			case 1 -> new ItemStack(Items.PRISMARINE_SHARD, 1 + random.nextInt(2));
			case 2 -> new ItemStack(Items.CLAY_BALL, 2 + random.nextInt(2));
			default -> new ItemStack(Items.INK_SAC, 1);
		};
		FishEncounterTier tier = rollTier(random, FishingConfig.get().encounterBalance.materialTierWeights);
		int xp = Math.max(6, Math.round(xpForTier(tier) * 0.65f));
		return new EncounterPlan(EncounterType.MATERIAL, tier, null, stack, xp, true, 0.0f, 0.0f, 0.0f);
	}

	private static EncounterPlan prepareTreasureEncounter(Random random) {
		int pick = random.nextInt(4);
		ItemStack stack = switch (pick) {
			case 0 -> new ItemStack(Items.NAUTILUS_SHELL, 1);
			case 1 -> new ItemStack(Items.HEART_OF_THE_SEA, 1);
			case 2 -> new ItemStack(Items.NAME_TAG, 1);
			default -> new ItemStack(Items.PRISMARINE_CRYSTALS, 2 + random.nextInt(2));
		};
		FishEncounterTier tier = rollTier(random, FishingConfig.get().encounterBalance.treasureTierWeights);
		int xp = Math.max(14, Math.round(xpForTier(tier) * 1.05f));
		return new EncounterPlan(EncounterType.TREASURE, tier, null, stack, xp, true, 0.0f, 0.0f, 0.0f);
	}

	private static CatchResult resolveFishCatch(Random random, EncounterPlan plan) {
		FishingConfig.SpeciesConfig species = plan.species() != null ? plan.species() : FishingConfig.SpeciesConfig.defaults().get(0);
		FishEncounterTier tier = plan.tier();
		float rarityScale = weightScaleForTier(tier);

		float weightScale = rarityScale * (1.0f + clamp(plan.weightBonus(), 0.0f, 0.90f));
		float lengthScale = (0.92f + (rarityScale - 1.0f) * 0.7f) * (1.0f + clamp(plan.lengthBonus(), 0.0f, 0.90f));
		float weight = randRange(random, species.minKg, species.maxKg * weightScale);
		float length = randRange(random, species.minCm, species.maxCm * lengthScale);
		String weightStr = format(weight);
		String lengthStr = format(length);

		Text rarityText = tierDisplayName(tier);
		Text speciesText = Text.literal(species.name).formatted(Formatting.WHITE);
		Text title = Text.translatable("fishka.catch.fish.title", rarityText, speciesText).formatted(colorForTier(tier));
		Text statLine = Text.translatable("fishka.catch.fish.statline", weightStr, lengthStr).formatted(Formatting.GRAY);

		ItemStack stack = new ItemStack(itemForTier(tier));
		stack.set(DataComponentTypes.CUSTOM_NAME, title.copy());
		stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
			Text.translatable("fishka.catch.fish.lore.weight", weightStr).formatted(Formatting.GRAY),
			Text.translatable("fishka.catch.fish.lore.length", lengthStr).formatted(Formatting.GRAY),
			Text.translatable("fishka.catch.fish.lore.rarity", rarityText).formatted(colorForTier(tier))
		)));

		return new CatchResult(stack, plan.xp(), title, statLine, tier, true);
	}

	private static CatchResult resolveMaterialCatch(EncounterPlan plan) {
		ItemStack stack = plan.fixedStack().copy();
		Text rarityText = tierDisplayName(plan.tier());
		Text title = Text.translatable("fishka.catch.material.title", rarityText, stack.getName()).formatted(colorForTier(plan.tier()));
		Text statLine = Text.translatable("fishka.catch.material.statline").formatted(Formatting.GRAY);
		return new CatchResult(stack, plan.xp(), title, statLine, plan.tier(), plan.showRarityRays());
	}

	private static CatchResult resolveTreasureCatch(EncounterPlan plan) {
		ItemStack stack = plan.fixedStack().copy();
		Text rarityText = tierDisplayName(plan.tier());
		Text title = Text.translatable("fishka.catch.treasure.title", rarityText, stack.getName()).formatted(colorForTier(plan.tier()));
		Text statLine = Text.translatable("fishka.catch.treasure.statline").formatted(Formatting.GRAY);
		return new CatchResult(stack, plan.xp(), title, statLine, plan.tier(), plan.showRarityRays());
	}

	private static FishingConfig.SpeciesConfig pickSpecies(Random random, SpeciesContext context) {
		List<FishingConfig.SpeciesConfig> species = FishingConfig.get().species;
		if (species.isEmpty()) {
			return FishingConfig.SpeciesConfig.defaults().get(0);
		}

		float totalWeight = 0.0f;
		List<Float> weights = new ArrayList<>(species.size());
		for (FishingConfig.SpeciesConfig cfg : species) {
			float weight = Math.max(0.05f, cfg.baseWeight);
			if (context.biomePath.contains(cfg.biomeTag.toLowerCase(Locale.ROOT))) {
				weight *= 1.8f;
			}
			if (context.timeBucket.equalsIgnoreCase(cfg.preferredTime)) {
				weight *= 1.35f;
			}
			weights.add(weight);
			totalWeight += weight;
		}

		float roll = random.nextFloat() * totalWeight;
		for (int i = 0; i < species.size(); i++) {
			roll -= weights.get(i);
			if (roll <= 0) {
				return species.get(i);
			}
		}
		return species.get(species.size() - 1);
	}

	private static SpeciesContext buildContextFromBobber(ServerPlayerEntity player, FishingBobberEntity bobber) {
		if (bobber != null && !bobber.isRemoved()) {
			return buildContext(bobber.getWorld(), bobber.getBlockPos(), bobber.getWorld().getTimeOfDay());
		}
		return buildContext(player.getWorld(), player.getBlockPos(), player.getWorld().getTimeOfDay());
	}

	private static SpeciesContext buildContext(World world, BlockPos pos, long timeOfDay) {
		String biomePath = world.getBiome(pos)
			.getKey()
			.map(key -> key.getValue().getPath())
			.orElse("unknown");
		long dayTime = timeOfDay % 24000L;
		String timeBucket = (dayTime >= 13000L && dayTime <= 23000L) ? "night" : "day";
		return new SpeciesContext(biomePath.toLowerCase(Locale.ROOT), timeBucket);
	}

	private static FishEncounterTier rollTier(Random random, int level, float rarityLuckBonus) {
		float luck = clamp(rarityLuckBonus, 0.0f, 0.80f);
		int common = Math.max(30, 56 - level / 2);
		int uncommon = 26 + level / 3;
		int rare = 12 + level / 4;
		int epic = 5 + level / 8;
		int legendary = 1 + level / 25;

		common = Math.max(1, Math.round(common * (1.0f - luck * 0.65f)));
		uncommon = Math.max(1, Math.round(uncommon * (1.0f - luck * 0.35f)));
		rare = Math.max(1, Math.round(rare * (1.0f + luck * 0.55f)));
		epic = Math.max(1, Math.round(epic * (1.0f + luck * 0.90f)));
		legendary = Math.max(1, Math.round(legendary * (1.0f + luck * 1.40f)));

		int total = common + uncommon + rare + epic + legendary;
		int roll = random.nextInt(total);

		if ((roll -= common) < 0) return FishEncounterTier.COMMON;
		if ((roll -= uncommon) < 0) return FishEncounterTier.UNCOMMON;
		if ((roll -= rare) < 0) return FishEncounterTier.RARE;
		if ((roll -= epic) < 0) return FishEncounterTier.EPIC;
		return FishEncounterTier.LEGENDARY;
	}

	private static FishEncounterTier rollTier(Random random, FishingConfig.TierWeightConfig weights) {
		int common = Math.max(1, weights.common);
		int uncommon = Math.max(1, weights.uncommon);
		int rare = Math.max(1, weights.rare);
		int epic = Math.max(1, weights.epic);
		int legendary = Math.max(1, weights.legendary);

		int total = common + uncommon + rare + epic + legendary;
		int roll = random.nextInt(total);
		if ((roll -= common) < 0) return FishEncounterTier.COMMON;
		if ((roll -= uncommon) < 0) return FishEncounterTier.UNCOMMON;
		if ((roll -= rare) < 0) return FishEncounterTier.RARE;
		if ((roll -= epic) < 0) return FishEncounterTier.EPIC;
		return FishEncounterTier.LEGENDARY;
	}

	private static int xpForTier(FishEncounterTier tier) {
		FishingConfig.LootConfig cfg = FishingConfig.get().loot;
		return switch (tier) {
			case COMMON -> cfg.commonXp;
			case UNCOMMON -> cfg.uncommonXp;
			case RARE -> cfg.rareXp;
			case EPIC -> cfg.epicXp;
			case LEGENDARY -> cfg.legendaryXp;
		};
	}

	private static float weightScaleForTier(FishEncounterTier tier) {
		return switch (tier) {
			case COMMON -> 1.00f;
			case UNCOMMON -> 1.15f;
			case RARE -> 1.35f;
			case EPIC -> 1.65f;
			case LEGENDARY -> 2.05f;
		};
	}

	private static Formatting colorForTier(FishEncounterTier tier) {
		return switch (tier) {
			case COMMON -> Formatting.WHITE;
			case UNCOMMON -> Formatting.GREEN;
			case RARE -> Formatting.BLUE;
			case EPIC -> Formatting.LIGHT_PURPLE;
			case LEGENDARY -> Formatting.GOLD;
		};
	}

	private static Text tierDisplayName(FishEncounterTier tier) {
		return Text.translatable("fishka.rarity." + tier.name().toLowerCase(Locale.ROOT)).formatted(colorForTier(tier));
	}

	private static net.minecraft.item.Item itemForTier(FishEncounterTier tier) {
		return switch (tier) {
			case COMMON -> Items.COD;
			case UNCOMMON -> Items.SALMON;
			case RARE -> Items.TROPICAL_FISH;
			case EPIC -> Items.PUFFERFISH;
			case LEGENDARY -> Items.COOKED_SALMON;
		};
	}

	private static float randRange(Random random, float min, float max) {
		return min + random.nextFloat() * (max - min);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static String format(float value) {
		return String.format(Locale.US, "%.2f", value).replace('.', ',');
	}

	public enum EncounterType {
		FISH,
		MATERIAL,
		TREASURE
	}

	public record EncounterPlan(
		EncounterType type,
		FishEncounterTier tier,
		FishingConfig.SpeciesConfig species,
		ItemStack fixedStack,
		int xp,
		boolean showRarityRays,
		float lengthBonus,
		float weightBonus,
		float rarityLuckBonus
	) {
	}

	public record CatchResult(
		ItemStack itemStack,
		int xp,
		Text title,
		Text statLine,
		FishEncounterTier tier,
		boolean showRarityRays
	) {
	}

	private record SpeciesContext(String biomePath, String timeBucket) {
	}
}

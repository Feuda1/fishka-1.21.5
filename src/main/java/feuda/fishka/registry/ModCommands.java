package feuda.fishka.registry;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import feuda.fishka.Fishka;
import feuda.fishka.fishing.FishingConfig;
import feuda.fishka.fishing.FishingDataState;
import feuda.fishka.fishing.FishingEconomyService;
import feuda.fishka.fishing.FishEncounterTier;
import feuda.fishka.fishing.FishingProfileSyncService;
import feuda.fishka.fishing.FishingProgressionService;
import feuda.fishka.fishing.RodModuleCatalog;
import feuda.fishka.fishing.RodModuleSlot;
import feuda.fishka.fishing.RodModuleState;
import feuda.fishka.fishing.FishingSkillNode;
import feuda.fishka.fishing.FishingSkillService;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ModCommands {
	private ModCommands() {
	}

	public static void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("fishka")
				.executes(context -> {
					context.getSource().sendFeedback(() -> Text.translatable("fishka.command.loaded"), false);
					return Command.SINGLE_SUCCESS;
				})
				.then(CommandManager.literal("profile")
					.executes(ModCommands::showProfile))
				.then(CommandManager.literal("reloadconfig")
					.requires(source -> source.hasPermissionLevel(2))
					.executes(context -> {
						FishingConfig.reload();
						context.getSource().sendFeedback(() -> Text.translatable("fishka.command.reload").formatted(Formatting.GREEN), false);
						return Command.SINGLE_SUCCESS;
					}))
				.then(CommandManager.literal("admin")
					.requires(source -> source.hasPermissionLevel(2))
					.then(CommandManager.literal("addxp")
						.then(CommandManager.argument("target", EntityArgumentType.player())
							.then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
								.executes(context -> applyMutation(
									context,
									EntityArgumentType.getPlayer(context, "target"),
									FishingProgressionService.addXp(EntityArgumentType.getPlayer(context, "target"), IntegerArgumentType.getInteger(context, "amount"))
								)))))
					.then(CommandManager.literal("removexp")
						.then(CommandManager.argument("target", EntityArgumentType.player())
							.then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
								.executes(context -> applyMutation(
									context,
									EntityArgumentType.getPlayer(context, "target"),
									FishingProgressionService.removeXp(EntityArgumentType.getPlayer(context, "target"), IntegerArgumentType.getInteger(context, "amount"))
								)))))
					.then(CommandManager.literal("setxp")
						.then(CommandManager.argument("target", EntityArgumentType.player())
							.then(CommandManager.argument("value", IntegerArgumentType.integer(0))
								.executes(context -> applyMutation(
									context,
									EntityArgumentType.getPlayer(context, "target"),
									FishingProgressionService.setCurrentLevelXp(EntityArgumentType.getPlayer(context, "target"), IntegerArgumentType.getInteger(context, "value"))
								)))))
					.then(CommandManager.literal("settotalxp")
						.then(CommandManager.argument("target", EntityArgumentType.player())
							.then(CommandManager.argument("value", IntegerArgumentType.integer(0))
								.executes(context -> applyMutation(
									context,
									EntityArgumentType.getPlayer(context, "target"),
									FishingProgressionService.setTotalXp(EntityArgumentType.getPlayer(context, "target"), IntegerArgumentType.getInteger(context, "value"))
								)))))
					.then(CommandManager.literal("addlevel")
						.then(CommandManager.argument("target", EntityArgumentType.player())
							.then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
								.executes(context -> applyMutation(
									context,
									EntityArgumentType.getPlayer(context, "target"),
									FishingProgressionService.addLevels(EntityArgumentType.getPlayer(context, "target"), IntegerArgumentType.getInteger(context, "amount"))
								)))))
					.then(CommandManager.literal("removelevel")
						.then(CommandManager.argument("target", EntityArgumentType.player())
							.then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
								.executes(context -> applyMutation(
									context,
									EntityArgumentType.getPlayer(context, "target"),
									FishingProgressionService.removeLevels(EntityArgumentType.getPlayer(context, "target"), IntegerArgumentType.getInteger(context, "amount"))
								)))))
					.then(CommandManager.literal("setlevel")
						.then(CommandManager.argument("target", EntityArgumentType.player())
							.then(CommandManager.argument("level", IntegerArgumentType.integer(1))
								.executes(context -> applyMutation(
									context,
									EntityArgumentType.getPlayer(context, "target"),
									FishingProgressionService.setLevel(EntityArgumentType.getPlayer(context, "target"), IntegerArgumentType.getInteger(context, "level"))
								)))))
					.then(CommandManager.literal("clear")
						.then(CommandManager.argument("target", EntityArgumentType.player())
							.executes(context -> applyMutation(
								context,
								EntityArgumentType.getPlayer(context, "target"),
								FishingProgressionService.resetProgress(EntityArgumentType.getPlayer(context, "target"))
							))))
					.then(CommandManager.literal("money")
						.then(CommandManager.literal("add")
							.then(CommandManager.argument("target", EntityArgumentType.player())
								.then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
									.executes(context -> applyMoneyUpdate(
										context,
										EntityArgumentType.getPlayer(context, "target"),
										FishingEconomyService.addMoney(
											EntityArgumentType.getPlayer(context, "target"),
											IntegerArgumentType.getInteger(context, "amount")
										)
									)))))
						.then(CommandManager.literal("remove")
							.then(CommandManager.argument("target", EntityArgumentType.player())
								.then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
									.executes(context -> applyMoneyUpdate(
										context,
										EntityArgumentType.getPlayer(context, "target"),
										FishingEconomyService.removeMoney(
											EntityArgumentType.getPlayer(context, "target"),
											IntegerArgumentType.getInteger(context, "amount")
										)
									)))))
						.then(CommandManager.literal("set")
							.then(CommandManager.argument("target", EntityArgumentType.player())
								.then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
									.executes(context -> applyMoneyUpdate(
										context,
										EntityArgumentType.getPlayer(context, "target"),
										FishingEconomyService.setMoney(
											EntityArgumentType.getPlayer(context, "target"),
											IntegerArgumentType.getInteger(context, "amount")
										)
									)))))
						.then(CommandManager.literal("balance")
							.then(CommandManager.argument("target", EntityArgumentType.player())
								.executes(context -> showMoney(
									context,
									EntityArgumentType.getPlayer(context, "target")
								)))))
					.then(CommandManager.literal("skill")
						.then(CommandManager.literal("reset")
							.then(CommandManager.argument("target", EntityArgumentType.player())
								.executes(context -> resetSkills(
									context,
									EntityArgumentType.getPlayer(context, "target")
								))))
						.then(CommandManager.literal("unlock")
							.then(CommandManager.argument("target", EntityArgumentType.player())
								.then(CommandManager.argument("node", StringArgumentType.word())
									.executes(context -> unlockSkillNode(
										context,
										EntityArgumentType.getPlayer(context, "target"),
										StringArgumentType.getString(context, "node")
									)))))
						.then(CommandManager.literal("points")
							.then(CommandManager.argument("target", EntityArgumentType.player())
								.executes(context -> showSkillPoints(
									context,
									EntityArgumentType.getPlayer(context, "target")
								))))
					)
					.then(CommandManager.literal("module")
						.then(CommandManager.literal("give")
							.then(CommandManager.argument("target", EntityArgumentType.player())
								.then(CommandManager.argument("slot", StringArgumentType.word())
									.then(CommandManager.argument("tier", StringArgumentType.word())
										.executes(context -> giveModule(
											context,
											EntityArgumentType.getPlayer(context, "target"),
											StringArgumentType.getString(context, "slot"),
											StringArgumentType.getString(context, "tier"),
											1
										))
										.then(CommandManager.argument("count", IntegerArgumentType.integer(1, 64))
											.executes(context -> giveModule(
												context,
												EntityArgumentType.getPlayer(context, "target"),
												StringArgumentType.getString(context, "slot"),
												StringArgumentType.getString(context, "tier"),
												IntegerArgumentType.getInteger(context, "count")
											)))
									))))
						.then(CommandManager.literal("setheld")
							.then(CommandManager.argument("target", EntityArgumentType.player())
								.then(CommandManager.argument("slot", StringArgumentType.word())
									.then(CommandManager.argument("tier", StringArgumentType.word())
										.executes(context -> setHeldModule(
											context,
											EntityArgumentType.getPlayer(context, "target"),
											StringArgumentType.getString(context, "slot"),
											StringArgumentType.getString(context, "tier")
										))))))
						.then(CommandManager.literal("clearheld")
							.then(CommandManager.argument("target", EntityArgumentType.player())
								.executes(context -> clearHeldModules(
									context,
									EntityArgumentType.getPlayer(context, "target")
								))))
					)
				);

			dispatcher.register(root);
		});

		Fishka.LOGGER.info("Registered mod commands");
	}

	private static int showProfile(CommandContext<ServerCommandSource> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		FishingProgressionService.FishingProfile profile = FishingProgressionService.getProfile(player);
		int nextXp = FishingProgressionService.xpForNextLevel(profile.level());
		context.getSource().sendFeedback(() -> Text.translatable("fishka.command.profile.title").formatted(Formatting.BLUE), false);
		context.getSource().sendFeedback(() -> Text.translatable("fishka.command.profile.level", profile.level()).formatted(Formatting.GOLD), false);
		context.getSource().sendFeedback(() -> Text.translatable("fishka.command.profile.xp", profile.currentLevelXp(), nextXp).formatted(Formatting.AQUA), false);
		context.getSource().sendFeedback(() -> Text.translatable("fishka.command.profile.catches", profile.totalCatches()).formatted(Formatting.GREEN), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int applyMutation(
		CommandContext<ServerCommandSource> context,
		ServerPlayerEntity target,
		FishingProgressionService.MutationResult mutation
	) {
		FishingProfileSyncService.syncTo(target, mutation.levelDelta() > 0);

		FishingProgressionService.FishingProfile after = mutation.after();
		int nextXp = FishingProgressionService.xpForNextLevel(after.level());
		context.getSource().sendFeedback(
			() -> Text.translatable("fishka.command.admin.updated", target.getDisplayName()).formatted(Formatting.GREEN),
			true
		);
		context.getSource().sendFeedback(
			() -> Text.translatable("fishka.command.profile.level", after.level()).formatted(Formatting.GOLD),
			false
		);
		context.getSource().sendFeedback(
			() -> Text.translatable("fishka.command.profile.xp", after.currentLevelXp(), nextXp).formatted(Formatting.AQUA),
			false
		);
		context.getSource().sendFeedback(
			() -> Text.translatable("fishka.command.profile.catches", after.totalCatches()).formatted(Formatting.GREEN),
			false
		);

		Entity sourceEntity = context.getSource().getEntity();
		boolean samePlayer = sourceEntity instanceof ServerPlayerEntity sourcePlayer && sourcePlayer.getUuid().equals(target.getUuid());
		if (!samePlayer) {
			target.sendMessage(Text.translatable("fishka.command.admin.changed_by_admin").formatted(Formatting.YELLOW), false);
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int applyMoneyUpdate(CommandContext<ServerCommandSource> context, ServerPlayerEntity target, int money) {
		FishingProfileSyncService.syncTo(target, false);
		context.getSource().sendFeedback(
			() -> Text.translatable("fishka.command.admin.money_updated", target.getDisplayName(), money).formatted(Formatting.GREEN),
			true
		);
		return Command.SINGLE_SUCCESS;
	}

	private static int showMoney(CommandContext<ServerCommandSource> context, ServerPlayerEntity target) {
		int money = FishingEconomyService.getMoney(target);
		context.getSource().sendFeedback(
			() -> Text.translatable("fishka.command.admin.money_balance", target.getDisplayName(), money).formatted(Formatting.GOLD),
			false
		);
		return Command.SINGLE_SUCCESS;
	}

	private static int resetSkills(CommandContext<ServerCommandSource> context, ServerPlayerEntity target) {
		FishingSkillService.resetSkills(target);
		FishingProfileSyncService.syncTo(target, false);
		context.getSource().sendFeedback(
			() -> Text.translatable("fishka.command.admin.skill_reset", target.getDisplayName()).formatted(Formatting.GREEN),
			true
		);
		return Command.SINGLE_SUCCESS;
	}

	private static int unlockSkillNode(CommandContext<ServerCommandSource> context, ServerPlayerEntity target, String rawNode) {
		FishingSkillNode node = parseNode(rawNode);
		if (node == null) {
			context.getSource().sendError(Text.translatable("fishka.command.admin.skill_unknown_node", rawNode));
			return 0;
		}
		FishingSkillService.unlockDirect(target, node);
		FishingProfileSyncService.syncTo(target, false);
		context.getSource().sendFeedback(
			() -> Text.translatable("fishka.command.admin.skill_unlocked", target.getDisplayName(), node.id()).formatted(Formatting.GREEN),
			true
		);
		return Command.SINGLE_SUCCESS;
	}

	private static int showSkillPoints(CommandContext<ServerCommandSource> context, ServerPlayerEntity target) {
		FishingDataState.FishingProfileData data = FishingProgressionService.getProfileData(target);
		int available = FishingSkillService.availableSkillPoints(data);
		context.getSource().sendFeedback(
			() -> Text.translatable("fishka.command.admin.skill_points", target.getDisplayName(), available, data.skillPointsSpent()).formatted(Formatting.AQUA),
			false
		);
		return Command.SINGLE_SUCCESS;
	}

	private static int giveModule(
		CommandContext<ServerCommandSource> context,
		ServerPlayerEntity target,
		String rawSlot,
		String rawTier,
		int count
	) {
		RodModuleSlot slot = RodModuleSlot.fromToken(rawSlot);
		if (slot == null) {
			context.getSource().sendError(Text.translatable("fishka.command.admin.module_unknown_slot", rawSlot));
			return 0;
		}
		FishEncounterTier tier = RodModuleCatalog.parseTier(rawTier);
		if (tier == null) {
			context.getSource().sendError(Text.translatable("fishka.command.admin.module_unknown_tier", rawTier));
			return 0;
		}
		Item item = ModItems.getRodModuleItem(slot, tier);
		if (item == null) {
			context.getSource().sendError(Text.translatable("fishka.command.admin.module_unknown_slot", rawSlot));
			return 0;
		}

		int given = 0;
		for (int i = 0; i < count; i++) {
			ItemStack stack = new ItemStack(item);
			if (!target.giveItemStack(stack)) {
				target.dropItem(stack, false);
			}
			given++;
		}

		Text moduleName = new ItemStack(item).getName();
		int finalGiven = given;
		context.getSource().sendFeedback(
			() -> Text.translatable("fishka.command.admin.module_given", target.getDisplayName(), moduleName, finalGiven).formatted(Formatting.GREEN),
			true
		);
		return Command.SINGLE_SUCCESS;
	}

	private static int setHeldModule(
		CommandContext<ServerCommandSource> context,
		ServerPlayerEntity target,
		String rawSlot,
		String rawTier
	) {
		RodModuleSlot slot = RodModuleSlot.fromToken(rawSlot);
		if (slot == null) {
			context.getSource().sendError(Text.translatable("fishka.command.admin.module_unknown_slot", rawSlot));
			return 0;
		}
		FishEncounterTier tier = RodModuleCatalog.parseTier(rawTier);
		if (tier == null) {
			context.getSource().sendError(Text.translatable("fishka.command.admin.module_unknown_tier", rawTier));
			return 0;
		}
		Item item = ModItems.getRodModuleItem(slot, tier);
		if (item == null) {
			context.getSource().sendError(Text.translatable("fishka.command.admin.module_unknown_slot", rawSlot));
			return 0;
		}

		ItemStack held = target.getMainHandStack();
		if (!RodModuleState.supports(held)) {
			context.getSource().sendError(Text.translatable("fishka.command.admin.module_no_rod", target.getDisplayName()));
			return 0;
		}

		RodModuleState.setModule(held, slot, new ItemStack(item));
		target.getInventory().markDirty();
		target.playerScreenHandler.sendContentUpdates();
		context.getSource().sendFeedback(
			() -> Text.translatable("fishka.command.admin.module_setheld", target.getDisplayName(), new ItemStack(item).getName()).formatted(Formatting.GREEN),
			true
		);
		return Command.SINGLE_SUCCESS;
	}

	private static int clearHeldModules(CommandContext<ServerCommandSource> context, ServerPlayerEntity target) {
		ItemStack held = target.getMainHandStack();
		if (!RodModuleState.supports(held)) {
			context.getSource().sendError(Text.translatable("fishka.command.admin.module_no_rod", target.getDisplayName()));
			return 0;
		}
		RodModuleState.clearAll(held);
		target.getInventory().markDirty();
		target.playerScreenHandler.sendContentUpdates();
		context.getSource().sendFeedback(
			() -> Text.translatable("fishka.command.admin.module_cleared", target.getDisplayName()).formatted(Formatting.GREEN),
			true
		);
		return Command.SINGLE_SUCCESS;
	}

	private static FishingSkillNode parseNode(String raw) {
		if (raw == null) {
			return null;
		}
		String normalized = raw.trim().toLowerCase();
		if ("unlock_fisher_crafting".equals(normalized) || "crafting".equals(normalized) || "0".equals(normalized)) {
			return FishingSkillNode.UNLOCK_FISHER_CRAFTING;
		}
		return null;
	}
}

package feuda.fishka.fishing;

import feuda.fishka.fishing.net.FishkaCatchFeedbackS2CPayload;
import feuda.fishka.fishing.net.FishkaMinigameInputC2SPayload;
import feuda.fishka.fishing.net.FishkaMinigameLifecycleS2CPayload;
import feuda.fishka.fishing.net.FishkaMinigameStateS2CPayload;
import feuda.fishka.mixin.FishingBobberEntityBiteAccessor;
import feuda.fishka.mixin.FishingBobberEntityCountdownAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class FishingMiniGameManager {
	private static final Map<UUID, Long> LAST_CAST_TICK = new ConcurrentHashMap<>();
	private static final Map<UUID, WaitingSession> WAITING_SESSIONS = new ConcurrentHashMap<>();
	private static final Map<UUID, ActiveSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
	private static final AtomicInteger NEXT_SESSION_ID = new AtomicInteger(1);

	private FishingMiniGameManager() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(FishingMiniGameManager::onServerTick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID playerId = handler.player.getUuid();
			LAST_CAST_TICK.remove(playerId);
			WAITING_SESSIONS.remove(playerId);
			ACTIVE_SESSIONS.remove(playerId);
		});
	}

	public static void onCast(ServerPlayerEntity player, Hand hand) {
		long now = player.getServer().getTicks();
		LAST_CAST_TICK.put(player.getUuid(), now);
		WAITING_SESSIONS.put(player.getUuid(), new WaitingSession(hand, now, resolveBiteSpeedBonus(player, hand)));
		ACTIVE_SESSIONS.remove(player.getUuid());
	}

	public static long ticksSinceCast(ServerPlayerEntity player) {
		long now = player.getServer().getTicks();
		long castTick = LAST_CAST_TICK.getOrDefault(player.getUuid(), now);
		return Math.max(0L, now - castTick);
	}

	public static boolean hasActiveSession(ServerPlayerEntity player) {
		return ACTIVE_SESSIONS.containsKey(player.getUuid());
	}

	public static boolean hasWaitingSession(ServerPlayerEntity player) {
		return WAITING_SESSIONS.containsKey(player.getUuid());
	}

	public static boolean startIfPossible(ServerPlayerEntity player, Hand hand, FishingBobberEntity bobber) {
		if (ACTIVE_SESSIONS.containsKey(player.getUuid())) {
			return false;
		}
		if (WAITING_SESSIONS.containsKey(player.getUuid())) {
			return true;
		}
		if (bobber == null || bobber.isRemoved() || !bobber.isTouchingWater()) {
			return false;
		}
		WAITING_SESSIONS.put(
			player.getUuid(),
			new WaitingSession(hand, player.getServer().getTicks(), resolveBiteSpeedBonus(player, hand))
		);
		return true;
	}

	public static void handleInputPacket(ServerPlayerEntity player, FishkaMinigameInputC2SPayload payload) {
		ActiveSession session = ACTIVE_SESSIONS.get(player.getUuid());
		if (session == null || session.sessionId != payload.sessionId()) {
			return;
		}
		boolean holdingNow = payload.action() == FishkaMinigameInputC2SPayload.Action.START_HOLD;
		if (holdingNow == session.holding) {
			return;
		}

		int debounceTicks = Math.max(0, FishingConfig.get().miniGameBalance.inputToggleDebounceTicks);
		long now = player.getServer().getTicks();
		// Never debounce STOP_HOLD to avoid stuck holding state on packet timing races.
		if (holdingNow && debounceTicks > 0 && now - session.lastInputToggleTick < debounceTicks) {
			return;
		}

		session.holding = holdingNow;
		session.lastInputToggleTick = now;
	}

	public static void cancelSession(ServerPlayerEntity player, FishkaMinigameLifecycleS2CPayload.Reason reason) {
		UUID playerId = player.getUuid();
		WAITING_SESSIONS.remove(playerId);
		ActiveSession active = ACTIVE_SESSIONS.remove(playerId);
		if (active == null) {
			return;
		}
		sendLifecycle(player, active.sessionId, FishkaMinigameLifecycleS2CPayload.Event.CANCEL, reason);
		sendInactiveState(player, active.sessionId);
	}

	private static void onServerTick(MinecraftServer server) {
		long now = server.getTicks();
		tickWaitingSessions(server, now);
		tickActiveSessions(server, now);
	}

	private static void tickWaitingSessions(MinecraftServer server, long now) {
		int minWait = Math.max(0, FishingConfig.get().miniGame.minWaitAfterCastTicks);

		for (UUID playerId : WAITING_SESSIONS.keySet().toArray(new UUID[0])) {
			WaitingSession waiting = WAITING_SESSIONS.get(playerId);
			if (waiting == null) {
				continue;
			}

			ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
			if (player == null) {
				WAITING_SESSIONS.remove(playerId);
				continue;
			}

			FishingBobberEntity bobber = player.fishHook;
			if (bobber == null || bobber.isRemoved()) {
				WAITING_SESSIONS.remove(playerId);
				continue;
			}
			if (!bobber.isTouchingWater()) {
				continue;
			}
			if (FishingConfig.get().rodModules.biteCountdownAccelerationEnabled && waiting.biteSpeedBonus() > 0.0f) {
				accelerateBiteCountdowns(bobber, waiting.biteSpeedBonus());
			}
			int effectiveMinWait = effectiveMinWaitTicks(minWait, waiting.biteSpeedBonus());
			if (now - waiting.castTick() < effectiveMinWait) {
				continue;
			}
			if (!hasCaughtFish(bobber)) {
				continue;
			}

			ItemStack rodStack = player.getStackInHand(waiting.hand());
			if (!(rodStack.getItem() instanceof FishingRodItem)) {
				rodStack = player.getMainHandStack();
			}
			FishingLootService.EncounterPlan encounterPlan = FishingLootService.prepareEncounter(
				player,
				bobber,
				rodStack,
				waiting.hand()
			);
			startActiveSession(player, waiting, encounterPlan, now);
			WAITING_SESSIONS.remove(playerId);
		}
	}

	private static void tickActiveSessions(MinecraftServer server, long now) {
		for (ActiveSession session : ACTIVE_SESSIONS.values().toArray(new ActiveSession[0])) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(session.playerId);
			if (player == null) {
				ACTIVE_SESSIONS.remove(session.playerId);
				continue;
			}

			FishingBobberEntity bobber = player.fishHook;
			if (!isValidDuelState(player, bobber)) {
				failSession(player, session, FishkaMinigameLifecycleS2CPayload.Reason.INVALID_STATE);
				continue;
			}

			session.elapsedTicks++;
			updateBurstState(session, player.getRandom());
			updateFishTarget(session, player.getRandom());
			updateSafeZone(session);
			updateSessionPhysics(session);
			updateProgress(session);
			updateBobberMotion(player, bobber, session);
			if (session.softStartTicksLeft > 0) {
				session.softStartTicksLeft--;
			}

			int timeLeftTicks = Math.max(0, session.durationTicks - session.elapsedTicks);
			if (session.catchProgress01 >= 1.0f) {
				successSession(player, bobber, session);
				continue;
			}
			if (session.catchProgress01 <= 0.0f) {
				failSession(player, session, FishkaMinigameLifecycleS2CPayload.Reason.TENSION_COLLAPSE);
				continue;
			}

			if (now - session.lastSyncTick >= session.syncIntervalTicks) {
				sendState(player, session, timeLeftTicks, true);
				session.lastSyncTick = now;
			}
		}
	}

	private static void startActiveSession(
		ServerPlayerEntity player,
		WaitingSession waiting,
		FishingLootService.EncounterPlan encounterPlan,
		long now
	) {
		ItemStack rodStack = player.getStackInHand(waiting.hand());
		if (!(rodStack.getItem() instanceof FishingRodItem)) {
			rodStack = player.getMainHandStack();
		}

		FishingBalanceProfile balance = FishingBalanceProfile.forEncounter(player, waiting.hand(), rodStack, encounterPlan);
		int sessionId = NEXT_SESSION_ID.getAndIncrement();
		ActiveSession session = new ActiveSession(player.getUuid(), waiting.hand(), sessionId, balance, encounterPlan);

		float minCenter = session.safeZoneHalfWidth01 + 0.05f;
		float maxCenter = 1.0f - session.safeZoneHalfWidth01 - 0.05f;
		float initialCenter = clamp(0.5f + (player.getRandom().nextFloat() - 0.5f) * 0.20f, minCenter, maxCenter);
		session.safeZoneCenter01 = initialCenter;
		session.tension01 = initialCenter;
		session.fishTarget01 = initialCenter;
		session.fishForce01 = initialCenter;
		session.fishTargetVelocity = (player.getRandom().nextFloat() - 0.5f) * 0.01f;
		session.catchProgress01 = 0.34f;
		session.softStartTicksLeft = balance.softStartTicks();
		session.lastSyncTick = now;
		session.lastInputToggleTick = now;
		session.nextChaosImpulseTick = 3;
		FishingBobberEntity bobber = player.fishHook;
		if (bobber != null && !bobber.isRemoved()) {
			Vec3d fromPlayer = bobber.getPos().subtract(player.getPos());
			double horizontal = Math.sqrt(fromPlayer.x * fromPlayer.x + fromPlayer.z * fromPlayer.z);
			session.castDistance = (float) MathHelper.clamp(horizontal, 4.0, 20.0);
			session.surfaceY = (float) bobber.getY();
			session.bobberWavePhase = player.getRandom().nextFloat() * 6.28318f;
		}
		planBursts(session, player.getRandom());

		ACTIVE_SESSIONS.put(player.getUuid(), session);
		sendLifecycle(player, session.sessionId, FishkaMinigameLifecycleS2CPayload.Event.START, FishkaMinigameLifecycleS2CPayload.Reason.NONE);
		sendState(player, session, session.durationTicks, true);
	}

	private static boolean isValidDuelState(ServerPlayerEntity player, FishingBobberEntity bobber) {
		if (bobber == null || bobber.isRemoved()) {
			return false;
		}
		if (!player.isAlive() || player.isRemoved()) {
			return false;
		}
		if (bobber.squaredDistanceTo(player) > 1024.0) {
			return false;
		}
		ItemStack main = player.getMainHandStack();
		ItemStack off = player.getOffHandStack();
		return main.getItem() instanceof FishingRodItem || off.getItem() instanceof FishingRodItem;
	}

	private static void planBursts(ActiveSession session, Random random) {
		if (!session.balance.burstEnabled() || session.balance.burstCount() <= 0) {
			session.plannedBurstStarts = new int[0];
			return;
		}

		int count = session.balance.burstCount();
		int[] starts = new int[count];
		int minTick = Math.max(1, Math.round(session.durationTicks * Math.min(session.balance.burstStartMin01(), session.balance.burstStartMax01())));
		int maxTick = Math.max(minTick + 1, Math.round(session.durationTicks * Math.max(session.balance.burstStartMin01(), session.balance.burstStartMax01())));

		if (count == 1) {
			starts[0] = randomBetween(random, minTick, maxTick);
		} else {
			for (int i = 0; i < count; i++) {
				float ratio = i / (float) (count - 1);
				int base = Math.round(minTick + ratio * (maxTick - minTick));
				int jitter = Math.max(2, session.durationTicks / 40);
				starts[i] = clampInt(base + randomBetween(random, -jitter, jitter), minTick, maxTick);
			}
			Arrays.sort(starts);
			int minGap = 10;
			for (int i = 1; i < starts.length; i++) {
				if (starts[i] <= starts[i - 1] + minGap) {
					starts[i] = Math.min(maxTick, starts[i - 1] + minGap);
				}
			}
		}

		session.plannedBurstStarts = starts;
	}

	private static void updateBurstState(ActiveSession session, Random random) {
		if (session.burstActive) {
			if (session.elapsedTicks >= session.burstEndTick) {
				session.burstActive = false;
				return;
			}
			if (session.elapsedTicks < session.nextBurstImpulseTick) {
				return;
			}
			if (random.nextFloat() < 0.45f) {
				session.burstDirection *= -1.0f;
			}
			float impulse = session.balance.burstImpulseStrength() * (0.80f + random.nextFloat() * 0.50f) * session.burstDirection;
			session.fishTargetVelocity += impulse;
			session.fishTarget01 = clamp01(session.fishTarget01 + impulse * 0.55f);
			session.nextBurstImpulseTick = session.elapsedTicks + randomBetween(
				random,
				session.balance.burstImpulseEveryMinTicks(),
				session.balance.burstImpulseEveryMaxTicks()
			);
			return;
		}

		if (session.nextBurstIndex >= session.plannedBurstStarts.length) {
			return;
		}
		if (session.elapsedTicks < session.plannedBurstStarts[session.nextBurstIndex]) {
			return;
		}

		session.burstActive = true;
		session.nextBurstIndex++;
		session.burstDirection = random.nextBoolean() ? 1.0f : -1.0f;
		int duration = randomBetween(random, session.balance.burstDurationMinTicks(), session.balance.burstDurationMaxTicks());
		session.burstEndTick = session.elapsedTicks + duration;
		session.nextBurstImpulseTick = session.elapsedTicks;
	}

	private static void updateFishTarget(ActiveSession session, Random random) {
		float forceScale = softStartScale(session.balance.softStartFishForceStartScale(), session.softStartTicksLeft, session.balance.softStartTicks());
		if (session.elapsedTicks >= session.nextChaosImpulseTick) {
			float chaosImpulse = chaosImpulseForTier(session.balance.tier(), session.burstActive);
			session.fishTargetVelocity += (random.nextFloat() - 0.5f) * 2.0f * chaosImpulse * forceScale;
			session.nextChaosImpulseTick = session.elapsedTicks + randomBetween(
				random,
				chaosIntervalMinForTier(session.balance.tier(), session.burstActive),
				chaosIntervalMaxForTier(session.balance.tier(), session.burstActive)
			);
		}

		float noiseAmp = session.burstActive ? 3.4f : 2.4f;
		float noise = (random.nextFloat() - 0.5f) * (session.balance.fishForcePerTick() * noiseAmp) * forceScale;
		float damping = fishVelocityDampingForTier(session.balance.tier(), session.burstActive);
		session.fishTargetVelocity = (session.fishTargetVelocity + noise) * damping;
		float maxVel = maxFishTargetVelocityForTier(session.balance.tier(), session.burstActive);
		session.fishTargetVelocity = clamp(session.fishTargetVelocity, -maxVel, maxVel);
		session.fishTarget01 += session.fishTargetVelocity;

		if (session.fishTarget01 < 0.02f) {
			session.fishTarget01 = 0.02f;
			session.fishTargetVelocity *= -0.55f;
		} else if (session.fishTarget01 > 0.98f) {
			session.fishTarget01 = 0.98f;
			session.fishTargetVelocity *= -0.55f;
		}

		session.fishForce01 = session.fishTarget01;
	}

	private static void updateSafeZone(ActiveSession session) {
		float minCenter = session.safeZoneHalfWidth01 + 0.02f;
		float maxCenter = 1.0f - session.safeZoneHalfWidth01 - 0.02f;
		float maxStep = session.balance.safeZoneMoveSpeedPerTick() * (session.burstActive ? 1.55f : 1.0f);
		float deltaRaw = (session.fishTarget01 - session.safeZoneCenter01) * 0.92f + session.fishTargetVelocity * 0.52f;
		float delta = clamp(deltaRaw, -maxStep, maxStep);
		session.safeZoneCenter01 = clamp(session.safeZoneCenter01 + delta, minCenter, maxCenter);
	}

	private static void updateSessionPhysics(ActiveSession session) {
		float releaseScale = softStartScale(
			session.balance.softStartReleasePenaltyStartScale(),
			session.softStartTicksLeft,
			session.balance.softStartTicks()
		);

		float playerAccel = session.holding
			? session.balance.holdAccelerationPerTick()
			: -session.balance.releaseAccelerationPerTick() * releaseScale;
		if (!session.holding) {
			// Always bias toward the left when player does not hold RMB.
			playerAccel -= Math.max(0.0015f, session.balance.releaseAccelerationPerTick() * 0.42f);
		}

		float maxVelocity = session.balance.maxTensionVelocityPerTick();
		float targetVelocity = (session.tensionVelocity + playerAccel) * session.balance.tensionDamping();
		targetVelocity = clamp(targetVelocity, -maxVelocity, maxVelocity);
		session.tensionVelocity = moveToward(session.tensionVelocity, targetVelocity, session.balance.velocitySlewPerTick());
		if (!session.holding && session.tensionVelocity > -0.002f) {
			session.tensionVelocity -= Math.max(0.0010f, session.balance.releaseAccelerationPerTick() * 0.28f);
		}

		session.tension01 += session.tensionVelocity;

		if (session.tension01 < 0.0f) {
			session.tension01 = 0.0f;
			session.tensionVelocity *= -session.balance.tensionBounceFactor();
		} else if (session.tension01 > 1.0f) {
			session.tension01 = 1.0f;
			session.tensionVelocity *= -session.balance.tensionBounceFactor();
		}
	}

	private static void updateProgress(ActiveSession session) {
		boolean inZone = Math.abs(session.tension01 - session.safeZoneCenter01) <= session.safeZoneHalfWidth01;
		float outZoneScale = softStartScale(
			session.balance.softStartOutZoneLossStartScale(),
			session.softStartTicksLeft,
			session.balance.softStartTicks()
		);

		float delta = inZone
			? session.balance.progressGainInZonePerTick()
			: -session.balance.progressLossOutZonePerTick() * outZoneScale;
		session.catchProgress01 = clamp01(session.catchProgress01 + delta);
	}

	private static void updateBobberMotion(ServerPlayerEntity player, FishingBobberEntity bobber, ActiveSession session) {
		Vec3d playerPos = player.getPos().add(0.0, 0.15, 0.0);
		Vec3d bobberPos = bobber.getPos();

		Vec3d radial = bobberPos.subtract(playerPos);
		Vec3d radialFlat = new Vec3d(radial.x, 0.0, radial.z);
		if (radialFlat.lengthSquared() < 1.0e-6) {
			float yaw = player.getYaw() * 0.017453292f;
			radialFlat = new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw));
		}
		radialFlat = radialFlat.normalize();
		Vec3d tangent = new Vec3d(-radialFlat.z, 0.0, radialFlat.x);

		float progress01 = clamp01(session.catchProgress01);
		float desiredDistance = MathHelper.lerp(progress01, session.castDistance, Math.max(2.4f, session.castDistance * 0.26f));
		float fishOffset = (session.fishTarget01 - 0.5f) * 2.0f;
		float lateral = fishOffset * (0.95f + session.balance.fishForcePerTick() * 26.0f) * (1.0f - progress01 * 0.45f);

		float waveAmp = session.burstActive ? 0.060f : 0.034f;
		float wave = (float) Math.sin(session.elapsedTicks * 0.37f + session.bobberWavePhase) * waveAmp;
		float targetY = session.surfaceY + wave;

		Vec3d target = playerPos
			.add(radialFlat.multiply(desiredDistance))
			.add(tangent.multiply(lateral))
			.add(0.0, targetY - playerPos.y, 0.0);

		Vec3d pull = target.subtract(bobberPos);
		Vec3d pullFlat = new Vec3d(pull.x, 0.0, pull.z);
		double gain = session.burstActive ? 0.13 : 0.10;
		Vec3d desiredFlatVelocity = pullFlat.multiply(gain);
		Vec3d current = bobber.getVelocity();
		Vec3d horizontalVelocity = new Vec3d(current.x, 0.0, current.z).multiply(0.78).add(desiredFlatVelocity);
		double maxSpeed = session.burstActive ? 0.20 : 0.14;
		if (horizontalVelocity.lengthSquared() > maxSpeed * maxSpeed) {
			horizontalVelocity = horizontalVelocity.normalize().multiply(maxSpeed);
		}

		double verticalVelocity = MathHelper.clamp((targetY - bobberPos.y) * 0.24 - current.y * 0.40, -0.05, 0.05);
		Vec3d steered = steerAroundObstacles(bobber, new Vec3d(horizontalVelocity.x, verticalVelocity, horizontalVelocity.z), session);
		bobber.setVelocity(steered);
	}

	private static Vec3d steerAroundObstacles(FishingBobberEntity bobber, Vec3d desiredVelocity, ActiveSession session) {
		Vec3d horizontal = new Vec3d(desiredVelocity.x, 0.0, desiredVelocity.z);
		if (horizontal.lengthSquared() < 1.0e-6) {
			return desiredVelocity;
		}

		Vec3d dir = horizontal.normalize();
		Vec3d start = bobber.getPos().add(0.0, 0.10, 0.0);
		double probeDistance = Math.max(0.45, Math.min(1.25, horizontal.length() * 7.5));
		Vec3d end = start.add(dir.multiply(probeDistance));
		HitResult hit = bobber.getWorld().raycast(new RaycastContext(
			start,
			end,
			RaycastContext.ShapeType.COLLIDER,
			RaycastContext.FluidHandling.NONE,
			bobber
		));

		if (hit.getType() != HitResult.Type.BLOCK) {
			session.avoidTicks = 0;
			return desiredVelocity;
		}

		if (session.avoidTicks <= 0) {
			session.avoidDirection = session.fishTargetVelocity >= 0.0f ? 1 : -1;
			session.avoidTicks = 8;
		} else {
			session.avoidTicks--;
			if ((session.avoidTicks % 4) == 0) {
				session.avoidDirection *= -1;
			}
		}

		Vec3d perp = session.avoidDirection >= 0
			? new Vec3d(-dir.z, 0.0, dir.x)
			: new Vec3d(dir.z, 0.0, -dir.x);
		Vec3d detour = perp.multiply(Math.max(0.08, horizontal.length() * 0.95));
		Vec3d adjustedHorizontal = horizontal.multiply(0.42).add(detour);
		double maxDetourSpeed = 0.16;
		if (adjustedHorizontal.lengthSquared() > maxDetourSpeed * maxDetourSpeed) {
			adjustedHorizontal = adjustedHorizontal.normalize().multiply(maxDetourSpeed);
		}

		return new Vec3d(adjustedHorizontal.x, desiredVelocity.y, adjustedHorizontal.z);
	}

	private static void successSession(ServerPlayerEntity player, FishingBobberEntity bobber, ActiveSession session) {
		ACTIVE_SESSIONS.remove(session.playerId);
		sendLifecycle(player, session.sessionId, FishkaMinigameLifecycleS2CPayload.Event.SUCCESS, FishkaMinigameLifecycleS2CPayload.Reason.NONE);
		sendInactiveState(player, session.sessionId);

		bobber.discard();

		FishingLootService.CatchResult catchResult = FishingLootService.resolveCatch(player, session.encounterPlan);
		ItemStack rewardStack = catchResult.itemStack().copy();
		ItemStack displayStack = rewardStack.copy();
		boolean inserted = player.giveItemStack(rewardStack);
		if (!inserted && !rewardStack.isEmpty()) {
			player.dropItem(rewardStack, false);
		}

		FishingProgressionService.ProgressResult progress = FishingProgressionService.addCatchXp(player, catchResult.xp());
		int newLevel = progress.levelUps() > 0 ? progress.profile().level() : -1;
		FishingProfileSyncService.syncTo(player, progress.levelUps() > 0);
		FishingConfig.RewardHudConfig rewardCfg = FishingConfig.get().rewardHud;
		int displayTicks = Math.max(20, rewardCfg.displayTicks);
		int revealDelayTicks = Math.max(0, rewardCfg.revealDelayTicks);

		ServerPlayNetworking.send(
			player,
			new FishkaCatchFeedbackS2CPayload(
				displayStack,
				catchResult.title(),
				catchResult.statLine(),
				catchResult.xp(),
				newLevel,
				displayTicks,
				catchResult.tier(),
				revealDelayTicks,
				catchResult.showRarityRays()
			)
		);
	}

	private static void failSession(
		ServerPlayerEntity player,
		ActiveSession session,
		FishkaMinigameLifecycleS2CPayload.Reason reason
	) {
		ACTIVE_SESSIONS.remove(session.playerId);
		sendLifecycle(player, session.sessionId, FishkaMinigameLifecycleS2CPayload.Event.FAIL, reason);
		sendInactiveState(player, session.sessionId);

		FishingBobberEntity bobber = player.fishHook;
		if (bobber != null && !bobber.isRemoved()) {
			bobber.discard();
		}
	}

	private static boolean hasCaughtFish(FishingBobberEntity bobber) {
		return ((FishingBobberEntityBiteAccessor) bobber).fishka$hasCaughtFish();
	}

	private static float resolveBiteSpeedBonus(ServerPlayerEntity player, Hand hand) {
		ItemStack rodStack = player.getStackInHand(hand);
		return RodComponentBonusProvider.resolve(player, hand, rodStack).biteSpeedBonus();
	}

	private static int effectiveMinWaitTicks(int baseMinWait, float biteSpeedBonus) {
		float clampedBonus = clamp01(biteSpeedBonus);
		float multiplier = 1.0f - clampedBonus * 0.65f;
		return Math.max(0, Math.round(baseMinWait * multiplier));
	}

	private static void accelerateBiteCountdowns(FishingBobberEntity bobber, float biteSpeedBonus) {
		if (!(bobber instanceof FishingBobberEntityCountdownAccessor accessor)) {
			return;
		}
		int extra = Math.max(0, Math.round(clamp01(biteSpeedBonus) * 2.25f));
		if (extra <= 0) {
			return;
		}
		int wait = accessor.fishka$getWaitCountdown();
		if (wait > 0) {
			accessor.fishka$setWaitCountdown(Math.max(0, wait - extra));
		}
		int travel = accessor.fishka$getFishTravelCountdown();
		if (travel > 0) {
			accessor.fishka$setFishTravelCountdown(Math.max(0, travel - extra));
		}
		int hook = accessor.fishka$getHookCountdown();
		if (hook > 0) {
			accessor.fishka$setHookCountdown(Math.max(0, hook - extra));
		}
	}

	private static void sendState(ServerPlayerEntity player, ActiveSession session, int timeLeftTicks, boolean active) {
		ServerPlayNetworking.send(
			player,
			new FishkaMinigameStateS2CPayload(
				session.sessionId,
				active,
				session.catchProgress01,
				session.tension01,
				normalizeVelocity(session.tensionVelocity),
				session.safeZoneCenter01,
				session.safeZoneHalfWidth01,
				session.fishForce01,
				timeLeftTicks,
				session.softStartTicksLeft,
				session.burstActive,
				session.balance.tier()
			)
		);
	}

	private static void sendInactiveState(ServerPlayerEntity player, int sessionId) {
		ServerPlayNetworking.send(
			player,
			new FishkaMinigameStateS2CPayload(
				sessionId,
				false,
				0.0f,
				0.0f,
				0.5f,
				0.5f,
				0.1f,
				0.5f,
				0,
				0,
				false,
				FishEncounterTier.COMMON
			)
		);
	}

	private static void sendLifecycle(
		ServerPlayerEntity player,
		int sessionId,
		FishkaMinigameLifecycleS2CPayload.Event event,
		FishkaMinigameLifecycleS2CPayload.Reason reason
	) {
		ServerPlayNetworking.send(player, new FishkaMinigameLifecycleS2CPayload(sessionId, event, reason));
	}

	private static float softStartScale(float startScale, int softStartTicksLeft, int softStartTicks) {
		if (softStartTicks <= 0 || softStartTicksLeft <= 0) {
			return 1.0f;
		}
		float progress = 1.0f - (softStartTicksLeft / (float) softStartTicks);
		return lerp(startScale, 1.0f, clamp01(progress));
	}

	private static float normalizeVelocity(float velocity) {
		return clamp01((velocity + 0.16f) / 0.32f);
	}

	private static int randomBetween(Random random, int min, int max) {
		if (max <= min) {
			return min;
		}
		return min + random.nextInt(max - min + 1);
	}

	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float clamp01(float value) {
		return clamp(value, 0.0f, 1.0f);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float lerp(float start, float end, float alpha) {
		return start + (end - start) * alpha;
	}

	private static float moveToward(float current, float target, float maxDelta) {
		if (Math.abs(target - current) <= maxDelta) {
			return target;
		}
		return current + Math.copySign(maxDelta, target - current);
	}

	private static float chaosImpulseForTier(FishEncounterTier tier, boolean burst) {
		float base = switch (tier) {
			case COMMON -> 0.012f;
			case UNCOMMON -> 0.016f;
			case RARE -> 0.023f;
			case EPIC -> 0.031f;
			case LEGENDARY -> 0.040f;
		};
		return burst ? base * 1.60f : base;
	}

	private static float maxFishTargetVelocityForTier(FishEncounterTier tier, boolean burst) {
		float base = switch (tier) {
			case COMMON -> 0.048f;
			case UNCOMMON -> 0.064f;
			case RARE -> 0.085f;
			case EPIC -> 0.108f;
			case LEGENDARY -> 0.132f;
		};
		return burst ? base * 1.42f : base;
	}

	private static int chaosIntervalMinForTier(FishEncounterTier tier, boolean burst) {
		int base = switch (tier) {
			case COMMON -> 9;
			case UNCOMMON -> 7;
			case RARE -> 5;
			case EPIC -> 4;
			case LEGENDARY -> 3;
		};
		return burst ? Math.max(2, base - 2) : base;
	}

	private static int chaosIntervalMaxForTier(FishEncounterTier tier, boolean burst) {
		int base = switch (tier) {
			case COMMON -> 12;
			case UNCOMMON -> 10;
			case RARE -> 7;
			case EPIC -> 6;
			case LEGENDARY -> 5;
		};
		return burst ? Math.max(3, base - 2) : base;
	}

	private static float fishVelocityDampingForTier(FishEncounterTier tier, boolean burst) {
		float base = switch (tier) {
			case COMMON -> 0.91f;
			case UNCOMMON -> 0.90f;
			case RARE -> 0.885f;
			case EPIC -> 0.865f;
			case LEGENDARY -> 0.84f;
		};
		return burst ? Math.max(0.76f, base - 0.045f) : base;
	}

	private record WaitingSession(Hand hand, long castTick, float biteSpeedBonus) {
	}

	private static final class ActiveSession {
		private final UUID playerId;
		private final Hand hand;
		private final int sessionId;
		private final FishingBalanceProfile balance;
		private final FishingLootService.EncounterPlan encounterPlan;
		private final int durationTicks;
		private final int syncIntervalTicks;
		private final float safeZoneHalfWidth01;

		private boolean holding;
		private long lastSyncTick;
		private long lastInputToggleTick;
		private int elapsedTicks;
		private int softStartTicksLeft;

		private float catchProgress01;
		private float tension01 = 0.5f;
		private float tensionVelocity;
		private float safeZoneCenter01 = 0.5f;
		private float fishForce01 = 0.5f;
		private float fishTarget01 = 0.5f;
		private float fishTargetVelocity;
		private int nextChaosImpulseTick;
		private float castDistance = 8.0f;
		private float surfaceY;
		private float bobberWavePhase;
		private int avoidDirection = 1;
		private int avoidTicks;

		private boolean burstActive;
		private int burstEndTick;
		private int nextBurstImpulseTick;
		private float burstDirection = 1.0f;
		private int[] plannedBurstStarts = new int[0];
		private int nextBurstIndex;

		private ActiveSession(UUID playerId, Hand hand, int sessionId, FishingBalanceProfile balance, FishingLootService.EncounterPlan encounterPlan) {
			this.playerId = playerId;
			this.hand = hand;
			this.sessionId = sessionId;
			this.balance = balance;
			this.encounterPlan = encounterPlan;
			this.durationTicks = balance.durationTicks();
			this.syncIntervalTicks = balance.stateSyncIntervalTicks();
			this.safeZoneHalfWidth01 = balance.safeZoneHalfWidth();
		}
	}
}

package dev.alist.progressivedifficulty.passive;

import dev.alist.progressivedifficulty.config.ConfigManager;
import dev.alist.progressivedifficulty.config.ModConfig;
import dev.alist.progressivedifficulty.progression.WorldProgressionState;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class PassiveRetaliationManager {
	private static final Map<UUID, Aggression> AGGRESSION = new HashMap<>();
	private static final Set<Animal> GOAL_INSTALLED = Collections.newSetFromMap(new WeakHashMap<>());

	private PassiveRetaliationManager() {
	}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Animal animal && isSupported(animal) && GOAL_INSTALLED.add(animal)) {
				animal.getGoalSelector().addGoal(0, new PassiveCombatGoal(animal));
			}
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (damage > 0.0F && entity instanceof Animal animal && entity.level() instanceof ServerLevel level) {
				alertHerd(level, animal, source);
			}
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof Animal animal && entity.level() instanceof ServerLevel level) {
				alertHerd(level, animal, source);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				AGGRESSION.values().removeIf(aggression -> aggression.expiresAtTick() <= server.getTickCount());
			}
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			AGGRESSION.clear();
			GOAL_INSTALLED.clear();
		});
	}

	public static ServerPlayer targetFor(Animal animal) {
		if (!(animal.level() instanceof ServerLevel level)) {
			return null;
		}

		Aggression aggression = AGGRESSION.get(animal.getUUID());
		MinecraftServer server = level.getServer();
		if (aggression == null || aggression.expiresAtTick() <= server.getTickCount()) {
			AGGRESSION.remove(animal.getUUID());
			return null;
		}

		ServerPlayer player = server.getPlayerList().getPlayer(aggression.playerId());
		if (player == null || !player.isAlive() || player.isCreative() || player.isSpectator()
				|| player.level() != animal.level()) {
			AGGRESSION.remove(animal.getUUID());
			return null;
		}
		return player;
	}

	public static PassiveCombatProfile profileFor(Animal animal) {
		ModConfig.PassiveRetaliation config = ConfigManager.get().passiveRetaliation;
		int tier = animal.level() instanceof ServerLevel level
				? WorldProgressionState.get(level.getServer()).currentTier() : 0;
		if (animal.getType() == EntityTypes.COW) {
			return new PassiveCombatProfile(scaledDamage(config.cow.damage, config.cow.tier8Damage, tier),
					config.cow.speedMultiplier, config.cow.knockback);
		}
		if (animal.getType() == EntityTypes.PIG) {
			return new PassiveCombatProfile(scaledDamage(config.pig.damage, config.pig.tier8Damage, tier),
					config.pig.speedMultiplier, config.pig.knockback);
		}
		return new PassiveCombatProfile(scaledDamage(config.sheep.damage, config.sheep.tier8Damage, tier),
				config.sheep.speedMultiplier, config.sheep.knockback);
	}

	private static double scaledDamage(double tier0Damage, double tier8Damage, int tier) {
		return tier0Damage + (tier8Damage - tier0Damage) * Math.max(0, Math.min(8, tier)) / 8.0;
	}

	public static int attackCooldownTicks() {
		return ConfigManager.get().passiveRetaliation.attackCooldownTicks;
	}

	private static void alertHerd(ServerLevel level, Animal attacked, DamageSource source) {
		ModConfig.PassiveRetaliation config = ConfigManager.get().passiveRetaliation;
		if (!config.enabled || !isEnabled(attacked, config)) {
			return;
		}

		if (!(source.getEntity() instanceof ServerPlayer player)
				|| player.isCreative() || player.isSpectator()) {
			return;
		}

		long expiresAtTick = level.getServer().getTickCount() + config.aggressionSeconds * 20L;
		level.getEntitiesOfClass(
				Animal.class,
				attacked.getBoundingBox().inflate(config.alertRadius),
				candidate -> candidate.getType() == attacked.getType()
						&& candidate.isAlive()
						&& candidate.distanceToSqr(attacked) <= config.alertRadius * config.alertRadius
						&& (config.babiesRetaliate || !candidate.isBaby())
		).forEach(animal -> AGGRESSION.put(animal.getUUID(), new Aggression(player.getUUID(), expiresAtTick)));
	}

	private static boolean isSupported(Animal animal) {
		return animal.getType() == EntityTypes.COW
				|| animal.getType() == EntityTypes.PIG
				|| animal.getType() == EntityTypes.SHEEP;
	}

	private static boolean isEnabled(Animal animal, ModConfig.PassiveRetaliation config) {
		return animal.getType() == EntityTypes.COW && config.cow.enabled
				|| animal.getType() == EntityTypes.PIG && config.pig.enabled
				|| animal.getType() == EntityTypes.SHEEP && config.sheep.enabled;
	}

	private record Aggression(UUID playerId, long expiresAtTick) {
	}
}

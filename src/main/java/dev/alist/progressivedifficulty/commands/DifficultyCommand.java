package dev.alist.progressivedifficulty.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.alist.progressivedifficulty.config.ConfigManager;
import dev.alist.progressivedifficulty.behaviour.HostileBehaviourManager;
import dev.alist.progressivedifficulty.difficulty.AttributeModifierManager;
import dev.alist.progressivedifficulty.difficulty.DifficultyManager;
import dev.alist.progressivedifficulty.difficulty.EquipmentManager;
import dev.alist.progressivedifficulty.progression.ProgressionManager;
import dev.alist.progressivedifficulty.progression.ProgressionTier;
import dev.alist.progressivedifficulty.progression.WorldProgressionState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Comparator;
import java.util.Locale;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class DifficultyCommand {
	private DifficultyCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				literal("pdifficulty")
						.then(literal("status").executes(context -> status(context.getSource())))
						.then(literal("inspect").executes(context -> inspect(context.getSource())))
						.then(literal("settier")
								.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
								.then(argument("tier", IntegerArgumentType.integer(0, 8))
										.executes(context -> setTier(
												context.getSource(),
												IntegerArgumentType.getInteger(context, "tier")
										))))
						.then(literal("reset")
								.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
								.executes(context -> reset(context.getSource())))
		));
	}

	private static int inspect(CommandSourceStack source) {
		ServerLevel level = source.getLevel();
		double radius = 16.0;
		AABB area = new AABB(
				source.getPosition().x - radius,
				source.getPosition().y - radius,
				source.getPosition().z - radius,
				source.getPosition().x + radius,
				source.getPosition().y + radius,
				source.getPosition().z + radius
		);

		LivingEntity target = level.getEntitiesOfClass(
				LivingEntity.class,
				area,
				entity -> entity.isAlive()
						&& DifficultyManager.isHostile(entity)
						&& entity.distanceToSqr(source.getPosition()) <= radius * radius
		).stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(source.getPosition())))
				.orElse(null);

		if (target == null) {
			source.sendFailure(Component.literal("No hostile mob found within 16 blocks."));
			return 0;
		}

		AttributeInstance health = target.getAttribute(Attributes.MAX_HEALTH);
		AttributeInstance damage = target.getAttribute(Attributes.ATTACK_DAMAGE);
		double healthMultiplier = AttributeModifierManager.appliedHealthMultiplier(target);
		double damageMultiplier = AttributeModifierManager.appliedDamageMultiplier(target);

		source.sendSystemMessage(Component.literal("Progressive Difficulty — Mob Inspection")
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		source.sendSystemMessage(Component.literal("Mob: ").append(target.getDisplayName()));
		source.sendSystemMessage(Component.literal(String.format(
				Locale.ROOT,
				"Position: %.1f, %.1f, %.1f",
				target.getX(), target.getY(), target.getZ()
		)));
		source.sendSystemMessage(Component.literal(String.format(
				Locale.ROOT,
				"Health: %.1f / %.1f (base %.1f, PD %.2fx)",
				target.getHealth(), target.getMaxHealth(), health.getBaseValue(), healthMultiplier
		)));

		if (damage == null || Double.isNaN(damageMultiplier)) {
			source.sendSystemMessage(Component.literal("Attack Damage: not used by this mob"));
		} else {
			source.sendSystemMessage(Component.literal(String.format(
					Locale.ROOT,
					"Attack Damage: %.2f (base %.2f, PD %.2fx)",
					damage.getValue(), damage.getBaseValue(), damageMultiplier
			)));
		}

		if (target instanceof Mob mob) {
			double speedMultiplier = HostileBehaviourManager.appliedSpeedMultiplier(mob);
			if (!Double.isNaN(speedMultiplier)) {
				AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
				source.sendSystemMessage(Component.literal(String.format(
						Locale.ROOT, "Movement Speed: %.3f (base %.3f, PD %.2fx)",
						speed.getValue(), speed.getBaseValue(), speedMultiplier
				)));
			}
			source.sendSystemMessage(Component.literal("Tactical Behaviour: "
					+ HostileBehaviourManager.tacticalBehaviour(mob)));
			source.sendSystemMessage(Component.literal("Equipment (PD processed: "
					+ EquipmentManager.wasProcessed(mob) + "):"));
			for (EquipmentSlot slot : new EquipmentSlot[]{
					EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
					EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
			}) {
				ItemStack stack = mob.getItemBySlot(slot);
				if (!stack.isEmpty()) {
					Component line = Component.literal("  " + slot.getName() + ": ").append(stack.getHoverName());
					for (var enchantment : stack.getEnchantments().entrySet()) {
						line = line.copy().append(Component.literal(" ["))
								.append(Enchantment.getFullname(enchantment.getKey(), enchantment.getIntValue()))
								.append(Component.literal("]"));
					}
					source.sendSystemMessage(line);
				}
			}
		}
		return 1;
	}

	private static int status(CommandSourceStack source) {
		WorldProgressionState state = WorldProgressionState.get(source.getServer());
		long worldDay = ProgressionManager.worldDay(source.getServer());

		source.sendSystemMessage(Component.literal("Progressive Difficulty")
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		source.sendSystemMessage(Component.literal("Current Tier: " + state.currentTier()));
		source.sendSystemMessage(Component.literal("Mob Kills: " + state.playerMobKills()));
		source.sendSystemMessage(Component.literal("World Day: " + worldDay));
		source.sendSystemMessage(Component.empty());

		for (int tierId = 1; tierId <= 8; tierId++) {
			ProgressionTier tier = ProgressionTier.byId(tierId);
			String objective = objective(tier);
			String status = milestoneStatus(state, tier);
			ChatFormatting colour = "COMPLETE".equals(status) ? ChatFormatting.GREEN
					: "LOCKED".equals(status) ? ChatFormatting.DARK_GRAY : ChatFormatting.YELLOW;
			source.sendSystemMessage(Component.literal("Milestone " + tierId + " — " + objective + ": ")
					.append(Component.literal(status).withStyle(colour)));
		}

		return 1;
	}

	private static int setTier(CommandSourceStack source, int tier) {
		WorldProgressionState.get(source.getServer()).setTierForTesting(tier);
		source.sendSuccess(() -> Component.literal("Progressive Difficulty set to Tier " + tier + "."), true);
		return tier;
	}

	private static int reset(CommandSourceStack source) {
		WorldProgressionState.get(source.getServer()).reset();
		source.sendSuccess(() -> Component.literal("Progressive Difficulty reset to Tier 0."), true);
		return 1;
	}

	private static String milestoneStatus(WorldProgressionState state, ProgressionTier tier) {
		return state.isUnlocked(tier) ? "COMPLETE" : "INCOMPLETE";
	}

	private static String objective(ProgressionTier tier) {
		return switch (tier) {
			case KILL_TEN_MOBS -> "Kill " + ConfigManager.get().progression.tier1MobKills + " Mobs";
			case WEAR_IRON_ARMOUR -> "Wear One Piece of Iron Armour";
			case ENTER_NETHER -> "Enter Nether";
			case OBTAIN_ANCIENT_DEBRIS -> "Obtain Ancient Debris";
			case KILL_ENDER_DRAGON -> "Kill Ender Dragon";
			case OBTAIN_ELYTRA -> "Obtain Elytra";
			case FULL_NETHERITE_ARMOUR -> "Full Netherite Armour";
			case DAY_ONE_THOUSAND -> "Day " + ConfigManager.get().progression.tier7Day;
			case WORLD_CREATED -> "World Created";
		};
	}
}

package dev.alist.progressivedifficulty.progression;

import dev.alist.progressivedifficulty.config.ConfigManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class MilestoneTracker {
	private static final int CHECK_INTERVAL_TICKS = 20;

	private MilestoneTracker() {
	}

	public static void register() {
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, killer, killed, damageSource) -> {
			ServerPlayer player = killer instanceof ServerPlayer directPlayer
					? directPlayer
					: damageSource.getEntity() instanceof ServerPlayer attributedPlayer ? attributedPlayer : null;
			if (player == null) {
				return;
			}

			MinecraftServer server = level.getServer();
			WorldProgressionState state = WorldProgressionState.get(server);
			int kills = state.incrementMobKills();

			if (kills >= ConfigManager.get().progression.tier1MobKills) {
				ProgressionManager.unlock(server, ProgressionTier.KILL_TEN_MOBS);
			}

			if (killed instanceof EnderDragon) {
				ProgressionManager.unlock(server, ProgressionTier.KILL_ENDER_DRAGON);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
				return;
			}

			checkWorldMilestones(server);
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				checkPlayerMilestones(server, player);
			}
		});
	}

	private static void checkWorldMilestones(MinecraftServer server) {
		WorldProgressionState state = WorldProgressionState.get(server);
		if (state.playerMobKills() >= ConfigManager.get().progression.tier1MobKills) {
			ProgressionManager.unlock(server, ProgressionTier.KILL_TEN_MOBS);
		}

		if (ProgressionManager.worldDay(server) >= ConfigManager.get().progression.tier7Day) {
			ProgressionManager.unlock(server, ProgressionTier.DAY_ONE_THOUSAND);
		}
	}

	private static void checkPlayerMilestones(MinecraftServer server, ServerPlayer player) {
		if (isWearingIron(player)) {
			ProgressionManager.unlock(server, ProgressionTier.WEAR_IRON_ARMOUR);
		}

		if (Level.NETHER.equals(player.level().dimension())) {
			ProgressionManager.unlock(server, ProgressionTier.ENTER_NETHER);
		}

		if (hasInInventory(player, Items.ANCIENT_DEBRIS)) {
			ProgressionManager.unlock(server, ProgressionTier.OBTAIN_ANCIENT_DEBRIS);
		}

		if (hasInInventory(player, Items.ELYTRA)
				|| player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
			ProgressionManager.unlock(server, ProgressionTier.OBTAIN_ELYTRA);
		}

		if (isWearingFullNetherite(player)) {
			ProgressionManager.unlock(server, ProgressionTier.FULL_NETHERITE_ARMOUR);
		}
	}

	private static boolean isWearingIron(ServerPlayer player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).getItem() == Items.IRON_HELMET
				|| player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.IRON_CHESTPLATE
				|| player.getItemBySlot(EquipmentSlot.LEGS).getItem() == Items.IRON_LEGGINGS
				|| player.getItemBySlot(EquipmentSlot.FEET).getItem() == Items.IRON_BOOTS;
	}

	private static boolean hasInInventory(ServerPlayer player, Item item) {
		return player.getInventory().contains(stack -> stack.getItem() == item);
	}

	private static boolean isWearingFullNetherite(ServerPlayer player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).getItem() == Items.NETHERITE_HELMET
				&& player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.NETHERITE_CHESTPLATE
				&& player.getItemBySlot(EquipmentSlot.LEGS).getItem() == Items.NETHERITE_LEGGINGS
				&& player.getItemBySlot(EquipmentSlot.FEET).getItem() == Items.NETHERITE_BOOTS;
	}
}

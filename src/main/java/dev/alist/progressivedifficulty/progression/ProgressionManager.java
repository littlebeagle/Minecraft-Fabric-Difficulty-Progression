package dev.alist.progressivedifficulty.progression;

import dev.alist.progressivedifficulty.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class ProgressionManager {
	private ProgressionManager() {
	}

	public static void unlock(MinecraftServer server, ProgressionTier tier) {
		WorldProgressionState state = WorldProgressionState.get(server);
		if (!state.unlock(tier)) {
			return;
		}

		Component notification = Component.empty()
				.append(Component.literal("Difficulty Increased").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
				.append(Component.literal("\nTier " + state.currentTier() + " — " + milestoneName(tier))
						.withStyle(ChatFormatting.GOLD));

		server.getPlayerList().broadcastSystemMessage(notification, false);
	}

	public static long worldDay(MinecraftServer server) {
		return Math.max(0L, server.overworld().getOverworldClockTime() / 24_000L);
	}

	private static String milestoneName(ProgressionTier tier) {
		return switch (tier) {
			case KILL_TEN_MOBS -> "Kill " + ConfigManager.get().progression.tier1MobKills + " Mobs";
			case DAY_ONE_THOUSAND -> "Day " + ConfigManager.get().progression.tier7Day;
			default -> tier.displayName();
		};
	}
}

package dev.alist.progressivedifficulty.difficulty;

import dev.alist.progressivedifficulty.config.ConfigManager;
import dev.alist.progressivedifficulty.config.ModConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class DimensionModifier {
	private DimensionModifier() {
	}

	public static Contribution in(ServerLevel level) {
		ModConfig.Dimensions config = ConfigManager.get().difficulty.dimensions;
		if (Level.NETHER.equals(level.dimension())) {
			return new Contribution(config.netherHealthBonus, config.netherDamageBonus, "nether");
		}
		if (Level.END.equals(level.dimension())) {
			return new Contribution(config.endHealthBonus, config.endDamageBonus, "end");
		}
		return new Contribution(0.0, 0.0, "overworld");
	}

	public record Contribution(double healthBonus, double damageBonus, String dimension) {
	}
}

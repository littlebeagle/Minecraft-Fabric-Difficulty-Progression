package dev.alist.progressivedifficulty.difficulty;

import dev.alist.progressivedifficulty.config.ConfigManager;
import dev.alist.progressivedifficulty.config.ModConfig;

public final class DepthModifier {
	private DepthModifier() {
	}

	public static Contribution at(double y) {
		ModConfig.Depth config = ConfigManager.get().difficulty.depth;
		if (y > config.normalAboveY) {
			return new Contribution(0.0, 0.0, "normal");
		}
		if (y >= config.moderateBelowY) {
			return new Contribution(config.shallowHealthBonus, config.shallowDamageBonus, "shallow");
		}
		if (y > config.deepAtOrBelowY) {
			return new Contribution(config.moderateHealthBonus, config.moderateDamageBonus, "moderate");
		}
		return new Contribution(config.deepHealthBonus, config.deepDamageBonus, "deep");
	}

	public record Contribution(double healthBonus, double damageBonus, String band) {
	}
}

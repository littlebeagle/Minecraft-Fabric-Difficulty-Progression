package dev.alist.progressivedifficulty.difficulty;

import dev.alist.progressivedifficulty.config.ModConfig;
import dev.alist.progressivedifficulty.config.ConfigManager;
import dev.alist.progressivedifficulty.progression.WorldProgressionState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;

public final class DifficultyManager {
	private DifficultyManager() {
	}

	public static boolean isHostile(LivingEntity entity) {
		return entity.getType().getCategory() == MobCategory.MONSTER;
	}

	public static DifficultyProfile profileFor(ServerLevel level, LivingEntity entity) {
		ModConfig.Difficulty config = ConfigManager.get().difficulty;
		int tier = WorldProgressionState.get(level.getServer()).currentTier();
		DepthModifier.Contribution depth = DepthModifier.at(entity.getY());
		DimensionModifier.Contribution dimension = DimensionModifier.in(level);

		double health = Math.max(0.1,
				config.healthMultipliers[tier] + depth.healthBonus() + dimension.healthBonus());
		double damage = Math.max(0.1,
				config.damageMultipliers[tier] + depth.damageBonus() + dimension.damageBonus());

		return new DifficultyProfile(tier, health, damage, depth.band(), dimension.dimension());
	}
}

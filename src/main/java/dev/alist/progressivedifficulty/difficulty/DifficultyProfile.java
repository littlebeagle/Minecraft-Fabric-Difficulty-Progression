package dev.alist.progressivedifficulty.difficulty;

public record DifficultyProfile(
		int tier,
		double healthMultiplier,
		double damageMultiplier,
		String depthBand,
		String dimension
) {
}

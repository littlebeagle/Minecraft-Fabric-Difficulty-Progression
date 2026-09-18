package dev.alist.progressivedifficulty.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.alist.progressivedifficulty.ProgressiveDifficulty;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class WorldProgressionState extends SavedData {
	private static final Codec<WorldProgressionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("currentTier", 0).forGetter(WorldProgressionState::currentTier),
			Codec.INT.optionalFieldOf("playerMobKills", 0).forGetter(WorldProgressionState::playerMobKills),
			Codec.BOOL.optionalFieldOf("tier1Unlocked", false).forGetter(state -> state.isUnlocked(1)),
			Codec.BOOL.optionalFieldOf("ironArmourUnlocked", false).forGetter(state -> state.isUnlocked(2)),
			// Keep legacy field names tied to their original milestone meanings.
			Codec.BOOL.optionalFieldOf("tier2Unlocked", false).forGetter(state -> state.isUnlocked(3)),
			Codec.BOOL.optionalFieldOf("tier3Unlocked", false).forGetter(state -> state.isUnlocked(4)),
			Codec.BOOL.optionalFieldOf("tier4Unlocked", false).forGetter(state -> state.isUnlocked(5)),
			Codec.BOOL.optionalFieldOf("tier5Unlocked", false).forGetter(state -> state.isUnlocked(6)),
			Codec.BOOL.optionalFieldOf("tier6Unlocked", false).forGetter(state -> state.isUnlocked(7)),
			Codec.BOOL.optionalFieldOf("tier7Unlocked", false).forGetter(state -> state.isUnlocked(8))
	).apply(instance, WorldProgressionState::new));

	private static final SavedDataType<WorldProgressionState> TYPE = new SavedDataType<>(
			ProgressiveDifficulty.id("world_progression"),
			WorldProgressionState::new,
			CODEC,
			null
	);

	private int currentTier;
	private int playerMobKills;
	private final boolean[] unlocked = new boolean[9];

	public WorldProgressionState() {
		unlocked[0] = true;
	}

	private WorldProgressionState(
			int currentTier,
			int playerMobKills,
			boolean tier1Unlocked,
			boolean ironArmourUnlocked,
			boolean tier2Unlocked,
			boolean tier3Unlocked,
			boolean tier4Unlocked,
			boolean tier5Unlocked,
			boolean tier6Unlocked,
			boolean tier7Unlocked
	) {
		this.playerMobKills = Math.max(0, playerMobKills);
		unlocked[0] = true;
		unlocked[1] = tier1Unlocked;
		unlocked[2] = ironArmourUnlocked;
		unlocked[3] = tier2Unlocked;
		unlocked[4] = tier3Unlocked;
		unlocked[5] = tier4Unlocked;
		unlocked[6] = tier5Unlocked;
		unlocked[7] = tier6Unlocked;
		unlocked[8] = tier7Unlocked;
		// Tiers now equal the number of distinct milestones completed. This also
		// migrates order-dependent saves without re-awarding old milestones.
		this.currentTier = completedMilestoneCount();
	}

	public static WorldProgressionState get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public int currentTier() {
		return currentTier;
	}

	public int playerMobKills() {
		return playerMobKills;
	}

	public boolean isUnlocked(ProgressionTier tier) {
		return isUnlocked(tier.id());
	}

	public boolean isUnlocked(int tier) {
		return tier >= 0 && tier < unlocked.length && unlocked[tier];
	}

	public int incrementMobKills() {
		playerMobKills++;
		setDirty();
		return playerMobKills;
	}

	public boolean unlock(ProgressionTier tier) {
		if (unlocked[tier.id()]) {
			return false;
		}

		unlocked[tier.id()] = true;
		currentTier = Math.min(8, currentTier + 1);
		setDirty();
		return true;
	}

	public void setTierForTesting(int tier) {
		int safeTier = Math.max(0, Math.min(8, tier));
		currentTier = safeTier;
		for (int index = 1; index < unlocked.length; index++) {
			unlocked[index] = index <= safeTier;
		}
		setDirty();
	}

	private int completedMilestoneCount() {
		int count = 0;
		for (int index = 1; index < unlocked.length; index++) {
			if (unlocked[index]) count++;
		}
		return count;
	}

	public void reset() {
		currentTier = 0;
		playerMobKills = 0;
		unlocked[0] = true;
		for (int index = 1; index < unlocked.length; index++) {
			unlocked[index] = false;
		}
		setDirty();
	}
}

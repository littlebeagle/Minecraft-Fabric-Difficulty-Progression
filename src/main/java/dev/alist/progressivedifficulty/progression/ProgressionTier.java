package dev.alist.progressivedifficulty.progression;

public enum ProgressionTier {
	WORLD_CREATED(0, "World Created"),
	KILL_TEN_MOBS(1, "Kill 10 Mobs"),
	WEAR_IRON_ARMOUR(2, "Wear Iron Armour"),
	ENTER_NETHER(3, "Enter Nether"),
	OBTAIN_ANCIENT_DEBRIS(4, "Ancient Debris"),
	KILL_ENDER_DRAGON(5, "Kill Ender Dragon"),
	OBTAIN_ELYTRA(6, "Obtain Elytra"),
	FULL_NETHERITE_ARMOUR(7, "Full Netherite Armour"),
	DAY_ONE_THOUSAND(8, "Day 1000");

	private final int id;
	private final String displayName;

	ProgressionTier(int id, String displayName) {
		this.id = id;
		this.displayName = displayName;
	}

	public int id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public static ProgressionTier byId(int id) {
		int safeId = Math.max(0, Math.min(8, id));
		return values()[safeId];
	}
}

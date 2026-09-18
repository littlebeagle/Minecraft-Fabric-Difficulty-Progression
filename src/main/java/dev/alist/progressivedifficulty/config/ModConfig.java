package dev.alist.progressivedifficulty.config;

public final class ModConfig {
	public Progression progression = new Progression();
	public Difficulty difficulty = new Difficulty();
	public Equipment equipment = new Equipment();
	public Behaviours behaviours = new Behaviours();
	public PassiveRetaliation passiveRetaliation = new PassiveRetaliation();

	public void validate() {
		if (progression == null) {
			progression = new Progression();
		}

		progression.tier1MobKills = Math.max(1, progression.tier1MobKills);
		progression.tier7Day = Math.max(0, progression.tier7Day);

		if (difficulty == null) {
			difficulty = new Difficulty();
		}
		difficulty.validate();

		if (equipment == null) {
			equipment = new Equipment();
		}
		equipment.validate();

		if (behaviours == null) {
			behaviours = new Behaviours();
		}
		behaviours.validate();

		if (passiveRetaliation == null) {
			passiveRetaliation = new PassiveRetaliation();
		}
		passiveRetaliation.validate();
	}

	public static final class Behaviours {
		private static final double[] DEFAULT_SKELETON_MELEE = {0.0, 0.0, 0.0, 0.05, 0.10, 0.16, 0.24, 0.32, 0.35};
		private static final double[] DEFAULT_PILLAGER_MELEE = {0.0, 0.0, 0.0, 0.0, 0.08, 0.14, 0.22, 0.30, 0.33};
		private static final double[] DEFAULT_CREEPER_SPEED = {1.0, 1.0, 1.01, 1.02, 1.03, 1.05, 1.07, 1.09, 1.13};
		private static final double[] DEFAULT_SPIDER_SPEED = {1.0, 1.02, 1.03, 1.04, 1.06, 1.08, 1.11, 1.14, 1.19};

		public boolean enabled = true;
		public double[] skeletonMeleeChance = DEFAULT_SKELETON_MELEE.clone();
		public double[] pillagerMeleeChance = DEFAULT_PILLAGER_MELEE.clone();
		public double skeletonSwitchDistance = 4.0;
		public double pillagerSwitchDistance = 4.0;
		public double switchBackDistance = 7.0;
		public double meleePursuitSpeed = 1.15;
		public int meleeAttackCooldownTicks = 20;
		public double[] creeperSpeedMultipliers = DEFAULT_CREEPER_SPEED.clone();
		public double[] spiderSpeedMultipliers = DEFAULT_SPIDER_SPEED.clone();

		private void validate() {
			skeletonMeleeChance = validateChances(skeletonMeleeChance, DEFAULT_SKELETON_MELEE);
			pillagerMeleeChance = validateChances(pillagerMeleeChance, DEFAULT_PILLAGER_MELEE);
			creeperSpeedMultipliers = validateSpeed(creeperSpeedMultipliers, DEFAULT_CREEPER_SPEED);
			spiderSpeedMultipliers = validateSpeed(spiderSpeedMultipliers, DEFAULT_SPIDER_SPEED);
			skeletonSwitchDistance = positive(skeletonSwitchDistance, 4.0);
			pillagerSwitchDistance = positive(pillagerSwitchDistance, 4.0);
			switchBackDistance = Math.max(positive(switchBackDistance, 7.0),
					Math.max(skeletonSwitchDistance, pillagerSwitchDistance));
			meleePursuitSpeed = positive(meleePursuitSpeed, 1.15);
			meleeAttackCooldownTicks = Math.max(1, meleeAttackCooldownTicks);
		}

		private static double[] validateChances(double[] values, double[] defaults) {
			values = migrateTierValues(values, defaults);
			for (int index = 0; index < values.length; index++) {
				values[index] = Double.isFinite(values[index]) ? Math.max(0.0, Math.min(1.0, values[index])) : defaults[index];
			}
			return values;
		}

		private static double[] validateSpeed(double[] values, double[] defaults) {
			values = migrateTierValues(values, defaults);
			for (int index = 0; index < values.length; index++) {
				values[index] = Double.isFinite(values[index]) ? Math.max(0.1, Math.min(3.0, values[index])) : defaults[index];
			}
			return values;
		}

		private static double positive(double value, double fallback) {
			return Double.isFinite(value) && value > 0.0 ? value : fallback;
		}
	}

	public static final class Equipment {
		private static final double[] DEFAULT_EQUIPMENT_CHANCE = {0.0, 0.06, 0.09, 0.12, 0.20, 0.29, 0.39, 0.50, 0.66};
		private static final double[] DEFAULT_ARMOR_PIECE_CHANCE = {0.0, 0.04, 0.065, 0.09, 0.15, 0.22, 0.30, 0.40, 0.54};
		private static final double[] DEFAULT_ENCHANTMENT_CHANCE = {0.0, 0.0, 0.01, 0.02, 0.08, 0.15, 0.23, 0.33, 0.47};
		private static final int[] DEFAULT_ENCHANTMENT_POWER = {0, 0, 2, 3, 6, 10, 15, 22, 32};

		public boolean enabled = true;
		public double[] equipmentChance = DEFAULT_EQUIPMENT_CHANCE.clone();
		public double[] armorPieceChance = DEFAULT_ARMOR_PIECE_CHANCE.clone();
		public double[] enchantmentChance = DEFAULT_ENCHANTMENT_CHANCE.clone();
		public int[] enchantmentPower = DEFAULT_ENCHANTMENT_POWER.clone();
		public double shallowChanceBonus = 0.02;
		public double moderateChanceBonus = 0.05;
		public double deepChanceBonus = 0.10;
		public double netherChanceBonus = 0.08;
		public double endChanceBonus = 0.10;
		public double netherEnchantmentBonus = 0.06;
		public double endEnchantmentBonus = 0.08;
		public double tier8NetheriteChance = 0.35;
		public double equippedItemDropChance = 0.085;

		private void validate() {
			equipmentChance = validateChances(equipmentChance, DEFAULT_EQUIPMENT_CHANCE);
			armorPieceChance = validateChances(armorPieceChance, DEFAULT_ARMOR_PIECE_CHANCE);
			enchantmentChance = validateChances(enchantmentChance, DEFAULT_ENCHANTMENT_CHANCE);
			enchantmentPower = migrateTierValues(enchantmentPower, DEFAULT_ENCHANTMENT_POWER);
			for (int index = 0; index < enchantmentPower.length; index++) {
				enchantmentPower[index] = Math.max(0, Math.min(50, enchantmentPower[index]));
			}
			shallowChanceBonus = chance(shallowChanceBonus, 0.02);
			moderateChanceBonus = chance(moderateChanceBonus, 0.05);
			deepChanceBonus = chance(deepChanceBonus, 0.10);
			netherChanceBonus = chance(netherChanceBonus, 0.08);
			endChanceBonus = chance(endChanceBonus, 0.10);
			netherEnchantmentBonus = chance(netherEnchantmentBonus, 0.06);
			endEnchantmentBonus = chance(endEnchantmentBonus, 0.08);
			tier8NetheriteChance = chance(tier8NetheriteChance, 0.35);
			equippedItemDropChance = chance(equippedItemDropChance, 0.085);
		}

		private static double[] validateChances(double[] values, double[] defaults) {
			values = migrateTierValues(values, defaults);
			for (int index = 0; index < values.length; index++) {
				values[index] = chance(values[index], defaults[index]);
			}
			return values;
		}

		private static double chance(double value, double fallback) {
			return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : fallback;
		}
	}

	public static final class Progression {
		public int tier1MobKills = 10;
		public int tier7Day = 1000;
	}

	public static final class Difficulty {
		private static final double[] DEFAULT_HEALTH = {1.0, 1.05, 1.075, 1.10, 1.15, 1.25, 1.35, 1.50, 1.80};
		private static final double[] DEFAULT_DAMAGE = {1.0, 1.0, 1.025, 1.05, 1.10, 1.15, 1.20, 1.30, 1.45};

		public boolean enabled = true;
		public double[] healthMultipliers = DEFAULT_HEALTH.clone();
		public double[] damageMultipliers = DEFAULT_DAMAGE.clone();
		public Depth depth = new Depth();
		public Dimensions dimensions = new Dimensions();

		private void validate() {
			healthMultipliers = validateMultipliers(healthMultipliers, DEFAULT_HEALTH);
			damageMultipliers = validateMultipliers(damageMultipliers, DEFAULT_DAMAGE);
			if (depth == null) depth = new Depth();
			if (dimensions == null) dimensions = new Dimensions();
			depth.validate();
			dimensions.validate();
		}

		private static double[] validateMultipliers(double[] values, double[] defaults) {
			values = migrateTierValues(values, defaults);
			for (int index = 0; index < values.length; index++) {
				if (!Double.isFinite(values[index]) || values[index] < 0.1) {
					values[index] = defaults[index];
				}
			}
			return values;
		}
	}

	public static final class Depth {
		public int normalAboveY = 32;
		public int moderateBelowY = 0;
		public int deepAtOrBelowY = -32;
		public double shallowHealthBonus = 0.03;
		public double shallowDamageBonus = 0.02;
		public double moderateHealthBonus = 0.07;
		public double moderateDamageBonus = 0.05;
		public double deepHealthBonus = 0.12;
		public double deepDamageBonus = 0.10;

		private void validate() {
			if (normalAboveY < moderateBelowY) normalAboveY = 32;
			if (moderateBelowY <= deepAtOrBelowY) moderateBelowY = 0;
			shallowHealthBonus = finiteBonus(shallowHealthBonus, 0.03);
			shallowDamageBonus = finiteBonus(shallowDamageBonus, 0.02);
			moderateHealthBonus = finiteBonus(moderateHealthBonus, 0.07);
			moderateDamageBonus = finiteBonus(moderateDamageBonus, 0.05);
			deepHealthBonus = finiteBonus(deepHealthBonus, 0.12);
			deepDamageBonus = finiteBonus(deepDamageBonus, 0.10);
		}
	}

	public static final class Dimensions {
		public double netherHealthBonus = 0.10;
		public double netherDamageBonus = 0.10;
		public double endHealthBonus = 0.15;
		public double endDamageBonus = 0.15;

		private void validate() {
			netherHealthBonus = finiteBonus(netherHealthBonus, 0.10);
			netherDamageBonus = finiteBonus(netherDamageBonus, 0.10);
			endHealthBonus = finiteBonus(endHealthBonus, 0.15);
			endDamageBonus = finiteBonus(endDamageBonus, 0.15);
		}
	}

	private static double finiteBonus(double value, double fallback) {
		return Double.isFinite(value) && value > -0.9 ? value : fallback;
	}

	private static double[] migrateTierValues(double[] values, double[] defaults) {
		if (values == null) return defaults.clone();
		if (values.length == defaults.length) return values;
		if (values.length != 8 || defaults.length != 9) return defaults.clone();
		double[] migrated = new double[9];
		migrated[0] = values[0];
		migrated[1] = values[1];
		migrated[2] = (values[1] + values[2]) / 2.0;
		for (int index = 2; index < values.length; index++) migrated[index + 1] = values[index];
		migrated[8] += (values[7] - values[6]) * 0.2;
		return migrated;
	}

	private static int[] migrateTierValues(int[] values, int[] defaults) {
		if (values == null) return defaults.clone();
		if (values.length == defaults.length) return values;
		if (values.length != 8 || defaults.length != 9) return defaults.clone();
		int[] migrated = new int[9];
		migrated[0] = values[0];
		migrated[1] = values[1];
		migrated[2] = (values[1] + values[2]) / 2;
		for (int index = 2; index < values.length; index++) migrated[index + 1] = values[index];
		migrated[8] += Math.max(1, Math.round((values[7] - values[6]) * 0.2F));
		return migrated;
	}

	public static final class PassiveRetaliation {
		public boolean enabled = true;
		public double alertRadius = 16.0;
		public int aggressionSeconds = 30;
		public int attackCooldownTicks = 20;
		public boolean babiesRetaliate = false;
		public Cow cow = new Cow();
		public Pig pig = new Pig();
		public Sheep sheep = new Sheep();

		private void validate() {
			alertRadius = Math.max(0.0, Math.min(128.0, alertRadius));
			aggressionSeconds = Math.max(1, aggressionSeconds);
			attackCooldownTicks = Math.max(1, attackCooldownTicks);
			if (cow == null) cow = new Cow();
			if (pig == null) pig = new Pig();
			if (sheep == null) sheep = new Sheep();
			cow.validate();
			pig.validate();
			sheep.validate();
		}
	}

	public static final class Cow {
		public boolean enabled = true;
		public double damage = 3.0;
		public double speedMultiplier = 1.0;
		public double knockback = 0.5;
		public double tier8Damage = 5.2;

		private void validate() {
			damage = Math.max(0.0, damage);
			tier8Damage = Math.max(0.0, tier8Damage);
			speedMultiplier = Math.max(0.1, speedMultiplier);
			knockback = Math.max(0.0, knockback);
		}
	}

	public static final class Pig {
		public boolean enabled = true;
		public double damage = 2.0;
		public double speedMultiplier = 1.25;
		public double knockback = 0.15;
		public double tier8Damage = 3.65;

		private void validate() {
			damage = Math.max(0.0, damage);
			tier8Damage = Math.max(0.0, tier8Damage);
			speedMultiplier = Math.max(0.1, speedMultiplier);
			knockback = Math.max(0.0, knockback);
		}
	}

	public static final class Sheep {
		public boolean enabled = true;
		public double damage = 2.5;
		public double speedMultiplier = 1.0;
		public double knockback = 0.35;
		public double tier8Damage = 4.15;

		private void validate() {
			damage = Math.max(0.0, damage);
			tier8Damage = Math.max(0.0, tier8Damage);
			speedMultiplier = Math.max(0.1, speedMultiplier);
			knockback = Math.max(0.0, knockback);
		}
	}
}

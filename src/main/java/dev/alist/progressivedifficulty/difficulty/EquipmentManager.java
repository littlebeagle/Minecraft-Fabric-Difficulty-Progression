package dev.alist.progressivedifficulty.difficulty;

import dev.alist.progressivedifficulty.config.ConfigManager;
import dev.alist.progressivedifficulty.config.ModConfig;
import dev.alist.progressivedifficulty.progression.WorldProgressionState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public final class EquipmentManager {
	public static final String PROCESSED_TAG = "progressivedifficulty.equipment_processed";
	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
	};

	private EquipmentManager() {
	}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob mob && isEligible(mob)) {
				apply(level, mob);
			}
		});
	}

	public static boolean wasProcessed(Mob mob) {
		return mob.entityTags().contains(PROCESSED_TAG);
	}

	private static boolean isEligible(Mob mob) {
		return mob instanceof Zombie
				|| mob instanceof AbstractSkeleton
				|| mob instanceof AbstractPiglin
				|| mob instanceof Pillager;
	}

	private static void apply(ServerLevel level, Mob mob) {
		if (!mob.addTag(PROCESSED_TAG)) return;

		ModConfig.Equipment config = ConfigManager.get().equipment;
		int tier = WorldProgressionState.get(level.getServer()).currentTier();
		if (!config.enabled || tier == 0) return;

		RandomSource random = mob.getRandom();
		double locationBonus = locationBonus(level, mob.getY(), config);
		double equipmentChance = clamp(config.equipmentChance[tier] + locationBonus);
		double armorChance = clamp(config.armorPieceChance[tier] + locationBonus);
		double enchantmentChance = clamp(config.enchantmentChance[tier] + enchantmentLocationBonus(level, config));

		if (mob.getMainHandItem().isEmpty() && random.nextDouble() < equipmentChance) {
			equip(mob, EquipmentSlot.MAINHAND, new ItemStack(selectWeapon(mob, tier, random)), config, level, tier, enchantmentChance);
		} else {
			enchantExisting(mob, EquipmentSlot.MAINHAND, level, tier, enchantmentChance);
		}

		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (mob.getItemBySlot(slot).isEmpty() && random.nextDouble() < armorChance) {
				equip(mob, slot, new ItemStack(selectArmor(slot, tier, random)), config, level, tier, enchantmentChance);
			} else {
				enchantExisting(mob, slot, level, tier, enchantmentChance);
			}
		}
	}

	private static void equip(Mob mob, EquipmentSlot slot, ItemStack stack, ModConfig.Equipment config,
			ServerLevel level, int tier, double enchantmentChance) {
		if (stack.isEmpty()) return;
		enchant(stack, mob, level, tier, enchantmentChance);
		mob.setItemSlot(slot, stack);
		mob.setDropChance(slot, isNetherite(stack) ? 0.0F : (float) config.equippedItemDropChance);
	}

	private static void enchantExisting(Mob mob, EquipmentSlot slot, ServerLevel level, int tier, double chance) {
		ItemStack stack = mob.getItemBySlot(slot);
		if (!stack.isEmpty() && !stack.isEnchanted()) enchant(stack, mob, level, tier, chance);
	}

	private static void enchant(ItemStack stack, Mob mob, ServerLevel level, int tier, double chance) {
		if (stack.isEnchantable() && mob.getRandom().nextDouble() < chance) {
			EnchantmentHelper.enchantItem(
					mob.getRandom(), stack, ConfigManager.get().equipment.enchantmentPower[tier],
					level.registryAccess(), java.util.Optional.of(
							level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
									.getOrThrow(EnchantmentTags.IN_ENCHANTING_TABLE)
					)
			);
		}
	}

	private static Item selectWeapon(Mob mob, int tier, RandomSource random) {
		if (mob instanceof AbstractSkeleton && !(mob instanceof WitherSkeleton)) return Items.BOW;
		if (mob instanceof Pillager) return Items.CROSSBOW;
		if (tier == 8 && random.nextDouble() < ConfigManager.get().equipment.tier8NetheriteChance) {
			return random.nextDouble() < 0.20 ? Items.NETHERITE_AXE : Items.NETHERITE_SWORD;
		}
		int quality = quality(tier, random);
		if (quality <= 1) return random.nextBoolean() ? Items.WOODEN_SWORD : Items.STONE_SWORD;
		if (quality == 2) return random.nextBoolean() ? Items.GOLDEN_SWORD : Items.STONE_SWORD;
		if (quality <= 4 || (quality == 5 && random.nextDouble() >= 0.45)) {
			return random.nextDouble() < 0.25 ? Items.IRON_AXE : Items.IRON_SWORD;
		}
		return random.nextDouble() < 0.20 ? Items.DIAMOND_AXE : Items.DIAMOND_SWORD;
	}

	private static Item selectArmor(EquipmentSlot slot, int tier, RandomSource random) {
		if (tier == 8 && random.nextDouble() < ConfigManager.get().equipment.tier8NetheriteChance) {
			return netherite()[armorIndex(slot)];
		}
		int quality = quality(tier, random);
		Item[] set;
		if (quality <= 1) set = leather();
		else if (quality == 2) set = random.nextBoolean() ? gold() : chainmail();
		else if (quality <= 4 || (quality == 5 && random.nextDouble() >= 0.45)) set = iron();
		else set = diamond();
		return set[armorIndex(slot)];
	}

	private static int quality(int tier, RandomSource random) {
		int base = Math.max(1, tier);
		int spread = random.nextDouble() < 0.22 ? 1 : random.nextDouble() < 0.35 ? -1 : 0;
		return Math.max(1, Math.min(6, base + spread));
	}

	private static int armorIndex(EquipmentSlot slot) {
		return switch (slot) {
			case FEET -> 0;
			case LEGS -> 1;
			case CHEST -> 2;
			case HEAD -> 3;
			default -> throw new IllegalArgumentException("Not an armor slot: " + slot);
		};
	}

	private static Item[] leather() { return new Item[]{Items.LEATHER_BOOTS, Items.LEATHER_LEGGINGS, Items.LEATHER_CHESTPLATE, Items.LEATHER_HELMET}; }
	private static Item[] gold() { return new Item[]{Items.GOLDEN_BOOTS, Items.GOLDEN_LEGGINGS, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_HELMET}; }
	private static Item[] chainmail() { return new Item[]{Items.CHAINMAIL_BOOTS, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_HELMET}; }
	private static Item[] iron() { return new Item[]{Items.IRON_BOOTS, Items.IRON_LEGGINGS, Items.IRON_CHESTPLATE, Items.IRON_HELMET}; }
	private static Item[] diamond() { return new Item[]{Items.DIAMOND_BOOTS, Items.DIAMOND_LEGGINGS, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_HELMET}; }
	private static Item[] netherite() { return new Item[]{Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HELMET}; }

	private static boolean isNetherite(ItemStack stack) {
		Item item = stack.getItem();
		return item == Items.NETHERITE_SWORD || item == Items.NETHERITE_AXE
				|| item == Items.NETHERITE_BOOTS || item == Items.NETHERITE_LEGGINGS
				|| item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_HELMET;
	}

	private static double locationBonus(ServerLevel level, double y, ModConfig.Equipment config) {
		double bonus;
		ModConfig.Depth depth = ConfigManager.get().difficulty.depth;
		if (y > depth.normalAboveY) bonus = 0.0;
		else if (y >= depth.moderateBelowY) bonus = config.shallowChanceBonus;
		else if (y > depth.deepAtOrBelowY) bonus = config.moderateChanceBonus;
		else bonus = config.deepChanceBonus;
		if (Level.NETHER.equals(level.dimension())) bonus += config.netherChanceBonus;
		else if (Level.END.equals(level.dimension())) bonus += config.endChanceBonus;
		return bonus;
	}

	private static double enchantmentLocationBonus(ServerLevel level, ModConfig.Equipment config) {
		if (Level.NETHER.equals(level.dimension())) return config.netherEnchantmentBonus;
		if (Level.END.equals(level.dimension())) return config.endEnchantmentBonus;
		return 0.0;
	}

	private static double clamp(double value) {
		return Math.max(0.0, Math.min(1.0, value));
	}
}

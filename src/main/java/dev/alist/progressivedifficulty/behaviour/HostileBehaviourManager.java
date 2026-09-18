package dev.alist.progressivedifficulty.behaviour;

import dev.alist.progressivedifficulty.ProgressiveDifficulty;
import dev.alist.progressivedifficulty.config.ConfigManager;
import dev.alist.progressivedifficulty.config.ModConfig;
import dev.alist.progressivedifficulty.progression.WorldProgressionState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class HostileBehaviourManager {
	public static final Identifier CREEPER_SPEED_ID = ProgressiveDifficulty.id("creeper_speed");
	public static final Identifier SPIDER_SPEED_ID = ProgressiveDifficulty.id("spider_speed");
	public static final String SKELETON_MELEE_TAG = "progressivedifficulty.skeleton_melee";
	public static final String PILLAGER_MELEE_TAG = "progressivedifficulty.pillager_melee";
	private static final String SKELETON_EVALUATED_TAG = "progressivedifficulty.skeleton_melee_evaluated";
	private static final String PILLAGER_EVALUATED_TAG = "progressivedifficulty.pillager_melee_evaluated";
	private static final Set<Mob> GOAL_INSTALLED = Collections.newSetFromMap(new WeakHashMap<>());

	private HostileBehaviourManager() {
	}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			ModConfig.Behaviours config = ConfigManager.get().behaviours;
			if (!config.enabled) return;
			int tier = WorldProgressionState.get(level.getServer()).currentTier();
			if (entity instanceof Creeper creeper) applySpeed(creeper, CREEPER_SPEED_ID, config.creeperSpeedMultipliers[tier]);
			else if (entity instanceof Spider spider) applySpeed(spider, SPIDER_SPEED_ID, config.spiderSpeedMultipliers[tier]);
			else if (entity instanceof AbstractSkeleton skeleton && !(skeleton instanceof WitherSkeleton)) {
				configureCloseRange(level, skeleton, tier, SKELETON_EVALUATED_TAG, SKELETON_MELEE_TAG,
						config.skeletonMeleeChance[tier], Items.BOW, meleeWeapon(tier), config.skeletonSwitchDistance);
			} else if (entity instanceof Pillager pillager) {
				configureCloseRange(level, pillager, tier, PILLAGER_EVALUATED_TAG, PILLAGER_MELEE_TAG,
						config.pillagerMeleeChance[tier], Items.CROSSBOW, meleeWeapon(tier), config.pillagerSwitchDistance);
			}
		});
	}

	public static double appliedSpeedMultiplier(Mob mob) {
		Identifier id = mob instanceof Creeper ? CREEPER_SPEED_ID : mob instanceof Spider ? SPIDER_SPEED_ID : null;
		AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
		if (id == null || speed == null) return Double.NaN;
		AttributeModifier modifier = speed.getModifier(id);
		return modifier == null ? 1.0 : 1.0 + modifier.amount();
	}

	public static String tacticalBehaviour(Mob mob) {
		if (mob.entityTags().contains(SKELETON_MELEE_TAG)) return "close-range weapon switcher";
		if (mob.entityTags().contains(PILLAGER_MELEE_TAG)) return "close-range weapon switcher";
		return "vanilla";
	}

	private static void configureCloseRange(ServerLevel level, Mob mob, int tier, String evaluatedTag,
			String capableTag, double chance, Item rangedWeapon, Item meleeWeapon, double switchDistance) {
		if (!mob.entityTags().contains(evaluatedTag)) {
			mob.addTag(evaluatedTag);
			if (mob.getRandom().nextDouble() < chance) mob.addTag(capableTag);
		}
		if (!mob.entityTags().contains(capableTag) || !GOAL_INSTALLED.add(mob)) return;

		normalizeWeapons(mob, rangedWeapon, meleeWeapon);
		if (!mob.getMainHandItem().is(rangedWeapon) || mob.getOffhandItem().isEmpty()) return;
		mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
		ModConfig.Behaviours config = ConfigManager.get().behaviours;
		mob.getGoalSelector().addGoal(1, new CloseRangeWeaponGoal(
				mob, rangedWeapon, switchDistance, config.switchBackDistance,
				config.meleePursuitSpeed, config.meleeAttackCooldownTicks
		));
	}

	private static void normalizeWeapons(Mob mob, Item rangedWeapon, Item meleeWeapon) {
		if (mob.getOffhandItem().is(rangedWeapon) && !mob.getMainHandItem().is(rangedWeapon)) {
			ItemStack main = mob.getMainHandItem();
			mob.setItemSlot(EquipmentSlot.MAINHAND, mob.getOffhandItem());
			mob.setItemSlot(EquipmentSlot.OFFHAND, main);
		}
		if (mob.getMainHandItem().is(rangedWeapon) && mob.getOffhandItem().isEmpty()) {
			mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(meleeWeapon));
		}
	}

	private static Item meleeWeapon(int tier) {
		if (tier >= 8) return Items.NETHERITE_AXE;
		if (tier >= 5) return Items.DIAMOND_AXE;
		if (tier >= 3) return Items.IRON_AXE;
		return Items.STONE_AXE;
	}

	private static void applySpeed(Mob mob, Identifier id, double multiplier) {
		AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null || speed.hasModifier(id)) return;
		speed.addOrReplacePermanentModifier(new AttributeModifier(
				id, multiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
		));
	}
}

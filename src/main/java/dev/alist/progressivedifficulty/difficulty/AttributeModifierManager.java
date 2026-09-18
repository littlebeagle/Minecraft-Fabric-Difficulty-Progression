package dev.alist.progressivedifficulty.difficulty;

import dev.alist.progressivedifficulty.ProgressiveDifficulty;
import dev.alist.progressivedifficulty.config.ConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class AttributeModifierManager {
	public static final Identifier HEALTH_MODIFIER_ID = ProgressiveDifficulty.id("difficulty_health");
	public static final Identifier DAMAGE_MODIFIER_ID = ProgressiveDifficulty.id("difficulty_damage");

	private AttributeModifierManager() {
	}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof LivingEntity living && DifficultyManager.isHostile(living)) {
				apply(level, living);
			}
		});
	}

	public static double appliedHealthMultiplier(LivingEntity entity) {
		return appliedMultiplier(entity.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER_ID);
	}

	public static double appliedDamageMultiplier(LivingEntity entity) {
		return appliedMultiplier(entity.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_MODIFIER_ID);
	}

	private static void apply(ServerLevel level, LivingEntity entity) {
		AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
		if (health == null) {
			return;
		}

		if (!ConfigManager.get().difficulty.enabled) {
			removeScaling(entity, health);
			return;
		}

		if (health.hasModifier(HEALTH_MODIFIER_ID)) {
			return;
		}

		DifficultyProfile profile = DifficultyManager.profileFor(level, entity);
		float healthRatio = entity.getHealth() / Math.max(1.0F, entity.getMaxHealth());
		health.addOrReplacePermanentModifier(new AttributeModifier(
				HEALTH_MODIFIER_ID,
				profile.healthMultiplier() - 1.0,
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE
		));
		entity.setHealth(entity.getMaxHealth() * healthRatio);

		AttributeInstance damage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage != null) {
			damage.addOrReplacePermanentModifier(new AttributeModifier(
					DAMAGE_MODIFIER_ID,
					profile.damageMultiplier() - 1.0,
					AttributeModifier.Operation.ADD_MULTIPLIED_BASE
			));
		}
	}

	private static void removeScaling(LivingEntity entity, AttributeInstance health) {
		if (!health.hasModifier(HEALTH_MODIFIER_ID)) {
			return;
		}

		float healthRatio = entity.getHealth() / Math.max(1.0F, entity.getMaxHealth());
		health.removeModifier(HEALTH_MODIFIER_ID);
		entity.setHealth(entity.getMaxHealth() * healthRatio);

		AttributeInstance damage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage != null) {
			damage.removeModifier(DAMAGE_MODIFIER_ID);
		}
	}

	private static double appliedMultiplier(AttributeInstance attribute, Identifier modifierId) {
		if (attribute == null) {
			return Double.NaN;
		}
		AttributeModifier modifier = attribute.getModifier(modifierId);
		return modifier == null ? 1.0 : 1.0 + modifier.amount();
	}
}

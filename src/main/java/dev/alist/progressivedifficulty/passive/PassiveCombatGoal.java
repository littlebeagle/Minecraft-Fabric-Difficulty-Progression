package dev.alist.progressivedifficulty.passive;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;

public final class PassiveCombatGoal extends Goal {
	private static final int PATH_RECALCULATION_TICKS = 10;

	private final Animal animal;
	private ServerPlayer target;
	private int attackCooldown;
	private int pathRecalculationCooldown;

	public PassiveCombatGoal(Animal animal) {
		this.animal = animal;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		target = PassiveRetaliationManager.targetFor(animal);
		return target != null;
	}

	@Override
	public boolean canContinueToUse() {
		target = PassiveRetaliationManager.targetFor(animal);
		return target != null;
	}

	@Override
	public void start() {
		animal.setTarget(target);
		pathRecalculationCooldown = 0;
	}

	@Override
	public void stop() {
		animal.getNavigation().stop();
		if (animal.getTarget() == target) {
			animal.setTarget(null);
		}
		target = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (target == null || !(animal.level() instanceof ServerLevel level)) {
			return;
		}

		PassiveCombatProfile profile = PassiveRetaliationManager.profileFor(animal);
		animal.getLookControl().setLookAt(target, 30.0F, 30.0F);

		if (--pathRecalculationCooldown <= 0) {
			pathRecalculationCooldown = PATH_RECALCULATION_TICKS;
			animal.getNavigation().moveTo(target, profile.speedMultiplier());
		}

		if (attackCooldown > 0) {
			attackCooldown--;
		}

		double reach = animal.getBbWidth() * 2.0F + target.getBbWidth();
		if (attackCooldown == 0 && animal.distanceToSqr(target) <= reach * reach) {
			DamageSource source = animal.damageSources().mobAttack(animal);
			animal.swingForAttack(InteractionHand.MAIN_HAND);
			if (target.hurtServer(level, source, (float) profile.damage()) && profile.knockback() > 0.0) {
				target.knockback(
						profile.knockback(),
						animal.getX() - target.getX(),
						animal.getZ() - target.getZ(),
						source,
						(float) profile.damage()
				);
			}
			attackCooldown = PassiveRetaliationManager.attackCooldownTicks();
		}
	}
}

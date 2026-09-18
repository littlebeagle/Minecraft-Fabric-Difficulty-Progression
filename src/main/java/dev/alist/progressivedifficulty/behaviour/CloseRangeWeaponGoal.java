package dev.alist.progressivedifficulty.behaviour;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

final class CloseRangeWeaponGoal extends Goal {
	private static final int PATH_RECALCULATION_TICKS = 10;

	private final Mob mob;
	private final Item rangedWeapon;
	private final double engageDistanceSquared;
	private final double disengageDistanceSquared;
	private final double pursuitSpeed;
	private final int attackCooldownTicks;
	private LivingEntity target;
	private int attackCooldown;
	private int pathCooldown;

	CloseRangeWeaponGoal(Mob mob, Item rangedWeapon, double engageDistance, double disengageDistance,
			double pursuitSpeed, int attackCooldownTicks) {
		this.mob = mob;
		this.rangedWeapon = rangedWeapon;
		this.engageDistanceSquared = engageDistance * engageDistance;
		this.disengageDistanceSquared = disengageDistance * disengageDistance;
		this.pursuitSpeed = pursuitSpeed;
		this.attackCooldownTicks = attackCooldownTicks;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		target = mob.getTarget();
		return validTarget(target) && mob.distanceToSqr(target) <= engageDistanceSquared && hasBothWeapons();
	}

	@Override
	public boolean canContinueToUse() {
		target = mob.getTarget();
		return validTarget(target) && mob.distanceToSqr(target) <= disengageDistanceSquared && hasBothWeapons();
	}

	@Override
	public void start() {
		if (mob.getMainHandItem().is(rangedWeapon)) {
			swapHands();
			mob.swingForAttack(InteractionHand.MAIN_HAND);
		}
		pathCooldown = 0;
	}

	@Override
	public void stop() {
		mob.getNavigation().stop();
		if (mob.getOffhandItem().is(rangedWeapon)) {
			swapHands();
			mob.swingForAttack(InteractionHand.MAIN_HAND);
		}
		target = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (target == null || !(mob.level() instanceof ServerLevel level)) return;
		mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (--pathCooldown <= 0) {
			pathCooldown = PATH_RECALCULATION_TICKS;
			mob.getNavigation().moveTo(target, pursuitSpeed);
		}
		if (attackCooldown > 0) attackCooldown--;
		if (attackCooldown == 0 && mob.isWithinMeleeAttackRange(target)) {
			mob.doHurtTarget(level, target);
			attackCooldown = attackCooldownTicks;
		}
	}

	private boolean validTarget(LivingEntity candidate) {
		return candidate instanceof Player player && player.isAlive() && !player.isCreative() && !player.isSpectator();
	}

	private boolean hasBothWeapons() {
		return mob.getMainHandItem().is(rangedWeapon) || mob.getOffhandItem().is(rangedWeapon);
	}

	private void swapHands() {
		ItemStack main = mob.getMainHandItem();
		ItemStack offhand = mob.getOffhandItem();
		mob.setItemSlot(EquipmentSlot.MAINHAND, offhand);
		mob.setItemSlot(EquipmentSlot.OFFHAND, main);
	}
}

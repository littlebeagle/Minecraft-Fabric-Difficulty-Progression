package dev.alist.progressivedifficulty.client.mixin;

import dev.alist.progressivedifficulty.client.animation.AttackAnimationRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements AttackAnimationRenderState {
	@Unique
	private float progressivedifficulty$attackAnimation;

	@Override
	public float progressivedifficulty$getAttackAnimation() {
		return progressivedifficulty$attackAnimation;
	}

	@Override
	public void progressivedifficulty$setAttackAnimation(float progress) {
		progressivedifficulty$attackAnimation = progress;
	}
}

package dev.alist.progressivedifficulty.client.mixin;

import dev.alist.progressivedifficulty.client.animation.AttackAnimationRenderState;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(QuadrupedModel.class)
public abstract class QuadrupedModelMixin {
	@Shadow protected ModelPart head;
	@Shadow protected ModelPart body;
	@Shadow protected ModelPart rightFrontLeg;
	@Shadow protected ModelPart leftFrontLeg;

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)V", at = @At("TAIL"))
	private void progressivedifficulty$animateAttack(LivingEntityRenderState state, CallbackInfo callbackInfo) {
		float progress = ((AttackAnimationRenderState) state).progressivedifficulty$getAttackAnimation();
		if (progress <= 0.0F) return;
		float impact = Mth.sin(progress * Mth.PI);

		if ((Object) this instanceof CowModel) {
			head.xRot += impact * 0.55F;
			head.z -= impact * 1.8F;
			rightFrontLeg.xRot -= impact * 0.18F;
			leftFrontLeg.xRot -= impact * 0.18F;
		} else if ((Object) this instanceof PigModel) {
			head.xRot += impact * 0.22F;
			head.z -= impact * 2.4F;
			body.xRot -= impact * 0.08F;
			rightFrontLeg.xRot -= impact * 0.28F;
			leftFrontLeg.xRot -= impact * 0.28F;
		}
	}
}

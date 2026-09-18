package dev.alist.progressivedifficulty.client.mixin;

import dev.alist.progressivedifficulty.client.animation.AttackAnimationRenderState;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SheepModel.class)
public abstract class SheepModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/SheepRenderState;)V", at = @At("TAIL"))
	private void progressivedifficulty$animateHeadbutt(SheepRenderState state, CallbackInfo callbackInfo) {
		float progress = ((AttackAnimationRenderState) state).progressivedifficulty$getAttackAnimation();
		if (progress <= 0.0F) return;
		float impact = Mth.sin(progress * Mth.PI);
		ModelPart head = ((SheepModel) (Object) this).root().getChild("head");
		head.xRot += impact * 0.65F;
		head.z -= impact * 2.0F;
	}
}

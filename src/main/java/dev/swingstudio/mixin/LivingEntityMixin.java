package dev.swingstudio.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.swingstudio.SwingTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	private static final String SWING = "swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z";

	@ModifyExpressionValue(
		method = SWING,
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getModifiedSwingDuration(Lnet/minecraft/world/item/component/SwingAnimation;)I")
	)
	private int swingstudio$scaleDuration(int duration, @Local(argsOnly = true) InteractionHand hand, @Local(argsOnly = true) SwingAnimation animation) {
		return (Object) this instanceof LocalPlayer player ? SwingTracker.scaleDuration(player, hand, duration, animation.duration()) : duration;
	}

	@Inject(method = SWING, at = @At("RETURN"))
	private void swingstudio$track(InteractionHand hand, SwingAnimation animation, boolean sendToSwingingEntity, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && (Object) this instanceof LocalPlayer player) {
			SwingTracker.started(player, hand);
		}
	}
}

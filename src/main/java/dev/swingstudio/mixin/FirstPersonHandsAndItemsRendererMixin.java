package dev.swingstudio.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.sugar.Local;
import dev.swingstudio.Preview;
import dev.swingstudio.SwingAnimator;
import dev.swingstudio.SwingConfig;
import dev.swingstudio.SwingSettings;
import dev.swingstudio.SwingTracker;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {
	@WrapOperation(
		method = "submitHandsWithItems",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
		)
	)
	private void swingstudio$hand(
		FirstPersonHandsAndItemsRenderer renderer,
		PlayerRenderState playerState,
		FirstPersonHandsAndItemsRenderState state,
		float partialTicks,
		float xRot,
		InteractionHand hand,
		float attack,
		ItemStack itemStack,
		float inverseArmHeight,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int lightCoords,
		Operation<Void> original
	) {
		SwingConfig config = SwingConfig.get();
		if (config.enabled && !config.equipAnimation) {
			inverseArmHeight = 0.0F;
		}

		SwingSettings settings = config.active(itemStack);
		AvatarRenderState avatar = playerState.avatarRenderState;
		if (settings == null || avatar == null) {
			original.call(renderer, playerState, state, partialTicks, xRot, hand, attack, itemStack, inverseArmHeight, poseStack, submitNodeCollector, lightCoords);
			return;
		}

		LivingEntity.SwingDescription swing = avatar.currentSwing;
		boolean swinging = swing != null && swing.hand() == hand;
		float curved = swinging ? settings.shape(SwingTracker.progress(swing, hand, attack)) : 0.0F;
		Float frozen = Preview.frozen();
		if (frozen != null && hand == InteractionHand.MAIN_HAND) {
			swinging = true;
			curved = settings.shape(frozen);
		}

		HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? avatar.mainArm : avatar.mainArm.getOpposite();
		int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
		int swingIndex = Math.max(0, SwingTracker.count(hand) - 1);
		SwingSettings.VerticalMode vertical = settings.verticalMode();
		boolean both = settings.alternate && vertical == SwingSettings.VerticalMode.ALTERNATE;
		boolean otherSide = settings.alternate && swingIndex % 2 == 1;
		boolean upsideDown = settings.upsideDown(swingIndex, both);
		float randomScale = 1.0F + SwingTracker.random(hand) * settings.variation / 100.0F;
		SwingAnimator.begin(settings, curved, swinging, invert, otherSide, upsideDown, randomScale);
		poseStack.pushPose();
		try {
			SwingAnimator.applyHandPose(poseStack, settings, invert, state.viewXRot - state.xBob, state.viewYRot - state.yBob);
			float vanillaAttack = swinging ? Mth.clamp(curved, 0.0F, 1.0F) : attack;
			original.call(renderer, playerState, state, partialTicks, xRot, hand, vanillaAttack, itemStack, inverseArmHeight, poseStack, submitNodeCollector, lightCoords);
		} finally {
			poseStack.popPose();
			SwingAnimator.end();
		}
	}

	@ModifyExpressionValue(
		method = "submitArmWithItem",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;currentSwing:Lnet/minecraft/world/entity/LivingEntity$SwingDescription;",
			opcode = Opcodes.GETFIELD
		)
	)
	private LivingEntity.@Nullable SwingDescription swingstudio$heldMoment(
		LivingEntity.@Nullable SwingDescription swing, @Local(argsOnly = true) InteractionHand hand, @Local(argsOnly = true) ItemStack itemStack
	) {
		if (Preview.frozen() == null || hand != InteractionHand.MAIN_HAND || SwingAnimator.settings() == null) {
			return swing;
		}

		SwingAnimation animation = itemStack.getAttackAnimation();
		return new LivingEntity.SwingDescription(hand, animation, Math.max(1, animation.duration()));
	}

	@Inject(method = "applyItemArmTransform", at = @At("TAIL"))
	private void swingstudio$turnSwing(PoseStack poseStack, HumanoidArm arm, float inverseArmHeight, CallbackInfo ci) {
		SwingAnimator.afterArmTransform(poseStack);
	}

	@ModifyExpressionValue(
		method = "submitArmWithItem",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/SwingAnimation;type()Lnet/minecraft/world/item/SwingAnimationType;")
	)
	private SwingAnimationType swingstudio$style(SwingAnimationType type) {
		return SwingAnimator.style(type);
	}

	@WrapOperation(
		method = "submitArmWithItem",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"
		)
	)
	private void swingstudio$slash(FirstPersonHandsAndItemsRenderer renderer, float animation, PoseStack poseStack, int invert, HumanoidArm arm, Operation<Void> original) {
		SwingSettings settings = SwingAnimator.settings();
		if (settings == null) {
			original.call(renderer, animation, poseStack, invert, arm);
			return;
		}

		SwingAnimator.swing(poseStack, SwingAnimator.progress(animation), invert, settings);
	}

	@WrapOperation(
		method = "submitArmWithItem",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/model/effects/SpearAnimations;firstPersonAttack(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"
		)
	)
	private void swingstudio$stab(float attack, PoseStack poseStack, int invert, HumanoidArm arm, Operation<Void> original) {
		SwingSettings settings = SwingAnimator.settings();
		if (settings == null) {
			original.call(attack, poseStack, invert, arm);
			return;
		}

		SwingAnimator.stab(poseStack, SwingAnimator.progress(attack), invert, settings);
	}

	@WrapOperation(
		method = "submitArmWithItem",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
		)
	)
	private void swingstudio$itemPose(
		ItemStackRenderState itemState,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		int lightCoords,
		int overlayCoords,
		int outlineColor,
		Operation<Void> original
	) {
		SwingAnimator.applyItemPose(poseStack);
		original.call(itemState, poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
	}
}

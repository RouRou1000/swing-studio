package dev.swingstudio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.item.SwingAnimationType;
import org.jspecify.annotations.Nullable;

public final class SwingAnimator {
	private static final float PI = (float) Math.PI;

	@Nullable
	private static SwingSettings settings;
	private static float curved;
	private static boolean swinging;
	private static int invert = 1;
	private static boolean holdApplied;
	private static boolean flipSide;
	private static boolean flipVertical;
	private static float sizeScale = 1.0F;

	private SwingAnimator() {
	}

	public static void begin(SwingSettings active, float curvedProgress, boolean isSwinging, int armInvert, boolean otherSide, boolean upsideDown, float randomScale) {
		settings = active;
		curved = curvedProgress;
		swinging = isSwinging;
		invert = armInvert;
		holdApplied = false;
		flipSide = otherSide;
		flipVertical = upsideDown;
		sizeScale = randomScale;
	}

	public static void end() {
		settings = null;
	}

	public static @Nullable SwingSettings settings() {
		return settings;
	}

	public static float progress(float vanilla) {
		return swinging ? curved : vanilla;
	}

	public static SwingAnimationType style(SwingAnimationType type) {
		SwingSettings active = settings;
		if (active == null) {
			return type;
		}

		return switch (active.style) {
			case AUTO -> type;
			case STAB -> SwingAnimationType.STAB;
			case NONE -> SwingAnimationType.NONE;
			default -> SwingAnimationType.WHACK;
		};
	}

	private static float signedSqrt(float value) {
		return value < 0.0F ? -Mth.sqrt(-value) : Mth.sqrt(value);
	}

	public static void swing(PoseStack poseStack, float attack, int armInvert, SwingSettings active) {
		int side = active.mirror != flipSide ? -armInvert : armInvert;
		float strength = active.strength / 100.0F * sizeScale;
		float mx = strength * active.moveX / 100.0F;
		float my = strength * active.moveY / 100.0F;
		float mz = strength * active.moveZ / 100.0F;
		float mt = strength * active.turnStrength / 100.0F;
		float v = flipVertical ? -1.0F : 1.0F;
		my *= v;
		float root = signedSqrt(attack);
		float arc = Mth.sin(root * PI);
		switch (active.style) {
			case CHOP -> {
				poseStack.translate(side * -0.1F * arc * mx, 0.12F * Mth.sin(root * PI * 2.0F) * my, -0.25F * arc * mz);
				poseStack.rotateDegrees(Axis.XP, v * -95.0F * arc * mt);
			}
			case SWEEP -> {
				poseStack.translate(side * -0.35F * arc * mx, 0.05F * arc * my, -0.15F * Mth.sin(attack * PI) * mz);
				poseStack.rotateDegrees(Axis.YP, side * 55.0F * arc * mt);
				poseStack.rotateDegrees(Axis.ZP, v * side * -15.0F * arc * mt);
				poseStack.rotateDegrees(Axis.XP, v * -30.0F * arc * mt);
			}
			case UPPERCUT -> {
				poseStack.translate(side * -0.12F * arc * mx, 0.22F * arc * my, -0.18F * arc * mz);
				poseStack.rotateDegrees(Axis.XP, v * 35.0F * arc * mt);
				poseStack.rotateDegrees(Axis.ZP, v * side * 10.0F * arc * mt);
			}
			case PUNCH -> {
				poseStack.translate(side * -0.06F * arc * mx, 0.02F * arc * my, -0.38F * arc * mz);
				poseStack.rotateDegrees(Axis.XP, v * -12.0F * arc * mt);
			}
			default -> slash(poseStack, attack, side, v, mx, my, mz, mt);
		}
	}

	private static void slash(PoseStack poseStack, float attack, int side, float v, float mx, float my, float mz, float mt) {
		float root = signedSqrt(attack);
		float xSwing = -0.4F * Mth.sin(root * PI);
		float ySwing = 0.2F * Mth.sin(root * PI * 2.0F);
		float zSwing = -0.2F * Mth.sin(attack * PI);
		poseStack.translate(side * xSwing * mx, ySwing * my, zSwing * mz);
		float yRotation = Mth.sin(attack * attack * PI);
		poseStack.rotateDegrees(Axis.YP, side * (45.0F + yRotation * -20.0F * mt));
		float xzRotation = Mth.sin(root * PI);
		poseStack.rotateDegrees(Axis.ZP, v * side * xzRotation * -20.0F * mt);
		poseStack.rotateDegrees(Axis.XP, v * xzRotation * -80.0F * mt);
		poseStack.rotateDegrees(Axis.YP, side * -45.0F);
	}

	public static void stab(PoseStack poseStack, float attack, int armInvert, SwingSettings active) {
		int side = active.mirror != flipSide ? -armInvert : armInvert;
		float strength = active.strength / 100.0F * sizeScale;
		float mx = strength * active.moveX / 100.0F;
		float my = strength * active.moveY / 100.0F;
		float mz = strength * active.moveZ / 100.0F;
		float mt = strength * active.turnStrength / 100.0F;
		float v = flipVertical ? -1.0F : 1.0F;
		my *= v;
		float starting = Ease.inOutSine(progress(attack, 0.0F, 0.05F));
		float middle = Ease.outBack(progress(attack, 0.05F, 0.2F));
		float ending = Ease.inOutExpo(progress(attack, 0.4F, 1.0F));
		poseStack.translate(side * 0.1F * (starting - middle) * mx, -0.075F * (starting - ending) * my, 0.65F * (starting - middle) * mz);
		poseStack.rotateDegrees(Axis.XP, v * -70.0F * (starting - ending) * mt);
		poseStack.translate(0.0F, 0.0F, -0.25F * (ending - middle) * mz);
	}

	private static float progress(float time, float start, float end) {
		return Mth.clamp(Mth.inverseLerp(time, start, end), 0.0F, 1.0F);
	}

	private static float envelope(float t, float peak) {
		if (t <= peak) {
			return Mth.sin(t / peak * PI * 0.5F);
		}

		return Mth.cos((t - peak) / (1.0F - peak) * PI * 0.5F);
	}

	public static void afterArmTransform(PoseStack poseStack) {
		SwingSettings active = settings;
		if (active == null || !active.rotateSwing || holdApplied || !active.hasHoldAngles()) {
			return;
		}

		rotate(poseStack, -active.rotX, invert * active.rotY, invert * active.rotZ);
		holdApplied = true;
	}

	public static void applyItemPose(PoseStack poseStack) {
		SwingSettings active = settings;
		if (active == null) {
			return;
		}

		float pulse = 1.0F;
		if (swinging && active.hasSwingRotation()) {
			float amount = envelope(curved, active.peak / 100.0F);
			int side = flipSide ? -invert : invert;
			float v = flipVertical ? -1.0F : 1.0F;
			if (active.pushX != 0.0F || active.pushY != 0.0F || active.pushZ != 0.0F) {
				poseStack.translate(side * active.pushX * amount, v * active.pushY * amount, -active.pushZ * amount);
			}

			pulse = 1.0F + (active.pop / 100.0F - 1.0F) * Math.max(0.0F, amount);
			rotate(poseStack, v * -active.tilt * amount, side * active.turn * amount, v * side * active.roll * amount);
			if (active.hasSpin()) {
				float angle = 360.0F * active.spins * curved;
				switch (active.spinAxis) {
					case FLIP -> poseStack.rotateDegrees(Axis.XP, v * -angle);
					case TOP -> poseStack.rotateDegrees(Axis.YP, invert * angle);
					case SCREW -> poseStack.rotateDegrees(Axis.ZP, v * invert * angle);
					default -> {
					}
				}
			}

			if (active.wobble != 0.0F) {
				float fade = 1.0F - Mth.clamp(curved, 0.0F, 1.0F);
				poseStack.rotateDegrees(Axis.ZP, v * invert * active.wobble * fade * Mth.sin(curved * active.wobbleSpeed * PI * 2.0F));
			}
		}

		if (active.idleRock != 0.0F) {
			poseStack.rotateDegrees(Axis.ZP, invert * active.idleRock * (float) Math.sin(idleSeconds() * Math.PI * 0.7 * active.idleSpeed / 100.0));
		}

		if (!holdApplied && active.hasHoldAngles()) {
			rotate(poseStack, -active.rotX, invert * active.rotY, invert * active.rotZ);
		}

		if (active.hasScale() || pulse != 1.0F) {
			float size = active.size / 100.0F * pulse;
			poseStack.scale(size * active.stretchX / 100.0F, size * active.stretchY / 100.0F, size * active.stretchZ / 100.0F);
		}
	}

	public static void applyHandPose(PoseStack poseStack, SwingSettings active, int armInvert, float lagX, float lagY) {
		if (active.sway != 100) {
			float extra = (active.sway - 100) / 100.0F;
			poseStack.rotateDegrees(Axis.XP, lagX * 0.1F * extra);
			poseStack.rotateDegrees(Axis.YP, lagY * 0.1F * extra);
		}

		float idle = 0.0F;
		if (active.idleBob > 0.0F) {
			idle = active.idleBob * (float) Math.sin(idleSeconds() * Math.PI * active.idleSpeed / 100.0);
		}

		if (active.posX != 0.0F || active.posY != 0.0F || active.posZ != 0.0F || idle != 0.0F) {
			poseStack.translate(armInvert * active.posX, active.posY + idle, -active.posZ);
		}
	}

	private static double idleSeconds() {
		return (System.nanoTime() % 3_600_000_000_000L) / 1.0E9;
	}

	private static void rotate(PoseStack poseStack, float x, float y, float z) {
		if (x != 0.0F) {
			poseStack.rotateDegrees(Axis.XP, x);
		}

		if (y != 0.0F) {
			poseStack.rotateDegrees(Axis.YP, y);
		}

		if (z != 0.0F) {
			poseStack.rotateDegrees(Axis.ZP, z);
		}
	}
}

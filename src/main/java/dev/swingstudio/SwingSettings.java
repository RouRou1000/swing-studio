package dev.swingstudio;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.minecraft.util.Mth;

public final class SwingSettings {
	public static final int MIN_SPEED = 25;
	public static final int MAX_SPEED = 400;
	public static final int MAX_STRENGTH = 200;
	public static final float MAX_SWING_ANGLE = 180.0F;
	public static final int MAX_SPINS = 3;
	public static final float MAX_OFFSET = 0.6F;
	public static final int MIN_SIZE = 10;
	public static final int MAX_SIZE = 200;

	public enum Style {
		AUTO,
		SLASH,
		CHOP,
		SWEEP,
		UPPERCUT,
		STAB,
		PUNCH,
		NONE
	}

	public enum Direction {
		NORMAL,
		FLIPPED,
		ALTERNATE
	}

	public enum VerticalMode {
		NATURAL,
		DOWN,
		UP,
		ALTERNATE
	}

	public enum SpinAxis {
		NONE,
		FLIP,
		TOP,
		SCREW
	}

	public boolean custom = true;

	public int speed = 100;
	public int strength = 100;
	public Style style = Style.AUTO;
	public float x1 = 0.0F;
	public float y1 = 0.0F;
	public float x2 = 1.0F;
	public float y2 = 1.0F;
	public boolean mirror;
	public int hitPause;
	public int moveX = 100;
	public int moveY = 100;
	public int moveZ = 100;
	public int turnStrength = 100;
	public boolean reverse;
	public boolean alternate;
	@org.jspecify.annotations.Nullable
	public VerticalMode vertical;
	public boolean alternateVertical;
	public boolean flipVertical;
	public int variation;
	public float pushX;
	public float pushY;
	public float pushZ;
	public int pop = 100;

	public float tilt;
	public float turn;
	public float roll;
	public int peak = 25;
	public SpinAxis spinAxis = SpinAxis.NONE;
	public int spins = 1;
	public float wobble;
	public int wobbleSpeed = 3;

	public float posX;
	public float posY;
	public float posZ;
	public float rotX;
	public float rotY;
	public float rotZ;
	public boolean rotateSwing = true;
	public int size = 100;
	public int stretchX = 100;
	public int stretchY = 100;
	public int stretchZ = 100;

	public float idleBob;
	public int idleSpeed = 100;
	public int sway = 100;
	public float idleRock;

	public SwingSettings copy() {
		SwingSettings copy = new SwingSettings();
		copy.copyFrom(this);
		return copy;
	}

	public void copyFrom(SwingSettings other) {
		try {
			for (Field field : SwingSettings.class.getDeclaredFields()) {
				if (!Modifier.isStatic(field.getModifiers())) {
					field.set(this, field.get(other));
				}
			}
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	public void copyAnimationFrom(SwingSettings other) {
		SwingSettings hold = this.copy();
		this.copyFrom(other);
		this.posX = hold.posX;
		this.posY = hold.posY;
		this.posZ = hold.posZ;
		this.rotX = hold.rotX;
		this.rotY = hold.rotY;
		this.rotZ = hold.rotZ;
		this.rotateSwing = hold.rotateSwing;
		this.size = hold.size;
		this.stretchX = hold.stretchX;
		this.stretchY = hold.stretchY;
		this.stretchZ = hold.stretchZ;
	}

	public Direction sideDirection() {
		return this.alternate ? Direction.ALTERNATE : this.mirror ? Direction.FLIPPED : Direction.NORMAL;
	}

	public void setSideDirection(Direction direction) {
		this.mirror = direction == Direction.FLIPPED;
		this.alternate = direction == Direction.ALTERNATE;
	}

	public VerticalMode verticalMode() {
		return this.vertical == null ? VerticalMode.NATURAL : this.vertical;
	}

	public boolean upsideDown(int swingIndex, boolean alternatingBothWays) {
		return switch (this.verticalMode()) {
			case NATURAL -> false;
			case DOWN -> this.naturallyUp();
			case UP -> !this.naturallyUp();
			case ALTERNATE -> alternatingBothWays ? swingIndex / 2 % 2 == 1 : swingIndex % 2 == 1;
		};
	}

	public boolean naturallyUp() {
		return this.style == Style.UPPERCUT;
	}

	public boolean hasSpin() {
		return this.spinAxis != SpinAxis.NONE && this.spins != 0;
	}

	public boolean hasSwingRotation() {
		return this.tilt != 0.0F || this.turn != 0.0F || this.roll != 0.0F || this.hasSpin() || this.wobble != 0.0F
			|| this.pushX != 0.0F || this.pushY != 0.0F || this.pushZ != 0.0F || this.pop != 100;
	}

	public boolean hasHoldAngles() {
		return this.rotX != 0.0F || this.rotY != 0.0F || this.rotZ != 0.0F;
	}

	public boolean hasScale() {
		return this.size != 100 || this.stretchX != 100 || this.stretchY != 100 || this.stretchZ != 100;
	}

	public float curve(float t) {
		return Curve.evaluate(this.x1, this.y1, this.x2, this.y2, t);
	}

	public float shape(float t) {
		if (this.reverse) {
			t = 1.0F - t;
		}

		if (this.hitPause > 0) {
			float pause = this.hitPause / 100.0F;
			float hit = 0.25F;
			float before = hit * (1.0F - pause);
			if (t < before) {
				t /= 1.0F - pause;
			} else if (t < before + pause) {
				t = hit;
			} else {
				t = (t - pause) / (1.0F - pause);
			}
		}

		return this.curve(t);
	}

	public void setCurve(Curve.Preset preset) {
		this.x1 = preset.x1;
		this.y1 = preset.y1;
		this.x2 = preset.x2;
		this.y2 = preset.y2;
	}

	public SwingSettings sanitize() {
		this.speed = Mth.clamp(this.speed, MIN_SPEED, MAX_SPEED);
		this.strength = Mth.clamp(this.strength, 0, MAX_STRENGTH);
		if (this.style == null) {
			this.style = Style.AUTO;
		}

		this.x1 = clean(this.x1, 0.0F, 1.0F, 0.0F);
		this.x2 = clean(this.x2, 0.0F, 1.0F, 1.0F);
		this.y1 = clean(this.y1, Curve.MIN_Y, Curve.MAX_Y, 0.0F);
		this.y2 = clean(this.y2, Curve.MIN_Y, Curve.MAX_Y, 1.0F);
		this.hitPause = Mth.clamp(this.hitPause, 0, 60);
		this.moveX = Mth.clamp(this.moveX, -100, 300);
		this.moveY = Mth.clamp(this.moveY, -100, 300);
		this.moveZ = Mth.clamp(this.moveZ, -100, 300);
		this.turnStrength = Mth.clamp(this.turnStrength, -100, 300);
		this.variation = Mth.clamp(this.variation, 0, 60);
		this.pushX = clean(this.pushX, -0.5F, 0.5F, 0.0F);
		this.pushY = clean(this.pushY, -0.5F, 0.5F, 0.0F);
		this.pushZ = clean(this.pushZ, -0.5F, 0.5F, 0.0F);
		this.pop = Mth.clamp(this.pop, 25, 250);
		this.tilt = clean(this.tilt, -MAX_SWING_ANGLE, MAX_SWING_ANGLE, 0.0F);
		this.turn = clean(this.turn, -MAX_SWING_ANGLE, MAX_SWING_ANGLE, 0.0F);
		this.roll = clean(this.roll, -MAX_SWING_ANGLE, MAX_SWING_ANGLE, 0.0F);
		this.peak = Mth.clamp(this.peak, 5, 90);
		if (this.spinAxis == null) {
			this.spinAxis = SpinAxis.NONE;
		}

		if (this.vertical == null) {
			this.vertical = this.alternateVertical ? VerticalMode.ALTERNATE : this.flipVertical ? VerticalMode.UP : VerticalMode.NATURAL;
		}

		this.alternateVertical = false;
		this.flipVertical = false;
		this.spins = Mth.clamp(this.spins, -MAX_SPINS, MAX_SPINS);
		if (this.spins == 0) {
			this.spins = 1;
		}

		this.wobble = clean(this.wobble, 0.0F, 45.0F, 0.0F);
		this.wobbleSpeed = Mth.clamp(this.wobbleSpeed, 1, 8);
		this.posX = clean(this.posX, -MAX_OFFSET, MAX_OFFSET, 0.0F);
		this.posY = clean(this.posY, -MAX_OFFSET, MAX_OFFSET, 0.0F);
		this.posZ = clean(this.posZ, -MAX_OFFSET, MAX_OFFSET, 0.0F);
		this.rotX = clean(this.rotX, -180.0F, 180.0F, 0.0F);
		this.rotY = clean(this.rotY, -180.0F, 180.0F, 0.0F);
		this.rotZ = clean(this.rotZ, -180.0F, 180.0F, 0.0F);
		this.size = Mth.clamp(this.size, MIN_SIZE, MAX_SIZE);
		this.stretchX = Mth.clamp(this.stretchX, 25, 300);
		this.stretchY = Mth.clamp(this.stretchY, 25, 300);
		this.stretchZ = Mth.clamp(this.stretchZ, 25, 300);
		this.idleBob = clean(this.idleBob, 0.0F, 0.1F, 0.0F);
		this.idleSpeed = Mth.clamp(this.idleSpeed, 25, 400);
		this.sway = Mth.clamp(this.sway, 0, 400);
		this.idleRock = clean(this.idleRock, 0.0F, 20.0F, 0.0F);
		return this;
	}

	private static float clean(float value, float min, float max, float fallback) {
		return Float.isFinite(value) ? Mth.clamp(value, min, max) : fallback;
	}
}

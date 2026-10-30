package dev.swingstudio;

import dev.swingstudio.SwingSettings.SpinAxis;
import dev.swingstudio.SwingSettings.Style;
import java.util.List;
import java.util.function.UnaryOperator;

public final class Presets {
	public enum Category {
		CLASSIC(0xFF2E4C80),
		PVP(0xFFB0382A),
		WEAPONS(0xFF4E7A3A),
		SMOOTH(0xFFC08A1E),
		CRAZY(0xFF7A3E6E);

		public final int color;

		Category(int color) {
			this.color = color;
		}
	}

	public record Preset(String id, Category category, UnaryOperator<Spec> spec, boolean withHold) {
		public Preset(String id, Category category, UnaryOperator<Spec> spec) {
			this(id, category, spec, false);
		}

		public SwingSettings build() {
			return this.spec.apply(new Spec()).settings.sanitize();
		}

		public String nameKey() {
			return "swingstudio.p." + this.id;
		}

		public String descriptionKey() {
			return "swingstudio.p." + this.id + ".desc";
		}
	}

	public static final class Spec {
		final SwingSettings settings = new SwingSettings();

		Spec speed(int percent) {
			this.settings.speed = percent;
			return this;
		}

		Spec strength(int percent) {
			this.settings.strength = percent;
			return this;
		}

		Spec style(Style style) {
			this.settings.style = style;
			return this;
		}

		Spec curve(Curve.Preset curve) {
			this.settings.setCurve(curve);
			return this;
		}

		Spec tilt(float degrees) {
			this.settings.tilt = degrees;
			return this;
		}

		Spec turn(float degrees) {
			this.settings.turn = degrees;
			return this;
		}

		Spec roll(float degrees) {
			this.settings.roll = degrees;
			return this;
		}

		Spec peak(int percent) {
			this.settings.peak = percent;
			return this;
		}

		Spec spin(SpinAxis axis, int turns) {
			this.settings.spinAxis = axis;
			this.settings.spins = turns;
			return this;
		}

		Spec wobble(float degrees, int speed) {
			this.settings.wobble = degrees;
			this.settings.wobbleSpeed = speed;
			return this;
		}

		Spec pause(int percent) {
			this.settings.hitPause = percent;
			return this;
		}

		Spec mirror() {
			this.settings.mirror = true;
			return this;
		}

		Spec move(int x, int y, int z) {
			this.settings.moveX = x;
			this.settings.moveY = y;
			this.settings.moveZ = z;
			return this;
		}

		Spec turnStrength(int percent) {
			this.settings.turnStrength = percent;
			return this;
		}

		Spec reverse() {
			this.settings.reverse = true;
			return this;
		}

		Spec alternate() {
			this.settings.alternate = true;
			return this;
		}

		Spec alternateVertical() {
			this.settings.vertical = SwingSettings.VerticalMode.ALTERNATE;
			return this;
		}

		Spec variation(int percent) {
			this.settings.variation = percent;
			return this;
		}

		Spec push(float x, float y, float z) {
			this.settings.pushX = x;
			this.settings.pushY = y;
			this.settings.pushZ = z;
			return this;
		}

		Spec pop(int percent) {
			this.settings.pop = percent;
			return this;
		}

		Spec hold(float posX, float posY, float posZ, float rotX, float rotY, float rotZ, int size, boolean rotateSwing) {
			this.settings.posX = posX;
			this.settings.posY = posY;
			this.settings.posZ = posZ;
			this.settings.rotX = rotX;
			this.settings.rotY = rotY;
			this.settings.rotZ = rotZ;
			this.settings.size = size;
			this.settings.rotateSwing = rotateSwing;
			return this;
		}

		Spec curve(float x1, float y1, float x2, float y2) {
			this.settings.x1 = x1;
			this.settings.y1 = y1;
			this.settings.x2 = x2;
			this.settings.y2 = y2;
			return this;
		}
	}

	public static final List<Preset> ALL = List.of(
		new Preset("vanilla", Category.CLASSIC, s -> s),
		new Preset("smooth", Category.CLASSIC, s -> s.speed(90).curve(Curve.Preset.SMOOTH)),
		new Preset("quick", Category.CLASSIC, s -> s.speed(150).curve(Curve.Preset.FAST_START)),
		new Preset("heavy", Category.CLASSIC, s -> s.speed(60).strength(120).curve(Curve.Preset.SLOW_START).tilt(15)),
		new Preset("lazy", Category.CLASSIC, s -> s.speed(55).strength(70).curve(Curve.Preset.SMOOTH)),
		new Preset("snappy", Category.CLASSIC, s -> s.speed(120).curve(Curve.Preset.SNAP)),
		new Preset("soft", Category.CLASSIC, s -> s.strength(60).curve(Curve.Preset.SMOOTH)),
		new Preset("wide", Category.CLASSIC, s -> s.speed(90).strength(160)),
		new Preset("short", Category.CLASSIC, s -> s.strength(45).speed(110).curve(Curve.Preset.SMOOTH)),
		new Preset("organic", Category.CLASSIC, s -> s.variation(30).curve(Curve.Preset.SMOOTH)),
		new Preset("old_school", Category.CLASSIC, s -> s.speed(110).strength(115).curve(Curve.Preset.FAST_START).turnStrength(120)),
		new Preset("slow_pvp", Category.PVP, s -> s.style(Style.NONE).speed(62).curve(0.118F, 0.0F, 0.179F, 0.836F).peak(5)
			.tilt(138.7F).turn(49.2F).roll(-15.4F).push(-0.081F, -0.0085F, 0.0F)
			.hold(-0.044F, 0.061F, -0.034F, 81.2F, -17.9F, 49.2F, 44, false), true),
		new Preset("pvp_clean", Category.PVP, s -> s.speed(130).strength(80).curve(Curve.Preset.FAST_START)),
		new Preset("pvp_tight", Category.PVP, s -> s.speed(140).strength(55)),
		new Preset("pvp_turbo", Category.PVP, s -> s.speed(220).strength(70).curve(Curve.Preset.SNAP)),
		new Preset("blockhit", Category.PVP, s -> s.speed(120).strength(110).tilt(20).roll(-20)),
		new Preset("crit", Category.PVP, s -> s.style(Style.CHOP).speed(110).pause(15).curve(Curve.Preset.OVERSHOOT)),
		new Preset("combo", Category.PVP, s -> s.style(Style.SWEEP).speed(170).strength(80)),
		new Preset("jab", Category.PVP, s -> s.style(Style.PUNCH).speed(160)),
		new Preset("low_profile", Category.PVP, s -> s.speed(120).strength(35)),
		new Preset("hitstop", Category.PVP, s -> s.pause(30).curve(Curve.Preset.SNAP)),
		new Preset("combo_lr", Category.PVP, s -> s.alternate().speed(150).strength(80)),
		new Preset("combo_ud", Category.PVP, s -> s.alternateVertical().speed(150).strength(85)),
		new Preset("combo_four", Category.PVP, s -> s.alternate().alternateVertical().speed(160).strength(80)),
		new Preset("flat", Category.PVP, s -> s.speed(130).move(100, 20, 100).turnStrength(60)),
		new Preset("lumberjack", Category.WEAPONS, s -> s.style(Style.CHOP).speed(80).strength(120).pause(10)),
		new Preset("hammer", Category.WEAPONS, s -> s.style(Style.CHOP).speed(70).tilt(50).curve(Curve.Preset.WIND_UP)),
		new Preset("mace_slam", Category.WEAPONS, s -> s.style(Style.CHOP).speed(60).strength(150).pause(20).curve(Curve.Preset.OVERSHOOT)),
		new Preset("spear", Category.WEAPONS, s -> s.style(Style.STAB).speed(110)),
		new Preset("rapier", Category.WEAPONS, s -> s.style(Style.STAB).speed(160).strength(80).tilt(-10)),
		new Preset("dagger", Category.WEAPONS, s -> s.style(Style.PUNCH).speed(190).roll(20)),
		new Preset("katana", Category.WEAPONS, s -> s.style(Style.SWEEP).speed(130).turn(15).curve(Curve.Preset.SNAP)),
		new Preset("scythe", Category.WEAPONS, s -> s.style(Style.SWEEP).mirror().speed(85).strength(130).roll(30)),
		new Preset("uppercut", Category.WEAPONS, s -> s.style(Style.UPPERCUT).speed(120)),
		new Preset("shield_bash", Category.WEAPONS, s -> s.style(Style.PUNCH).speed(110).strength(120)),
		new Preset("miner", Category.WEAPONS, s -> s.style(Style.CHOP).speed(120).strength(80)),
		new Preset("lunge", Category.WEAPONS, s -> s.style(Style.STAB).speed(120).push(0.0F, 0.0F, 0.25F)),
		new Preset("backhand", Category.WEAPONS, s -> s.mirror().speed(120)),
		new Preset("cinematic", Category.SMOOTH, s -> s.speed(50).strength(120).curve(Curve.Preset.SMOOTH)),
		new Preset("silk", Category.SMOOTH, s -> s.speed(80).curve(Curve.Preset.BOTH).wobble(4, 3)),
		new Preset("floaty", Category.SMOOTH, s -> s.speed(70).curve(Curve.Preset.SMOOTH).tilt(-10).wobble(3, 2)),
		new Preset("elastic", Category.SMOOTH, s -> s.curve(Curve.Preset.OVERSHOOT).wobble(10, 4)),
		new Preset("wind_up", Category.SMOOTH, s -> s.strength(120).curve(Curve.Preset.WIND_UP)),
		new Preset("whip", Category.SMOOTH, s -> s.speed(140).tilt(25).wobble(8, 5).curve(Curve.Preset.SNAP)),
		new Preset("pendulum", Category.SMOOTH, s -> s.style(Style.SWEEP).speed(70).wobble(6, 2).curve(Curve.Preset.SMOOTH)),
		new Preset("bounce", Category.SMOOTH, s -> s.style(Style.UPPERCUT).speed(110).curve(Curve.Preset.OVERSHOOT)),
		new Preset("late_turn", Category.SMOOTH, s -> s.tilt(30).peak(70).curve(Curve.Preset.SMOOTH)),
		new Preset("pop", Category.SMOOTH, s -> s.speed(110).pop(140).curve(Curve.Preset.OVERSHOOT)),
		new Preset("slow_motion", Category.SMOOTH, s -> s.speed(25).strength(130).curve(Curve.Preset.SMOOTH)),
		new Preset("flip", Category.CRAZY, s -> s.spin(SpinAxis.FLIP, 1)),
		new Preset("double_flip", Category.CRAZY, s -> s.speed(90).spin(SpinAxis.FLIP, 2)),
		new Preset("backflip", Category.CRAZY, s -> s.style(Style.UPPERCUT).spin(SpinAxis.FLIP, -1)),
		new Preset("propeller", Category.CRAZY, s -> s.strength(50).spin(SpinAxis.TOP, 2)),
		new Preset("drill", Category.CRAZY, s -> s.style(Style.PUNCH).spin(SpinAxis.SCREW, 3)),
		new Preset("tornado", Category.CRAZY, s -> s.style(Style.SWEEP).speed(80).spin(SpinAxis.TOP, 3)),
		new Preset("boomerang", Category.CRAZY, s -> s.style(Style.SWEEP).mirror().spin(SpinAxis.SCREW, -2)),
		new Preset("jelly", Category.CRAZY, s -> s.wobble(25, 6)),
		new Preset("chaos", Category.CRAZY, s -> s.speed(130).tilt(60).turn(-45).roll(80).spin(SpinAxis.SCREW, 1).wobble(12, 5)),
		new Preset("rewind", Category.CRAZY, s -> s.reverse()),
		new Preset("hyper", Category.CRAZY, s -> s.speed(400).curve(Curve.Preset.SNAP)),
		new Preset("robot", Category.CRAZY, s -> s.speed(90).pause(40).curve(Curve.Preset.SNAP)),
		new Preset("ghost", Category.CRAZY, s -> s.style(Style.NONE).tilt(10).wobble(6, 3)),
		new Preset("statue", Category.CRAZY, s -> s.style(Style.NONE).strength(0))
	);

	private Presets() {
	}

	public static void apply(SwingSettings target, Preset preset, boolean keepHold, boolean inverted) {
		SwingSettings built = preset.build();
		if (inverted) {
			invert(built);
		}

		if (keepHold && !preset.withHold()) {
			target.copyAnimationFrom(built);
		} else {
			target.copyFrom(built);
		}

		target.custom = true;
	}

	public static void invert(SwingSettings settings) {
		settings.mirror = !settings.mirror;
		settings.tilt = -settings.tilt;
		settings.turn = -settings.turn;
		settings.roll = -settings.roll;
		settings.spins = -settings.spins;
		settings.pushX = -settings.pushX;
	}
}

package dev.swingstudio.screen;

import dev.swingstudio.Curve;
import dev.swingstudio.Level;
import dev.swingstudio.SwingConfig;
import dev.swingstudio.SwingSettings;
import dev.swingstudio.SwingSettings.SpinAxis;
import dev.swingstudio.SwingSettings.Style;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

final class Options {
	enum Page {
		PRESETS(Paper.RED),
		SWING(Paper.BLUE),
		ROTATION(Paper.PLUM),
		HOLD(Paper.GREEN),
		IDLE(Paper.MUSTARD),
		CONFIGS(0xFF2F6B6B),
		MORE(Paper.INK_SOFT);

		final int color;

		Page(int color) {
			this.color = color;
		}

		Component title() {
			return Component.translatable("swingstudio.page." + this.name().toLowerCase(Locale.ROOT));
		}
	}

	interface Context {
		SwingSettings view();

		void edit(Consumer<SwingSettings> change);

		void beginChange();

		void refresh();

		void copySettings();

		void pasteSettings();

		boolean canPaste();

		void clearAllItems();
	}

	abstract static class Option {
		final String id;
		final Page page;
		final Level level;

		Option(String id, Page page, Level level) {
			this.id = id;
			this.page = page;
			this.level = level;
		}

		Component label() {
			return Component.translatable("swingstudio." + this.id);
		}

		@Nullable
		Component tip() {
			String key = "swingstudio." + this.id + ".tip";
			return Language.getInstance().has(key) ? Component.translatable(key) : null;
		}

		boolean searchable() {
			return true;
		}

		String haystack() {
			StringBuilder text = new StringBuilder(this.label().getString()).append(' ');
			Component tip = this.tip();
			if (tip != null) {
				text.append(tip.getString()).append(' ');
			}

			String keys = "swingstudio." + this.id + ".keys";
			if (Language.getInstance().has(keys)) {
				text.append(Component.translatable(keys).getString()).append(' ');
			}

			return text.append(this.page.title().getString()).toString().toLowerCase(Locale.ROOT);
		}

		boolean changed(SwingSettings settings) {
			return false;
		}

		abstract List<AbstractWidget> build(Context context, int x, int y, int width);
	}

	static final class HeadingOption extends Option {
		HeadingOption(String id, Page page, Level level) {
			super(id, page, level);
		}

		@Override
		boolean searchable() {
			return false;
		}

		@Override
		List<AbstractWidget> build(Context context, int x, int y, int width) {
			return List.of(new Heading(x, y, width, this.label(), this.page.color));
		}
	}

	static final class SliderOption extends Option {
		private final double min;
		private final double max;
		private final double step;
		private final ToDoubleFunction<SwingSettings> get;
		private final BiConsumer<SwingSettings, Double> set;
		private final DoubleFunction<String> format;
		@Nullable
		private Predicate<SwingSettings> enabledWhen;

		SliderOption(String id, Page page, Level level, double min, double max, double step, ToDoubleFunction<SwingSettings> get, BiConsumer<SwingSettings, Double> set, DoubleFunction<String> format) {
			super(id, page, level);
			this.min = min;
			this.max = max;
			this.step = step;
			this.get = get;
			this.set = set;
			this.format = format;
		}

		SliderOption enabledWhen(Predicate<SwingSettings> condition) {
			this.enabledWhen = condition;
			return this;
		}

		private double defaultValue() {
			return this.get.applyAsDouble(DEFAULTS);
		}

		@Override
		boolean changed(SwingSettings settings) {
			return Math.abs(this.get.applyAsDouble(settings) - this.defaultValue()) > this.step / 2.0;
		}

		@Override
		List<AbstractWidget> build(Context context, int x, int y, int width) {
			Predicate<SwingSettings> condition = this.enabledWhen;
			RulerSlider slider = new RulerSlider(
				x, y, width, this.label(), this.min, this.max, this.step, this.defaultValue(),
				() -> this.get.applyAsDouble(context.view()),
				value -> context.edit(settings -> this.set.accept(settings, value)),
				this.format
			).tooltip(this.tip()).enabledWhen(condition == null ? null : () -> condition.test(context.view()));
			return List.of(slider);
		}
	}

	static final class ToggleOption extends Option {
		private final Predicate<SwingSettings> get;
		private final BiConsumer<SwingSettings, Boolean> set;

		ToggleOption(String id, Page page, Level level, Predicate<SwingSettings> get, BiConsumer<SwingSettings, Boolean> set) {
			super(id, page, level);
			this.get = get;
			this.set = set;
		}

		@Override
		boolean changed(SwingSettings settings) {
			return this.get.test(settings) != this.get.test(DEFAULTS);
		}

		@Override
		List<AbstractWidget> build(Context context, int x, int y, int width) {
			return List.of(new CheckToggle(x, y, width, this::label, () -> this.get.test(context.view()), () -> {
				boolean next = !this.get.test(context.view());
				context.edit(settings -> this.set.accept(settings, next));
			}, this.tip()));
		}
	}

	static final class ChoiceOption<T extends Enum<T>> extends Option {
		private final T[] values;
		private final String prefix;
		private final Function<SwingSettings, T> get;
		private final BiConsumer<SwingSettings, T> set;

		ChoiceOption(String id, Page page, Level level, T[] values, Function<SwingSettings, T> get, BiConsumer<SwingSettings, T> set) {
			super(id, page, level);
			this.values = values;
			this.prefix = "swingstudio." + id + ".";
			this.get = get;
			this.set = set;
		}

		@Override
		boolean changed(SwingSettings settings) {
			return this.get.apply(settings) != this.get.apply(DEFAULTS);
		}

		@Override
		String haystack() {
			StringBuilder text = new StringBuilder(super.haystack());
			for (T value : this.values) {
				text.append(' ').append(Component.translatable(this.prefix + value.name().toLowerCase(Locale.ROOT)).getString().toLowerCase(Locale.ROOT));
			}

			return text.toString();
		}

		@Override
		List<AbstractWidget> build(Context context, int x, int y, int width) {
			return List.of(new ChoiceRow<>(
				x, y, width, this.label(), this.tip(), this.values,
				value -> Component.translatable(this.prefix + value.name().toLowerCase(Locale.ROOT)),
				value -> Component.translatable(this.prefix + value.name().toLowerCase(Locale.ROOT) + ".tip"),
				() -> this.get.apply(context.view()),
				value -> context.edit(settings -> this.set.accept(settings, value))
			));
		}
	}

	static final class GlobalToggle extends Option {
		private final BooleanSupplier get;
		private final Runnable toggle;

		GlobalToggle(String id, Page page, Level level, BooleanSupplier get, Runnable toggle) {
			super(id, page, level);
			this.get = get;
			this.toggle = toggle;
		}

		@Override
		List<AbstractWidget> build(Context context, int x, int y, int width) {
			return List.of(new CheckToggle(x, y, width, this::label, this.get, () -> {
				context.beginChange();
				this.toggle.run();
				SwingConfig.save();
			}, this.tip()));
		}
	}

	static final class CustomOption extends Option {
		interface Builder {
			List<AbstractWidget> build(Context context, int x, int y, int width);
		}

		private final Builder builder;

		CustomOption(String id, Page page, Level level, Builder builder) {
			super(id, page, level);
			this.builder = builder;
		}

		@Override
		List<AbstractWidget> build(Context context, int x, int y, int width) {
			return this.builder.build(context, x, y, width);
		}
	}

	static final List<Option> ALL = new ArrayList<>();
	static final SwingSettings DEFAULTS = new SwingSettings();

	private Options() {
	}

	private static String percent(double value) {
		return Math.round(value) + " %";
	}

	private static String degrees(double value) {
		long rounded = Math.round(value);
		return (rounded > 0 ? "+" : "") + rounded + "°";
	}

	private static String blocks(double value) {
		return String.format(Locale.ROOT, "%+.2f", value);
	}

	private static String plain(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private static String times(double value) {
		return Math.round(value) + "×";
	}

	private static void heading(String id, Page page, Level level) {
		ALL.add(new HeadingOption("h." + id, page, level));
	}

	private static SliderOption slider(String id, Page page, Level level, double min, double max, double step, ToDoubleFunction<SwingSettings> get, BiConsumer<SwingSettings, Double> set, DoubleFunction<String> format) {
		SliderOption option = new SliderOption(id, page, level, min, max, step, get, set, format);
		ALL.add(option);
		return option;
	}

	private static void toggle(String id, Page page, Level level, Predicate<SwingSettings> get, BiConsumer<SwingSettings, Boolean> set) {
		ALL.add(new ToggleOption(id, page, level, get, set));
	}

	private static void global(String id, Page page, Level level, BooleanSupplier get, Runnable toggle) {
		ALL.add(new GlobalToggle(id, page, level, get, toggle));
	}

	private static int round(double value) {
		return (int) Math.round(value);
	}

	static {
		Page page = Page.SWING;
		heading("tempo", page, Level.EASY);
		slider("speed", page, Level.EASY, SwingSettings.MIN_SPEED, SwingSettings.MAX_SPEED, 5, s -> s.speed, (s, v) -> s.speed = round(v), Options::percent);
		slider("strength", page, Level.EASY, 0, SwingSettings.MAX_STRENGTH, 5, s -> s.strength, (s, v) -> s.strength = round(v), Options::percent);
		ALL.add(new CustomOption("curve_presets", page, Level.EASY, (context, x, y, width) -> List.of(
			new CurvePresetRow(x, y, width, context::view, preset -> context.edit(settings -> settings.setCurve(preset)))
		)));
		ALL.add(new CustomOption("curve_graph", page, Level.NORMAL, (context, x, y, width) -> List.of(
			new CurveEditor(x, y + 2, width - 2, 76, context::view, context::edit)
		)));
		heading("motion", page, Level.NORMAL);
		ALL.add(new ChoiceOption<>("style", page, Level.NORMAL, Style.values(), s -> s.style, (s, v) -> s.style = v));
		ALL.add(new ChoiceOption<>("direction_vertical", page, Level.NORMAL, SwingSettings.VerticalMode.values(), SwingSettings::verticalMode, (s, v) -> s.vertical = v));
		ALL.add(new ChoiceOption<>("direction_side", page, Level.NORMAL, SwingSettings.Direction.values(), SwingSettings::sideDirection, SwingSettings::setSideDirection));
		toggle("reverse", page, Level.HARD, s -> s.reverse, (s, v) -> s.reverse = v);
		slider("hit_pause", page, Level.HARD, 0, 60, 5, s -> s.hitPause, (s, v) -> s.hitPause = round(v), Options::percent);
		slider("variation", page, Level.HARD, 0, 60, 5, s -> s.variation, (s, v) -> s.variation = round(v), Options::percent);
		heading("push", page, Level.HARD);
		slider("push_x", page, Level.HARD, -0.5, 0.5, 0.01, s -> s.pushX, (s, v) -> s.pushX = v.floatValue(), Options::blocks);
		slider("push_y", page, Level.HARD, -0.5, 0.5, 0.01, s -> s.pushY, (s, v) -> s.pushY = v.floatValue(), Options::blocks);
		slider("push_z", page, Level.HARD, -0.5, 0.5, 0.01, s -> s.pushZ, (s, v) -> s.pushZ = v.floatValue(), Options::blocks);
		slider("pop", page, Level.HARD, 25, 250, 5, s -> s.pop, (s, v) -> s.pop = round(v), Options::percent);
		heading("fine", page, Level.EXPERT);
		slider("move_x", page, Level.EXPERT, -100, 300, 5, s -> s.moveX, (s, v) -> s.moveX = round(v), Options::percent);
		slider("move_y", page, Level.EXPERT, -100, 300, 5, s -> s.moveY, (s, v) -> s.moveY = round(v), Options::percent);
		slider("move_z", page, Level.EXPERT, -100, 300, 5, s -> s.moveZ, (s, v) -> s.moveZ = round(v), Options::percent);
		slider("turn_strength", page, Level.EXPERT, -100, 300, 5, s -> s.turnStrength, (s, v) -> s.turnStrength = round(v), Options::percent);
		heading("curve_numbers", page, Level.EXPERT);
		slider("curve_x1", page, Level.EXPERT, 0, 1, 0.01, s -> s.x1, (s, v) -> s.x1 = v.floatValue(), Options::plain);
		slider("curve_y1", page, Level.EXPERT, Curve.MIN_Y, Curve.MAX_Y, 0.01, s -> s.y1, (s, v) -> s.y1 = v.floatValue(), Options::plain);
		slider("curve_x2", page, Level.EXPERT, 0, 1, 0.01, s -> s.x2, (s, v) -> s.x2 = v.floatValue(), Options::plain);
		slider("curve_y2", page, Level.EXPERT, Curve.MIN_Y, Curve.MAX_Y, 0.01, s -> s.y2, (s, v) -> s.y2 = v.floatValue(), Options::plain);

		page = Page.ROTATION;
		float angle = SwingSettings.MAX_SWING_ANGLE;
		heading("swing_rot", page, Level.NORMAL);
		slider("tilt", page, Level.NORMAL, -angle, angle, 5, s -> s.tilt, (s, v) -> s.tilt = v.floatValue(), Options::degrees);
		slider("turn", page, Level.NORMAL, -angle, angle, 5, s -> s.turn, (s, v) -> s.turn = v.floatValue(), Options::degrees);
		slider("roll", page, Level.NORMAL, -angle, angle, 5, s -> s.roll, (s, v) -> s.roll = v.floatValue(), Options::degrees);
		slider("peak", page, Level.EXPERT, 5, 90, 5, s -> s.peak, (s, v) -> s.peak = round(v), Options::percent);
		heading("spin", page, Level.NORMAL);
		ALL.add(new ChoiceOption<>("spin", page, Level.NORMAL, SpinAxis.values(), s -> s.spinAxis, (s, v) -> s.spinAxis = v));
		slider("spins", page, Level.NORMAL, -SwingSettings.MAX_SPINS, SwingSettings.MAX_SPINS, 1, s -> s.spins, (s, v) -> {
			int turns = round(v);
			s.spins = turns == 0 ? (s.spins > 0 ? -1 : 1) : turns;
		}, value -> (value > 0 ? "+" : "") + Math.round(value) + "×").enabledWhen(s -> s.spinAxis != SpinAxis.NONE);
		heading("wobble", page, Level.HARD);
		slider("wobble", page, Level.HARD, 0, 45, 1, s -> s.wobble, (s, v) -> s.wobble = v.floatValue(), Options::degrees);
		slider("wobble_speed", page, Level.HARD, 1, 8, 1, s -> s.wobbleSpeed, (s, v) -> s.wobbleSpeed = round(v), Options::times)
			.enabledWhen(s -> s.wobble != 0.0F);

		page = Page.HOLD;
		float offset = SwingSettings.MAX_OFFSET;
		heading("position", page, Level.NORMAL);
		slider("pos_x", page, Level.NORMAL, -offset, offset, 0.01, s -> s.posX, (s, v) -> s.posX = v.floatValue(), Options::blocks);
		slider("pos_y", page, Level.NORMAL, -offset, offset, 0.01, s -> s.posY, (s, v) -> s.posY = v.floatValue(), Options::blocks);
		slider("pos_z", page, Level.NORMAL, -offset, offset, 0.01, s -> s.posZ, (s, v) -> s.posZ = v.floatValue(), Options::blocks);
		heading("angle", page, Level.NORMAL);
		slider("rot_x", page, Level.NORMAL, -180, 180, 5, s -> s.rotX, (s, v) -> s.rotX = v.floatValue(), Options::degrees);
		slider("rot_y", page, Level.NORMAL, -180, 180, 5, s -> s.rotY, (s, v) -> s.rotY = v.floatValue(), Options::degrees);
		slider("rot_z", page, Level.NORMAL, -180, 180, 5, s -> s.rotZ, (s, v) -> s.rotZ = v.floatValue(), Options::degrees);
		toggle("rotate_swing", page, Level.HARD, s -> s.rotateSwing, (s, v) -> s.rotateSwing = v);
		heading("size", page, Level.EASY);
		slider("size", page, Level.EASY, SwingSettings.MIN_SIZE, SwingSettings.MAX_SIZE, 5, s -> s.size, (s, v) -> s.size = round(v), Options::percent);
		slider("stretch_x", page, Level.EXPERT, 25, 300, 5, s -> s.stretchX, (s, v) -> s.stretchX = round(v), Options::percent);
		slider("stretch_y", page, Level.EXPERT, 25, 300, 5, s -> s.stretchY, (s, v) -> s.stretchY = round(v), Options::percent);
		slider("stretch_z", page, Level.EXPERT, 25, 300, 5, s -> s.stretchZ, (s, v) -> s.stretchZ = round(v), Options::percent);

		page = Page.IDLE;
		heading("idle", page, Level.HARD);
		slider("idle_bob", page, Level.HARD, 0, 0.1, 0.005, s -> s.idleBob, (s, v) -> s.idleBob = v.floatValue(), value -> String.format(Locale.ROOT, "%.3f", value));
		slider("idle_speed", page, Level.HARD, 25, 400, 5, s -> s.idleSpeed, (s, v) -> s.idleSpeed = round(v), Options::percent)
			.enabledWhen(s -> s.idleBob > 0.0F || s.idleRock > 0.0F);
		slider("idle_rock", page, Level.HARD, 0, 20, 1, s -> s.idleRock, (s, v) -> s.idleRock = v.floatValue(), Options::degrees);
		heading("sway", page, Level.HARD);
		slider("sway", page, Level.HARD, 0, 400, 10, s -> s.sway, (s, v) -> s.sway = round(v), Options::percent);

		page = Page.MORE;
		heading("general", page, Level.EASY);
		global("equip", page, Level.EASY, () -> SwingConfig.get().equipAnimation, () -> SwingConfig.get().equipAnimation ^= true);
		global("keep_hold", page, Level.NORMAL, () -> SwingConfig.get().keepHoldOnPreset, () -> SwingConfig.get().keepHoldOnPreset ^= true);
		global("auto_preview", page, Level.NORMAL, () -> SwingConfig.get().autoPreview, () -> SwingConfig.get().autoPreview ^= true);
		ALL.add(new CustomOption("preview_pause", page, Level.EXPERT, (context, x, y, width) -> List.of(new RulerSlider(
			x, y, width, Component.translatable("swingstudio.preview_pause"), 0, 60, 2, 12,
			() -> SwingConfig.get().previewPause,
			value -> {
				context.beginChange();
				SwingConfig.get().previewPause = round(value);
				SwingConfig.save();
			},
			value -> String.format(Locale.ROOT, "%.1f s", value / 20.0)
		).tooltip(Component.translatable("swingstudio.preview_pause.tip")))));
		global("ignore_effects", page, Level.HARD, () -> SwingConfig.get().ignoreEffects, () -> SwingConfig.get().ignoreEffects ^= true);
		heading("tools", page, Level.NORMAL);
		ALL.add(new CustomOption("copy_paste", page, Level.NORMAL, (context, x, y, width) -> {
			int half = (width - 6) / 2;
			StampButton copy = StampButton.of(x, y + 1, half, 16, Component.translatable("swingstudio.copy"), () -> {
				context.copySettings();
				context.refresh();
			}).tooltip(Component.translatable("swingstudio.copy.tip"));
			StampButton paste = StampButton.of(x + half + 4, y + 1, width - half - 6, 16, Component.translatable("swingstudio.paste"), context::pasteSettings)
				.tooltip(Component.translatable("swingstudio.paste.tip"));
			paste.active = context.canPaste();
			return List.of(copy, paste);
		}));
		ALL.add(new CustomOption("clear_items", page, Level.HARD, (context, x, y, width) -> {
			ConfirmStamp clear = new ConfirmStamp(x, y + 1, width - 2, () -> Component.translatable("swingstudio.clear_items", SwingConfig.get().items.size()),
				Component.translatable("swingstudio.clear_items.confirm"), context::clearAllItems);
			clear.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("swingstudio.clear_items.tip")));
			clear.active = !SwingConfig.get().items.isEmpty();
			return List.of(clear);
		}));
		ALL.add(new CustomOption("key_info", page, Level.EASY, (context, x, y, width) -> List.of(
			new TextBlock(x, y + 4, width, Component.translatable("swingstudio.key_info", dev.swingstudio.SwingStudio.openKey.getTranslatedKeyMessage()), Paper.INK_SOFT)
		)));
	}

	static boolean pageVisible(Page page, Level mode) {
		if (page == Page.PRESETS || page == Page.CONFIGS) {
			return true;
		}

		for (Option option : ALL) {
			if (option.page == page && mode.shows(option.level)) {
				return true;
			}
		}

		return false;
	}

	static boolean matches(String haystack, String query) {
		for (String term : query.split("\\s+")) {
			if (!term.isEmpty() && !haystack.contains(term)) {
				return false;
			}
		}

		return true;
	}

	static final class ConfirmStamp extends StampButton {
		ConfirmStamp(int x, int y, int width, Supplier<Component> label, Component confirm, Runnable action) {
			this(x, y, width, label, confirm, action, new boolean[1]);
		}

		private ConfirmStamp(int x, int y, int width, Supplier<Component> label, Component confirm, Runnable action, boolean[] armedBox) {
			super(x, y, width, 16, () -> armedBox[0] ? confirm : label.get(), () -> {
				if (armedBox[0]) {
					armedBox[0] = false;
					action.run();
				} else {
					armedBox[0] = true;
				}
			}, () -> armedBox[0]);
		}
	}
}

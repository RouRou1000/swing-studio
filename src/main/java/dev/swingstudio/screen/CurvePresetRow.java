package dev.swingstudio.screen;

import dev.swingstudio.Curve;
import dev.swingstudio.SwingSettings;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class CurvePresetRow extends AbstractWidget {
	private static final int SKETCH = 34;

	private final Supplier<SwingSettings> view;
	private final Consumer<Curve.Preset> apply;

	CurvePresetRow(int x, int y, int width, Supplier<SwingSettings> view, Consumer<Curve.Preset> apply) {
		super(x, y, width, 18, Component.translatable("swingstudio.curve_presets"));
		this.view = view;
		this.apply = apply;
		this.setTooltip(Tooltip.create(Component.translatable("swingstudio.curve_presets.tip")));
	}

	static Component nameOf(SwingSettings settings) {
		Curve.Preset preset = Curve.Preset.of(settings);
		return Component.translatable(preset == null ? "swingstudio.curve.custom" : "swingstudio.curve." + preset.name().toLowerCase(Locale.ROOT));
	}

	private int middle() {
		return this.getX() + (this.width - SKETCH) / 2;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		SwingSettings settings = this.view.get();
		int x = this.getX();
		int y = this.getY();
		String label = this.getMessage().getString();
		Paper.text(graphics, label, x + 2, y + 5, Paper.INK);
		int labelEnd = x + Paper.font().width(label) + 8;
		int sketchX = x + this.width - SKETCH;
		boolean lit = this.isHovered();
		boolean leftHalf = mouseX < this.middle();
		Paper.triangleLeft(graphics, labelEnd, y + 8, 4, lit && leftHalf ? Paper.RED : Paper.INK_SOFT);
		Paper.triangleRight(graphics, sketchX - 10, y + 8, 4, lit && !leftHalf ? Paper.RED : Paper.INK_SOFT);
		String name = Paper.fit(nameOf(settings).getString(), sketchX - 14 - (labelEnd + 6));
		Paper.centered(graphics, name, (labelEnd + 4 + sketchX - 10) / 2, y + 5, Paper.BLUE);

		graphics.fill(sketchX, y + 1, sketchX + SKETCH, y + 17, Paper.CARD);
		graphics.outline(sketchX, y + 1, SKETCH, 16, Paper.INK_SOFT);
		int previous = y + 14;
		for (int i = 0; i <= SKETCH - 6; i++) {
			float t = i / (float) (SKETCH - 6);
			float value = settings.shape(t);
			int py = y + 14 - Math.round(value * 10.0F);
			py = Math.max(y + 2, Math.min(y + 15, py));
			graphics.fill(sketchX + 3 + i, Math.min(previous, py), sketchX + 4 + i, Math.max(previous, py) + 1, Paper.BLUE);
			previous = py;
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.step(event.x() < this.middle() ? -1 : 1);
	}

	private void step(int direction) {
		Curve.Preset[] presets = Curve.Preset.values();
		Curve.Preset current = Curve.Preset.of(this.view.get());
		int index = current == null ? (direction > 0 ? 0 : presets.length - 1) : Math.floorMod(current.ordinal() + direction, presets.length);
		this.apply.accept(presets[index]);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_LEFT) {
			this.step(-1);
			return true;
		}

		if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT) {
			this.step(1);
			return true;
		}

		return false;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, Component.empty().append(this.getMessage()).append(" ").append(nameOf(this.view.get())));
	}
}

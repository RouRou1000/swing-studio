package dev.swingstudio.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

final class RulerSlider extends AbstractWidget {
	static final int HEIGHT = 24;

	private final Component label;
	private final double min;
	private final double max;
	private final double step;
	private final double defaultValue;
	private final DoubleSupplier getter;
	private final DoubleConsumer setter;
	private final DoubleFunction<String> format;
	@Nullable
	private BooleanSupplier enabledWhen;
	private boolean dragging;
	private static final int STEP_BOX = 9;

	RulerSlider(
		int x, int y, int width, Component label, double min, double max, double step, double defaultValue, DoubleSupplier getter, DoubleConsumer setter, DoubleFunction<String> format
	) {
		super(x, y, width, HEIGHT, label);
		this.label = label;
		this.min = min;
		this.max = max;
		this.step = step;
		this.defaultValue = defaultValue;
		this.getter = getter;
		this.setter = setter;
		this.format = format;
	}

	RulerSlider enabledWhen(@Nullable BooleanSupplier condition) {
		this.enabledWhen = condition;
		return this;
	}

	RulerSlider tooltip(@Nullable Component text) {
		if (text != null) {
			this.setTooltip(Tooltip.create(text));
		}

		return this;
	}

	private double value() {
		return Mth.clamp(this.getter.getAsDouble(), this.min, this.max);
	}

	private void set(double value) {
		double snapped = Math.round(value / this.step) * this.step;
		this.setter.accept(Mth.clamp(snapped, this.min, this.max));
	}

	private int left() {
		return this.getX() + STEP_BOX + 5;
	}

	private int span() {
		return this.width - 2 * STEP_BOX - 11;
	}

	private boolean isDefault() {
		return Math.abs(this.value() - this.defaultValue) < this.step / 2.0;
	}

	private int resetX() {
		return this.getX() + this.width - 3 - Paper.font().width(this.format.apply(this.value())) - 11;
	}

	private boolean overReset(double mouseX, double mouseY) {
		return !this.isDefault() && mouseY < this.getY() + 12 && mouseX >= this.resetX() - 1 && mouseX < this.resetX() + 9;
	}

	private boolean overMinus(double mouseX, double mouseY) {
		return mouseY >= this.getY() + 12 && mouseX < this.getX() + STEP_BOX + 3;
	}

	private boolean overPlus(double mouseX, double mouseY) {
		return mouseY >= this.getY() + 12 && mouseX >= this.getX() + this.width - STEP_BOX - 3;
	}

	private int xOf(double value) {
		return this.left() + (int) Math.round((value - this.min) / (this.max - this.min) * this.span());
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		if (this.enabledWhen != null) {
			this.active = this.enabledWhen.getAsBoolean();
		}

		int x = this.getX();
		int y = this.getY();
		boolean lit = this.active && (this.isHovered() || this.isFocused());
		int ink = this.active ? Paper.INK : Paper.INK_FAINT;
		String valueText = this.format.apply(this.value());
		int valueWidth = Paper.font().width(valueText);
		String labelText = Paper.fit(this.label.getString(), this.width - valueWidth - 24);
		Paper.text(graphics, labelText, x + 2, y + 2, ink);
		if (lit) {
			Paper.dashed(graphics, x + 2, x + 2 + Paper.font().width(labelText), y + 11, Paper.INK_SOFT);
		}

		boolean isDefault = this.isDefault();
		int valueColor = !this.active ? Paper.INK_FAINT : isDefault ? Paper.INK_SOFT : Paper.BLUE;
		Paper.text(graphics, valueText, x + this.width - 3 - valueWidth, y + 2, valueColor);
		if (!isDefault && this.active) {
			this.drawReset(graphics, this.resetX(), y + 2, this.isHovered() && this.overReset(mouseX, mouseY));
		}

		int base = y + 19;
		int left = this.left();
		int right = left + this.span();
		this.drawStep(graphics, x + 1, base - 7, false, this.isHovered() && this.overMinus(mouseX, mouseY));
		this.drawStep(graphics, x + this.width - STEP_BOX - 1, base - 7, true, this.isHovered() && this.overPlus(mouseX, mouseY));
		graphics.fill(left, base, right + 1, base + 1, ink);
		for (int i = 0; i <= 10; i++) {
			int tx = left + Math.round(this.span() * i / 10.0F);
			int tall = i % 5 == 0 ? 4 : 2;
			graphics.fill(tx, base - tall, tx + 1, base, this.active ? Paper.INK_SOFT : Paper.INK_FAINT);
		}

		int home = this.xOf(Mth.clamp(this.defaultValue, this.min, this.max));
		int pointer = this.xOf(this.value());
		graphics.fill(home - 1, base + 2, home + 2, base + 4, Paper.MUSTARD);
		if (this.active) {
			graphics.fill(Math.min(home, pointer), base - 1, Math.max(home, pointer) + 1, base + 1, Paper.RED);
		}

		int pointerColor = this.active ? Paper.RED : Paper.INK_FAINT;
		graphics.fill(pointer, base - 5, pointer + 1, base + 1, pointerColor);
		Paper.triangleDown(graphics, pointer, base - 9, lit || this.dragging ? 5 : 4, pointerColor);
	}

	private void drawStep(GuiGraphicsExtractor graphics, int x, int y, boolean plus, boolean lit) {
		int ink = this.active ? Paper.INK : Paper.INK_FAINT;
		graphics.fill(x, y, x + STEP_BOX, y + STEP_BOX, lit && this.active ? Paper.SHEET_SHADE : Paper.CARD);
		graphics.outline(x, y, STEP_BOX, STEP_BOX, ink);
		graphics.fill(x + 2, y + 4, x + STEP_BOX - 2, y + 5, ink);
		if (plus) {
			graphics.fill(x + 4, y + 2, x + 5, y + STEP_BOX - 2, ink);
		}
	}

	private void drawReset(GuiGraphicsExtractor graphics, int x, int y, boolean lit) {
		int color = lit ? Paper.RED : Paper.INK_SOFT;
		graphics.fill(x + 2, y, x + 6, y + 1, color);
		graphics.fill(x + 6, y + 1, x + 7, y + 5, color);
		graphics.fill(x + 2, y + 5, x + 6, y + 6, color);
		Paper.triangleLeft(graphics, x, y + 5, 3, color);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.isActive() && event.button() == InputConstants.MOUSE_BUTTON_RIGHT && this.isMouseOver(event.x(), event.y())) {
			this.playDownSound(Minecraft.getInstance().getSoundManager());
			this.setter.accept(this.defaultValue);
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.dragging = false;
		if (this.overReset(event.x(), event.y())) {
			this.setter.accept(this.defaultValue);
		} else if (this.overMinus(event.x(), event.y())) {
			this.set(this.value() - this.step);
		} else if (this.overPlus(event.x(), event.y())) {
			this.set(this.value() + this.step);
		} else {
			this.dragging = true;
			this.setFromMouse(event.x());
		}
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dx, double dy) {
		if (this.dragging) {
			this.setFromMouse(event.x());
		}
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		this.dragging = false;
	}

	private void setFromMouse(double mouseX) {
		double t = Mth.clamp((mouseX - this.left()) / this.span(), 0.0, 1.0);
		this.set(this.min + t * (this.max - this.min));
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!this.isActive()) {
			return false;
		}

		double amount = event.hasShiftDown() ? this.step * 10.0 : this.step;
		if (event.key() == InputConstants.KEY_LEFT) {
			this.set(this.value() - amount);
			return true;
		}

		if (event.key() == InputConstants.KEY_RIGHT) {
			this.set(this.value() + amount);
			return true;
		}

		return false;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, Component.empty().append(this.label).append(": ").append(this.format.apply(this.value())));
	}
}

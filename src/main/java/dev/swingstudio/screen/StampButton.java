package dev.swingstudio.screen;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

class StampButton extends AbstractButton {
	private final Supplier<Component> label;
	private final Runnable action;
	@Nullable
	private final BooleanSupplier selected;

	StampButton(int x, int y, int width, int height, Supplier<Component> label, Runnable action, @Nullable BooleanSupplier selected) {
		super(x, y, width, height, label.get());
		this.label = label;
		this.action = action;
		this.selected = selected;
	}

	static StampButton of(int x, int y, int width, int height, Component label, Runnable action) {
		return new StampButton(x, y, width, height, () -> label, action, null);
	}

	StampButton tooltip(Component text) {
		this.setTooltip(Tooltip.create(text));
		return this;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.action.run();
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.setMessage(this.label.get());
		boolean on = this.selected != null && this.selected.getAsBoolean();
		boolean down = this.active && this.isHoveredOrFocused();
		int shift = down ? 1 : 0;
		int x = this.getX() + shift;
		int y = this.getY() + shift;
		if (this.active) {
			Paper.shadow(graphics, this.getX(), this.getY(), this.width, this.height, down ? 1 : 2);
		}

		int face = on ? Paper.INK : down ? Paper.CARD : Paper.SHEET_SHADE;
		graphics.fill(x, y, x + this.width, y + this.height, face);
		graphics.outline(x, y, this.width, this.height, this.active ? Paper.INK : Paper.INK_FAINT);
		int color = on ? Paper.SHEET : this.active ? Paper.INK : Paper.INK_FAINT;
		String text = Paper.fit(this.getMessage().getString(), this.width - 6);
		Paper.centered(graphics, text, x + this.width / 2, y + (this.height - 7) / 2, color);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}

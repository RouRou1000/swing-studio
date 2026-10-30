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

final class CheckToggle extends AbstractButton {
	static final int HEIGHT = 14;

	private final Supplier<Component> label;
	private final BooleanSupplier on;
	private final Runnable toggle;

	CheckToggle(int x, int y, int width, Supplier<Component> label, BooleanSupplier on, Runnable toggle, @Nullable Component tooltip) {
		super(x, y, width, HEIGHT, label.get());
		this.label = label;
		this.on = on;
		this.toggle = toggle;
		if (tooltip != null) {
			this.setTooltip(Tooltip.create(tooltip));
		}
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.toggle.run();
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.setMessage(this.label.get());
		int x = this.getX();
		int y = this.getY();
		int boxY = y + (this.height - 9) / 2;
		boolean lit = this.isHoveredOrFocused();
		graphics.fill(x + 1, boxY, x + 10, boxY + 9, lit ? Paper.SHEET : Paper.CARD);
		graphics.outline(x + 1, boxY, 9, 9, Paper.INK);
		if (this.on.getAsBoolean()) {
			Paper.cross(graphics, x + 2, boxY + 1, 7, Paper.RED);
		}

		String text = Paper.fit(this.getMessage().getString(), this.width - 15);
		Paper.text(graphics, text, x + 14, y + (this.height - 7) / 2, this.active ? Paper.INK : Paper.INK_FAINT);
		if (lit) {
			Paper.dashed(graphics, x + 14, x + 14 + Paper.font().width(text), y + this.height - 2, Paper.INK_SOFT);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}

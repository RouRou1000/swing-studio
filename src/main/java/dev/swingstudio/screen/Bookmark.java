package dev.swingstudio.screen;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

final class Bookmark extends AbstractButton {
	private static final int EXTRA = 7;

	private final int color;
	private final BooleanSupplier selected;
	private final Runnable action;
	private final IntSupplier badge;

	Bookmark(int x, int y, Component label, int color, BooleanSupplier selected, Runnable action, IntSupplier badge) {
		super(x, y, Paper.font().width(label) + 26 + EXTRA, 16, label);
		this.color = color;
		this.selected = selected;
		this.action = action;
		this.badge = badge;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.action.run();
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		boolean on = this.selected.getAsBoolean();
		boolean lit = this.isHoveredOrFocused();
		int length = this.width - (on ? 0 : lit ? EXTRA / 2 : EXTRA);
		int x = this.getX();
		int y = this.getY();
		graphics.fill(x + 2, y + 2, x + length + 2, y + this.height + 1, Paper.SHADOW);
		int face = on ? this.color : darker(this.color);
		int rows = this.height - 1;
		int middle = rows / 2;
		for (int row = 0; row < rows; row++) {
			int notch = Math.max(0, 4 - Math.abs(row - middle));
			graphics.fill(x, y + row, x + length - notch, y + row + 1, face);
		}

		if (on) {
			Paper.dashed(graphics, x + 4, x + length - 6, y + 2, 0x80FFFFFF);
			Paper.dashed(graphics, x + 4, x + length - 6, y + this.height - 3, 0x80FFFFFF);
		}

		Paper.text(graphics, this.getMessage().getString(), x + 7, y + 4, Paper.SHEET);
		int count = this.badge.getAsInt();
		if (count > 0) {
			String text = count > 9 ? "9+" : String.valueOf(count);
			int tagX = x + 9 + Paper.font().width(this.getMessage());
			int tagW = Paper.font().width(text) + 4;
			graphics.fill(tagX, y + 3, tagX + tagW, y + 12, Paper.NOTE);
			Paper.text(graphics, text, tagX + 2, y + 4, Paper.INK);
		}
	}

	private static int darker(int color) {
		int r = (color >> 16 & 0xFF) * 3 / 4;
		int g = (color >> 8 & 0xFF) * 3 / 4;
		int b = (color & 0xFF) * 3 / 4;
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}

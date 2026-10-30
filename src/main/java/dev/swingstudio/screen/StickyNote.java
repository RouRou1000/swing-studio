package dev.swingstudio.screen;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

final class StickyNote extends AbstractButton {
	private final List<FormattedCharSequence> lines;
	private final Runnable action;

	StickyNote(int x, int y, int width, Component text, Runnable action) {
		super(x, y, width, 0, text);
		this.lines = Paper.font().split(text, width - 12);
		this.action = action;
		this.height = this.lines.size() * 10 + 8;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.action.run();
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x = this.getX() + 2;
		int y = this.getY() + 2;
		int w = this.width - 4;
		int h = this.height - 3;
		boolean lit = this.isHoveredOrFocused();
		Paper.shadow(graphics, x, y, w, h, lit ? 3 : 2);
		graphics.fill(x, y, x + w, y + h, Paper.NOTE);
		graphics.fill(x + w / 2 - 12, y - 3, x + w / 2 + 12, y + 3, 0xA0FFFFFF);
		int ty = y + 4;
		for (FormattedCharSequence line : this.lines) {
			graphics.text(Paper.font(), line, x + 5, ty, Paper.INK, false);
			ty += 10;
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}

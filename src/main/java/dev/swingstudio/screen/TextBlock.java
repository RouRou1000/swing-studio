package dev.swingstudio.screen;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

final class TextBlock extends AbstractWidget {
	private final List<FormattedCharSequence> lines;
	private final int color;

	TextBlock(int x, int y, int width, Component text, int color) {
		super(x, y, width, 0, text);
		this.lines = Paper.font().split(text, width - 4);
		this.color = color;
		this.height = this.lines.size() * 10 + 2;
		this.active = false;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int y = this.getY() + 1;
		for (FormattedCharSequence line : this.lines) {
			graphics.text(Paper.font(), line, this.getX() + 2, y, this.color, false);
			y += 10;
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, this.getMessage());
	}
}

package dev.swingstudio.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

final class NameLine extends AbstractWidget {
	private final EditBox box;

	NameLine(int x, int y, int width, EditBox box) {
		super(x, y, width, 17, Component.empty());
		this.box = box;
		this.active = false;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int color = this.box.isFocused() ? Paper.RED : Paper.INK_SOFT;
		graphics.fill(this.getX() + 2, this.getY() + 15, this.getX() + this.width, this.getY() + 16, color);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
	}
}

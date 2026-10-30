package dev.swingstudio.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

final class IconStamp extends StampButton {
	enum Icon {
		UNDO,
		REDO,
		PLAY
	}

	private final Icon icon;

	IconStamp(int x, int y, int size, Icon icon, Component narration, Runnable action) {
		super(x, y, size, size, () -> narration, action, null);
		this.icon = icon;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		boolean down = this.active && this.isHoveredOrFocused();
		int shift = down ? 1 : 0;
		int x = this.getX() + shift;
		int y = this.getY() + shift;
		if (this.active) {
			Paper.shadow(graphics, this.getX(), this.getY(), this.width, this.height, down ? 1 : 2);
		}

		graphics.fill(x, y, x + this.width, y + this.height, down ? Paper.CARD : Paper.SHEET_SHADE);
		graphics.outline(x, y, this.width, this.height, this.active ? Paper.INK : Paper.INK_FAINT);
		int ink = this.active ? Paper.INK : Paper.INK_FAINT;
		int cx = x + this.width / 2;
		int cy = y + this.height / 2;
		switch (this.icon) {
			case UNDO -> {
				graphics.fill(cx - 2, cy - 3, cx + 3, cy - 2, ink);
				graphics.fill(cx + 3, cy - 2, cx + 4, cy + 2, ink);
				graphics.fill(cx - 1, cy + 2, cx + 3, cy + 3, ink);
				Paper.triangleLeft(graphics, cx - 5, cy - 3, 3, ink);
			}
			case REDO -> {
				graphics.fill(cx - 2, cy - 3, cx + 3, cy - 2, ink);
				graphics.fill(cx - 3, cy - 2, cx - 2, cy + 2, ink);
				graphics.fill(cx - 2, cy + 2, cx + 2, cy + 3, ink);
				Paper.triangleRight(graphics, cx + 3, cy - 3, 3, ink);
			}
			case PLAY -> Paper.triangleRight(graphics, cx - 2, cy, 4, this.active ? Paper.RED : Paper.INK_FAINT);
		}
	}
}

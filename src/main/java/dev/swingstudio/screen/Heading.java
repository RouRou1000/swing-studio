package dev.swingstudio.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

final class Heading extends AbstractButton {
	private final int color;
	private final boolean collapsed;
	private final int changed;
	@Nullable
	private final Runnable toggle;

	Heading(int x, int y, int width, Component text, int color) {
		this(x, y, width, text, color, false, 0, null);
	}

	Heading(int x, int y, int width, Component text, int color, boolean collapsed, int changed, @Nullable Runnable toggle) {
		super(x, y, width, 17, text);
		this.color = color;
		this.collapsed = collapsed;
		this.changed = changed;
		this.toggle = toggle;
		this.active = toggle != null;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		if (this.toggle != null) {
			this.toggle.run();
		}
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x = this.getX();
		int y = this.getY() + 5;
		boolean lit = this.active && this.isHoveredOrFocused();
		int textX = x + 6;
		if (this.toggle != null) {
			if (this.collapsed) {
				Paper.triangleRight(graphics, x, y + 3, 4, this.color);
			} else {
				Paper.triangleDown(graphics, x + 3, y + 1, 4, this.color);
			}

			textX = x + 10;
		} else {
			graphics.fill(x, y + 1, x + 3, y + 6, this.color);
		}

		String count = this.changed > 0 ? Component.translatable("swingstudio.section.changed", this.changed).getString() : "";
		int countWidth = count.isEmpty() ? 0 : Paper.font().width(count) + 6;
		String text = Paper.fit(this.getMessage().getString(), this.width - (textX - x) - countWidth - 6);
		Paper.text(graphics, text, textX, y, this.color);
		int lineStart = textX + Paper.font().width(text) + 4;
		if (!count.isEmpty()) {
			Paper.text(graphics, count, lineStart, y, Paper.BLUE);
			lineStart += countWidth;
		}

		int faint = (this.color & 0x00FFFFFF) | (lit ? 0xC0000000 : 0x60000000);
		Paper.dashed(graphics, lineStart, x + this.width - 2, y + 4, faint);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}

package dev.swingstudio.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

final class Paper {
	static final int SHEET = 0xFFEFE6CF;
	static final int SHEET_SHADE = 0xFFE2D5B6;
	static final int CARD = 0xFFF8F2E2;
	static final int NOTE = 0xFFF2D46E;
	static final int INK = 0xFF2B231C;
	static final int INK_SOFT = 0xFF75685A;
	static final int INK_FAINT = 0xFFAA9C88;
	static final int RED = 0xFFB0382A;
	static final int BLUE = 0xFF2E4C80;
	static final int MUSTARD = 0xFFC08A1E;
	static final int GREEN = 0xFF4E7A3A;
	static final int PLUM = 0xFF7A3E6E;
	static final int RULE = 0x2A3A5A9A;
	static final int MARGIN = 0x90B0382A;
	static final int SHADOW = 0x66000000;

	private Paper() {
	}

	static Font font() {
		return Minecraft.getInstance().font;
	}

	static void text(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
		graphics.text(font(), text, x, y, color, false);
	}

	static void text(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
		graphics.text(font(), text, x, y, color, false);
	}

	static void centered(GuiGraphicsExtractor graphics, String text, int centerX, int y, int color) {
		graphics.text(font(), text, centerX - font().width(text) / 2, y, color, false);
	}

	static String fit(String text, int maxWidth) {
		Font font = font();
		if (font.width(text) <= maxWidth) {
			return text;
		}

		String dots = "…";
		return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width(dots))) + dots;
	}

	static void sketch(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
		graphics.outline(x, y, w, h, color);
		int faint = (color & 0x00FFFFFF) | 0x70000000;
		graphics.fill(x + 2, y - 1, x + w - 1, y, faint);
		graphics.fill(x + w, y + 1, x + w + 1, y + h - 1, faint);
	}

	static void shadow(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int offset) {
		graphics.fill(x + offset, y + offset, x + w + offset, y + h + offset, SHADOW);
	}

	static void dashed(GuiGraphicsExtractor graphics, int x0, int x1, int y, int color) {
		for (int x = x0; x < x1; x += 4) {
			graphics.fill(x, y, Math.min(x + 2, x1), y + 1, color);
		}
	}

	static void triangleDown(GuiGraphicsExtractor graphics, int centerX, int y, int size, int color) {
		for (int i = 0; i < size; i++) {
			graphics.fill(centerX - size + 1 + i, y + i, centerX + size - i, y + i + 1, color);
		}
	}

	static void triangleLeft(GuiGraphicsExtractor graphics, int x, int centerY, int size, int color) {
		for (int i = 0; i < size; i++) {
			graphics.fill(x + i, centerY - i, x + i + 1, centerY + i + 1, color);
		}
	}

	static void triangleRight(GuiGraphicsExtractor graphics, int x, int centerY, int size, int color) {
		for (int i = 0; i < size; i++) {
			graphics.fill(x + size - 1 - i, centerY - i, x + size - i, centerY + i + 1, color);
		}
	}

	static void cross(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
		for (int i = 0; i < size; i++) {
			graphics.fill(x + i, y + i, x + i + 2, y + i + 1, color);
			graphics.fill(x + size - 1 - i, y + i, x + size + 1 - i, y + i + 1, color);
		}
	}

	static void magnifier(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x + 2, y, x + 5, y + 1, color);
		graphics.fill(x + 1, y + 1, x + 2, y + 2, color);
		graphics.fill(x + 5, y + 1, x + 6, y + 2, color);
		graphics.fill(x, y + 2, x + 1, y + 5, color);
		graphics.fill(x + 6, y + 2, x + 7, y + 5, color);
		graphics.fill(x + 1, y + 5, x + 2, y + 6, color);
		graphics.fill(x + 5, y + 5, x + 6, y + 6, color);
		graphics.fill(x + 2, y + 6, x + 5, y + 7, color);
		graphics.fill(x + 6, y + 6, x + 8, y + 7, color);
		graphics.fill(x + 7, y + 7, x + 9, y + 8, color);
		graphics.fill(x + 8, y + 8, x + 9, y + 9, color);
	}

	static void hand(GuiGraphicsExtractor graphics, int x, int y) {
		int skin = 0xFFE0AC86;
		int shade = 0xFFB98463;
		graphics.fill(x + 4, y + 7, x + 12, y + 15, skin);
		graphics.fill(x + 4, y + 2, x + 6, y + 8, skin);
		graphics.fill(x + 6, y + 1, x + 8, y + 8, skin);
		graphics.fill(x + 8, y + 1, x + 10, y + 8, skin);
		graphics.fill(x + 10, y + 3, x + 12, y + 8, skin);
		graphics.fill(x + 1, y + 7, x + 4, y + 10, skin);
		graphics.fill(x + 6, y + 2, x + 7, y + 7, shade);
		graphics.fill(x + 8, y + 2, x + 9, y + 7, shade);
		graphics.fill(x + 10, y + 4, x + 11, y + 7, shade);
		graphics.fill(x + 4, y + 14, x + 12, y + 15, shade);
	}

	static void sheet(GuiGraphicsExtractor graphics, int x, int y, int right, int bottom, int ruleTop, boolean tornRight) {
		for (int row = y; row < bottom; row++) {
			int tear = tornRight ? tear(row) : 0;
			graphics.fill(right, row, right + tear + 4, row + 1, SHADOW);
		}

		graphics.fill(x + 3, bottom, right + 3, bottom + 3, SHADOW);
		graphics.fill(x, y, right, bottom, SHEET);
		if (tornRight) {
			for (int row = y; row < bottom; row++) {
				graphics.fill(right, row, right + tear(row), row + 1, SHEET);
			}
		}

		for (int line = ruleTop; line < bottom - 2; line += 12) {
			graphics.fill(x, line, right, line + 1, RULE);
		}

		graphics.fill(x + 3, y, x + 4, bottom, MARGIN);
	}

	private static int tear(int row) {
		int a = Math.abs(row % 7 - 3);
		int b = (row / 7 * 5) % 3;
		return Math.max(0, a - b + 1);
	}
}

package dev.swingstudio.screen;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

final class Hints {
	static int height(int width) {
		return width >= 140 ? 60 : 92;
	}

	enum Kind {
		MOVE_X(Paper.RED),
		MOVE_Y(Paper.GREEN),
		MOVE_Z(Paper.BLUE),
		TURN_X(Paper.RED),
		TURN_Y(Paper.GREEN),
		TURN_Z(Paper.BLUE),
		SCALE(Paper.PLUM),
		TIME(Paper.MUSTARD),
		NONE(Paper.INK_FAINT);

		final int color;

		Kind(int color) {
			this.color = color;
		}
	}

	private Hints() {
	}

	static Kind kindOf(@Nullable String id) {
		if (id == null) {
			return Kind.NONE;
		}

		return switch (id) {
			case "pos_x", "move_x", "push_x" -> Kind.MOVE_X;
			case "pos_y", "move_y", "push_y", "idle_bob" -> Kind.MOVE_Y;
			case "pos_z", "move_z", "push_z" -> Kind.MOVE_Z;
			case "rot_x", "tilt" -> Kind.TURN_X;
			case "rot_y", "turn", "sway" -> Kind.TURN_Y;
			case "rot_z", "roll", "idle_rock", "wobble", "wobble_speed", "spin", "spins" -> Kind.TURN_Z;
			case "size", "stretch_x", "stretch_y", "stretch_z", "pop" -> Kind.SCALE;
			case "speed", "hit_pause", "curve_presets", "curve_graph", "peak", "reverse", "idle_speed", "variation",
				"curve_x1", "curve_y1", "curve_x2", "curve_y2" -> Kind.TIME;
			default -> Kind.NONE;
		};
	}

	static void draw(GuiGraphicsExtractor graphics, @Nullable String id, int x, int y, int width, ItemStack icon) {
		Kind kind = kindOf(id);
		int cx = x + 25;
		int cy = y + 26;
		graphics.pose().pushMatrix();
		graphics.pose().translate(cx - 16, cy - 16);
		graphics.pose().scale(2.0F, 2.0F);
		if (icon.isEmpty()) {
			Paper.hand(graphics, 0, 0);
		} else {
			graphics.item(icon, 0, 0);
		}

		graphics.pose().popMatrix();
		int c = kind.color;
		switch (kind) {
			case MOVE_X -> {
				line(graphics, cx - 20, cy + 21, cx + 20, cy + 21, c);
				Paper.triangleRight(graphics, cx + 19, cy + 21, 4, c);
				Paper.triangleLeft(graphics, cx - 22, cy + 21, 4, c);
				Paper.text(graphics, "+", cx + 16, cy + 11, c);
				Paper.text(graphics, "−", cx - 21, cy + 11, c);
			}
			case MOVE_Y -> {
				line(graphics, cx + 21, cy - 20, cx + 21, cy + 20, c);
				Paper.triangleDown(graphics, cx + 21, cy + 18, 4, c);
				up(graphics, cx + 21, cy - 22, 4, c);
				Paper.text(graphics, "+", cx + 14, cy - 22, c);
				Paper.text(graphics, "−", cx + 14, cy + 14, c);
			}
			case MOVE_Z -> {
				line(graphics, cx + 14, cy + 16, cx - 14, cy - 14, c);
				graphics.fill(cx - 16, cy - 16, cx - 11, cy - 15, c);
				graphics.fill(cx - 16, cy - 16, cx - 15, cy - 11, c);
				Paper.text(graphics, "+", cx - 22, cy - 11, c);
				Paper.text(graphics, "−", cx + 16, cy + 12, c);
			}
			case TURN_X -> arc(graphics, cx, cy, 20, 20, -80, 70, c, true);
			case TURN_Y -> arc(graphics, cx, cy + 18, 22, 6, 200, 340, c, true);
			case TURN_Z -> arc(graphics, cx, cy, 21, 21, -150, 150, c, true);
			case SCALE -> {
				for (int sx = -1; sx <= 1; sx += 2) {
					for (int sy = -1; sy <= 1; sy += 2) {
						int px = cx + sx * 20;
						int py = cy + sy * 20;
						graphics.fill(Math.min(px, px - sx * 6), py, Math.max(px, px - sx * 6) + 1, py + 1, c);
						graphics.fill(px, Math.min(py, py - sy * 6), px + 1, Math.max(py, py - sy * 6) + 1, c);
					}
				}
			}
			case TIME -> {
				int left = cx - 20;
				int right = cx + 20;
				graphics.fill(left, cy + 22, right, cy + 23, c);
				float t = (System.currentTimeMillis() % 1400L) / 1000.0F;
				int dot = left + Math.round(Mth.clamp(t, 0.0F, 1.0F) * (right - left));
				graphics.fill(dot - 2, cy + 20, dot + 3, cy + 25, Paper.INK);
			}
			default -> {
			}
		}

		boolean wide = width >= 140;
		int textX = wide ? x + 54 : x;
		int textWidth = wide ? width - 54 : width;
		int ty = wide ? y + 4 : y + 52;
		if (id != null && kind != Kind.NONE) {
			Component name = Component.translatable("swingstudio." + id).copy().withStyle(Style.EMPTY.withBold(true));
			graphics.text(Paper.font(), Paper.font().split(name, textWidth).get(0), textX, ty, Paper.INK, false);
			ty += 11;
		}

		String key = kind == Kind.NONE ? "swingstudio.hint.none" : "swingstudio.hint." + kind.name().toLowerCase(Locale.ROOT);
		if (Language.getInstance().has(key)) {
			List<FormattedCharSequence> lines = Paper.font().split(Component.translatable(key), textWidth);
			for (int i = 0; i < Math.min(lines.size(), wide ? 4 : 3); i++) {
				graphics.text(Paper.font(), lines.get(i), textX, ty, Paper.INK_SOFT, false);
				ty += 10;
			}
		}
	}

	private static void up(GuiGraphicsExtractor graphics, int centerX, int y, int size, int color) {
		for (int i = 0; i < size; i++) {
			graphics.fill(centerX - i, y + i, centerX + i + 1, y + i + 1, color);
		}
	}

	private static void line(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
		int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
		for (int i = 0; i <= steps; i++) {
			int px = x0 + Math.round((float) (x1 - x0) * i / Math.max(1, steps));
			int py = y0 + Math.round((float) (y1 - y0) * i / Math.max(1, steps));
			graphics.fill(px, py, px + 1, py + 1, color);
		}
	}

	private static void arc(GuiGraphicsExtractor graphics, int cx, int cy, int rx, int ry, int from, int to, int color, boolean head) {
		for (int angle = from; angle <= to; angle += 6) {
			double r = Math.toRadians(angle);
			int px = cx + (int) Math.round(Math.cos(r) * rx);
			int py = cy + (int) Math.round(Math.sin(r) * ry);
			graphics.fill(px, py, px + 2, py + 2, color);
		}

		if (head) {
			double r = Math.toRadians(to);
			int px = cx + (int) Math.round(Math.cos(r) * rx);
			int py = cy + (int) Math.round(Math.sin(r) * ry);
			graphics.fill(px - 2, py - 2, px + 4, py + 4, color);
			graphics.fill(px - 1, py - 1, px + 3, py + 3, Paper.SHEET);
			graphics.fill(px, py, px + 2, py + 2, color);
		}
	}
}

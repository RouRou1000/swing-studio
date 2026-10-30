package dev.swingstudio.screen;

import dev.swingstudio.Curve;
import dev.swingstudio.SwingSettings;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

final class CurveEditor extends AbstractWidget {
	private static final int PAD = 5;
	private static final int HANDLE = 3;
	private static final long LOOP_MS = 1400L;
	private static final long RUN_MS = 1000L;

	private final Supplier<SwingSettings> view;
	private final Consumer<Consumer<SwingSettings>> edit;
	private int dragging = -1;

	CurveEditor(int x, int y, int width, int height, Supplier<SwingSettings> view, Consumer<Consumer<SwingSettings>> edit) {
		super(x, y, width, height, Component.translatable("swingstudio.curve.narration"));
		this.view = view;
		this.edit = edit;
	}

	private int left() {
		return this.getX() + PAD;
	}

	private int right() {
		return this.getX() + this.width - PAD;
	}

	private int top() {
		return this.getY() + PAD;
	}

	private int bottom() {
		return this.getY() + this.height - PAD;
	}

	private int px(float t) {
		return this.left() + Math.round(t * (this.right() - this.left()));
	}

	private int py(float value) {
		float t = (value - Curve.MIN_Y) / (Curve.MAX_Y - Curve.MIN_Y);
		return this.bottom() - Math.round(t * (this.bottom() - this.top()));
	}

	private float valueAt(double y) {
		float t = (float) ((this.bottom() - y) / (this.bottom() - this.top()));
		return Curve.MIN_Y + t * (Curve.MAX_Y - Curve.MIN_Y);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		SwingSettings settings = this.view.get();
		int x = this.getX();
		int y = this.getY();
		Paper.shadow(graphics, x, y, this.width, this.height, 2);
		graphics.fill(x, y, x + this.width, y + this.height, Paper.CARD);
		int left = this.left();
		int right = this.right();
		for (int i = 0; i <= 10; i++) {
			int gx = left + Math.round((right - left) * i / 10.0F);
			graphics.fill(gx, this.top(), gx + 1, this.bottom() + 1, 0x223A5A9A);
		}

		for (int i = 0; i <= 11; i++) {
			int gy = this.py(Curve.MIN_Y + i * 0.2F);
			graphics.fill(left, gy, right + 1, gy + 1, 0x223A5A9A);
		}

		graphics.outline(x, y, this.width, this.height, this.isFocused() || this.dragging >= 0 ? Paper.RED : Paper.INK);
		int zero = this.py(0.0F);
		int one = this.py(1.0F);
		graphics.fill(left, zero, right + 1, zero + 1, Paper.INK_FAINT);
		graphics.fill(left, one, right + 1, one + 1, Paper.INK_FAINT);
		for (int px = left; px <= right; px += 3) {
			float t = (float) (px - left) / (right - left);
			graphics.fill(px, this.py(t), px + 1, this.py(t) + 1, Paper.INK_FAINT);
		}

		int h1x = this.px(settings.x1);
		int h1y = this.py(settings.y1);
		int h2x = this.px(settings.x2);
		int h2y = this.py(settings.y2);
		line(graphics, left, zero, h1x, h1y, 0xA0B0382A);
		line(graphics, right, one, h2x, h2y, 0xA0B0382A);

		int previous = this.py(settings.shape(0.0F));
		for (int px = left; px <= right; px++) {
			float t = (float) (px - left) / (right - left);
			int current = this.py(settings.shape(t));
			graphics.fill(px, Math.min(previous, current), px + 1, Math.max(previous, current) + 2, Paper.BLUE);
			previous = current;
		}

		long phase = System.currentTimeMillis() % LOOP_MS;
		float t = Math.min(1.0F, (float) phase / RUN_MS);
		int dotX = this.px(t);
		int dotY = this.py(settings.shape(t));
		graphics.fill(dotX, this.top(), dotX + 1, this.bottom() + 1, 0x302B231C);
		graphics.fill(dotX - 2, dotY - 2, dotX + 3, dotY + 3, Paper.INK);

		int hovered = this.dragging >= 0 ? this.dragging : this.isMouseOver(mouseX, mouseY) ? this.nearest(mouseX, mouseY) : -1;
		this.handle(graphics, h1x, h1y, hovered == 0);
		this.handle(graphics, h2x, h2y, hovered == 1);
	}

	private void handle(GuiGraphicsExtractor graphics, int x, int y, boolean lit) {
		graphics.fill(x - HANDLE - 1, y - HANDLE - 1, x + HANDLE + 2, y + HANDLE + 2, Paper.INK);
		graphics.fill(x - HANDLE, y - HANDLE, x + HANDLE + 1, y + HANDLE + 1, lit ? Paper.NOTE : Paper.RED);
	}

	private static void line(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
		int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
		if (steps == 0) {
			return;
		}

		for (int i = 0; i <= steps; i++) {
			int x = x0 + Math.round((float) (x1 - x0) * i / steps);
			int y = y0 + Math.round((float) (y1 - y0) * i / steps);
			graphics.fill(x, y, x + 1, y + 1, color);
		}
	}

	private int nearest(double mouseX, double mouseY) {
		SwingSettings settings = this.view.get();
		double first = distance(mouseX, mouseY, this.px(settings.x1), this.py(settings.y1));
		double second = distance(mouseX, mouseY, this.px(settings.x2), this.py(settings.y2));
		return first <= second ? 0 : 1;
	}

	private static double distance(double ax, double ay, double bx, double by) {
		return (ax - bx) * (ax - bx) + (ay - by) * (ay - by);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.dragging = this.nearest(event.x(), event.y());
		this.moveHandle(event.x(), event.y());
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dx, double dy) {
		if (this.dragging >= 0) {
			this.moveHandle(event.x(), event.y());
		}
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		this.dragging = -1;
	}

	private void moveHandle(double mouseX, double mouseY) {
		float hx = Mth.clamp((float) ((mouseX - this.left()) / (this.right() - this.left())), 0.0F, 1.0F);
		float hy = Mth.clamp(this.valueAt(mouseY), Curve.MIN_Y, Curve.MAX_Y);
		float rx = Math.round(hx * 100.0F) / 100.0F;
		float ry = Math.round(hy * 100.0F) / 100.0F;
		if (this.dragging == 0) {
			this.edit.accept(settings -> {
				settings.x1 = rx;
				settings.y1 = ry;
			});
		} else {
			this.edit.accept(settings -> {
				settings.x2 = rx;
				settings.y2 = ry;
			});
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, this.getMessage());
	}
}

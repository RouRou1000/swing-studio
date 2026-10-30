package dev.swingstudio.screen;

import dev.swingstudio.Level;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class ModeSelector extends AbstractWidget {
	private final Supplier<Level> get;
	private final Consumer<Level> set;

	ModeSelector(int x, int y, int width, Supplier<Level> get, Consumer<Level> set) {
		super(x, y, width, 14, Component.translatable("swingstudio.mode"));
		this.get = get;
		this.set = set;
	}

	private static Component name(Level level) {
		return Component.translatable("swingstudio.mode." + level.name().toLowerCase(Locale.ROOT));
	}

	private int segment() {
		return this.width / Level.values().length;
	}

	private Level levelAt(double mouseX) {
		int index = (int) ((mouseX - this.getX()) / this.segment());
		return Level.values()[Math.max(0, Math.min(Level.values().length - 1, index))];
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		Level current = this.get.get();
		int x = this.getX();
		int y = this.getY();
		int seg = this.segment();
		Level hoveredLevel = this.isHovered() ? this.levelAt(mouseX) : null;
		for (Level level : Level.values()) {
			int sx = x + level.ordinal() * seg;
			String text = Paper.fit(name(level).getString(), seg - 4);
			int cx = sx + seg / 2;
			if (level == current) {
				Paper.shadow(graphics, sx + 1, y, seg - 2, this.height, 1);
				graphics.fill(sx + 1, y, sx + seg - 1, y + this.height, Paper.RED);
				Paper.centered(graphics, text, cx, y + 3, Paper.SHEET);
			} else {
				Paper.centered(graphics, text, cx, y + 3, level == hoveredLevel ? Paper.INK : Paper.INK_SOFT);
				if (level == hoveredLevel) {
					Paper.dashed(graphics, cx - Paper.font().width(text) / 2, cx + Paper.font().width(text) / 2, y + 12, Paper.INK_SOFT);
				}
			}

			if (level.ordinal() > 0 && level != current && level.ordinal() - 1 != current.ordinal()) {
				graphics.fill(sx, y + 6, sx + 1, y + 8, Paper.INK_FAINT);
			}
		}

		if (hoveredLevel != null) {
			Component tip = Component.translatable("swingstudio.mode." + hoveredLevel.name().toLowerCase(Locale.ROOT) + ".tip");
			graphics.setTooltipForNextFrame(Paper.font().split(tip, 200), mouseX, mouseY);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.set.accept(this.levelAt(event.x()));
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, Component.empty().append(this.getMessage()).append(": ").append(name(this.get.get())));
	}
}

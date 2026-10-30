package dev.swingstudio.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

final class ChoiceRow<T> extends AbstractWidget {
	private static final int LINE = 15;
	private static final int GAP = 9;

	private record Word<T>(T value, String text, int x, int y, int width) {
	}

	private final Component label;
	@Nullable
	private final Component tip;
	private final Supplier<T> get;
	private final Consumer<T> set;
	private final Function<T, Component> tipOf;
	private final List<Word<T>> words = new ArrayList<>();
	private final int labelWidth;

	ChoiceRow(int x, int y, int width, Component label, @Nullable Component tip, T[] values, Function<T, Component> name, Function<T, Component> tipOf, Supplier<T> get, Consumer<T> set) {
		super(x, y, width, LINE, label);
		this.label = label;
		this.tip = tip;
		this.get = get;
		this.set = set;
		this.tipOf = tipOf;
		this.labelWidth = Math.min(Paper.font().width(label) + 8, width / 3);
		int cx = this.labelWidth;
		int cy = 0;
		for (T value : values) {
			String text = name.apply(value).getString();
			int w = Paper.font().width(text);
			if (cx + w > width - 4 && cx > this.labelWidth) {
				cx = this.labelWidth;
				cy += LINE;
			}

			this.words.add(new Word<>(value, text, cx, cy, w));
			cx += w + GAP;
		}

		this.height = cy + LINE;
	}

	@Nullable
	private Word<T> wordAt(double mouseX, double mouseY) {
		for (Word<T> word : this.words) {
			int wx = this.getX() + word.x();
			int wy = this.getY() + word.y();
			if (mouseX >= wx - 3 && mouseX < wx + word.width() + 4 && mouseY >= wy && mouseY < wy + LINE) {
				return word;
			}
		}

		return null;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x = this.getX();
		int y = this.getY();
		Paper.text(graphics, Paper.fit(this.label.getString(), this.labelWidth - 4), x + 2, y + 4, Paper.INK);
		T current = this.get.get();
		Word<T> hovered = this.isHovered() ? this.wordAt(mouseX, mouseY) : null;
		for (Word<T> word : this.words) {
			int wx = x + word.x();
			int wy = y + word.y();
			boolean chosen = word.value() == current;
			if (chosen) {
				Paper.sketch(graphics, wx - 3, wy + 1, word.width() + 6, LINE - 2, Paper.RED);
			}

			Paper.text(graphics, word.text(), wx, wy + 4, chosen ? Paper.INK : Paper.INK_SOFT);
			if (word == hovered && !chosen) {
				Paper.dashed(graphics, wx, wx + word.width(), wy + 12, Paper.INK_SOFT);
			}
		}

		if (hovered != null) {
			graphics.setTooltipForNextFrame(Paper.font().split(this.tipOf.apply(hovered.value()), 200), mouseX, mouseY);
		} else if (this.isHovered() && this.tip != null && mouseX < x + this.labelWidth) {
			graphics.setTooltipForNextFrame(Paper.font().split(this.tip, 200), mouseX, mouseY);
		}
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		Word<T> word = this.wordAt(event.x(), event.y());
		if (word != null) {
			this.set.accept(word.value());
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int direction = event.key() == InputConstants.KEY_RIGHT ? 1 : event.key() == InputConstants.KEY_LEFT ? -1 : 0;
		if (direction == 0) {
			return false;
		}

		T current = this.get.get();
		int index = 0;
		for (int i = 0; i < this.words.size(); i++) {
			if (this.words.get(i).value() == current) {
				index = i;
			}
		}

		this.set.accept(this.words.get(Math.floorMod(index + direction, this.words.size())).value());
		return true;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, this.label);
	}
}

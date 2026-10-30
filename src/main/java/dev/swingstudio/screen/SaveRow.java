package dev.swingstudio.screen;

import dev.swingstudio.ConfigFiles;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class SaveRow extends AbstractButton {
	static final int HEIGHT = 20;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM. HH:mm", Locale.ROOT);

	private final ConfigFiles.Saved saved;
	private final Consumer<ConfigFiles.Saved> load;
	private final Consumer<ConfigFiles.Saved> delete;
	private boolean armed;

	SaveRow(int x, int y, int width, ConfigFiles.Saved saved, Consumer<ConfigFiles.Saved> load, Consumer<ConfigFiles.Saved> delete) {
		super(x, y, width, HEIGHT, Component.literal(saved.name()));
		this.saved = saved;
		this.load = load;
		this.delete = delete;
	}

	private int deleteX() {
		return this.getX() + this.width - 16;
	}

	private int loadX() {
		return this.deleteX() - this.loadWidth() - 4;
	}

	private int loadWidth() {
		return Paper.font().width(Component.translatable("swingstudio.saves.load")) + 10;
	}

	private boolean overDelete(double mouseX) {
		return mouseX >= this.deleteX() - 1;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		if (this.overDelete(event.x())) {
			if (this.armed) {
				this.delete.accept(this.saved);
			} else {
				this.armed = true;
			}
		} else {
			this.armed = false;
			this.load.accept(this.saved);
		}
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.load.accept(this.saved);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x = this.getX();
		int y = this.getY();
		boolean lit = this.isHoveredOrFocused();
		boolean onDelete = this.isHovered() && this.overDelete(mouseX);
		if (!this.isHovered()) {
			this.armed = false;
		}

		Paper.shadow(graphics, x, y, this.width - 2, this.height - 3, lit ? 3 : 2);
		graphics.fill(x, y, x + this.width - 2, y + this.height - 3, Paper.CARD);
		graphics.fill(x, y, x + 3, y + this.height - 3, Paper.BLUE);
		if (lit) {
			graphics.outline(x, y, this.width - 2, this.height - 3, Paper.INK);
		}

		String date = DATE.format(Instant.ofEpochMilli(this.saved.modified()).atZone(ZoneId.systemDefault()));
		int dateWidth = Paper.font().width(date);
		int nameWidth = this.loadX() - x - 12 - dateWidth - 6;
		Paper.text(graphics, Paper.fit(this.saved.name(), nameWidth), x + 7, y + 5, Paper.INK);
		Paper.text(graphics, date, this.loadX() - dateWidth - 6, y + 5, Paper.INK_FAINT);

		boolean onLoad = lit && !onDelete;
		int lx = this.loadX();
		graphics.fill(lx, y + 2, lx + this.loadWidth(), y + 15, onLoad ? Paper.INK : Paper.SHEET_SHADE);
		graphics.outline(lx, y + 2, this.loadWidth(), 13, Paper.INK);
		Paper.text(graphics, Component.translatable("swingstudio.saves.load").getString(), lx + 5, y + 5, onLoad ? Paper.SHEET : Paper.INK);

		int dx = this.deleteX();
		graphics.fill(dx, y + 2, dx + 13, y + 15, this.armed ? Paper.RED : onDelete ? Paper.SHEET_SHADE : Paper.CARD);
		graphics.outline(dx, y + 2, 13, 13, this.armed ? Paper.RED : Paper.INK_SOFT);
		Paper.cross(graphics, dx + 3, y + 5, 7, this.armed ? Paper.SHEET : Paper.RED);
		if (onDelete) {
			graphics.setTooltipForNextFrame(Paper.font().split(Component.translatable(this.armed ? "swingstudio.saves.delete.confirm" : "swingstudio.saves.delete"), 200), mouseX, mouseY);
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}

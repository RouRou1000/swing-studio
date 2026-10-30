package dev.swingstudio.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.swingstudio.SwingConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

final class ItemPickerScreen extends Screen {
	private static final int CELL = 20;
	private static final int CHIP_HEIGHT = 14;
	private static ItemCategory category = ItemCategory.ALL;

	private record Entry(String key, ItemStack stack, Component name, String search) {
	}

	private final Screen parent;
	private final List<String> selection;
	private final List<Entry> entries = new ArrayList<>();
	private List<Entry> shown = List.of();
	private EditBox search;
	private int searchY;
	private int gridLeft;
	private int gridTop;
	private int gridBottom;
	private int columns;
	private int scroll;
	@Nullable
	private Boolean painting;

	ItemPickerScreen(Screen parent, List<String> selection) {
		super(Component.translatable("swingstudio.picker.title"));
		this.parent = parent;
		this.selection = selection;
		Component handName = Component.translatable("swingstudio.empty_hand");
		this.entries.add(new Entry(SwingConfig.key(Items.AIR), ItemStack.EMPTY, handName, searchText(handName, "hand air")));
		for (Item item : BuiltInRegistries.ITEM) {
			if (item == Items.AIR) {
				continue;
			}

			ItemStack stack = item.getDefaultInstance();
			String key = SwingConfig.key(item);
			Component name = stack.getHoverName();
			this.entries.add(new Entry(key, stack, name, searchText(name, key)));
		}
	}

	private static String searchText(Component name, String key) {
		return (name.getString() + " " + key).toLowerCase(Locale.ROOT);
	}

	@Override
	protected void init() {
		int contentWidth = Math.min(this.width - 20, 24 * CELL);
		this.gridLeft = (this.width - contentWidth) / 2;
		int y = 30;
		this.searchY = y;
		this.search = new EditBox(this.font, this.gridLeft + 16, y + 3, contentWidth - 20, 10, this.search, Component.translatable("swingstudio.picker.search"));
		this.search.setBordered(false);
		this.search.setTextColor(Paper.INK);
		this.search.setTextShadow(false);
		this.search.setResponder(text -> this.applyFilter());
		this.addRenderableWidget(this.search);
		y += 20;

		int x = this.gridLeft;
		for (ItemCategory each : ItemCategory.values()) {
			int chipWidth = this.font.width(each.label()) + 10;
			if (x + chipWidth > this.gridLeft + contentWidth) {
				x = this.gridLeft;
				y += CHIP_HEIGHT + 4;
			}

			this.addRenderableWidget(new StampButton(x, y, chipWidth, CHIP_HEIGHT, each::label, () -> {
				category = each;
				this.applyFilter();
			}, () -> category == each));
			x += chipWidth + 4;
		}

		this.gridTop = y + CHIP_HEIGHT + 6;
		this.columns = Math.max(1, (contentWidth - 6) / CELL);
		this.gridBottom = this.gridTop + Math.max(1, (this.height - 50 - this.gridTop) / CELL) * CELL;

		int buttonY = this.height - 24;
		int buttonWidth = (contentWidth - 12) / 4;
		this.addRenderableWidget(StampButton.of(this.gridLeft, buttonY, buttonWidth, 16, Component.translatable("swingstudio.picker.held"), this::addHeld)
			.tooltip(Component.translatable("swingstudio.picker.held.tip")));
		this.addRenderableWidget(StampButton.of(this.gridLeft + (buttonWidth + 4), buttonY, buttonWidth, 16, Component.translatable("swingstudio.picker.all_shown"), this::selectShown)
			.tooltip(Component.translatable("swingstudio.picker.all_shown.tip")));
		this.addRenderableWidget(StampButton.of(this.gridLeft + 2 * (buttonWidth + 4), buttonY, buttonWidth, 16, Component.translatable("swingstudio.picker.clear"), this.selection::clear)
			.tooltip(Component.translatable("swingstudio.picker.clear.tip")));
		this.addRenderableWidget(StampButton.of(this.gridLeft + 3 * (buttonWidth + 4), buttonY, contentWidth - 3 * (buttonWidth + 4) - 2, 16, CommonComponents.GUI_DONE, this::onClose));

		this.setInitialFocus(this.search);
		this.applyFilter();
	}

	private void applyFilter() {
		String query = this.search.getValue().trim().toLowerCase(Locale.ROOT);
		List<Entry> matches = new ArrayList<>();
		for (Entry entry : this.entries) {
			if (category.test(entry.stack(), entry.key()) && (query.isEmpty() || entry.search().contains(query))) {
				matches.add(entry);
			}
		}

		this.shown = matches;
		this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
	}

	private int visibleRows() {
		return Math.max(1, (this.gridBottom - this.gridTop) / CELL);
	}

	private int maxScroll() {
		int rows = Mth.positiveCeilDiv(this.shown.size(), this.columns);
		return Math.max(0, rows - this.visibleRows());
	}

	private void addHeld() {
		if (this.minecraft.player != null) {
			String key = SwingConfig.key(this.minecraft.player.getMainHandItem());
			if (!this.selection.contains(key)) {
				this.selection.add(key);
			}
		}
	}

	private void selectShown() {
		for (Entry entry : this.shown) {
			if (!this.selection.contains(entry.key())) {
				this.selection.add(entry.key());
			}
		}
	}

	private int indexAt(double mouseX, double mouseY) {
		if (mouseX < this.gridLeft || mouseX >= this.gridLeft + this.columns * CELL || mouseY < this.gridTop || mouseY >= this.gridBottom) {
			return -1;
		}

		int column = (int) ((mouseX - this.gridLeft) / CELL);
		int row = (int) ((mouseY - this.gridTop) / CELL) + this.scroll;
		int index = row * this.columns + column;
		return index < this.shown.size() ? index : -1;
	}

	private void paint(int index) {
		if (index < 0 || this.painting == null) {
			return;
		}

		String key = this.shown.get(index).key();
		if (this.painting) {
			if (!this.selection.contains(key)) {
				this.selection.add(key);
			}
		} else {
			this.selection.remove(key);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		Paper.centered(graphics, this.title.getString(), this.width / 2, 7, Paper.RED);
		Paper.centered(graphics, Paper.fit(Component.translatable("swingstudio.picker.help").getString(), this.width - 30), this.width / 2, 18, Paper.INK_SOFT);
		boolean typing = this.search.isFocused();
		Paper.magnifier(graphics, this.gridLeft + 2, this.searchY + 2, typing ? Paper.RED : Paper.INK_SOFT);
		Paper.dashed(graphics, this.gridLeft + 14, this.gridLeft + this.columns * CELL + 6, this.searchY + 14, typing ? Paper.RED : Paper.INK_FAINT);
		if (this.search.getValue().isEmpty() && !typing) {
			Paper.text(graphics, Component.translatable("swingstudio.picker.search"), this.gridLeft + 16, this.searchY + 3, Paper.INK_FAINT);
		}

		int gridRight = this.gridLeft + this.columns * CELL;
		Paper.shadow(graphics, this.gridLeft - 2, this.gridTop - 2, gridRight - this.gridLeft + 10, this.gridBottom - this.gridTop + 4, 2);
		graphics.fill(this.gridLeft - 2, this.gridTop - 2, gridRight + 8, this.gridBottom + 2, Paper.CARD);
		graphics.outline(this.gridLeft - 2, this.gridTop - 2, gridRight - this.gridLeft + 10, this.gridBottom - this.gridTop + 4, Paper.INK);
		int hovered = this.indexAt(mouseX, mouseY);
		int first = this.scroll * this.columns;
		int last = Math.min(this.shown.size(), first + this.visibleRows() * this.columns);
		SwingConfig config = SwingConfig.get();
		for (int index = first; index < last; index++) {
			Entry entry = this.shown.get(index);
			int cx = this.gridLeft + (index % this.columns) * CELL;
			int cy = this.gridTop + (index / this.columns - this.scroll) * CELL;
			boolean selected = this.selection.contains(entry.key());
			if (selected) {
				graphics.fill(cx, cy, cx + CELL, cy + CELL, 0x40B0382A);
				Paper.sketch(graphics, cx, cy, CELL, CELL, Paper.RED);
			} else if (index == hovered) {
				graphics.fill(cx, cy, cx + CELL, cy + CELL, Paper.SHEET_SHADE);
			}

			if (entry.stack().isEmpty()) {
				Paper.hand(graphics, cx + 2, cy + 2);
			} else {
				graphics.item(entry.stack(), cx + 2, cy + 2);
			}

			if (config.hasOwn(entry.key())) {
				graphics.fill(cx + CELL - 5, cy + 1, cx + CELL - 1, cy + 5, Paper.MUSTARD);
			}
		}

		if (this.shown.isEmpty()) {
			Paper.centered(graphics, Component.translatable("swingstudio.picker.none").getString(), (this.gridLeft + gridRight) / 2, this.gridTop + 8, Paper.INK_SOFT);
		}

		int maxScroll = this.maxScroll();
		if (maxScroll > 0) {
			int trackHeight = this.gridBottom - this.gridTop;
			int thumbHeight = Math.max(10, trackHeight * this.visibleRows() / (this.visibleRows() + maxScroll));
			int thumbY = this.gridTop + (trackHeight - thumbHeight) * this.scroll / maxScroll;
			graphics.fill(gridRight + 4, this.gridTop, gridRight + 5, this.gridBottom, Paper.INK_FAINT);
			graphics.fill(gridRight + 3, thumbY, gridRight + 6, thumbY + thumbHeight, Paper.INK);
		}

		this.drawSelectionLine(graphics, gridRight);
		super.extractRenderState(graphics, mouseX, mouseY, a);

		if (hovered >= 0 && this.painting == null) {
			Entry entry = this.shown.get(hovered);
			List<Component> lines = new ArrayList<>();
			lines.add(entry.name());
			lines.add(Component.translatable(config.hasOwn(entry.key()) ? "swingstudio.picker.own" : "swingstudio.picker.follows_all"));
			graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
		}
	}

	private void drawSelectionLine(GuiGraphicsExtractor graphics, int gridRight) {
		int y = this.gridBottom + 6;
		Component text;
		if (this.selection.isEmpty()) {
			text = Component.translatable("swingstudio.picker.selected.none");
		} else {
			StringBuilder names = new StringBuilder();
			for (String key : this.selection) {
				if (!names.isEmpty()) {
					names.append(", ");
				}

				names.append(nameOf(key).getString());
				if (names.length() > 200) {
					break;
				}
			}

			text = Component.translatable("swingstudio.picker.selected", this.selection.size(), names.toString());
		}

		Paper.text(graphics, Paper.fit(text.getString(), gridRight - this.gridLeft + 6), this.gridLeft, y, this.selection.isEmpty() ? Paper.INK_SOFT : Paper.BLUE);
	}

	static Component nameOf(String key) {
		Item item = itemOf(key);
		return item == Items.AIR ? Component.translatable("swingstudio.empty_hand") : item.getDefaultInstance().getHoverName();
	}

	static Item itemOf(String key) {
		net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.tryParse(key);
		return id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}

		int index = this.indexAt(event.x(), event.y());
		if (index >= 0 && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			this.setFocused(null);
			this.painting = !this.selection.contains(this.shown.get(index).key());
			this.paint(index);
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (this.painting != null) {
			this.paint(this.indexAt(event.x(), event.y()));
			return true;
		}

		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.painting != null) {
			this.painting = null;
			return true;
		}

		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (scrollY != 0.0) {
			this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY) * 2, 0, this.maxScroll());
			return true;
		}

		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_PAGEDOWN) {
			this.scroll = Mth.clamp(this.scroll + this.visibleRows(), 0, this.maxScroll());
			return true;
		}

		if (event.key() == InputConstants.KEY_PAGEUP) {
			this.scroll = Mth.clamp(this.scroll - this.visibleRows(), 0, this.maxScroll());
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int contentWidth = this.columns * CELL + 6;
		int left = this.gridLeft - 12;
		Paper.sheet(graphics, left, 2, left + contentWidth + 24, this.height - 2, 1000, false);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}

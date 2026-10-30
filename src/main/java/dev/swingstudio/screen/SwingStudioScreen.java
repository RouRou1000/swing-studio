package dev.swingstudio.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.swingstudio.ConfigFiles;
import dev.swingstudio.Level;
import dev.swingstudio.Presets;
import dev.swingstudio.Preview;
import dev.swingstudio.SwingConfig;
import dev.swingstudio.SwingSettings;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class SwingStudioScreen extends Screen implements Options.Context {
	private static final int PAD = 8;
	private static final int GAP = 3;
	private static final int SCROLL_STEP = 18;
	private static final int UNDO_LIMIT = 60;

	private record Placed(AbstractWidget widget, int baseY, @Nullable String optionId) {
	}

	private enum PreviewSpeed {
		FULL(1.0F),
		HALF(0.5F),
		QUARTER(0.25F);

		final float scale;

		PreviewSpeed(float scale) {
			this.scale = scale;
		}
	}

	private static Options.Page page = Options.Page.PRESETS;
	private static Presets.@Nullable Category presetFilter;
	private static final List<String> TARGETS = new ArrayList<>();
	private static final Set<String> COLLAPSED = new HashSet<>();
	private static final Deque<String> UNDO = new ArrayDeque<>();
	private static final Deque<String> REDO = new ArrayDeque<>();
	private static boolean onlyChanged;
	private static String saveName = "";
	@Nullable
	private static Component status;
	private static int statusColor = Paper.INK_SOFT;
	@Nullable
	private static SwingSettings clipboard;
	private static boolean session;
	@Nullable
	private static CameraType sessionCamera;
	private static PreviewSpeed previewSpeed = PreviewSpeed.FULL;
	private static boolean freeze;
	private static float freezeMoment = 0.25F;

	@Nullable
	private final Screen parent;
	private final List<Placed> content = new ArrayList<>();
	private final Set<GuiEventListener> contentSet = new HashSet<>();
	private EditBox search;
	private boolean openingPicker;
	private boolean changing;
	private int swingTimer;
	private int panelRight;
	private int left;
	private int innerWidth;
	private int summaryY;
	private int searchY;
	private int searchWidth;
	private int contentTop;
	private int contentBottom;
	private int contentHeight;
	private int scroll;
	private int cardX;
	private int cardY;
	private int cardWidth;
	private int cardHeight;
	private int hintTop;
	private int bookmarksRight;
	@Nullable
	private PresetCard previewedCard;
	private boolean previewedInverted;
	@Nullable
	private String hintId;
	private IconStamp undoButton;
	private IconStamp redoButton;

	public SwingStudioScreen(@Nullable Screen parent) {
		super(Component.translatable("swingstudio.title"));
		this.parent = parent;
	}

	private static boolean editingAll() {
		return TARGETS.isEmpty();
	}

	@Override
	public SwingSettings view() {
		SwingConfig config = SwingConfig.get();
		return editingAll() ? config.all : config.settingsFor(TARGETS.get(0));
	}

	@Override
	public void edit(Consumer<SwingSettings> change) {
		this.beginChange();
		SwingConfig config = SwingConfig.get();
		if (editingAll()) {
			change.accept(config.all);
		} else {
			for (String key : TARGETS) {
				change.accept(config.own(key));
			}
		}
	}

	@Override
	public void beginChange() {
		if (!this.changing) {
			UNDO.push(SwingConfig.snapshot());
			while (UNDO.size() > UNDO_LIMIT) {
				UNDO.removeLast();
			}

			REDO.clear();
			this.changing = true;
		}
	}

	private void endChange() {
		if (this.changing) {
			this.changing = false;
			SwingConfig.save();
			if (!freeze) {
				this.swingNow();
			}
		}
	}

	private void undo() {
		if (!UNDO.isEmpty()) {
			REDO.push(SwingConfig.snapshot());
			this.restoreKeepingMode(UNDO.pop());
		}
	}

	private void redo() {
		if (!REDO.isEmpty()) {
			UNDO.push(SwingConfig.snapshot());
			this.restoreKeepingMode(REDO.pop());
		}
	}

	private void restoreKeepingMode(String snapshot) {
		Level current = mode();
		SwingConfig.restore(snapshot);
		SwingConfig.get().mode = current;
		SwingConfig.save();
		this.rebuildWidgets();
	}

	@Override
	public void refresh() {
		this.buildContent();
	}

	@Override
	public void copySettings() {
		clipboard = this.view().copy();
	}

	@Override
	public void pasteSettings() {
		if (clipboard != null) {
			SwingSettings copied = clipboard;
			this.edit(settings -> settings.copyFrom(copied));
		}
	}

	@Override
	public boolean canPaste() {
		return clipboard != null;
	}

	@Override
	public void clearAllItems() {
		this.beginChange();
		SwingConfig.get().items.clear();
		TARGETS.clear();
		this.endChange();
		this.rebuildWidgets();
	}

	private void updatePreview() {
		if (editingAll()) {
			Preview.set(null);
		} else {
			Item item = ItemPickerScreen.itemOf(TARGETS.get(0));
			Preview.set(item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance());
		}

		Preview.setTimeScale(previewSpeed.scale);
		Preview.setFrozen(freeze ? freezeMoment : null);
	}

	private static Level mode() {
		return SwingConfig.get().mode;
	}

	private int changedOn(Options.Page each) {
		SwingSettings view = this.view();
		int count = 0;
		for (Options.Option option : Options.ALL) {
			if (option.page == each && option.changed(view)) {
				count++;
			}
		}

		return count;
	}

	private int changedTotal() {
		int count = 0;
		for (Options.Page each : Options.Page.values()) {
			count += this.changedOn(each);
		}

		return count;
	}

	@Override
	protected void init() {
		this.openingPicker = false;
		this.content.clear();
		this.contentSet.clear();
		TARGETS.removeIf(key -> {
			Identifier id = Identifier.tryParse(key);
			return id == null || !BuiltInRegistries.ITEM.containsKey(id);
		});
		if (!session) {
			session = true;
			if (this.minecraft.player != null) {
				sessionCamera = this.minecraft.options.getCameraType();
				this.minecraft.options.setCameraType(CameraType.FIRST_PERSON);
			}
		}

		if (!Options.pageVisible(page, mode())) {
			page = Options.Page.PRESETS;
		}

		this.updatePreview();
		int panelWidth = Math.min(Mth.clamp(this.width * 9 / 20, 226, 312), this.width - 74);
		this.panelRight = panelWidth;
		this.left = PAD;
		this.innerWidth = panelWidth - PAD - 6;
		int x = this.left;
		int w = this.innerWidth;
		SwingConfig config = SwingConfig.get();

		this.addRenderableWidget(new CheckToggle(x + w - 62, 6, 62, () -> Component.translatable("swingstudio.enabled"), () -> SwingConfig.get().enabled, () -> {
			this.beginChange();
			SwingConfig.get().enabled ^= true;
		}, Component.translatable("swingstudio.enabled.tip")));

		int titleRight = x + this.font.width(this.title.getString().toUpperCase(Locale.ROOT)) + 16;
		int dipRight = x + w - 62 - 6;
		int dipWidth = Math.min(this.font.width(Component.translatable("swingstudio.equip.short")) + 16, dipRight - titleRight);
		this.addRenderableWidget(new CheckToggle(dipRight - dipWidth, 6, dipWidth, () -> Component.translatable("swingstudio.equip.short"), () -> SwingConfig.get().equipAnimation, () -> {
			this.beginChange();
			SwingConfig.get().equipAnimation ^= true;
		}, Component.translatable("swingstudio.equip.tip")));

		this.addRenderableWidget(new ModeSelector(x, 24, w, SwingStudioScreen::mode, level -> {
			config.mode = level;
			SwingConfig.save();
			this.rebuildWidgets();
		}));

		int y = 43;
		int half = (w - 6) / 2;
		this.addRenderableWidget(new StampButton(x, y, half, 15, () -> Component.translatable("swingstudio.target.all"), () -> {
			TARGETS.clear();
			this.rebuildWidgets();
		}, SwingStudioScreen::editingAll).tooltip(Component.translatable("swingstudio.target.all.tip")));
		this.addRenderableWidget(new StampButton(x + half + 4, y, w - half - 6, 15, () -> Component.translatable(
			editingAll() ? "swingstudio.target.pick" : "swingstudio.target.change"
		), this::openPicker, () -> !editingAll()).tooltip(Component.translatable("swingstudio.target.pick.tip")));
		y += 20;
		this.summaryY = y;
		int customWidth = this.customWidth();
		this.addRenderableWidget(new CheckToggle(x + w - customWidth, y + 1, customWidth, () -> Component.translatable("swingstudio.custom"), () -> this.view().custom, () -> {
			boolean custom = !this.view().custom;
			this.edit(settings -> settings.custom = custom);
		}, Component.translatable("swingstudio.custom.tip")));
		y += 20;

		this.searchY = y;
		int filterWidth = Math.min(w / 3, this.font.width(Component.translatable("swingstudio.only_changed", 99)) + 10);
		this.searchWidth = w - filterWidth - 4;
		this.search = new EditBox(this.font, x + 16, y + 3, this.searchWidth - 30, 10, this.search, Component.translatable("swingstudio.search"));
		this.search.setBordered(false);
		this.search.setTextColor(Paper.INK);
		this.search.setTextShadow(false);
		this.search.setMaxLength(40);
		this.search.setResponder(text -> {
			this.scroll = 0;
			this.buildContent();
		});
		this.addRenderableWidget(this.search);
		this.addRenderableWidget(new StampButton(x + w - filterWidth - 2, y, filterWidth, 13, () -> Component.translatable("swingstudio.only_changed", this.changedTotal()), () -> {
			onlyChanged = !onlyChanged;
			this.scroll = 0;
			this.buildContent();
		}, () -> onlyChanged).tooltip(Component.translatable("swingstudio.only_changed.tip")));

		this.contentTop = y + 18;
		this.contentBottom = this.height - 26;

		int bookmarkY = 43;
		this.bookmarksRight = this.panelRight;
		for (Options.Page each : Options.Page.values()) {
			if (!Options.pageVisible(each, mode())) {
				continue;
			}

			Bookmark bookmark = this.addRenderableWidget(new Bookmark(this.panelRight - 2, bookmarkY, each.title(), each.color,
				() -> page == each && this.query().isEmpty() && !onlyChanged,
				() -> this.openPage(each),
				() -> this.changedOn(each)));
			this.bookmarksRight = Math.max(this.bookmarksRight, bookmark.getRight());
			bookmarkY += 19;
		}

		int buttonY = this.height - 21;
		IconStamp undo = new IconStamp(x, buttonY, 15, IconStamp.Icon.UNDO, Component.translatable("swingstudio.undo"), this::undo);
		undo.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("swingstudio.undo.tip")));
		this.undoButton = this.addRenderableWidget(undo);
		IconStamp redo = new IconStamp(x + 19, buttonY, 15, IconStamp.Icon.REDO, Component.translatable("swingstudio.redo"), this::redo);
		redo.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("swingstudio.redo.tip")));
		this.redoButton = this.addRenderableWidget(redo);
		int rest = w - 40;
		int buttonWidth = (rest - 6) / 2;
		this.addRenderableWidget(StampButton.of(x + 40, buttonY, buttonWidth, 15, Component.translatable("swingstudio.reset"), this::resetTargets)
			.tooltip(Component.translatable(editingAll() ? "swingstudio.reset.all.tip" : "swingstudio.reset.items.tip")));
		this.addRenderableWidget(StampButton.of(x + 40 + buttonWidth + 4, buttonY, rest - buttonWidth - 6, 15, CommonComponents.GUI_DONE, this::onClose));

		this.buildPreviewCard();
		this.buildContent();
	}

	private void openPage(Options.Page each) {
		page = each;
		onlyChanged = false;
		this.scroll = 0;
		if (!this.search.getValue().isEmpty()) {
			this.search.setValue("");
		} else {
			this.buildContent();
		}
	}

	private int customWidth() {
		return Math.min(this.innerWidth / 2, this.font.width(Component.translatable("swingstudio.custom")) + 18);
	}

	private String query() {
		return this.search == null ? "" : this.search.getValue().trim().toLowerCase(Locale.ROOT);
	}

	private void buildPreviewCard() {
		this.cardWidth = Math.min(172, this.width - 6 - this.bookmarksRight - 6);
		this.cardHeight = 0;
		if (this.cardWidth < 104) {
			return;
		}

		this.cardX = this.width - this.cardWidth - 6;
		this.cardY = 24;
		int x = this.cardX + 6;
		int w = this.cardWidth - 12;
		int y = this.cardY + 16;
		boolean world = this.minecraft.player != null;

		IconStamp play = new IconStamp(x, y, 15, IconStamp.Icon.PLAY, Component.translatable("swingstudio.test"), this::swingNow);
		play.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("swingstudio.test.tip")));
		play.active = world && !freeze;
		this.addRenderableWidget(play);
		this.addRenderableWidget(new CheckToggle(x + 21, y + 1, w - 21, () -> Component.translatable("swingstudio.auto_preview.short"),
			() -> SwingConfig.get().autoPreview, () -> {
				SwingConfig.get().autoPreview ^= true;
				SwingConfig.save();
			}, Component.translatable("swingstudio.auto_preview.tip")));
		y += 20;

		ChoiceRow<PreviewSpeed> speedRow = this.addRenderableWidget(new ChoiceRow<>(x, y, w, Component.translatable("swingstudio.preview_speed"), Component.translatable("swingstudio.preview_speed.tip"),
			PreviewSpeed.values(),
			speed -> Component.literal(Math.round(speed.scale * 100) + " %"),
			speed -> Component.translatable("swingstudio.preview_speed.tip"),
			() -> previewSpeed,
			speed -> {
				previewSpeed = speed;
				Preview.setTimeScale(speed.scale);
				this.swingNow();
			}));
		y += speedRow.getHeight() + 2;

		this.addRenderableWidget(new CheckToggle(x, y, w, () -> Component.translatable("swingstudio.freeze"), () -> freeze, () -> {
			freeze = !freeze;
			Preview.setFrozen(freeze ? freezeMoment : null);
			this.rebuildWidgets();
		}, Component.translatable("swingstudio.freeze.tip")));
		y += 15;
		this.addRenderableWidget(new RulerSlider(x, y, w, Component.translatable("swingstudio.freeze_moment"), 0, 100, 1, 25,
			() -> freezeMoment * 100.0,
			value -> {
				freezeMoment = (float) (value / 100.0);
				if (freeze) {
					Preview.setFrozen(freezeMoment);
				}
			},
			value -> Math.round(value) + " %"
		).enabledWhen(() -> freeze).tooltip(Component.translatable("swingstudio.freeze_moment.tip")));
		y += RulerSlider.HEIGHT + 2;

		this.addRenderableWidget(new StampButton(x, y, w - 2, 15, () -> Component.translatable("swingstudio.compare"), () -> {
			Preview.setCompare(!Preview.compare());
			this.swingNow();
		}, Preview::compare).tooltip(Component.translatable("swingstudio.compare.tip")));
		y += 21;

		this.hintTop = y;
		this.cardHeight = y + Hints.height(w) + 4 - this.cardY;
	}

	private void drawPreviewCard(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (this.cardHeight == 0) {
			return;
		}

		int x = this.cardX;
		int y = this.cardY;
		Paper.shadow(graphics, x, y, this.cardWidth, this.cardHeight, 3);
		graphics.fill(x, y, x + this.cardWidth, y + this.cardHeight, Paper.SHEET);
		graphics.fill(x + this.cardWidth / 2 - 14, y - 3, x + this.cardWidth / 2 + 14, y + 4, 0xA0FFFFFF);
		Component title = this.previewedCard != null
			? Component.translatable(this.previewedInverted ? "swingstudio.preview.trying_inverted" : "swingstudio.preview.trying", this.previewedCard.getMessage())
			: Preview.compare() ? Component.translatable("swingstudio.preview.original") : Component.translatable("swingstudio.preview.title");
		Paper.text(graphics, Paper.fit(title.getString(), this.cardWidth - 12), x + 6, y + 5, this.previewedCard != null || Preview.compare() ? Paper.RED : Paper.INK);
		Paper.dashed(graphics, x + 6, x + this.cardWidth - 6, y + 14, Paper.INK_FAINT);
		Paper.dashed(graphics, x + 6, x + this.cardWidth - 6, this.hintTop - 2, Paper.INK_FAINT);
		Hints.draw(graphics, this.hintId, x + 6, this.hintTop + 2, this.cardWidth - 12, this.previewIcon());
		if (this.minecraft.player == null) {
			int ty = this.cardY + this.cardHeight + 10;
			for (var line : this.font.split(Component.translatable("swingstudio.preview.no_world"), this.cardWidth)) {
				graphics.text(this.font, line, x, ty, 0xFFE8E0CC, true);
				ty += 10;
			}
		}
	}

	private ItemStack previewIcon() {
		ItemStack shown = Preview.stack();
		if (shown != null) {
			return shown;
		}

		LocalPlayer player = this.minecraft.player;
		return player == null ? new ItemStack(Items.DIAMOND_SWORD) : player.getMainHandItem();
	}

	private void buildContent() {
		for (Placed placed : this.content) {
			this.removeWidget(placed.widget());
		}

		this.content.clear();
		this.contentSet.clear();
		this.previewedCard = null;
		Preview.setSettings(null);
		int x = this.left + 2;
		int w = this.innerWidth - 8;
		int y = 2;
		String query = this.query();
		if (!query.isEmpty()) {
			y = this.buildSearch(query, x, y, w);
		} else if (onlyChanged) {
			y = this.buildChanged(x, y, w);
		} else {
			y = this.hiddenNote(x, y, w);
			if (page == Options.Page.PRESETS) {
				y = this.buildPresets(x, y, w, null);
			} else if (page == Options.Page.CONFIGS) {
				y = this.buildConfigs(x, y, w);
			} else {
				y = this.buildPage(page, x, y, w);
			}
		}

		this.contentHeight = y + 4;
		this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
		this.layoutContent();
	}

	private int buildConfigs(int x, int y, int w) {
		int color = Options.Page.CONFIGS.color;
		if (status != null) {
			y = this.place(new TextBlock(x, y, w, status, statusColor), y, null);
		}

		y = this.place(new Heading(x, y, w, Component.translatable("swingstudio.saves.heading"), color), y, null);
		int buttonWidth = Math.max(60, this.font.width(Component.translatable("swingstudio.saves.save")) + 14);
		EditBox name = new EditBox(this.font, x + 4, y + 4, w - buttonWidth - 14, 10, Component.translatable("swingstudio.saves.name"));
		name.setBordered(false);
		name.setTextColor(Paper.INK);
		name.setTextShadow(false);
		name.setMaxLength(32);
		name.setValue(saveName);
		name.setHint(Component.translatable("swingstudio.saves.name").copy().withStyle(style -> style.withColor(Paper.INK_FAINT)));
		name.setResponder(text -> saveName = text);
		this.place(new NameLine(x, y, w - buttonWidth - 6, name), y, null);
		this.place(name, y, null);
		y = this.place(new StampButton(x + w - buttonWidth - 2, y + 1, buttonWidth, 15, () -> Component.translatable(
			ConfigFiles.exists(saveName) ? "swingstudio.saves.overwrite" : "swingstudio.saves.save"
		), this::saveCurrent, null).tooltip(Component.translatable("swingstudio.saves.save.tip")), y, null);

		List<ConfigFiles.Saved> saves = ConfigFiles.list();
		y = this.place(new Heading(x, y + 2, w, Component.translatable("swingstudio.saves.list", saves.size()), color), y + 2, null);
		if (saves.isEmpty()) {
			y = this.place(new TextBlock(x, y, w, Component.translatable("swingstudio.saves.empty"), Paper.INK_SOFT), y, null);
		}

		for (ConfigFiles.Saved saved : saves) {
			y = this.place(new SaveRow(x, y, w, saved, this::loadSave, this::deleteSave), y, null) - 2;
		}

		y = this.place(new Heading(x, y + 4, w, Component.translatable("swingstudio.share.heading"), color), y + 4, null);
		int half = (w - 6) / 2;
		this.place(StampButton.of(x, y + 1, half, 15, Component.translatable("swingstudio.share.copy"), this::copyCode)
			.tooltip(Component.translatable("swingstudio.share.copy.tip")), y, null);
		y = this.place(StampButton.of(x + half + 4, y + 1, w - half - 6, 15, Component.translatable("swingstudio.share.paste"), this::pasteCode)
			.tooltip(Component.translatable("swingstudio.share.paste.tip")), y, null) + 2;
		this.place(StampButton.of(x, y + 1, half, 15, Component.translatable("swingstudio.share.export"), this::exportFile)
			.tooltip(Component.translatable("swingstudio.share.export.tip")), y, null);
		y = this.place(StampButton.of(x + half + 4, y + 1, w - half - 6, 15, Component.translatable("swingstudio.share.folder"), () -> ConfigFiles.open(ConfigFiles.EXPORTS))
			.tooltip(Component.translatable("swingstudio.share.folder.tip")), y, null) + 2;
		return this.place(new TextBlock(x, y + 2, w, Component.translatable("swingstudio.share.help"), Paper.INK_SOFT), y + 2, null);
	}

	private static void report(String key, int color, Object... args) {
		status = Component.translatable(key, args);
		statusColor = color;
	}

	private void saveCurrent() {
		String name = ConfigFiles.cleanName(saveName);
		if (name.isEmpty()) {
			report("swingstudio.saves.need_name", Paper.RED);
		} else if (ConfigFiles.save(name)) {
			report("swingstudio.saves.saved", Paper.GREEN, name);
			saveName = "";
		} else {
			report("swingstudio.saves.failed", Paper.RED);
		}

		this.buildContent();
	}

	private void loadSave(ConfigFiles.Saved saved) {
		this.beginChange();
		boolean ok = ConfigFiles.load(saved);
		this.endChange();
		if (ok) {
			report("swingstudio.saves.loaded", Paper.GREEN, saved.name());
		} else {
			report("swingstudio.saves.broken", Paper.RED, saved.name());
		}

		this.rebuildWidgets();
	}

	private void deleteSave(ConfigFiles.Saved saved) {
		ConfigFiles.delete(saved);
		report("swingstudio.saves.deleted", Paper.INK_SOFT, saved.name());
		this.buildContent();
	}

	private void copyCode() {
		this.minecraft.keyboardHandler.setClipboard(ConfigFiles.exportCode());
		report("swingstudio.share.copied", Paper.GREEN);
		this.buildContent();
	}

	private void pasteCode() {
		this.importAndReport(() -> ConfigFiles.importText(this.minecraft.keyboardHandler.getClipboard()), "swingstudio.share.pasted", "swingstudio.share.invalid");
	}

	private void exportFile() {
		Path file = ConfigFiles.exportFile();
		if (file != null) {
			report("swingstudio.share.exported", Paper.GREEN, file.getFileName().toString());
		} else {
			report("swingstudio.saves.failed", Paper.RED);
		}

		this.buildContent();
	}

	private void importAndReport(java.util.function.BooleanSupplier action, String okKey, String failKey) {
		this.beginChange();
		boolean ok = action.getAsBoolean();
		this.endChange();
		report(ok ? okKey : failKey, ok ? Paper.GREEN : Paper.RED);
		page = Options.Page.CONFIGS;
		this.rebuildWidgets();
	}

	@Override
	public void onFilesDrop(List<Path> files) {
		for (Path file : files) {
			this.importAndReport(() -> ConfigFiles.importFile(file), "swingstudio.share.dropped", "swingstudio.share.invalid");
			return;
		}
	}

	private int buildPage(Options.Page each, int x, int y, int w) {
		List<Options.Option> options = new ArrayList<>();
		for (Options.Option option : Options.ALL) {
			if (option.page == each && mode().shows(option.level)) {
				options.add(option);
			}
		}

		SwingSettings view = this.view();
		boolean folded = false;
		for (int i = 0; i < options.size(); i++) {
			Options.Option option = options.get(i);
			if (option instanceof Options.HeadingOption) {
				int changed = 0;
				for (int j = i + 1; j < options.size() && !(options.get(j) instanceof Options.HeadingOption); j++) {
					if (options.get(j).changed(view)) {
						changed++;
					}
				}

				String id = option.id;
				folded = COLLAPSED.contains(id);
				y = this.place(new Heading(x, y, w, option.label(), each.color, folded, changed, () -> {
					if (!COLLAPSED.remove(id)) {
						COLLAPSED.add(id);
					}

					this.buildContent();
				}), y, null);
			} else if (!folded) {
				y = this.place(option.build(this, x, y, w), y, option.id);
			}
		}

		return y;
	}

	private int buildChanged(int x, int y, int w) {
		SwingSettings view = this.view();
		int found = 0;
		for (Options.Page each : Options.Page.values()) {
			boolean headed = false;
			for (Options.Option option : Options.ALL) {
				if (option.page != each || !option.changed(view)) {
					continue;
				}

				if (!headed) {
					y = this.place(new Heading(x, y, w, each.title(), each.color), y, null);
					headed = true;
				}

				y = this.place(option.build(this, x, y, w), y, option.id);
				found++;
			}
		}

		if (found == 0) {
			y = this.place(new TextBlock(x, y + 6, w, Component.translatable("swingstudio.only_changed.none"), Paper.INK_SOFT), y + 6, null);
		}

		return y;
	}

	private int place(List<AbstractWidget> widgets, int y, @Nullable String optionId) {
		int bottom = y;
		for (AbstractWidget widget : widgets) {
			this.addWidget(widget);
			this.content.add(new Placed(widget, widget.getY(), optionId));
			this.contentSet.add(widget);
			bottom = Math.max(bottom, widget.getY() + widget.getHeight());
		}

		return bottom + GAP;
	}

	private int place(AbstractWidget widget, int y, @Nullable String optionId) {
		return this.place(List.of(widget), y, optionId);
	}

	private int hiddenNote(int x, int y, int w) {
		SwingSettings view = this.view();
		int count = 0;
		Level needed = mode();
		for (Options.Option option : Options.ALL) {
			if (!mode().shows(option.level) && option.changed(view)) {
				count++;
				if (option.level.ordinal() > needed.ordinal()) {
					needed = option.level;
				}
			}
		}

		if (count == 0) {
			return y;
		}

		Level reveal = needed;
		Component text = Component.translatable("swingstudio.hidden_note", count, modeName(reveal));
		return this.place(new StickyNote(x, y, w, text, () -> {
			SwingConfig.get().mode = reveal;
			SwingConfig.save();
			this.rebuildWidgets();
		}), y, null) + 2;
	}

	private static Component modeName(Level level) {
		return Component.translatable("swingstudio.mode." + level.name().toLowerCase(Locale.ROOT));
	}

	private int buildPresets(int x, int y, int w, @Nullable String query) {
		if (query == null) {
			Presets.Category[] filters = new Presets.Category[Presets.Category.values().length + 1];
			System.arraycopy(Presets.Category.values(), 0, filters, 1, Presets.Category.values().length);
			y = this.place(new ChoiceRow<>(x, y, w, Component.translatable("swingstudio.preset_filter"), null, filters,
				category -> Component.translatable(category == null ? "swingstudio.preset_filter.all" : "swingstudio.category." + category.name().toLowerCase(Locale.ROOT)),
				category -> Component.translatable(category == null ? "swingstudio.preset_filter.all.tip" : "swingstudio.category." + category.name().toLowerCase(Locale.ROOT) + ".tip"),
				() -> presetFilter,
				category -> {
					presetFilter = category;
					this.scroll = 0;
					this.buildContent();
				}), y, null);
			if (mode().shows(Level.NORMAL)) {
				y = this.place(new CheckToggle(x, y, w, () -> Component.translatable("swingstudio.keep_hold"), () -> SwingConfig.get().keepHoldOnPreset, () -> {
					SwingConfig.get().keepHoldOnPreset ^= true;
					SwingConfig.save();
				}, Component.translatable("swingstudio.keep_hold.tip")), y, null);
			}

			y += 2;
		}

		int columns = w >= 200 ? 2 : 1;
		int cardWidth = (w - (columns - 1) * 3) / columns;
		int index = 0;
		int rowY = y;
		for (Presets.Preset preset : Presets.ALL) {
			if (query == null ? presetFilter != null && preset.category() != presetFilter : !matchesPreset(preset, query)) {
				continue;
			}

			int column = index % columns;
			if (column == 0 && index > 0) {
				rowY += PresetCard.HEIGHT + 1;
			}

			this.place(new PresetCard(x + column * (cardWidth + 3), rowY, cardWidth, preset, this::applyPreset), rowY, null);
			index++;
		}

		return index == 0 ? y : rowY + PresetCard.HEIGHT + GAP;
	}

	private static boolean matchesPreset(Presets.Preset preset, String query) {
		String haystack = (Component.translatable(preset.nameKey()).getString() + " " + Component.translatable(preset.descriptionKey()).getString() + " "
			+ Component.translatable("swingstudio.category." + preset.category().name().toLowerCase(Locale.ROOT)).getString() + " " + preset.id())
			.toLowerCase(Locale.ROOT);
		return Options.matches(haystack, query);
	}

	private void applyPreset(Presets.Preset preset, boolean inverted) {
		boolean keepHold = SwingConfig.get().keepHoldOnPreset;
		this.edit(settings -> Presets.apply(settings, preset, keepHold, inverted));
		this.previewedCard = null;
		Preview.setSettings(null);
	}

	private int buildSearch(String query, int x, int y, int w) {
		int found = 0;
		int hidden = 0;
		Level needed = mode();
		boolean presetsMatch = false;
		for (Presets.Preset preset : Presets.ALL) {
			presetsMatch |= matchesPreset(preset, query);
		}

		if (presetsMatch) {
			y = this.place(new Heading(x, y, w, Options.Page.PRESETS.title(), Options.Page.PRESETS.color), y, null);
			y = this.buildPresets(x, y, w, query);
			found++;
		}

		for (Options.Page each : Options.Page.values()) {
			boolean headed = false;
			for (Options.Option option : Options.ALL) {
				if (option.page != each || !option.searchable() || !Options.matches(option.haystack(), query)) {
					continue;
				}

				if (!mode().shows(option.level)) {
					hidden++;
					if (option.level.ordinal() > needed.ordinal()) {
						needed = option.level;
					}

					continue;
				}

				if (!headed) {
					y = this.place(new Heading(x, y, w, each.title(), each.color), y, null);
					headed = true;
				}

				y = this.place(option.build(this, x, y, w), y, option.id);
				found++;
			}
		}

		if (hidden > 0) {
			Level reveal = needed;
			y = this.place(new StickyNote(x, y + 2, w, Component.translatable("swingstudio.search.more", hidden, modeName(reveal)), () -> {
				SwingConfig.get().mode = reveal;
				SwingConfig.save();
				this.rebuildWidgets();
			}), y + 2, null);
		}

		if (found == 0 && hidden == 0) {
			y = this.place(new TextBlock(x, y + 6, w, Component.translatable("swingstudio.search.none"), Paper.INK_SOFT), y + 6, null);
		}

		return y;
	}

	private int maxScroll() {
		return Math.max(0, this.contentHeight - (this.contentBottom - this.contentTop));
	}

	private void layoutContent() {
		for (Placed placed : this.content) {
			AbstractWidget widget = placed.widget();
			widget.setY(this.contentTop + placed.baseY() - this.scroll);
			widget.visible = widget.getY() + widget.getHeight() > this.contentTop && widget.getY() < this.contentBottom;
		}
	}

	@Override
	public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
		boolean inside = mouseY >= this.contentTop && mouseY < this.contentBottom && mouseX < this.panelRight;
		for (GuiEventListener child : this.children()) {
			if (!inside && this.contentSet.contains(child)) {
				continue;
			}

			if (child.isMouseOver(mouseX, mouseY)) {
				return Optional.of(child);
			}
		}

		return Optional.empty();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (mouseX < this.panelRight && mouseY >= this.contentTop && mouseY < this.contentBottom && scrollY != 0.0) {
			this.scroll = Mth.clamp(this.scroll - (int) Math.round(scrollY * SCROLL_STEP), 0, this.maxScroll());
			this.layoutContent();
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void openPicker() {
		this.openingPicker = true;
		this.minecraft.gui.setScreen(new ItemPickerScreen(this, TARGETS));
	}

	private void resetTargets() {
		this.beginChange();
		SwingConfig config = SwingConfig.get();
		if (editingAll()) {
			config.all = new SwingSettings();
		} else {
			for (String key : TARGETS) {
				config.items.remove(key);
			}
		}

		this.endChange();
		this.rebuildWidgets();
	}

	private void swingNow() {
		LocalPlayer player = this.minecraft.player;
		if (player != null && !freeze) {
			ItemStack stack = Preview.stackInHand(player, InteractionHand.MAIN_HAND);
			player.swing(InteractionHand.MAIN_HAND, stack.getAttackAnimation(), false);
			this.swingTimer = 0;
		}
	}

	@Override
	public void tick() {
		LocalPlayer player = this.minecraft.player;
		SwingConfig config = SwingConfig.get();
		if (player == null || !config.autoPreview || freeze) {
			return;
		}

		ItemStack stack = Preview.stackInHand(player, InteractionHand.MAIN_HAND);
		SwingSettings settings = config.active(stack);
		int speed = settings == null ? 100 : settings.speed;
		int swingTicks = Mth.ceil(Math.max(1, stack.getAttackAnimation().duration()) * 100.0F / speed / previewSpeed.scale);
		if (++this.swingTimer >= swingTicks + config.previewPause) {
			this.swingNow();
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		if (this.minecraft.level == null) {
			super.extractBackground(graphics, mouseX, mouseY, a);
		}

		Paper.sheet(graphics, 0, 0, this.panelRight, this.height, this.contentTop + 11, true);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		this.drawTitle(graphics);
		this.drawSummary(graphics, mouseX, mouseY);
		this.drawSearchLine(graphics);
		this.drawPreviewCard(graphics, mouseX, mouseY);
		this.undoButton.active = !UNDO.isEmpty();
		this.redoButton.active = !REDO.isEmpty();
		super.extractRenderState(graphics, mouseX, mouseY, a);

		PresetCard.hovered = null;
		boolean inside = mouseY >= this.contentTop && mouseY < this.contentBottom && mouseX < this.panelRight;
		int contentMouseX = inside ? mouseX : -1000;
		int contentMouseY = inside ? mouseY : -1000;
		graphics.enableScissor(0, this.contentTop, this.panelRight, this.contentBottom);
		String hovered = null;
		for (Placed placed : this.content) {
			AbstractWidget widget = placed.widget();
			if (widget.visible) {
				widget.extractRenderState(graphics, contentMouseX, contentMouseY, a);
				if (placed.optionId() != null && (widget.isHovered() || widget.isFocused() && this.isDragging())) {
					hovered = placed.optionId();
				}
			}
		}

		graphics.disableScissor();
		if (hovered != null) {
			this.hintId = hovered;
		} else if (inside) {
			this.hintId = null;
		}

		this.drawScrollbar(graphics);
		this.updatePresetPreview();
	}

	private void drawTitle(GuiGraphicsExtractor graphics) {
		String text = this.title.getString().toUpperCase(Locale.ROOT);
		int w = this.font.width(text) + 10;
		graphics.pose().pushMatrix();
		graphics.pose().translate(this.left + w / 2.0F + 1, 11.0F);
		graphics.pose().rotate(-0.045F);
		Paper.sketch(graphics, -w / 2, -6, w, 13, Paper.RED);
		Paper.text(graphics, text, -w / 2 + 5, -3, Paper.RED);
		graphics.pose().popMatrix();
	}

	private void drawSearchLine(GuiGraphicsExtractor graphics) {
		int x = this.left;
		int y = this.searchY;
		int w = this.searchWidth;
		boolean focused = this.search.isFocused();
		Paper.magnifier(graphics, x + 2, y + 2, focused ? Paper.RED : Paper.INK_SOFT);
		Paper.dashed(graphics, x + 14, x + w - 2, y + 14, focused ? Paper.RED : Paper.INK_FAINT);
		if (this.search.getValue().isEmpty() && !focused) {
			Paper.text(graphics, Paper.fit(Component.translatable("swingstudio.search").getString(), w - 32), x + 16, y + 3, Paper.INK_FAINT);
		}

		if (!this.search.getValue().isEmpty()) {
			Paper.cross(graphics, x + w - 11, y + 3, 7, Paper.INK_SOFT);
		}
	}

	private boolean overClearSearch(double mouseX, double mouseY) {
		int x = this.left + this.searchWidth - 13;
		return !this.search.getValue().isEmpty() && mouseX >= x && mouseX < x + 12 && mouseY >= this.searchY && mouseY < this.searchY + 13;
	}

	private void drawScrollbar(GuiGraphicsExtractor graphics) {
		int maxScroll = this.maxScroll();
		if (maxScroll <= 0) {
			return;
		}

		int x = this.left + this.innerWidth - 3;
		int track = this.contentBottom - this.contentTop;
		int thumb = Math.max(14, track * track / this.contentHeight);
		int thumbY = this.contentTop + (track - thumb) * this.scroll / maxScroll;
		graphics.fill(x + 1, this.contentTop, x + 2, this.contentBottom, Paper.INK_FAINT);
		graphics.fill(x, thumbY, x + 3, thumbY + thumb, Paper.INK);
	}

	private void updatePresetPreview() {
		PresetCard card = PresetCard.hovered;
		boolean inverted = card != null && PresetCard.hoveredInverted;
		if (card == this.previewedCard && inverted == this.previewedInverted) {
			return;
		}

		this.previewedCard = card;
		this.previewedInverted = inverted;
		if (card == null) {
			Preview.setSettings(null);
			return;
		}

		SwingSettings trying = this.view().copy();
		Presets.apply(trying, card.preset, SwingConfig.get().keepHoldOnPreset, inverted);
		Preview.setSettings(trying);
		this.swingNow();
	}

	private void drawSummary(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int x = this.left;
		int y = this.summaryY;
		int w = this.innerWidth - this.customWidth() - 4;
		if (editingAll()) {
			int own = SwingConfig.get().items.size();
			Component text = own == 0
				? Component.translatable("swingstudio.summary.all")
				: Component.translatable("swingstudio.summary.all_except", own);
			Paper.text(graphics, Paper.fit(text.getString(), w), x + 2, y + 4, Paper.INK_SOFT);
			return;
		}

		int slots = Math.max(1, Math.min(TARGETS.size(), (w - 40) / 17));
		for (int i = 0; i < slots; i++) {
			Item item = ItemPickerScreen.itemOf(TARGETS.get(i));
			if (item == Items.AIR) {
				Paper.hand(graphics, x + i * 17, y);
			} else {
				graphics.item(item.getDefaultInstance(), x + i * 17, y);
			}
		}

		int textX = x + slots * 17 + 3;
		Component text = TARGETS.size() == 1
			? ItemPickerScreen.nameOf(TARGETS.get(0))
			: Component.translatable("swingstudio.summary.items", TARGETS.size());
		Paper.text(graphics, Paper.fit(text.getString(), x + w - textX), textX, y + 4, Paper.BLUE);
		if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 17) {
			List<Component> lines = new ArrayList<>();
			for (String key : TARGETS) {
				lines.add(ItemPickerScreen.nameOf(key));
				if (lines.size() >= 20) {
					lines.add(Component.literal("…"));
					break;
				}
			}

			graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.overClearSearch(event.x(), event.y())) {
			this.search.setValue("");
			return true;
		}

		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}

		if (!editingAll() && event.x() >= this.left && event.x() < this.left + this.innerWidth / 2 && event.y() >= this.summaryY && event.y() < this.summaryY + 17) {
			this.openPicker();
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean handled = super.mouseReleased(event);
		this.endChange();
		return handled;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.hasControlDown() && event.key() == InputConstants.KEY_Z) {
			if (event.hasShiftDown()) {
				this.redo();
			} else {
				this.undo();
			}

			return true;
		}

		if (event.hasControlDown() && event.key() == InputConstants.KEY_Y) {
			this.redo();
			return true;
		}

		if (event.hasControlDown() && event.key() == InputConstants.KEY_F) {
			this.setFocused(this.search);
			return true;
		}

		boolean handled = super.keyPressed(event);
		this.endChange();
		return handled;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		this.endChange();
		SwingConfig.save();
		Preview.setSettings(null);
		this.previewedCard = null;
		if (!this.openingPicker) {
			endSession(this.minecraft);
		}
	}

	private static void endSession(Minecraft minecraft) {
		if (!session) {
			return;
		}

		session = false;
		Preview.reset();
		previewSpeed = PreviewSpeed.FULL;
		freeze = false;
		if (sessionCamera != null) {
			minecraft.options.setCameraType(sessionCamera);
			sessionCamera = null;
		}
	}

	public static void checkSession(Minecraft minecraft) {
		Screen screen = minecraft.gui.screen();
		if (session && !(screen instanceof SwingStudioScreen) && !(screen instanceof ItemPickerScreen)) {
			endSession(minecraft);
		}
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}

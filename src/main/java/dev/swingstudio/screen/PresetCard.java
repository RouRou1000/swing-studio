package dev.swingstudio.screen;

import dev.swingstudio.Presets;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;

final class PresetCard extends AbstractButton {
	static final int HEIGHT = 21;
	private static final int FLIP = 14;
	private static final long STAMP_MS = 1100L;

	@Nullable
	static PresetCard hovered;
	static boolean hoveredInverted;

	final Presets.Preset preset;
	private final BiConsumer<Presets.Preset, Boolean> apply;
	private final Component description;
	private static final long TIP_DELAY_MS = 600L;
	private long hoverStart;
	private long stampedAt;
	private boolean stampedInverted;

	PresetCard(int x, int y, int width, Presets.Preset preset, BiConsumer<Presets.Preset, Boolean> apply) {
		super(x, y, width, HEIGHT, Component.translatable(preset.nameKey()));
		this.preset = preset;
		this.apply = apply;
		this.description = Component.translatable(preset.descriptionKey());
	}

	private int flipX() {
		return this.getX() + this.width - FLIP - 6;
	}

	private int flipY() {
		return this.getY() + (this.height - 3 - FLIP) / 2;
	}

	private boolean overFlip(double mouseX, double mouseY) {
		return mouseX >= this.flipX() - 2 && mouseX < this.flipX() + FLIP + 2 && mouseY >= this.flipY() - 2 && mouseY < this.flipY() + FLIP + 2;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.choose(this.overFlip(event.x(), event.y()));
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.choose(false);
	}

	private void choose(boolean inverted) {
		this.apply.accept(this.preset, inverted);
		this.stampedAt = System.currentTimeMillis();
		this.stampedInverted = inverted;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		boolean lit = this.isHoveredOrFocused();
		boolean onFlip = this.isHovered() && this.overFlip(mouseX, mouseY);
		if (this.isHovered()) {
			hovered = this;
			hoveredInverted = onFlip;
		}

		int lift = lit ? 1 : 0;
		int x = this.getX() - lift;
		int y = this.getY() - lift;
		Paper.shadow(graphics, this.getX(), this.getY(), this.width - 2, this.height - 3, lit ? 3 : 2);
		graphics.fill(x, y, x + this.width - 2, y + this.height - 3, Paper.CARD);
		graphics.fill(x, y, x + 3, y + this.height - 3, this.preset.category().color);
		if (lit) {
			graphics.outline(x, y, this.width - 2, this.height - 3, Paper.INK);
		}

		int textWidth = this.width - 12 - FLIP - 6;
		Component bold = this.getMessage().copy().withStyle(Style.EMPTY.withBold(true));
		if (Paper.font().width(bold) <= textWidth) {
			graphics.text(Paper.font(), bold, x + 7, y + 5, Paper.INK, false);
		} else {
			Paper.text(graphics, Paper.fit(this.getMessage().getString(), textWidth), x + 7, y + 5, Paper.INK);
		}
		this.drawFlip(graphics, this.flipX() - lift, this.flipY() - lift, onFlip);

		long now = System.currentTimeMillis();
		if (now - this.stampedAt < STAMP_MS) {
			this.stamp(graphics, x + this.width - 48, y + 9);
		}

		if (!this.isHovered()) {
			this.hoverStart = 0L;
		} else if (this.hoverStart == 0L) {
			this.hoverStart = now;
		} else if (now - this.hoverStart > TIP_DELAY_MS) {
			Component tip = onFlip ? Component.translatable("swingstudio.invert.tip") : this.description;
			graphics.setTooltipForNextFrame(Paper.font().split(tip, 200), mouseX, mouseY);
		}
	}

	private void drawFlip(GuiGraphicsExtractor graphics, int x, int y, boolean lit) {
		graphics.fill(x, y, x + FLIP, y + FLIP, lit ? Paper.INK : Paper.SHEET_SHADE);
		graphics.outline(x, y, FLIP, FLIP, Paper.INK);
		int ink = lit ? Paper.SHEET : Paper.INK;
		int red = lit ? Paper.NOTE : Paper.RED;
		graphics.fill(x + 4, y + 4, x + 11, y + 5, ink);
		Paper.triangleLeft(graphics, x + 2, y + 4, 3, ink);
		graphics.fill(x + 3, y + 9, x + 10, y + 10, red);
		Paper.triangleRight(graphics, x + 10, y + 9, 3, red);
	}

	private void stamp(GuiGraphicsExtractor graphics, int centerX, int centerY) {
		String text = Component.translatable(this.stampedInverted ? "swingstudio.applied_inverted" : "swingstudio.applied").getString();
		int w = Paper.font().width(text) + 8;
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, centerY);
		graphics.pose().rotate(-0.22F);
		graphics.fill(-w / 2, -7, w / 2, 7, 0xD0F8F2E2);
		Paper.sketch(graphics, -w / 2, -7, w, 14, Paper.RED);
		Paper.text(graphics, text, -w / 2 + 4, -3, Paper.RED);
		graphics.pose().popMatrix();
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}

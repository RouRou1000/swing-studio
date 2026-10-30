package dev.swingstudio;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class Preview {
	@Nullable
	private static ItemStack stack;
	@Nullable
	private static SwingSettings settings;
	private static float timeScale = 1.0F;
	@Nullable
	private static Float frozen;
	private static boolean compare;

	private Preview() {
	}

	public static @Nullable ItemStack stack() {
		return stack;
	}

	public static void set(@Nullable ItemStack shown) {
		stack = shown;
	}

	public static @Nullable SwingSettings settings() {
		return settings;
	}

	public static void setSettings(@Nullable SwingSettings trying) {
		settings = trying;
	}

	public static float timeScale() {
		return timeScale;
	}

	public static void setTimeScale(float scale) {
		timeScale = scale;
	}

	public static @Nullable Float frozen() {
		return frozen;
	}

	public static void setFrozen(@Nullable Float moment) {
		frozen = moment;
	}

	public static boolean compare() {
		return compare;
	}

	public static void setCompare(boolean on) {
		compare = on;
	}

	public static void reset() {
		stack = null;
		settings = null;
		timeScale = 1.0F;
		frozen = null;
		compare = false;
	}

	public static ItemStack stackInHand(LocalPlayer player, InteractionHand hand) {
		return hand == InteractionHand.MAIN_HAND && stack != null ? stack : player.getItemInHand(hand);
	}
}

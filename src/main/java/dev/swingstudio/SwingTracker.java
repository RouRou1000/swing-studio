package dev.swingstudio;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public final class SwingTracker {
	private static final float NANOS_PER_TICK = 5.0E7F;

	private static final class HandSwing {
		@Nullable
		Object swing;
		long startNanos;
		float durationTicks;
		float carry;
		int count;
		float random;
	}

	private static final HandSwing[] HANDS = {new HandSwing(), new HandSwing()};
	private static final java.util.Random RANDOM = new java.util.Random();
	private static float pendingTicks;

	private SwingTracker() {
	}

	public static int scaleDuration(LocalPlayer player, InteractionHand hand, int vanillaTicks, int baseTicks) {
		SwingConfig config = SwingConfig.get();
		if (config.enabled && config.ignoreEffects && baseTicks > 0) {
			vanillaTicks = baseTicks;
		}

		pendingTicks = vanillaTicks;
		if (vanillaTicks <= 0) {
			return vanillaTicks;
		}

		SwingSettings settings = config.active(Preview.stackInHand(player, hand));
		float ticks = settings == null ? vanillaTicks : vanillaTicks * 100.0F / settings.speed;
		ticks /= Preview.timeScale();
		pendingTicks = ticks;
		return Math.abs(ticks - vanillaTicks) < 1.0E-3F ? vanillaTicks : Math.max(1, Mth.ceil(ticks - 1.0E-3F));
	}

	public static void started(LocalPlayer player, InteractionHand hand) {
		HandSwing state = HANDS[hand.ordinal()];
		long now = System.nanoTime();
		float previous = state.swing != null ? raw(state, now) : 1.0F;
		state.carry = previous > 0.0F && previous < 1.0F ? previous : 0.0F;
		state.swing = player.getCurrentSwing();
		state.startNanos = now;
		state.durationTicks = Math.max(pendingTicks, 0.01F);
		state.count++;
		state.random = RANDOM.nextFloat() * 2.0F - 1.0F;
	}

	public static int count(InteractionHand hand) {
		return HANDS[hand.ordinal()].count;
	}

	public static float random(InteractionHand hand) {
		return HANDS[hand.ordinal()].random;
	}

	public static float progress(LivingEntity.@Nullable SwingDescription current, InteractionHand hand, float vanilla) {
		HandSwing state = HANDS[hand.ordinal()];
		if (current == null || state.swing != current) {
			return vanilla;
		}

		return raw(state, System.nanoTime());
	}

	private static float raw(HandSwing state, long now) {
		float elapsed = (now - state.startNanos) / NANOS_PER_TICK;
		if (state.carry > 0.0F) {
			if (elapsed < 1.0F) {
				return state.carry + (1.0F - state.carry) * elapsed;
			}

			elapsed -= 1.0F;
		}

		return Math.min(1.0F, elapsed / state.durationTicks);
	}
}

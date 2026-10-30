package dev.swingstudio;

public final class Curve {
	public static final float MIN_Y = -0.6F;
	public static final float MAX_Y = 1.6F;

	public enum Preset {
		LINEAR(0.0F, 0.0F, 1.0F, 1.0F),
		SMOOTH(0.45F, 0.0F, 0.55F, 1.0F),
		FAST_START(0.1F, 0.6F, 0.3F, 1.0F),
		SLOW_START(0.7F, 0.0F, 0.9F, 0.4F),
		SNAP(0.85F, 0.0F, 0.15F, 1.0F),
		WIND_UP(0.4F, -0.6F, 0.6F, 1.0F),
		OVERSHOOT(0.3F, 0.0F, 0.4F, 1.6F),
		BOTH(0.65F, -0.55F, 0.3F, 1.55F);

		public final float x1;
		public final float y1;
		public final float x2;
		public final float y2;

		Preset(float x1, float y1, float x2, float y2) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}

		public boolean matches(SwingSettings settings) {
			return near(this.x1, settings.x1) && near(this.y1, settings.y1) && near(this.x2, settings.x2) && near(this.y2, settings.y2);
		}

		public static Preset of(SwingSettings settings) {
			for (Preset preset : values()) {
				if (preset.matches(settings)) {
					return preset;
				}
			}

			return null;
		}

		private static boolean near(float a, float b) {
			return Math.abs(a - b) < 0.005F;
		}
	}

	private Curve() {
	}

	public static float evaluate(float x1, float y1, float x2, float y2, float t) {
		if (t <= 0.0F) {
			return 0.0F;
		}

		if (t >= 1.0F) {
			return 1.0F;
		}

		if (x1 == y1 && x2 == y2) {
			return t;
		}

		return bezier(y1, y2, solveX(x1, x2, t));
	}

	private static float bezier(float p1, float p2, float s) {
		float inv = 1.0F - s;
		return 3.0F * inv * inv * s * p1 + 3.0F * inv * s * s * p2 + s * s * s;
	}

	private static float slope(float p1, float p2, float s) {
		float inv = 1.0F - s;
		return 3.0F * inv * inv * p1 + 6.0F * inv * s * (p2 - p1) + 3.0F * s * s * (1.0F - p2);
	}

	private static float solveX(float x1, float x2, float x) {
		float s = x;
		for (int i = 0; i < 6; i++) {
			float error = bezier(x1, x2, s) - x;
			if (Math.abs(error) < 1.0E-5F) {
				return s;
			}

			float d = slope(x1, x2, s);
			if (Math.abs(d) < 1.0E-5F) {
				break;
			}

			s -= error / d;
		}

		float lo = 0.0F;
		float hi = 1.0F;
		s = x;
		for (int i = 0; i < 30; i++) {
			float value = bezier(x1, x2, s);
			if (Math.abs(value - x) < 1.0E-5F) {
				break;
			}

			if (value < x) {
				lo = s;
			} else {
				hi = s;
			}

			s = (lo + hi) * 0.5F;
		}

		return s;
	}
}

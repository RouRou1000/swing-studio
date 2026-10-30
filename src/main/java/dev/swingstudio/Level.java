package dev.swingstudio;

public enum Level {
	EASY,
	NORMAL,
	HARD,
	EXPERT;

	public boolean shows(Level required) {
		return required.ordinal() <= this.ordinal();
	}
}

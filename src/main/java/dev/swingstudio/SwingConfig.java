package dev.swingstudio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class SwingConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve(SwingStudio.MOD_ID + ".json");

	private static SwingConfig instance = new SwingConfig();

	public boolean enabled = true;
	public Level mode = Level.NORMAL;
	public boolean equipAnimation = true;
	public boolean autoPreview = true;
	public int previewPause = 12;
	public boolean ignoreEffects;
	public boolean keepHoldOnPreset = true;
	public SwingSettings all = new SwingSettings();
	public TreeMap<String, SwingSettings> items = new TreeMap<>();

	public static SwingConfig get() {
		return instance;
	}

	public static String key(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	public static String key(ItemStack stack) {
		return key(stack.isEmpty() ? Items.AIR : stack.getItem());
	}

	public SwingSettings settingsFor(String key) {
		SwingSettings own = this.items.get(key);
		return own != null ? own : this.all;
	}

	public SwingSettings settingsFor(ItemStack stack) {
		return this.settingsFor(key(stack));
	}

	public @Nullable SwingSettings active(ItemStack stack) {
		if (!this.enabled || Preview.compare()) {
			return null;
		}

		SwingSettings trying = Preview.settings();
		if (trying != null) {
			return trying;
		}

		SwingSettings settings = this.settingsFor(stack);
		return settings.custom ? settings : null;
	}

	public boolean hasOwn(String key) {
		return this.items.containsKey(key);
	}

	public SwingSettings own(String key) {
		return this.items.computeIfAbsent(key, k -> this.all.copy());
	}

	public static String snapshot() {
		return GSON.toJson(instance);
	}

	public static void restore(String json) {
		SwingConfig restored = GSON.fromJson(json, SwingConfig.class);
		if (restored != null) {
			instance = restored;
			instance.sanitize();
			save();
		}
	}

	public static void load() {
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE)) {
				SwingConfig loaded = GSON.fromJson(reader, SwingConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (IOException | RuntimeException e) {
				SwingStudio.LOGGER.warn("Could not read {}, using defaults", FILE, e);
			}
		}

		instance.sanitize();
	}

	public static void save() {
		instance.sanitize();
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			SwingStudio.LOGGER.warn("Could not write {}", FILE, e);
		}
	}

	private void sanitize() {
		if (this.mode == null) {
			this.mode = Level.NORMAL;
		}

		this.previewPause = Mth.clamp(this.previewPause, 0, 60);
		if (this.all == null) {
			this.all = new SwingSettings();
		}

		this.all.sanitize();
		if (this.items == null) {
			this.items = new TreeMap<>();
		}

		this.items.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
		for (Map.Entry<String, SwingSettings> entry : this.items.entrySet()) {
			entry.getValue().sanitize();
		}
	}
}

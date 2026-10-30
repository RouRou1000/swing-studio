package dev.swingstudio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;

public final class ConfigFiles {
	private static final Path ROOT = FabricLoader.getInstance().getConfigDir().resolve(SwingStudio.MOD_ID);
	public static final Path SAVES = ROOT.resolve("saves");
	public static final Path EXPORTS = ROOT.resolve("exports");
	private static final String CODE_PREFIX = "SWS1:";
	private static final int MAX_NAME = 32;

	public record Saved(String name, Path file, long modified) {
	}

	private ConfigFiles() {
	}

	public static String cleanName(String name) {
		String cleaned = name.trim().replaceAll("[^\\p{L}\\p{N} _-]", "").replaceAll("\\s+", " ");
		return cleaned.length() > MAX_NAME ? cleaned.substring(0, MAX_NAME).trim() : cleaned;
	}

	public static List<Saved> list() {
		List<Saved> saves = new ArrayList<>();
		if (!Files.isDirectory(SAVES)) {
			return saves;
		}

		try (Stream<Path> files = Files.list(SAVES)) {
			files.filter(file -> file.getFileName().toString().endsWith(".json")).forEach(file -> {
				String fileName = file.getFileName().toString();
				long modified = 0L;
				try {
					modified = Files.getLastModifiedTime(file).toMillis();
				} catch (IOException ignored) {
				}

				saves.add(new Saved(fileName.substring(0, fileName.length() - 5), file, modified));
			});
		} catch (IOException e) {
			SwingStudio.LOGGER.warn("Could not list {}", SAVES, e);
		}

		saves.sort(Comparator.comparingLong(Saved::modified).reversed());
		return saves;
	}

	public static boolean exists(String name) {
		String cleaned = cleanName(name);
		return !cleaned.isEmpty() && Files.exists(SAVES.resolve(cleaned + ".json"));
	}

	public static boolean save(String name) {
		String cleaned = cleanName(name);
		if (cleaned.isEmpty()) {
			return false;
		}

		try {
			Files.createDirectories(SAVES);
			Files.writeString(SAVES.resolve(cleaned + ".json"), SwingConfig.snapshot(), StandardCharsets.UTF_8);
			return true;
		} catch (IOException e) {
			SwingStudio.LOGGER.warn("Could not save setup {}", cleaned, e);
			return false;
		}
	}

	public static boolean load(Saved saved) {
		try {
			return apply(Files.readString(saved.file(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			SwingStudio.LOGGER.warn("Could not read {}", saved.file(), e);
			return false;
		}
	}

	public static boolean delete(Saved saved) {
		try {
			return Files.deleteIfExists(saved.file());
		} catch (IOException e) {
			SwingStudio.LOGGER.warn("Could not delete {}", saved.file(), e);
			return false;
		}
	}

	public static String exportCode() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
			gzip.write(SwingConfig.snapshot().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		return CODE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
	}

	public static boolean importText(@Nullable String text) {
		if (text == null) {
			return false;
		}

		String trimmed = text.trim();
		if (trimmed.startsWith(CODE_PREFIX)) {
			try {
				byte[] zipped = Base64.getUrlDecoder().decode(trimmed.substring(CODE_PREFIX.length()).replaceAll("\\s", ""));
				try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(zipped))) {
					return apply(new String(gzip.readNBytes(4 * 1024 * 1024), StandardCharsets.UTF_8));
				}
			} catch (IOException | IllegalArgumentException e) {
				return false;
			}
		}

		return apply(trimmed);
	}

	public static boolean importFile(Path file) {
		try {
			if (Files.size(file) > 4 * 1024 * 1024) {
				return false;
			}

			return importText(Files.readString(file, StandardCharsets.UTF_8));
		} catch (IOException e) {
			return false;
		}
	}

	public static @Nullable Path exportFile() {
		try {
			Files.createDirectories(EXPORTS);
			String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT));
			Path file = EXPORTS.resolve("swing-studio_" + stamp + ".json");
			Files.writeString(file, SwingConfig.snapshot(), StandardCharsets.UTF_8);
			return file;
		} catch (IOException e) {
			SwingStudio.LOGGER.warn("Could not export", e);
			return null;
		}
	}

	public static void open(Path folder) {
		try {
			Files.createDirectories(folder);
			String path = folder.toAbsolutePath().toString();
			String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
			ProcessBuilder process = os.contains("win") ? new ProcessBuilder("explorer", path)
				: os.contains("mac") ? new ProcessBuilder("open", path)
				: new ProcessBuilder("xdg-open", path);
			process.start();
		} catch (IOException e) {
			SwingStudio.LOGGER.warn("Could not open {}", folder, e);
		}
	}

	private static boolean apply(String json) {
		try {
			JsonElement parsed = JsonParser.parseString(json);
			if (!parsed.isJsonObject()) {
				return false;
			}

			JsonObject object = parsed.getAsJsonObject();
			if (!object.has("all") && !object.has("items")) {
				return false;
			}

			Level mode = SwingConfig.get().mode;
			SwingConfig.restore(json);
			SwingConfig.get().mode = mode;
			SwingConfig.save();
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}

package dev.alist.progressivedifficulty.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.alist.progressivedifficulty.ProgressiveDifficulty;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("progressivedifficulty.json");

	private static ModConfig config = new ModConfig();

	private ConfigManager() {
	}

	public static void initialize() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());

			if (Files.notExists(CONFIG_PATH)) {
				config.validate();
				save();
				return;
			}

			try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
				ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
				config = loaded == null ? new ModConfig() : loaded;
				config.validate();
			}
			save();
		} catch (IOException | JsonParseException exception) {
			ProgressiveDifficulty.LOGGER.error(
					"Could not load {}; using default settings for this session.",
					CONFIG_PATH,
					exception
			);
			config = new ModConfig();
		}
	}

	public static ModConfig get() {
		return config;
	}

	private static void save() throws IOException {
		try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
			GSON.toJson(config, writer);
		}
	}
}

package pl.botmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Konfiguracja moda – plik config/botmod.json. */
public class BotConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Maksymalna liczba botów na jednego gracza. */
	public int maxBotowNaGracza = 5;

	public static BotConfig wczytaj() {
		Path plik = FabricLoader.getInstance().getConfigDir().resolve("botmod.json");
		BotConfig cfg = new BotConfig();
		try {
			if (Files.exists(plik)) {
				BotConfig wczytany = GSON.fromJson(Files.readString(plik), BotConfig.class);
				if (wczytany != null) cfg = wczytany;
			}
			if (cfg.maxBotowNaGracza < 1) cfg.maxBotowNaGracza = 1;
			Files.writeString(plik, GSON.toJson(cfg)); // zapis (tworzy plik przy pierwszym uruchomieniu)
		} catch (IOException | RuntimeException e) {
			BotMod.LOGGER.error("Nie udało się wczytać konfiguracji BotMod, używam domyślnej.", e);
		}
		return cfg;
	}
}

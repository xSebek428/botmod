package pl.botmod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Ustawienia wyglądu panelu botów (zapisywane w config/botmod-client.json). */
public class ClientSettings {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final int[] KOLORY = {0x3B82F6, 0x22C55E, 0xEF4444, 0xA855F7, 0xF59E0B, 0xEC4899};

	public int kolor = 0x3B82F6;
	public boolean zaokraglone = true;
	public boolean dzwieki = true;
	public boolean cienTekstu = true;
	/** 0 = Ciemny, 1 = Jasny, 2 = Podstawowy */
	public int motyw = 2;

	private static ClientSettings instancja;

	private static Path plik() {
		return FabricLoader.getInstance().getConfigDir().resolve("botmod-client.json");
	}

	public static ClientSettings get() {
		if (instancja == null) {
			try {
				if (Files.exists(plik())) instancja = GSON.fromJson(Files.readString(plik()), ClientSettings.class);
			} catch (Exception ignored) {}
			if (instancja == null) instancja = new ClientSettings();
		}
		return instancja;
	}

	public void zapisz() {
		try { Files.writeString(plik(), GSON.toJson(this)); } catch (Exception ignored) {}
	}
}

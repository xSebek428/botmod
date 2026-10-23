package pl.botmod.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel botów (klawisz B) w stylu „Mod Menu”: boczne menu z zakładkami (Boty / Ustawienia / Pomoc)
 * i jasne wiersze po prawej. Rysowany ręcznie; kliknięcia wysyłają zwykłe komendy /bot ...
 */
public class BotScreen extends Screen {
	private record Obszar(int x, int y, int w, int h, Runnable akcja) {
		boolean trafia(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
	}

	private static final String[] ZAKLADKI = {"Boty", "Ustawienia", "Pomoc"};
	private static final String[] MOTYWY = {"Ciemny", "Jasny", "Podstawowy"};

	private final List<String> nazwy;
	private final ClientSettings cfg = ClientSettings.get();
	private final List<Obszar> obszary = new ArrayList<>();
	private int zakladka = 0;
	private int przewiniecie = 0;

	// aktualna paleta
	private int tlo, boczne, wiersz, tekst, przycisk;

	public BotScreen(List<String> nazwy) {
		super(Text.literal("Panel botów"));
		this.nazwy = nazwy;
	}

	private void paleta() {
		switch (cfg.motyw) {
			case 0 -> { tlo = 0xFF202020; boczne = 0xFF141414; wiersz = 0xFF333333; tekst = 0xFFFFFFFF; przycisk = 0xFF0E0E0E; }
			case 1 -> { tlo = 0xFFD8D8D8; boczne = 0xFFBEBEBE; wiersz = 0xFFEDEDED; tekst = 0xFF111111; przycisk = 0xFFCFCFCF; }
			default -> { tlo = 0xFF3C3C3C; boczne = 0xFF2B2B2B; wiersz = 0xFF555555; tekst = 0xFFFFFFFF; przycisk = 0xFF272727; }
		}
	}

	private int akcent() { return 0xFF000000 | cfg.kolor; }

	private static int mieszaj(int a, int b, float t) {
		int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
		return 0xFF000000 | (r << 16) | (g << 8) | bl;
	}

	// ---------- rysowanie pomocnicze ----------

	private void prostokat(DrawContext c, int x, int y, int w, int h, int kolor) {
		if (cfg.zaokraglone && w > 4 && h > 4) { // „zaokrąglone” rogi – ucięte narożniki
			c.fill(x + 1, y, x + w - 1, y + h, kolor);
			c.fill(x, y + 1, x + w, y + h - 1, kolor);
		} else {
			c.fill(x, y, x + w, y + h, kolor);
		}
	}

	private void napis(DrawContext c, String s, int x, int y, int kolor) {
		c.drawText(this.textRenderer, s, x, y, kolor, cfg.cienTekstu);
	}

	private void klik() {
		if (cfg.dzwieki && this.client != null) {
			this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		}
	}

	private void przycisk(DrawContext c, String etykieta, int x, int y, int w, int h, int mx, int my,
						  boolean aktywny, int kolorHover, Runnable akcja) {
		boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
		int kolor = aktywny ? akcent() : (hover ? mieszaj(przycisk, kolorHover, 0.55f) : przycisk);
		prostokat(c, x, y, w, h, kolor);
		int tk = aktywny ? 0xFFFFFFFF : tekst;
		napis(c, etykieta, x + (w - textRenderer.getWidth(etykieta)) / 2, y + (h - 8) / 2, tk);
		obszary.add(new Obszar(x, y, w, h, () -> { klik(); akcja.run(); }));
	}

	private void przelacznik(DrawContext c, int x, int y, boolean wlaczony, Runnable akcja) {
		int w = 40, h = 20;
		prostokat(c, x, y, w, h, wlaczony ? 0xFF22C55E : przycisk);
		int ks = wlaczony ? x + w - 18 : x + 2;
		prostokat(c, ks, y + 2, 16, 16, 0xFFFFFFFF);
		obszary.add(new Obszar(x, y, w, h, () -> { klik(); akcja.run(); cfg.zapisz(); }));
	}

	private void komenda(String tekst) {
		if (this.client != null && this.client.player != null) {
			this.client.player.networkHandler.sendChatCommand(tekst);
		}
	}

	// ---------- główne rysowanie ----------

	@Override
	public void render(DrawContext c, int mx, int my, float delta) {
		paleta();
		obszary.clear();
		this.renderInGameBackground(c);

		int W = Math.min(440, this.width - 10), H = Math.min(290, this.height - 10);
		int px = (this.width - W) / 2, py = (this.height - H) / 2;
		prostokat(c, px, py, W, H, tlo);

		// --- panel boczny ---
		int sx = px + 10, sy = py + 10, sw = 130, sh = H - 20;
		prostokat(c, sx, sy, sw, sh, boczne);
		prostokat(c, sx + 10, sy + 12, 20, 20, akcent()); // „logo”
		napis(c, "≡", sx + 17, sy + 18, 0xFFFFFFFF);
		napis(c, "Bot Menu", sx + 38, sy + 18, akcent());
		for (int i = 0; i < ZAKLADKI.length; i++) {
			int ty = sy + 48 + i * 30;
			boolean wybrana = zakladka == i;
			int ii = i;
			boolean hover = mx >= sx + 8 && mx < sx + sw - 8 && my >= ty && my < ty + 26;
			if (wybrana) prostokat(c, sx + 8, ty, sw - 16, 26, mieszaj(boczne, akcent(), 0.55f));
			else if (hover) prostokat(c, sx + 8, ty, sw - 16, 26, mieszaj(boczne, 0xFFFFFFFF, 0.08f));
			napis(c, ZAKLADKI[i], sx + 20, ty + 9, wybrana ? 0xFFFFFFFF : mieszaj(tekst, boczne, 0.25f));
			obszary.add(new Obszar(sx + 8, ty, sw - 16, 26, () -> { klik(); zakladka = ii; przewiniecie = 0; }));
		}

		// --- zawartość ---
		int cx = px + 152, cw = W - 162, cy = py + 16;
		napis(c, ZAKLADKI[zakladka], cx, cy, akcent());
		c.fill(cx, cy + 14, cx + cw, cy + 15, boczne);
		int y0 = cy + 24;

		switch (zakladka) {
			case 0 -> zakladkaBoty(c, mx, my, cx, y0, cw, py + H - 12);
			case 1 -> zakladkaUstawienia(c, mx, my, cx, y0, cw);
			default -> zakladkaPomoc(c, cx, y0, cw);
		}
	}

	private void zakladkaBoty(DrawContext c, int mx, int my, int cx, int y0, int cw, int dol) {
		if (nazwy.isEmpty()) {
			napis(c, "Nie masz żadnych botów.", cx, y0 + 4, mieszaj(tekst, tlo, 0.4f));
			napis(c, "Użyj komendy /bot spawn <nazwa>.", cx, y0 + 18, mieszaj(tekst, tlo, 0.4f));
			return;
		}
		int h = 28, odstep = 4;
		int widoczne = Math.max(1, (dol - y0) / (h + odstep));
		przewiniecie = Math.max(0, Math.min(przewiniecie, Math.max(0, nazwy.size() - widoczne)));
		for (int i = 0; i < Math.min(widoczne, nazwy.size() - przewiniecie); i++) {
			String nazwa = nazwy.get(i + przewiniecie);
			int y = y0 + i * (h + odstep);
			prostokat(c, cx, y, cw, h, wiersz);
			napis(c, nazwa, cx + 8, y + 10, tekst);
			int bx = cx + cw - 4;
			// przyciski od prawej: Usuń, Atakuj, Stój, Podążaj
			bx -= 40; przycisk(c, "Usuń", bx, y + 4, 38, 20, mx, my, false, 0xFFEF4444, () -> {
				komenda("bot remove " + nazwa);
				nazwy.remove(nazwa);
			});
			bx -= 50; przycisk(c, "Atakuj", bx, y + 4, 48, 20, mx, my, false, akcent(), () -> komenda("bot attack " + nazwa));
			bx -= 40; przycisk(c, "Stój", bx, y + 4, 38, 20, mx, my, false, akcent(), () -> komenda("bot stay " + nazwa));
			bx -= 56; przycisk(c, "Podążaj", bx, y + 4, 54, 20, mx, my, false, akcent(), () -> komenda("bot follow " + nazwa));
		}
		if (nazwy.size() > widoczne) {
			napis(c, "Przewiń kółkiem myszy (" + (przewiniecie + 1) + "-" + (przewiniecie + widoczne) + "/" + nazwy.size() + ")",
					cx, dol - 4, mieszaj(tekst, tlo, 0.4f));
		}
	}

	private void zakladkaUstawienia(DrawContext c, int mx, int my, int cx, int y0, int cw) {
		int h = 26, odstep = 4;
		int y = y0;

		// Kolor GUI
		prostokat(c, cx, y, cw, h, wiersz);
		napis(c, "Kolor GUI", cx + 8, y + 9, tekst);
		String hex = String.format("#%06X", cfg.kolor);
		napis(c, hex, cx + cw - 100 - textRenderer.getWidth(hex), y + 9, mieszaj(tekst, wiersz, 0.25f));
		prostokat(c, cx + cw - 92, y + 4, 84, 18, akcent());
		obszary.add(new Obszar(cx + cw - 92, y + 4, 84, 18, () -> {
			klik();
			int idx = 0;
			for (int i = 0; i < ClientSettings.KOLORY.length; i++) if (ClientSettings.KOLORY[i] == cfg.kolor) idx = i;
			cfg.kolor = ClientSettings.KOLORY[(idx + 1) % ClientSettings.KOLORY.length];
			cfg.zapisz();
		}));
		y += h + odstep;

		// Przełączniki
		prostokat(c, cx, y, cw, h, wiersz);
		napis(c, "Zaokrąglone ramki", cx + 8, y + 9, tekst);
		przelacznik(c, cx + cw - 48, y + 3, cfg.zaokraglone, () -> cfg.zaokraglone = !cfg.zaokraglone);
		y += h + odstep;

		prostokat(c, cx, y, cw, h, wiersz);
		napis(c, "Dźwięki menu", cx + 8, y + 9, tekst);
		przelacznik(c, cx + cw - 48, y + 3, cfg.dzwieki, () -> cfg.dzwieki = !cfg.dzwieki);
		y += h + odstep;

		prostokat(c, cx, y, cw, h, wiersz);
		napis(c, "Cień tekstu w menu", cx + 8, y + 9, tekst);
		przelacznik(c, cx + cw - 48, y + 3, cfg.cienTekstu, () -> cfg.cienTekstu = !cfg.cienTekstu);
		y += h + odstep;

		// Motyw
		prostokat(c, cx, y, cw, h, wiersz);
		napis(c, "Motyw", cx + 8, y + 9, tekst);
		int bx = cx + cw - 4;
		for (int i = MOTYWY.length - 1; i >= 0; i--) {
			int szer = textRenderer.getWidth(MOTYWY[i]) + 14;
			bx -= szer + 3;
			int ii = i;
			przycisk(c, MOTYWY[i], bx, y + 3, szer, 20, mx, my, cfg.motyw == i, akcent(), () -> { cfg.motyw = ii; cfg.zapisz(); });
		}
	}

	private void zakladkaPomoc(DrawContext c, int cx, int y0, int cw) {
		String[] linie = {
				"/bot spawn <nazwa>  – tworzy bota",
				"/bot remove <nazwa>, /bot removeall",
				"/bot list  – lista botów",
				"/bot follow | stay | attack | mine | guard <nazwa>",
				"/bot goto <nazwa> <x> <y> <z>",
				"",
				"Prawy klik na bocie – jego ekwipunek.",
				"Klawisz B – ten panel."
		};
		for (int i = 0; i < linie.length; i++) {
			napis(c, linie[i], cx, y0 + i * 14, tekst);
		}
	}

	// ---------- wejście ----------

	@Override
	public boolean mouseClicked(double mx, double my, int przycisk) {
		if (przycisk == 0) {
			for (Obszar o : new ArrayList<>(obszary)) {
				if (o.trafia(mx, my)) { o.akcja().run(); return true; }
			}
		}
		return super.mouseClicked(mx, my, przycisk);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double poziomo, double pionowo) {
		if (zakladka == 0) przewiniecie = Math.max(0, przewiniecie - (int) Math.signum(pionowo));
		return true;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}

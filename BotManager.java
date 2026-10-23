package pl.botmod;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import pl.botmod.entity.BotEntity;

import java.util.ArrayList;
import java.util.List;

/** Pomocnicze metody do wyszukiwania botów gracza (tylko boty w załadowanych chunkach). */
public final class BotManager {
	private BotManager() {}

	public static List<BotEntity> lista(ServerPlayerEntity gracz) {
		List<BotEntity> wynik = new ArrayList<>();
		for (ServerWorld swiat : gracz.getServer().getWorlds()) {
			wynik.addAll(swiat.getEntitiesByType(BotMod.BOT,
					bot -> bot.isAlive() && gracz.getUuid().equals(bot.getWlascicielUuid())));
		}
		return wynik;
	}

	public static BotEntity znajdz(ServerPlayerEntity gracz, String nazwa) {
		for (BotEntity bot : lista(gracz)) {
			if (bot.getBotName().equalsIgnoreCase(nazwa)) return bot;
		}
		return null;
	}
}

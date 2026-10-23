package pl.botmod.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Uuids;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skin bota = skin gracza o tej nazwie (pobierany asynchronicznie), a do czasu pobrania
 * (lub gdy gracz nie istnieje) – skin domyślny.
 * UWAGA: to jedno z miejsc najbardziej zależnych od wersji API; w razie błędu kompilacji
 * można całą metodę zastąpić samym: return DefaultSkinHelper.getSkinTextures(Uuids.getOfflinePlayerUuid(nazwa));
 */
public final class BotSkins {
	private static final Map<String, GameProfile> PROFILE = new ConcurrentHashMap<>();
	private static final Set<String> ZAPYTANE = ConcurrentHashMap.newKeySet();

	private BotSkins() {}

	public static SkinTextures pobierz(String nazwa) {
		String klucz = nazwa.toLowerCase();
		GameProfile profil = PROFILE.get(klucz);
		if (profil != null) {
			return MinecraftClient.getInstance().getSkinProvider().getSkinTextures(profil);
		}
		if (ZAPYTANE.add(klucz)) {
			SkullBlockEntity.fetchProfileByName(nazwa).thenAccept(opt -> opt.ifPresent(p -> PROFILE.put(klucz, p)));
		}
		return DefaultSkinHelper.getSkinTextures(Uuids.getOfflinePlayerUuid(nazwa));
	}
}

package pl.botmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.botmod.entity.BotEntity;
import pl.botmod.net.BotListPayload;
import pl.botmod.net.RequestListPayload;

import java.util.List;

/** Główna klasa moda (część wspólna: serwer + singleplayer). */
public class BotMod implements ModInitializer {
	public static final String ID = "botmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);
	public static BotConfig CONFIG = new BotConfig();

	public static final RegistryKey<EntityType<?>> BOT_KLUCZ =
			RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(ID, "bot"));

	/** Typ encji bota. */
	public static final EntityType<BotEntity> BOT = Registry.register(
			Registries.ENTITY_TYPE, BOT_KLUCZ,
			EntityType.Builder.create(BotEntity::new, SpawnGroup.MISC)
					.dimensions(0.6f, 1.8f)
					.maxTrackingRange(10)
					.build(BOT_KLUCZ));

	@Override
	public void onInitialize() {
		CONFIG = BotConfig.wczytaj();

		// Atrybuty bota (zdrowie, prędkość itd.)
		FabricDefaultAttributeRegistry.register(BOT, BotEntity.utworzAtrybuty());

		// Pakiety sieciowe: klient prosi o listę botów, serwer odpowiada
		PayloadTypeRegistry.playC2S().register(RequestListPayload.ID, RequestListPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BotListPayload.ID, BotListPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(RequestListPayload.ID, (payload, context) -> {
			ServerPlayerEntity gracz = context.player();
			List<String> nazwy = BotManager.lista(gracz).stream().map(BotEntity::getBotName).toList();
			ServerPlayNetworking.send(gracz, new BotListPayload(nazwy));
		});

		// Komendy /bot ...
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				BotCommands.zarejestruj(dispatcher));

		LOGGER.info("BotMod załadowany.");
	}
}

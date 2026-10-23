package pl.botmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import pl.botmod.entity.BotEntity;

import java.util.List;

/** Komendy /bot ... (wszystkie komunikaty po polsku). */
public final class BotCommands {
	private BotCommands() {}

	/** Podpowiedzi nazw botów gracza przy wpisywaniu komendy. */
	private static final SuggestionProvider<ServerCommandSource> PODPOWIEDZI = (ctx, builder) -> {
		ServerPlayerEntity gracz = ctx.getSource().getPlayer();
		if (gracz != null) {
			return CommandSource.suggestMatching(
					BotManager.lista(gracz).stream().map(BotEntity::getBotName).toList(), builder);
		}
		return builder.buildFuture();
	};

	@FunctionalInterface
	private interface Akcja {
		void wykonaj(ServerCommandSource zrodlo, ServerPlayerEntity gracz, BotEntity bot);
	}

	public static void zarejestruj(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("bot")
				.then(CommandManager.literal("spawn")
						.then(CommandManager.argument("nazwa", StringArgumentType.word()).executes(ctx -> {
							ServerPlayerEntity gracz = ctx.getSource().getPlayerOrThrow();
							return stworz(ctx.getSource(), gracz, StringArgumentType.getString(ctx, "nazwa"));
						})))
				.then(komenda("remove", (z, g, b) -> {
					b.oddajEkwipunek();
					b.discard();
					z.sendFeedback(() -> Text.literal("Usunięto bota " + b.getBotName() + "."), false);
				}))
				.then(CommandManager.literal("removeall").executes(ctx -> {
					ServerPlayerEntity gracz = ctx.getSource().getPlayerOrThrow();
					List<BotEntity> boty = BotManager.lista(gracz);
					for (BotEntity b : boty) { b.oddajEkwipunek(); b.discard(); }
					ctx.getSource().sendFeedback(() -> Text.literal("Usunięto wszystkie boty (" + boty.size() + ")."), false);
					return boty.size();
				}))
				.then(CommandManager.literal("list").executes(ctx -> {
					ServerPlayerEntity gracz = ctx.getSource().getPlayerOrThrow();
					List<BotEntity> boty = BotManager.lista(gracz);
					if (boty.isEmpty()) {
						ctx.getSource().sendFeedback(() -> Text.literal("Nie masz żadnych botów."), false);
						return 0;
					}
					ctx.getSource().sendFeedback(() -> Text.literal("Twoje boty (" + boty.size() + "/"
							+ BotMod.CONFIG.maxBotowNaGracza + "):"), false);
					for (BotEntity b : boty) {
						BlockPos p = b.getBlockPos();
						ctx.getSource().sendFeedback(() -> Text.literal(" - " + b.getBotName() + " | "
								+ b.getTryb().opis + " | zdrowie: " + Math.round(b.getHealth()) + "/"
								+ Math.round(b.getMaxHealth()) + " | pozycja: " + p.getX() + " " + p.getY() + " " + p.getZ()), false);
					}
					return boty.size();
				}))
				.then(komenda("follow", (z, g, b) -> {
					b.ustawPodazaj();
					z.sendFeedback(() -> Text.literal("Bot " + b.getBotName() + " podąża za Tobą."), false);
				}))
				.then(komenda("stay", (z, g, b) -> {
					b.ustawStoj();
					z.sendFeedback(() -> Text.literal("Bot " + b.getBotName() + " stoi w miejscu."), false);
				}))
				.then(CommandManager.literal("goto")
						.then(CommandManager.argument("nazwa", StringArgumentType.word()).suggests(PODPOWIEDZI)
								.then(CommandManager.argument("pozycja", BlockPosArgumentType.blockPos()).executes(ctx -> {
									ServerPlayerEntity gracz = ctx.getSource().getPlayerOrThrow();
									BotEntity bot = BotManager.znajdz(gracz, StringArgumentType.getString(ctx, "nazwa"));
									if (bot == null) return brakBota(ctx.getSource(), StringArgumentType.getString(ctx, "nazwa"));
									BlockPos cel = BlockPosArgumentType.getBlockPos(ctx, "pozycja");
									bot.ustawIdz(cel);
									ctx.getSource().sendFeedback(() -> Text.literal("Bot " + bot.getBotName() + " idzie do "
											+ cel.getX() + " " + cel.getY() + " " + cel.getZ() + "."), false);
									return 1;
								}))))
				.then(komenda("attack", (z, g, b) -> {
					b.ustawAtakuj();
					z.sendFeedback(() -> Text.literal("Bot " + b.getBotName() + " atakuje najbliższych wrogów."), false);
				}))
				.then(komenda("mine", (z, g, b) -> {
					// Kopie w kierunku, w którym patrzy gracz w chwili wpisania komendy
					b.ustawKopanie(Direction.fromRotation(g.getYaw()));
					z.sendFeedback(() -> Text.literal("Bot " + b.getBotName() + " kopie bloki przed sobą."), false);
				}))
				.then(komenda("guard", (z, g, b) -> {
					b.ustawStraz();
					z.sendFeedback(() -> Text.literal("Bot " + b.getBotName() + " pilnuje tego miejsca."), false);
				})));
	}

	/** Buduje komendę w stylu /bot <nazwa_komendy> <nazwa_bota>. */
	private static LiteralArgumentBuilder<ServerCommandSource> komenda(String nazwaKomendy, Akcja akcja) {
		return CommandManager.literal(nazwaKomendy)
				.then(CommandManager.argument("nazwa", StringArgumentType.word()).suggests(PODPOWIEDZI).executes(ctx -> {
					ServerPlayerEntity gracz = ctx.getSource().getPlayerOrThrow();
					String nazwa = StringArgumentType.getString(ctx, "nazwa");
					BotEntity bot = BotManager.znajdz(gracz, nazwa);
					if (bot == null) return brakBota(ctx.getSource(), nazwa);
					akcja.wykonaj(ctx.getSource(), gracz, bot);
					return 1;
				}));
	}

	private static int brakBota(ServerCommandSource zrodlo, String nazwa) {
		zrodlo.sendError(Text.literal("Nie masz bota o nazwie \"" + nazwa + "\"."));
		return 0;
	}

	private static int stworz(ServerCommandSource zrodlo, ServerPlayerEntity gracz, String nazwa) {
		if (!nazwa.matches("[A-Za-z0-9_]{1,16}")) {
			zrodlo.sendError(Text.literal("Nazwa bota: 1-16 znaków (litery, cyfry, _)."));
			return 0;
		}
		List<BotEntity> boty = BotManager.lista(gracz);
		if (boty.size() >= BotMod.CONFIG.maxBotowNaGracza) {
			zrodlo.sendError(Text.literal("Osiągnięto limit botów (" + BotMod.CONFIG.maxBotowNaGracza + ")."));
			return 0;
		}
		if (BotManager.znajdz(gracz, nazwa) != null) {
			zrodlo.sendError(Text.literal("Masz już bota o nazwie " + nazwa + "."));
			return 0;
		}
		ServerWorld swiat = (ServerWorld) gracz.getWorld();
		BotEntity bot = new BotEntity(BotMod.BOT, swiat);
		bot.refreshPositionAndAngles(gracz.getX(), gracz.getY(), gracz.getZ(), gracz.getYaw(), 0);
		bot.ustawBota(nazwa, gracz.getUuid());
		swiat.spawnEntity(bot);
		zrodlo.sendFeedback(() -> Text.literal("Stworzono bota " + nazwa + "."), false);
		return 1;
	}
}

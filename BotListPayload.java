package pl.botmod.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/** Serwer -> klient: nazwy botów należących do gracza. */
public record BotListPayload(List<String> nazwy) implements CustomPayload {
	public static final Id<BotListPayload> ID = new Id<>(Identifier.of("botmod", "bot_list"));
	public static final PacketCodec<ByteBuf, BotListPayload> CODEC =
			PacketCodecs.STRING.collect(PacketCodecs.toList()).xmap(BotListPayload::new, BotListPayload::nazwy);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

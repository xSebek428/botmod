package pl.botmod.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Klient -> serwer: „wyślij mi listę moich botów” (otwarcie panelu klawiszem B). */
public record RequestListPayload() implements CustomPayload {
	public static final Id<RequestListPayload> ID = new Id<>(Identifier.of("botmod", "request_list"));
	public static final PacketCodec<ByteBuf, RequestListPayload> CODEC = PacketCodec.unit(new RequestListPayload());

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

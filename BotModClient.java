package pl.botmod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import pl.botmod.BotMod;
import pl.botmod.net.BotListPayload;
import pl.botmod.net.RequestListPayload;

import java.util.ArrayList;

/** Część kliencka: renderer bota, klawisz B i odbiór listy botów. */
public class BotModClient implements ClientModInitializer {
	private static KeyBinding klawiszPanelu;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(BotMod.BOT, BotEntityRenderer::new);

		klawiszPanelu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.botmod.open_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.botmod"));

		ClientTickEvents.END_CLIENT_TICK.register(klient -> {
			while (klawiszPanelu.wasPressed()) {
				if (klient.player != null && klient.currentScreen == null) {
					ClientPlayNetworking.send(new RequestListPayload());
				}
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(BotListPayload.ID, (payload, context) ->
				context.client().setScreen(new BotScreen(new ArrayList<>(payload.nazwy()))));
	}
}

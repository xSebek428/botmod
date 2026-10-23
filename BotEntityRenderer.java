package pl.botmod.client;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Identifier;
import pl.botmod.entity.BotEntity;

/**
 * Renderer bota – model gracza (szerokie ręce) ze skinem gracza o nazwie bota.
 * Pancerz bota nie jest rysowany (działa tylko jako ochrona), trzymany przedmiot jest widoczny.
 */
public class BotEntityRenderer extends BipedEntityRenderer<BotEntity, PlayerEntityRenderState, PlayerEntityModel> {

	public BotEntityRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new PlayerEntityModel(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
	}

	@Override
	public PlayerEntityRenderState createRenderState() {
		return new PlayerEntityRenderState();
	}

	@Override
	public void updateRenderState(BotEntity bot, PlayerEntityRenderState stan, float delta) {
		super.updateRenderState(bot, stan, delta);
		stan.skinTextures = BotSkins.pobierz(bot.getBotName());
	}

	@Override
	public Identifier getTexture(PlayerEntityRenderState stan) {
		return stan.skinTextures.texture();
	}
}

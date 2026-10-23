package pl.botmod.entity;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/**
 * Bot – encja wyglądająca jak gracz, sterowana przez mod.
 * Cała logika zachowań działa po stronie serwera (metoda serwerTick).
 */
public class BotEntity extends PathAwareEntity {

	/** Tryby pracy bota. */
	public enum Tryb {
		STOJ("stoi w miejscu"), PODAZAJ("podąża za właścicielem"), IDZ("idzie do celu"),
		ATAKUJ("atakuje wrogów"), KOPIE("kopie bloki"), STRAZ("pilnuje miejsca");

		public final String opis;
		Tryb(String opis) { this.opis = opis; }
	}

	/** Rozmiar ekwipunku: sloty 0-5 to wyposażenie, 6-17 to schowek (np. jedzenie). */
	public static final int ROZMIAR = 18;
	/** Sloty 0-5 odpowiadają kolejno tym slotom wyposażenia. */
	private static final EquipmentSlot[] SLOTY = {
			EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private final SimpleInventory ekwipunek = new SimpleInventory(ROZMIAR);
	private UUID wlasciciel;
	private Tryb tryb = Tryb.STOJ;
	private BlockPos cel;
	private BlockPos posterunek;
	private Direction kierunekKopania = Direction.NORTH;

	private int cooldownAtaku;
	private int cooldownJedzenia;
	private int zablokowany;
	private int postepKopania;
	private BlockPos kopanyBlok;

	public BotEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.setPersistent(); // bot nie znika sam z siebie
	}

	public static DefaultAttributeContainer.Builder utworzAtrybuty() {
		return PathAwareEntity.createMobAttributes()
				.add(EntityAttributes.MAX_HEALTH, 20.0)
				.add(EntityAttributes.MOVEMENT_SPEED, 0.25)
				.add(EntityAttributes.ATTACK_DAMAGE, 1.0)
				.add(EntityAttributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(0, new SwimGoal(this));
		this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		this.goalSelector.add(6, new LookAroundGoal(this));
	}

	// ---------- Gettery / settery ----------

	public String getBotName() {
		return getCustomName() != null ? getCustomName().getString() : "Bot";
	}

	public void ustawBota(String nazwa, UUID wlasciciel) {
		this.setCustomName(Text.literal(nazwa));
		this.setCustomNameVisible(true);
		this.wlasciciel = wlasciciel;
	}

	public UUID getWlascicielUuid() { return wlasciciel; }
	public Tryb getTryb() { return tryb; }

	public void ustawStoj() {
		tryb = Tryb.STOJ;
		getNavigation().stop();
	}

	public void ustawPodazaj() {
		tryb = Tryb.PODAZAJ;
	}

	public void ustawIdz(BlockPos cel) {
		this.cel = cel;
		this.zablokowany = 0;
		this.tryb = Tryb.IDZ;
	}

	public void ustawAtakuj() {
		tryb = Tryb.ATAKUJ;
	}

	public void ustawKopanie(Direction kierunek) {
		this.kierunekKopania = kierunek;
		this.postepKopania = 0;
		this.kopanyBlok = null;
		this.tryb = Tryb.KOPIE;
	}

	public void ustawStraz() {
		this.posterunek = getBlockPos();
		this.tryb = Tryb.STRAZ;
	}

	/** Wyrzuca zawartość ekwipunku pod nogi bota (używane przy usuwaniu komendą). */
	public void oddajEkwipunek() {
		if (!getWorld().isClient) {
			ItemScatterer.spawn(getWorld(), getBlockPos(), ekwipunek);
			ekwipunek.clear();
			for (EquipmentSlot slot : SLOTY) equipStack(slot, ItemStack.EMPTY);
		}
	}

	// ---------- Interakcja: prawy klik otwiera ekwipunek ----------

	@Override
	public ActionResult interactMob(PlayerEntity gracz, Hand reka) {
		if (!getWorld().isClient) {
			if (gracz.getUuid().equals(wlasciciel)) {
				gracz.openHandledScreen(new SimpleNamedScreenHandlerFactory(
						(syncId, inv, p) -> new GenericContainerScreenHandler(
								ScreenHandlerType.GENERIC_9X2, syncId, inv, ekwipunek, 2),
						Text.literal("Ekwipunek bota " + getBotName()
								+ " (broń, tarcza, hełm, napierśnik, spodnie, buty, potem jedzenie)")));
			} else {
				gracz.sendMessage(Text.literal("To nie jest Twój bot."), true);
			}
		}
		return ActionResult.SUCCESS;
	}

	// ---------- Śmierć ----------

	@Override
	public void onDeath(DamageSource zrodlo) {
		if (!getWorld().isClient) {
			oddajEkwipunek(); // bot dropi swój ekwipunek
			PlayerEntity w = wlascicielGracz();
			if (w != null) w.sendMessage(Text.literal("Twój bot " + getBotName() + " zginął."), false);
		}
		super.onDeath(zrodlo);
	}

	// ---------- Logika serwerowa ----------

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient && getWorld() instanceof ServerWorld swiat) {
			serwerTick(swiat);
		}
	}

	private void serwerTick(ServerWorld swiat) {
		if (cooldownAtaku > 0) cooldownAtaku--;
		synchronizujWyposazenie();
		jedz();

		switch (tryb) {
			case STOJ -> { /* nic nie robi */ }
			case PODAZAJ -> podazaj(swiat);
			case IDZ -> idz();
			case ATAKUJ -> {
				HostileEntity wrog = znajdzWroga(16);
				if (wrog != null) atakuj(swiat, wrog); else getNavigation().stop();
			}
			case STRAZ -> straz(swiat);
			case KOPIE -> kop(swiat);
		}
	}

	/** Kopiuje przedmioty z ekwipunku do slotów wyposażenia (to, co bot realnie nosi). */
	private void synchronizujWyposazenie() {
		for (int i = 0; i < SLOTY.length; i++) {
			ItemStack stos = ekwipunek.getStack(i);
			if (!ItemStack.areEqual(getEquippedStack(SLOTY[i]), stos)) {
				equipStack(SLOTY[i], stos);
			}
		}
	}

	/** Automatyczne jedzenie, gdy bot jest ranny i ma jedzenie w schowku. */
	private void jedz() {
		if (cooldownJedzenia > 0) { cooldownJedzenia--; return; }
		if (getHealth() >= getMaxHealth() - 4) return;
		for (int i = SLOTY.length; i < ROZMIAR; i++) {
			ItemStack stos = ekwipunek.getStack(i);
			FoodComponent jedzenie = stos.get(DataComponentTypes.FOOD);
			if (jedzenie != null) {
				heal(Math.max(2, jedzenie.nutrition()));
				stos.decrement(1);
				playSound(SoundEvents.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
				cooldownJedzenia = 40;
				return;
			}
		}
	}

	private PlayerEntity wlascicielGracz() {
		return wlasciciel == null ? null : getWorld().getPlayerByUuid(wlasciciel);
	}

	private void podazaj(ServerWorld swiat) {
		PlayerEntity w = wlascicielGracz();
		if (w == null || w.getWorld() != getWorld()) { getNavigation().stop(); return; }
		double d = squaredDistanceTo(w);
		if (d > 1024) { // ponad 32 bloki – teleport do właściciela
			refreshPositionAndAngles(w.getX(), w.getY(), w.getZ(), getYaw(), 0);
			getNavigation().stop();
		} else if (d > 9) {
			getNavigation().startMovingTo(w, 1.15);
		} else {
			getNavigation().stop();
		}
	}

	private void idz() {
		if (cel == null) { tryb = Tryb.STOJ; return; }
		Vec3d punkt = Vec3d.ofBottomCenter(cel);
		if (squaredDistanceTo(punkt) < 2.25) {
			getNavigation().stop();
			tryb = Tryb.STOJ;
			PlayerEntity w = wlascicielGracz();
			if (w != null) w.sendMessage(Text.literal("Bot " + getBotName() + " dotarł do celu."), false);
			return;
		}
		if (getNavigation().isIdle()) {
			getNavigation().startMovingTo(punkt.x, punkt.y, punkt.z, 1.0);
			if (++zablokowany > 60) { // brak ścieżki – poddaje się
				tryb = Tryb.STOJ;
				PlayerEntity w = wlascicielGracz();
				if (w != null) w.sendMessage(Text.literal("Bot " + getBotName() + " nie może znaleźć drogi do celu."), false);
			}
		} else {
			zablokowany = 0;
		}
	}

	private void straz(ServerWorld swiat) {
		if (posterunek == null) posterunek = getBlockPos();
		HostileEntity wrog = znajdzWroga(10);
		if (wrog != null && wrog.getBlockPos().getSquaredDistance(posterunek) < 14 * 14) {
			atakuj(swiat, wrog);
		} else if (getBlockPos().getSquaredDistance(posterunek) > 9) {
			getNavigation().startMovingTo(posterunek.getX() + 0.5, posterunek.getY(), posterunek.getZ() + 0.5, 1.0);
		} else {
			getNavigation().stop();
		}
	}

	private void atakuj(ServerWorld swiat, HostileEntity wrog) {
		getLookControl().lookAt(wrog, 30f, 30f);
		if (distanceTo(wrog) > 2.5f) {
			getNavigation().startMovingTo(wrog, 1.2);
		} else {
			getNavigation().stop();
			if (cooldownAtaku <= 0) {
				swingHand(Hand.MAIN_HAND);
				tryAttack(swiat, wrog); // obrażenia zależą od trzymanej broni
				cooldownAtaku = 20;
			}
		}
	}

	private HostileEntity znajdzWroga(double zasieg) {
		List<HostileEntity> lista = getWorld().getEntitiesByClass(HostileEntity.class,
				getBoundingBox().expand(zasieg), e -> e.isAlive());
		HostileEntity najblizszy = null;
		double najd = Double.MAX_VALUE;
		for (HostileEntity e : lista) {
			double d = squaredDistanceTo(e);
			if (d < najd) { najd = d; najblizszy = e; }
		}
		return najblizszy;
	}

	/** Kopanie: niszczy blok przed botem (na wysokości stóp, potem głowy); jeśli pusto – idzie naprzód. */
	private void kop(ServerWorld swiat) {
		BlockPos stopy = getBlockPos().offset(kierunekKopania);
		BlockPos glowa = stopy.up();
		BlockPos wybrany = null;
		for (BlockPos p : new BlockPos[]{stopy, glowa}) {
			BlockState stan = swiat.getBlockState(p);
			if (!stan.isAir() && stan.getFluidState().isEmpty() && stan.getHardness(swiat, p) >= 0) {
				wybrany = p;
				break;
			}
		}
		if (wybrany == null) { // nic do kopania – krok naprzód
			kopanyBlok = null;
			getMoveControl().moveTo(stopy.getX() + 0.5, stopy.getY(), stopy.getZ() + 0.5, 0.8);
			return;
		}
		if (!wybrany.equals(kopanyBlok)) { kopanyBlok = wybrany; postepKopania = 0; }
		getNavigation().stop();
		getLookControl().lookAt(wybrany.getX() + 0.5, wybrany.getY() + 0.5, wybrany.getZ() + 0.5);
		float twardosc = swiat.getBlockState(wybrany).getHardness(swiat, wybrany);
		int potrzeba = Math.max(5, (int) (twardosc * 30));
		postepKopania++;
		if (postepKopania % 5 == 0) swingHand(Hand.MAIN_HAND);
		if (postepKopania >= potrzeba) {
			swiat.breakBlock(wybrany, true, this);
			postepKopania = 0;
			kopanyBlok = null;
		}
	}

	// ---------- Zapis / odczyt ----------

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (wlasciciel != null) nbt.putUuid("Wlasciciel", wlasciciel);
		nbt.putInt("Tryb", tryb.ordinal());
		nbt.putString("Kierunek", kierunekKopania.getName());
		if (cel != null) nbt.putLong("Cel", cel.asLong());
		if (posterunek != null) nbt.putLong("Posterunek", posterunek.asLong());
		Inventories.writeNbt(nbt, ekwipunek.getHeldStacks(), getRegistryManager());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.containsUuid("Wlasciciel")) wlasciciel = nbt.getUuid("Wlasciciel");
		int t = nbt.getInt("Tryb");
		if (t >= 0 && t < Tryb.values().length) tryb = Tryb.values()[t];
		Direction k = Direction.byName(nbt.getString("Kierunek"));
		if (k != null) kierunekKopania = k;
		if (nbt.contains("Cel")) cel = BlockPos.fromLong(nbt.getLong("Cel"));
		if (nbt.contains("Posterunek")) posterunek = BlockPos.fromLong(nbt.getLong("Posterunek"));
		Inventories.readNbt(nbt, ekwipunek.getHeldStacks(), getRegistryManager());
	}

	@Override
	public boolean canImmediatelyDespawn(double odleglosc) {
		return false;
	}
}

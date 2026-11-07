package svenskacraft.discovery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import svenskacraft.SvenskaCraft;
import svenskacraft.audio.AudioPlayer;
import svenskacraft.settings.Settings;
import svenskacraft.vocab.Vocabulary;

/**
 * Finds words in the world: items you pick up, how many you pick up (numbers), animals you look at,
 * things you do (verbs) and moments (phrases). New words get a title, one voice clip and a chime.
 * Known words only get a quiet hotbar reminder, and only for items and animals.
 */
public final class Discovery {
	private static final long TITLE_COOLDOWN_MS = 3_000;
	private static final double LOOK_RANGE = 10.0;
	private static final double LOOK_COS = Math.cos(Math.toRadians(12));

	private final Map<String, Long> lastReminder = new HashMap<>();
	private final Map<String, Integer> holdTicks = new HashMap<>();
	private long lastTitle;
	private long lastAnyReminder;

	// event tracking
	private ClientLevel lastLevel;
	private int ticksInWorld;
	private boolean wasOnGround = true;
	private boolean wasSleeping;
	private int lastHurtTime;
	private long lastDayTime = -1;
	private double walked;
	private Vec3 lastPos;
	private ItemStack lastHeld = ItemStack.EMPTY;
	private int lastHeldCount;

	public void onPickup(ItemStack stack, int amount) {
		if (!SvenskaCraft.settings.enabled || stack.isEmpty()) return;
		Vocabulary.Entry e = SvenskaCraft.vocab.byItem(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		if (e != null) found(e, true);
		Vocabulary.Entry n = SvenskaCraft.vocab.byAmount(amount);
		if (n != null) found(n, false);
	}

	public void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null) {
			lastLevel = null;
			return;
		}
		if (mc.level != lastLevel) {
			lastLevel = mc.level;
			ticksInWorld = 0;
			lastPos = p.position();
			lastDayTime = -1;
		}
		ticksInWorld++;
		if (!SvenskaCraft.settings.enabled || mc.isPaused()) return;

		if (ticksInWorld == 60) event("join");

		// verbs
		if (lastPos != null && p.onGround() && !p.isSprinting()) {
			Vec3 d = p.position().subtract(lastPos);
			walked += Math.sqrt(d.x * d.x + d.z * d.z);
			if (walked > 20) event("walk");
		}
		lastPos = p.position();
		if (held("sprint", p.isSprinting(), 20)) event("sprint");
		if (wasOnGround && !p.onGround() && p.getDeltaMovement().y > 0.2 && !p.isInWater() && !p.getAbilities().flying) event("jump");
		wasOnGround = p.onGround();
		if (held("swim", p.isInWater() && !p.onGround(), 60)) event("swim");
		if (held("eat", p.isUsingItem() && p.getUseItem().has(DataComponents.FOOD), 10)) event("eat");
		if (held("dig", mc.gameMode != null && mc.gameMode.isDestroying(), 10)) event("dig");
		ItemStack hand = p.getMainHandItem();
		if (hand.getItem() instanceof BlockItem && ItemStack.isSameItem(hand, lastHeld) && hand.getCount() == lastHeldCount - 1
			&& mc.options.keyUse.isDown()) event("build");
		lastHeld = hand.copy();
		lastHeldCount = hand.getCount();

		// moments
		if (p.isSleeping() && !wasSleeping) { event("sleep"); event("night"); }
		wasSleeping = p.isSleeping();
		long day = Math.floorMod(mc.level.getOverworldClockTime(), 24000L);
		if (lastDayTime >= 0) {
			if (lastDayTime < 13000 && day >= 13000) event("night");
			if ((lastDayTime >= 22500 || lastDayTime > day + 12000) && day < 1000) event("morning");
		}
		lastDayTime = day;
		if (mc.gui.screen() instanceof MerchantScreen) event("trade");
		if (p.hurtTime > 0 && lastHurtTime == 0) event("hurt");
		lastHurtTime = p.hurtTime;
		if (p.getHealth() > 0 && p.getHealth() <= 6) event("low_health");

		// animals
		if (ticksInWorld % 10 == 0) lookForAnimals(mc, p);
	}

	private boolean held(String key, boolean active, int ticks) {
		int t = active ? holdTicks.getOrDefault(key, 0) + 1 : 0;
		holdTicks.put(key, t);
		return t == ticks;
	}

	private void lookForAnimals(Minecraft mc, LocalPlayer p) {
		Vec3 eye = p.getEyePosition();
		Vec3 view = p.getViewVector(1.0f);
		for (Entity ent : mc.level.entitiesForRendering()) {
			if (ent == p || ent.distanceTo(p) > LOOK_RANGE) continue;
			Vec3 to = ent.getBoundingBox().getCenter().subtract(eye).normalize();
			if (to.dot(view) < LOOK_COS || !p.hasLineOfSight(ent)) continue;
			String key = BuiltInRegistries.ENTITY_TYPE.getKey(ent.getType()).getPath();
			if (ent instanceof TamableAnimal t && t.isTame() && !SvenskaCraft.vocab.byEntity(key + ":tame").isEmpty()) key += ":tame";
			for (Vocabulary.Entry e : SvenskaCraft.vocab.byEntity(key)) found(e, true);
		}
	}

	private void event(String name) {
		for (Vocabulary.Entry e : SvenskaCraft.vocab.byEvent(name)) found(e, false);
	}

	/** @param remind show a hotbar reminder when the word is already known */
	public void found(Vocabulary.Entry e, boolean remind) {
		long now = System.currentTimeMillis();
		if (SvenskaCraft.progress.discover(e.id(), now)) announceNew(e, now);
		else if (remind) remind(e, now);
	}

	private void announceNew(Vocabulary.Entry e, long now) {
		Minecraft mc = Minecraft.getInstance();
		Settings s = SvenskaCraft.settings;
		boolean showEn = s.translations == Settings.Translations.ALWAYS;
		if (now - lastTitle >= TITLE_COOLDOWN_MS) {
			lastTitle = now;
			mc.gui.hud.setTimes(5, 50, 15);
			String sub = showEn ? e.en()
				: s.level == svenskacraft.learning.Level.ADVANCED && e.text().definition() != null ? e.text().definition() : "";
			mc.gui.hud.setSubtitle(Component.literal(sub).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
			mc.gui.hud.setTitle(Component.literal(e.word()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		} else {
			mc.gui.hud.setOverlayMessage(pair(e, showEn), false);
		}
		MutableComponent line = Component.literal("[" + SvenskaCraft.pack.ui("newWord") + "] ").withStyle(ChatFormatting.AQUA)
			.append(Component.literal(e.word()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		if (s.translations == Settings.Translations.ALWAYS) {
			line.append(Component.literal(" = " + e.en()).withStyle(ChatFormatting.GRAY));
		} else if (s.translations == Settings.Translations.ON_DEMAND) {
			line.append(Component.literal(" [?]").withStyle(st -> st.withColor(ChatFormatting.DARK_GRAY)
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(e.en())))));
		}
		SvenskaCraft.say(line);
		AudioPlayer.cue(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.6f);
		var w = SvenskaCraft.progress.wordOrCreate(e.id());
		if (s.voice == Settings.VoiceMode.DISCOVERY && !w.spoken) {
			SvenskaCraft.audio.play(SvenskaCraft.pack.wordSound(e.id()), AudioPlayer.Priority.AUTO);
		}
		w.spoken = true;
		SvenskaCraft.progressChanged();
	}

	private void remind(Vocabulary.Entry e, long now) {
		int seconds = SvenskaCraft.settings.intensity.reminderSeconds;
		if (seconds < 0 || now - lastAnyReminder < TITLE_COOLDOWN_MS) return;
		Long last = lastReminder.get(e.id());
		if (last != null && now - last < seconds * 1000L) return;
		lastReminder.put(e.id(), now);
		lastAnyReminder = now;
		Minecraft.getInstance().gui.hud.setOverlayMessage(pair(e, SvenskaCraft.settings.translations == Settings.Translations.ALWAYS), false);
	}

	private static Component pair(Vocabulary.Entry e, boolean withEnglish) {
		MutableComponent c = Component.literal(e.word()).withStyle(ChatFormatting.GOLD);
		if (withEnglish) c.append(Component.literal("  ·  " + e.en()).withStyle(ChatFormatting.GRAY));
		return c;
	}

	public static List<String> supportedEvents() {
		return List.of("join", "walk", "sprint", "jump", "swim", "eat", "dig", "build", "sleep", "night", "morning", "trade", "hurt", "low_health");
	}
}

package svenskacraft;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenskacraft.audio.AudioPlayer;
import svenskacraft.discovery.Discovery;
import svenskacraft.lang.LanguagePack;
import svenskacraft.learning.DailyGoal;
import svenskacraft.learning.Milestones;
import svenskacraft.learning.Scheduler;
import svenskacraft.progress.LegacyMigration;
import svenskacraft.progress.Progress;
import svenskacraft.progress.ProgressStore;
import svenskacraft.settings.Settings;
import svenskacraft.ui.JournalScreen;
import svenskacraft.ui.QuizScreen;
import svenskacraft.ui.SettingsScreen;
import svenskacraft.vocab.Vocabulary;

public class SvenskaCraft implements ClientModInitializer {
	public static final String MOD_ID = "svenskacraft";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

	public static Path configDir;
	public static Settings settings;
	public static LanguagePack pack;
	public static Vocabulary vocab;
	public static Progress progress;
	public static ProgressStore store;
	public static final AudioPlayer audio = new AudioPlayer();
	public static final Discovery discovery = new Discovery();

	private static KeyMapping journalKey;
	private static int quizTimer;
	private static Supplier<Screen> pendingScreen;
	private static boolean wasInWorld;
	private static long worldTicks;

	@Override
	public void onInitializeClient() {
		Path root = FabricLoader.getInstance().getConfigDir();
		configDir = root.resolve(MOD_ID);
		settings = Settings.load(configDir);
		loadLanguage(settings.language);
		if (LegacyMigration.run(root.resolve("svenskacraft.json"), settings, progress, System.currentTimeMillis())) {
			LOG.info("Migrated old SvenskaCraft config ({} words)", progress.discoveredIds().size());
			saveSettings();
			store.markDirty();
			store.flush(progress, System.currentTimeMillis());
		}

		journalKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.svenskacraft.journal", InputConstants.KEY_J,
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))));
		ClientTickEvents.END_CLIENT_TICK.register(SvenskaCraft::tick);
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> tooltip(stack, lines));
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> saveProgressNow());
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
			ClientCommands.literal("svenska")
				.executes(c -> { help(); return 1; })
				// Screens are opened on the next tick: chat closes itself right after running a command.
				.then(ClientCommands.literal("quiz").executes(c -> { pendingScreen = SvenskaCraft::quizScreen; return 1; }))
				.then(ClientCommands.literal("journal").executes(c -> { pendingScreen = JournalScreen::new; return 1; }))
				.then(ClientCommands.literal("words").executes(c -> { pendingScreen = JournalScreen::new; return 1; }))
				.then(ClientCommands.literal("settings").executes(c -> { pendingScreen = () -> new SettingsScreen(null); return 1; }))
				.then(ClientCommands.literal("on").executes(c -> { setEnabled(true); return 1; }))
				.then(ClientCommands.literal("off").executes(c -> { setEnabled(false); return 1; }))
				.then(ClientCommands.literal("penalty")
					.then(ClientCommands.literal("on").executes(c -> { setConsequence(Settings.Consequence.SURVIVAL); return 1; }))
					.then(ClientCommands.literal("off").executes(c -> { setConsequence(Settings.Consequence.NONE); return 1; })))
				.then(ClientCommands.literal("interval")
					.then(ClientCommands.argument("minutes", IntegerArgumentType.integer(1, 120)).executes(c -> {
						settings.intervalMinutes = IntegerArgumentType.getInteger(c, "minutes");
						saveSettings();
						quizTimer = 0;
						say(Component.literal("Quiz every " + settings.intervalMinutes + " min.").withStyle(ChatFormatting.AQUA));
						return 1;
					})))
				.then(ClientCommands.literal("stats").executes(c -> { stats(); return 1; }))));
	}

	private static void loadLanguage(String code) {
		pack = LanguagePack.load(code);
		vocab = Vocabulary.load(pack);
		if (!vocab.missing().isEmpty()) LOG.warn("Language '{}' has no text for: {}", code, vocab.missing());
		store = new ProgressStore(configDir);
		progress = store.load(code);
		LOG.info("SvenskaCraft: {} words in {} ({} discovered)", vocab.entries().size(), pack.name(), progress.discoveredIds().size());
	}

	private static void tick(Minecraft mc) {
		audio.tick();
		boolean inWorld = mc.player != null && mc.level != null;
		if (wasInWorld && !inWorld) saveProgressNow();
		wasInWorld = inWorld;
		store.saveIfDueQuietly(progress);
		if (!inWorld) return;

		while (journalKey.consumeClick()) {
			if (mc.gui.screen() == null) mc.gui.setScreen(new JournalScreen());
		}
		if (pendingScreen != null && mc.gui.screen() == null) {
			Screen s = pendingScreen.get();
			pendingScreen = null;
			if (s != null) mc.gui.setScreen(s);
			return;
		}
		if (++worldTicks % 200 == 0) DailyGoal.rollOver(progress, LocalDate.now().toEpochDay());
		discovery.tick(mc);
		if (!settings.enabled || mc.isPaused() || !mc.player.isAlive() || mc.gui.screen() instanceof QuizScreen) return;

		int interval = settings.quizMinutes() * 60 * 20;
		if (quizTimer < interval) quizTimer++;
		if (quizTimer == interval - 200 && !progress.discoveredIds().isEmpty()) {
			mc.gui.hud.setOverlayMessage(Component.literal("Quiz in 10 seconds").withStyle(ChatFormatting.YELLOW), false);
		}
		if (quizTimer >= interval && mc.gui.screen() == null) {
			quizTimer = 0;
			Screen q = quizScreen();
			if (q != null) mc.gui.setScreen(q);
		}
	}

	/** A quiz round for the current intensity, or null when no word has been discovered yet. */
	private static Screen quizScreen() {
		quizTimer = 0;
		List<String> ids = Scheduler.pick(progress.discoveredIds(), progress, settings.intensity.questions, System.currentTimeMillis(), new Random());
		if (ids.isEmpty()) {
			say(Component.literal("Discover some words first – pick things up, look at animals!").withStyle(ChatFormatting.GRAY));
			return null;
		}
		return new QuizScreen(ids.stream().map(vocab::byId).filter(java.util.Objects::nonNull).toList());
	}

	/** Called by the pickup mixin. */
	public static void onPickup(ItemStack stack, int amount) {
		discovery.onPickup(stack, amount);
	}

	/** Called after every answered quiz/practice question: counts toward the daily goal. */
	public static void answered() {
		if (DailyGoal.countAnswer(progress, LocalDate.now().toEpochDay(), settings.dailyGoal)) {
			Minecraft mc = Minecraft.getInstance();
			String days = progress.dayStreak > 1 ? " · " + progress.dayStreak + " " + pack.ui("daysInARow") : "";
			SystemToast.add(mc.gui.toastManager(), new SystemToast.SystemToastId(),
				Component.literal("✔ " + pack.ui("dailyGoal") + days), Component.literal("Daily goal reached: " + settings.dailyGoal + " questions"));
			say(Component.literal("✔ " + pack.ui("dailyGoal") + " (daily goal) reached!" + days).withStyle(ChatFormatting.GREEN));
			AudioPlayer.cue(SoundEvents.PLAYER_LEVELUP, 1.2f, 0.3f);
		}
	}

	/** Adds the Swedish word under the item name for words the player has discovered. */
	private static void tooltip(ItemStack stack, List<Component> lines) {
		if (!settings.enabled || !settings.tooltips || lines.isEmpty()) return;
		Vocabulary.Entry e = vocab.byItem(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		if (e == null || !progress.isDiscovered(e.id())) return;
		lines.add(1, Component.literal(pack.nativeName() + ": ").withStyle(ChatFormatting.DARK_AQUA)
			.append(Component.literal(e.word()).withStyle(ChatFormatting.GOLD)));
	}

	/** Mark progress as changed and announce new milestones. */
	public static void progressChanged() {
		store.markDirty();
		Minecraft mc = Minecraft.getInstance();
		for (Milestones.Milestone m : Milestones.check(progress, vocab)) {
			Component desc = Component.literal(m.description() + (m.shield() ? " · +1 shield" : ""));
			SystemToast.add(mc.gui.toastManager(), new SystemToast.SystemToastId(), Component.literal("★ " + m.title()), desc);
			say(Component.literal("★ Milestone: " + m.title()).withStyle(ChatFormatting.GOLD).append(Component.literal(" – ").append(desc).withStyle(ChatFormatting.GRAY)));
			AudioPlayer.cue(SoundEvents.PLAYER_LEVELUP, 1.4f, 0.3f);
		}
	}

	public static void saveSettings() {
		try {
			settings.save(configDir);
		} catch (Exception e) {
			LOG.warn("Could not save settings", e);
		}
	}

	private static void saveProgressNow() {
		try {
			store.flush(progress, System.currentTimeMillis());
		} catch (Exception e) {
			LOG.warn("Could not save progress", e);
		}
	}

	public static void say(Component msg) {
		Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(msg);
	}

	private static void setEnabled(boolean on) {
		settings.enabled = on;
		saveSettings();
		say(Component.literal(on ? "SvenskaCraft is ON." : "SvenskaCraft is OFF.").withStyle(ChatFormatting.AQUA));
	}

	private static void setConsequence(Settings.Consequence c) {
		settings.consequence = c;
		saveSettings();
		say(Component.literal("Quiz consequences: " + c.label).withStyle(ChatFormatting.AQUA));
	}

	private static void stats() {
		Milestones.Stats s = Milestones.stats(progress, vocab);
		say(Component.literal("Words " + s.discovered() + "/" + vocab.entries().size() + " · familiar " + s.familiarOrBetter()
			+ " · mastered " + s.mastered() + " · points " + progress.points + " · streak " + progress.streak
			+ " · shields " + progress.shields + " · today " + progress.answeredToday + "/" + settings.dailyGoal
			+ " · day streak " + progress.dayStreak).withStyle(ChatFormatting.AQUA));
	}

	private static void help() {
		say(Component.literal("SvenskaCraft – commands (journal: J)").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		for (String line : new String[] {
			"/svenska journal – your words, sounds, examples, progress",
			"/svenska settings – intensity, translations, voice, penalties",
			"/svenska quiz – quiz now",
			"/svenska interval <minutes> – time between quizzes",
			"/svenska penalty on|off – survival challenge / no penalty",
			"/svenska on|off – turn the mod on/off",
			"/svenska stats – progress summary"}) {
			say(Component.literal("  " + line).withStyle(ChatFormatting.GRAY));
		}
	}
}

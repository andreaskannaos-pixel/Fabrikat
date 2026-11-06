package svenskacraft;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SvenskaCraft implements ClientModInitializer {
	public static final String MOD_ID = "svenskacraft";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);
	public static Config config;

	private static final long GLOBAL_COOLDOWN_MS = 3_000;
	private static final long WORD_COOLDOWN_MS = 120_000;
	private static final Map<String, Long> lastTaught = new HashMap<>();
	private static long lastAnyTaught = 0;
	private static int quizTimer = 0;
	private static boolean quizRequested = false;

	@Override
	public void onInitializeClient() {
		config = Config.load();
		LOG.info("SvenskaCraft loaded with {} words", Words.ALL.size());
		ClientTickEvents.END_CLIENT_TICK.register(SvenskaCraft::tick);
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
			ClientCommands.literal("svenska")
				.executes(c -> { help(); return 1; })
				// Queued: chat closes itself right after running a command, which would close the quiz.
				.then(ClientCommands.literal("quiz").executes(c -> { quizRequested = true; return 1; }))
				.then(ClientCommands.literal("on").executes(c -> { setEnabled(true); return 1; }))
				.then(ClientCommands.literal("off").executes(c -> { setEnabled(false); return 1; }))
				.then(ClientCommands.literal("penalty")
					.then(ClientCommands.literal("on").executes(c -> { setPenalty(true); return 1; }))
					.then(ClientCommands.literal("off").executes(c -> { setPenalty(false); return 1; })))
				.then(ClientCommands.literal("interval")
					.then(ClientCommands.argument("minutes", IntegerArgumentType.integer(1, 120)).executes(c -> {
						config.intervalMinutes = IntegerArgumentType.getInteger(c, "minutes");
						config.save();
						quizTimer = 0;
						say(Component.literal("Quiz var " + config.intervalMinutes + ":e minut (every " + config.intervalMinutes + " min).").withStyle(ChatFormatting.AQUA));
						return 1;
					})))
				.then(ClientCommands.literal("words").executes(c -> { listWords(); return 1; }))
				.then(ClientCommands.literal("stats").executes(c -> { stats(); return 1; }))));
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null || mc.isPaused() || !mc.player.isAlive()) return;
		if (mc.gui.screen() instanceof QuizScreen) return;
		if (quizRequested && mc.gui.screen() == null) {
			quizRequested = false;
			startQuiz();
			return;
		}
		if (!config.enabled) return;
		int interval = config.intervalMinutes * 60 * 20;
		if (quizTimer < interval) quizTimer++;
		if (quizTimer == interval - 200) {
			mc.gui.hud.setOverlayMessage(Component.literal("Svenska-quiz om 10 sekunder! (Quiz in 10 seconds!)").withStyle(ChatFormatting.YELLOW), false);
		}
		// Wait until no other screen (inventory, chest, chat...) is open.
		if (quizTimer >= interval && mc.gui.screen() == null) startQuiz();
	}

	public static void startQuiz() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		quizTimer = 0;
		mc.gui.setScreen(QuizScreen.random(mc.player.getRandom()));
	}

	/** Called when the local player picks up an item entity. */
	public static void onPickup(ItemStack stack) {
		if (!config.enabled || stack.isEmpty()) return;
		Word w = Words.byItem(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		if (w == null) return;
		long now = System.currentTimeMillis();
		if (now - lastAnyTaught < GLOBAL_COOLDOWN_MS) return;
		Long last = lastTaught.get(w.id());
		if (last != null && now - last < WORD_COOLDOWN_MS) return;
		lastAnyTaught = now;
		lastTaught.put(w.id(), now);

		Minecraft mc = Minecraft.getInstance();
		mc.gui.hud.setTimes(5, 50, 15);
		mc.gui.hud.setSubtitle(Component.literal(w.en()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		mc.gui.hud.setTitle(Component.literal(w.sv()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		Voice.play("w." + w.id());
		say(Component.literal("[SV] ").withStyle(ChatFormatting.AQUA)
			.append(Component.literal(w.sv()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(" = " + w.en()).withStyle(ChatFormatting.GRAY)));
		if (config.learned.add(w.id())) config.save();
	}

	public static void say(Component msg) {
		Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(msg);
	}

	private static void setEnabled(boolean on) {
		config.enabled = on;
		config.save();
		say(Component.literal(on ? "SvenskaCraft är PÅ (on)." : "SvenskaCraft är AV (off).").withStyle(ChatFormatting.AQUA));
	}

	private static void setPenalty(boolean on) {
		config.penalty = on;
		config.save();
		say(Component.literal(on ? "Straff PÅ: fel svar = du förlorar ett föremål." : "Straff AV: inga föremål försvinner.").withStyle(ChatFormatting.AQUA));
	}

	private static void listWords() {
		say(Component.literal("Dina ord (" + config.learned.size() + "/" + Words.ALL.size() + "):").withStyle(ChatFormatting.AQUA));
		for (String id : config.learned) {
			Word w = Words.byId(id);
			if (w != null) say(Component.literal("  " + w.sv()).withStyle(ChatFormatting.GOLD).append(Component.literal(" = " + w.en()).withStyle(ChatFormatting.GRAY)));
		}
	}

	private static void stats() {
		say(Component.literal("Poäng (points): " + config.points + "   Svit (streak): " + config.streak
			+ "   Ord (words): " + config.learned.size() + "/" + Words.ALL.size()).withStyle(ChatFormatting.AQUA));
	}

	private static void help() {
		say(Component.literal("SvenskaCraft – kommandon:").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		for (String line : new String[] {
			"/svenska quiz – quiz now",
			"/svenska interval <minutes> – time between quizzes",
			"/svenska penalty on|off – lose an item on wrong answer",
			"/svenska on|off – turn the mod on/off",
			"/svenska words – words you have learned",
			"/svenska stats – points and streak"}) {
			say(Component.literal("  " + line).withStyle(ChatFormatting.GRAY));
		}
	}
}

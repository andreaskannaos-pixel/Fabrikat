package svenskacraft.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import svenskacraft.SvenskaCraft;
import svenskacraft.settings.Settings;

/** Player settings. Every change is saved immediately. */
public class SettingsScreen extends Screen {
	private static final int[] INTERVALS = {0, 2, 5, 10, 15, 20, 30};
	private static final int[] ANSWER_TIMES = {15, 20, 30, 45, 60};

	private final Screen parent;

	public SettingsScreen(Screen parent) {
		super(Component.literal("SvenskaCraft settings"));
		this.parent = parent;
	}

	private Settings s() { return SvenskaCraft.settings; }

	@Override
	protected void init() {
		int cx = width / 2;
		int y = Math.max(28, height / 2 - 70);
		int lx = cx - 155, rx = cx + 5;

		cycle(lx, y, () -> "Learning intensity: " + s().intensity.label, () -> s().intensity = Settings.next(s().intensity));
		cycle(rx, y, () -> "Translations: " + s().translations.label, () -> s().translations = Settings.next(s().translations));
		y += 24;
		cycle(lx, y, () -> "Voice: " + s().voice.label, () -> {
			s().voice = Settings.next(s().voice);
			if (s().voice == Settings.VoiceMode.OFF) SvenskaCraft.audio.stop();
		});
		addRenderableWidget(new AbstractSliderButton(rx, y, 150, 20, Component.empty(), s().voiceVolume / 100.0) {
			{ updateMessage(); }

			@Override
			protected void updateMessage() { setMessage(Component.literal("Voice volume: " + (int) Math.round(value * 100) + "%")); }

			@Override
			protected void applyValue() {
				s().voiceVolume = (int) Math.round(value * 100);
				SvenskaCraft.saveSettings();
			}
		});
		y += 24;
		cycle(lx, y, () -> "Quiz penalty: " + s().consequence.shortLabel, () -> s().consequence = Settings.next(s().consequence));
		cycle(rx, y, () -> "Quiz every: " + (s().intervalMinutes == 0 ? "auto (" + s().quizMinutes() + " min)" : s().intervalMinutes + " min"),
			() -> s().intervalMinutes = nextOf(INTERVALS, s().intervalMinutes));
		y += 24;
		cycle(lx, y, () -> "Answer time: " + s().answerSeconds + " s", () -> s().answerSeconds = nextOf(ANSWER_TIMES, s().answerSeconds));
		cycle(rx, y, () -> "SvenskaCraft: " + (s().enabled ? "ON" : "OFF"), () -> s().enabled = !s().enabled);
		y += 38;
		addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(cx - 50, y, 100, 20).build());
	}

	private void cycle(int x, int y, java.util.function.Supplier<String> label, Runnable change) {
		addRenderableWidget(Button.builder(Component.literal(label.get()), b -> {
			change.run();
			SvenskaCraft.saveSettings();
			rebuildWidgets();
		}).bounds(x, y, 150, 20).build());
	}

	private static int nextOf(int[] values, int current) {
		for (int i = 0; i < values.length; i++) if (values[i] == current) return values[(i + 1) % values.length];
		return values[0];
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		int cx = width / 2;
		int top = Math.max(28, height / 2 - 70);
		g.centeredText(font, Component.literal("SvenskaCraft – Settings"), cx, top - 20, 0xFFFFCC00);
		String hint = switch (s().intensity) {
			case RELAXED -> "Relaxed: 1 question every 15 min, no reminders for known words.";
			case BALANCED -> "Balanced: 2 questions every 10 min, occasional reminders.";
			case IMMERSIVE -> "Immersive: 3 questions every 5 min, frequent reminders.";
		};
		g.centeredText(font, Component.literal(hint), cx, top + 97, 0xFF999999);
	}

	@Override
	public void onClose() { minecraft.gui.setScreen(parent); }

	@Override
	public boolean isPauseScreen() { return false; }
}

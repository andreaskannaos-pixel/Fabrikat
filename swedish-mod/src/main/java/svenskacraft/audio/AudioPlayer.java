package svenskacraft.audio;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import svenskacraft.SvenskaCraft;
import svenskacraft.settings.Settings;

/**
 * One voice channel: voice clips never overlap.
 * MANUAL (player pressed ▶) and REACTION replace whatever is playing; AUTO (discovery) waits its turn.
 * Voice clips use Minecraft's "Voice/Speech" volume slider times the mod's own volume setting.
 * Short non-voice cues (chimes) are separate and stay on even when voice is off.
 */
public final class AudioPlayer {
	public enum Priority { MANUAL, REACTION, AUTO }

	private static final int MAX_QUEUE = 2;
	private static final int MIN_TICKS = 3;     // grace period while the sound engine starts a clip
	private static final int GAP_TICKS = 8;     // breathing room between queued clips

	private final Deque<String> queue = new ArrayDeque<>();
	private final Map<String, Integer> lastReaction = new HashMap<>();
	private final RandomSource random = RandomSource.create();
	private SoundInstance current;
	private int ticks;
	private int startedAt;
	private int idleSince;

	public boolean voiceEnabled() { return SvenskaCraft.settings.voice != Settings.VoiceMode.OFF; }

	public boolean isPlaying() {
		if (current == null) return false;
		if (ticks - startedAt < MIN_TICKS) return true;
		return Minecraft.getInstance().getSoundManager().isActive(current);
	}

	public void play(String soundName, Priority priority) {
		if (!voiceEnabled() || soundName == null) return;
		if (priority == Priority.AUTO) {
			if (isPlaying() || !queue.isEmpty() || ticks - idleSince < GAP_TICKS) {
				if (queue.size() >= MAX_QUEUE) queue.pollFirst();
				queue.addLast(soundName);
				return;
			}
		} else {
			stop();
		}
		start(soundName);
	}

	/** Plays a random reaction of the given kind, never the same line twice in a row. Voice mode "Discovery only". */
	public void react(String kind) {
		if (SvenskaCraft.settings.voice != Settings.VoiceMode.DISCOVERY) return;
		List<String> lines = SvenskaCraft.pack.reactions(kind);
		if (lines.isEmpty()) return;
		int last = lastReaction.getOrDefault(kind, -1);
		int pick = random.nextInt(lines.size());
		if (lines.size() > 1 && pick == last) pick = (pick + 1 + random.nextInt(lines.size() - 1)) % lines.size();
		lastReaction.put(kind, pick);
		play(SvenskaCraft.pack.reactionSound(kind, pick), Priority.REACTION);
	}

	public void stop() {
		queue.clear();
		if (current != null) Minecraft.getInstance().getSoundManager().stop(current);
		current = null;
		idleSince = ticks;
	}

	public void tick() {
		ticks++;
		if (current != null && !isPlaying()) {
			current = null;
			idleSince = ticks;
		}
		if (current == null && !queue.isEmpty() && ticks - idleSince >= GAP_TICKS) {
			if (voiceEnabled()) start(queue.pollFirst());
			else queue.clear();
		}
	}

	private void start(String soundName) {
		float volume = SvenskaCraft.settings.voiceVolume / 100f;
		if (volume <= 0) return;
		Identifier id = Identifier.fromNamespaceAndPath(SvenskaCraft.MOD_ID, soundName);
		// While a pausing screen is open (singleplayer quiz/journal) Minecraft pauses every sound except
		// music and UI, so the voice uses the UI channel then; otherwise the Voice/Speech channel.
		Minecraft mc = Minecraft.getInstance();
		boolean paused = mc.isPaused() || (mc.gui.screen() != null && mc.gui.screen().isPauseScreen() && mc.hasSingleplayerServer());
		current = new SimpleSoundInstance(id, paused ? SoundSource.UI : SoundSource.VOICE, volume, 1.0f, random, false, 0,
			SoundInstance.Attenuation.NONE, 0, 0, 0, true);
		startedAt = ticks;
		Minecraft.getInstance().getSoundManager().play(current);
	}

	// ---- Subtle non-voice cues (UI channel) ----

	public static void cue(SoundEvent event, float pitch, float volume) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
	}

	public static void cue(Holder<SoundEvent> event, float pitch, float volume) {
		cue(event.value(), pitch, volume);
	}
}

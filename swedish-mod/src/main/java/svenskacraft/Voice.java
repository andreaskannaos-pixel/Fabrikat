package svenskacraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class Voice {
	private Voice() {}

	/** Plays svenskacraft:<name>, e.g. "w.diamond", "fb.diamond", "fx.quiz". */
	public static void play(String name) {
		SoundEvent ev = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(SvenskaCraft.MOD_ID, name));
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, 1.0f, 1.0f));
	}
}

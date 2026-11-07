package svenskacraft.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import svenskacraft.SvenskaCraft;
import svenskacraft.learning.WordState;
import svenskacraft.vocab.Vocabulary.Entry;

/** Shared bits for the journal and quiz screens. */
final class Journal {
	private Journal() {}

	static ItemStack icon(Entry e) {
		return iconOf(e.icon(), e.iconCount());
	}

	static ItemStack iconOf(String id, int count) {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id)), count);
	}

	static Font font() { return Minecraft.getInstance().font; }

	static String stateName(WordState s) {
		String sv = SvenskaCraft.pack.ui("state." + s.name().toLowerCase());
		String en = switch (s) {
			case NEW -> "New";
			case LEARNING -> "Learning";
			case FAMILIAR -> "Familiar";
			case MASTERED -> "Mastered";
		};
		return sv + " (" + en + ")";
	}

	static String stateShort(WordState s) { return SvenskaCraft.pack.ui("state." + s.name().toLowerCase()); }

	static ChatFormatting stateColor(WordState s) {
		return switch (s) {
			case NEW -> ChatFormatting.DARK_GRAY;
			case LEARNING -> ChatFormatting.YELLOW;
			case FAMILIAR -> ChatFormatting.AQUA;
			case MASTERED -> ChatFormatting.GREEN;
		};
	}

	static int stateArgb(WordState s) {
		return switch (s) {
			case NEW -> 0xFF555555;
			case LEARNING -> 0xFFFFFF55;
			case FAMILIAR -> 0xFF55FFFF;
			case MASTERED -> 0xFF55FF55;
		};
	}
}

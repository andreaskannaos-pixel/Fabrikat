package svenskacraft;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import svenskacraft.settings.Settings;

/** Quiz consequences. A shield (milestone reward) blocks one penalty. */
public final class Penalty {
	private Penalty() {}

	/** Applies the configured consequence for a wrong answer. Returns a message, or empty for "no penalty". */
	public static Component apply(Minecraft mc, Settings.Consequence consequence) {
		if (consequence == Settings.Consequence.NONE) return Component.empty();
		if (SvenskaCraft.progress.shields > 0) {
			SvenskaCraft.progress.shields--;
			SvenskaCraft.progressChanged();
			return Component.literal("Din sköld skyddade dig! (Your shield protected you – " + SvenskaCraft.progress.shields + " left)")
				.withStyle(ChatFormatting.AQUA);
		}
		return remove(mc, consequence == Settings.Consequence.SURVIVAL);
	}

	private static Component remove(Minecraft mc, boolean wholeStack) {
		LocalPlayer p = mc.player;
		if (p == null) return Component.empty();
		Inventory inv = p.getInventory();
		List<Integer> filled = new ArrayList<>();
		for (int i = 0; i < 36; i++) if (!inv.getItem(i).isEmpty()) filled.add(i);
		if (filled.isEmpty()) {
			return Component.literal("Din inventory var tom – du hade tur! (Empty inventory – lucky!)").withStyle(ChatFormatting.YELLOW);
		}
		int slot = filled.get(p.getRandom().nextInt(filled.size()));
		ItemStack lost = inv.getItem(slot).copy();
		int count = wholeStack ? lost.getCount() : 1;

		if (mc.hasSingleplayerServer()) {
			// Singleplayer / LAN host: really delete it on the built-in server.
			IntegratedServer server = mc.getSingleplayerServer();
			UUID id = p.getUUID();
			server.execute(() -> {
				ServerPlayer sp = server.getPlayerList().getPlayer(id);
				if (sp != null) sp.getInventory().removeItem(slot, count);
			});
		} else if (mc.gameMode != null) {
			// On a server a client mod can't delete items, so it is thrown away instead.
			int menuSlot = slot < 9 ? 36 + slot : slot;
			mc.gameMode.handleContainerInput(p.inventoryMenu.containerId, menuSlot, wholeStack ? 1 : 0, ContainerInput.THROW, p);
		}
		return Component.literal("Du förlorade: " + count + "x ").withStyle(ChatFormatting.RED)
			.append(lost.getHoverName().copy().withStyle(ChatFormatting.GOLD));
	}
}

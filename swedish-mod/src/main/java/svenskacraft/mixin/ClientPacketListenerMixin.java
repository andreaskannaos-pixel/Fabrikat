package svenskacraft.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import svenskacraft.SvenskaCraft;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Inject(method = "handleTakeItemEntity", at = @At("HEAD"))
	private void svenskacraft$onPickup(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		// The handler first runs on the network thread, then again on the main thread.
		if (!mc.isSameThread() || mc.player == null || mc.level == null) return;
		if (packet.getPlayerId() != mc.player.getId()) return;
		Entity e = mc.level.getEntity(packet.getItemId());
		if (e instanceof ItemEntity item) SvenskaCraft.onPickup(item.getItem());
	}
}

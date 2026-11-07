package svenskacraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Quizzes must never interrupt a fight. A quiz only starts after the player has been
 * out of danger for a few seconds: no monsters close by, not hurt, burning, falling,
 * drowning, gliding, low on health or swinging their weapon.
 */
public final class Safety {
	public static final int SAFE_TICKS = 100; // 5 seconds
	private static final double MONSTER_RANGE = 16.0;

	private int safeTicks;

	public void tick(Minecraft mc) {
		safeTicks = inDanger(mc) ? 0 : safeTicks + 1;
	}

	public boolean safe() { return safeTicks >= SAFE_TICKS; }

	static boolean inDanger(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null) return true;
		if (p.hurtTime > 0 || p.isOnFire() || p.isInLava() || p.isUnderWater() || p.isFallFlying()) return true;
		if (p.fallDistance > 2 || p.getHealth() <= 6) return true;
		if (mc.options.keyAttack.isDown()) return true;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof Enemy && e.isAlive() && e.distanceTo(p) < MONSTER_RANGE) return true;
		}
		return false;
	}
}

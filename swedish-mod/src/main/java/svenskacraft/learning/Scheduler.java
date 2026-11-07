package svenskacraft.learning;

import java.util.*;
import svenskacraft.progress.Progress;
import svenskacraft.progress.WordProgress;

/** Chooses quiz words: difficult and overdue words more often, mastered words now and then, no quick repeats. */
public final class Scheduler {
	public static final int RECENT_COUNT = 5;
	public static final long RECENT_MS = 10 * Mastery.MINUTE;
	private static final int RECENT_KEEP = 10;

	private Scheduler() {}

	public static List<String> pick(Collection<String> discovered, Progress progress, int count, long now, Random rnd) {
		List<String> fresh = new ArrayList<>();
		List<String> recent = new ArrayList<>();
		Set<String> lastFew = new HashSet<>(lastN(progress.recent, RECENT_COUNT));
		for (String id : discovered) {
			WordProgress w = progress.word(id);
			if (w == null || !w.discovered()) continue;
			boolean tooSoon = lastFew.contains(id) || (w.lastAskedAt > 0 && now - w.lastAskedAt < RECENT_MS);
			(tooSoon ? recent : fresh).add(id);
		}
		List<String> out = weightedSample(fresh, progress, Math.min(count, fresh.size()), now, rnd);
		if (out.size() < count && !recent.isEmpty()) {
			// Not enough words: reuse the ones asked longest ago, but never the very last question.
			String last = progress.recent.isEmpty() ? null : progress.recent.get(progress.recent.size() - 1);
			recent.sort(Comparator.comparingLong(id -> progress.word(id).lastAskedAt));
			for (String id : recent) {
				if (out.size() >= count) break;
				if (!id.equals(last) || recent.size() + fresh.size() == 1) out.add(id);
			}
		}
		return out;
	}

	public static double weight(WordProgress w, long now) {
		if (w.asked() == 0) return 4.0;
		WordState state = Mastery.state(w);
		long due = Mastery.dueAt(w);
		boolean isDue = now >= due;
		double overdue = isDue ? Math.min(3.0, (double) (now - due) / Mastery.interval(w)) : 0;
		return switch (state) {
			case LEARNING -> 3.0 * (isDue ? 1 + overdue : 0.5);
			case FAMILIAR -> 1.5 * (isDue ? 1 + overdue : 0.3);
			case MASTERED -> isDue ? 0.6 * (1 + overdue) : 0.05;
			case NEW -> 0;
		};
	}

	public static void remember(Progress progress, String id) {
		progress.recent.add(id);
		while (progress.recent.size() > RECENT_KEEP) progress.recent.remove(0);
	}

	private static List<String> weightedSample(List<String> ids, Progress progress, int count, long now, Random rnd) {
		List<String> pool = new ArrayList<>(ids);
		List<String> out = new ArrayList<>();
		while (out.size() < count && !pool.isEmpty()) {
			double total = 0;
			double[] w = new double[pool.size()];
			for (int i = 0; i < pool.size(); i++) {
				w[i] = Math.max(1e-6, weight(progress.word(pool.get(i)), now));
				total += w[i];
			}
			double r = rnd.nextDouble() * total;
			int pick = pool.size() - 1;
			for (int i = 0; i < w.length; i++) {
				r -= w[i];
				if (r <= 0) { pick = i; break; }
			}
			out.add(pool.remove(pick));
		}
		return out;
	}

	private static List<String> lastN(List<String> list, int n) {
		return list.subList(Math.max(0, list.size() - n), list.size());
	}
}

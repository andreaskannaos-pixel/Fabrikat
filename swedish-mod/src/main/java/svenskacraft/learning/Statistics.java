package svenskacraft.learning;

import java.util.Iterator;
import java.util.Map;
import svenskacraft.progress.Progress;
import svenskacraft.progress.Stats.DayStats;
import svenskacraft.progress.Stats.KindStats;

/** Records answers per question kind and per day. Keeps 120 days of history. */
public final class Statistics {
	public static final int KEEP_DAYS = 120;

	private Statistics() {}

	public static DayStats day(Progress p, long day) {
		return p.days.computeIfAbsent(Long.toString(day), k -> new DayStats());
	}

	public static KindStats kind(Progress p, String kind) {
		return p.kinds.computeIfAbsent(kind, k -> new KindStats());
	}

	public static void answer(Progress p, String kind, boolean typed, boolean correct, boolean fresh, long day) {
		count(kind(p, kind), correct, fresh);
		if (typed) count(kind(p, "TYPED"), correct, fresh);
		DayStats d = day(p, day);
		d.answered++;
		if (correct) d.correct++;
		trim(p, day);
	}

	public static void newWord(Progress p, long day) {
		day(p, day).newWords++;
		trim(p, day);
	}

	public static void mastered(Progress p, long day) {
		day(p, day).mastered++;
	}

	public static int correct(Progress p, String kind) {
		KindStats k = p.kinds.get(kind);
		return k == null ? 0 : k.correct;
	}

	/** Sum over the last n days, including today. */
	public static DayStats lastDays(Progress p, long today, int n) {
		DayStats sum = new DayStats();
		for (long d = today - n + 1; d <= today; d++) {
			DayStats x = p.days.get(Long.toString(d));
			if (x == null) continue;
			sum.answered += x.answered;
			sum.correct += x.correct;
			sum.newWords += x.newWords;
			sum.mastered += x.mastered;
		}
		return sum;
	}

	private static void count(KindStats k, boolean correct, boolean fresh) {
		if (correct) k.correct++;
		else k.wrong++;
		if (correct && fresh) k.freshCorrect++;
	}

	public static int freshCorrect(Progress p, String... kinds) {
		int n = 0;
		for (String kind : kinds) {
			KindStats k = p.kinds.get(kind);
			if (k != null) n += k.freshCorrect;
		}
		return n;
	}

	private static void trim(Progress p, long today) {
		for (Iterator<Map.Entry<String, DayStats>> it = p.days.entrySet().iterator(); it.hasNext(); ) {
			try {
				if (today - Long.parseLong(it.next().getKey()) >= KEEP_DAYS) it.remove();
			} catch (NumberFormatException e) {
				it.remove();
			}
		}
	}
}

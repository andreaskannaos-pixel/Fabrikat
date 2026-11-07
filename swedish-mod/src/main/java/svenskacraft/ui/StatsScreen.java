package svenskacraft.ui;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import svenskacraft.SvenskaCraft;
import svenskacraft.learning.Mastery;
import svenskacraft.learning.Statistics;
import svenskacraft.learning.WordState;
import svenskacraft.progress.Progress;
import svenskacraft.progress.Stats;
import svenskacraft.vocab.Vocabulary.Entry;

/** Learning statistics: this week, the last 14 days, mastery spread, accuracy per question type, streaks. */
public class StatsScreen extends Screen {
	private static final Map<String, String> KINDS = new LinkedHashMap<>();
	static {
		KINDS.put("CHOICE", "Multiple choice");
		KINDS.put("GENDER", "en / ett");
		KINDS.put("LISTEN", "Listening");
		KINDS.put("TYPE", "Spelling");
		KINDS.put("FORM", "Grammar forms");
		KINDS.put("CLOZE", "Gap sentences");
		KINDS.put("DEFINITION", "Definitions");
		KINDS.put("SENTENCE", "Sentence building");
		KINDS.put("TYPED", "All typed answers");
	}

	private final Screen parent;

	public StatsScreen(Screen parent) {
		super(Component.literal("Statistics"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width - 48, 4, 42, 16).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		Progress p = SvenskaCraft.progress;
		long today = LocalDate.now().toEpochDay();
		g.text(font, Component.literal(SvenskaCraft.pack.ui("stats") + " · Statistics").withStyle(ChatFormatting.BOLD), 8, 8, 0xFFFFCC00);

		// ---- left column
		int x = 10, y = 26;
		Stats.DayStats week = Statistics.lastDays(p, today, 7);
		g.text(font, Component.literal("This week").withStyle(ChatFormatting.BOLD), x, y, 0xFFFFFFFF);
		y += 11;
		g.text(font, Component.literal("Questions answered: " + week.answered), x, y, 0xFFCCCCCC);
		y += 10;
		g.text(font, Component.literal("Correct: " + pct(week.correct, week.answered)), x, y, 0xFFCCCCCC);
		y += 10;
		g.text(font, Component.literal("New words: " + week.newWords + " · Mastered: " + week.mastered), x, y, 0xFFCCCCCC);
		y += 16;

		int[] states = new int[4];
		for (Entry e : SvenskaCraft.vocab.entries()) states[Mastery.state(p.word(e.id())).ordinal()]++;
		int total = SvenskaCraft.vocab.entries().size();
		g.text(font, Component.literal("Your words").withStyle(ChatFormatting.BOLD), x, y, 0xFFFFFFFF);
		y += 11;
		int barW = 190, bx = x;
		int[] colors = {0xFF555555, 0xFFFFDD55, 0xFF55DDFF, 0xFF55FF55};
		for (int i = 0; i < 4; i++) {
			int w = barW * states[i] / Math.max(1, total);
			g.fill(bx, y, bx + w, y + 8, colors[i]);
			bx += w;
		}
		y += 11;
		WordState[] order = WordState.values();
		for (int i = 0; i < 4; i++) {
			g.fill(x + (i % 2) * 96, y + (i / 2) * 10 + 1, x + (i % 2) * 96 + 6, y + (i / 2) * 10 + 7, colors[i]);
			g.text(font, Component.literal(Journal.stateShort(order[i]) + ": " + states[i]), x + (i % 2) * 96 + 9, y + (i / 2) * 10, 0xFFCCCCCC);
		}
		y += 26;

		g.text(font, Component.literal("Streaks").withStyle(ChatFormatting.BOLD), x, y, 0xFFFFFFFF);
		y += 11;
		g.text(font, Component.literal("Daily goal: " + p.dayStreak + " days (best " + p.bestDayStreak + ")"), x, y, 0xFFCCCCCC);
		y += 10;
		g.text(font, Component.literal("Correct in a row: best " + p.bestStreak), x, y, 0xFFCCCCCC);
		y += 10;
		g.text(font, Component.literal("Daily quests completed: " + p.questsCompleted), x, y, 0xFFCCCCCC);

		// ---- right column: last 14 days
		int rx = width / 2 + 4, ry = 26, chartW = width - rx - 10, chartH = 44;
		g.text(font, Component.literal("Last 14 days").withStyle(ChatFormatting.BOLD), rx, ry, 0xFFFFFFFF);
		ry += 12;
		int max = 1;
		for (int d = 0; d < 14; d++) {
			Stats.DayStats ds = p.days.get(Long.toString(today - 13 + d));
			if (ds != null) max = Math.max(max, ds.answered);
		}
		int colW = Math.max(3, chartW / 14);
		g.fill(rx, ry + chartH, rx + colW * 14, ry + chartH + 1, 0xFF666666);
		for (int d = 0; d < 14; d++) {
			Stats.DayStats ds = p.days.get(Long.toString(today - 13 + d));
			if (ds == null || ds.answered == 0) continue;
			int h = chartH * ds.answered / max, hc = chartH * ds.correct / max;
			int cx0 = rx + d * colW + 1;
			g.fill(cx0, ry + chartH - h, cx0 + colW - 2, ry + chartH, 0xFFAA5555);
			g.fill(cx0, ry + chartH - hc, cx0 + colW - 2, ry + chartH, 0xFF55CC55);
		}
		ry += chartH + 4;
		g.text(font, Component.literal("green = correct, red = wrong (most: " + max + ")"), rx, ry, 0xFF888888);
		ry += 16;

		// accuracy per question type
		g.text(font, Component.literal("Accuracy by question type").withStyle(ChatFormatting.BOLD), rx, ry, 0xFFFFFFFF);
		ry += 11;
		for (Map.Entry<String, String> k : KINDS.entrySet()) {
			Stats.KindStats ks = p.kinds.get(k.getKey());
			int c = ks == null ? 0 : ks.correct, t = ks == null ? 0 : ks.total();
			g.text(font, Component.literal(k.getValue()), rx, ry, t == 0 ? 0xFF666666 : 0xFFCCCCCC);
			String v = t == 0 ? "–" : c + "/" + t + "  " + pct(c, t);
			g.text(font, Component.literal(v), width - 10 - font.width(v), ry, t == 0 ? 0xFF666666 : accuracyColor(c, t));
			ry += 10;
			if (ry > height - 10) break;
		}
	}

	private static String pct(int part, int whole) {
		return whole == 0 ? "–" : Math.round(100.0 * part / whole) + "%";
	}

	private static int accuracyColor(int c, int t) {
		double a = (double) c / t;
		return a >= 0.8 ? 0xFF55FF55 : a >= 0.5 ? 0xFFFFDD55 : 0xFFFF7777;
	}

	@Override
	public void onClose() { minecraft.gui.setScreen(parent); }

	@Override
	public boolean isPauseScreen() { return true; }
}

package svenskacraft.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import svenskacraft.SvenskaCraft;
import svenskacraft.audio.AudioPlayer;
import svenskacraft.learning.Forms;
import svenskacraft.learning.Level;
import svenskacraft.learning.Mastery;
import svenskacraft.learning.Milestones;
import svenskacraft.learning.Scheduler;
import svenskacraft.learning.WordState;
import svenskacraft.progress.WordProgress;
import svenskacraft.settings.Settings;
import svenskacraft.vocab.Category;
import svenskacraft.vocab.Hints;
import svenskacraft.vocab.Vocabulary.Entry;

/** Vocabulary journal: words by category with replay, example sentences, mastery, and an overview page. */
public class JournalScreen extends Screen {
	private static final int ROW_H = 18;
	private static final int TAB_H = 15;

	/** null = overview */
	private String tab;
	private int page;
	private String selected;
	private final List<String> revealedEn = new ArrayList<>();

	public JournalScreen() {
		super(Component.literal("Journal"));
	}

	private int x0() { return 128; }

	private int x1() { return width - 6; }

	private int listTop() { return 26; }

	private int detailTop() {
		var lvl = SvenskaCraft.settings.level;
		int extra = (lvl.atLeast(Level.INTERMEDIATE) ? 10 : 0) + (lvl == Level.ADVANCED ? 10 : 0)
			- (SvenskaCraft.settings.translations == Settings.Translations.NEVER ? 10 : 0);
		return height - 78 - Math.max(0, extra);
	}

	/** "diamanten · diamanter", "går · gick · gått", "tredje" – shown from Intermediate level. */
	private static String formsLine(Entry e) {
		var m = Forms.all(e.text());
		List<String> parts = new ArrayList<>();
		for (Forms.Type t : new Forms.Type[] {Forms.Type.DEFINITE, Forms.Type.NEUTER, Forms.Type.PLURAL, Forms.Type.PRESENT, Forms.Type.PAST, Forms.Type.SUPINE, Forms.Type.ORDINAL}) {
			if (m.containsKey(t)) parts.add(Forms.first(m.get(t)));
		}
		return String.join(" · ", parts);
	}

	private int rowsPerPage() { return Math.max(1, (detailTop() - listTop() - 2) / ROW_H); }

	/** Discovered words first (in data order), then the ones still to find. */
	private List<Entry> entries() {
		if (tab == null) return List.of();
		List<Entry> found = new ArrayList<>(), hidden = new ArrayList<>();
		for (Entry e : SvenskaCraft.vocab.inCategory(tab)) (SvenskaCraft.progress.isDiscovered(e.id()) ? found : hidden).add(e);
		found.addAll(hidden);
		return found;
	}

	private int pages() { return Math.max(1, (entries().size() + rowsPerPage() - 1) / rowsPerPage()); }

	private boolean voice() { return SvenskaCraft.audio.voiceEnabled(); }

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(Component.literal(SvenskaCraft.pack.ui("stats")), b -> minecraft.gui.setScreen(new StatsScreen(this)))
			.bounds(width - 176, 4, 60, 16).build());
		addRenderableWidget(Button.builder(Component.literal("Settings"), b -> minecraft.gui.setScreen(new SettingsScreen(this)))
			.bounds(width - 112, 4, 60, 16).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(width - 48, 4, 42, 16).build());

		int y = listTop();
		addRenderableWidget(tabButton(null, "Översikt", y));
		for (Category c : SvenskaCraft.vocab.categories()) {
			y += TAB_H;
			int[] n = count(c.id());
			addRenderableWidget(tabButton(c.id(), SvenskaCraft.pack.category(c.id()) + " " + n[0] + "/" + n[1], y));
		}
		if (tab == null) return;

		List<Entry> list = entries();
		page = Math.min(page, pages() - 1);
		int start = page * rowsPerPage();
		for (int i = start; i < Math.min(list.size(), start + rowsPerPage()); i++) {
			Entry e = list.get(i);
			if (!voice() || !SvenskaCraft.progress.isDiscovered(e.id())) continue;
			int ry = listTop() + (i - start) * ROW_H;
			addRenderableWidget(Button.builder(Component.literal("♪"), b -> playWord(e)).bounds(x1() - 17, ry + 1, 16, 16).build());
		}
		List<String> found = new ArrayList<>();
		for (Entry e : list) if (SvenskaCraft.progress.isDiscovered(e.id())) found.add(e.id());
		String practiceLabel = SvenskaCraft.settings.level == Level.ADVANCED ? "Öva" : "Öva (practice)";
		Button practice = addRenderableWidget(Button.builder(Component.literal(practiceLabel), b -> startPractice(found))
			.bounds(x1() - 92, height - 20, 92, 16).build());
		practice.active = !found.isEmpty();
		// sentence practice: words of this category whose example sentence works as a puzzle at this level
		List<String> sentenceWords = new ArrayList<>();
		for (String id : found) {
			Entry e = SvenskaCraft.vocab.byId(id);
			if (e != null && svenskacraft.learning.SentenceBuilder.tiles(e.example(), SvenskaCraft.settings.level) != null) sentenceWords.add(id);
		}
		Button sentences = addRenderableWidget(Button.builder(Component.literal(SvenskaCraft.pack.ui("sentences")), b -> {
			List<String> ids = Scheduler.pick(sentenceWords, SvenskaCraft.progress, 5, System.currentTimeMillis(), new Random());
			if (!ids.isEmpty()) minecraft.gui.setScreen(new QuizScreen(ids.stream().map(SvenskaCraft.vocab::byId).toList(), true, this,
				svenskacraft.learning.QuestionPlanner.Kind.SENTENCE));
		}).bounds(x1() - 92 - 66, height - 20, 62, 16).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Practice building sentences"))).build());
		sentences.active = !sentenceWords.isEmpty();
		if (pages() > 1) {
			addRenderableWidget(Button.builder(Component.literal("<"), b -> { page = Math.max(0, page - 1); rebuildWidgets(); })
				.bounds(x0(), height - 20, 20, 16).build()).active = page > 0;
			addRenderableWidget(Button.builder(Component.literal(">"), b -> { page = Math.min(pages() - 1, page + 1); rebuildWidgets(); })
				.bounds(x0() + 70, height - 20, 20, 16).build()).active = page < pages() - 1;
		}
		Entry sel = selectedEntry();
		if (sel != null && voice()) {
			addRenderableWidget(Button.builder(Component.literal("♪ Mening"),
				b -> SvenskaCraft.audio.play(SvenskaCraft.pack.exampleSound(sel.id()), AudioPlayer.Priority.MANUAL))
				.bounds(x1() - 64, detailTop() + 3, 62, 16).build());
		}
	}

	/** Up to 5 words from this category, chosen like a quiz (weak words first), with no timer or penalty. */
	private void startPractice(List<String> found) {
		List<String> ids = Scheduler.pick(found, SvenskaCraft.progress, 5, System.currentTimeMillis(), new Random());
		if (ids.isEmpty()) return;
		minecraft.gui.setScreen(new QuizScreen(ids.stream().map(SvenskaCraft.vocab::byId).toList(), true, this));
	}

	private Button tabButton(String id, String label, int y) {
		Button b = Button.builder(Component.literal(label), btn -> {
			tab = id;
			page = 0;
			selected = null;
			rebuildWidgets();
		}).bounds(6, y, 116, TAB_H - 1).build();
		b.active = !java.util.Objects.equals(tab, id);
		return b;
	}

	private void playWord(Entry e) {
		SvenskaCraft.audio.play(SvenskaCraft.pack.wordSound(e.id()), AudioPlayer.Priority.MANUAL);
	}

	private Entry selectedEntry() {
		if (selected == null || tab == null) return null;
		Entry e = SvenskaCraft.vocab.byId(selected);
		return e != null && SvenskaCraft.progress.isDiscovered(e.id()) ? e : null;
	}

	/** [discovered, total] */
	private int[] count(String category) {
		int d = 0, t = 0;
		for (Entry e : SvenskaCraft.vocab.inCategory(category)) {
			t++;
			if (SvenskaCraft.progress.isDiscovered(e.id())) d++;
		}
		return new int[] {d, t};
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		if (tab == null) return false;
		double mx = event.x(), my = event.y();
		if (mx < x0() || mx > x1() - 20 || my < listTop() || my >= listTop() + rowsPerPage() * ROW_H) return false;
		int i = page * rowsPerPage() + (int) ((my - listTop()) / ROW_H);
		List<Entry> list = entries();
		if (i >= list.size()) return false;
		Entry e = list.get(i);
		if (!SvenskaCraft.progress.isDiscovered(e.id())) return false;
		selected = e.id();
		if (!revealedEn.contains(e.id())) revealedEn.add(e.id());
		rebuildWidgets();
		return true;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		Milestones.Stats s = Milestones.stats(SvenskaCraft.progress, SvenskaCraft.vocab);
		Component title = Component.literal(SvenskaCraft.pack.ui("journal") + " · Vocabulary").withStyle(ChatFormatting.BOLD);
		g.text(font, title, 8, 8, 0xFFFFCC00);
		String head = SvenskaCraft.settings.level == Level.ADVANCED
			? s.discovered() + "/" + SvenskaCraft.vocab.entries().size() + " ord · " + s.mastered() + " kan"
			: s.discovered() + "/" + SvenskaCraft.vocab.entries().size() + " words · " + s.mastered() + " mastered";
		int hx = 8 + font.width(title) + 12;
		if (hx + font.width(head) < width - 182) g.text(font, Component.literal(head), hx, 8, 0xFFAAAAAA);
		if (tab == null) drawOverview(g, s);
		else drawList(g, mouseX, mouseY);
	}

	private void drawList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		Settings.Translations tr = SvenskaCraft.settings.translations;
		List<Entry> list = entries();
		int start = page * rowsPerPage();
		for (int i = start; i < Math.min(list.size(), start + rowsPerPage()); i++) {
			Entry e = list.get(i);
			int y = listTop() + (i - start) * ROW_H;
			WordProgress w = SvenskaCraft.progress.word(e.id());
			WordState st = Mastery.state(w);
			boolean hover = mouseX >= x0() && mouseX < x1() - 20 && mouseY >= y && mouseY < y + ROW_H;
			int bg = e.id().equals(selected) ? 0x60FFFFFF : hover ? 0x30FFFFFF : (i % 2 == 0 ? 0x30000000 : 0x18000000);
			g.fill(x0(), y, x1(), y + ROW_H - 1, bg);
			if (st == WordState.NEW) {
				if (Hints.showIcon(e.concept())) {
					QuizScreen.drawIcon(g, e, x0() + 2, y + 1, 1.0f);
					g.fill(x0() + 2, y + 1, x0() + 18, y + 17, 0xB0101010); // dimmed: still to find
				} else {
					g.text(font, Component.literal("?"), x0() + 7, y + 5, 0xFF666666);
				}
				g.text(font, Component.literal("???"), x0() + 22, y + 5, 0xFF777777);
				g.text(font, Component.literal(Hints.hint(e.concept())).withStyle(ChatFormatting.ITALIC), x0() + 60, y + 5, 0xFF666666);
				continue;
			}
			QuizScreen.drawIcon(g, e, x0() + 2, y + 1, 1.0f);
			g.text(font, Component.literal(e.word()).withStyle(ChatFormatting.BOLD), x0() + 22, y + 5, 0xFFFFAA00);
			String en = switch (tr) {
				case ALWAYS -> e.en();
				case ON_DEMAND -> revealedEn.contains(e.id()) ? e.en() : "· · ·";
				case NEVER -> "";
			};
			g.text(font, Component.literal(en), x0() + 120, y + 5, 0xFFCCCCCC);
			String badge = Journal.stateShort(st);
			g.text(font, Component.literal(badge), x1() - 22 - font.width(badge), y + 5, Journal.stateArgb(st));
		}
		if (pages() > 1) g.text(font, Component.literal("Sida " + (page + 1) + "/" + pages()), x0() + 26, height - 16, 0xFFAAAAAA);

		// detail panel
		int dy = detailTop();
		g.fill(x0(), dy, x1(), height - 24, 0x50000000);
		Entry sel = selectedEntry();
		if (sel == null) {
			g.text(font, Component.literal("Click a word to see an example sentence."), x0() + 6, dy + 6, 0xFF888888);
			return;
		}
		WordProgress w = SvenskaCraft.progress.word(sel.id());
		WordState st = Mastery.state(w);
		g.text(font, Component.literal(sel.word()).withStyle(ChatFormatting.BOLD), x0() + 6, dy + 6, 0xFFFFAA00);
		g.text(font, Component.literal(Journal.stateName(st)), x0() + 12 + font.width(sel.word()) + 6, dy + 6, Journal.stateArgb(st));
		int ty = dy + 20;
		for (FormattedCharSequence line : font.split(Component.literal("“" + sel.example() + "”"), x1() - x0() - 12).subList(0, 1)) {
			g.text(font, line, x0() + 6, ty, 0xFFFFFFFF);
			ty += 10;
		}
		if (SvenskaCraft.settings.translations != Settings.Translations.NEVER) {
			g.text(font, Component.literal(sel.exampleEn()).withStyle(ChatFormatting.ITALIC), x0() + 6, ty, 0xFF999999);
			ty += 10;
		}
		var lvl = SvenskaCraft.settings.level;
		String forms = formsLine(sel);
		if (lvl.atLeast(Level.INTERMEDIATE) && !forms.isEmpty()) {
			g.text(font, Component.literal(SvenskaCraft.pack.ui("forms") + ": ").withStyle(ChatFormatting.DARK_AQUA)
				.append(Component.literal(forms).withStyle(ChatFormatting.AQUA)), x0() + 6, ty, 0xFFFFFFFF);
			ty += 10;
		}
		if (lvl == Level.ADVANCED && sel.text().definition() != null) {
			g.text(font, Component.literal(SvenskaCraft.pack.ui("definition") + ": ").withStyle(ChatFormatting.DARK_AQUA)
				.append(Component.literal(sel.text().definition()).withStyle(ChatFormatting.GRAY)), x0() + 6, ty, 0xFFFFFFFF);
			ty += 10;
		}
		long dueIn = Mastery.dueAt(w) - System.currentTimeMillis();
		boolean sv = SvenskaCraft.settings.level == Level.ADVANCED;
		String when = dueIn <= 0 ? (sv ? "nu" : "now") : human(dueIn);
		String stats = "Rätt " + w.correct + " · Fel " + w.wrong + (sv ? " · Nästa repetition: " : " · Next review: ") + when;
		g.text(font, Component.literal(stats), x0() + 6, ty + 2, 0xFF888888);
	}

	private void drawOverview(GuiGraphicsExtractor g, Milestones.Stats s) {
		int x = x0(), y = listTop();
		var p = SvenskaCraft.progress;
		g.text(font, Component.literal("Discovered " + s.discovered() + " · Familiar " + s.familiarOrBetter() + " · Mastered " + s.mastered()), x, y, 0xFFFFFFFF);
		y += 11;
		g.text(font, Component.literal("Points " + p.points + " · Best streak " + p.bestStreak + " · Shields " + p.shields + "/3"), x, y, 0xFFAAAAAA);
		y += 11;
		int goal = SvenskaCraft.settings.dailyGoal;
		if (goal > 0) {
			boolean today = p.today == java.time.LocalDate.now().toEpochDay();
			int done = today ? p.answeredToday : 0;
			String streak = p.dayStreak > 1 ? " · " + p.dayStreak + " " + SvenskaCraft.pack.ui("daysInARow") : "";
			g.text(font, Component.literal(SvenskaCraft.pack.ui("dailyGoal") + " (today): " + Math.min(done, goal) + "/" + goal + streak),
				x, y, done >= goal ? 0xFF55FF55 : 0xFFFFDD55);
			y += 11;
		}
		// today's quests
		y += 2;
		g.text(font, Component.literal(SvenskaCraft.pack.ui("quests") + " (today's quests)"), x, y, 0xFFFFCC00);
		y += 11;
		for (svenskacraft.learning.Quests.Quest q : p.quests) {
			String text = q.describe(q.category == null ? "" : SvenskaCraft.pack.category(q.category));
			g.text(font, Component.literal((q.done ? "✔ " : "○ ") + text + "  " + q.progress + "/" + q.target), x + 4, y, q.done ? 0xFF55FF55 : 0xFFDDDDDD);
			y += 10;
		}
		if (p.quests.isEmpty()) { g.text(font, Component.literal("Join a world to get today's quests."), x + 4, y, 0xFF888888); y += 10; }
		y += 4;
		// categories in two columns
		int colW = (x1() - x) / 2 - 4;
		int labelW = 0;
		for (Category c : SvenskaCraft.vocab.categories()) labelW = Math.max(labelW, font.width(SvenskaCraft.pack.category(c.id())));
		List<Category> cats = SvenskaCraft.vocab.categories();
		int rows = (cats.size() + 1) / 2;
		for (int i = 0; i < cats.size(); i++) {
			Category c = cats.get(i);
			int cx0 = x + (i / rows) * (colW + 8), cy = y + (i % rows) * 10;
			List<Entry> list = SvenskaCraft.vocab.inCategory(c.id());
			int disc = 0, mast = 0;
			for (Entry e : list) {
				WordState st = Mastery.state(p.word(e.id()));
				if (st != WordState.NEW) disc++;
				if (st == WordState.MASTERED) mast++;
			}
			int barX = cx0 + labelW + 4, barW = Math.max(20, colW - labelW - 34);
			g.text(font, Component.literal(SvenskaCraft.pack.category(c.id())), cx0, cy, 0xFFDDDDDD);
			g.fill(barX, cy + 1, barX + barW, cy + 7, 0xFF333333);
			g.fill(barX, cy + 1, barX + barW * disc / list.size(), cy + 7, 0xFF3A7BD5);
			g.fill(barX, cy + 1, barX + barW * mast / list.size(), cy + 7, 0xFF55DD55);
			g.text(font, Component.literal(disc + "/" + list.size()), barX + barW + 3, cy, 0xFFAAAAAA);
		}
		y += rows * 10;
		y += 4;
		List<Milestones.Milestone> all = Milestones.all(SvenskaCraft.vocab);
		long done = all.stream().filter(m -> p.milestones.contains(m.id())).count();
		g.text(font, Component.literal("Milestones " + done + "/" + all.size() + "   (blue = discovered, green = mastered)"), x, y, 0xFFFFCC00);
		y += 11;
		for (Milestones.Milestone m : all) {
			if (y > height - 12) break;
			if (p.milestones.contains(m.id())) continue;
			g.text(font, Component.literal("Next: " + m.title() + " – " + m.description() + (m.shield() ? "  (+shield)" : "")), x, y, 0xFF999999);
			break;
		}
	}

	private static String human(long ms) {
		long min = ms / 60_000;
		if (min < 1) return "< 1 min";
		if (min < 60) return min + " min";
		long h = min / 60;
		if (h < 24) return h + " h";
		return (h / 24) + " days";
	}

	@Override
	public boolean isPauseScreen() { return true; } // singleplayer: the world waits while you read or answer
}

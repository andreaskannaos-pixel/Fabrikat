package svenskacraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public class QuizScreen extends Screen {
	private final Word answer;
	private final List<Word> options;
	private final int correctIndex;
	/** true: Swedish word shown, pick English. false: item + English shown, pick Swedish. */
	private final boolean svToEn;
	private final ItemStack icon;
	private final Button[] buttons = new Button[4];
	private final int limitTicks;

	private int ticks = 0;
	private int chosen = -2; // -2 = not answered yet, -1 = timed out
	private int closeAt = -1;
	private Component resultLine = Component.empty();
	private Component penaltyLine = Component.empty();

	private QuizScreen(Word answer, List<Word> options, boolean svToEn) {
		super(Component.literal("Svenska-quiz"));
		this.answer = answer;
		this.options = options;
		this.correctIndex = options.indexOf(answer);
		this.svToEn = svToEn;
		this.icon = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(answer.items().get(0))));
		this.limitTicks = Math.max(5, SvenskaCraft.config.answerSeconds) * 20;
	}

	public static QuizScreen random(RandomSource rnd) {
		List<Word> pool = new ArrayList<>();
		for (String id : SvenskaCraft.config.learned) {
			Word w = Words.byId(id);
			if (w != null) pool.add(w);
		}
		if (pool.size() < 4) pool = Words.ALL;
		Word answer = pool.get(rnd.nextInt(pool.size()));
		boolean svToEn = rnd.nextBoolean();

		List<Word> others = new ArrayList<>(Words.ALL);
		others.remove(answer);
		Collections.shuffle(others, new java.util.Random(rnd.nextLong()));
		List<Word> options = new ArrayList<>();
		options.add(answer);
		for (Word w : others) {
			if (options.size() == 4) break;
			String text = svToEn ? w.en() : w.sv();
			if (options.stream().noneMatch(o -> (svToEn ? o.en() : o.sv()).equals(text))) options.add(w);
		}
		Collections.shuffle(options, new java.util.Random(rnd.nextLong()));
		return new QuizScreen(answer, options, svToEn);
	}

	private String label(Word w) { return svToEn ? w.en() : w.sv(); }

	@Override
	protected void init() {
		int cx = width / 2;
		int top = height / 2 - 95;
		for (int i = 0; i < 4; i++) {
			final int idx = i;
			int x = (i % 2 == 0) ? cx - 152 : cx + 2;
			int y = top + 92 + (i / 2) * 24;
			buttons[i] = addRenderableWidget(Button.builder(buttonText(i), b -> answer(idx)).bounds(x, y, 150, 20).build());
		}
		if (svToEn) {
			addRenderableWidget(Button.builder(Component.literal("♪ Hör igen (listen)"), b -> Voice.play("w." + answer.id()))
				.bounds(cx - 60, top + 142, 120, 20).build());
		}
		if (ticks == 0) Voice.play("fx.quiz");
	}

	private Component buttonText(int i) {
		String text = label(options.get(i));
		if (chosen == -2) return Component.literal(text);
		if (i == correctIndex) return Component.literal("✔ " + text).withStyle(ChatFormatting.GREEN);
		if (i == chosen) return Component.literal("✘ " + text).withStyle(ChatFormatting.RED);
		return Component.literal(text).withStyle(ChatFormatting.DARK_GRAY);
	}

	private void answer(int idx) {
		if (chosen != -2) return;
		chosen = idx;
		Config cfg = SvenskaCraft.config;
		Component pair = Component.literal(answer.sv()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
			.append(Component.literal(" = " + answer.en()).withStyle(ChatFormatting.GRAY));
		if (idx == correctIndex) {
			cfg.points++;
			cfg.streak++;
			Voice.play("fx.right");
			resultLine = Component.literal("RÄTT! (Correct!)  ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD).append(pair);
			SvenskaCraft.say(Component.literal("✔ Rätt! ").withStyle(ChatFormatting.GREEN).append(pair)
				.append(Component.literal("  (svit: " + cfg.streak + ")").withStyle(ChatFormatting.DARK_GRAY)));
		} else {
			cfg.streak = 0;
			if (idx == -1) Voice.play("fx.timeout");
			Voice.play("fb." + answer.id());
			resultLine = Component.literal(idx == -1 ? "TIDEN ÄR UTE! (Time's up!)  " : "FEL! (Wrong!)  ")
				.withStyle(ChatFormatting.RED, ChatFormatting.BOLD).append(pair);
			SvenskaCraft.say(Component.literal("✘ Fel! Rätt svar: ").withStyle(ChatFormatting.RED).append(pair));
			if (cfg.penalty && minecraft != null) {
				penaltyLine = Penalty.apply(minecraft);
				SvenskaCraft.say(penaltyLine);
			}
		}
		cfg.save();
		for (int i = 0; i < 4; i++) buttons[i].setMessage(buttonText(i));
		closeAt = ticks + 60;
	}

	@Override
	public void tick() {
		ticks++;
		if (svToEn && ticks == 35 && chosen == -2) Voice.play("w." + answer.id());
		if (chosen == -2 && ticks >= limitTicks) answer(-1);
		if (closeAt >= 0 && ticks >= closeAt) onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		int cx = width / 2;
		int top = height / 2 - 95;
		g.centeredText(font, Component.literal("★ SVENSKA-QUIZ! ★").withStyle(ChatFormatting.BOLD), cx, top, 0xFFFFCC00);
		if (svToEn) {
			g.centeredText(font, Component.literal("Vad betyder…"), cx, top + 18, 0xFFFFFFFF);
			g.pose().pushMatrix();
			g.pose().translate(cx, top + 32);
			g.pose().scale(2.0f, 2.0f);
			g.centeredText(font, Component.literal(answer.sv()).withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFAA00);
			g.pose().popMatrix();
			g.centeredText(font, Component.literal("(What does it mean?)"), cx, top + 56, 0xFFAAAAAA);
		} else {
			g.pose().pushMatrix();
			g.pose().translate(cx - 16, top + 14);
			g.pose().scale(2.0f, 2.0f);
			g.item(icon, 0, 0);
			g.pose().popMatrix();
			g.centeredText(font, Component.literal("Hur säger man \"" + answer.en() + "\" på svenska?"), cx, top + 52, 0xFFFFFFFF);
			g.centeredText(font, Component.literal("(How do you say it in Swedish?)"), cx, top + 64, 0xFFAAAAAA);
		}

		// Time bar
		int barW = 304;
		int x0 = cx - barW / 2;
		int y0 = top + 80;
		float left = chosen == -2 ? Math.max(0f, 1f - (ticks + partialTick) / limitTicks) : 0f;
		g.fill(x0, y0, x0 + barW, y0 + 5, 0xFF333333);
		int color = left > 0.5f ? 0xFF55FF55 : left > 0.2f ? 0xFFFFDD33 : 0xFFFF5555;
		g.fill(x0, y0, x0 + (int) (barW * left), y0 + 5, color);

		int infoY = top + 170;
		if (chosen == -2) {
			if (SvenskaCraft.config.penalty) {
				g.centeredText(font, Component.literal("Fel svar = du förlorar ett föremål! (Wrong = you lose an item!)"), cx, infoY, 0xFFFF6666);
			}
		} else {
			g.centeredText(font, resultLine, cx, infoY, 0xFFFFFFFF);
			g.centeredText(font, penaltyLine, cx, infoY + 13, 0xFFFFFFFF);
		}
	}

	@Override
	public boolean shouldCloseOnEsc() { return false; }

	@Override
	public boolean isPauseScreen() { return false; }
}

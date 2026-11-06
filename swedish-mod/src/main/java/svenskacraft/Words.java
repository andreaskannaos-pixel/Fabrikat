package svenskacraft;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class Words {
	public static final List<Word> ALL;
	private static final Map<String, Word> BY_ITEM = new HashMap<>();
	private static final Map<String, Word> BY_ID = new HashMap<>();

	static {
		try (var in = Words.class.getResourceAsStream("/assets/svenskacraft/words.json")) {
			List<Word> list = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), new TypeToken<List<Word>>() {}.getType());
			ALL = List.copyOf(list);
		} catch (Exception e) {
			throw new RuntimeException("SvenskaCraft: could not load words.json", e);
		}
		for (Word w : ALL) {
			BY_ID.put(w.id(), w);
			for (String item : w.items()) BY_ITEM.put("minecraft:" + item, w);
		}
	}

	private Words() {}

	public static Word byItem(String itemId) { return BY_ITEM.get(itemId); }

	public static Word byId(String id) { return BY_ID.get(id); }
}

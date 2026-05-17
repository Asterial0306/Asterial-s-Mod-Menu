package asterial.mod.menu.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ModMenuConfig {
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mod-menu.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ModMenuConfig INSTANCE = new ModMenuConfig();

	public String defaultSort = "NAME_ASC";
	public boolean showLibraries = false;
	public boolean showSearchSuggestions = true;
	public boolean rememberState = false;
	public String entryDensity = "normal";
	public boolean hideFabricAuto = false;
	public boolean showAuthor = false;
	public boolean enableHotkey = true;
	public boolean showSearchHistory = true;
	public boolean enableBookmarks = true;

	public String lastSort = "";
	public String lastFilter = "";

	private Set<String> bookmarkedMods = new HashSet<>();
	private List<String> searchHistory = new ArrayList<>();

	public static ModMenuConfig getInstance() {
		return INSTANCE;
	}

	public Set<String> getBookmarkedMods() {
		return bookmarkedMods != null ? bookmarkedMods : (bookmarkedMods = new HashSet<>());
	}

	public List<String> getSearchHistory() {
		return searchHistory != null ? searchHistory : (searchHistory = new ArrayList<>());
	}

	public boolean isBookmarked(String modId) {
		return bookmarkedMods.contains(modId);
	}

	public void toggleBookmark(String modId) {
		if (bookmarkedMods.contains(modId)) {
			bookmarkedMods.remove(modId);
		} else {
			bookmarkedMods.add(modId);
		}
	}

	public void addSearchHistory(String query) {
		if (query == null || query.trim().isEmpty()) return;
		query = query.trim();
		searchHistory.remove(query);
		searchHistory.add(0, query);
		if (searchHistory.size() > 10) {
			searchHistory = new ArrayList<>(searchHistory.subList(0, 10));
		}
	}

	public void clearSearchHistory() {
		searchHistory.clear();
	}

	public int getEntryHeight() {
		return switch (entryDensity) {
			case "compact" -> 28;
			case "spacious" -> 44;
			default -> 36;
		};
	}

	public int getIconSize() {
		return switch (entryDensity) {
			case "compact" -> 16;
			case "spacious" -> 24;
			default -> 20;
		};
	}

	public static void load() {
		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				ModMenuConfig loaded = GSON.fromJson(reader, ModMenuConfig.class);
				if (loaded != null) {
					INSTANCE = loaded;
				}
			} catch (Exception e) {
				INSTANCE = new ModMenuConfig();
			}
		}
		if (INSTANCE == null) {
			INSTANCE = new ModMenuConfig();
		}
		if (INSTANCE.bookmarkedMods == null) {
			INSTANCE.bookmarkedMods = new HashSet<>();
		}
		if (INSTANCE.searchHistory == null) {
			INSTANCE.searchHistory = new ArrayList<>();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(INSTANCE, writer);
			}
		} catch (IOException ignored) {}
	}
}
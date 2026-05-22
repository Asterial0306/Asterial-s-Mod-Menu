package asterial.mod.menu.client.screen;

import asterial.mod.menu.client.config.ModMenuApiBridge;
import asterial.mod.menu.config.ModConfigProvider;
import asterial.mod.menu.config.ModMenuConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public class ModMenuScreen extends Screen {

	private List<ModEntryData> modEntries = new ArrayList<>();
	private List<ModEntryData> visibleEntries = new ArrayList<>();
	private double scrollAmount = 0;
	private boolean draggingScrollbar = false;
	private double dragStartY = 0;
	private double dragStartScroll = 0;
	private int listTop;
	private int listBottom;
	private int listLeft;
	private int listWidth;
	private TextFieldWidget searchField;
	private ButtonWidget sortButton;
	private ButtonWidget filterButton;
	private String searchQuery = "";
	private SortMode currentSort = SortMode.NAME_ASC;
	private FilterMode currentFilter = FilterMode.ALL;
	private static final int SCROLLBAR_WIDTH = 4;
	private static final int MAX_SUGGESTIONS = 6;
	private static final int SUGGESTION_HEIGHT = 12;

	private int entryHeight = 36;
	private int iconSize = 20;

	private List<String> suggestions = new ArrayList<>();
	private int selectedSuggestionIndex = -1;
	private boolean showSuggestions = false;
	private Map<String, ModConfigProvider> configProviders = new HashMap<>();

	private Set<String> bookmarkedMods = new HashSet<>();

	private enum SortMode {
		NAME_ASC, NAME_DESC, VERSION;

		SortMode next() {
			SortMode[] values = values();
			return values[(ordinal() + 1) % values.length];
		}

		Text displayName() {
			return Text.translatable("mod-menu.sort." + name().toLowerCase());
		}
	}

	private enum FilterMode {
		ALL, MODS_ONLY, LIBRARIES, BOOKMARKS;

		FilterMode next() {
			FilterMode[] values = values();
			return values[(ordinal() + 1) % values.length];
		}

		Text displayName() {
			return Text.translatable("mod-menu.filter." + name().toLowerCase());
		}
	}

	private static final Set<String> LIBRARY_IDS = Set.of(
		"minecraft", "java", "fabricloader", "fabric-api", "fabricapi"
	);

	private final Screen parent;

	protected ModMenuScreen(Text title, Screen parent) {
		super(title);
		this.parent = parent;
	}

	public static ModMenuScreen create(Screen parent) {
		return new ModMenuScreen(Text.translatable("mod-menu.view_mods"), parent);
	}

	@Override
	protected void init() {
		super.init();

		ModMenuConfig.load();
		ModMenuConfig config = ModMenuConfig.getInstance();

		this.entryHeight = config.getEntryHeight();
		this.iconSize = config.getIconSize();

		this.bookmarkedMods = new HashSet<>(config.getBookmarkedMods());

		this.configProviders.clear();
		for (ModConfigProvider provider : FabricLoader.getInstance()
				.getEntrypoints("mod-menu", ModConfigProvider.class)) {
			this.configProviders.put(provider.getModId(), provider);
		}

		for (ModConfigProvider provider : ModMenuApiBridge.discover()) {
			this.configProviders.putIfAbsent(provider.getModId(), provider);
		}

		try {
			this.currentSort = SortMode.valueOf(config.defaultSort);
		} catch (IllegalArgumentException e) {
			this.currentSort = SortMode.NAME_ASC;
		}

		if (config.rememberState && !config.lastSort.isEmpty()) {
			try { this.currentSort = SortMode.valueOf(config.lastSort); } catch (Exception ignored) {}
		}
		if (config.rememberState && !config.lastFilter.isEmpty()) {
			try { this.currentFilter = FilterMode.valueOf(config.lastFilter); } catch (Exception ignored) {}
		}

		if (config.showLibraries) {
			this.currentFilter = FilterMode.ALL;
		}

		ButtonWidget backButton = ButtonWidget.builder(
				Text.literal("←"),
				btn -> {
					if (this.client != null) {
						this.client.setScreen(this.parent);
					}
				}
			)
			.dimensions(4, 4, 20, 20)
			.build();
		this.addDrawableChild(backButton);

		ButtonWidget folderButton = ButtonWidget.builder(
				Text.literal("📂"),
				btn -> openModsFolder()
			)
			.dimensions(26, 4, 20, 20)
			.build();
		this.addDrawableChild(folderButton);

		this.listWidth = Math.min(this.width, 340);
		this.listLeft = (this.width - this.listWidth) / 2;

		int rightButtonsWidth = 58 + 4 + 58;
		int searchWidth = this.listWidth - 24 - rightButtonsWidth;

		this.searchField = new TextFieldWidget(
			this.textRenderer,
			this.listLeft + 12,
			28,
			searchWidth,
			16,
			Text.translatable("mod-menu.search_hint")
		);
		this.searchField.setChangedListener(query -> {
			this.searchQuery = query.toLowerCase().trim();
			updateSuggestions();
			applyFilters();
			this.scrollAmount = 0;
		});
		this.addDrawableChild(this.searchField);

		int sortX = this.listLeft + 12 + searchWidth + 4;
		int filterX = sortX + 62;

		this.sortButton = ButtonWidget.builder(
				this.currentSort.displayName(),
				btn -> {
					this.currentSort = this.currentSort.next();
					this.sortButton.setMessage(this.currentSort.displayName());
					applyFilters();
					this.scrollAmount = 0;
				}
			)
			.dimensions(sortX, 28, 58, 16)
			.build();
		this.addDrawableChild(this.sortButton);

		this.filterButton = ButtonWidget.builder(
				this.currentFilter.displayName(),
				btn -> {
					this.currentFilter = this.currentFilter.next();
					if (config.showLibraries) {
						this.currentFilter = FilterMode.ALL;
					}
					this.filterButton.setMessage(this.currentFilter.displayName());
					applyFilters();
					this.scrollAmount = 0;
				}
			)
			.dimensions(filterX, 28, 58, 16)
			.build();
		this.addDrawableChild(this.filterButton);

		this.listTop = 52;
		this.listBottom = this.height - 4;

		this.modEntries.clear();
		Collection<ModContainer> allMods = FabricLoader.getInstance().getAllMods();
		List<ModContainer> sortedMods = allMods.stream()
			.filter(mod -> {
				String id = mod.getMetadata().getId();
				if (id.equals("minecraft") || id.equals("fabricloader") || id.equals("fabric-api") || id.equals("fabricapi")) {
					return true;
				}
				if (config.hideFabricAuto && id.startsWith("fabric-")) {
					return false;
				}
				return true;
			})
			.toList();

		for (ModContainer mod : sortedMods) {
			String id = mod.getMetadata().getId();
			String name = mod.getMetadata().getName();
			String version = mod.getMetadata().getVersion().getFriendlyString();
			String description = mod.getMetadata().getDescription();
			String iconPath = mod.getMetadata().getIconPath(Integer.SIZE).orElse(null);
			BufferedImage iconImage = loadIcon(mod, iconPath);
			int color = hashToColor(id);
			boolean isLibrary = LIBRARY_IDS.contains(id);
			String author = "";
			var authors = mod.getMetadata().getAuthors();
			if (!authors.isEmpty()) {
				author = authors.iterator().next().getName();
			}
			String homepage = "";
			try {
				homepage = mod.getMetadata().getContact().asMap().getOrDefault("homepage", "");
			} catch (Exception ignored) {}
			String license = "";
			try {
				var licenses = mod.getMetadata().getLicense();
				if (!licenses.isEmpty()) {
					license = String.join(", ", licenses);
				}
			} catch (Exception ignored) {}
			String environment = mod.getMetadata().getEnvironment().toString();
			this.modEntries.add(new ModEntryData(id, name, version, description != null ? description : "",
					iconImage, color, isLibrary, author, homepage, license, environment));
		}

		applyFilters();
		this.scrollAmount = 0;
	}

	private void openModsFolder() {
		try {
			Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
			if (Files.isDirectory(modsDir)) {
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().open(modsDir.toFile());
				} else {
					Runtime.getRuntime().exec(new String[]{"explorer", modsDir.toAbsolutePath().toString()});
				}
			}
		} catch (Exception ignored) {}
	}

	private void updateSuggestions() {
		ModMenuConfig config = ModMenuConfig.getInstance();
		if (this.searchQuery.isEmpty()) {
			this.suggestions.clear();
			this.selectedSuggestionIndex = -1;
			this.showSuggestions = false;
		} else if (!config.showSearchSuggestions) {
			this.suggestions.clear();
			this.selectedSuggestionIndex = -1;
			this.showSuggestions = false;
		} else {
			this.suggestions = this.modEntries.stream()
				.map(e -> e.name)
				.filter(name -> name.toLowerCase().contains(this.searchQuery))
				.distinct()
				.limit(MAX_SUGGESTIONS)
				.collect(java.util.stream.Collectors.toList());
			this.selectedSuggestionIndex = this.suggestions.isEmpty() ? -1 : 0;
			this.showSuggestions = !this.suggestions.isEmpty();
		}
	}

	private void selectSuggestion(int index) {
		if (index >= 0 && index < this.suggestions.size()) {
			String selected = this.suggestions.get(index);
			this.searchField.setText(selected);
			this.searchField.setCursorToEnd(false);
			this.showSuggestions = false;
			this.searchQuery = selected.toLowerCase().trim();
			ModMenuConfig.getInstance().addSearchHistory(selected.trim());
			applyFilters();
		}
	}

	private void selectSearchHistory(int index) {
		ModMenuConfig config = ModMenuConfig.getInstance();
		List<String> history = config.getSearchHistory();
		if (index >= 0 && index < history.size()) {
			String selected = history.get(index);
			this.searchField.setText(selected);
			this.searchField.setCursorToEnd(false);
			this.searchQuery = selected.toLowerCase().trim();
			this.showSuggestions = false;
			applyFilters();
		}
	}

	private void commitSearchHistory() {
		if (!this.searchQuery.isEmpty()) {
			ModMenuConfig.getInstance().addSearchHistory(this.searchQuery);
		}
	}

	private void applyFilters() {
		List<ModEntryData> result = new ArrayList<>(this.modEntries);

		if (this.currentFilter == FilterMode.MODS_ONLY) {
			result.removeIf(e -> e.isLibrary);
		} else if (this.currentFilter == FilterMode.LIBRARIES) {
			result.removeIf(e -> !e.isLibrary);
		} else if (this.currentFilter == FilterMode.BOOKMARKS) {
			result.removeIf(e -> !this.bookmarkedMods.contains(e.modId));
		}

		if (!this.searchQuery.isEmpty()) {
			result.removeIf(e ->
				!e.name.toLowerCase().contains(this.searchQuery)
				&& !e.description.toLowerCase().contains(this.searchQuery)
			);
		}

		switch (this.currentSort) {
			case NAME_ASC -> result.sort(Comparator.comparing(e -> e.name));
			case NAME_DESC -> result.sort(Comparator.comparing(e -> e.name, Comparator.reverseOrder()));
			case VERSION -> result.sort(Comparator.comparing(e -> e.version));
		}

		this.visibleEntries = result;
	}

	private BufferedImage loadIcon(ModContainer mod, String iconPath) {
		if (iconPath == null || iconPath.isEmpty()) return null;
		try {
			var resource = mod.findPath(iconPath.replace("\\", "/"));
			if (resource.isPresent()) {
				try (InputStream input = Files.newInputStream(resource.get())) {
					return ImageIO.read(input);
				}
			}
		} catch (Exception ignored) {}
		return null;
	}

	private static int hashToColor(String id) {
		int hash = id.hashCode();
		int r = ((hash >> 16) & 0xFF) % 128 + 80;
		int g = ((hash >> 8) & 0xFF) % 128 + 80;
		int b = (hash & 0xFF) % 128 + 80;
		return (255 << 24) | (r << 16) | (g << 8) | b;
	}

	private boolean isSearchHistoryVisible() {
		return ModMenuConfig.getInstance().showSearchHistory
			&& this.searchQuery.isEmpty()
			&& this.searchField.isFocused();
	}

	private void drawSuggestions(DrawContext context, int mouseX, int mouseY) {
		List<String> items;
		if (isSearchHistoryVisible()) {
			items = ModMenuConfig.getInstance().getSearchHistory();
		} else if (this.showSuggestions) {
			items = this.suggestions;
		} else {
			return;
		}
		if (items.isEmpty()) return;

		int sx = this.listLeft + 12;
		int sy = 28 + 16 + 2;
		int sw = this.searchField.getWidth();
		int totalHeight = items.size() * SUGGESTION_HEIGHT;
		boolean isHistory = isSearchHistoryVisible();

		context.fill(sx, sy, sx + sw, sy + totalHeight, 0xFF1A1A1A);

		for (int i = 0; i < items.size(); i++) {
			String item = items.get(i);
			int itemY = sy + i * SUGGESTION_HEIGHT;

			if (!isHistory && i == this.selectedSuggestionIndex) {
				context.fill(sx, itemY, sx + sw, itemY + SUGGESTION_HEIGHT, 0xFFCCCCCC);
			}

			if (mouseX >= sx && mouseX <= sx + sw
					&& mouseY >= itemY && mouseY <= itemY + SUGGESTION_HEIGHT) {
				if (!isHistory) this.selectedSuggestionIndex = i;
				context.fill(sx, itemY, sx + sw, itemY + SUGGESTION_HEIGHT, 0xFF888888);
			}

			context.drawTextWithShadow(this.textRenderer,
				Text.literal(item), sx + 4, itemY + 2,
				isHistory ? 0xFFAAAAAA : 0xFFFFFF);
		}

		}

	private void toggleBookmark(String modId) {
		ModMenuConfig config = ModMenuConfig.getInstance();
		config.toggleBookmark(modId);
		if (config.isBookmarked(modId)) {
			this.bookmarkedMods.add(modId);
		} else {
			this.bookmarkedMods.remove(modId);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.drawCenteredTextWithShadow(
			this.textRenderer,
			Text.translatable("mod-menu.mods_title"),
			this.width / 2,
			14,
			0xFFFFFF
		);

		int visibleHeight = this.listBottom - this.listTop;
		int totalHeight = this.visibleEntries.size() * this.entryHeight;
		double maxScroll = Math.max(0, totalHeight - visibleHeight);
		this.scrollAmount = Math.max(0, Math.min(this.scrollAmount, maxScroll));

		context.enableScissor(this.listLeft, this.listTop, this.listLeft + this.listWidth, this.listBottom);

		for (int i = 0; i < this.visibleEntries.size(); i++) {
			int entryY = this.listTop + i * this.entryHeight - (int) this.scrollAmount;
			if (entryY + this.entryHeight < this.listTop || entryY > this.listBottom) continue;

			ModEntryData entry = this.visibleEntries.get(i);
			int iconX = this.listLeft + 4;

			int textOffset = this.entryHeight >= 40 ? 36 : 28;
			int textX = this.listLeft + textOffset;
			int maxTextWidth = this.listWidth - 64 - (textOffset - 28);

			int lineH = 10;
			int gap = 2;
			boolean showAuthor = ModMenuConfig.getInstance().showAuthor && !entry.author.isEmpty() && this.entryHeight >= 40;
			boolean showDesc = this.entryHeight >= 32;

			int visibleLines = 1;
			if (showDesc) visibleLines++;
			if (showAuthor) visibleLines++;

			int textBlockH = visibleLines * lineH + (visibleLines - 1) * gap;

			int contentH = Math.max(this.iconSize, textBlockH);
			int contentBaseY = entryY + (this.entryHeight - contentH) / 2;

			int iconY = contentBaseY + (contentH - this.iconSize) / 2;

			if (entry.iconImage != null) {
				drawBufferedImage(context, entry.iconImage, iconX, iconY, this.iconSize, this.iconSize);
			} else {
				context.fill(iconX, iconY, iconX + this.iconSize, iconY + this.iconSize, entry.color);
				String initial = entry.name.substring(0, 1).toUpperCase();
				int textWidth = this.textRenderer.getWidth(initial);
				context.drawTextWithShadow(
					this.textRenderer,
					Text.literal(initial),
					iconX + (this.iconSize - textWidth) / 2,
					iconY + (this.iconSize - 8) / 2,
					0xFFFFFF
				);
			}

			context.drawTextWithShadow(this.textRenderer, Text.literal(entry.name), textX, contentBaseY, 0xFFFFFF);

			if (showDesc) {
				if (!entry.description.isEmpty()) {
					String desc = truncateText(entry.description, this.textRenderer, maxTextWidth);
					context.drawTextWithShadow(this.textRenderer, Text.literal(desc), textX, contentBaseY + lineH + gap, 0xAAAAAA);
				} else {
					context.drawTextWithShadow(this.textRenderer, Text.literal("v" + entry.version), textX, contentBaseY + lineH + gap, 0x888888);
				}
			}

			if (showAuthor) {
				context.drawTextWithShadow(this.textRenderer,
					Text.translatable("mod-menu.detail.author").append(Text.literal(entry.author)),
					textX, contentBaseY + (lineH + gap) * 2, 0x666666);
			}

			int starSize = 10;
			int starX = this.listLeft + this.listWidth - 30;
			int starY = entryY + (this.entryHeight - starSize) / 2;
			boolean isBookmarked = this.bookmarkedMods.contains(entry.modId);
			boolean starHovered = ModMenuConfig.getInstance().enableBookmarks
				&& mouseX >= starX - 2 && mouseX <= starX + starSize + 2
				&& mouseY >= starY - 2 && mouseY <= starY + starSize + 2;

			if (ModMenuConfig.getInstance().enableBookmarks) {
				context.drawTextWithShadow(this.textRenderer,
					Text.literal(isBookmarked ? "★" : "☆"),
					starX, starY,
					starHovered ? 0xFFE0B040 : (isBookmarked ? 0xFFD0A030 : 0xFF666666));
			}

			int detailX = this.listLeft + this.listWidth - 18;
			int detailY = entryY + (this.entryHeight - 10) / 2;
			boolean hovered = mouseX >= detailX && mouseX <= detailX + 12
					&& mouseY >= detailY && mouseY <= detailY + 12;
			context.drawTextWithShadow(this.textRenderer,
				Text.literal(">"),
				detailX, detailY,
				hovered ? 0xFFFFFF : 0x888888);
		}

		context.disableScissor();

		if (totalHeight > visibleHeight) {
			int barX = this.listLeft + this.listWidth - 6;
			int scrollbarHeight = getScrollbarHeight(visibleHeight, totalHeight);
			int scrollbarY = this.listTop + (int) (this.scrollAmount * (visibleHeight - scrollbarHeight) / maxScroll);

			int barColor = this.draggingScrollbar || isOnScrollbar(mouseX, mouseY)
				? 0xFFCCCCCC : 0xFFAAAAAA;

			context.fill(barX, scrollbarY, barX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, barColor);
		}

		drawSuggestions(context, mouseX, mouseY);
	}

	private void drawBufferedImage(DrawContext context, BufferedImage image, int x, int y, int width, int height) {
		int imgW = image.getWidth();
		int imgH = image.getHeight();
		if (imgW <= 0 || imgH <= 0) return;

		float scaleX = (float) width / imgW;
		float scaleY = (float) height / imgH;

		for (int py = 0; py < height; py++) {
			for (int px = 0; px < width; px++) {
				int srcX = (int) (px / scaleX);
				int srcY = (int) (py / scaleY);
				if (srcX >= imgW || srcY >= imgH) continue;
				int argb = image.getRGB(srcX, srcY);
				int alpha = (argb >> 24) & 0xFF;
				if (alpha == 0) continue;
				context.fill(x + px, y + py, x + px + 1, y + py + 1, argb | 0xFF000000);
			}
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (mouseX >= this.listLeft && mouseX <= this.listLeft + this.listWidth
				&& mouseY >= this.listTop && mouseY <= this.listBottom) {
			this.scrollAmount -= verticalAmount * 10;
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isSearchHistoryVisible() && button == 0) {
			List<String> history = ModMenuConfig.getInstance().getSearchHistory();
			int sx = this.listLeft + 12;
			int sy = 28 + 16 + 2;
			int sw = this.searchField.getWidth();
			int totalHeight = history.size() * SUGGESTION_HEIGHT;

			if (mouseX >= sx && mouseX <= sx + sw
					&& mouseY >= sy && mouseY <= sy + totalHeight) {
				int clickedIndex = (int) ((mouseY - sy) / SUGGESTION_HEIGHT);
				if (clickedIndex >= 0 && clickedIndex < history.size()) {
					selectSearchHistory(clickedIndex);
					return true;
				}
			}
		}

		if (this.showSuggestions && button == 0) {
			int sx = this.listLeft + 12;
			int sy = 28 + 16 + 2;
			int sw = this.searchField.getWidth();
			int totalHeight = this.suggestions.size() * SUGGESTION_HEIGHT;

			if (mouseX >= sx && mouseX <= sx + sw
					&& mouseY >= sy && mouseY <= sy + totalHeight) {
				int clickedIndex = (int) ((mouseY - sy) / SUGGESTION_HEIGHT);
				if (clickedIndex >= 0 && clickedIndex < this.suggestions.size()) {
					selectSuggestion(clickedIndex);
					return true;
				}
			}

			this.showSuggestions = false;
		}

		if (isOnScrollbar(mouseX, mouseY) && button == 0) {
			int visibleHeight = this.listBottom - this.listTop;
			int totalHeight = this.visibleEntries.size() * this.entryHeight;
			double maxScroll = Math.max(0, totalHeight - visibleHeight);
			if (maxScroll > 0) {
				this.draggingScrollbar = true;
				this.dragStartY = mouseY;
				this.dragStartScroll = this.scrollAmount;
				return true;
			}
		}
		if (isInsideList(mouseX, mouseY)) {
			if (this.searchField.isFocused()) {
				this.searchField.setFocused(false);
			}

			int starX = this.listLeft + this.listWidth - 30;
			if (ModMenuConfig.getInstance().enableBookmarks && button == 0) {
				for (int i = 0; i < this.visibleEntries.size(); i++) {
					int entryY = this.listTop + i * this.entryHeight - (int) this.scrollAmount;
					if (entryY + this.entryHeight < this.listTop || entryY > this.listBottom) continue;
					int starY = entryY + (this.entryHeight - 10) / 2;
					if (mouseX >= starX - 2 && mouseX <= starX + 14
							&& mouseY >= starY - 2 && mouseY <= starY + 14) {
						toggleBookmark(this.visibleEntries.get(i).modId);
						return true;
					}
				}
			}

			ModEntryData clickedEntry = getEntryAt(mouseX, mouseY);
			if (clickedEntry != null && button == 0) {
				if (this.client != null) {
					ModConfigProvider provider = this.configProviders.get(clickedEntry.modId);
					this.client.setScreen(new ModDetailScreen(this,
						clickedEntry.modId, clickedEntry.name, clickedEntry.version,
						clickedEntry.description, clickedEntry.iconImage,
						clickedEntry.color, clickedEntry.author, clickedEntry.isLibrary,
						clickedEntry.homepage, clickedEntry.license, clickedEntry.environment,
						provider));
				}
				return true;
			}

			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (this.draggingScrollbar) {
			this.draggingScrollbar = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (this.draggingScrollbar && button == 0) {
			int visibleHeight = this.listBottom - this.listTop;
			int totalHeight = this.visibleEntries.size() * this.entryHeight;
			double maxScroll = Math.max(0, totalHeight - visibleHeight);
			if (maxScroll > 0) {
				double dragDelta = mouseY - this.dragStartY;
				double scrollRatio = maxScroll / (visibleHeight - getScrollbarHeight(visibleHeight, totalHeight));
				this.scrollAmount = this.dragStartScroll + dragDelta * scrollRatio;
			}
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	private boolean isOnScrollbar(double mouseX, double mouseY) {
		int visibleHeight = this.listBottom - this.listTop;
		int totalHeight = this.visibleEntries.size() * this.entryHeight;
		if (totalHeight <= visibleHeight) return false;

		int barX = this.listLeft + this.listWidth - 6;
		int sh = getScrollbarHeight(visibleHeight, totalHeight);
		int sy = this.listTop + (int) (this.scrollAmount * (visibleHeight - sh) / Math.max(1, totalHeight - visibleHeight));

		return mouseX >= barX - 2 && mouseX <= barX + SCROLLBAR_WIDTH + 6
				&& mouseY >= sy - 4 && mouseY <= sy + sh + 4;
	}

	private int getScrollbarHeight(int visibleHeight, int totalHeight) {
		return Math.max(20, visibleHeight * visibleHeight / totalHeight);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.showSuggestions && !this.suggestions.isEmpty()) {
			if (keyCode == 257 || keyCode == 335 || keyCode == 258) {
				if (this.selectedSuggestionIndex >= 0) {
					selectSuggestion(this.selectedSuggestionIndex);
				}
				return true;
			}
			if (keyCode == 265) {
				this.selectedSuggestionIndex--;
				if (this.selectedSuggestionIndex < 0) {
					this.selectedSuggestionIndex = this.suggestions.size() - 1;
				}
				return true;
			}
			if (keyCode == 264) {
				this.selectedSuggestionIndex++;
				if (this.selectedSuggestionIndex >= this.suggestions.size()) {
					this.selectedSuggestionIndex = 0;
				}
				return true;
			}
		}
		if (keyCode == 257 || keyCode == 335) {
			if (!this.searchQuery.isEmpty()) {
				commitSearchHistory();
			}
		}
		if (keyCode == 256 && this.searchField.isFocused()) {
			this.searchField.setFocused(false);
			this.showSuggestions = false;
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private ModEntryData getEntryAt(double mouseX, double mouseY) {
		for (int i = 0; i < this.visibleEntries.size(); i++) {
			int entryY = this.listTop + i * this.entryHeight - (int) this.scrollAmount;
			if (entryY + this.entryHeight < this.listTop || entryY > this.listBottom) continue;

			ModEntryData entry = this.visibleEntries.get(i);

			int detailX = this.listLeft + this.listWidth - 18;
			int detailY = entryY + (this.entryHeight - 10) / 2;
			if (mouseX >= detailX && mouseX <= detailX + 12
					&& mouseY >= detailY && mouseY <= detailY + 12) {
				return entry;
			}
		}
		return null;
	}

	private boolean isInsideList(double x, double y) {
		return x >= this.listLeft && x <= this.listLeft + this.listWidth
				&& y >= this.listTop && y <= this.listBottom;
	}

	private String truncateText(String text, TextRenderer renderer, int maxWidth) {
		if (renderer.getWidth(text) <= maxWidth) return text;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			sb.append(text.charAt(i));
			if (renderer.getWidth(sb.toString()) > maxWidth - 10) break;
		}
		sb.append("...");
		return sb.toString();
	}

	@Override
	public void close() {
		ModMenuConfig cfg = ModMenuConfig.getInstance();
		if (cfg.rememberState) {
			cfg.lastSort = this.currentSort.name();
			cfg.lastFilter = this.currentFilter.name();
		}
		cfg.getBookmarkedMods().clear();
		cfg.getBookmarkedMods().addAll(this.bookmarkedMods);
		ModMenuConfig.save();
		super.close();
	}

	private record ModEntryData(String modId, String name, String version, String description,
								java.awt.image.BufferedImage iconImage, int color, boolean isLibrary,
								String author, String homepage, String license, String environment) {}
}
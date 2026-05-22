package asterial.mod.menu.client.screen;

import asterial.mod.menu.config.ModMenuConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class ModMenuConfigScreen extends Screen {

	private final Screen parent;
	private ModMenuConfig cfg;

	private int cardL;
	private int cardT;
	private int cardW;
	private ButtonWidget densityButton;
	private ButtonWidget authorButton;

	public ModMenuConfigScreen(Screen parent) {
		super(Text.translatable("mod-menu.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		this.cfg = ModMenuConfig.getInstance();

		this.addDrawableChild(ButtonWidget.builder(
			Text.literal("←"),
			btn -> { if (this.client != null) this.client.setScreen(this.parent); }
		).dimensions(UI.BACK_X, UI.BACK_Y, UI.BACK_SIZE, UI.BACK_SIZE).build());

		this.cardW = Math.min(this.width - 40, 360);
		this.cardL = (this.width - this.cardW) / 2;
		this.cardT = UI.TITLE_Y + 16;

		int x = this.cardL + UI.PADDING;
		int y = this.cardT + UI.PADDING;
		int itemW = this.cardW - UI.PADDING * 2;

		addCycle(x, y, itemW,
			() -> getSortLabel(cfg.defaultSort),
			() -> { cfg.defaultSort = nextSort(cfg.defaultSort); });
		y += UI.ITEM_H + UI.GAP;

		addToggle(x, y, itemW,
			() -> cfg.rememberState,
			v -> cfg.rememberState = v);
		y += UI.ITEM_H + UI.GAP;

		this.densityButton = addCycleBtn(x, y, itemW,
			() -> getDensityLabel(cfg.entryDensity),
			() -> {
				cfg.entryDensity = nextDensity(cfg.entryDensity);
				if (cfg.showAuthor && !cfg.entryDensity.equals("spacious")) {
					cfg.showAuthor = false;
					if (this.authorButton != null) {
						this.authorButton.setMessage(Text.translatable("mod-menu.config.off"));
					}
				}
			});
		y += UI.ITEM_H + UI.GAP;

		addToggle(x, y, itemW,
			() -> cfg.showLibraries,
			v -> cfg.showLibraries = v);
		y += UI.ITEM_H + UI.GAP;

		addToggle(x, y, itemW,
			() -> cfg.hideFabricAuto,
			v -> cfg.hideFabricAuto = v);
		y += UI.ITEM_H + UI.GAP;

		this.authorButton = addToggleBtn(x, y, itemW,
			() -> cfg.showAuthor,
			v -> {
				cfg.showAuthor = v;
				if (v && !cfg.entryDensity.equals("spacious")) {
					cfg.entryDensity = "spacious";
					if (this.densityButton != null) {
						this.densityButton.setMessage(getDensityLabel("spacious"));
					}
				}
			});
		y += UI.ITEM_H + UI.GAP;

		addToggle(x, y, itemW,
			() -> cfg.showSearchSuggestions,
			v -> cfg.showSearchSuggestions = v);
		y += UI.ITEM_H + UI.GAP;

		addToggle(x, y, itemW,
			() -> cfg.enableHotkey,
			v -> cfg.enableHotkey = v);
		y += UI.ITEM_H + UI.GAP;

		addToggle(x, y, itemW,
			() -> cfg.showSearchHistory,
			v -> cfg.showSearchHistory = v);
		y += UI.ITEM_H + UI.GAP;

		addToggle(x, y, itemW,
			() -> cfg.enableBookmarks,
			v -> cfg.enableBookmarks = v);
	}

	private void addCycle(int x, int y, int itemW, Supplier<Text> getLabel, Runnable cycle) {
		ButtonWidget btn = ButtonWidget.builder(
			getLabel.get(),
			b -> { cycle.run(); b.setMessage(getLabel.get()); }
		).dimensions(x + itemW - UI.BTN_W, y, UI.BTN_W, UI.BTN_H).build();
		this.addDrawableChild(btn);
	}

	private ButtonWidget addCycleBtn(int x, int y, int itemW, Supplier<Text> getLabel, Runnable cycle) {
		ButtonWidget btn = ButtonWidget.builder(
			getLabel.get(),
			b -> { cycle.run(); b.setMessage(getLabel.get()); }
		).dimensions(x + itemW - UI.BTN_W, y, UI.BTN_W, UI.BTN_H).build();
		this.addDrawableChild(btn);
		return btn;
	}

	private void addToggle(int x, int y, int itemW, BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
		Text offText = Text.translatable("mod-menu.config.off");
		Text onText = Text.translatable("mod-menu.config.on");
		ButtonWidget btn = ButtonWidget.builder(
			getter.getAsBoolean() ? onText : offText,
			b -> {
				boolean nv = !getter.getAsBoolean();
				setter.accept(nv);
				b.setMessage(nv ? onText : offText);
			}
		).dimensions(x + itemW - UI.BTN_W, y, UI.BTN_W, UI.BTN_H).build();
		this.addDrawableChild(btn);
	}

	private ButtonWidget addToggleBtn(int x, int y, int itemW, BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
		Text offText = Text.translatable("mod-menu.config.off");
		Text onText = Text.translatable("mod-menu.config.on");
		ButtonWidget btn = ButtonWidget.builder(
			getter.getAsBoolean() ? onText : offText,
			b -> {
				boolean nv = !getter.getAsBoolean();
				setter.accept(nv);
				b.setMessage(nv ? onText : offText);
			}
		).dimensions(x + itemW - UI.BTN_W, y, UI.BTN_W, UI.BTN_H).build();
		this.addDrawableChild(btn);
		return btn;
	}

	@Override
	public void render(DrawContext ctx, int mx, int my, float delta) {
		super.render(ctx, mx, my, delta);

		Text configTitle = Text.translatable("mod-menu.config.title");
		ctx.drawText(this.textRenderer, configTitle, this.width / 2 - this.textRenderer.getWidth(configTitle) / 2, UI.TITLE_Y, UI.COLOR_TITLE, true);

		int items = 10;
		int cardH = UI.PADDING * 2 + items * UI.ITEM_H + UI.GAP * (items - 1);

		ctx.fill(this.cardL, this.cardT,
			this.cardL + this.cardW, this.cardT + cardH, UI.BG);

		ctx.fill(this.cardL, this.cardT,
			this.cardL + this.cardW, this.cardT + 1, 0xFF4A6EA5);

		int x = this.cardL + UI.PADDING;
		int y = this.cardT + UI.PADDING;

		drawL(ctx, x, y, "mod-menu.config.default_sort"); y += UI.ITEM_H + UI.GAP;
		drawL(ctx, x, y, "mod-menu.config.remember_state"); y += UI.ITEM_H + UI.GAP;
		drawL(ctx, x, y, "mod-menu.config.entry_density"); y += UI.ITEM_H + UI.GAP;
		drawL(ctx, x, y, "mod-menu.config.show_libraries"); y += UI.ITEM_H + UI.GAP;
		drawL(ctx, x, y, "mod-menu.config.hide_fabric"); y += UI.ITEM_H + UI.GAP;
		String authorKey = (cfg.showAuthor && !cfg.entryDensity.equals("spacious"))
			? "mod-menu.config.show_author_hint" : "mod-menu.config.show_author";
		drawL(ctx, x, y, authorKey); y += UI.ITEM_H + UI.GAP;
		drawL(ctx, x, y, "mod-menu.config.show_suggestions"); y += UI.ITEM_H + UI.GAP;
		drawL(ctx, x, y, "mod-menu.config.enable_hotkey"); y += UI.ITEM_H + UI.GAP;
		drawL(ctx, x, y, "mod-menu.config.show_search_history"); y += UI.ITEM_H + UI.GAP;
		drawL(ctx, x, y, "mod-menu.config.enable_bookmarks");
	}

	private void drawL(DrawContext ctx, int x, int y, String key) {
		ctx.drawText(this.textRenderer, Text.translatable(key), x, y + 5, UI.COLOR_TEXT, true);
	}

	@Override
	public void close() { ModMenuConfig.save(); super.close(); }
	@Override
	public void removed() { ModMenuConfig.save(); super.removed(); }

	private static Text getSortLabel(String s) {
		return switch (s) {
			case "NAME_ASC" -> Text.translatable("mod-menu.sort.name_asc");
			case "NAME_DESC" -> Text.translatable("mod-menu.sort.name_desc");
			case "VERSION" -> Text.translatable("mod-menu.sort.version");
			default -> Text.translatable("mod-menu.sort.name_asc");
		};
	}

	private static String nextSort(String s) {
		return switch (s) {
			case "NAME_ASC" -> "NAME_DESC";
			case "NAME_DESC" -> "VERSION";
			default -> "NAME_ASC";
		};
	}

	private static Text getDensityLabel(String d) {
		return switch (d) {
			case "compact" -> Text.translatable("mod-menu.config.density.compact");
			case "spacious" -> Text.translatable("mod-menu.config.density.spacious");
			default -> Text.translatable("mod-menu.config.density.normal");
		};
	}

	private static String nextDensity(String d) {
		return switch (d) {
			case "compact" -> "normal";
			case "normal" -> "spacious";
			default -> "compact";
		};
	}
}
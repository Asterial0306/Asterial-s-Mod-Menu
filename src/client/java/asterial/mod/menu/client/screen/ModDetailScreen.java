package asterial.mod.menu.client.screen;

import asterial.mod.menu.config.ModConfigProvider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.awt.image.BufferedImage;

public class ModDetailScreen extends Screen {

	private final Screen parent;
	private final String modId;
	private final String modName;
	private final String version;
	private final String description;
	private final BufferedImage iconImage;
	private final int color;
	private final String author;
	private final boolean isLibrary;
	private final String homepage;
	private final String license;
	private final String environment;
	private final ModConfigProvider configProvider;
	private static final int ICON_SIZE = 64;

	private int cardLeft;
	private int cardTop;
	private int cardWidth;

	public ModDetailScreen(Screen parent, String modId, String modName, String version,
			String description, BufferedImage iconImage, int color,
			String author, boolean isLibrary, String homepage, String license,
			String environment, ModConfigProvider configProvider) {
		super(Text.literal(modName));
		this.parent = parent;
		this.modId = modId;
		this.modName = modName;
		this.version = version;
		this.description = description;
		this.iconImage = iconImage;
		this.color = color;
		this.author = author;
		this.isLibrary = isLibrary;
		this.homepage = homepage;
		this.license = license;
		this.environment = environment;
		this.configProvider = configProvider;
	}

	@Override
	protected void init() {
		super.init();

		this.addDrawableChild(ButtonWidget.builder(
			Text.literal("←"),
			btn -> { if (this.client != null) this.client.setScreen(this.parent); }
		).dimensions(UI.BACK_X, UI.BACK_Y, UI.BACK_SIZE, UI.BACK_SIZE).build());

		this.cardWidth = Math.min(this.width - 40, 400);
		this.cardLeft = (this.width - this.cardWidth) / 2;
		this.cardTop = UI.TITLE_Y + 16;

		int cardHeight = calculateCardHeight();
		int buttonY = this.cardTop + cardHeight - UI.PADDING - UI.BTN_H;

		boolean hasHomepage = !this.homepage.isEmpty();
		boolean hasConfig = this.configProvider != null;

		if (hasConfig) {
			int configX;
			if (hasHomepage) {
				configX = this.cardLeft + this.cardWidth - UI.PADDING - (UI.BTN_W * 2 + UI.GAP);
			} else {
				configX = this.cardLeft + this.cardWidth - UI.PADDING - UI.BTN_W;
			}
			ButtonWidget configButton = ButtonWidget.builder(
				Text.translatable("mod-menu.detail.config"),
				btn -> {
					if (this.client != null) {
						this.client.setScreen(this.configProvider.createConfigScreen(this));
					}
				}
			).dimensions(configX, buttonY, UI.BTN_W, UI.BTN_H).build();
			this.addDrawableChild(configButton);
		}

		if (hasHomepage) {
			int homeX = this.cardLeft + this.cardWidth - UI.PADDING - UI.BTN_W;
			ButtonWidget homeButton = ButtonWidget.builder(
				Text.translatable("mod-menu.detail.homepage"),
				btn -> openUrl(this.homepage)
			).dimensions(homeX, buttonY, UI.BTN_W, UI.BTN_H).build();
			this.addDrawableChild(homeButton);
		}
	}

	private void openUrl(String url) {
		if (this.client != null) {
			this.client.setScreen(new ConfirmOpenUrlScreen(this, url));
		}
	}

	private int calculateCardHeight() {
		int infoLineCount = getInfoLineCount();
		int iconAreaHeight = Math.max(ICON_SIZE, infoLineCount * 12 + 4);

		int h = UI.PADDING;
		h += iconAreaHeight;
		h += UI.PADDING;

		if (!this.description.isEmpty()) {
			String[] descLines = wrapText(this.description, this.cardWidth - UI.PADDING * 2);
			h += descLines.length * 10;
		}
		h += UI.PADDING;
		h += 10 + UI.PADDING;
		h += UI.BTN_H + UI.PADDING;

		return h;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.drawCenteredTextWithShadow(
			this.textRenderer,
			Text.translatable("mod-menu.detail.title"),
			this.width / 2,
			UI.TITLE_Y,
			UI.COLOR_TITLE
		);

		int cardHeight = calculateCardHeight();

		context.fill(
			this.cardLeft, this.cardTop,
			this.cardLeft + this.cardWidth, this.cardTop + cardHeight,
			UI.BG
		);

		context.fill(
			this.cardLeft, this.cardTop,
			this.cardLeft + this.cardWidth, this.cardTop + 1,
			0xFF4A6EA5
		);

		int contentX = this.cardLeft + UI.PADDING;
		int iconX = contentX;
		int iconY = this.cardTop + UI.PADDING;

		if (this.iconImage != null) {
			drawBufferedImage(context, this.iconImage, iconX, iconY, ICON_SIZE, ICON_SIZE);
		} else {
			context.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, this.color);
			String initial = this.modName.substring(0, 1).toUpperCase();
			int textWidth = this.textRenderer.getWidth(initial);
			context.drawTextWithShadow(this.textRenderer,
				Text.literal(initial),
				iconX + (ICON_SIZE - textWidth) / 2,
				iconY + (ICON_SIZE - 8) / 2,
				0xFFFFFF);
		}

		int textX = iconX + ICON_SIZE + 12;
		int textY = iconY + 4;

		context.drawTextWithShadow(this.textRenderer,
			Text.literal(this.modName),
			textX, textY,
			UI.COLOR_TEXT);
		textY += 12;

		context.drawTextWithShadow(this.textRenderer,
			Text.literal("v" + this.version),
			textX, textY,
			UI.COLOR_SECONDARY);
		textY += 12;

		context.drawTextWithShadow(this.textRenderer,
			Text.translatable("mod-menu.detail.id").append(Text.literal(this.modId)),
			textX, textY,
			UI.COLOR_DIM);
		textY += 12;

		if (!this.author.isEmpty()) {
			context.drawTextWithShadow(this.textRenderer,
				Text.translatable("mod-menu.detail.author").append(Text.literal(this.author)),
				textX, textY,
				UI.COLOR_DIM);
			textY += 12;
		}

		if (!this.license.isEmpty()) {
			context.drawTextWithShadow(this.textRenderer,
				Text.translatable("mod-menu.detail.license").append(Text.literal(this.license)),
				textX, textY,
				UI.COLOR_DIM);
			textY += 12;
		}

		context.drawTextWithShadow(this.textRenderer,
			Text.translatable(switch (this.environment) {
				case "CLIENT" -> "mod-menu.detail.env.client";
				case "SERVER" -> "mod-menu.detail.env.server";
				default -> "mod-menu.detail.env.universal";
			}),
			textX, textY,
			UI.COLOR_DIM);

		int infoLineCount = getInfoLineCount();
		int iconAreaHeight = Math.max(ICON_SIZE, infoLineCount * 12 + 4);

		int dividerY = iconY + iconAreaHeight + UI.PADDING;
		context.fill(
			contentX, dividerY,
			this.cardLeft + this.cardWidth - UI.PADDING, dividerY + 1,
			UI.COLOR_DIVIDER
		);

		int descY = dividerY + 8;

		if (!this.description.isEmpty()) {
			String[] descLines = wrapText(this.description, this.cardWidth - UI.PADDING * 2);
			for (String line : descLines) {
				context.drawTextWithShadow(this.textRenderer,
					Text.literal(line),
					contentX, descY,
					UI.COLOR_SECONDARY);
				descY += 10;
			}
		} else {
			context.drawTextWithShadow(this.textRenderer,
				Text.translatable("mod-menu.detail.no_desc"),
				contentX, descY,
				UI.COLOR_MUTED);
		}

		int typeY = this.cardTop + cardHeight - UI.PADDING - UI.BTN_H - UI.PADDING - 10;
		context.drawTextWithShadow(this.textRenderer,
			Text.translatable(this.isLibrary ? "mod-menu.detail.type.library" : "mod-menu.detail.type.mod"),
			contentX, typeY,
			UI.COLOR_DIM);
	}

	@Override
	public void close() {
		if (this.client != null) {
			this.client.setScreen(this.parent);
		}
	}

	private int getInfoLineCount() {
		int count = 3;
		if (!this.author.isEmpty()) count++;
		if (!this.license.isEmpty()) count++;
		count++;
		return count;
	}

	private String[] wrapText(String text, int maxWidth) {
		java.util.List<String> lines = new java.util.ArrayList<>();
		StringBuilder currentLine = new StringBuilder();
		for (String word : text.split(" ")) {
			String testLine = currentLine.length() > 0
				? currentLine.toString() + " " + word
				: word;
			if (this.textRenderer.getWidth(testLine) > maxWidth) {
				if (currentLine.length() > 0) {
					lines.add(currentLine.toString());
					currentLine = new StringBuilder(word);
				} else {
					lines.add(word);
				}
			} else {
				if (currentLine.length() > 0) {
					currentLine.append(" ").append(word);
				} else {
					currentLine.append(word);
				}
			}
		}
		if (currentLine.length() > 0) {
			lines.add(currentLine.toString());
		}
		return lines.toArray(new String[0]);
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
}
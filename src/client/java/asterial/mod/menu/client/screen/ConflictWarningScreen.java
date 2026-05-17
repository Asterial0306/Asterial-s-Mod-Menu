package asterial.mod.menu.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConflictWarningScreen extends Screen {

	private final Screen parent;
	private int dialogX;
	private int dialogY;
	private int dialogW;
	private int dialogH;

	public ConflictWarningScreen(Screen parent) {
		super(Text.translatable("mod-menu.conflict.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();

		this.dialogW = Math.min(320, this.width - 40);
		this.dialogH = 110;
		this.dialogX = (this.width - this.dialogW) / 2;
		this.dialogY = (this.height - this.dialogH) / 2;

		int btnY = this.dialogY + this.dialogH - UI.PADDING - UI.BTN_H;
		int btnX = this.dialogX + (this.dialogW - UI.BTN_W) / 2;

		ButtonWidget closeButton = ButtonWidget.builder(
				Text.translatable("mod-menu.conflict.ok"),
				btn -> {
					if (this.client != null) {
						this.client.setScreen(this.parent);
					}
				}
			)
			.dimensions(btnX, btnY, UI.BTN_W, UI.BTN_H)
			.build();
		this.addDrawableChild(closeButton);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.fill(this.dialogX, this.dialogY,
			this.dialogX + this.dialogW, this.dialogY + this.dialogH,
			UI.BG);

		context.fill(this.dialogX, this.dialogY,
			this.dialogX + this.dialogW, this.dialogY + 1,
			0xFFE04B4B);

		int centerX = this.dialogX + this.dialogW / 2;
		int y = this.dialogY + UI.PADDING + 4;

		context.drawCenteredTextWithShadow(this.textRenderer,
			Text.translatable("mod-menu.conflict.header"),
			centerX, y, 0xFFE04B4B);
		y += 16;

		context.drawCenteredTextWithShadow(this.textRenderer,
			Text.translatable("mod-menu.conflict.line1"),
			centerX, y, UI.COLOR_TEXT);
		y += 12;

		context.drawCenteredTextWithShadow(this.textRenderer,
			Text.translatable("mod-menu.conflict.line2"),
			centerX, y, UI.COLOR_SECONDARY);
		y += 12;

		context.drawCenteredTextWithShadow(this.textRenderer,
			Text.translatable("mod-menu.conflict.line3"),
			centerX, y, UI.COLOR_DIM);
	}

	@Override
	public void close() {
		if (this.client != null) {
			this.client.setScreen(this.parent);
		}
	}
}
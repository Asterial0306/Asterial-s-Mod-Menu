package asterial.mod.menu.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public class ConfirmOpenUrlScreen extends Screen {

	private final Screen parent;
	private final String url;
	private int dialogX;
	private int dialogY;
	private int dialogW;
	private int dialogH;

	public ConfirmOpenUrlScreen(Screen parent, String url) {
		super(Text.translatable("mod-menu.url.confirm_title"));
		this.parent = parent;
		this.url = url;
	}

	@Override
	protected void init() {
		super.init();

		this.dialogW = Math.min(280, this.width - 40);
		this.dialogH = 90;
		this.dialogX = (this.width - this.dialogW) / 2;
		this.dialogY = (this.height - this.dialogH) / 2;

		int btnY = this.dialogY + this.dialogH - UI.PADDING - UI.BTN_H;
		int btnGap = 8;
		int totalBtnW = UI.BTN_W * 2 + btnGap;
		int btnStartX = this.dialogX + (this.dialogW - totalBtnW) / 2;

		ButtonWidget openButton = ButtonWidget.builder(
				Text.translatable("mod-menu.url.open"),
				btn -> {
					Util.getOperatingSystem().open(this.url);
					if (this.client != null) {
						this.client.setScreen(this.parent);
					}
				}
			)
			.dimensions(btnStartX, btnY, UI.BTN_W, UI.BTN_H)
			.build();
		this.addDrawableChild(openButton);

		ButtonWidget cancelButton = ButtonWidget.builder(
				Text.translatable("mod-menu.url.cancel"),
				btn -> {
					if (this.client != null) {
						this.client.setScreen(this.parent);
					}
				}
			)
			.dimensions(btnStartX + UI.BTN_W + btnGap, btnY, UI.BTN_W, UI.BTN_H)
			.build();
		this.addDrawableChild(cancelButton);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.fill(this.dialogX, this.dialogY,
			this.dialogX + this.dialogW, this.dialogY + this.dialogH,
			UI.BG);

		context.fill(this.dialogX, this.dialogY,
			this.dialogX + this.dialogW, this.dialogY + 1,
			0xFF4A6EA5);

		int centerX = this.dialogX + this.dialogW / 2;
		int textTop = this.dialogY + UI.PADDING + 8;

		context.drawCenteredTextWithShadow(this.textRenderer,
			Text.translatable("mod-menu.url.confirm_msg"),
			centerX, textTop,
			UI.COLOR_TEXT);

		String displayUrl = this.url.length() > 45
			? this.url.substring(0, 42) + "..." : this.url;
		context.drawCenteredTextWithShadow(this.textRenderer,
			Text.literal(displayUrl),
			centerX, textTop + 14,
			UI.COLOR_SECONDARY);
	}

	@Override
	public void close() {
		if (this.client != null) {
			this.client.setScreen(this.parent);
		}
	}
}
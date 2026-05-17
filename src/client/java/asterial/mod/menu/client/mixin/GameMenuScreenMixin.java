package asterial.mod.menu.client.mixin;

import asterial.mod.menu.client.screen.ModMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Screen.class)
public abstract class GameMenuScreenMixin {

	@Shadow
	@Final
	private List<Drawable> drawables;

	@Shadow
	@Final
	private List<Selectable> selectables;

	@Shadow
	@Final
	private List<Element> children;

	@Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("TAIL"))
	private void onInit(CallbackInfo ci) {
		Screen screen = (Screen) (Object) this;
		if (!(screen instanceof GameMenuScreen)) {
			return;
		}

		ButtonWidget disconnectButton = null;
		for (Drawable d : drawables) {
			if (d instanceof ButtonWidget btn) {
				Text msg = btn.getMessage();
				String msgStr = msg.getString();
				if (msgStr.equals("menu.disconnect") || msgStr.equals("退出保存并回到标题画面")) {
					disconnectButton = btn;
					break;
				}
			}
		}

		if (disconnectButton == null) {
			ButtonWidget lowest = null;
			for (Drawable d : drawables) {
				if (d instanceof ButtonWidget btn) {
					if (lowest == null || btn.getY() > lowest.getY()) {
						lowest = btn;
					}
				}
			}
			disconnectButton = lowest;
		}

		if (disconnectButton != null) {
			disconnectButton.setY(disconnectButton.getY() + 24);
		}

		int btnY;
		if (disconnectButton != null) {
			btnY = disconnectButton.getY() - 24;
		} else {
			btnY = screen.height - 46;
		}

		ButtonWidget button = ButtonWidget.builder(
			Text.translatable("mod-menu.view_mods"),
			btn -> MinecraftClient.getInstance().setScreen(ModMenuScreen.create(screen))
		)
		.dimensions(screen.width / 2 - 100, btnY, 200, 20)
		.build();

		this.drawables.add(button);
		this.selectables.add(button);
		this.children.add(button);
	}
}
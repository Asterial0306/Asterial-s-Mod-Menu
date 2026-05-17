package asterial.mod.menu.client.mixin;

import asterial.mod.menu.client.screen.ConflictWarningScreen;
import asterial.mod.menu.client.screen.ModMenuScreen;
import asterial.mod.menu.config.ModMenuConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Screen.class)
public abstract class TitleScreenMixin {

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
		if (!(screen instanceof TitleScreen)) {
			return;
		}

		if (FabricLoader.getInstance().isModLoaded("modmenu")) {
			MinecraftClient.getInstance().setScreen(new ConflictWarningScreen(screen));
			return;
		}

		int centerX = screen.width / 2;
		int realmsButtonY = screen.height / 4 + 48 + 24 * 2;

		for (Drawable d : drawables) {
			if (d instanceof ButtonWidget btn && btn.getY() > realmsButtonY) {
				btn.setY(btn.getY() + 24);
			}
		}

		ButtonWidget button = ButtonWidget.builder(
			Text.translatable("mod-menu.view_mods"),
			btn -> MinecraftClient.getInstance().setScreen(ModMenuScreen.create(null))
		)
		.dimensions(centerX - 100, realmsButtonY + 24, 200, 20)
		.build();

		this.drawables.add(button);
		this.selectables.add(button);
		this.children.add(button);
	}

	@Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
	private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		Screen screen = (Screen) (Object) this;
		if (!(screen instanceof TitleScreen)) {
			return;
		}
		if (!ModMenuConfig.getInstance().enableHotkey) {
			return;
		}
		if (keyCode == GLFW.GLFW_KEY_M && !Screen.hasShiftDown() && !Screen.hasControlDown()) {
			MinecraftClient.getInstance().setScreen(ModMenuScreen.create(null));
			cir.setReturnValue(true);
		}
	}
}
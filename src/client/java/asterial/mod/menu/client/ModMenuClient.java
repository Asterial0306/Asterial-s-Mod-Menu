package asterial.mod.menu.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ModMenuClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.mod-menu.open",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_M,
			"key.categories.mod-menu"
		));
	}
}
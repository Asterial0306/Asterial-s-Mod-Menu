package asterial.mod.menu.client.config;

import asterial.mod.menu.client.screen.ModMenuConfigScreen;
import asterial.mod.menu.config.ModConfigProvider;
import net.minecraft.client.gui.screen.Screen;

public class ModMenuConfigProvider implements ModConfigProvider {
	@Override
	public String getModId() {
		return "mod-menu";
	}

	@Override
	public Screen createConfigScreen(Screen parent) {
		return new ModMenuConfigScreen(parent);
	}
}
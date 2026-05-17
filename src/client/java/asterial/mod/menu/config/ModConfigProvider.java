package asterial.mod.menu.config;

import net.minecraft.client.gui.screen.Screen;

public interface ModConfigProvider {
	String getModId();
	Screen createConfigScreen(Screen parent);
}
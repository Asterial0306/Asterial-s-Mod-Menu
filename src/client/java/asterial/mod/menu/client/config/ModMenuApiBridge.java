package asterial.mod.menu.client.config;

import asterial.mod.menu.config.ModConfigProvider;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.screen.Screen;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModMenuApiBridge {

	public static List<ModConfigProvider> discover() {
		List<ModConfigProvider> providers = new ArrayList<>();
		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			try {
				List<String> entryClasses = getModmenuEntrypoints(mod);
				for (String className : entryClasses) {
					Class<?> entryClass = Class.forName(className, true,
							Thread.currentThread().getContextClassLoader());
					Object entry = entryClass.getDeclaredConstructor().newInstance();
					Method getFactory = entry.getClass().getMethod("getModConfigScreenFactory");
					Object factory = getFactory.invoke(entry);
					if (factory != null) {
						Method create = findCreateMethod(factory.getClass());
						if (create != null) {
							create.setAccessible(true);
							String modId = mod.getMetadata().getId();
							providers.add(new ReflectedModConfigProvider(modId, create, factory));
						}
					}
				}
			} catch (Exception ignored) {}
		}
		return providers;
	}

	private static List<String> getModmenuEntrypoints(ModContainer mod) {
		List<String> classes = new ArrayList<>();
		try {
			for (Path root : mod.getRootPaths()) {
				Path fmj = root.resolve("fabric.mod.json");
				if (Files.exists(fmj)) {
					try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(fmj))) {
						JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
						if (json.has("entrypoints")) {
							JsonObject entrypoints = json.getAsJsonObject("entrypoints");
							if (entrypoints.has("modmenu")) {
								for (JsonElement elem : entrypoints.getAsJsonArray("modmenu")) {
									classes.add(elem.getAsString());
								}
							}
						}
					}
					break;
				}
			}
		} catch (Exception ignored) {}
		return classes;
	}

	private static Method findCreateMethod(Class<?> factoryClass) {
		for (Method m : factoryClass.getMethods()) {
			if (m.getName().equals("create") && m.getParameterCount() == 1
					&& Screen.class.isAssignableFrom(m.getReturnType())) {
				return m;
			}
		}
		return null;
	}

	private static class ReflectedModConfigProvider implements ModConfigProvider {
		private final String modId;
		private final Method createMethod;
		private final Object factoryInstance;

		ReflectedModConfigProvider(String modId, Method createMethod, Object factoryInstance) {
			this.modId = modId;
			this.createMethod = createMethod;
			this.factoryInstance = factoryInstance;
		}

		@Override
		public String getModId() {
			return modId;
		}

		@Override
		public Screen createConfigScreen(Screen parent) {
			try {
				createMethod.setAccessible(true);
				return (Screen) createMethod.invoke(factoryInstance, parent);
			} catch (Exception ignored) {}
			return null;
		}
	}
}
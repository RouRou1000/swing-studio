package dev.swingstudio;

import com.mojang.blaze3d.platform.InputConstants;
import dev.swingstudio.screen.SwingStudioScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SwingStudio implements ClientModInitializer {
	public static final String MOD_ID = "swingstudio";
	public static final Logger LOGGER = LoggerFactory.getLogger("Swing Studio");

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	public static KeyMapping openKey;

	@Override
	public void onInitializeClient() {
		SwingConfig.load();
		openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key." + MOD_ID + ".open", InputConstants.KEY_K, CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(SwingStudio::onTick);

		String capture = System.getProperty(MOD_ID + ".capture", "");
		if (!capture.isEmpty() && !capture.equals("false")) {
			try {
				Class.forName(capture.equals("short") ? "dev.swingstudio.dev.DevShortCapture" : "dev.swingstudio.dev.DevCapture").getMethod("start").invoke(null);
			} catch (ReflectiveOperationException e) {
				LOGGER.warn("Capture requested but not available", e);
			}
		}
	}

	private static void onTick(Minecraft client) {
		SwingStudioScreen.checkSession(client);
		while (openKey.consumeClick()) {
			if (client.gui.screen() == null) {
				client.gui.setScreen(new SwingStudioScreen(null));
			}
		}
	}
}

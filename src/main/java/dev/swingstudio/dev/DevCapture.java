package dev.swingstudio.dev;

import dev.swingstudio.ConfigFiles;
import dev.swingstudio.Level;
import dev.swingstudio.Presets;
import dev.swingstudio.Preview;
import dev.swingstudio.SwingConfig;
import dev.swingstudio.SwingSettings;
import dev.swingstudio.SwingStudio;
import dev.swingstudio.screen.SwingStudioScreen;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DevCapture {
	private static final int SWING_FRAMES = 22;
	private static final int REST_FRAMES = 6;

	private final Deque<Runnable> steps = new ArrayDeque<>();
	private int warmup;
	private boolean scripted;

	public static void start() {
		DevCapture capture = new DevCapture();
		ClientTickEvents.END_CLIENT_TICK.register(capture::tick);
	}

	private void tick(Minecraft client) {
		if (client.player == null) {
			return;
		}

		if (this.warmup++ < 120) {
			if (this.warmup == 20) {
				this.prepareWorld(client);
			}

			return;
		}

		if (!this.scripted) {
			this.scripted = true;
			this.script(client);
		}

		Runnable step = this.steps.poll();
		if (step == null) {
			SwingStudio.LOGGER.info("[capture] done");
			client.stop();
			return;
		}

		step.run();
	}

	private void prepareWorld(Minecraft client) {
		IntegratedServer server = client.getSingleplayerServer();
		if (server == null) {
			return;
		}

		java.util.UUID id = client.player.getUUID();
		server.execute(() -> {
			var source = server.createCommandSourceStack();
			server.getCommands().performPrefixedCommand(source, "time set 6000");
			server.getCommands().performPrefixedCommand(source, "weather clear");
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null) {
				ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
				sword.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
				player.setItemInHand(InteractionHand.MAIN_HAND, sword);
			}
		});
	}

	private void script(Minecraft client) {
		SwingConfig config = SwingConfig.get();
		config.enabled = true;
		config.mode = Level.EXPERT;
		config.equipAnimation = false;
		config.autoPreview = false;
		config.items.clear();
		config.all = new SwingSettings();
		this.then(() -> {
			client.player.setXRot(12.0F);
		});
		this.wait(10);

		for (String id : List.of("vanilla", "slow_pvp", "katana", "mace_slam", "double_flip", "uppercut", "drill", "scythe")) {
			Presets.Preset preset = Presets.ALL.stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
			this.then(() -> {
				config.all = new SwingSettings();
				Presets.apply(config.all, preset, false, false);
				SwingStudio.LOGGER.info("[capture] gif {} speed {}", id, config.all.speed);
			});
			this.swing("game-" + id);
		}

		this.then(() -> {
			config.all = new SwingSettings();
			Presets.apply(config.all, find("katana"), false, false);
			Preview.setFrozen(0.22F);
		});
		this.wait(4);
		this.shot("still-katana");
		this.then(() -> Preview.setFrozen(null));

		this.then(() -> {
			config.all = new SwingSettings();
			Presets.apply(config.all, find("slow_pvp"), false, false);
			ConfigFiles.save("PvP Main");
			config.all.speed = 140;
			ConfigFiles.save("Fast Chill");
			Presets.apply(config.all, find("katana"), false, false);
		});
		this.editor(client, "PRESETS", null, 0.2F, "editor-presets");
		this.editor(client, "SWING", "curve_graph", 0.2F, "editor-swing");
		this.editor(client, "ROTATION", "tilt", 0.25F, "editor-rotation");
		this.editor(client, "HOLD", "rot_z", 0.0F, "editor-hold");
		this.editor(client, "CONFIGS", null, 0.0F, "editor-configs");

		this.then(() -> {
			config.all = new SwingSettings();
			setStatic("page", pageValue("HOLD"));
			client.gui.setScreen(new SwingStudioScreen(null));
		});
		this.wait(10);
		this.then(() -> {
			setHint(client, "rot_z");
			Preview.setFrozen(0.0F);
		});
		for (int i = 0; i < 36; i++) {
			int frame = i;
			this.then(() -> {
				double t = frame / 35.0;
				config.all.rotZ = Math.round(70.0 * Math.sin(Math.PI * t) / 5.0) * 5.0F;
				config.all.size = 100 + Math.round(30.0F * (float) Math.sin(Math.PI * t) / 5.0F) * 5;
				setHint(client, "rot_z");
			});
			this.shot(String.format("live-%03d", frame));
		}

		this.then(() -> {
			client.gui.setScreen(null);
			Preview.setFrozen(null);
		});
		this.wait(5);
	}

	private static Presets.Preset find(String id) {
		return Presets.ALL.stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
	}

	private void swing(String name) {
		int index = 0;
		for (int i = 0; i < REST_FRAMES / 2; i++, index++) {
			this.then(() -> Preview.setFrozen(0.0F));
			this.shot(String.format("%s-%03d", name, index));
		}

		for (int i = 0; i < SWING_FRAMES; i++, index++) {
			float moment = (i + 1) / (float) SWING_FRAMES;
			this.then(() -> Preview.setFrozen(moment));
			this.shot(String.format("%s-%03d", name, index));
		}

		for (int i = 0; i < REST_FRAMES / 2; i++, index++) {
			this.then(() -> Preview.setFrozen(0.0F));
			this.shot(String.format("%s-%03d", name, index));
		}

		this.then(() -> Preview.setFrozen(null));
	}

	private void editor(Minecraft client, String pageName, String hint, float moment, String name) {
		this.then(() -> {
			client.gui.setScreen(null);
			setStatic("page", pageValue(pageName));
			client.gui.setScreen(new SwingStudioScreen(null));
		});
		this.wait(6);
		this.then(() -> {
			Preview.setFrozen(moment);
			setHint(client, hint);
		});
		this.wait(12);
		this.then(() -> setHint(client, hint));
		this.wait(2);
		this.shot(name);
	}

	private void then(Runnable step) {
		this.steps.add(step);
	}

	private void wait(int ticks) {
		for (int i = 0; i < ticks; i++) {
			this.steps.add(() -> {
			});
		}
	}

	private void shot(String name) {
		this.wait(1);
		this.then(() -> {
			Minecraft client = Minecraft.getInstance();
			Screenshot.grab(client.gameDirectory, "sws-" + name + ".png", client.gameRenderer.mainRenderTarget(), 1, message -> {
			});
		});
	}

	private static Object pageValue(String name) {
		try {
			@SuppressWarnings({"unchecked", "rawtypes"})
			Class<Enum> type = (Class<Enum>) Class.forName("dev.swingstudio.screen.Options$Page");
			return Enum.valueOf(type, name);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void setStatic(String field, Object value) {
		try {
			Field f = SwingStudioScreen.class.getDeclaredField(field);
			f.setAccessible(true);
			f.set(null, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void setHint(Minecraft client, String hint) {
		if (client.gui.screen() instanceof SwingStudioScreen screen) {
			try {
				Field f = SwingStudioScreen.class.getDeclaredField("hintId");
				f.setAccessible(true);
				f.set(screen, hint);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}
	}
}

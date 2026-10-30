package dev.swingstudio.dev;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.swingstudio.ConfigFiles;
import dev.swingstudio.Level;
import dev.swingstudio.Presets;
import dev.swingstudio.Preview;
import dev.swingstudio.SwingConfig;
import dev.swingstudio.SwingSettings;
import dev.swingstudio.SwingStudio;
import dev.swingstudio.screen.SwingStudioScreen;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DevShortCapture {
	private static final int FRAMES = 48;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Deque<Runnable> steps = new ArrayDeque<>();
	private final Map<String, Object> meta = new LinkedHashMap<>();
	private int warmup;
	private boolean scripted;
	private double homeX;
	private double homeY;
	private double homeZ;
	private float homeYaw;

	public static void start() {
		DevShortCapture capture = new DevShortCapture();
		ClientTickEvents.END_CLIENT_TICK.register(capture::tick);
	}

	private void tick(Minecraft client) {
		if (client.player == null) {
			return;
		}

		if (this.warmup++ < 120) {
			if (this.warmup == 1) {
				this.homeYaw = client.player.getYRot();
				client.gui.setScreen(new Still());
			}

			if (this.warmup == 20) {
				this.homeX = client.player.getX();
				this.homeY = client.player.getY();
				this.homeZ = client.player.getZ();
				client.player.setYRot(this.homeYaw);
				this.noon(client);
				this.command(client, "weather clear", "gamemode creative @p");
				this.hand(client, sword(true));
			}

			return;
		}

		if (!this.scripted) {
			this.scripted = true;
			this.script(client);
		}

		Runnable step = this.steps.poll();
		if (step == null) {
			this.writeMeta(client);
			SwingStudio.LOGGER.info("[short] done");
			client.stop();
			return;
		}

		step.run();
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
			client.player.setYRot(this.homeYaw);
			client.player.setXRot(12.0F);
		});
		this.wait(10);

		for (String id : List.of("vanilla", "slow_pvp", "katana", "double_flip", "drill", "scythe", "uppercut", "whip")) {
			this.preset(config, id);
			this.swing("world-" + id);
		}

		this.then(() -> {
			this.fly(client);
			this.command(client, "tp @p 2000.5 200 2000.5 0 0");
			client.player.setYRot(0.0F);
			client.player.setXRot(0.0F);
		});
		this.wait(40);
		this.then(() -> this.command(client, "fill 1978 184 2010 2022 216 2010 minecraft:magenta_concrete",
			"setblock 2000 199 2000 minecraft:barrier"));
		this.wait(20);
		this.then(() -> {
			this.noon(client);
			client.player.setYRot(0.0F);
			client.player.setXRot(0.0F);
		});
		this.wait(10);
		this.then(() -> this.hand(client, sword(false)));
		this.wait(6);
		for (String id : List.of("vanilla", "slow_pvp", "katana", "double_flip", "drill", "scythe", "uppercut", "whip",
			"backflip", "tornado", "rapier", "jelly", "pendulum", "hyper", "wind_up", "silk")) {
			this.preset(config, id);
			this.swing("key-sword-" + id);
		}

		this.preset(config, "combo_four");
		for (int k = 0; k < 4; k++) {
			this.then(() -> client.player.swing(InteractionHand.MAIN_HAND, client.player.getMainHandItem().getAttackAnimation(), false));
			this.wait(14);
			this.swing("key-sword-combo_four-" + k);
		}

		Object[][] items = {
			{Items.TOTEM_OF_UNDYING, "propeller"}, {Items.MACE, "mace_slam"}, {Items.NETHERITE_AXE, "lumberjack"},
			{Items.TRIDENT, "spear"}, {Items.AIR, "jab"}, {Items.SHIELD, "shield_bash"}, {Items.BLAZE_ROD, "flip"},
			{Items.FISHING_ROD, "boomerang"}
		};
		for (Object[] pair : items) {
			Item item = (Item) pair[0];
			String id = (String) pair[1];
			String name = item == Items.AIR ? "hand" : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
			this.then(() -> this.hand(client, item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item)));
			this.wait(4);
			this.preset(config, id);
			this.swing("key-" + name + "-" + id);
		}

		this.then(() -> {
			this.hand(client, sword(false));
			config.all = new SwingSettings();
			config.all.custom = true;
			Preview.setFrozen(0.0F);
		});
		this.wait(4);
		for (int i = 0; i < 60; i++) {
			float t = i / 59.0F;
			this.then(() -> holdPose(config.all, t));
			this.shot(String.format("key-sword-hold-%03d", i));
		}

		this.then(() -> Preview.setFrozen(null));

		this.then(() -> {
			this.command(client, "tp @p " + this.homeX + " " + this.homeY + " " + this.homeZ + " " + this.homeYaw + " 12");
			this.hand(client, sword(true));
		});
		this.wait(40);

		this.then(() -> {
			config.all = new SwingSettings();
			Presets.apply(config.all, find("vanilla"), false, false);
		});
		this.open(client, "PRESETS");
		List<String> tried = List.of("slow_pvp", "katana", "double_flip", "scythe", "drill", "whip");
		for (int p = 0; p < tried.size(); p++) {
			String id = tried.get(p);
			for (int i = 0; i < 16; i++) {
				float moment = i == 0 ? 0.0F : i / 15.0F;
				int index = p * 16 + i;
				this.then(() -> {
					SwingSettings trying = new SwingSettings();
					Presets.apply(trying, find(id), false, false);
					Preview.setSettings(trying);
					Preview.setFrozen(moment);
				});
				this.shot(String.format("ed-presets-%03d", index));
			}
		}

		this.then(() -> {
			Preview.setSettings(null);
			this.layout(client, "ed-presets");
		});

		this.then(() -> {
			config.all = new SwingSettings();
			Presets.apply(config.all, find("vanilla"), false, false);
		});
		this.open(client, "SWING");
		this.then(() -> setHint(client, "curve_graph"));
		for (int i = 0; i < 72; i++) {
			float t = i / 71.0F;
			float moment = (i % 12) / 11.0F;
			this.then(() -> {
				curve(config.all, t);
				Preview.setFrozen(moment);
				setHint(client, "curve_graph");
			});
			this.shot(String.format("ed-curve-%03d", i));
		}

		this.then(() -> this.layout(client, "ed-curve"));

		this.then(() -> {
			config.all = new SwingSettings();
			Presets.apply(config.all, find("vanilla"), false, false);
		});
		this.open(client, "ROTATION");
		for (int i = 0; i < 48; i++) {
			float t = i / 47.0F;
			float moment = (i % 12) / 11.0F;
			this.then(() -> {
				config.all.tilt = Math.round(120.0F * smooth(Math.min(1.0F, t * 1.6F)) / 5.0F) * 5.0F;
				config.all.spinAxis = SwingSettings.SpinAxis.NONE;
				Preview.setFrozen(moment);
				setHint(client, "tilt");
			});
			this.shot(String.format("ed-tilt-%03d", i));
		}

		this.then(() -> this.layout(client, "ed-tilt"));

		this.then(() -> {
			config.all = new SwingSettings();
			config.all.custom = true;
		});
		this.open(client, "HOLD");
		this.then(() -> Preview.setFrozen(0.0F));
		for (int i = 0; i < 60; i++) {
			float t = i / 59.0F;
			this.then(() -> {
				holdPose(config.all, t);
				setHint(client, t < 0.5F ? "rot_z" : "size");
			});
			this.shot(String.format("ed-hold-%03d", i));
		}

		this.then(() -> this.layout(client, "ed-hold"));

		for (Level level : Level.values()) {
			this.then(() -> {
				config.all = new SwingSettings();
				Presets.apply(config.all, find("katana"), false, false);
				config.mode = level;
			});
			this.open(client, "SWING");
			this.then(() -> Preview.setFrozen(0.3F));
			this.shot("ed-mode-" + level.name().toLowerCase());
		}

		this.then(() -> config.mode = Level.EXPERT);

		this.open(client, "PRESETS");
		this.then(() -> {
			EditBox search = (EditBox) get(client.gui.screen(), "search");
			search.setValue("spin");
		});
		this.wait(3);
		this.shot("ed-search");
		this.then(() -> this.layout(client, "ed-search"));

		this.then(() -> {
			config.all = new SwingSettings();
			Presets.apply(config.all, find("slow_pvp"), false, false);
			ConfigFiles.save("PvP Main");
			Presets.apply(config.all, find("katana"), false, false);
			ConfigFiles.save("Katana Night");
			Presets.apply(config.all, find("double_flip"), false, false);
			ConfigFiles.save("Silly Flips");
			this.meta.put("code", ConfigFiles.exportCode());
		});
		this.open(client, "CONFIGS");
		this.shot("ed-configs");
		this.then(() -> this.layout(client, "ed-configs"));

		this.then(() -> {
			try {
				Class<?> type = Class.forName("dev.swingstudio.screen.ItemPickerScreen");
				Constructor<?> make = type.getDeclaredConstructor(Screen.class, List.class);
				make.setAccessible(true);
				List<String> chosen = new ArrayList<>(List.of("minecraft:diamond_sword", "minecraft:netherite_sword", "minecraft:iron_sword", "minecraft:mace"));
				client.gui.setScreen((Screen) make.newInstance(client.gui.screen(), chosen));
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		});
		this.wait(8);
		this.shot("ed-picker");
		this.then(() -> this.layout(client, "ed-picker"));

		this.then(() -> {
			client.gui.setScreen(new Still());
			Preview.reset();
		});
		this.wait(5);
	}

	private void preset(SwingConfig config, String id) {
		this.then(() -> {
			config.all = new SwingSettings();
			Presets.apply(config.all, find(id), false, false);
			Map<String, Object> info = new LinkedHashMap<>();
			info.put("settings", GSON.toJsonTree(config.all));
			this.meta.put("preset:" + id, info);
		});
	}

	private void swing(String name) {
		this.then(() -> Preview.setFrozen(0.0F));
		this.shot(String.format("%s-%03d", name, 0));
		for (int i = 1; i <= FRAMES; i++) {
			float moment = i / (float) FRAMES;
			this.then(() -> Preview.setFrozen(moment));
			this.shot(String.format("%s-%03d", name, i));
		}

		this.then(() -> Preview.setFrozen(null));
	}

	private void open(Minecraft client, String pageName) {
		this.then(() -> {
			client.gui.setScreen(new Still());
			setStatic("page", pageValue(pageName));
			client.gui.setScreen(new SwingStudioScreen(null));
			Preview.set(sword(true));
			Preview.setFrozen(0.0F);
		});
		this.wait(8);
	}

	private static void holdPose(SwingSettings s, float t) {
		float a = smooth(Math.min(1.0F, t * 2.0F));
		float b = smooth(Math.max(0.0F, t * 2.0F - 1.0F));
		s.rotZ = Math.round(60.0F * a / 5.0F) * 5.0F;
		s.rotX = Math.round(-20.0F * a / 5.0F) * 5.0F;
		s.size = 100 + Math.round(35.0F * b / 5.0F) * 5;
		s.posX = Math.round(-0.12F * b * 100.0F) / 100.0F;
	}

	private static void curve(SwingSettings s, float t) {
		float[][] keys = {{0.0F, 0.0F, 1.0F, 1.0F}, {0.7F, 0.0F, 0.15F, 1.0F}, {0.3F, 1.5F, 0.6F, 1.25F}};
		float x = Math.min(1.9999F, t * 2.0F);
		int k = (int) x;
		float f = smooth(Math.min(1.0F, (x - k) * 1.5F));
		float[] from = keys[k];
		float[] to = keys[k + 1];
		s.x1 = from[0] + (to[0] - from[0]) * f;
		s.y1 = from[1] + (to[1] - from[1]) * f;
		s.x2 = from[2] + (to[2] - from[2]) * f;
		s.y2 = from[3] + (to[3] - from[3]) * f;
	}

	private static float smooth(float t) {
		return t * t * (3.0F - 2.0F * t);
	}

	private static ItemStack sword(boolean glint) {
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, glint);
		return sword;
	}

	private void hand(Minecraft client, ItemStack stack) {
		Preview.set(stack);
		this.command(client, "clear @p");
	}

	private void noon(Minecraft client) {
		IntegratedServer server = client.getSingleplayerServer();
		if (server == null) {
			return;
		}

		server.execute(() -> {
			var level = server.overworld();
			var clock = level.dimensionType().defaultClock();
			if (clock.isEmpty()) {
				SwingStudio.LOGGER.warn("[short] overworld has no default clock");
				return;
			}

			level.clockManager().setTotalTicks(clock.get(), 6000L);
			level.clockManager().setPaused(clock.get(), true);
			server.getPlayerList().broadcastAll(level.clockManager().createFullSyncPacket());
			SwingStudio.LOGGER.info("[short] clock set to noon");
		});
	}

	private void fly(Minecraft client) {
		client.player.getAbilities().flying = true;
		IntegratedServer server = client.getSingleplayerServer();
		UUID id = client.player.getUUID();
		if (server != null) {
			server.execute(() -> {
				ServerPlayer player = server.getPlayerList().getPlayer(id);
				if (player != null) {
					player.getAbilities().mayfly = true;
					player.getAbilities().flying = true;
					player.onUpdateAbilities();
				}
			});
		}
	}

	private void command(Minecraft client, String... commands) {
		IntegratedServer server = client.getSingleplayerServer();
		if (server == null) {
			return;
		}

		server.execute(() -> {
			var source = server.createCommandSourceStack();
			for (String command : commands) {
				server.getCommands().performPrefixedCommand(source, command);
			}
		});
	}

	private void layout(Minecraft client, String name) {
		Screen screen = client.gui.screen();
		if (screen == null) {
			return;
		}

		List<Map<String, Object>> boxes = new ArrayList<>();
		for (GuiEventListener child : screen.children()) {
			if (child instanceof AbstractWidget widget) {
				Map<String, Object> box = new LinkedHashMap<>();
				box.put("type", widget.getClass().getSimpleName());
				box.put("x", widget.getX());
				box.put("y", widget.getY());
				box.put("w", widget.getWidth());
				box.put("h", widget.getHeight());
				box.put("text", widget.getMessage().getString());
				boxes.add(box);
			}
		}

		Map<String, Object> info = new LinkedHashMap<>();
		info.put("guiScale", client.getWindow().getGuiScale());
		info.put("width", screen.width);
		info.put("height", screen.height);
		info.put("widgets", boxes);
		this.meta.put("layout:" + name, info);
	}

	private void writeMeta(Minecraft client) {
		try {
			Path file = client.gameDirectory.toPath().resolve("screenshots/short-meta.json");
			Files.writeString(file, GSON.toJson(this.meta));
		} catch (IOException e) {
			SwingStudio.LOGGER.warn("Could not write capture meta", e);
		}
	}

	private static final class Still extends Screen {
		Still() {
			super(net.minecraft.network.chat.Component.empty());
		}

		@Override
		public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}
	}

	private static Presets.Preset find(String id) {
		return Presets.ALL.stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
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
		this.then(() -> Minecraft.getInstance().gui.hud.getChat().clearMessages(false));
		this.wait(1);
		this.then(() -> {
			Minecraft client = Minecraft.getInstance();
			Screenshot.grab(client.gameDirectory, "short-" + name + ".png", client.gameRenderer.mainRenderTarget(), 1, message -> {
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

	private static Object get(Object target, String field) {
		try {
			Field f = target.getClass().getDeclaredField(field);
			f.setAccessible(true);
			return f.get(target);
		} catch (ReflectiveOperationException e) {
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

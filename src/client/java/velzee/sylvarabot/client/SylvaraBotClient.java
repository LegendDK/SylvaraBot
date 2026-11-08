package velzee.sylvarabot.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class SylvaraBotClient implements ClientModInitializer {

	private static KeyMapping openGuiKey;
	private static KeyMapping emergencyStopKey;

	// Menggunakan konstruktor new Identifier
	public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath("sylvarabot", "main")
	);

	@Override
	public void onInitializeClient() {
		MovementController.registerEvents();
		AutoLumberjackController.registerEvents();

		// Auto-reset state saat disconnect dari server
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			System.out.println("[SylvaraBot] Client disconnect terdeteksi. Mereset seluruh state bot...");
			MovementController.reset();
			AutoLumberjackController.resetState();
		});

		// 1. REGISTRASI KEYBIND (SYLVARABOT)
		openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.sylvarabot.opengui",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_K,
				KEY_CATEGORY
		));

		emergencyStopKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.sylvarabot.stop",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_X,
				KEY_CATEGORY
		));

		// 2. HANDLER TOMBOL DI CLIENT TICK
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;

			while (openGuiKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new SylvaraScreen());
				}
			}

			while (emergencyStopKey.consumeClick()) {
				MovementController.stop();
				AutoLumberjackController.stopTask();
				client.player.displayClientMessage(Component.literal("§c[SylvaraBot] Emergency Stop via Shortcut (X)!"), false);
			}
		});

		// 4. REGISTRASI COMMAND IN-GAME
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("sylvaragui")
					.executes(context -> {
						Minecraft client = Minecraft.getInstance();
						client.execute(() -> client.setScreen(new SylvaraScreen()));
						return 1;
					}));

			dispatcher.register(ClientCommandManager.literal("stop")
					.executes(context -> {
						MovementController.stop();
						AutoLumberjackController.stopTask();
						context.getSource().sendFeedback(Component.literal("§c[SylvaraBot] Rute dan AFK Farm dihentikan total."));
						return 1;
					}));

			dispatcher.register(ClientCommandManager.literal("nbt")
					.executes(context -> {
						Minecraft client = Minecraft.getInstance();
						client.execute(() -> checkItemInfo(client));
						return 1;
					}));
		});
	}

	public static void checkItemInfo(Minecraft client) {
		if (client.player == null) return;
		ItemStack heldItem = client.player.getMainHandItem();

		if (heldItem.isEmpty()) {
			client.player.displayClientMessage(Component.literal("§c[SylvaraBot] Tangan utama kosong!"), false);
			return;
		}

		String name = heldItem.getHoverName().getString();
		String id = BuiltInRegistries.ITEM.getKey(heldItem.getItem()).toString();
		String components = heldItem.getComponents().toString();

		client.player.displayClientMessage(Component.literal("§e[SylvaraBot INFO] Item: §f" + name), false);
		client.player.displayClientMessage(Component.literal("§7ID: §f" + id + " §7| Count: §f" + heldItem.getCount()), false);

		// Opsional: cetak ringkasan komponen untuk lihat data detail jika diperlukan
		System.out.println("[SylvaraBot NBT Debug] " + name + " -> Components: " + components);
	}
}
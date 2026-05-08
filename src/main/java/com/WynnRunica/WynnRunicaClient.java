package com.WynnRunica;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.net.URI;

public class WynnRunicaClient implements ClientModInitializer {

    public static boolean enabled = true;
    private static KeyBinding toggleKey;

    private static final KeyBinding.Category WR_CATEGORY =
            KeyBinding.Category.create(Identifier.of("WynnRunica"));


    @Override
    public void onInitializeClient() {

        TranslationUpdater.update();
        TranslationPrinter.loadTraslations();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Включить / выключить перевод",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                WR_CATEGORY
        ));


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {

                enabled = !enabled;
                String status = enabled ? "§aвключён" : "§cвыключен";
                if (client.player != null) {
                    client.inGameHud.getChatHud().addMessage(
                            Text.literal("[§3Wynn§fRunica] Перевод " + status)
                    );
                }
            }
        });


        new Thread(VersionChecker::versionCheck).start();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (VersionChecker.hasUpdate && client.player != null) {
                Text msg = Text.literal("[§3Wynn§fRunica] §eНовая версия §a" + VersionChecker.latestVersion + "§e! ")
                        .append(Text.literal("§b[Скачать]")
                                .styled(style -> style.withClickEvent(
                                        new ClickEvent.OpenUrl(URI.create(VersionChecker.latestUrl))
                                )));

                client.inGameHud.getChatHud().addMessage(msg);


            }
        });

    }
}

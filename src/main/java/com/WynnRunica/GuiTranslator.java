package com.WynnRunica;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class GuiTranslator {

    public static void translateStack(ItemStack stack) {
        int key = GuiTranslationCache.keyFor(stack);
        if (!GuiTranslationCache.originals.containsKey(key)) {
            GuiTranslationCache.originals.put(key, stack.copy());
        }

        Text name = stack.get(DataComponentTypes.CUSTOM_NAME);
        boolean isCustom = name != null;
        if (name == null)
            name = stack.get(DataComponentTypes.ITEM_NAME);

        if (name != null) {
            var ex = TextEmojiUtils.extract(name);
            String translated = TranslationPrinter.getGuiTranslation(ex.key);
            if (!translated.equals(ex.key)) {

                Style style = ex.contentStyle != null && ex.contentStyle != Style.EMPTY
                        ? ex.contentStyle
                        : name.getStyle();
                Text newName = TextEmojiUtils.rebuild(translated, ex.icons, style);

                if (isCustom)
                    stack.set(DataComponentTypes.CUSTOM_NAME, newName);
                else
                    stack.set(DataComponentTypes.ITEM_NAME, newName);
            }
        }

        LoreComponent lore = stack.get(DataComponentTypes.LORE);

        if (lore != null) {

            List<Text> newLines = new ArrayList<>();
            boolean changed = false;

            for (Text line : lore.lines()) {
                var ex = TextEmojiUtils.extract(line);
                String translated = TranslationPrinter.getGuiTranslation(ex.key);
                if (!translated.equals(ex.key)) {
                    newLines.add(TextEmojiUtils.rebuild(translated, ex.icons, line.getStyle()));
                    changed = true;

                } else {
                    newLines.add(line);
                }
            }

            if (changed)
                stack.set(DataComponentTypes.LORE, new LoreComponent(newLines));
        }
    }

    public static void refreshOpenScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;
        ScreenHandler handler = client.player.currentScreenHandler;
        if (handler == null)
            return;

        boolean enabled = WynnRunicaClient.enabled;

        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty())
                continue;
            ItemStack original = GuiTranslationCache.originals.get(GuiTranslationCache.keyFor(stack));
            if (original == null)
                continue;

            restoreComponent(stack, original, DataComponentTypes.CUSTOM_NAME);
            restoreComponent(stack, original, DataComponentTypes.ITEM_NAME);
            restoreComponent(stack, original, DataComponentTypes.LORE);

            if (enabled) {
                translateStack(stack);
            }
        }
    }

    private static <T> void restoreComponent(ItemStack stack, ItemStack original,
            ComponentType<T> type) {
        T value = original.get(type);
        if (value != null) {
            stack.set(type, value);
        } else {
            stack.remove(type);
        }
    }
}
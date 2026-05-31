package com.WynnRunica;

import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class GuiTranslationCache {

    public static final Map<Integer, ItemStack> originals = new HashMap<>();

    private static int lastSyncId = -1;

    public static void resetIfSyncIdChanged(int syncId) {
        if (syncId != lastSyncId) {
            originals.clear();
            lastSyncId = syncId;
        }
    }

    public static int keyFor(ItemStack stack) {
        return System.identityHashCode(stack);
    }
}

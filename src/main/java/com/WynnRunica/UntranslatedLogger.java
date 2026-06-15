package com.WynnRunica;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

public class UntranslatedLogger {

    public static volatile boolean ENABLED = false;

    private static Path logFile;
    private static final Set<String> seen = Collections.synchronizedSet(new HashSet<>());
    private static final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private static volatile boolean writerStarted = false;

    private static final Pattern HAS_WORD = Pattern.compile("\\p{L}{2,}");

    static {
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve("WynnRunica");
            Files.createDirectories(dir);
            logFile = dir.resolve("untranslated.txt");

            if (Files.exists(logFile)) {
                for (String line : Files.readAllLines(logFile)) {
                    if (!line.isEmpty()) seen.add(line);
                }
            }

        } catch (IOException e) {
            System.out.println("[WynnRunica] untranslated logger init pizdec: " + e.getMessage());
        }
    }

    public static void log(String text) {
        if (!ENABLED || logFile == null || text == null) return;

        if (containsCyrillic(text)) return;
        String bare = stripDecor(text);
        if (bare.isEmpty()) return;
        if (!HAS_WORD.matcher(bare).find()) return;

        String normalized = text.replaceAll("(?<!§)\\d+[.,/\\d]*", "<num>");

        if (!seen.add(normalized)) return;
        queue.add(normalized);
        ensureWriterStarted();
    }

    public static void setEnabled(boolean on) {
        ENABLED = on;
        if (on) ensureWriterStarted();
    }

    private static void ensureWriterStarted() {

        if (writerStarted) return;
        synchronized (UntranslatedLogger.class) {

            if (writerStarted) return;
            writerStarted = true;
            Thread t = new Thread(UntranslatedLogger::writerLoop, "WynnRunica-UntranslatedWriter");
            t.setDaemon(true);
            t.start();
        }
    }

    private static void writerLoop() {
        while (true) {
            try {
                if (queue.isEmpty()) {
                    Thread.sleep(100);
                    continue;
                }
                if (logFile == null) {
                    queue.clear();
                    continue;
                }
                try (BufferedWriter w = new BufferedWriter(new FileWriter(logFile.toFile(), true))) {
                    String line;
                    while ((line = queue.poll()) != null) {
                        w.write(line);
                        w.newLine();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException e) {
                System.out.println("[WynnRunica] untranslated write fail: " + e.getMessage());
                queue.clear();
            }
        }
    }

    private static String stripDecor(String s) {

        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {

            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) { i++; continue; } // §x - код цвета/стиля
            if (c >= 0xE000 && c <= 0xF8FF) continue;              // PUA-иконки
            sb.append(c);
        }

        return sb.toString().replace("<em>", "").trim();
    }

    private static boolean containsCyrillic(String text) {

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c >= 0x0400 && c <= 0x04FF) {
                return true;
            }
        }

        return false;
    }
}

package com.WynnRunica;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

public class TranslationPrinter {
    public static HashMap<String, String> translations = new HashMap<>();
    public static HashMap<String, String> guiTranslations = new HashMap<>();
    public static final HashMap<String, List<String>> questToKeys = new HashMap<>();
    private static String currentQuest = null;

    public static class GuiPattern {
        public final java.util.regex.Pattern pattern;
        public final String translationTemplate;

        public GuiPattern(java.util.regex.Pattern pattern, String translationTemplate) {
            this.pattern = pattern;
            this.translationTemplate = translationTemplate;
        }
    }

    public static final List<GuiPattern> guiPatterns = new ArrayList<>();

    public static void reload() {
        HashMap<String, String> rawTranslations = TranslationLoader.loadFromConfig();
        guiTranslations = TranslationLoader.loadGuiFromConfig();
        TranslationLoader.keyToQuest.clear();
        questToKeys.clear();

        HashMap<String, String> cleanTranslations = new HashMap<>();
        for (java.util.Map.Entry<String, String> entry : rawTranslations.entrySet()) {
            String rawKey = entry.getKey();
            String cleanKey = rawKey.replace(" ", "").toLowerCase();
            String ruText = entry.getValue();
            cleanTranslations.put(cleanKey, ruText);

            String quest = TranslationLoader.keyToQuest.remove(rawKey);
            if (quest != null) {
                TranslationLoader.keyToQuest.put(cleanKey, quest);
                questToKeys.computeIfAbsent(quest, q -> new ArrayList<>()).add(cleanKey);
            }
        }
        translations = cleanTranslations;

        guiPatterns.clear();
        for (String key : guiTranslations.keySet()) {
            if (!key.contains("<num>")) continue;

            String[] parts = key.split("<num>", -1);
            StringBuilder pb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) pb.append("([+\\-]?\\d+[.,/\\d]*)");
                pb.append(java.util.regex.Pattern.quote(parts[i]));
            }
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pb.toString());
                guiPatterns.add(new GuiPattern(p, guiTranslations.get(key)));
            } catch (Exception e) {
                System.out.println("[WynnRunica] Failed to compile pattern for: " + key);
            }
        }
    }


    public static String getTranslation(String text, boolean updateQuest) {
        String cleanText = text.replace(" ", "").toLowerCase();

        String exact = translations.get(cleanText);
        if (exact != null) {
            if (updateQuest) {
                String newQuest = TranslationLoader.keyToQuest.get(cleanText);
                if (newQuest != null && !newQuest.equals(currentQuest)) {
                    currentQuest = newQuest;
                }
            }
            return exact;
        }


        if (currentQuest == null) return text;
        List<String> keysOfQuest = questToKeys.get(currentQuest);
        if (keysOfQuest == null || keysOfQuest.isEmpty()) return text;

        String bestKey = null;
        double bestScore = 0.0;
        for (String key : keysOfQuest) {
            double score = Epstein.similarity(cleanText, key);
            if (score > bestScore) {
                bestScore = score;
                bestKey = key;
            }
        }
        if (bestScore > 0.82) {
            return translations.get(bestKey);
        }
        return text;
    }

    public static String getGuiTranslation(String text) {
        if (text == null || text.isEmpty()) return text;

        String exact = guiTranslations.get(text);
        if (exact != null) return exact;

        for (GuiPattern gp : guiPatterns) {
            java.util.regex.Matcher m = gp.pattern.matcher(text);
            if (m.matches()) {
                String result = gp.translationTemplate;
                for (int g = 1; g <= m.groupCount(); g++) {
                    result = result.replaceFirst("<num>", java.util.regex.Matcher.quoteReplacement(m.group(g)));
                }
                return result;
            }
        }

        return text;
    }



    /* public static int getTranslationsCount() {
        return translations.size();
    }   забыл зачем )))
    */

}

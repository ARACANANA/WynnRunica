package com.WynnRunica;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class TranslationUpdater {

    private static boolean isUpdate = true;

    public static void update() {

        if (isUpdate == false) return;
        try {
            URL address = new URL("https://api.github.com/repos/Hayoumi/WynnRunica/git/trees/main?recursive=1");

            BufferedReader reader = new BufferedReader(new InputStreamReader(address.openStream()));

            StringBuilder sb = new StringBuilder();
            String files;
            while ((files = reader.readLine()) != null) {
                sb.append(files);
            }

            JsonObject root = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonArray tree = root.getAsJsonArray("tree");
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("WynnRunica");
            Path questsPath = configPath.resolve("quests");
            Path guiPath    = configPath.resolve("gui");

            Files.createDirectories(questsPath);
            Files.createDirectories(guiPath);

            for (int i = 0; i < tree.size(); i++) {

                JsonObject item = tree.get(i).getAsJsonObject();
                String path = item.get("path").getAsString();
                path = path.replace(" ", "%20");
                String type = item.get("type").getAsString();

                Path destDir;
                if (path.startsWith("src/main/resources/quests/") && type.equals("blob")) {
                    destDir = questsPath;
                } else if (path.startsWith("src/main/resources/gui/") && type.equals("blob")) {
                    destDir = guiPath;
                } else {
                    continue;
                }

                URL fileAdress = new URL("https://raw.githubusercontent.com/Hayoumi/WynnRunica/main/" + path);
                BufferedReader Filereader = new BufferedReader(new InputStreamReader(fileAdress.openStream()));

                StringBuilder textInFile = new StringBuilder();
                String text;
                String fileName = path.substring(path.lastIndexOf('/') + 1).replace("%20", " ");

                while ((text = Filereader.readLine()) != null) {
                    textInFile.append(text).append("\n");
                }

                Files.writeString(destDir.resolve(fileName), textInFile);

            }

        } catch (IOException e) {
            System.out.println("Oshibka auto updeyta questov: " + e.getMessage());
        }

    }
}

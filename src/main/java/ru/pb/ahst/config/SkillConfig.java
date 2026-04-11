// SkillConfig.java
package ru.pb.ahst.config;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.screen.skill_tree.SkillData;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class SkillConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;
    private static Map<String, SkillData> skills = new LinkedHashMap<>();

    public static void init(Path configDir) {
        configPath = configDir.resolve(AHSkillTree.MOD_ID).resolve("AHSkills.json");
        loadConfig();
    }

    private static void loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                createDefaultConfig();
            }

            String json = Files.readString(configPath);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray skillsArray = root.getAsJsonArray("skills");

            skills.clear();
            for (JsonElement element : skillsArray) {
                JsonObject obj = element.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String name = obj.get("name").getAsString();
                String description = obj.get("description").getAsString();
                int x = obj.get("x").getAsInt();
                int y = obj.get("y").getAsInt();

                List<String> prerequisites = new ArrayList<>();
                if (obj.has("prerequisites")) {
                    JsonArray prereqArray = obj.getAsJsonArray("prerequisites");
                    for (JsonElement prereq : prereqArray) {
                        prerequisites.add(prereq.getAsString());
                    }
                }

                // Загружаем конфликты
                List<String> conflicts = new ArrayList<>();
                if (obj.has("conflicts")) {
                    JsonArray conflictArray = obj.getAsJsonArray("conflicts");
                    for (JsonElement conflict : conflictArray) {
                        conflicts.add(conflict.getAsString());
                    }
                }

                skills.put(id, new SkillData(id, name, description, x, y, prerequisites, conflicts));
            }

            AHSkillTree.LOGGER.info("Loaded {} skills from config", skills.size());

        } catch (Exception e) {
            AHSkillTree.LOGGER.error("Failed to load skill config", e);
            createDefaultConfig();
        }
    }

    private static void createDefaultConfig() {
        try {
            Files.createDirectories(configPath.getParent());

            JsonObject root = new JsonObject();
            JsonArray skillsArray = new JsonArray();

            // Пример навыков с координатами
            skillsArray.add(createSkillJson(
                    "combat_basic", "Базовый бой", "Увеличивает урон на 5%",
                    0, -100, new ArrayList<>(), new ArrayList<>()
            ));

            skillsArray.add(createSkillJson(
                    "combat_advanced", "Продвинутый бой", "Увеличивает урон на 15%",
                    -80, -20, List.of("combat_basic"), new ArrayList<>()
            ));

            root.add("skills", skillsArray);
            Files.writeString(configPath, GSON.toJson(root));

        } catch (IOException e) {
            AHSkillTree.LOGGER.error("Failed to create default skill config", e);
        }
    }

    private static JsonObject createSkillJson(String id, String name, String description,
                                              int x, int y, List<String> prerequisites,
                                              List<String> conflicts) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("name", name);
        obj.addProperty("description", description);
        obj.addProperty("x", x);
        obj.addProperty("y", y);

        if (!prerequisites.isEmpty()) {
            JsonArray prereqArray = new JsonArray();
            for (String prereq : prerequisites) {
                prereqArray.add(prereq);
            }
            obj.add("prerequisites", prereqArray);
        }

        if (!conflicts.isEmpty()) {
            JsonArray conflictArray = new JsonArray();
            for (String conflict : conflicts) {
                conflictArray.add(conflict);
            }
            obj.add("conflicts", conflictArray);
        }

        return obj;
    }

    public static List<SkillData> getAllSkills() {
        return new ArrayList<>(skills.values());
    }

    public static SkillData getSkill(String id) {
        return skills.get(id);
    }
}
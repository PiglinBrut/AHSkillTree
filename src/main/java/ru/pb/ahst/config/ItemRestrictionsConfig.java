// ItemRestrictionsConfig.java
package ru.pb.ahst.config;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import ru.pb.ahst.AHSkillTree;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ItemRestrictionsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;
    private static final Set<ResourceLocation> BLACKLISTED_ITEMS = new HashSet<>();
    private static final Set<String> BLACKLISTED_TAGS = new HashSet<>();
    private static final Map<String, Set<ResourceLocation>> SKILL_UNLOCKS = new HashMap<>();

    public static void init(Path configDir) {
        configPath = configDir.resolve(AHSkillTree.MOD_ID).resolve("AHItemRestrictions.json");
        loadConfig();
    }

    private static void loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                createDefaultConfig();
            }

            String json = Files.readString(configPath);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            BLACKLISTED_ITEMS.clear();
            BLACKLISTED_TAGS.clear();
            SKILL_UNLOCKS.clear();

            // Загружаем черный список предметов
            if (root.has("blacklisted_items")) {
                JsonArray items = root.getAsJsonArray("blacklisted_items");
                for (JsonElement item : items) {
                    BLACKLISTED_ITEMS.add(ResourceLocation.parse(item.getAsString()));
                }
            }

            // Загружаем черный список тегов
            if (root.has("blacklisted_tags")) {
                JsonArray tags = root.getAsJsonArray("blacklisted_tags");
                for (JsonElement tag : tags) {
                    BLACKLISTED_TAGS.add(tag.getAsString());
                }
            }

            // Загружаем разблокировки по навыкам
            if (root.has("skill_unlocks")) {
                JsonArray unlocks = root.getAsJsonArray("skill_unlocks");
                for (JsonElement unlockElem : unlocks) {
                    JsonObject unlockObj = unlockElem.getAsJsonObject();
                    String skillId = unlockObj.get("skill_id").getAsString();
                    Set<ResourceLocation> items = new HashSet<>();

                    if (unlockObj.has("items")) {
                        JsonArray itemsArray = unlockObj.getAsJsonArray("items");
                        for (JsonElement item : itemsArray) {
                            items.add(ResourceLocation.parse(item.getAsString()));
                        }
                    }

                    SKILL_UNLOCKS.put(skillId, items);
                }
            }

            AHSkillTree.LOGGER.info("Loaded {} blacklisted items, {} blacklisted tags, {} skill unlocks",
                    BLACKLISTED_ITEMS.size(), BLACKLISTED_TAGS.size(), SKILL_UNLOCKS.size());

        } catch (Exception e) {
            AHSkillTree.LOGGER.error("Failed to load item restrictions config", e);
            createDefaultConfig();
        }
    }

    private static void createDefaultConfig() {
        try {
            Files.createDirectories(configPath.getParent());

            JsonObject root = new JsonObject();

            // Черный список предметов
            JsonArray blacklistedItems = new JsonArray();
            blacklistedItems.add("minecraft:iron_sword");
            blacklistedItems.add("minecraft:diamond_sword");
            root.add("blacklisted_items", blacklistedItems);

            // Черный список тегов
            JsonArray blacklistedTags = new JsonArray();
            blacklistedTags.add("minecraft:swords");
            blacklistedTags.add("minecraft:axes");
            root.add("blacklisted_tags", blacklistedTags);

            // Разблокировки по навыкам
            JsonArray skillUnlocks = new JsonArray();

            JsonObject combatBasicUnlock = new JsonObject();
            combatBasicUnlock.addProperty("skill_id", "combat_basic");
            JsonArray combatItems = new JsonArray();
            combatItems.add("minecraft:iron_sword");
            combatBasicUnlock.add("items", combatItems);
            skillUnlocks.add(combatBasicUnlock);

            JsonObject ultimateUnlock = new JsonObject();
            ultimateUnlock.addProperty("skill_id", "ultimate");
            JsonArray ultimateItems = new JsonArray();
            ultimateItems.add("minecraft:diamond_sword");
            ultimateUnlock.add("items", ultimateItems);
            skillUnlocks.add(ultimateUnlock);

            root.add("skill_unlocks", skillUnlocks);

            Files.writeString(configPath, GSON.toJson(root));

        } catch (IOException e) {
            AHSkillTree.LOGGER.error("Failed to create default item restrictions config", e);
        }
    }

    public static boolean isItemBlacklisted(ResourceLocation itemId) {
        return BLACKLISTED_ITEMS.contains(itemId);
    }

    public static boolean isTagBlacklisted(String tagId) {
        return BLACKLISTED_TAGS.contains(tagId);
    }

    public static Set<ResourceLocation> getUnlockedItemsForSkill(String skillId) {
        return SKILL_UNLOCKS.getOrDefault(skillId, Collections.emptySet());
    }

    public static Set<String> getBlacklistedTags() {
        return new HashSet<>(BLACKLISTED_TAGS);
    }
}
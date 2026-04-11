// SkillEffectsConfig.java
package ru.pb.ahst.config;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import ru.pb.ahst.AHSkillTree;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SkillEffectsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;
    private static final Map<String, SkillEffects> SKILL_EFFECTS = new HashMap<>();

    public static void init(Path configDir) {
        configPath = configDir.resolve(AHSkillTree.MOD_ID).resolve("AHSkillEffects.json");
        loadConfig();
    }

    private static void loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                createDefaultConfig();
            }

            String json = Files.readString(configPath);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray effectsArray = root.getAsJsonArray("skill_effects");

            SKILL_EFFECTS.clear();
            for (JsonElement element : effectsArray) {
                JsonObject obj = element.getAsJsonObject();
                String skillId = obj.get("skill_id").getAsString();

                SkillEffects effects = new SkillEffects();

                // Атрибут бонусы
                if (obj.has("attribute_bonuses")) {
                    JsonArray bonuses = obj.getAsJsonArray("attribute_bonuses");
                    for (JsonElement bonusElem : bonuses) {
                        JsonObject bonusObj = bonusElem.getAsJsonObject();
                        String attributeId = bonusObj.get("attribute").getAsString();
                        String operation = bonusObj.get("operation").getAsString();
                        double amount = bonusObj.get("amount").getAsDouble();
                        String name = bonusObj.has("name") ? bonusObj.get("name").getAsString() : skillId + "_bonus";

                        effects.attributeBonuses.add(new AttributeBonus(
                                ResourceLocation.parse(attributeId),
                                AttributeModifier.Operation.valueOf(operation),
                                amount,
                                name
                        ));
                    }
                }

                // Множители атрибутов
                if (obj.has("attribute_multipliers")) {
                    JsonArray multipliers = obj.getAsJsonArray("attribute_multipliers");
                    for (JsonElement multElem : multipliers) {
                        JsonObject multObj = multElem.getAsJsonObject();
                        String attributeId = multObj.get("attribute").getAsString();
                        double multiplier = multObj.get("multiplier").getAsDouble();

                        effects.attributeMultipliers.add(new AttributeMultiplier(
                                ResourceLocation.parse(attributeId),
                                multiplier
                        ));
                    }
                }

                // Разрешенные предметы
                if (obj.has("unlocked_items")) {
                    JsonArray items = obj.getAsJsonArray("unlocked_items");
                    for (JsonElement itemElem : items) {
                        String itemId = itemElem.getAsString();
                        effects.unlockedItems.add(ResourceLocation.parse(itemId));
                    }
                }

                // Запрещенные предметы
                if (obj.has("locked_items")) {
                    JsonArray items = obj.getAsJsonArray("locked_items");
                    for (JsonElement itemElem : items) {
                        String itemId = itemElem.getAsString();
                        effects.lockedItems.add(ResourceLocation.parse(itemId));
                    }
                }

                SKILL_EFFECTS.put(skillId, effects);
            }

            AHSkillTree.LOGGER.info("Loaded effects for {} skills", SKILL_EFFECTS.size());

        } catch (Exception e) {
            AHSkillTree.LOGGER.error("Failed to load skill effects config", e);
            createDefaultConfig();
        }
    }

    private static void createDefaultConfig() {
        try {
            Files.createDirectories(configPath.getParent());

            JsonObject root = new JsonObject();
            JsonArray effectsArray = new JsonArray();

            // Пример эффектов для combat_basic
            JsonObject combatBasic = new JsonObject();
            combatBasic.addProperty("skill_id", "combat_basic");
            JsonArray bonuses = new JsonArray();
            JsonObject damageBonus = new JsonObject();
            damageBonus.addProperty("attribute", "minecraft:generic.attack_damage");
            damageBonus.addProperty("operation", "ADDITION");
            damageBonus.addProperty("amount", 2.0);
            bonuses.add(damageBonus);
            combatBasic.add("attribute_bonuses", bonuses);
            effectsArray.add(combatBasic);

            // Пример эффектов для ultimate
            JsonObject ultimate = new JsonObject();
            ultimate.addProperty("skill_id", "ultimate");
            JsonArray multipliers = new JsonArray();
            JsonObject damageMult = new JsonObject();
            damageMult.addProperty("attribute", "minecraft:generic.attack_damage");
            damageMult.addProperty("multiplier", 1.5);
            multipliers.add(damageMult);
            ultimate.add("attribute_multipliers", multipliers);

            JsonArray unlocked = new JsonArray();
            unlocked.add("minecraft:diamond_sword");
            ultimate.add("unlocked_items", unlocked);
            effectsArray.add(ultimate);

            root.add("skill_effects", effectsArray);
            Files.writeString(configPath, GSON.toJson(root));

        } catch (IOException e) {
            AHSkillTree.LOGGER.error("Failed to create default effects config", e);
        }
    }

    public static SkillEffects getEffects(String skillId) {
        return SKILL_EFFECTS.getOrDefault(skillId, new SkillEffects());
    }

    public static class SkillEffects {
        public List<AttributeBonus> attributeBonuses = new ArrayList<>();
        public List<AttributeMultiplier> attributeMultipliers = new ArrayList<>();
        public List<ResourceLocation> unlockedItems = new ArrayList<>();
        public List<ResourceLocation> lockedItems = new ArrayList<>();
    }

    public static class AttributeBonus {
        public ResourceLocation attribute;
        public AttributeModifier.Operation operation;
        public double amount;
        public String name;

        public AttributeBonus(ResourceLocation attribute, AttributeModifier.Operation operation, double amount, String name) {
            this.attribute = attribute;
            this.operation = operation;
            this.amount = amount;
            this.name = name;
        }
    }

    public static class AttributeMultiplier {
        public ResourceLocation attribute;
        public double multiplier;

        public AttributeMultiplier(ResourceLocation attribute, double multiplier) {
            this.attribute = attribute;
            this.multiplier = multiplier;
        }
    }
}
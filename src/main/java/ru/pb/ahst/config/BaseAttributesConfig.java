// AHBaseAttributesConfig.java
package ru.pb.ahst.config;

import com.google.gson.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import ru.pb.ahst.AHSkillTree;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class BaseAttributesConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;
    private static Map<String, Double> attributes = new LinkedHashMap<>();

    public static void init(Path configDir) {
        configPath = configDir.resolve(AHSkillTree.MOD_ID).resolve("AHBaseAttributes.json");
        loadConfig();
    }

    private static void loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                createDefaultConfig();
            }

            String json = Files.readString(configPath);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject attributesObject = root.getAsJsonObject("attributes");

            attributes.clear();
            for (Map.Entry<String, JsonElement> entry : attributesObject.entrySet()) {
                String attributeId = entry.getKey();
                double baseValue = entry.getValue().getAsDouble();
                attributes.put(attributeId, baseValue);
            }

            AHSkillTree.LOGGER.info("Loaded {} base attributes from config", attributes.size());

        } catch (Exception e) {
            AHSkillTree.LOGGER.error("Failed to load AH Base Attributes config", e);
            createDefaultConfig();
        }
    }

    private static void createDefaultConfig() {
        try {
            Files.createDirectories(configPath.getParent());

            JsonObject root = new JsonObject();
            JsonObject attributesObject = new JsonObject();

            // Стандартные атрибуты с базовыми значениями
            attributesObject.addProperty("minecraft:generic.max_health", 40.0);
            attributesObject.addProperty("minecraft:generic.attack_damage", 3.0);
            attributesObject.addProperty("minecraft:generic.armor", 4.0);
            attributesObject.addProperty("minecraft:generic.armor_toughness", 2.0);
            attributesObject.addProperty("minecraft:generic.movement_speed", 0.12);
            attributesObject.addProperty("minecraft:generic.knockback_resistance", 0.2);

            root.add("attributes", attributesObject);
            Files.writeString(configPath, GSON.toJson(root));

            AHSkillTree.LOGGER.info("Created default AH Base Attributes config at: {}", configPath);

        } catch (IOException e) {
            AHSkillTree.LOGGER.error("Failed to create default AH Base Attributes config", e);
        }
    }

    public static void applyAttributesToPlayer(ServerPlayer player) {
        if (player == null) return;

        boolean isNewPlayer = !player.getPersistentData().contains("ah_attributes_applied");

        if (isNewPlayer) {
            AHSkillTree.LOGGER.info("Applying base attributes to new player: {}", player.getName().getString());

            for (Map.Entry<String, Double> entry : attributes.entrySet()) {
                String attributeId = entry.getKey();
                double baseValue = entry.getValue();

                ResourceLocation attributeKey = ResourceLocation.tryParse(attributeId);
                if (attributeKey == null) {
                    AHSkillTree.LOGGER.warn("Invalid attribute ID format: {}", attributeId);
                    continue;
                }

                Optional<Holder.Reference<Attribute>> attributeHolder = BuiltInRegistries.ATTRIBUTE.getHolder(attributeKey);
                if (attributeHolder.isEmpty()) {
                    AHSkillTree.LOGGER.warn("Unknown attribute: {}", attributeId);
                    continue;
                }

                AttributeInstance attributeInstance = player.getAttribute(attributeHolder.get());

                if (attributeInstance != null) {
                    attributeInstance.setBaseValue(baseValue);
                    AHSkillTree.LOGGER.debug("Set attribute {} to {}", attributeId, baseValue);
                } else {
                    AHSkillTree.LOGGER.warn("Player does not have attribute: {}", attributeId);
                }
            }

            player.getPersistentData().putBoolean("ah_attributes_applied", true);

            player.setHealth(player.getMaxHealth());

            AHSkillTree.LOGGER.info("Successfully applied base attributes to player: {}", player.getName().getString());
        }
    }

    public static void reloadConfig() {
        loadConfig();
    }

    public static Map<String, Double> getAttributes() {
        return new LinkedHashMap<>(attributes);
    }

    public static double getAttributeValue(String attributeId) {
        return attributes.getOrDefault(attributeId, -1.0);
    }

    public static boolean hasAttribute(String attributeId) {
        return attributes.containsKey(attributeId);
    }
}
package ru.pb.ahst.config;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.effects.bonuses.BonusType;
import ru.pb.ahst.effects.bonuses.SkillBonus;
import ru.pb.ahst.effects.conditions.Condition;
import ru.pb.ahst.effects.conditions.ConditionType;

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
        ConditionType.init();
        BonusType.init();
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

                // Атрибут бонусы (старый формат)
                if (obj.has("attribute_bonuses")) {
                    JsonArray bonuses = obj.getAsJsonArray("attribute_bonuses");
                    for (JsonElement bonusElem : bonuses) {
                        JsonObject bonusObj = bonusElem.getAsJsonObject();
                        String attributeId = bonusObj.get("attribute").getAsString();
                        String operationStr = bonusObj.get("operation").getAsString();
                        double amount = bonusObj.get("amount").getAsDouble();
                        String name = bonusObj.has("name") ? bonusObj.get("name").getAsString() : skillId + "_bonus";

                        AttributeModifier.Operation operation = parseOperation(operationStr);

                        effects.attributeBonuses.add(new AttributeBonus(
                                ResourceLocation.parse(attributeId),
                                operation,
                                amount,
                                name
                        ));
                    }
                }

                // Множители атрибутов (старый формат)
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

                // Условные эффекты (НОВЫЙ ФОРМАТ)
                if (obj.has("conditional_effects")) {
                    JsonArray conditionalArray = obj.getAsJsonArray("conditional_effects");
                    for (JsonElement condElem : conditionalArray) {
                        JsonObject condObj = condElem.getAsJsonObject();
                        ConditionalEffect condEffect = parseConditionalEffect(condObj, skillId);
                        if (condEffect != null) {
                            effects.conditionalEffects.add(condEffect);
                        }
                    }
                }

                // НОВЫЕ БОНУСЫ (через систему BonusType)
                if (obj.has("bonuses")) {
                    JsonArray bonusesArray = obj.getAsJsonArray("bonuses");
                    for (JsonElement bonusElem : bonusesArray) {
                        JsonObject bonusObj = bonusElem.getAsJsonObject();
                        SkillBonus bonus = BonusType.create(bonusObj, skillId);
                        if (bonus != null) {
                            effects.bonuses.add(bonus);
                        }
                    }
                }

                // Разрешенные предметы
                if (obj.has("unlocked_items")) {
                    JsonArray items = obj.getAsJsonArray("unlocked_items");
                    for (JsonElement itemElem : items) {
                        effects.unlockedItems.add(ResourceLocation.parse(itemElem.getAsString()));
                    }
                }

                // Разблокировка предметов по тегам
                if (obj.has("unlocked_item_tags")) {
                    JsonArray tagsArray = obj.getAsJsonArray("unlocked_item_tags");
                    for (JsonElement tagElem : tagsArray) {
                        String tagStr = tagElem.getAsString();
                        if (tagStr.startsWith("#")) tagStr = tagStr.substring(1);
                        TagKey<Item> tag = ItemTags.create(ResourceLocation.parse(tagStr));
                        effects.unlockedItemTags.add(tag);
                    }
                }

                // Разблокировка блоков
                if (obj.has("unlocked_blocks")) {
                    JsonArray blocks = obj.getAsJsonArray("unlocked_blocks");
                    for (JsonElement blockElem : blocks) {
                        effects.unlockedBlocks.add(ResourceLocation.parse(blockElem.getAsString()));
                    }
                }

                // Разблокировка блоков по тегам
                if (obj.has("unlocked_block_tags")) {
                    JsonArray tagsArray = obj.getAsJsonArray("unlocked_block_tags");
                    for (JsonElement tagElem : tagsArray) {
                        String tagStr = tagElem.getAsString();
                        if (tagStr.startsWith("#")) tagStr = tagStr.substring(1);
                        TagKey<Block> tag = BlockTags.create(ResourceLocation.parse(tagStr));
                        effects.unlockedBlockTags.add(tag);
                    }
                }

                // Запрещенные предметы
                if (obj.has("locked_items")) {
                    JsonArray items = obj.getAsJsonArray("locked_items");
                    for (JsonElement itemElem : items) {
                        effects.lockedItems.add(ResourceLocation.parse(itemElem.getAsString()));
                    }
                }

                // Блокировка предметов по тегам
                if (obj.has("locked_item_tags")) {
                    JsonArray tagsArray = obj.getAsJsonArray("locked_item_tags");
                    for (JsonElement tagElem : tagsArray) {
                        String tagStr = tagElem.getAsString();
                        if (tagStr.startsWith("#")) tagStr = tagStr.substring(1);
                        TagKey<Item> tag = ItemTags.create(ResourceLocation.parse(tagStr));
                        effects.lockedItemTags.add(tag);
                    }
                }

                // Блокировка блоков
                if (obj.has("locked_blocks")) {
                    JsonArray blocks = obj.getAsJsonArray("locked_blocks");
                    for (JsonElement blockElem : blocks) {
                        effects.lockedBlocks.add(ResourceLocation.parse(blockElem.getAsString()));
                    }
                }

                // Блокировка блоков по тегам
                if (obj.has("locked_block_tags")) {
                    JsonArray tagsArray = obj.getAsJsonArray("locked_block_tags");
                    for (JsonElement tagElem : tagsArray) {
                        String tagStr = tagElem.getAsString();
                        if (tagStr.startsWith("#")) tagStr = tagStr.substring(1);
                        TagKey<Block> tag = BlockTags.create(ResourceLocation.parse(tagStr));
                        effects.lockedBlockTags.add(tag);
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

            // Пример с новыми бонусами
            JsonObject vampireSkill = new JsonObject();
            vampireSkill.addProperty("skill_id", "vampiric_strike");
            JsonArray bonuses = new JsonArray();

            JsonObject lifesteal = new JsonObject();
            lifesteal.addProperty("type", "ahst:lifesteal");
            lifesteal.addProperty("percentage", 0.15);
            bonuses.add(lifesteal);

            vampireSkill.add("bonuses", bonuses);

            JsonObject conditional = new JsonObject();
            conditional.addProperty("type", "ahst:health_percentage");
            conditional.addProperty("min", 30);

            JsonObject conditionalEffect = new JsonObject();
            conditionalEffect.add("condition", conditional);
            JsonArray condBonuses = new JsonArray();
            JsonObject critBonus = new JsonObject();
            critBonus.addProperty("type", "ahst:crit_chance");
            critBonus.addProperty("chance", 0.30);
            condBonuses.add(critBonus);
            conditionalEffect.add("bonuses", condBonuses);

            JsonArray conditionalEffects = new JsonArray();
            conditionalEffects.add(conditionalEffect);
            vampireSkill.add("conditional_effects", conditionalEffects);

            effectsArray.add(vampireSkill);

            root.add("skill_effects", effectsArray);
            Files.writeString(configPath, GSON.toJson(root));

        } catch (IOException e) {
            AHSkillTree.LOGGER.error("Failed to create default effects config", e);
        }
    }

    private static AttributeModifier.Operation parseOperation(String operationStr) {
        try {
            return AttributeModifier.Operation.valueOf(operationStr);
        } catch (IllegalArgumentException e) {
            return switch (operationStr.toUpperCase()) {
                case "ADDITION" -> AttributeModifier.Operation.ADD_VALUE;
                case "MULTIPLY_BASE" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                case "MULTIPLY_TOTAL" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                default -> AttributeModifier.Operation.ADD_VALUE;
            };
        }
    }

    private static ConditionalEffect parseConditionalEffect(JsonObject condObj, String skillId) {
        JsonObject conditionObj = condObj.getAsJsonObject("condition");
        if (conditionObj == null) return null;

        Condition condition = ConditionType.create(conditionObj);
        if (condition == null) return null;

        ConditionalEffect effect = new ConditionalEffect(condition);

        // Старый формат (attribute_bonuses)
        if (condObj.has("attribute_bonuses")) {
            JsonArray bonuses = condObj.getAsJsonArray("attribute_bonuses");
            for (JsonElement bonusElem : bonuses) {
                JsonObject bonusObj = bonusElem.getAsJsonObject();
                String attributeId = bonusObj.get("attribute").getAsString();
                String operationStr = bonusObj.get("operation").getAsString();
                double amount = bonusObj.get("amount").getAsDouble();
                String name = bonusObj.has("name") ? bonusObj.get("name").getAsString() : skillId + "_conditional_bonus";

                effect.attributeBonuses.add(new AttributeBonus(
                        ResourceLocation.parse(attributeId),
                        parseOperation(operationStr),
                        amount,
                        name
                ));
            }
        }

        // Старый формат (attribute_multipliers)
        if (condObj.has("attribute_multipliers")) {
            JsonArray multipliers = condObj.getAsJsonArray("attribute_multipliers");
            for (JsonElement multElem : multipliers) {
                JsonObject multObj = multElem.getAsJsonObject();
                String attributeId = multObj.get("attribute").getAsString();
                double multiplier = multObj.get("multiplier").getAsDouble();

                effect.attributeMultipliers.add(new AttributeMultiplier(
                        ResourceLocation.parse(attributeId),
                        multiplier
                ));
            }
        }

        // Новый формат (bonuses)
        if (condObj.has("bonuses")) {
            JsonArray bonusesArray = condObj.getAsJsonArray("bonuses");
            for (JsonElement bonusElem : bonusesArray) {
                JsonObject bonusObj = bonusElem.getAsJsonObject();
                SkillBonus bonus = BonusType.create(bonusObj, skillId);
                if (bonus != null) {
                    effect.bonuses.add(bonus);
                }
            }
        }

        return effect;
    }

    public static SkillEffects getEffects(String skillId) {
        return SKILL_EFFECTS.getOrDefault(skillId, new SkillEffects());
    }

    public static class SkillEffects {
        public List<AttributeBonus> attributeBonuses = new ArrayList<>();
        public List<AttributeMultiplier> attributeMultipliers = new ArrayList<>();
        public List<ConditionalEffect> conditionalEffects = new ArrayList<>();
        public List<SkillBonus> bonuses = new ArrayList<>(); // НОВОЕ

        public List<ResourceLocation> unlockedItems = new ArrayList<>();
        public List<ResourceLocation> unlockedBlocks = new ArrayList<>();

        public List<TagKey<Item>> unlockedItemTags = new ArrayList<>();
        public List<TagKey<Block>> unlockedBlockTags = new ArrayList<>();

        public List<ResourceLocation> lockedItems = new ArrayList<>();
        public List<ResourceLocation> lockedBlocks = new ArrayList<>();

        public List<TagKey<Item>> lockedItemTags = new ArrayList<>();
        public List<TagKey<Block>> lockedBlockTags = new ArrayList<>();
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

    public static class ConditionalEffect {
        public Condition condition;
        public List<AttributeBonus> attributeBonuses = new ArrayList<>();
        public List<AttributeMultiplier> attributeMultipliers = new ArrayList<>();
        public List<SkillBonus> bonuses = new ArrayList<>(); // НОВОЕ

        public ConditionalEffect(Condition condition) {
            this.condition = condition;
        }
    }
}
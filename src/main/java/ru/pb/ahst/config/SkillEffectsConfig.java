// SkillEffectsConfig.java
package ru.pb.ahst.config;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.level.block.Block;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.effects.ConditionContext;

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
                        String operationStr = bonusObj.get("operation").getAsString();
                        double amount = bonusObj.get("amount").getAsDouble();
                        String name = bonusObj.has("name") ? bonusObj.get("name").getAsString() : skillId + "_bonus";

                        AttributeModifier.Operation operation;
                        try {
                            operation = AttributeModifier.Operation.valueOf(operationStr);
                        } catch (IllegalArgumentException e) {
                            // Поддержка старых названий
                            operation = switch (operationStr.toUpperCase()) {
                                case "ADDITION" -> AttributeModifier.Operation.ADD_VALUE;
                                case "MULTIPLY_BASE" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                                case "MULTIPLY_TOTAL" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                                default -> AttributeModifier.Operation.ADD_VALUE;
                            };
                            AHSkillTree.LOGGER.warn("Old operation '{}' converted to {}", operationStr, operation);
                        }

                        effects.attributeBonuses.add(new AttributeBonus(
                                ResourceLocation.parse(attributeId),
                                operation,
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

                // Условные эффекты
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

                // Разрешенные предметы
                if (obj.has("unlocked_items")) {
                    JsonArray items = obj.getAsJsonArray("unlocked_items");
                    for (JsonElement itemElem : items) {
                        String itemId = itemElem.getAsString();
                        effects.unlockedItems.add(ResourceLocation.parse(itemId));
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
                        String itemId = itemElem.getAsString();
                        effects.lockedItems.add(ResourceLocation.parse(itemId));
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

            // Пример эффектов для combat_basic
            JsonObject combatBasic = new JsonObject();
            combatBasic.addProperty("skill_id", "combat_basic");
            JsonArray bonuses = new JsonArray();
            JsonObject damageBonus = new JsonObject();
            damageBonus.addProperty("attribute", "minecraft:generic.attack_damage");
            damageBonus.addProperty("operation", "ADD_VALUE");
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

        String type = conditionObj.get("type").getAsString();
        Condition condition = switch (type) {
            case "health_percentage" -> {
                double min = conditionObj.has("min") ? conditionObj.get("min").getAsDouble() : 0;
                double max = conditionObj.has("max") ? conditionObj.get("max").getAsDouble() : 100;
                yield new HealthPercentageCondition(min, max);
            }
            case "health_absolute" -> {
                double min = conditionObj.has("min") ? conditionObj.get("min").getAsDouble() : 0;
                double max = conditionObj.has("max") ? conditionObj.get("max").getAsDouble() : Double.MAX_VALUE;
                yield new HealthAbsoluteCondition(min, max);
            }
            case "food_percentage" -> {
                double min = conditionObj.has("min") ? conditionObj.get("min").getAsDouble() : 0;
                double max = conditionObj.has("max") ? conditionObj.get("max").getAsDouble() : 100;
                yield new FoodPercentageCondition(min, max);
            }
            case "food_absolute" -> {
                int min = conditionObj.has("min") ? conditionObj.get("min").getAsInt() : 0;
                int max = conditionObj.has("max") ? conditionObj.get("max").getAsInt() : 20;
                yield new FoodAbsoluteCondition(min, max);
            }
            case "on_fire" -> new OnFireCondition();
            case "sprinting" -> new SprintingCondition();
            case "in_water" -> new InWaterCondition();
            case "in_lava" -> new InLavaCondition();
            case "time_of_day" -> {
                long min = conditionObj.has("min") ? conditionObj.get("min").getAsLong() : 0;
                long max = conditionObj.has("max") ? conditionObj.get("max").getAsLong() : 24000;
                yield new TimeOfDayCondition(min, max);
            }
            case "experience_levels" -> {
                int min = conditionObj.has("min") ? conditionObj.get("min").getAsInt() : 0;
                int max = conditionObj.has("max") ? conditionObj.get("max").getAsInt() : Integer.MAX_VALUE;
                yield new ExperienceLevelsCondition(min, max);
            }
            case "distance_to_target" -> {
                double min = conditionObj.has("min") ? conditionObj.get("min").getAsDouble() : 0;
                double max = conditionObj.has("max") ? conditionObj.get("max").getAsDouble() : Double.MAX_VALUE;
                yield new DistanceToTargetCondition(min, max);
            }
            default -> {
                AHSkillTree.LOGGER.warn("Unknown condition type: {}", type);
                yield null;
            }
        };

        if (condition == null) return null;

        ConditionalEffect effect = new ConditionalEffect(condition);

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

        return effect;
    }

    public interface Condition {
        boolean test(ConditionContext context);
    }

    public static class HealthPercentageCondition implements Condition {
        public final double min;
        public final double max;

        public HealthPercentageCondition(double min, double max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean test(ConditionContext context) {
            float percentage = context.player.getHealth() / context.player.getMaxHealth() * 100;
            return percentage >= min && percentage <= max;
        }
    }

    public static class HealthAbsoluteCondition implements Condition {
        public final double min;
        public final double max;

        public HealthAbsoluteCondition(double min, double max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean test(ConditionContext context) {
            float health = context.player.getHealth();
            return health >= min && health <= max;
        }
    }

    public static class FoodPercentageCondition implements Condition {
        public final double min;
        public final double max;

        public FoodPercentageCondition(double min, double max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean test(ConditionContext context) {
            float percentage = context.player.getFoodData().getFoodLevel() / 20f * 100;
            return percentage >= min && percentage <= max;
        }
    }

    public static class FoodAbsoluteCondition implements Condition {
        public final int min;
        public final int max;

        public FoodAbsoluteCondition(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean test(ConditionContext context) {
            int food = context.player.getFoodData().getFoodLevel();
            return food >= min && food <= max;
        }
    }

    public static class OnFireCondition implements Condition {
        @Override
        public boolean test(ConditionContext context) {
            return context.player.isOnFire();
        }
    }

    public static class SprintingCondition implements Condition {
        @Override
        public boolean test(ConditionContext context) {
            return context.player.isSprinting();
        }
    }

    public static class InWaterCondition implements Condition {
        @Override
        public boolean test(ConditionContext context) {
            return context.player.isInWater();
        }
    }

    public static class InLavaCondition implements Condition {
        @Override
        public boolean test(ConditionContext context) {
            return context.player.isInLava();
        }
    }

    public static class TimeOfDayCondition implements Condition {
        public final long min;
        public final long max;

        public TimeOfDayCondition(long min, long max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean test(ConditionContext context) {
            long time = context.player.level().getDayTime() % 24000;
            if (min <= max) {
                return time >= min && time <= max;
            } else {
                return time >= min || time <= max;
            }
        }
    }

    public static class ExperienceLevelsCondition implements Condition {
        public final int min;
        public final int max;

        public ExperienceLevelsCondition(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean test(ConditionContext context) {
            int levels = context.player.experienceLevel;
            return levels >= min && levels <= max;
        }
    }

    public static class DistanceToTargetCondition implements Condition {
        public final double min;
        public final double max;

        public DistanceToTargetCondition(double min, double max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean test(ConditionContext context) {
            if (context.target == null) return false;
            double distance = context.player.distanceTo(context.target);
            return distance >= min && distance <= max;
        }
    }

    public static class SkillEffects {
        public List<AttributeBonus> attributeBonuses = new ArrayList<>();
        public List<AttributeMultiplier> attributeMultipliers = new ArrayList<>();
        public List<ConditionalEffect> conditionalEffects = new ArrayList<>();

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

        public ConditionalEffect(Condition condition) {
            this.condition = condition;
        }
    }
}
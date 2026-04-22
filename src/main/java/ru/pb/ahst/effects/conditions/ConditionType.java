package ru.pb.ahst.effects.conditions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import ru.pb.ahst.AHSkillTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ConditionType {
    private static final Map<ResourceLocation, Function<JsonObject, Condition>> REGISTRY = new HashMap<>();

    public static void register(ResourceLocation id, Function<JsonObject, Condition> factory) {
        REGISTRY.put(id, factory);
        AHSkillTree.LOGGER.debug("Registered condition type: {}", id);
    }

    public static Condition create(JsonObject json) {
        if (!json.has("type")) {
            AHSkillTree.LOGGER.warn("Condition missing 'type' field");
            return null;
        }

        String typeStr = json.get("type").getAsString();
        ResourceLocation typeId = ResourceLocation.tryParse(typeStr);
        if (typeId == null) {
            AHSkillTree.LOGGER.warn("Invalid condition type format: {}", typeStr);
            return null;
        }

        Function<JsonObject, Condition> factory = REGISTRY.get(typeId);
        if (factory == null) {
            AHSkillTree.LOGGER.warn("Unknown condition type: {}", typeId);
            return null;
        }

        return factory.apply(json);
    }

    public static void init() {
        // ===== Базовые условия (из вашего кода) =====
        register(ResourceLocation.parse("ahst:health_percentage"), json -> {
            double min = json.has("min") ? json.get("min").getAsDouble() : 0;
            double max = json.has("max") ? json.get("max").getAsDouble() : 100;
            return new HealthPercentageCondition(min, max);
        });

        register(ResourceLocation.parse("ahst:health_absolute"), json -> {
            double min = json.has("min") ? json.get("min").getAsDouble() : 0;
            double max = json.has("max") ? json.get("max").getAsDouble() : Double.MAX_VALUE;
            return new HealthAbsoluteCondition(min, max);
        });

        register(ResourceLocation.parse("ahst:food_percentage"), json -> {
            double min = json.has("min") ? json.get("min").getAsDouble() : 0;
            double max = json.has("max") ? json.get("max").getAsDouble() : 100;
            return new FoodPercentageCondition(min, max);
        });

        register(ResourceLocation.parse("ahst:food_absolute"), json -> {
            int min = json.has("min") ? json.get("min").getAsInt() : 0;
            int max = json.has("max") ? json.get("max").getAsInt() : 20;
            return new FoodAbsoluteCondition(min, max);
        });

        register(ResourceLocation.parse("ahst:has_skill"), json -> {
            String skillId = json.get("skill").getAsString();
            return new SkillLearnedCondition(skillId);
        });

        register(ResourceLocation.parse("ahst:lacks_skill"), json -> {
            String skillId = json.get("skill").getAsString();
            return new SkillNotCondition(new SkillLearnedCondition(skillId));
        });

        register(ResourceLocation.parse("ahst:on_fire"), json -> new OnFireCondition());
        register(ResourceLocation.parse("ahst:sprinting"), json -> new SprintingCondition());
        register(ResourceLocation.parse("ahst:in_water"), json -> new InWaterCondition());
        register(ResourceLocation.parse("ahst:in_lava"), json -> new InLavaCondition());

        register(ResourceLocation.parse("ahst:time_of_day"), json -> {
            long min = json.has("min") ? json.get("min").getAsLong() : 0;
            long max = json.has("max") ? json.get("max").getAsLong() : 24000;
            return new TimeOfDayCondition(min, max);
        });

        register(ResourceLocation.parse("ahst:experience_levels"), json -> {
            int min = json.has("min") ? json.get("min").getAsInt() : 0;
            int max = json.has("max") ? json.get("max").getAsInt() : Integer.MAX_VALUE;
            return new ExperienceLevelsCondition(min, max);
        });

        register(ResourceLocation.parse("ahst:distance_to_target"), json -> {
            double min = json.has("min") ? json.get("min").getAsDouble() : 0;
            double max = json.has("max") ? json.get("max").getAsDouble() : Double.MAX_VALUE;
            return new DistanceToTargetCondition(min, max);
        });

        // ===== НОВЫЕ УСЛОВИЯ =====
        register(ResourceLocation.parse("ahst:in_biome"), json -> {
            String biomeId = json.get("biome").getAsString();
            return new InBiomeCondition(biomeId);
        });

        register(ResourceLocation.parse("ahst:in_dimension"), json -> {
            String dimensionId = json.get("dimension").getAsString();
            return new InDimensionCondition(dimensionId);
        });

        register(ResourceLocation.parse("ahst:enemies_around"), json -> {
            double radius = json.has("radius") ? json.get("radius").getAsDouble() : 10.0;
            int min = json.has("min") ? json.get("min").getAsInt() : 1;
            int max = json.has("max") ? json.get("max").getAsInt() : Integer.MAX_VALUE;
            boolean aggressiveOnly = json.has("aggressive_only") ? json.get("aggressive_only").getAsBoolean() : true;
            return new EnemiesAroundCondition(radius, min, max, aggressiveOnly);
        });

        register(ResourceLocation.parse("ahst:target_type"), json -> {
            boolean isPlayer = json.has("is_player") && json.get("is_player").getAsBoolean();
            boolean isMonster = json.has("is_monster") && json.get("is_monster").getAsBoolean();
            List<EntityType<?>> types = new ArrayList<>();
            if (json.has("types")) {
                JsonArray typesArray = json.getAsJsonArray("types");
                for (JsonElement elem : typesArray) {
                    String typeStr = elem.getAsString();
                    EntityType.byString(typeStr).ifPresent(types::add);
                }
            }
            return new TargetTypeCondition(isPlayer, isMonster, types);
        });

        register(ResourceLocation.parse("ahst:is_sneaking"), json -> new IsSneakingCondition());

        register(ResourceLocation.parse("ahst:has_status_effect"), json -> {
            String effectId = json.get("effect").getAsString();
            int amplifier = json.has("amplifier") ? json.get("amplifier").getAsInt() : -1;
            return new HasStatusEffectCondition(effectId, amplifier);
        });

        register(ResourceLocation.parse("ahst:is_raining"), json -> new IsRainingCondition());
        register(ResourceLocation.parse("ahst:is_thundering"), json -> new IsThunderingCondition());

        register(ResourceLocation.parse("ahst:below_sea_level"), json -> new BelowSeaLevelCondition());
        register(ResourceLocation.parse("ahst:above_cloud_level"), json -> new AboveCloudLevelCondition());

        register(ResourceLocation.parse("ahst:is_on_ground"), json -> new IsOnGroundCondition());
        register(ResourceLocation.parse("ahst:is_falling"), json -> new IsFallingCondition());

        // ===== ЛОГИЧЕСКИЕ ОПЕРАТОРЫ =====
        register(ResourceLocation.parse("ahst:and"), json -> {
            JsonArray conditionsArray = json.getAsJsonArray("conditions");
            List<Condition> conditions = new ArrayList<>();
            for (JsonElement elem : conditionsArray) {
                Condition cond = create(elem.getAsJsonObject());
                if (cond != null) conditions.add(cond);
            }
            return new SkillAndCondition(conditions);
        });

        register(ResourceLocation.parse("ahst:or"), json -> {
            JsonArray conditionsArray = json.getAsJsonArray("conditions");
            List<Condition> conditions = new ArrayList<>();
            for (JsonElement elem : conditionsArray) {
                Condition cond = create(elem.getAsJsonObject());
                if (cond != null) conditions.add(cond);
            }
            return new SkillOrCondition(conditions);
        });

        register(ResourceLocation.parse("ahst:not"), json -> {
            Condition condition = create(json.getAsJsonObject("condition"));
            return new SkillNotCondition(condition);
        });
    }
}
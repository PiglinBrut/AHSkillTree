package ru.pb.ahst.effects.bonuses;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import ru.pb.ahst.AHSkillTree;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class BonusType {
    private static final Map<ResourceLocation, BiFunction<JsonObject, String, SkillBonus>> REGISTRY = new HashMap<>();

    public static void register(ResourceLocation id, BiFunction<JsonObject, String, SkillBonus> factory) {
        REGISTRY.put(id, factory);
        AHSkillTree.LOGGER.debug("Registered bonus type: {}", id);
    }

    public static SkillBonus create(JsonObject json, String skillId) {
        if (!json.has("type")) {
            AHSkillTree.LOGGER.warn("Bonus missing 'type' field");
            return null;
        }

        String typeStr = json.get("type").getAsString();
        ResourceLocation typeId = ResourceLocation.tryParse(typeStr);
        if (typeId == null) {
            AHSkillTree.LOGGER.warn("Invalid bonus type format: {}", typeStr);
            return null;
        }

        var factory = REGISTRY.get(typeId);
        if (factory == null) {
            AHSkillTree.LOGGER.warn("Unknown bonus type: {}", typeId);
            return null;
        }

        return factory.apply(json, skillId);
    }

    public static void init() {
        register(ResourceLocation.parse("ahst:lifesteal"), (json, skillId) -> {
            double percentage = json.get("percentage").getAsDouble();
            return new LifestealBonus(percentage, skillId);
        });

        register(ResourceLocation.parse("ahst:crit_chance"), (json, skillId) -> {
            double chance = json.get("chance").getAsDouble();
            double damageMultiplier = json.has("damage_multiplier") ? json.get("damage_multiplier").getAsDouble() : 1.5;
            return new CritChanceBonus(chance, damageMultiplier, skillId);
        });

        register(ResourceLocation.parse("ahst:mining_speed"), (json, skillId) -> {
            double multiplier = json.get("multiplier").getAsDouble();
            return new MiningSpeedBonus(multiplier, skillId);
        });

        register(ResourceLocation.parse("ahst:jump_power"), (json, skillId) -> {
            double bonus = json.get("bonus").getAsDouble();
            return new JumpPowerBonus(bonus, skillId);
        });

        register(ResourceLocation.parse("ahst:fall_damage_reduction"), (json, skillId) -> {
            double reduction = json.get("reduction").getAsDouble();
            return new FallDamageReductionBonus(reduction, skillId);
        });
    }
}
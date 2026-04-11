package ru.pb.ahst.effects;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;
import ru.pb.ahst.config.ItemRestrictionsConfig;
import ru.pb.ahst.config.SkillEffectsConfig;
import ru.pb.ahst.data.PlayerSkillData;

import java.util.*;

public class SkillEffectsManager {

    public static void applyAllSkillEffects(Player player, PlayerSkillData skillData) {
        clearAllSkillModifiers(player);

        for (String skillId : skillData.getLearnedSkills()) {
            applySkillEffects(player, skillId);
        }

        applyMultipliers(player, skillData);
    }

    private static void applySkillEffects(Player player, String skillId) {
        SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);

        for (SkillEffectsConfig.AttributeBonus bonus : effects.attributeBonuses) {
            Optional<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getOptional(bonus.attribute);
            if (attribute.isPresent()) {
                AttributeInstance instance = player.getAttribute((Holder<Attribute>) attribute.get());
                if (instance != null) {
                    AttributeModifier modifier = new AttributeModifier(
                            ResourceLocation.parse(bonus.name),
                            bonus.amount,
                            bonus.operation
                    );
                    instance.addPermanentModifier(modifier);
                }
            }
        }
    }

    private static void applyMultipliers(Player player, PlayerSkillData skillData) {
        Map<ResourceLocation, Double> multipliers = new HashMap<>();

        for (String skillId : skillData.getLearnedSkills()) {
            SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);
            for (SkillEffectsConfig.AttributeMultiplier mult : effects.attributeMultipliers) {
                multipliers.merge(mult.attribute, mult.multiplier, (a, b) -> a * b);
            }
        }

        for (Map.Entry<ResourceLocation, Double> entry : multipliers.entrySet()) {
            Optional<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getOptional(entry.getKey());
            if (attribute.isPresent()) {
                AttributeInstance instance = player.getAttribute((Holder<Attribute>) attribute.get());
                if (instance != null) {
                    double baseValue = instance.getBaseValue();
                    instance.setBaseValue(baseValue * entry.getValue());
                }
            }
        }
    }

    private static void clearAllSkillModifiers(Player player) {
        for (Attribute attribute : BuiltInRegistries.ATTRIBUTE) {
            AttributeInstance instance = player.getAttribute((Holder<Attribute>) attribute);
            if (instance != null) {
                instance.getModifiers().forEach(modifier -> {
                    if (modifier.id().getNamespace().equals("ahskilltree")) {
                        instance.removeModifier(modifier.id());
                    }
                });
            }
        }
    }

    public static boolean canUseItem(Player player, ItemStack itemStack, PlayerSkillData skillData) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        if (itemId == null) return true;

        if (ItemRestrictionsConfig.isItemBlacklisted(itemId)) {
            boolean hasUnlock = false;
            for (String skillId : skillData.getLearnedSkills()) {
                if (ItemRestrictionsConfig.getUnlockedItemsForSkill(skillId).contains(itemId)) {
                    hasUnlock = true;
                    break;
                }
            }
            if (!hasUnlock) return false;
        }

        return true;
    }
}
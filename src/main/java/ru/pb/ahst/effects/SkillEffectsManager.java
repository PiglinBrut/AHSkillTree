package ru.pb.ahst.effects;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.config.BlockedAction;
import ru.pb.ahst.config.ItemRestrictionsConfig;
import ru.pb.ahst.config.SkillEffectsConfig;
import ru.pb.ahst.data.PlayerSkillData;
import ru.pb.ahst.data.SkillDataAttachments;

import java.util.*;

public class SkillEffectsManager {
    private static final Set<BlockedAction> DEFAULT_ITEM_LOCKED_ACTIONS = Set.of(
            BlockedAction.ATTACK_ENTITY,
            BlockedAction.RIGHT_CLICK,
            BlockedAction.BREAK_BLOCK_BY_ITEM,
            BlockedAction.PLACE_BLOCK,
            BlockedAction.UNPREPAREDNESS_FOR_WEAPON,
            BlockedAction.UNPREPAREDNESS_FOR_ARMOR
    );

    private static final Set<BlockedAction> DEFAULT_BLOCK_LOCKED_ACTIONS = Set.of(
            BlockedAction.INTERACT_BLOCK
    );

    private static final Map<UUID, Map<String, List<AttributeModifier>>> ACTIVE_CONDITIONAL_MODIFIERS = new HashMap<>();

    public static void applyAllSkillEffects(Player player, PlayerSkillData skillData) {
        if (player == null) return;

        clearAllSkillModifiers(player);
        clearAllConditionalModifiers(player);

        ItemRestrictionsConfig.clearTempRestrictions();

        for (String skillId : skillData.getLearnedSkills()) {
            applySkillRestrictions(skillId);
            applySkillEffects(player, skillId);
            applyMultipliers(player, skillId);
        }
    }

    private static void applySkillRestrictions(String skillId) {
        SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);

        // Заблокированные предметы
        for (ResourceLocation itemId : effects.lockedItems) {
            ItemRestrictionsConfig.addTempItemRestriction(itemId, DEFAULT_ITEM_LOCKED_ACTIONS);
        }

        // Заблокированные теги предметов
        for (TagKey<Item> tag : effects.lockedItemTags) {
            ItemRestrictionsConfig.addTempItemTagRestriction(tag, DEFAULT_ITEM_LOCKED_ACTIONS);
        }

        // Заблокированные блоки
        for (ResourceLocation blockId : effects.lockedBlocks) {
            ItemRestrictionsConfig.addTempBlockRestriction(blockId, DEFAULT_BLOCK_LOCKED_ACTIONS);
        }

        // Заблокированные теги блоков
        for (TagKey<Block> tag : effects.lockedBlockTags) {
            ItemRestrictionsConfig.addTempBlockTagRestriction(tag, DEFAULT_BLOCK_LOCKED_ACTIONS);
        }
    }

    private static void applySkillEffects(Player player, String skillId) {
        SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);

        for (SkillEffectsConfig.AttributeBonus bonus : effects.attributeBonuses) {
            Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(bonus.attribute);

            if (holder.isPresent()) {
                AttributeInstance instance = player.getAttribute(holder.get());
                if (instance != null) {
                    AttributeModifier modifier = new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, bonus.name),
                            bonus.amount,
                            bonus.operation
                    );
                    instance.addPermanentModifier(modifier);
                }
            } else {
                AHSkillTree.LOGGER.warn("Attribute not found: {}", bonus.attribute);
            }
        }
    }

    private static void applyMultipliers(Player player, String skillId) {
        SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);

        for (SkillEffectsConfig.AttributeMultiplier mult : effects.attributeMultipliers) {
            Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(mult.attribute);

            if (holder.isPresent()) {
                AttributeInstance instance = player.getAttribute(holder.get());
                if (instance != null) {
                    double bonus = mult.multiplier - 1.0;

                    AttributeModifier modifier = new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID,
                                    "multiplier_" + skillId + "_" + mult.attribute.getPath().replace("/", "_")),
                            bonus,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    );
                    instance.addPermanentModifier(modifier);
                }
            } else {
                AHSkillTree.LOGGER.warn("Attribute not found for multiplier: {}", mult.attribute);
            }
        }

    }

    private static void clearAllSkillModifiers(Player player) {
        for (Holder.Reference<Attribute> holder : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
            AttributeInstance instance = player.getAttribute(holder);
            if (instance != null) {
                List<AttributeModifier> toRemove = new ArrayList<>();

                for (AttributeModifier modifier : instance.getModifiers()) {
                    if (modifier.id() != null && modifier.id().toString().startsWith(AHSkillTree.MOD_ID + ":")) {
                        toRemove.add(modifier);
                    }
                }

                for (AttributeModifier modifier : toRemove) {
                    instance.removeModifier(modifier.id());
                }
            }
        }
    }

    public static void updateConditionalEffects(Player player) {
        if (player == null || player.level().isClientSide) return;

        PlayerSkillData skillData = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
        ConditionContext context = new ConditionContext(player, getCurrentTarget(player));

        Map<String, List<AttributeModifier>> activeModifiers = ACTIVE_CONDITIONAL_MODIFIERS
                .computeIfAbsent(player.getUUID(), k -> new HashMap<>());

        for (String skillId : skillData.getLearnedSkills()) {
            SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);

            for (SkillEffectsConfig.ConditionalEffect condEffect : effects.conditionalEffects) {
                boolean conditionMet = condEffect.condition.test(context);
                String key = skillId + "_" + condEffect.hashCode();

                if (conditionMet) {
                    // Применяем эффекты если условие выполнено и они еще не активны
                    if (!activeModifiers.containsKey(key)) {
                        List<AttributeModifier> appliedModifiers = applyConditionalEffects(player, condEffect, skillId);
                        activeModifiers.put(key, appliedModifiers);
                    }
                } else {
                    // Удаляем эффекты если условие не выполнено
                    if (activeModifiers.containsKey(key)) {
                        removeConditionalEffects(player, activeModifiers.get(key));
                        activeModifiers.remove(key);
                    }
                }
            }
        }
    }

    private static List<AttributeModifier> applyConditionalEffects(Player player,
                                                                   SkillEffectsConfig.ConditionalEffect condEffect, String skillId) {
        List<AttributeModifier> appliedModifiers = new ArrayList<>();

        for (SkillEffectsConfig.AttributeBonus bonus : condEffect.attributeBonuses) {
            Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(bonus.attribute);
            if (holder.isPresent()) {
                AttributeInstance instance = player.getAttribute(holder.get());
                if (instance != null) {
                    AttributeModifier modifier = new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID,
                                    "conditional_" + bonus.name + "_" + UUID.randomUUID()),
                            bonus.amount,
                            bonus.operation
                    );
                    instance.addPermanentModifier(modifier);
                    appliedModifiers.add(modifier);
                }
            }
        }

        for (SkillEffectsConfig.AttributeMultiplier mult : condEffect.attributeMultipliers) {
            Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(mult.attribute);
            if (holder.isPresent()) {
                AttributeInstance instance = player.getAttribute(holder.get());
                if (instance != null) {
                    double bonus = mult.multiplier - 1.0;
                    AttributeModifier modifier = new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID,
                                    "conditional_mult_" + skillId + "_" + mult.attribute.getPath() + "_" + UUID.randomUUID()),
                            bonus,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    );
                    instance.addPermanentModifier(modifier);
                    appliedModifiers.add(modifier);
                }
            }
        }

        return appliedModifiers;
    }

    private static void removeConditionalEffects(Player player, List<AttributeModifier> modifiers) {
        for (AttributeModifier modifier : modifiers) {
            for (Holder.Reference<Attribute> holder : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
                AttributeInstance instance = player.getAttribute(holder);
                if (instance != null && instance.getModifier(modifier.id()) != null) {
                    instance.removeModifier(modifier.id());
                }
            }
        }
    }

    private static void clearAllConditionalModifiers(Player player) {
        if (player == null) return;

        Map<String, List<AttributeModifier>> activeModifiers = ACTIVE_CONDITIONAL_MODIFIERS.remove(player.getUUID());
        if (activeModifiers != null) {
            for (List<AttributeModifier> modifiers : activeModifiers.values()) {
                removeConditionalEffects(player, modifiers);
            }
        }
    }

    private static LivingEntity getCurrentTarget(Player player) {
        if (player.getLastHurtMob() instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        return null;
    }

    public static boolean isItemActionAllowed(Player player, ItemStack stack, BlockedAction action) {
        if (player == null || stack == null || stack.isEmpty()) return true;

        PlayerSkillData skillData = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        boolean isBlocked = ItemRestrictionsConfig.isActionBlocked(stack, null, action);
        if (!isBlocked) return true;

        for (String skillId : skillData.getLearnedSkills()) {
            SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);

            if (effects.unlockedItems.contains(itemId)) {
                return true;
            }

            for (TagKey<Item> tag : effects.unlockedItemTags) {
                if (stack.is(tag)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isBlockActionAllowed(Player player, BlockState block, BlockedAction action) {
        if (player == null || block == null || block.isEmpty()) return true;

        PlayerSkillData skillData = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block.getBlock());

        boolean isBlocked = ItemRestrictionsConfig.isActionBlocked(null, block, action);
        if (!isBlocked) return true;

        for (String skillId : skillData.getLearnedSkills()) {
            SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);

            if (effects.unlockedBlocks.contains(blockId)) {
                return true;
            }

            for (TagKey<Block> tag : effects.unlockedBlockTags) {
                if (block.is(tag)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isArmorActionAllowed(Player player, ItemStack armor, BlockedAction action) {
        if (player == null || armor == null || armor.isEmpty()) return true;

        PlayerSkillData skillData = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
        ResourceLocation armorId = BuiltInRegistries.ITEM.getKey(armor.getItem());

        boolean hasRestriction = ItemRestrictionsConfig.isActionBlocked(armor, null, action);
        if (!hasRestriction) return true;

        for (String skillId : skillData.getLearnedSkills()) {
            SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);

            if (effects.unlockedItems.contains(armorId)) {
                return true;
            }

            for (TagKey<Item> tag : effects.unlockedItemTags) {
                if (armor.is(tag)) {
                    return true;
                }
            }
        }

        return false;
    }

//    public static boolean isActionAllowed(Player player, ItemStack stack, BlockState block, BlockedAction action) {
//        if (player == null) return true;
//        if (player == null || player.hasPermissions(2)) return true;
//        PlayerSkillData skillData = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
//
//        if (action == BlockedAction.EQUIP_ARMOR || action == BlockedAction.UNPREPAREDNESS_FOR_ARMOR) {
//            return isArmorActionAllowed(player, stack, skillData, action);
//        }
//
//        if (stack != null && !stack.isEmpty()) {
//            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
//
//            boolean isBlocked = ItemRestrictionsConfig.isActionBlocked(stack, null, action);
//            if (!isBlocked) return true;
//
//            // Проверка разблокировки через навыки (предметы + теги)
//            for (String skillId : skillData.getLearnedSkills()) {
//                SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);
//
//                // По конкретным предметам
//                if (effects.unlockedItems.contains(itemId)) {
//                    return true;
//                }
//
//                // По тегам
//                for (TagKey<Item> tag : effects.unlockedItemTags) {
//                    if (stack.is(tag)) {
//                        return true;
//                    }
//                }
//            }
//            return false;
//        } else if (block != null && !block.isEmpty()) {
//            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block.getBlock());
//
//            boolean isBlocked = ItemRestrictionsConfig.isActionBlocked(null, block, action);
//            if (!isBlocked) return true;
//
//            // Проверка разблокировки через навыки (блоки + теги)
//            for (String skillId : skillData.getLearnedSkills()) {
//                SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);
//
//                // По конкретным предметам
//                if (effects.unlockedBlocks.contains(blockId)) {
//                    return true;
//                }
//
//                // По тегам
//                for (TagKey<Block> tag : effects.unlockedBlockTags) {
//                    if (block.is(tag)) {
//                        return true;
//                    }
//                }
//            }
//            return false;
//        }
//        return true;
//    }
//
//    private static boolean isArmorActionAllowed(Player player, ItemStack armor, PlayerSkillData skillData, BlockedAction action) {
//        if (armor == null || armor.isEmpty()) return true;
//
//        boolean hasRestriction = ItemRestrictionsConfig.isActionBlocked(armor, null, action);
//
//        ResourceLocation armorId = BuiltInRegistries.ITEM.getKey(armor.getItem());
//
//        if (!hasRestriction) return true;
//
//        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(armor.getItem());
//
//        for (String skillId : skillData.getLearnedSkills()) {
//            SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);
//            if (effects.unlockedItems.contains(itemId)) return true;
//            for (TagKey<Item> tag : effects.unlockedItemTags) {
//                if (armor.is(tag)) return true;
//
//            }
//        }
//
//        return false;
//    }
}
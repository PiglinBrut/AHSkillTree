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
            BlockedAction.UNPREPAREDNESS_FOR_ARMOR,
            BlockedAction.EQUIP_ARMOR,
            BlockedAction.EQUIP_CURIOS
    );

    private static final Set<BlockedAction> DEFAULT_BLOCK_LOCKED_ACTIONS = Set.of(
            BlockedAction.INTERACT_BLOCK
    );

    // Кэш состояний условных эффектов
    private static final Map<UUID, Map<String, Boolean>> CONDITION_CACHE = new HashMap<>();

    public static void applyAllSkillEffects(Player player, PlayerSkillData skillData) {
        if (player == null) return;

        clearAllSkillModifiers(player);
        clearAllConditionalEffects(player);

        ItemRestrictionsConfig.clearTempRestrictions();

        for (String skillId : skillData.getLearnedSkills()) {
            applySkillRestrictions(skillId);
            applySkillEffects(player, skillId);
            applyMultipliers(player, skillId);
        }
    }

    private static void applySkillRestrictions(String skillId) {
        SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);

        for (ResourceLocation itemId : effects.lockedItems) {
            ItemRestrictionsConfig.addTempItemRestriction(itemId, DEFAULT_ITEM_LOCKED_ACTIONS);
        }

        for (TagKey<Item> tag : effects.lockedItemTags) {
            ItemRestrictionsConfig.addTempItemTagRestriction(tag, DEFAULT_ITEM_LOCKED_ACTIONS);
        }

        for (ResourceLocation blockId : effects.lockedBlocks) {
            ItemRestrictionsConfig.addTempBlockRestriction(blockId, DEFAULT_BLOCK_LOCKED_ACTIONS);
        }

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

    public static void clearConditionCache(Player player) {
        if (player == null) return;
        CONDITION_CACHE.remove(player.getUUID());
    }

    public static void updateConditionalEffects(Player player) {
        if (player == null || player.level().isClientSide) return;

        PlayerSkillData skillData = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
        ConditionContext context = new ConditionContext(player, getCurrentTarget(player));

        UUID playerId = player.getUUID();
        Map<String, Boolean> cache = CONDITION_CACHE.computeIfAbsent(playerId, k -> new HashMap<>());

        for (String skillId : skillData.getLearnedSkills()) {
            SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);

            for (int i = 0; i < effects.conditionalEffects.size(); i++) {
                SkillEffectsConfig.ConditionalEffect condEffect = effects.conditionalEffects.get(i);
                String key = skillId + "_" + i;

                boolean conditionMet = condEffect.condition.test(context);
                Boolean wasMet = cache.get(key);

                if (wasMet == null || wasMet != conditionMet) {
                    if (conditionMet) {
                        // Старые attribute bonuses
                        for (SkillEffectsConfig.AttributeBonus bonus : condEffect.attributeBonuses) {
                            Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(bonus.attribute);
                            if (holder.isPresent()) {
                                AttributeInstance instance = player.getAttribute(holder.get());
                                if (instance != null) {
                                    AttributeModifier modifier = new AttributeModifier(
                                            ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID,
                                                    "conditional_" + bonus.name + "_" + key),
                                            bonus.amount,
                                            bonus.operation
                                    );
                                    instance.addPermanentModifier(modifier);
                                }
                            }
                        }

                        // Attribute multipliers
                        for (SkillEffectsConfig.AttributeMultiplier mult : condEffect.attributeMultipliers) {
                            Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(mult.attribute);
                            if (holder.isPresent()) {
                                AttributeInstance instance = player.getAttribute(holder.get());
                                if (instance != null) {
                                    double bonus = mult.multiplier - 1.0;
                                    AttributeModifier modifier = new AttributeModifier(
                                            ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID,
                                                    "conditional_mult_" + key + "_" + mult.attribute.getPath()),
                                            bonus,
                                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                                    );
                                    instance.addPermanentModifier(modifier);
                                }
                            }
                        }
                    } else {
                        // Удаляем эффекты
                        for (SkillEffectsConfig.AttributeBonus bonus : condEffect.attributeBonuses) {
                            Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(bonus.attribute);
                            if (holder.isPresent()) {
                                AttributeInstance instance = player.getAttribute(holder.get());
                                if (instance != null) {
                                    instance.removeModifier(ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID,
                                            "conditional_" + bonus.name + "_" + key));
                                }
                            }
                        }

                        for (SkillEffectsConfig.AttributeMultiplier mult : condEffect.attributeMultipliers) {
                            Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(mult.attribute);
                            if (holder.isPresent()) {
                                AttributeInstance instance = player.getAttribute(holder.get());
                                if (instance != null) {
                                    instance.removeModifier(ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID,
                                            "conditional_mult_" + key + "_" + mult.attribute.getPath()));
                                }
                            }
                        }
                    }

                    cache.put(key, conditionMet);
                }
            }
        }
    }

    private static void clearAllConditionalEffects(Player player) {
        if (player == null) return;

        UUID playerId = player.getUUID();
        CONDITION_CACHE.remove(playerId);
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

    public static boolean isCuriosActionAllowed(Player player, ItemStack curiosItem, BlockedAction action) {
        if (player == null || curiosItem == null || curiosItem.isEmpty()) return true;

        boolean hasRestriction = ItemRestrictionsConfig.isActionBlocked(curiosItem, null, action);
        if (!hasRestriction) return true;

        PlayerSkillData skillData = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(curiosItem.getItem());

        for (String skillId : skillData.getLearnedSkills()) {
            SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);

            if (effects.unlockedItems.contains(itemId)) return true;

            for (TagKey<Item> tag : effects.unlockedItemTags) {
                if (curiosItem.is(tag)) return true;
            }
        }

        return false;
    }
}
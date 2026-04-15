package ru.pb.ahst.effects;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.config.BaseAttributesConfig;
import ru.pb.ahst.config.BlockedAction;
import ru.pb.ahst.config.ItemRestrictionsConfig;
import ru.pb.ahst.config.SkillEffectsConfig;
import ru.pb.ahst.data.PlayerSkillData;
import ru.pb.ahst.data.SkillDataAttachments;

import java.util.List;

@EventBusSubscriber(modid = AHSkillTree.MOD_ID)
public class SkillEffectsEvents {

    private static boolean hasBetterCombat = false;

    static {
        try {
            Class.forName("net.bettercombat.BetterCombatMod");
            hasBetterCombat = true;
        } catch (ClassNotFoundException e) {
            hasBetterCombat = false;
        }
    }

    // ==================== Применение эффектов ====================

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && !event.getLevel().isClientSide) {
            BaseAttributesConfig.applyAttributesToPlayer(serverPlayer);
        } else if (event.getEntity() instanceof Player player && !event.getLevel().isClientSide) {
            applyEffects(player);
            updateArmorPenalties(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            BaseAttributesConfig.applyAttributesToPlayer(serverPlayer);
        }
        applyEffects(event.getEntity());
        updateArmorPenalties(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            BaseAttributesConfig.applyAttributesToPlayer(serverPlayer);
        }
        applyEffects(event.getEntity());
        updateArmorPenalties(event.getEntity());
    }

//    @SubscribeEvent
//    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
//        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
//            applyEffects(player);
//        }
//    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer original && event.getOriginal() instanceof ServerPlayer originalPlayer) {
            if (originalPlayer.getPersistentData().contains("ah_attributes_applied")) {
                original.getPersistentData().putBoolean("ah_attributes_applied", true);
            }
        }
    }

    private static void applyEffects(Player player) {
        if (player == null || player.level().isClientSide) return;
        PlayerSkillData skillData = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
        SkillEffectsManager.applyAllSkillEffects(player, skillData);

        updateArmorPenalties(player);
    }

    // ==================== Блокировка действий ====================

    private static void sendBlockedMessage(Player player) {
        player.displayClientMessage(
                Component.literal("§cЭто действие заблокировано! Изучите нужный навык.")
                        .withStyle(ChatFormatting.RED),
                true
        );
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (!SkillEffectsManager.isItemActionAllowed(player, stack, BlockedAction.RIGHT_CLICK)) {
            event.setCanceled(true);
            sendBlockedMessage(player);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        BlockState block = event.getLevel().getBlockState(event.getPos());

        if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
            if (!SkillEffectsManager.isItemActionAllowed(player, stack, BlockedAction.PLACE_BLOCK)) {
                event.setCanceled(true);
                sendBlockedMessage(player);
            }
        }
        if (!SkillEffectsManager.isBlockActionAllowed(player, block, BlockedAction.INTERACT_BLOCK)) {
            event.setCanceled(true);
            sendBlockedMessage(player);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();

        if (!SkillEffectsManager.isItemActionAllowed(player, stack, BlockedAction.ATTACK_ENTITY)) {
            event.setCanceled(true);
            sendBlockedMessage(player);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            EquipmentSlot slot = event.getSlot();
            ItemStack newStack = event.getTo();

            if (slot.isArmor() && !newStack.isEmpty()) {
                if (!SkillEffectsManager.isArmorActionAllowed(player, newStack, BlockedAction.EQUIP_ARMOR)) {
                    player.drop(newStack.copy(), false);
                    newStack.setCount(0);
                    return;
                }
            }

            updateArmorPenalties(player);

            if (hasBetterCombat) return;

            if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
                updateVanillaWeaponPenalty(player);
            }

        }
    }

    @SubscribeEvent
    public static void onBreakBlockByItem(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();

        if (player != null) {
            ItemStack stack = player.getMainHandItem();

            if (!SkillEffectsManager.isItemActionAllowed(player, stack, BlockedAction.BREAK_BLOCK_BY_ITEM)) {
                event.setCanceled(true);
                sendBlockedMessage(player);
            }
        }
    }

    private static void updateVanillaWeaponPenalty(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean mainHandUnprepared = isWeaponUnprepared(player, mainHand);
        boolean offHandUnprepared = isWeaponUnprepared(player, offHand);

        if (mainHandUnprepared || offHandUnprepared) {
            applyVanillaWeaponPenalty(player);
        } else {
            removeVanillaWeaponPenalty(player);
        }
    }

    private static void applyVanillaWeaponPenalty(Player player) {
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);

        AttributeModifier damagePenalty = new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "vanilla_penalty_damage"),
                -0.5,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        AttributeModifier speedPenalty = new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "vanilla_penalty_speed"),
                -0.5,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        if (attackDamage != null) {
            attackDamage.removeModifier(damagePenalty.id());
            attackDamage.addPermanentModifier(damagePenalty);
        }

        if (attackSpeed != null) {
            attackSpeed.removeModifier(speedPenalty.id());
            attackSpeed.addPermanentModifier(speedPenalty);
        }
    }

    private static void removeVanillaWeaponPenalty(Player player) {
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);

        if (attackDamage != null) {
            attackDamage.removeModifier(ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "vanilla_penalty_damage"));
        }

        if (attackSpeed != null) {
            attackSpeed.removeModifier(ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "vanilla_penalty_speed"));
        }
    }

    public static boolean isWeaponUnprepared(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return false;

        boolean hasRestriction = ItemRestrictionsConfig.isActionBlocked(
                stack, null, BlockedAction.UNPREPAREDNESS_FOR_WEAPON
        );
        if (!hasRestriction) return false;

        PlayerSkillData skillData = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        for (String skillId : skillData.getLearnedSkills()) {
            SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);

            if (effects.unlockedItems.contains(itemId)) return false;

            for (TagKey<Item> tag : effects.unlockedItemTags) {
                if (stack.is(tag)) return false;
            }
        }

        return true;
    }

    private static void updateArmorPenalties(Player player) {
        if (player == null || player.level().isClientSide) return;

        boolean hasAnyPenalty = false;
        double totalSpeedPenalty = 0;
        double totalDamagePenalty = 0;
        double totalArmorPenalty = 0;

        for (EquipmentSlot slot : getArmorSlots()) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) continue;

            if (shouldApplyArmorPenalty(player, armor)) {
                double multiplier = getArmorPenaltyMultiplier(armor);

                totalSpeedPenalty += -0.55 * multiplier;   // -55% скорости за предмет
                totalDamagePenalty += -0.40 * multiplier;  // -40% урона за предмет
                totalArmorPenalty += -0.30 * multiplier;   // -30% защиты за предмет
                hasAnyPenalty = true;
            }
        }

        applyAttributeModifier(player, Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "armor_penalty_speed"),
                totalSpeedPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyAttributeModifier(player, Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "armor_penalty_damage"),
                totalDamagePenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyAttributeModifier(player, Attributes.ARMOR, ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "armor_penalty_armor"),
                totalArmorPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void applyAttributeModifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        AttributeModifier modifier = new AttributeModifier(id, amount, operation);

        instance.removeModifier(id);

        if (Math.abs(amount) > 0.001) {
            instance.addPermanentModifier(modifier);
        }
    }

    private static double getArmorPenaltyMultiplier(ItemStack armor) {
        if (armor == null || armor.isEmpty()) return 1.0;

        Item item = armor.getItem();

        int armorValue = 0;

        if (item instanceof ArmorItem armorItem) {
            armorValue = armorItem.getDefense();
        }

        double multiplier = (armorValue / 10.0) * 0.5;

        multiplier = Math.max(0.1, Math.min(1.5, multiplier));

        return multiplier;
    }

    private static boolean shouldApplyArmorPenalty(Player player, ItemStack armor) {
        boolean hasRestriction = ItemRestrictionsConfig.isActionBlocked(
                armor, null, BlockedAction.UNPREPAREDNESS_FOR_ARMOR
        );
        if (!hasRestriction) return false;

        PlayerSkillData skillData = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(armor.getItem());

        for (String skillId : skillData.getLearnedSkills()) {
            SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);
            if (effects.unlockedItems.contains(itemId)) return false;
            for (TagKey<Item> tag : effects.unlockedItemTags) {
                if (armor.is(tag)) return false;
            }
        }

        return true;
    }

    private static List<EquipmentSlot> getArmorSlots() {
        return List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    }
}
package ru.pb.ahst.mixin;

import net.bettercombat.api.AttackHand;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.pb.ahst.config.BlockedAction;
import ru.pb.ahst.config.ItemRestrictionsConfig;
import ru.pb.ahst.config.SkillEffectsConfig;
import ru.pb.ahst.data.PlayerSkillData;
import ru.pb.ahst.data.SkillDataAttachments;

@Mixin(targets = "net.bettercombat.logic.PlayerAttackHelper", remap = false)
public class MixinBetterCombatAttack {

//    private static final Logger LOGGER = LoggerFactory.getLogger("AHSkillTree-Mixin");

    @Inject(
            method = "getDualWieldingAttackDamageMultiplier",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void onGetDualWieldingDamageMultiplier(Player player, AttackHand hand,
                                                          CallbackInfoReturnable<Float> cir) {
        if (player == null) return;

        ItemStack weaponStack = hand.isOffHand() ? player.getOffhandItem() : player.getMainHandItem();
        float originalMultiplier = cir.getReturnValue();

        if (isWeaponUnprepared(player, weaponStack)) {
            float newMultiplier = originalMultiplier * 0.5f;
            cir.setReturnValue(newMultiplier);
        }
    }

    @Inject(
            method = "getAttackCooldownTicksCapped",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void onGetAttackCooldown(Player player, CallbackInfoReturnable<Float> cir) {
        if (player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        float originalCooldown = cir.getReturnValue();
        float newCooldown = originalCooldown;

        if (isWeaponUnprepared(player, mainHand)) {
            newCooldown = Math.max(newCooldown, originalCooldown * 2.0f);
        }

        if (isWeaponUnprepared(player, offHand)) {
            newCooldown = Math.max(newCooldown, originalCooldown * 2.0f);
        }

        if (newCooldown != originalCooldown) {
            cir.setReturnValue(newCooldown);
        }
    }

    private static boolean isWeaponUnprepared(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return false;

        boolean hasRestriction = ItemRestrictionsConfig.isActionBlocked(
                stack, null, BlockedAction.UNPREPAREDNESS_FOR_WEAPON
        );
        if (!hasRestriction) return false;

        PlayerSkillData skillData = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        for (String skillId : skillData.getLearnedSkills()) {
            SkillEffectsConfig.SkillEffects effects = SkillEffectsConfig.getEffects(skillId);

            if (effects.unlockedItems.contains(itemId)) {
                return false;
            }

            for (TagKey<Item> tag : effects.unlockedItemTags) {
                if (stack.is(tag)) {
                    return false;
                }
            }
        }

        return true;
    }
}
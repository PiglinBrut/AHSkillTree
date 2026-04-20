package ru.pb.ahst.effects.bonuses;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import ru.pb.ahst.AHSkillTree;

import java.util.Random;

public class CritChanceBonus implements SkillBonus {
    private final double chance;
    private final double damageMultiplier;
    private final String key;
    private final String multKey;

    public CritChanceBonus(double chance, double damageMultiplier, String skillId) {
        this.chance = chance;
        this.damageMultiplier = damageMultiplier;
        this.key = AHSkillTree.MOD_ID + ":crit_" + skillId;
        this.multKey = key + "_mult";
    }

    @Override
    public void apply(Player player, String skillId) {
        CompoundTag critData = player.getPersistentData()
                .getCompound(AHSkillTree.MOD_ID + "_crit_data");
        critData.putDouble(key, chance);
        critData.putDouble(multKey, damageMultiplier);
        player.getPersistentData().put(AHSkillTree.MOD_ID + "_crit_data", critData);
    }

    @Override
    public void remove(Player player) {
        CompoundTag critData = player.getPersistentData()
                .getCompound(AHSkillTree.MOD_ID + "_crit_data");
        critData.remove(key);
        critData.remove(multKey);
        player.getPersistentData().put(AHSkillTree.MOD_ID + "_crit_data", critData);
    }

    public static boolean tryCrit(Player player, LivingEntity target) {
        CompoundTag critData = player.getPersistentData()
                .getCompound(AHSkillTree.MOD_ID + "_crit_data");

        Random random = new Random();
        for (String key : critData.getAllKeys()) {
            if (key.endsWith("_mult")) continue;
            double chance = critData.getDouble(key);
            if (random.nextDouble() < chance) {
                String multKey = key + "_mult";
                double mult = critData.contains(multKey) ? critData.getDouble(multKey) : 1.5;
                return true;
            }
        }
        return false;
    }
}

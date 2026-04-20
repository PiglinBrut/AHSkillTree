package ru.pb.ahst.effects.bonuses;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import ru.pb.ahst.AHSkillTree;

public class LifestealBonus implements SkillBonus {
    private final double percentage;
    private final String key;

    public LifestealBonus(double percentage, String skillId) {
        this.percentage = percentage;
        this.key = AHSkillTree.MOD_ID + ":lifesteal_" + skillId;
    }

    @Override
    public void apply(Player player, String skillId) {
        CompoundTag data = player.getPersistentData();
        double current = data.getDouble(key);
        data.putDouble(key, current + percentage);
    }

    @Override
    public void remove(Player player) {
        player.getPersistentData().remove(key);
    }

    public static double getTotalLifesteal(Player player) {
        double total = 0;
        CompoundTag data = player.getPersistentData();
        for (String key : data.getAllKeys()) {
            if (key.startsWith(AHSkillTree.MOD_ID + ":lifesteal_")) {
                total += data.getDouble(key);
            }
        }
        return total;
    }
}


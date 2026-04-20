package ru.pb.ahst.effects.bonuses;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import ru.pb.ahst.AHSkillTree;

public class MiningSpeedBonus implements SkillBonus {
    private final double multiplier;
    private final String key;

    public MiningSpeedBonus(double multiplier, String skillId) {
        this.multiplier = multiplier;
        this.key = AHSkillTree.MOD_ID + ":mining_speed_" + skillId;
    }

    @Override
    public void apply(Player player, String skillId) {
        CompoundTag data = player.getPersistentData();
        double current = data.getDouble(key);
        data.putDouble(key, current + multiplier - 1.0);
    }

    @Override
    public void remove(Player player) {
        player.getPersistentData().remove(key);
    }

    public static double getTotalMiningSpeedMultiplier(Player player) {
        double total = 1.0;
        CompoundTag data = player.getPersistentData();
        for (String key : data.getAllKeys()) {
            if (key.startsWith(AHSkillTree.MOD_ID + ":mining_speed_")) {
                total += data.getDouble(key);
            }
        }
        return total;
    }
}

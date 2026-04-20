package ru.pb.ahst.effects.bonuses;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import ru.pb.ahst.AHSkillTree;

public class FallDamageReductionBonus implements SkillBonus {
    private final double reduction;
    private final String key;

    public FallDamageReductionBonus(double reduction, String skillId) {
        this.reduction = reduction;
        this.key = AHSkillTree.MOD_ID + ":fall_reduction_" + skillId;
    }

    @Override
    public void apply(Player player, String skillId) {
        CompoundTag data = player.getPersistentData();
        double current = data.getDouble(key);
        data.putDouble(key, current + reduction);
    }

    @Override
    public void remove(Player player) {
        player.getPersistentData().remove(key);
    }

    public static double getTotalFallReduction(Player player) {
        double total = 0;
        CompoundTag data = player.getPersistentData();
        for (String key : data.getAllKeys()) {
            if (key.startsWith(AHSkillTree.MOD_ID + ":fall_reduction_")) {
                total += data.getDouble(key);
            }
        }
        return Math.min(1.0, total);
    }
}

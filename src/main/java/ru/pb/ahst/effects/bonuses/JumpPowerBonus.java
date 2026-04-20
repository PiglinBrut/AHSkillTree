package ru.pb.ahst.effects.bonuses;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import ru.pb.ahst.AHSkillTree;

public class JumpPowerBonus implements SkillBonus {
    private final double bonus;
    private final String key;

    public JumpPowerBonus(double bonus, String skillId) {
        this.bonus = bonus;
        this.key = AHSkillTree.MOD_ID + ":jump_power_" + skillId;
    }

    @Override
    public void apply(Player player, String skillId) {
        CompoundTag data = player.getPersistentData();
        double current = data.getDouble(key);
        data.putDouble(key, current + bonus);
    }

    @Override
    public void remove(Player player) {
        player.getPersistentData().remove(key);
    }

    public static double getTotalJumpBonus(Player player) {
        double total = 0;
        CompoundTag data = player.getPersistentData();
        for (String key : data.getAllKeys()) {
            if (key.startsWith(AHSkillTree.MOD_ID + ":jump_power_")) {
                total += data.getDouble(key);
            }
        }
        return total;
    }
}

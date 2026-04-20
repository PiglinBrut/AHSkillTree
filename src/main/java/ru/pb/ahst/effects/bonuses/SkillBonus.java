package ru.pb.ahst.effects.bonuses;

import net.minecraft.world.entity.player.Player;

public interface SkillBonus {
    void apply(Player player, String skillId);
    void remove(Player player);
}
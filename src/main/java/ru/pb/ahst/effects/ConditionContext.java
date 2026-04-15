// ConditionContext.java
package ru.pb.ahst.effects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ConditionContext {
    public final Player player;
    public final LivingEntity target;

    public ConditionContext(Player player, LivingEntity target) {
        this.player = player;
        this.target = target;
    }

    public ConditionContext(Player player) {
        this(player, null);
    }
}
package ru.pb.ahst.effects.conditions;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import ru.pb.ahst.effects.ConditionContext;

public class EnemiesAroundCondition implements Condition {
    private final double radius;
    private final int min;
    private final int max;
    private final boolean aggressiveOnly;

    public EnemiesAroundCondition(double radius, int min, int max, boolean aggressiveOnly) {
        this.radius = radius;
        this.min = min;
        this.max = max;
        this.aggressiveOnly = aggressiveOnly;
    }

    @Override
    public boolean test(ConditionContext context) {
        var level = context.player.level();
        var pos = context.player.blockPosition();

        long count = level.getEntitiesOfClass(LivingEntity.class,
                AABB.ofSize(pos.getCenter(), radius, radius, radius),
                e -> e != context.player &&
                        (!aggressiveOnly || (e instanceof Monster || e instanceof Enemy)) &&
                        e.isAlive()
        ).size();

        return count >= min && count <= max;
    }
}

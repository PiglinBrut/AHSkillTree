package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

public class DistanceToTargetCondition implements Condition {
    private final double min;
    private final double max;

    public DistanceToTargetCondition(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(ConditionContext context) {
        if (context.target == null) return false;
        double distance = context.player.distanceTo(context.target);
        return distance >= min && distance <= max;
    }
}

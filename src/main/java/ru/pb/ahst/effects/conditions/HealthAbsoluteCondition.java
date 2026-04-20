package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

// HealthAbsoluteCondition.java
public class HealthAbsoluteCondition implements Condition {
    private final double min;
    private final double max;

    public HealthAbsoluteCondition(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(ConditionContext context) {
        float health = context.player.getHealth();
        return health >= min && health <= max;
    }
}

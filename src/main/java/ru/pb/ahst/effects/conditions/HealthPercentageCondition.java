package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

public class HealthPercentageCondition implements Condition {
    private final double min;
    private final double max;

    public HealthPercentageCondition(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(ConditionContext context) {
        float percentage = context.player.getHealth() / context.player.getMaxHealth() * 100;
        return percentage >= min && percentage <= max;
    }
}


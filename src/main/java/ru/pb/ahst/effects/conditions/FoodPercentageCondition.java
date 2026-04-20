package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

public class FoodPercentageCondition implements Condition {
    private final double min;
    private final double max;

    public FoodPercentageCondition(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(ConditionContext context) {
        float percentage = context.player.getFoodData().getFoodLevel() / 20f * 100;
        return percentage >= min && percentage <= max;
    }
}

package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

public class FoodAbsoluteCondition implements Condition {
    private final int min;
    private final int max;

    public FoodAbsoluteCondition(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(ConditionContext context) {
        int food = context.player.getFoodData().getFoodLevel();
        return food >= min && food <= max;
    }
}

package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

public class InWaterCondition implements Condition {
    @Override
    public boolean test(ConditionContext context) {
        return context.player.isInWater();
    }
}

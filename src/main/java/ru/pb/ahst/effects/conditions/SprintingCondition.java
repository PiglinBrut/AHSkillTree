package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

public class SprintingCondition implements Condition {
    @Override
    public boolean test(ConditionContext context) {
        return context.player.isSprinting();
    }
}

package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

public class IsFallingCondition implements Condition {
    @Override
    public boolean test(ConditionContext context) {
        return context.player.fallDistance > 0 && !context.player.onGround();
    }
}

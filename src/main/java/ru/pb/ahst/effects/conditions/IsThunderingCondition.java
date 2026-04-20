package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

public class IsThunderingCondition implements Condition {
    @Override
    public boolean test(ConditionContext context) {
        return context.player.level().isThundering();
    }
}

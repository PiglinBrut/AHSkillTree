package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

public class BelowSeaLevelCondition implements Condition {
    @Override
    public boolean test(ConditionContext context) {
        return context.player.getY() < context.player.level().getSeaLevel();
    }
}

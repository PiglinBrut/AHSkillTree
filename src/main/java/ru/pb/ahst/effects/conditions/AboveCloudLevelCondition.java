package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

public class AboveCloudLevelCondition implements Condition {
    @Override
    public boolean test(ConditionContext context) {
        return context.player.getY() > 192; // Стандартная высота облаков
    }
}

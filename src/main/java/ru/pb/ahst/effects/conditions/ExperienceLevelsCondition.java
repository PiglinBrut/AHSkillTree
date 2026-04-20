package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

public class ExperienceLevelsCondition implements Condition {
    private final int min;
    private final int max;

    public ExperienceLevelsCondition(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(ConditionContext context) {
        int levels = context.player.experienceLevel;
        return levels >= min && levels <= max;
    }
}

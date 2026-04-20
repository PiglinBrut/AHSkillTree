package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

public class SkillNotCondition implements Condition {
    private final Condition condition;

    public SkillNotCondition(Condition condition) {
        this.condition = condition;
    }

    @Override
    public boolean test(ConditionContext context) {
        return !condition.test(context);
    }
}

package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

import java.util.List;

public class SkillOrCondition implements Condition {
    private final List<Condition> conditions;

    public SkillOrCondition(List<Condition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean test(ConditionContext context) {
        return conditions.stream().anyMatch(c -> c.test(context));
    }
}


package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;
import java.util.List;

public class SkillAndCondition implements Condition {
    private final List<Condition> conditions;

    public SkillAndCondition(List<Condition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean test(ConditionContext context) {
        return conditions.stream().allMatch(c -> c.test(context));
    }
}


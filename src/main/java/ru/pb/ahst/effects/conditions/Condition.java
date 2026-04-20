package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

public interface Condition {
    boolean test(ConditionContext context);
}
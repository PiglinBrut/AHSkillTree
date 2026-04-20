package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.effects.ConditionContext;

public class TimeOfDayCondition implements Condition {
    private final long min;
    private final long max;

    public TimeOfDayCondition(long min, long max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean test(ConditionContext context) {
        long time = context.player.level().getDayTime() % 24000;
        if (min <= max) {
            return time >= min && time <= max;
        } else {
            return time >= min || time <= max;
        }
    }
}

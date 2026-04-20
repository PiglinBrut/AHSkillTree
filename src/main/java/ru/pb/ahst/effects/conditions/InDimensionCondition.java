package ru.pb.ahst.effects.conditions;

import net.minecraft.resources.ResourceLocation;
import ru.pb.ahst.effects.ConditionContext;

public class InDimensionCondition implements Condition {
    private final ResourceLocation dimensionId;

    public InDimensionCondition(String dimensionId) {
        this.dimensionId = ResourceLocation.parse(dimensionId);
    }

    @Override
    public boolean test(ConditionContext context) {
        return context.player.level().dimension().location().equals(dimensionId);
    }
}

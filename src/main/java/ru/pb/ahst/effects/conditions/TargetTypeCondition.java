package ru.pb.ahst.effects.conditions;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import ru.pb.ahst.effects.ConditionContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TargetTypeCondition implements Condition {
    private final boolean isPlayer;
    private final boolean isMonster;
    private final Set<EntityType<?>> allowedTypes;

    public TargetTypeCondition(boolean isPlayer, boolean isMonster, List<EntityType<?>> types) {
        this.isPlayer = isPlayer;
        this.isMonster = isMonster;
        this.allowedTypes = new HashSet<>(types);
    }

    @Override
    public boolean test(ConditionContext context) {
        if (context.target == null) return false;

        if (isPlayer && context.target instanceof Player) return true;
        if (isMonster && (context.target instanceof Monster || context.target instanceof Enemy)) return true;
        if (allowedTypes.contains(context.target.getType())) return true;

        return false;
    }
}

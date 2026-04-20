package ru.pb.ahst.effects.conditions;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import ru.pb.ahst.effects.ConditionContext;

public class HasStatusEffectCondition implements Condition {
    private final ResourceLocation effectId;
    private final int amplifier;

    public HasStatusEffectCondition(String effectId, int amplifier) {
        this.effectId = ResourceLocation.parse(effectId);
        this.amplifier = amplifier;
    }

    @Override
    public boolean test(ConditionContext context) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
        if (effect == null) return false;

        Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);

        MobEffectInstance instance = context.player.getEffect(effectHolder);
        if (instance == null) return false;

        if (amplifier >= 0 && instance.getAmplifier() != amplifier) return false;

        return true;
    }
}

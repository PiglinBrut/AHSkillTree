package ru.pb.ahst.effects.conditions;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import ru.pb.ahst.effects.ConditionContext;

public class InBiomeCondition implements Condition {
    private final ResourceLocation biomeId;
    private final boolean checkTag;

    public InBiomeCondition(String biomeId) {
        if (biomeId.startsWith("#")) {
            this.biomeId = ResourceLocation.parse(biomeId.substring(1));
            this.checkTag = true;
        } else {
            this.biomeId = ResourceLocation.parse(biomeId);
            this.checkTag = false;
        }
    }

    @Override
    public boolean test(ConditionContext context) {
        var biome = context.player.level().getBiome(context.player.blockPosition());
        if (checkTag) {
            return biome.is(TagKey.create(Registries.BIOME, biomeId));
        } else {
            return biome.is(biomeId);
        }
    }
}

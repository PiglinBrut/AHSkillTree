package ru.pb.ahst.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.pb.ahst.AHSkillTree;

import java.util.Optional;
import java.util.function.Supplier;

public class AttributeRegistry {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, AHSkillTree.MOD_ID);
    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }

    public static final Supplier<Attribute> FALL_DAMAGE_REDUCTION = ATTRIBUTES.register(
            "fall_damage_reduction",
            () -> new RangedAttribute("attribute." + AHSkillTree.MOD_ID + ".fall_damage_reduction",
                    0.0, 0.0, 1.0).setSyncable(true)
    );

    public static final Supplier<Attribute> LIFESTEAL = ATTRIBUTES.register(
            "lifesteal",
            () -> new RangedAttribute("attribute." + AHSkillTree.MOD_ID + ".lifesteal",
                    0.0, 0.0, 1.0).setSyncable(true)
    );

    public static Optional<Holder.Reference<Attribute>> getFallReductionHolder() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "fall_damage_reduction");
        return BuiltInRegistries.ATTRIBUTE.getHolder(id);
    }

    public static Optional<Holder.Reference<Attribute>> getLifestealHolder() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "lifesteal");
        return BuiltInRegistries.ATTRIBUTE.getHolder(id);
    }
}

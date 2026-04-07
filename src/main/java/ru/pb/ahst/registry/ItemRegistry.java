package ru.pb.ahst.registry;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.util.tools.SkillManuscript;


public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AHSkillTree.MOD_ID);
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    // ==== Items ====
    public static final DeferredItem<SkillManuscript> SKILL_MANUSCRIPT = ITEMS.register("skill_manuscript", () -> new SkillManuscript(new Item.Properties().stacksTo(64)));
}

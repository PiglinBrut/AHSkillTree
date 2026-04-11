package ru.pb.ahst;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import ru.pb.ahst.command.SkillDebugCommand;
import ru.pb.ahst.config.SkillConfig;
import ru.pb.ahst.data.SkillDataAttachments;
import ru.pb.ahst.registry.ItemRegistry;
import ru.pb.ahst.registry.NetworkRegistry;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(AHSkillTree.MOD_ID)
public class AHSkillTree {
    public static final String MOD_ID = "ahskilltree";
    public static final Logger LOGGER = LogUtils.getLogger();
    public AHSkillTree(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        //modEventBus.addListener(NetworkRegistry::register);

        ItemRegistry.register(modEventBus);

        SkillDataAttachments.ATTACHMENT_TYPES.register(modEventBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Инициализируем конфиг при загрузке
            SkillConfig.init(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        });
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
//        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
//            event.accept(EXAMPLE_BLOCK_ITEM);
//        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
        SkillDebugCommand.register(event.getServer().getCommands().getDispatcher());
    }
}

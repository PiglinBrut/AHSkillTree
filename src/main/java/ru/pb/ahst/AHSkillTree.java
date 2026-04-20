package ru.pb.ahst;

import net.neoforged.fml.loading.FMLPaths;
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
import ru.pb.ahst.config.*;
import ru.pb.ahst.data.SkillDataAttachments;
import ru.pb.ahst.registry.ItemRegistry;
import ru.pb.ahst.registry.NetworkRegistry;

import java.nio.file.Path;

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
            Path configDir = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
            SkillConfig.init(configDir);
            SkillEffectsConfig.init(configDir);
            ItemRestrictionsConfig.init(configDir);
            BaseAttributesConfig.init(configDir);
            ConfigManager.init(configDir);
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
//        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
//            event.accept(EXAMPLE_BLOCK_ITEM);
//        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
        SkillDebugCommand.register(event.getServer().getCommands().getDispatcher());
    }
}

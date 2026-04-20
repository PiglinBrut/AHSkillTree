package ru.pb.ahst.config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import ru.pb.ahst.AHSkillTree;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AHSkillTree.MOD_ID)
public class ConfigManager {
    private static Path configDir;
    private static final List<Runnable> reloadables = new ArrayList<>();

    public static void init(Path path) {
        configDir = path;

        // Регистрируем конфиги
        registerReloadable(() -> BaseAttributesConfig.init(configDir));
        registerReloadable(() -> ItemRestrictionsConfig.init(configDir));
        registerReloadable(() -> SkillConfig.init(configDir));
        registerReloadable(() -> SkillEffectsConfig.init(configDir));

        // Первичная загрузка
        reloadAll();
    }

    public static void registerReloadable(Runnable reloadable) {
        reloadables.add(reloadable);
    }

    public static void reloadAll() {
        for (Runnable reloadable : reloadables) {
            reloadable.run();
        }
        AHSkillTree.LOGGER.info("All configurations reloaded");
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
    }
}
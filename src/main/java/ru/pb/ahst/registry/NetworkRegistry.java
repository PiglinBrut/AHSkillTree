package ru.pb.ahst.registry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.util.network.*;

@EventBusSubscriber(modid = AHSkillTree.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkRegistry {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0");

        // Клиент -> Сервер
        registrar.playToServer(
                LearnSkillPacket.TYPE,
                LearnSkillPacket.STREAM_CODEC,
                LearnSkillPacket::handle
        );

        registrar.playToServer(
                RequestSkillsSyncPacket.TYPE,
                RequestSkillsSyncPacket.STREAM_CODEC,
                RequestSkillsSyncPacket::handle
        );

        // Сервер -> Клиент
        registrar.playToClient(
                OpenSkillTreeScreenPacket.TYPE,
                OpenSkillTreeScreenPacket.STREAM_CODEC,
                OpenSkillTreeScreenPacket::handle
        );

        registrar.playToClient(
                UpdateSkillsPacket.TYPE,
                UpdateSkillsPacket.STREAM_CODEC,
                UpdateSkillsPacket::handle
        );

        registrar.playToClient(
                SyncAllSkillsPacket.TYPE,
                SyncAllSkillsPacket.STREAM_CODEC,
                SyncAllSkillsPacket::handle
        );

        registrar.playToClient(
                CloseSkillScreenPacket.TYPE,
                CloseSkillScreenPacket.STREAM_CODEC,
                CloseSkillScreenPacket::handle
        );
    }
}
package ru.pb.ahst.registry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import ru.pb.ahst.util.network.OpenSkillTreeScreenPacket;
import ru.pb.ahst.AHSkillTree;

@EventBusSubscriber(modid = AHSkillTree.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkRegistry {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0");
        registrar.playToClient(
                OpenSkillTreeScreenPacket.TYPE,
                OpenSkillTreeScreenPacket.STREAM_CODEC,
                OpenSkillTreeScreenPacket::handle
        );
    }
}

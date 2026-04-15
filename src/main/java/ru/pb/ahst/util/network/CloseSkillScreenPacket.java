// CloseSkillScreenPacket.java
package ru.pb.ahst.util.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.screen.skill_tree.SkillTreeScreen;

public class CloseSkillScreenPacket implements CustomPacketPayload {
    public static final Type<CloseSkillScreenPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "close_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CloseSkillScreenPacket> STREAM_CODEC = StreamCodec.ofMember(
            (packet, buf) -> {},
            buf -> new CloseSkillScreenPacket()
    );

    public static void handle(CloseSkillScreenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof SkillTreeScreen) {
                Minecraft.getInstance().player.closeContainer();
                Minecraft.getInstance().setScreen(null);
                AHSkillTree.LOGGER.info("Closed skill tree screen due to reset");
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
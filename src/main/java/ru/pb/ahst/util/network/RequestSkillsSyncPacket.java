// RequestSkillsSyncPacket.java
package ru.pb.ahst.util.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.data.PlayerSkillData;
import ru.pb.ahst.data.SkillDataAttachments;

public class RequestSkillsSyncPacket implements CustomPacketPayload {
    public static final Type<RequestSkillsSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "request_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSkillsSyncPacket> STREAM_CODEC = StreamCodec.ofMember(
            (packet, buf) -> {},
            buf -> new RequestSkillsSyncPacket()
    );

    public static void handle(RequestSkillsSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                PlayerSkillData skillData = serverPlayer.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
                AHSkillTree.LOGGER.info("Server sending skill sync to client for player {}",
                        serverPlayer.getName().getString());
                PacketDistributor.sendToPlayer(serverPlayer, new SyncAllSkillsPacket(skillData.getLearnedSkills(), skillData.getSkillPoints()));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
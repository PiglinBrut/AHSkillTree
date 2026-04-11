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

public class LearnSkillPacket implements CustomPacketPayload {
    private final String skillId;
    private final int cost;
    public static final Type<LearnSkillPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "learn_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LearnSkillPacket> STREAM_CODEC = StreamCodec.ofMember(
            LearnSkillPacket::write,
            LearnSkillPacket::new
    );

    public LearnSkillPacket(String skillId, int cost) {
        this.skillId = skillId;
        this.cost = cost;
    }

    public LearnSkillPacket(FriendlyByteBuf buf) {
        this.skillId = buf.readUtf();
        this.cost = buf.readInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(skillId);
        buf.writeInt(cost);
    }

    public static void handle(LearnSkillPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                PlayerSkillData skillData = serverPlayer.getData(SkillDataAttachments.PLAYER_SKILL_DATA);

                boolean success = skillData.learnSkill(packet.skillId, packet.cost);

                if (success) {
                    AHSkillTree.LOGGER.info("Server: Player {} learned skill {}",
                            serverPlayer.getName().getString(), packet.skillId);

                    // Отправляем обновление всем (или только игроку)
                    PacketDistributor.sendToPlayer(serverPlayer,
                            new UpdateSkillsPacket(skillData.getLearnedSkills(), skillData.getSkillPoints()));
                } else {
                    AHSkillTree.LOGGER.warn("Server: Failed to learn skill {} for player {}",
                            packet.skillId, serverPlayer.getName().getString());
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
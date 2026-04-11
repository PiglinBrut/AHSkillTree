package ru.pb.ahst.util.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.config.SkillConfig;
import ru.pb.ahst.data.PlayerSkillData;
import ru.pb.ahst.data.SkillDataAttachments;

import java.util.HashSet;
import java.util.Set;

public class UpdateSkillsPacket implements CustomPacketPayload {
    private final Set<String> learnedSkills;
    private final int skillPoints;
    public static final Type<UpdateSkillsPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "update_skills"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateSkillsPacket> STREAM_CODEC = StreamCodec.ofMember(
            UpdateSkillsPacket::write,
            UpdateSkillsPacket::new
    );

    public UpdateSkillsPacket(Set<String> learnedSkills, int skillPoints) {
        this.learnedSkills = new HashSet<>(learnedSkills);
        this.skillPoints = skillPoints;
    }

    public UpdateSkillsPacket(FriendlyByteBuf buf) {
        this.learnedSkills = new HashSet<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            this.learnedSkills.add(buf.readUtf());
        }
        this.skillPoints = buf.readInt();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(learnedSkills.size());
        for (String skillId : learnedSkills) {
            buf.writeUtf(skillId);
        }
        buf.writeInt(skillPoints);
    }

    public static void handle(UpdateSkillsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                PlayerSkillData skillData = Minecraft.getInstance().player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
                skillData.loadSkills(packet.learnedSkills, packet.skillPoints);

                AHSkillTree.LOGGER.info("Client received update: {} skills, {} points",
                        packet.learnedSkills.size(), packet.skillPoints);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
package ru.pb.ahst.util.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.util.tools.SkillManuscript;

public class OpenSkillTreeScreenPacket implements CustomPacketPayload {
    private final InteractionHand hand;
    public static final Type<OpenSkillTreeScreenPacket> TYPE = new Type(ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "open_skill_tree_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSkillTreeScreenPacket> STREAM_CODEC = CustomPacketPayload.codec(OpenSkillTreeScreenPacket::write, OpenSkillTreeScreenPacket::new);

    public OpenSkillTreeScreenPacket(InteractionHand pHand) {
        this.hand = pHand;
    }

    public OpenSkillTreeScreenPacket(FriendlyByteBuf buf) {
        this.hand = (InteractionHand)buf.readEnum(InteractionHand.class);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.hand);
    }

    public static void handle(OpenSkillTreeScreenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> SkillManuscript.openSkillTreeScreen(packet.hand));
    }

    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

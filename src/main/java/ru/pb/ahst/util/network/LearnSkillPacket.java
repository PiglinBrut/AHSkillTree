package ru.pb.ahst.util.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.data.PlayerSkillData;
import ru.pb.ahst.data.SkillDataAttachments;
import ru.pb.ahst.effects.SkillEffectsManager;
import ru.pb.ahst.registry.ItemRegistry;

public class LearnSkillPacket implements CustomPacketPayload {
    private final String skillId;
    private final int cost;
    public static final Type<LearnSkillPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AHSkillTree.MOD_ID, "learn_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LearnSkillPacket> STREAM_CODEC = StreamCodec.ofMember(
            LearnSkillPacket::write,
            LearnSkillPacket::new
    );

    private static final ItemStack REQUIRED_ITEM = new ItemStack(ItemRegistry.SKILL_MANUSCRIPT.get()); // Замените на ваш предмет
    private static final int REQUIRED_ITEM_COUNT = 1;

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

                if (!hasRequiredItem(serverPlayer)) {
                    AHSkillTree.LOGGER.warn("Server: Player {} doesn't have required item for skill {}",
                            serverPlayer.getName().getString(), packet.skillId);
                    return; // Нет предмета - выходим
                }

                consumeRequiredItem(serverPlayer);

                boolean success = skillData.learnSkill(packet.skillId, packet.cost);

                if (success) {
                    AHSkillTree.LOGGER.info("Server: Player {} learned skill {}",
                            serverPlayer.getName().getString(), packet.skillId);

                    // Отправляем обновление всем (или только игроку)
                    PacketDistributor.sendToPlayer(serverPlayer,
                            new UpdateSkillsPacket(skillData.getLearnedSkills(), skillData.getSkillPoints()));
                    SkillEffectsManager.applyAllSkillEffects(serverPlayer, skillData);
                } else {
                    AHSkillTree.LOGGER.warn("Server: Failed to learn skill {} for player {}",
                            packet.skillId, serverPlayer.getName().getString());
                }
            }
        });
    }

    private static boolean hasRequiredItem(ServerPlayer player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameComponents(stack, REQUIRED_ITEM)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (ItemStack.isSameItemSameComponents(stack, REQUIRED_ITEM)) {
                count += stack.getCount();
            }
        }
        return count >= REQUIRED_ITEM_COUNT;
    }

    private static void consumeRequiredItem(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (ItemStack.isSameItemSameComponents(stack, REQUIRED_ITEM)) {
                stack.shrink(REQUIRED_ITEM_COUNT);
                if (stack.isEmpty()) {
                    player.getInventory().items.set(i, ItemStack.EMPTY);
                }
                return;
            }
        }

        for (int i = 0; i < player.getInventory().offhand.size(); i++) {
            ItemStack stack = player.getInventory().offhand.get(i);
            if (ItemStack.isSameItemSameComponents(stack, REQUIRED_ITEM)) {
                stack.shrink(REQUIRED_ITEM_COUNT);
                if (stack.isEmpty()) {
                    player.getInventory().offhand.set(i, ItemStack.EMPTY);
                }
                return;
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
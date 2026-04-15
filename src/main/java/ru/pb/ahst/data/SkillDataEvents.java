package ru.pb.ahst.data;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.effects.SkillEffectsManager;
import ru.pb.ahst.util.network.SyncAllSkillsPacket;

@EventBusSubscriber(modid = AHSkillTree.MOD_ID)
public class SkillDataEvents {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerSkillData data = serverPlayer.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
            data.setPlayer(serverPlayer);
            SkillEffectsManager.applyAllSkillEffects(serverPlayer, data);
            PacketDistributor.sendToPlayer(serverPlayer,
                    new SyncAllSkillsPacket(data.getLearnedSkills(), data.getSkillPoints()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerSkillData data = serverPlayer.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
            data.setPlayer(serverPlayer);
            SkillEffectsManager.applyAllSkillEffects(serverPlayer, data);
            PacketDistributor.sendToPlayer(serverPlayer,
                    new SyncAllSkillsPacket(data.getLearnedSkills(), data.getSkillPoints()));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerSkillData data = serverPlayer.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
            data.setPlayer(serverPlayer);
            SkillEffectsManager.applyAllSkillEffects(serverPlayer, data);
            PacketDistributor.sendToPlayer(serverPlayer,
                    new SyncAllSkillsPacket(data.getLearnedSkills(), data.getSkillPoints()));
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerSkillData data = serverPlayer.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
            data.setPlayer(serverPlayer);
            SkillEffectsManager.applyAllSkillEffects(serverPlayer, data);
            PacketDistributor.sendToPlayer(serverPlayer,
                    new SyncAllSkillsPacket(data.getLearnedSkills(), data.getSkillPoints()));
        }
    }
}
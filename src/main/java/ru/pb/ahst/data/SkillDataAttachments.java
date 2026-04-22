package ru.pb.ahst.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import ru.pb.ahst.AHSkillTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Supplier;

public class SkillDataAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, AHSkillTree.MOD_ID);
    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }

    // Используем Codec для сериализации
    public static final Codec<PlayerSkillData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.listOf().fieldOf("learned_skills").forGetter(data ->
                            new ArrayList<>(data.getLearnedSkills())),
                    Codec.INT.fieldOf("skill_points").forGetter(PlayerSkillData::getSkillPoints)
            ).apply(instance, (skillsList, points) -> {
                PlayerSkillData data = new PlayerSkillData(null);
                data.loadSkills(new HashSet<>(skillsList), points);
                return data;
            })
    );

    public static final Supplier<AttachmentType<PlayerSkillData>> PLAYER_SKILL_DATA =
            ATTACHMENT_TYPES.register(
                    "player_skill_data",
                    () -> AttachmentType.<PlayerSkillData>builder((player) -> new PlayerSkillData((Player) player))
                            .serialize(CODEC)
                            .copyOnDeath()
                            .build()
            );
}
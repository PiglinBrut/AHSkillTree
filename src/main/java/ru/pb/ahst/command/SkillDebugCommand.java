package ru.pb.ahst.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.config.SkillConfig;
import ru.pb.ahst.data.PlayerSkillData;
import ru.pb.ahst.data.SkillDataAttachments;
import ru.pb.ahst.screen.skill_tree.SkillData;
import ru.pb.ahst.screen.skill_tree.SkillTreeScreen;
import ru.pb.ahst.util.network.CloseSkillScreenPacket;
import ru.pb.ahst.util.network.SyncAllSkillsPacket;

import java.util.HashSet;
import java.util.Set;

public class SkillDebugCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ahskilltree")
                .then(Commands.literal("debug")
                        .executes(SkillDebugCommand::executeDebug))
                .then(Commands.literal("reload")
                        .executes(SkillDebugCommand::executeReload))
                .then(Commands.literal("save")
                        .executes(SkillDebugCommand::executeSave))
                .then(Commands.literal("sync")
                        .executes(SkillDebugCommand::executeSync))
                .then(Commands.literal("points")
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(SkillDebugCommand::executePointsAdd)))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(SkillDebugCommand::executePointsRemove)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(SkillDebugCommand::executePointsSet)))
                        .then(Commands.literal("get")
                                .executes(SkillDebugCommand::executePointsGet)))
                .then(Commands.literal("skill")
                        .then(Commands.literal("learn")
                                .then(Commands.argument("skill_id", StringArgumentType.word())
                                        .executes(SkillDebugCommand::executeSkillLearn)))
                        .then(Commands.literal("unlearn")
                                .then(Commands.argument("skill_id", StringArgumentType.word())
                                        .executes(SkillDebugCommand::executeSkillUnlearn)))
                        .then(Commands.literal("list")
                                .executes(SkillDebugCommand::executeSkillList))
                        .then(Commands.literal("info")
                                .then(Commands.argument("skill_id", StringArgumentType.word())
                                        .executes(SkillDebugCommand::executeSkillInfo))))
                .then(Commands.literal("reset")
                        .executes(SkillDebugCommand::executeReset))
                .then(Commands.literal("help")
                        .executes(SkillDebugCommand::executeHelp))
        );
    }

    // ========== Основные команды ==========

    private static int executeDebug(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            PlayerSkillData data = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);

            source.sendSuccess(() -> Component.literal("§6=== Skill Tree Debug ==="), false);
            source.sendSuccess(() -> Component.literal("§7Player: §f" + player.getName().getString()), false);
            source.sendSuccess(() -> Component.literal("§7Skill Points: §a" + data.getSkillPoints()), false);
            source.sendSuccess(() -> Component.literal("§7Learned skills: §e" + data.getLearnedSkills().size()), false);

            if (!data.getLearnedSkills().isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7Skills:"), false);
                for (String skillId : data.getLearnedSkills()) {
                    SkillData skill = SkillConfig.getSkill(skillId);
                    String skillName = skill != null ? skill.getName() : skillId;
                    source.sendSuccess(() -> Component.literal("  §a- " + skillName + " §7(§f" + skillId + "§7)"), false);
                }
            }
        } else {
            source.sendFailure(Component.literal("This command can only be used by a player!"));
        }

        return 1;
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            PlayerSkillData data = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);

            source.sendSuccess(() -> Component.literal("§aReloaded skill data from config!"), false);
            AHSkillTree.LOGGER.info("Player {} reloaded skill data", player.getName().getString());
        } else {
            source.sendFailure(Component.literal("This command can only be used by a player!"));
        }

        return 1;
    }

    private static int executeSave(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            source.sendSuccess(() -> Component.literal("§aForced save of skill data!"), false);
            AHSkillTree.LOGGER.info("Force saved skills for {}", player.getName().getString());
        } else {
            source.sendFailure(Component.literal("This command can only be used by a player!"));
        }

        return 1;
    }

    private static int executeSync(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            PlayerSkillData data = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
            PacketDistributor.sendToPlayer(player, new SyncAllSkillsPacket(data.getLearnedSkills(), data.getSkillPoints()));
            source.sendSuccess(() -> Component.literal("§aSync sent to client!"), false);
            AHSkillTree.LOGGER.info("Sync sent to player {}", player.getName().getString());
        } else {
            source.sendFailure(Component.literal("This command can only be used by a player!"));
        }

        return 1;
    }

    private static int executeReset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            PlayerSkillData data = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);

            int oldPoints = data.getSkillPoints();
            int returnedPoints = data.getLearnedSkills().size();  // сколько навыков было

            data.resetSkills();

            // Отправляем обновление клиенту
            PacketDistributor.sendToPlayer(player,
                    new SyncAllSkillsPacket(data.getLearnedSkills(), data.getSkillPoints()));

            // Закрываем экран, если он открыт
            PacketDistributor.sendToPlayer(player, new CloseSkillScreenPacket());

            source.sendSuccess(() -> Component.literal("§aAll skills have been reset!"), false);
            source.sendSuccess(() -> Component.literal("§eReturned " + returnedPoints + " skill points"), false);

            AHSkillTree.LOGGER.info("Player {} reset skills. Returned {} points (total now: {})",
                    player.getName().getString(), returnedPoints, data.getSkillPoints());
        } else {
            source.sendFailure(Component.literal("This command can only be used by a player!"));
        }
        return 1;
    }

    // ========== Команды для очков навыков ==========

    private static int executePointsAdd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            PlayerSkillData data = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
            int amount = IntegerArgumentType.getInteger(context, "amount");

            data.addSkillPoints(amount);
            PacketDistributor.sendToPlayer(player, new SyncAllSkillsPacket(data.getLearnedSkills(), data.getSkillPoints()));

            source.sendSuccess(() -> Component.literal("§aAdded §e" + amount + " §askill points! Total: §e" + data.getSkillPoints()), false);
            AHSkillTree.LOGGER.info("Added {} skill points to player {}, total: {}", amount, player.getName().getString(), data.getSkillPoints());
        } else {
            source.sendFailure(Component.literal("This command can only be used by a player!"));
        }

        return 1;
    }

    private static int executePointsRemove(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            PlayerSkillData data = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
            int amount = IntegerArgumentType.getInteger(context, "amount");

            if (data.getSkillPoints() >= amount) {
                data.addSkillPoints(-amount);
                PacketDistributor.sendToPlayer(player, new SyncAllSkillsPacket(data.getLearnedSkills(), data.getSkillPoints()));

                source.sendSuccess(() -> Component.literal("§aRemoved §e" + amount + " §askill points! Total: §e" + data.getSkillPoints()), false);
                AHSkillTree.LOGGER.info("Removed {} skill points from player {}, total: {}", amount, player.getName().getString(), data.getSkillPoints());
            } else {
                source.sendFailure(Component.literal("§cNot enough skill points! You have §e" + data.getSkillPoints() + " §cpoints."));
            }
        } else {
            source.sendFailure(Component.literal("This command can only be used by a player!"));
        }

        return 1;
    }

    private static int executePointsSet(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            PlayerSkillData data = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
            int amount = IntegerArgumentType.getInteger(context, "amount");

            data.setSkillPoints(amount);
            PacketDistributor.sendToPlayer(player, new SyncAllSkillsPacket(data.getLearnedSkills(), data.getSkillPoints()));

            source.sendSuccess(() -> Component.literal("§aSet skill points to §e" + amount), false);
            AHSkillTree.LOGGER.info("Set skill points of player {} to {}", player.getName().getString(), amount);
        } else {
            source.sendFailure(Component.literal("This command can only be used by a player!"));
        }

        return 1;
    }

    private static int executePointsGet(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            PlayerSkillData data = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
            source.sendSuccess(() -> Component.literal("§7You have §e" + data.getSkillPoints() + " §7skill points."), false);
        } else {
            source.sendFailure(Component.literal("This command can only be used by a player!"));
        }

        return 1;
    }

    // ========== Команды для навыков ==========

    private static int executeSkillLearn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            String skillId = StringArgumentType.getString(context, "skill_id");
            SkillData skill = SkillConfig.getSkill(skillId);
            PlayerSkillData data = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);

            if (skill == null) {
                source.sendFailure(Component.literal("§cSkill not found: " + skillId));
                return 1;
            }

            if (data.isLearned(skillId)) {
                source.sendFailure(Component.literal("§cSkill already learned: " + skill.getName()));
                return 1;
            }

            // Игнорируем стоимость для команды
            if (data.learnSkill(skillId, 0)) {
                PacketDistributor.sendToPlayer(player, new SyncAllSkillsPacket(data.getLearnedSkills(), data.getSkillPoints()));
                source.sendSuccess(() -> Component.literal("§aLearned skill: §e" + skill.getName()), false);
                AHSkillTree.LOGGER.info("Player {} learned skill {} via command", player.getName().getString(), skillId);
            } else {
                source.sendFailure(Component.literal("§cCannot learn this skill! Check prerequisites or conflicts."));
            }
        } else {
            source.sendFailure(Component.literal("This command can only be used by a player!"));
        }

        return 1;
    }

    private static int executeSkillUnlearn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            String skillId = StringArgumentType.getString(context, "skill_id");
            SkillData skill = SkillConfig.getSkill(skillId);
            PlayerSkillData data = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);

            if (skill == null) {
                source.sendFailure(Component.literal("§cSkill not found: " + skillId));
                return 1;
            }

            if (!data.isLearned(skillId)) {
                source.sendFailure(Component.literal("§cSkill not learned: " + skill.getName()));
                return 1;
            }

            // Удаляем навык
            data.removeSkill(skillId);
            PacketDistributor.sendToPlayer(player, new SyncAllSkillsPacket(data.getLearnedSkills(), data.getSkillPoints()));
            source.sendSuccess(() -> Component.literal("§aUnlearned skill: §e" + skill.getName()), false);
            AHSkillTree.LOGGER.info("Player {} unlearned skill {} via command", player.getName().getString(), skillId);
        } else {
            source.sendFailure(Component.literal("This command can only be used by a player!"));
        }

        return 1;
    }

    private static int executeSkillList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("§6=== Available Skills ==="), false);

        if (source.getEntity() instanceof ServerPlayer player) {
            PlayerSkillData data = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);

            for (SkillData skill : SkillConfig.getAllSkills()) {
                boolean learned = data.isLearned(skill.getId());
                String status = learned ? "§a✓" : "§c✗";
                source.sendSuccess(() -> Component.literal(status + " §7" + skill.getName() + " §8(§f" + skill.getId() + "§8)"), false);
            }

            source.sendSuccess(() -> Component.literal("§7Total learned: §e" + data.getLearnedSkills().size() + "§7/" + SkillConfig.getAllSkills().size()), false);
        } else {
            // Если команда с консоли — просто список без статуса
            for (SkillData skill : SkillConfig.getAllSkills()) {
                source.sendSuccess(() -> Component.literal("§7" + skill.getName() + " §8(§f" + skill.getId() + "§8)"), false);
            }
        }
        return 1;
    }

    private static int executeSkillInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String skillId = StringArgumentType.getString(context, "skill_id");
        SkillData skill = SkillConfig.getSkill(skillId);

        if (skill == null) {
            source.sendFailure(Component.literal("§cSkill not found: " + skillId));
            return 1;
        }

        source.sendSuccess(() -> Component.literal("§6=== " + skill.getName() + " §7(" + skill.getId() + ")§6 ==="), false);
        source.sendSuccess(() -> Component.literal("§7Description: §f" + skill.getDescription()), false);

        if (!skill.getPrerequisites().isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7Prerequisites: §e" + String.join(", ", skill.getPrerequisites())), false);
        }
        if (!skill.getConflicts().isEmpty()) {
            source.sendSuccess(() -> Component.literal("§cConflicts: §e" + String.join(", ", skill.getConflicts())), false);
        }

        source.sendSuccess(() -> Component.literal("§7Position: §fX=" + skill.getX() + ", Y=" + skill.getY()), false);

        if (source.getEntity() instanceof ServerPlayer player) {
            PlayerSkillData data = player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
            boolean learned = data.isLearned(skill.getId());
            source.sendSuccess(() -> Component.literal("§7Status: " + (learned ? "§aLearned" : "§cNot learned")), false);
        }

        return 1;
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("§6=== AHSkillTree Commands ==="), false);
        source.sendSuccess(() -> Component.literal("§7/ahskilltree debug §8- §fShow debug info"), false);
        source.sendSuccess(() -> Component.literal("§7/ahskilltree reload §8- §fReload config"), false);
        source.sendSuccess(() -> Component.literal("§7/ahskilltree save §8- §fForce save data"), false);
        source.sendSuccess(() -> Component.literal("§7/ahskilltree sync §8- §fSync with client"), false);
        source.sendSuccess(() -> Component.literal("§7/ahskilltree reset §8- §fReset all skills"), false);
        source.sendSuccess(() -> Component.literal("§7/ahskilltree points add/remove/set/get §8- §fManage skill points"), false);
        source.sendSuccess(() -> Component.literal("§7/ahskilltree skill learn/unlearn §8- §fManage skills"), false);
        source.sendSuccess(() -> Component.literal("§7/ahskilltree skill list §8- §fList all skills"), false);
        source.sendSuccess(() -> Component.literal("§7/ahskilltree skill info §8- §fShow skill info"), false);

        return 1;
    }
}
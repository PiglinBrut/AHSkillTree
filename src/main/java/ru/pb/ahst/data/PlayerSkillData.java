package ru.pb.ahst.data;

import net.minecraft.world.entity.player.Player;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.config.SkillConfig;
import ru.pb.ahst.effects.SkillEffectsManager;
import ru.pb.ahst.screen.skill_tree.SkillData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerSkillData {
    private Player player;
    private final Set<String> learnedSkills = new HashSet<>();
    private int skillPoints = 0;

    public PlayerSkillData(Player player) {
        this.player = player;
    }

    public void setPlayer(Player player) { this.player = player; }
    public Player getPlayer() { return player; }

    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int points) { this.skillPoints = points; }
    public void addSkillPoints(int points) { this.skillPoints += points; }

    public boolean spendSkillPoints(int cost) {
        if (skillPoints >= cost) {
            skillPoints -= cost;
            return true;
        }
        return false;
    }

    public boolean learnSkill(String skillId, int cost) {
        if (learnedSkills.contains(skillId) || !canLearn(skillId)) return false;

        if (!spendSkillPoints(cost)) {
            if (player != null) AHSkillTree.LOGGER.warn("Not enough skill points for {}", skillId);
            return false;
        }

        learnedSkills.add(skillId);

        reapplyEffects();

        if (player != null) {
            AHSkillTree.LOGGER.info("Player {} learned {}", player.getName().getString(), skillId);
        }
        return true;
    }

    public boolean canLearn(String skillId) {
        SkillData skill = SkillConfig.getSkill(skillId);
        if (skill == null || learnedSkills.contains(skillId)) return false;

        for (String conflict : skill.getConflicts()) {
            if (learnedSkills.contains(conflict)) return false;
        }

        Set<String> prerequisites = skill.getPrerequisites();

        // Если нет требований - можно изучать
        if (prerequisites.isEmpty()) return true;

        for (String prereqId : prerequisites) {
            if (learnedSkills.contains(prereqId)) {
                return true;
            }
        }

        return false;
    }

    public boolean isLearned(String skillId) {
        return learnedSkills.contains(skillId);
    }

    public Set<String> getLearnedSkills() {
        return new HashSet<>(learnedSkills);
    }

    public boolean removeSkill(String skillId) {
        boolean removed = learnedSkills.remove(skillId);
        if (removed) {
            reapplyEffects();
            if (player != null) {
                AHSkillTree.LOGGER.info("Player {} unlearned {}", player.getName().getString(), skillId);
            }
        }
        return removed;
    }

    public void resetSkills() {
        int learnedCount = learnedSkills.size();

        skillPoints += learnedCount;

        learnedSkills.clear();

        reapplyEffects();

        if (player != null) {
            AHSkillTree.LOGGER.info("Player {} reset all skills. Returned {} skill points. New total: {}",
                    player.getName().getString(), learnedCount, skillPoints);
        }
    }

    private void reapplyEffects() {
        if (player != null && !player.level().isClientSide) {
            SkillEffectsManager.applyAllSkillEffects(player, this);
        }
    }

    // Для Codec и пакетов
    public void loadSkills(Set<String> learned, int points) {
        learnedSkills.clear();
        learnedSkills.addAll(learned);
        this.skillPoints = points;
    }
}
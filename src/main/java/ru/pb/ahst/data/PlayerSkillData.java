package ru.pb.ahst.data;

import net.minecraft.world.entity.player.Player;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.config.SkillConfig;
import ru.pb.ahst.screen.skill_tree.SkillData;

import java.util.HashSet;
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
        if (player != null) {
            AHSkillTree.LOGGER.info("Player {} learned {}", player.getName().getString(), skillId);
        }
        return true;
    }

    public boolean canLearn(String skillId) {
        SkillData skill = SkillConfig.getSkill(skillId);
        if (skill == null || learnedSkills.contains(skillId)) return false;

        if (!skill.getPrerequisites().isEmpty() && !learnedSkills.containsAll(skill.getPrerequisites())) {
            return false;
        }

        for (String conflict : skill.getConflicts()) {
            if (learnedSkills.contains(conflict)) return false;
        }
        return true;
    }

    public boolean isLearned(String skillId) {
        return learnedSkills.contains(skillId);
    }

    public Set<String> getLearnedSkills() {
        return new HashSet<>(learnedSkills);
    }

    public boolean removeSkill(String skillId) {
        return learnedSkills.remove(skillId);
    }

    public void resetSkills() {
        int learnedCount = learnedSkills.size();

        skillPoints += learnedCount;

        learnedSkills.clear();

        if (player != null) {
            AHSkillTree.LOGGER.info("Player {} reset all skills. Returned {} skill points. New total: {}",
                    player.getName().getString(), learnedCount, skillPoints);
        }
    }

    // Для Codec и пакетов
    public void loadSkills(Set<String> learned, int points) {
        learnedSkills.clear();
        learnedSkills.addAll(learned);
        this.skillPoints = points;
    }
}
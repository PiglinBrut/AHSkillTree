package ru.pb.ahst.effects.conditions;

import ru.pb.ahst.data.PlayerSkillData;
import ru.pb.ahst.data.SkillDataAttachments;
import ru.pb.ahst.effects.ConditionContext;

public class SkillLearnedCondition implements Condition {
    private final String requiredSkillId;

    public SkillLearnedCondition(String requiredSkillId) {
        this.requiredSkillId = requiredSkillId;
    }

    @Override
    public boolean test(ConditionContext context) {
        if (context.player == null) {
            return false;
        }

        PlayerSkillData skillData = context.player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
        boolean hasSkill = skillData.hasSkill(requiredSkillId);

        return hasSkill;
    }
}
package ru.pb.ahst.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import ru.pb.ahst.AHSkillTree;

import java.util.List;
import java.util.Set;

public class BetterCombatMixinPlugin implements IMixinConfigPlugin {

    private static boolean hasBetterCombat = false;

    static {
        try {
            Class.forName("net.bettercombat.BetterCombatMod");
            hasBetterCombat = true;
            AHSkillTree.LOGGER.info("[AHSkillTree] Better Combat detected, enabling compatibility mixin");
        } catch (ClassNotFoundException e) {
            hasBetterCombat = false;
            AHSkillTree.LOGGER.info("[AHSkillTree] Better Combat not detected, mixin will be skipped");
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Применяем миксин ТОЛЬКО если Better Combat установлен
        boolean shouldApply = hasBetterCombat;
        if (!shouldApply) {
            AHSkillTree.LOGGER.debug("[AHSkillTree] Skipping mixin {} (Better Combat not present)", mixinClassName);
        }
        return shouldApply;
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
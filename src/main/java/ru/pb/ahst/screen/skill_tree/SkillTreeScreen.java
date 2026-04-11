package ru.pb.ahst.screen.skill_tree;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.render.RenderHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec2;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector4f;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.config.ItemRestrictionsConfig;
import ru.pb.ahst.config.SkillConfig;
import ru.pb.ahst.config.SkillEffectsConfig;
import ru.pb.ahst.data.PlayerSkillData;
import ru.pb.ahst.data.SkillDataAttachments;
import ru.pb.ahst.util.network.LearnSkillPacket;
import ru.pb.ahst.util.network.RequestSkillsSyncPacket;

import java.util.ArrayList;
import java.util.List;

public class SkillTreeScreen extends Screen {

    private static final ResourceLocation WINDOW_LOCATION = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/gui/eldritch_research_screen/window.png");
    private static final ResourceLocation FRAME_LOCATION = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/gui/eldritch_research_screen/spell_frame.png");

    public static int WINDOW_WIDTH;
    public static int WINDOW_HEIGHT;
    public static final int WINDOW_INSIDE_WIDTH = 234;
    public static final int WINDOW_INSIDE_HEIGHT = 229;

    int leftPos;
    int topPos;
    InteractionHand activeHand;

    private List<SkillNode> nodes;
    private PlayerSkillData skillData;

    private Vec2 viewportOffset = Vec2.ZERO;
    private boolean isDragging;

    private int selectedSkillIndex = -1;
    private int holdProgress = -1;
    private static final int HOLD_TIME = 15;

    public SkillTreeScreen(Component pTitle, InteractionHand activeHand) {
        super(pTitle);
        this.activeHand = activeHand;
    }

    @Override
    protected void init() {
        super.init();

        WINDOW_WIDTH = (int) (this.width * 0.9);
        WINDOW_HEIGHT = (int) (this.height * 0.9);
        this.leftPos = (this.width - WINDOW_WIDTH) / 2;
        this.topPos = (this.height - WINDOW_HEIGHT) / 2;

        if (Minecraft.getInstance().player != null) {
            skillData = Minecraft.getInstance().player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
            if (skillData.getPlayer() == null) skillData.setPlayer(Minecraft.getInstance().player);

            PacketDistributor.sendToServer(new RequestSkillsSyncPacket());
            AHSkillTree.LOGGER.info("Requested skill sync from server");
        }

        // Загружаем только статические данные навыков
        this.nodes = new ArrayList<>();
        for (SkillData skill : SkillConfig.getAllSkills()) {
            nodes.add(new SkillNode(skill, skill.getX(), skill.getY()));
        }

        viewportOffset = Vec2.ZERO;
        selectedSkillIndex = -1;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
//        this.drawBackdrop(guiGraphics);

        renderConnections(guiGraphics, partialTick);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int i = 0; i < nodes.size(); i++) {
            SkillNode node = nodes.get(i);
            boolean isSelected = i == selectedSkillIndex;
            boolean isLearned = skillData.isLearned(node.skill.getId());
            boolean canLearn = skillData.canLearn(node.skill.getId());

            drawNode(guiGraphics, node, isSelected, canLearn, isLearned,
                    isSelected && holdProgress >= 0 ? holdProgress / (float) HOLD_TIME : 0);
        }

        guiGraphics.blit(WINDOW_LOCATION, leftPos, topPos, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Tooltip
        for (SkillNode node : nodes) {
            if (isHoveringNode(node, mouseX, mouseY)) {
                renderTooltip(guiGraphics, node.skill, mouseX, mouseY);
                break;
            }
        }

        RenderSystem.disableBlend();
    }

    public void refreshData() {
        // Перезагружаем данные из конфига
        if (Minecraft.getInstance().player != null) {
            skillData = Minecraft.getInstance().player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
        }

        // Пересоздаем узлы
        List<SkillData> skills = SkillConfig.getAllSkills();
        this.nodes.clear();

        int centerX = this.leftPos + WINDOW_WIDTH / 2;
        int centerY = this.topPos + WINDOW_HEIGHT / 2;

        for (SkillData skill : skills) {
            int x = centerX + skill.getX();
            int y = centerY + skill.getY();
            nodes.add(new SkillNode(skill, x, y));
        }

        AHSkillTree.LOGGER.info("Skill tree screen refreshed");
    }

    private void drawNode(GuiGraphics guiGraphics, SkillNode node, boolean isSelected,
                          boolean canLearn, boolean isLearned, float progress) {
        int screenX = leftPos + WINDOW_WIDTH / 2 + (int) viewportOffset.x + node.relX;
        int screenY = topPos + WINDOW_HEIGHT / 2 + (int) viewportOffset.y + node.relY;

        int frameU = isLearned ? 32 : 0;
        guiGraphics.blit(FRAME_LOCATION, screenX - 8, screenY - 8, frameU, 0, 32, 32, 64, 32);

        ResourceLocation skillTexture = ResourceLocation.fromNamespaceAndPath(
                AHSkillTree.MOD_ID, "textures/gui/skills/" + node.skill.getId() + ".png");
        RenderSystem.setShaderTexture(0, skillTexture);
        guiGraphics.blit(skillTexture, screenX, screenY, 0, 0, 16, 16, 16, 16);

        if (!canLearn && !isLearned) {
            guiGraphics.fill(screenX - 8, screenY - 8, screenX + 24, screenY + 24, 0xAA000000);
        }

        if (isSelected && progress > 0 && !isLearned && canLearn) {
            guiGraphics.fill(screenX, screenY, screenX + (int)(16 * progress), screenY + 16, 0x88FFFFFF);
        }
    }

    private void renderConnections(GuiGraphics guiGraphics, float partialTick) {
        if (nodes.isEmpty()) return;

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        float f = Mth.sin(((float)Minecraft.getInstance().player.tickCount + partialTick) * 0.1F);
        float glowIntensity = f * f * 0.8F + 0.2F;

        Vector4f normalColor = new Vector4f(0.5294118F, 0.6039216F, 0.68235296F, 0.5F);
        Vector4f glowColor = new Vector4f(0.95686275F, 0.25490198F, 1.0F, 0.5F);

        int centerX = leftPos + WINDOW_WIDTH / 2;
        int centerY = topPos + WINDOW_HEIGHT / 2;

        for (SkillNode node : nodes) {
            for (String prereqId : node.skill.getPrerequisites()) {
                SkillNode prereqNode = findNodeById(prereqId);
                if (prereqNode != null) {
                    int x1 = centerX + (int) viewportOffset.x + prereqNode.relX + 8;
                    int y1 = centerY + (int) viewportOffset.y + prereqNode.relY + 8;
                    int x2 = centerX + (int) viewportOffset.x + node.relX + 8;
                    int y2 = centerY + (int) viewportOffset.y + node.relY + 8;

                    drawConnection(guiGraphics, x1, y1, x2, y2, glowIntensity, normalColor, glowColor);
                }
            }
        }

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
    }

    private void drawConnection(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2,
                                float glowIntensity, Vector4f normalColor, Vector4f glowColor) {

        Vec2 direction = new Vec2(x2 - x1, y2 - y1);
        Vec2 perpendicular = new Vec2(-direction.y, direction.x).normalized().scale(1.5F);

        // Простая подсветка (можно улучшить позже)
        Vector4f color1 = lerpColor(normalColor, glowColor, glowIntensity);
        Vector4f color2 = lerpColor(normalColor, glowColor, glowIntensity);

        RenderHelper.quadBuilder()
                .vertex(x1 + perpendicular.x, y1 + perpendicular.y).color(colorToInt(fadeOutTowardEdges(x1, y1, color1)))
                .vertex(x2 + perpendicular.x, y2 + perpendicular.y).color(colorToInt(fadeOutTowardEdges(x2, y2, color2)))
                .vertex(x2 - perpendicular.x, y2 - perpendicular.y).color(colorToInt(fadeOutTowardEdges(x2, y2, color2)))
                .vertex(x1 - perpendicular.x, y1 - perpendicular.y).color(colorToInt(fadeOutTowardEdges(x1, y1, color1)))
                .build(guiGraphics, RenderType.gui());
    }

    private void renderTooltip(GuiGraphics guiGraphics, SkillData skill, int mouseX, int mouseY) {
        List<FormattedCharSequence> tooltip = new ArrayList<>();

        MutableComponent name = Component.literal(skill.getName())
                .withStyle(skillData.isLearned(skill.getId()) ? ChatFormatting.GREEN :
                        skillData.canLearn(skill.getId()) ? ChatFormatting.RED : ChatFormatting.GRAY);
        tooltip.add(name.getVisualOrderText());

        tooltip.addAll(this.font.split(Component.literal(skill.getDescription()), 180));
        tooltip.add(FormattedCharSequence.EMPTY);

        // Статус
        Component status;
        if (skillData.isLearned(skill.getId())) {
            status = Component.literal("✓ Изучено").withStyle(ChatFormatting.GREEN);
        } else if (skillData.canLearn(skill.getId())) {
            status = Component.literal("✗ Можно изучить").withStyle(ChatFormatting.YELLOW);
        } else {
            String prereqList = String.join(", ", skill.getPrerequisites());
            status = Component.literal("⛔ Требуются навыки: " + prereqList).withStyle(ChatFormatting.GRAY);
        }
        tooltip.add(status.getVisualOrderText());

        guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Проверяем клик по узлам
        for (int i = 0; i < nodes.size(); i++) {
            SkillNode node = nodes.get(i);
            String skillId = node.skill.getId();

            if (isHoveringNode(node, (int) mouseX, (int) mouseY)) {

                // НОВЫЕ ПРОВЕРКИ ПЕРЕД НАЧАЛОМ HOLD
                if (skillData.isLearned(skillId)) {
                    return true; // уже изучен
                }

                if (!skillData.canLearn(skillId)) {
                    // Можно добавить визуальный/звуковой отклик "нельзя изучить"
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.playNotifySound(
                                SoundEvents.VILLAGER_NO, SoundSource.MASTER, 0.8f, 1.0f);
                    }
                    return true;
                }

                // Главная проверка: достаточно ли очков
                int cost = 1; // или getSkillCost(skillId), если у тебя разная стоимость
                if (skillData.getSkillPoints() < cost) {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.playNotifySound(
                                SoundEvents.VILLAGER_NO, SoundSource.MASTER, 1.0f, 0.8f);
                        // Опционально: показать сообщение
                        Minecraft.getInstance().player.displayClientMessage(
                                Component.literal("§cНедостаточно очков навыков!").withStyle(ChatFormatting.RED),
                                true);
                    }
                    return true;
                }

                // Если все проверки прошли — начинаем hold
                selectedSkillIndex = i;
                holdProgress = 0;
//                isHolding = true;   // если у тебя есть это поле
                return true;
            }
        }

        // Панорамирование окна
        if (isHoveringWindow((int) mouseX, (int) mouseY)) {
            isDragging = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (isDragging && button == 0) {
            viewportOffset = new Vec2((float) (viewportOffset.x + dx), (float) (viewportOffset.y + dy));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (selectedSkillIndex != -1) {
            // Если отпустили раньше времени — сбрасываем прогресс
            if (holdProgress < HOLD_TIME) {
                selectedSkillIndex = -1;
                holdProgress = -1;
            }
        }
        isDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        super.tick();

        if (selectedSkillIndex != -1 && holdProgress >= 0) {
            SkillNode node = nodes.get(selectedSkillIndex);
            String skillId = node.skill.getId();

            if (skillData.isLearned(skillId) || !skillData.canLearn(skillId)) {
                // Сброс, если стало невозможно
                selectedSkillIndex = -1;
                holdProgress = -1;
                return;
            }

            holdProgress++;

            // Звук тика каждые 2 тика
            if (holdProgress % 3 == 0 && Minecraft.getInstance().player != null) {
                float pitch = 0.6f + (holdProgress / (float) HOLD_TIME) * 0.9f;
                Minecraft.getInstance().player.playNotifySound(
                        SoundRegistry.UI_TICK.get(), SoundSource.MASTER, 0.4f, pitch
                );
            }

            // Успешное изучение
            if (holdProgress >= HOLD_TIME) {
                int cost = 1;
                if (skillData.getSkillPoints() >= cost) {
                    PacketDistributor.sendToServer(new LearnSkillPacket(skillId, cost));
                    boolean success = skillData.learnSkill(skillId, cost);

                    if (success) {
                        Minecraft.getInstance().player.playNotifySound(
                                SoundRegistry.LEARN_ELDRITCH_SPELL.get(), SoundSource.MASTER, 1.0f, 1.0f);
                    }
                }

                selectedSkillIndex = -1;
                holdProgress = -1;
            }
        }
    }

    private SkillNode findNodeById(String id) {
        return nodes.stream().filter(n -> n.skill.getId().equals(id)).findFirst().orElse(null);
    }

    private int getSkillCost(String skillId) {
        return 1;
    }

    private Vector4f lerpColor(Vector4f a, Vector4f b, float delta) {
        float f = 1.0F - delta;
        return new Vector4f(a.x() * f + b.x() * delta, a.y() * f + b.y() * delta,
                a.z() * f + b.z() * delta, a.w() * f + b.w() * delta);
    }

    private Vector4f fadeOutTowardEdges(double x, double y, Vector4f color) {
        float margin = 40.0F;
        int boundXMin = (int)Mth.clamp(x - this.leftPos, 0.0F, (float)WINDOW_WIDTH);
        int boundXMax = WINDOW_WIDTH - (int)Mth.clamp(x - this.leftPos, 0.0F, (float)WINDOW_WIDTH);
        int boundYMin = (int)Mth.clamp(y - this.topPos, 0.0F, (float)WINDOW_HEIGHT);
        int boundYMax = WINDOW_HEIGHT - (int)Mth.clamp(y - this.topPos, 0.0F, (float)WINDOW_HEIGHT);

        float px = Mth.clamp((float)Math.min(boundXMin, boundXMax) / margin, 0.0F, 1.0F);
        float py = Mth.clamp((float)Math.min(boundYMin, boundYMax) / margin, 0.0F, 1.0F);
        float alpha = Mth.sqrt(px * py);

        return new Vector4f(color.x(), color.y(), color.z(), color.w() * alpha);
    }

    private int colorToInt(Vector4f color) {
        return ((int)(color.w() * 255) << 24) | ((int)(color.x() * 255) << 16) |
                ((int)(color.y() * 255) << 8) | ((int)(color.z() * 255));
    }

    private void drawBackdrop(GuiGraphics guiGraphics, int left, int top) {
        float f = Minecraft.getInstance().player != null ? (float)Minecraft.getInstance().player.tickCount * 0.02F : 0.0F;
        float color = (Mth.sin(f) + 1.0F) * 0.25F + 0.15F;
        RenderHelper.quadBuilder()
                .vertex((float)left + 9, (float)(this.height * 0.9) + 18)
                .vertex((float)(this.width * 0.9) + 9, (float)(this.height * 0.9) + 18)
                .vertex((float)(this.width * 0.9) + 9, (float)top + 18)
                .vertex((float)left + 9, (float)top + 18)
                .color(0.0F, 0.0F, 0.0F, color)
                .build(guiGraphics, RenderType.endPortal());
    }

    private boolean isHoveringNode(SkillNode node, int mouseX, int mouseY) {
        int screenX = leftPos + WINDOW_WIDTH / 2 + (int) viewportOffset.x + node.relX;
        int screenY = topPos + WINDOW_HEIGHT / 2 + (int) viewportOffset.y + node.relY;
        return mouseX >= screenX && mouseY >= screenY && mouseX < screenX + 16 && mouseY < screenY + 16;
    }

    private boolean isHoveringWindow(int mouseX, int mouseY) {
        return mouseX >= leftPos && mouseY >= topPos &&
                mouseX < leftPos + WINDOW_WIDTH && mouseY < topPos + WINDOW_HEIGHT;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (this.minecraft.options.keyInventory.isActiveAndMatches(key) || keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            SkillConfig.init(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
            SkillEffectsConfig.init(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());   // ← добавлено
            ItemRestrictionsConfig.init(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get()); // ← добавлено
        });
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static class SkillNode {
        final SkillData skill;
        final int relX, relY;

        SkillNode(SkillData skill, int relX, int relY) {
            this.skill = skill;
            this.relX = relX;
            this.relY = relY;
        }
    }
}
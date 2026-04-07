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
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class SkillTreeScreen extends Screen {

    // Текстуры
    private static final ResourceLocation WINDOW_LOCATION = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/gui/eldritch_research_screen/window.png");
    private static final ResourceLocation FRAME_LOCATION = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "textures/gui/eldritch_research_screen/spell_frame.png");

    // Размеры окна
    public static final int WINDOW_WIDTH = 252;
    public static final int WINDOW_HEIGHT = 256;
    public static final int WINDOW_INSIDE_WIDTH = 234;
    public static final int WINDOW_INSIDE_HEIGHT = 229;

    // Позиции
    int leftPos;
    int topPos;
    InteractionHand activeHand;

    // Данные навыков
    private List<SkillData> skills;
    private List<SkillNode> nodes;

    // Панорамирование
    private Vec2 viewportOffset = Vec2.ZERO;
    private boolean isDragging;
    private double lastMouseX;
    private double lastMouseY;

    // Выбор навыка
    private int selectedSkillIndex = -1;
    private int holdProgress = -1;
    private static final int HOLD_TIME = 15;

    public SkillTreeScreen(Component pTitle, InteractionHand activeHand) {
        super(pTitle);
        this.activeHand = activeHand;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - WINDOW_WIDTH) / 2;
        this.topPos = (this.height - WINDOW_HEIGHT) / 2;

        // СОЗДАЕМ ТЕСТОВЫЕ НАВЫКИ
        this.skills = new ArrayList<>();
        this.skills.add(new SkillData("test1", "Тест1", "Описание"));
        this.skills.add(new SkillData("test2", "Тест2", "Описание"));
        this.skills.add(new SkillData("test3", "Тест3", "Описание"));
        this.skills.add(new SkillData("test4", "Тест4", "Описание"));
        this.skills.add(new SkillData("test5", "Тест5", "Описание"));
        this.skills.add(new SkillData("test6", "Тест6", "Описание"));
        this.skills.add(new SkillData("test7", "Тест7", "Описание"));
        this.skills.add(new SkillData("test8", "Тест8", "Описание"));
        this.skills.add(new SkillData("test9", "Тест9", "Описание"));

        // ГЕНЕРИРУЕМ ПОЗИЦИИ НАВЫКОВ (по кругу)
        this.nodes = new ArrayList<>();
        int radius = 80;
        int centerX = this.leftPos + WINDOW_WIDTH / 2;
        int centerY = this.topPos + WINDOW_HEIGHT / 2;

        for (int i = 0; i < skills.size(); i++) {
            double angle = (2 * Math.PI / skills.size()) * i - Math.PI / 2;
            int x = centerX + (int)(radius * Math.cos(angle)) - 8;
            int y = centerY + (int)(radius * Math.sin(angle)) - 8;
            nodes.add(new SkillNode(skills.get(i), x, y));
        }
    }

    private void drawBackdrop(GuiGraphics guiGraphics, int left, int top) {
        float f = Minecraft.getInstance().player != null ? (float)Minecraft.getInstance().player.tickCount * 0.02F : 0.0F;
        float color = (Mth.sin(f) + 1.0F) * 0.25F + 0.15F;
        RenderHelper.QuadBuilder definitelynothowabuilderworks = RenderHelper.quadBuilder().vertex((float)left, (float)(top + 229)).vertex((float)(left + 234), (float)(top + 229)).vertex((float)(left + 234), (float)top).vertex((float)left, (float)top).color(0.0F, 0.0F, 0.0F, color);
        definitelynothowabuilderworks.build(guiGraphics, RenderType.endPortal());
        definitelynothowabuilderworks.build(guiGraphics, RenderType.guiOverlay());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Фон
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        this.drawBackdrop(guiGraphics, this.leftPos + 9, this.topPos + 18);

        // Отрисовка соединений между навыками
        renderConnections(guiGraphics, partialTick);

        // Отрисовка узлов (иконок навыков)
        for (int i = 0; i < nodes.size(); i++) {
            SkillNode node = nodes.get(i);
            boolean isSelected = (i == selectedSkillIndex);
            drawNode(guiGraphics, node, isSelected, holdProgress / (float)HOLD_TIME);
        }

        // Отрисовка рамки окна
        RenderSystem.enableBlend();
        guiGraphics.blit(WINDOW_LOCATION, leftPos, topPos, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Отрисовка тултипа при наведении
        for (int i = 0; i < nodes.size(); i++) {
            SkillNode node = nodes.get(i);
            if (isHoveringNode(node, mouseX, mouseY)) {
                renderTooltip(guiGraphics, node.skill, mouseX, mouseY);
                break;
            }
        }
    }

    private void renderConnections(GuiGraphics guiGraphics, float partialTick) {
        if (nodes.size() < 2) return;

        RenderSystem.enableDepthTest();

        // Эффект пульсации
        float f = Mth.sin(((float)Minecraft.getInstance().player.tickCount + partialTick) * 0.1F);
        float glowIntensity = f * f * 0.8F + 0.2F;

        // Цвета: обычный и свечение
        Vector4f normalColor = new Vector4f(0.5294118F, 0.6039216F, 0.68235296F, 0.5F);
        Vector4f glowColor = new Vector4f(0.95686275F, 0.25490198F, 1.0F, 0.5F);

        // Рисуем соединения между всеми соседними узлами
        for (int i = 0; i < nodes.size(); i++) {
            SkillNode currentNode = nodes.get(i);
            SkillNode nextNode = nodes.get((i + 1) % nodes.size()); // Замыкаем круг

            // Позиции узлов (центры иконок)
            float x1 = currentNode.x + 8 + viewportOffset.x;
            float y1 = currentNode.y + 8 + viewportOffset.y;
            float x2 = nextNode.x + 8 + viewportOffset.x;
            float y2 = nextNode.y + 8 + viewportOffset.y;

            // Вектор перпендикуляра для толщины линии
            Vec2 direction = new Vec2(x2 - x1, y2 - y1);
            Vec2 perpendicular = new Vec2(-direction.y, direction.x).normalized().scale(1.5F);

            // 4 точки для рисования "толстой" линии
            float x1m1 = x1 + perpendicular.x;
            float y1m1 = y1 + perpendicular.y;
            float x2m1 = x2 + perpendicular.x;
            float y2m1 = y2 + perpendicular.y;
            float x1m2 = x1 - perpendicular.x;
            float y1m2 = y1 - perpendicular.y;
            float x2m2 = x2 - perpendicular.x;
            float y2m2 = y2 - perpendicular.y;

            // Цвет с учетом изученности навыков
            float intensity1 = glowIntensity * (currentNode.skill.isLearned() ? 1 : 0);
            float intensity2 = glowIntensity * (nextNode.skill.isLearned() ? 1 : 0);

            Vector4f color1 = lerpColor(normalColor, glowColor, intensity1);
            Vector4f color2 = lerpColor(normalColor, glowColor, intensity2);

            // Применяем затухание к краям экрана
            Vector4f finalColor1 = fadeOutTowardEdges(x1m1, y1m1, color1);
            Vector4f finalColor2 = fadeOutTowardEdges(x2m1, y2m1, color2);
            Vector4f finalColor3 = fadeOutTowardEdges(x2m2, y2m2, color2);
            Vector4f finalColor4 = fadeOutTowardEdges(x1m2, y1m2, color1);

            // Рисуем линию
            RenderHelper.quadBuilder()
                    .vertex(x1m1, y1m1).color(colorToInt(finalColor1))
                    .vertex(x2m1, y2m1).color(colorToInt(finalColor2))
                    .vertex(x2m2, y2m2).color(colorToInt(finalColor3))
                    .vertex(x1m2, y1m2).color(colorToInt(finalColor4))
                    .build(guiGraphics, RenderType.gui());
        }
    }

    private Vector4f lerpColor(Vector4f a, Vector4f b, float delta) {
        float f = 1.0F - delta;
        return new Vector4f(
                a.x() * f + b.x() * delta,
                a.y() * f + b.y() * delta,
                a.z() * f + b.z() * delta,
                a.w() * f + b.w() * delta
        );
    }

    private Vector4f fadeOutTowardEdges(double x, double y, Vector4f color) {
        float margin = 40.0F;
        int maxWidth = WINDOW_WIDTH;
        int maxHeight = WINDOW_HEIGHT;

        int boundXMin = (int)Mth.clamp(x - this.leftPos, 0.0F, (float)maxWidth);
        int boundXMax = maxWidth - (int)Mth.clamp(x - this.leftPos, 0.0F, (float)maxWidth);
        int boundYMin = (int)Mth.clamp(y - this.topPos, 0.0F, (float)maxHeight);
        int boundYMax = maxHeight - (int)Mth.clamp(y - this.topPos, 0.0F, (float)maxHeight);

        float px = Mth.clamp((float)Math.min(boundXMin, boundXMax) / margin, 0.0F, 1.0F);
        float py = Mth.clamp((float)Math.min(boundYMin, boundYMax) / margin, 0.0F, 1.0F);
        float alpha = Mth.sqrt(px * py);

        return new Vector4f(color.x(), color.y(), color.z(), color.w() * alpha);
    }

    private int colorToInt(Vector4f color) {
        int r = (int)(color.x() * 255.0F) & 255;
        int g = (int)(color.y() * 255.0F) & 255;
        int b = (int)(color.z() * 255.0F) & 255;
        int a = (int)(color.w() * 255.0F) & 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void drawNode(GuiGraphics guiGraphics, SkillNode node, boolean isSelected, float progress) {
        int x = node.x;
        int y = node.y;

        // Рамка (выучен/не выучен)
        int frameU = node.skill.isLearned() ? 32 : 0;
        guiGraphics.blit(FRAME_LOCATION, x - 8, y - 8, frameU, 0, 32, 32, 64, 32);

        // Иконка навыка
        int color = node.skill.isLearned() ? 0xFF00FF00 : 0xFFFF0000;
        guiGraphics.fill(x, y, x + 16, y + 16, color);

        // ✅ Прогресс показываем ТОЛЬКО для не изученных и выбранных
        if (isSelected && !node.skill.isLearned() && progress > 0) {
            guiGraphics.fill(x, y, x + (int)(16 * progress), y + 16, 0x88FFFFFF);
        }
    }

    private void renderTooltip(GuiGraphics guiGraphics, SkillData skill, int mouseX, int mouseY) {
        List<FormattedCharSequence> tooltip = new ArrayList<>();

        // Название
        MutableComponent name = Component.literal(skill.getName())
                .withStyle(skill.isLearned() ? ChatFormatting.GREEN : ChatFormatting.RED);
        tooltip.add(name.getVisualOrderText());

        // Описание
        tooltip.addAll(this.font.split(Component.literal(skill.getDescription()), 180));

        // Пустая строка
        tooltip.add(FormattedCharSequence.EMPTY);

        // Статус
        Component status = skill.isLearned()
                ? Component.literal("✓ Уже изучено").withStyle(ChatFormatting.GREEN)
                : Component.literal("✗ Не изучено").withStyle(ChatFormatting.RED);
        tooltip.add(status.getVisualOrderText());

        guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Проверяем клик по навыку
        for (int i = 0; i < nodes.size(); i++) {
            SkillNode node = nodes.get(i);
            if (isHoveringNode(node, (int)mouseX, (int)mouseY)) {
                // ✅ Нельзя начать изучение уже изученного навыка
                if (!node.skill.isLearned()) {
                    selectedSkillIndex = i;
                    holdProgress = 0;
                }
                return true;
            }
        }

        // Начало панорамирования
        if (isHoveringWindow((int)mouseX, (int)mouseY)) {
            isDragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // ✅ Просто сбрасываем прогресс, НО НЕ ИЗУЧАЕМ
        if (selectedSkillIndex != -1) {
            // Если не до конца зажали - сбрасываем (без изучения)
            selectedSkillIndex = -1;
            holdProgress = -1;
        }

        isDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (isDragging) {
            // Панорамирование
            double dx = mouseX - lastMouseX;
            double dy = mouseY - lastMouseY;
            viewportOffset = new Vec2(
                    (float)(viewportOffset.x + dx),
                    (float)(viewportOffset.y + dy)
            );
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Закрытие по ESC или E
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (this.minecraft.options.keyInventory.isActiveAndMatches(key) || keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();

        // ✅ Изучаем навык, когда прогресс достиг HOLD_TIME
        if (selectedSkillIndex != -1 && holdProgress >= 0) {
            SkillData currentSkill = nodes.get(selectedSkillIndex).skill;

            // Проверяем, что навык еще не изучен
            if (!currentSkill.isLearned()) {
                if (holdProgress < HOLD_TIME) {
                    holdProgress++;

                    // Звук прогресса
                    if (Minecraft.getInstance().player != null && holdProgress % 2 == 0) {
                        float pitch = 0.5f + (holdProgress / (float)HOLD_TIME) * 0.8f;
                        Minecraft.getInstance().player.playNotifySound(
                                SoundRegistry.UI_TICK.get(),
                                SoundSource.MASTER,
                                0.5f,
                                pitch
                        );
                        Minecraft.getInstance().player.playNotifySound(
                                SoundEvents.SOUL_ESCAPE.value(),
                                SoundSource.MASTER,
                                0.5f,
                                pitch
                        );
                    }

                    // ✅ КОГДА ДОСТИГЛИ - СРАЗУ ИЗУЧАЕМ
                    if (holdProgress >= HOLD_TIME) {
                        learnSkill(selectedSkillIndex);
                        // ✅ Сбрасываем индекс, чтобы не продолжалось
                        selectedSkillIndex = -1;
                        holdProgress = -1;
                    }
                }
            } else {
                // Если навык вдруг уже изучен - сбрасываем
                selectedSkillIndex = -1;
                holdProgress = -1;
            }
        }
    }

    private void learnSkill(int index) {
        if (index < 0 || index >= skills.size()) return;

        SkillData skill = skills.get(index);

        // ✅ Защита от повторного изучения
        if (skill.isLearned()) return;

        skill.setLearned(true);

        // Звук изучения
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playNotifySound(
                    SoundRegistry.LEARN_ELDRITCH_SPELL.get(),
                    SoundSource.MASTER,
                    1.0f,
                    1.0f
            );
        }
    }

    private boolean isHoveringNode(SkillNode node, int mouseX, int mouseY) {
        int x = node.x;
        int y = node.y;
        return mouseX >= x && mouseY >= y && mouseX < x + 16 && mouseY < y + 16;
    }

    private boolean isHoveringWindow(int mouseX, int mouseY) {
        return mouseX >= leftPos && mouseY >= topPos &&
                mouseX < leftPos + WINDOW_WIDTH && mouseY < topPos + WINDOW_HEIGHT;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // === ВНУТРЕННИЕ КЛАССЫ ===

    static class SkillData {
        private final String id;
        private final String name;
        private final String description;
        private boolean learned;

        public SkillData(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.learned = false;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public boolean isLearned() { return learned; }
        public void setLearned(boolean learned) { this.learned = learned; }
    }

    static class SkillNode {
        SkillData skill;
        int x, y;

        SkillNode(SkillData skill, int x, int y) {
            this.skill = skill;
            this.x = x;
            this.y = y;
        }
    }
}
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;
import ru.pb.ahst.AHSkillTree;
import ru.pb.ahst.config.SkillConfig;
import ru.pb.ahst.data.PlayerSkillData;
import ru.pb.ahst.data.SkillDataAttachments;
import ru.pb.ahst.registry.ItemRegistry;
import ru.pb.ahst.util.network.LearnSkillPacket;
import ru.pb.ahst.util.network.RequestSkillsSyncPacket;

import java.util.ArrayList;
import java.util.List;

public class SkillTreeScreen extends Screen {

    private static final ResourceLocation WINDOW_LOCATION = ResourceLocation.fromNamespaceAndPath(
            "irons_spellbooks", "textures/gui/eldritch_research_screen/window.png");
    private static final ResourceLocation FRAME_LOCATION = ResourceLocation.fromNamespaceAndPath(
            "irons_spellbooks", "textures/gui/eldritch_research_screen/spell_frame.png");
    private static final ResourceLocation BACKGROUND_LAYER_1 = ResourceLocation.fromNamespaceAndPath(
            AHSkillTree.MOD_ID, "textures/gui/background/background.png");
    private static final ResourceLocation BACKGROUND_LAYER_2 = ResourceLocation.fromNamespaceAndPath(
            AHSkillTree.MOD_ID, "textures/gui/background/background_stars.png");
    private static final ResourceLocation BACKGROUND_LAYER_3 = ResourceLocation.fromNamespaceAndPath(
            AHSkillTree.MOD_ID, "textures/gui/background/background_taiga1.png");
    private static final ResourceLocation BACKGROUND_LAYER_4 = ResourceLocation.fromNamespaceAndPath(
            AHSkillTree.MOD_ID, "textures/gui/background/background_lake.png");
    private static final ResourceLocation BACKGROUND_LAYER_5 = ResourceLocation.fromNamespaceAndPath(
            AHSkillTree.MOD_ID, "textures/gui/background/background_taiga2.png");
    private static final ResourceLocation BACKGROUND_LAYER_6 = ResourceLocation.fromNamespaceAndPath(
            AHSkillTree.MOD_ID, "textures/gui/background/background_taiga3.png");
    private static final ResourceLocation BACKGROUND_LAYER_7 = ResourceLocation.fromNamespaceAndPath(
            AHSkillTree.MOD_ID, "textures/gui/background/background_taiga4.png");

    private float maxViewportOffset = 0;

    private static final ItemStack REQUIRED_ITEM = new ItemStack(ItemRegistry.SKILL_MANUSCRIPT.get());
    private static final int REQUIRED_ITEM_COUNT = 1;

    private static final int FADE_ZONE = 75;
    private static final int HOLD_TIME = 15;
    private static final int NODE_SIZE = 16;
    private static final int FRAME_SIZE = 32;

    private int windowWidth;
    private int windowHeight;
    private int leftPos;
    private int topPos;

    private List<SkillNode> nodes;
    private PlayerSkillData skillData;
    private Vec2 viewportOffset = Vec2.ZERO;
    private boolean isDragging;

    private int selectedSkillIndex = -1;
    private int holdProgress = -1;

    public SkillTreeScreen(Component pTitle) {
        super(pTitle);
    }

    @Override
    protected void init() {
        super.init();

        windowWidth = (int) (this.width * 0.9);
        windowHeight = (int) (this.height * 0.9);
        leftPos = (this.width - windowWidth) / 2;
        topPos = (this.height - windowHeight) / 2;

        calculateMaxViewportOffset();

        if (Minecraft.getInstance().player != null) {
            skillData = Minecraft.getInstance().player.getData(SkillDataAttachments.PLAYER_SKILL_DATA);
            if (skillData.getPlayer() == null) {
                skillData.setPlayer(Minecraft.getInstance().player);
            }
            PacketDistributor.sendToServer(new RequestSkillsSyncPacket());
        }

        nodes = new ArrayList<>();
        for (SkillData skill : SkillConfig.getAllSkills()) {
            nodes.add(new SkillNode(skill, skill.getX(), skill.getY()));
        }

        viewportOffset = Vec2.ZERO;
        selectedSkillIndex = -1;
        holdProgress = -1;
    }

    private void calculateMaxViewportOffset() {
        int texWidth = 1024;
        int texHeight = 1024;

        float menuW = width * 0.9f;
        float menuH = height * 0.9f;
        float scale = Math.max(menuW / texWidth, menuH / texHeight) * 1.4f;
        float scaledW = texWidth * scale;
        float maxTextureOffset = (scaledW - menuW) / 2;

        float maxOffset = Math.min(maxTextureOffset / 0.3f, 750);

        maxViewportOffset = maxOffset;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        drawBackdrop(guiGraphics);

        renderConnections(guiGraphics, partialTick);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int i = 0; i < nodes.size(); i++) {
            SkillNode node = nodes.get(i);
            boolean isSelected = i == selectedSkillIndex;
            boolean isLearned = skillData.isLearned(node.skill.getId());
            boolean canLearn = skillData.canLearn(node.skill.getId());
            float holdProgressValue = (isSelected && holdProgress >= 0) ? holdProgress / (float) HOLD_TIME : 0;

            drawNode(guiGraphics, node, isSelected, canLearn, isLearned, holdProgressValue);
        }

        drawNineSliceWindow(guiGraphics, leftPos, topPos, windowWidth, windowHeight);

        for (SkillNode node : nodes) {
            if (isHoveringNode(node, mouseX, mouseY)) {
                renderTooltip(guiGraphics, node.skill, mouseX, mouseY);
                break;
            }
        }

        RenderSystem.disableBlend();
    }

    private void drawBackdrop(GuiGraphics guiGraphics) {
        if (Minecraft.getInstance().player == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.enableScissor(
                (int) (width * 0.11f),
                (int) (height * 0.11f),
                (int) (width * 1.775f),
                (int) (height * 1.75f)
        );

        float offsetX = viewportOffset.x * 0.5f;
        float offsetY = viewportOffset.y * 0.5f;

        drawParallaxFinal(guiGraphics, BACKGROUND_LAYER_1, offsetX * 0.05f, offsetY * 0.05f);
        drawParallaxFinal(guiGraphics, BACKGROUND_LAYER_2, offsetX * 0.1f, offsetY * 0.1f);
        drawParallaxFinal(guiGraphics, BACKGROUND_LAYER_3, offsetX * 0.2f, offsetY * 0.2f);
        drawParallaxFinal(guiGraphics, BACKGROUND_LAYER_4, offsetX * 0.225f, offsetY * 0.225f);
        drawParallaxFinal(guiGraphics, BACKGROUND_LAYER_5, offsetX * 0.25f, offsetY * 0.25f);
        drawParallaxFinal(guiGraphics, BACKGROUND_LAYER_6, offsetX * 0.275f, offsetY * 0.275f);
        drawParallaxFinal(guiGraphics, BACKGROUND_LAYER_7, offsetX * 0.3f, offsetY * 0.3f);

        RenderSystem.disableScissor();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private void drawParallaxFinal(GuiGraphics guiGraphics, ResourceLocation texture,
                                   float offsetX, float offsetY) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int texWidth = 1024;
        int texHeight = 1024;

        float menuX = width * 0.05f;
        float menuY = height * 0.05f;
        float menuW = width * 0.9f;
        float menuH = height * 0.9f;

        float scale = Math.max(menuW / texWidth, menuH / texHeight) * 1.4f;

        float scaledW = texWidth * scale;
        float scaledH = texHeight * scale;

        float baseX = menuX + (menuW - scaledW) / 2;
        float baseY = menuY + (menuH - scaledH) / 2;

        float posX = baseX + offsetX;
        float posY = baseY + offsetY;

        float maxX = (scaledW - menuW) / 2;
        float maxY = (scaledH - menuH) / 2;

        posX = Mth.clamp(posX, baseX - maxX, baseX + maxX);
        posY = Mth.clamp(posY, baseY - maxY, baseY + maxY);

        guiGraphics.blit(texture,
                (int) posX, (int) posY,
                0, 0,
                (int) scaledW, (int) scaledH,
                (int) scaledW, (int) scaledH);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawNineSliceWindow(GuiGraphics guiGraphics,
                                     int x, int y, int width, int height) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, SkillTreeScreen.WINDOW_LOCATION);

        int textureSize = 256;
        int cornerSize = 23;
        int edgeLength = 210;
        int edgeSize = 23;

        guiGraphics.blit(SkillTreeScreen.WINDOW_LOCATION, x, y, 0, 0, cornerSize, cornerSize, textureSize, textureSize);

        guiGraphics.blit(SkillTreeScreen.WINDOW_LOCATION, x + width - cornerSize, y, 233, 0, cornerSize, cornerSize, textureSize, textureSize);

        guiGraphics.blit(SkillTreeScreen.WINDOW_LOCATION, x, y + height - cornerSize, 0, 233, cornerSize, cornerSize, textureSize, textureSize);

        guiGraphics.blit(SkillTreeScreen.WINDOW_LOCATION, x + width - cornerSize, y + height - cornerSize, 233, 233, cornerSize, cornerSize, textureSize, textureSize);

        int topWidth = width - cornerSize * 2;
        if (topWidth > 0) {
            for (int offset = 0; offset < topWidth; offset += edgeLength) {
                int segmentWidth = Math.min(edgeLength, topWidth - offset);

                int uvX = 23;
                guiGraphics.blit(SkillTreeScreen.WINDOW_LOCATION,
                        x + cornerSize + offset, y,
                        uvX, 0, segmentWidth, edgeSize, textureSize, textureSize);
            }
        }

        if (topWidth > 0) {
            for (int offset = 0; offset < topWidth; offset += edgeLength) {
                int segmentWidth = Math.min(edgeLength, topWidth - offset);
                guiGraphics.blit(SkillTreeScreen.WINDOW_LOCATION,
                        x + cornerSize + offset, y + height - edgeSize,
                        23, 233, segmentWidth, edgeSize, textureSize, textureSize);
            }
        }

        int leftHeight = height - cornerSize * 2;
        if (leftHeight > 0) {
            for (int offset = 0; offset < leftHeight; offset += edgeLength) {
                int segmentHeight = Math.min(edgeLength, leftHeight - offset);
                guiGraphics.blit(SkillTreeScreen.WINDOW_LOCATION,
                        x, y + cornerSize + offset,
                        0, 23, edgeSize, segmentHeight, textureSize, textureSize);
            }
        }

        if (leftHeight > 0) {
            for (int offset = 0; offset < leftHeight; offset += edgeLength) {
                int segmentHeight = Math.min(edgeLength, leftHeight - offset);
                guiGraphics.blit(SkillTreeScreen.WINDOW_LOCATION,
                        x + width - edgeSize, y + cornerSize + offset,
                        233, 23, edgeSize, segmentHeight, textureSize, textureSize);
            }
        }

        RenderSystem.disableBlend();
    }

    private void renderConnections(GuiGraphics guiGraphics, float partialTick) {
        if (nodes.isEmpty()) return;

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        assert Minecraft.getInstance().player != null;
        float pulse = Mth.sin(((float) Minecraft.getInstance().player.tickCount + partialTick) * 0.1F);
        float glowIntensity = pulse * pulse * 0.8F + 0.2F;

        Vector4f normalColor = new Vector4f(0.5294118F, 0.6039216F, 0.68235296F, 0.5F);

        Vector4f glowColor = new Vector4f(0.95686275F, 0.25490198F, 1.0F, 0.7F);

        for (SkillNode node : nodes) {
            for (String prereqId : node.skill.getPrerequisites()) {
                SkillNode prereqNode = findNodeById(prereqId);
                if (prereqNode != null) {
                    int[] from = getNodeCenter(prereqNode);
                    int[] to = getNodeCenter(node);

                    boolean isNodeLearned = skillData.isLearned(node.skill.getId());
                    boolean isPrereqLearned = skillData.isLearned(prereqNode.skill.getId());

                    int[] clipped = clipLineToWindow(from[0], from[1], to[0], to[1]);
                    if (clipped != null) {
                        drawConnectionWithGradient(guiGraphics,
                                clipped[0], clipped[1],
                                clipped[2], clipped[3],
                                glowIntensity, normalColor, glowColor,
                                isPrereqLearned, isNodeLearned);
                    }
                }
            }
        }

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
    }

    private int[] getNodeCenter(SkillNode node) {
        int centerX = leftPos + windowWidth / 2 + (int) viewportOffset.x + node.relX + NODE_SIZE / 2;
        int centerY = topPos + windowHeight / 2 + (int) viewportOffset.y + node.relY + NODE_SIZE / 2;
        return new int[]{centerX, centerY};
    }

    private void drawConnectionWithGradient(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2,
                                            float glowIntensity, Vector4f normalColor, Vector4f glowColor,
                                            boolean isStartLearned, boolean isEndLearned) {
        Vec2 direction = new Vec2(x2 - x1, y2 - y1);
        float length = (float) Math.sqrt(direction.x * direction.x + direction.y * direction.y);
        if (length < 1) return;

        Vec2 perpendicular = new Vec2(-direction.y, direction.x).normalized().scale(1.5F);

        float alpha1 = getPointAlpha(x1, y1);
        float alpha2 = getPointAlpha(x2, y2);

        if (alpha1 <= 0.01f && alpha2 <= 0.01f) return;

        Vector4f startColor;
        Vector4f endColor;

        if (isStartLearned && isEndLearned) {
            startColor = lerpColor(normalColor, glowColor, glowIntensity);
            endColor = lerpColor(normalColor, glowColor, glowIntensity);
        } else if (isStartLearned) {
            startColor = lerpColor(normalColor, glowColor, glowIntensity);
            endColor = normalColor;
        } else if (isEndLearned) {
            startColor = normalColor;
            endColor = lerpColor(normalColor, glowColor, glowIntensity);
        } else {
            startColor = normalColor;
            endColor = normalColor;
        }

        Vector4f finalColor1 = new Vector4f(startColor.x(), startColor.y(), startColor.z(), startColor.w() * alpha1);
        Vector4f finalColor2 = new Vector4f(endColor.x(), endColor.y(), endColor.z(), endColor.w() * alpha2);

        RenderHelper.quadBuilder()
                .vertex(x1 + perpendicular.x, y1 + perpendicular.y).color(colorToInt(finalColor1))
                .vertex(x2 + perpendicular.x, y2 + perpendicular.y).color(colorToInt(finalColor2))
                .vertex(x2 - perpendicular.x, y2 - perpendicular.y).color(colorToInt(finalColor2))
                .vertex(x1 - perpendicular.x, y1 - perpendicular.y).color(colorToInt(finalColor1))
                .build(guiGraphics, RenderType.gui());
    }

    private void drawNode(GuiGraphics guiGraphics, SkillNode node, boolean isSelected,
                          boolean canLearn, boolean isLearned, float progress) {
        int screenX = leftPos + windowWidth / 2 + (int) viewportOffset.x + node.relX;
        int screenY = topPos + windowHeight / 2 + (int) viewportOffset.y + node.relY;

        if (screenX + NODE_SIZE < leftPos || screenX > leftPos + windowWidth ||
                screenY + NODE_SIZE < topPos || screenY > topPos + windowHeight) {
            return;
        }

        float alpha = getPointAlpha(screenX + NODE_SIZE / 2, screenY + NODE_SIZE / 2);
        if (alpha <= 0.01f) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        boolean isLocked = !canLearn && !isLearned;
        float colorMultiplier = isLocked ? 0.4F : 1.0F;
        float finalAlpha = alpha * (isLocked ? 0.7F : 1.0F);

        guiGraphics.setColor(colorMultiplier, colorMultiplier, colorMultiplier, finalAlpha);

        int frameU = isLearned ? 32 : 0;
        guiGraphics.blit(FRAME_LOCATION, screenX - 8, screenY - 8, frameU, 0, FRAME_SIZE, FRAME_SIZE, 64, 32);

        ResourceLocation skillTexture = ResourceLocation.fromNamespaceAndPath(
                AHSkillTree.MOD_ID, "textures/gui/skills/" + node.skill.getId() + ".png");
        RenderSystem.setShaderTexture(0, skillTexture);
        guiGraphics.blit(skillTexture, screenX, screenY, 0, 0, NODE_SIZE, NODE_SIZE, NODE_SIZE, NODE_SIZE);

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (isSelected && progress > 0 && !isLearned && canLearn) {
            guiGraphics.fill(screenX, screenY, screenX + (int) (NODE_SIZE * progress), screenY + NODE_SIZE, 0x88FFFFFF);
        }

        RenderSystem.disableBlend();
    }

    private float getPointAlpha(int x, int y) {
        float relX = x - leftPos;
        float relY = y - topPos;

        float distToRight = windowWidth - relX;
        float distToBottom = windowHeight - relY;

        float minDist = Math.min(Math.min(relX, distToRight), Math.min(relY, distToBottom));

        if (minDist >= FADE_ZONE) {
            return 1.0F;
        } else if (minDist <= 0) {
            return 0.0F;
        } else {
            float alpha = minDist / FADE_ZONE;
            return alpha * alpha * (3 - 2 * alpha);
        }
    }

    private void renderTooltip(GuiGraphics guiGraphics, SkillData skill, int mouseX, int mouseY) {
        List<FormattedCharSequence> tooltip = new ArrayList<>();

        MutableComponent name = Component.literal(skill.getName())
                .withStyle(skillData.isLearned(skill.getId()) ? ChatFormatting.GREEN :
                        skillData.canLearn(skill.getId()) ? ChatFormatting.YELLOW : ChatFormatting.GRAY);
        tooltip.add(name.getVisualOrderText());

        tooltip.addAll(this.font.split(Component.literal(skill.getDescription()), 180));
        tooltip.add(FormattedCharSequence.EMPTY);

        Component itemReq = Component.literal("📦 Требуется: " + REQUIRED_ITEM.getHoverName().getString())
                .withStyle(ChatFormatting.GOLD);
        tooltip.add(itemReq.getVisualOrderText());

        Component status;
        if (skillData.isLearned(skill.getId())) {
            status = Component.literal("✓ Изучено").withStyle(ChatFormatting.GREEN);
        } else if (skillData.canLearn(skill.getId())) {
            status = Component.literal("★ Можно изучить").withStyle(ChatFormatting.YELLOW);
        } else {
            String prereqList = String.join(", ", skill.getPrerequisites());
            status = Component.literal("⛔ Требуется: " + prereqList).withStyle(ChatFormatting.GRAY);
        }
        tooltip.add(status.getVisualOrderText());

        guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < nodes.size(); i++) {
            SkillNode node = nodes.get(i);
            String skillId = node.skill.getId();

            if (isHoveringNode(node, (int) mouseX, (int) mouseY)) {
                if (skillData.isLearned(skillId)) {
                    return true;
                }

                if (!skillData.canLearn(skillId)) {
                    playSound(SoundEvents.VILLAGER_NO, 0.5f, 2.0f);
                    return true;
                }

                if (skillData.getSkillPoints() < 1) {
                    playSound(SoundEvents.VILLAGER_NO, 1.0f, 0.8f);
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.displayClientMessage(
                                Component.literal("§cНедостаточно очков навыков!").withStyle(ChatFormatting.RED), true);
                    }
                    return true;
                }

                if (!hasRequiredItem()) {
                    playSound(SoundEvents.VILLAGER_NO, 0.5f, 1.5f);
                    if (Minecraft.getInstance().player != null) {
                        String itemName = REQUIRED_ITEM.getHoverName().getString();
                        Minecraft.getInstance().player.displayClientMessage(
                                Component.literal("§cТребуется: " + itemName).withStyle(ChatFormatting.RED), true);
                    }
                    return true;
                }

                selectedSkillIndex = i;
                holdProgress = 0;
                return true;
            }
        }

        if (isHoveringWindow((int) mouseX, (int) mouseY)) {
            isDragging = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (isDragging && button == 0) {
            float newX = viewportOffset.x + (float) dx;
            float newY = viewportOffset.y + (float) dy;

            viewportOffset = new Vec2(
                    Mth.clamp(newX, -maxViewportOffset, maxViewportOffset),
                    Mth.clamp(newY, -maxViewportOffset, maxViewportOffset)
            );
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (selectedSkillIndex != -1 && holdProgress < HOLD_TIME) {
            selectedSkillIndex = -1;
            holdProgress = -1;
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
                selectedSkillIndex = -1;
                holdProgress = -1;
                return;
            }

            holdProgress++;

            if (holdProgress % 3 == 0) {
                float pitch = 0.6f + (holdProgress / (float) HOLD_TIME) * 0.9f;
                playSound(SoundRegistry.UI_TICK.get(), 0.4f, pitch);
            }

            if (holdProgress >= HOLD_TIME) {
                if (skillData.getSkillPoints() >= 1 && hasRequiredItem()) {
                    PacketDistributor.sendToServer(new LearnSkillPacket(skillId, 1));
                    skillData.learnSkill(skillId, 1);
                    playSound(SoundRegistry.LEARN_ELDRITCH_SPELL.get(), 1.0f, 1.0f);
                } else if (!hasRequiredItem()) {
                    // Если предмета нет - показываем сообщение
                    if (Minecraft.getInstance().player != null) {
                        String itemName = REQUIRED_ITEM.getHoverName().getString();
                        Minecraft.getInstance().player.displayClientMessage(
                                Component.literal("§cНет " + itemName + " для изучения!").withStyle(ChatFormatting.RED), true);
                    }
                }
                selectedSkillIndex = -1;
                holdProgress = -1;
            }
        }
    }

    private void playSound(SoundEvent sound, float volume, float pitch) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playNotifySound(sound, SoundSource.MASTER, volume, pitch);
        }
    }

    private SkillNode findNodeById(String id) {
        return nodes.stream().filter(n -> n.skill.getId().equals(id)).findFirst().orElse(null);
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

    private int colorToInt(Vector4f color) {
        return ((int) (color.w() * 255) << 24) |
                ((int) (color.x() * 255) << 16) |
                ((int) (color.y() * 255) << 8) |
                ((int) (color.z() * 255));
    }

    private int[] clipLineToWindow(int x1, int y1, int x2, int y2) {
        int left = leftPos;
        int right = leftPos + windowWidth;
        int top = topPos;
        int bottom = topPos + windowHeight;

        int code1 = computeOutCode(x1, y1, left, right, top, bottom);
        int code2 = computeOutCode(x2, y2, left, right, top, bottom);

        if ((code1 & code2) != 0) {
            return null;
        }

        int cx1 = x1, cy1 = y1;
        int cx2 = x2, cy2 = y2;

        if (code1 != 0) {
            int[] clipped = clipPoint(cx1, cy1, cx2, cy2, code1, left, right, top, bottom);
            cx1 = clipped[0];
            cy1 = clipped[1];
        }

        if (code2 != 0) {
            int[] clipped = clipPoint(cx2, cy2, cx1, cy1, code2, left, right, top, bottom);
            cx2 = clipped[0];
            cy2 = clipped[1];
        }

        return new int[]{cx1, cy1, cx2, cy2};
    }

    private int computeOutCode(int x, int y, int left, int right, int top, int bottom) {
        int code = 0;
        if (x < left) code |= 1;      // LEFT
        if (x > right) code |= 2;     // RIGHT
        if (y < top) code |= 4;       // TOP
        if (y > bottom) code |= 8;    // BOTTOM
        return code;
    }

    private int[] clipPoint(int x, int y, int otherX, int otherY, int code,
                            int left, int right, int top, int bottom) {
        double dx = otherX - x;
        double dy = otherY - y;

        if ((code & 1) != 0) { // LEFT
            double t = (left - x) / dx;
            x = left;
            y = (int) (y + dy * t);
        }
        if ((code & 2) != 0) { // RIGHT
            double t = (right - x) / dx;
            x = right;
            y = (int) (y + dy * t);
        }
        if ((code & 4) != 0) { // TOP
            double t = (top - y) / dy;
            y = top;
            x = (int) (x + dx * t);
        }
        if ((code & 8) != 0) { // BOTTOM
            double t = (bottom - y) / dy;
            y = bottom;
            x = (int) (x + dx * t);
        }

        return new int[]{x, y};
    }

    private boolean isHoveringNode(SkillNode node, int mouseX, int mouseY) {
        int screenX = leftPos + windowWidth / 2 + (int) viewportOffset.x + node.relX;
        int screenY = topPos + windowHeight / 2 + (int) viewportOffset.y + node.relY;
        return mouseX >= screenX && mouseY >= screenY &&
                mouseX < screenX + NODE_SIZE && mouseY < screenY + NODE_SIZE;
    }

    private boolean hasRequiredItem() {
        if (Minecraft.getInstance().player == null) return false;

        int count = 0;
        for (ItemStack stack : Minecraft.getInstance().player.getInventory().items) {
            if (ItemStack.isSameItemSameComponents(stack, REQUIRED_ITEM)) {
                count += stack.getCount();
            }
        }

        return count >= REQUIRED_ITEM_COUNT;
    }

    private boolean isHoveringWindow(int mouseX, int mouseY) {
        return mouseX >= leftPos && mouseY >= topPos &&
                mouseX < leftPos + windowWidth && mouseY < topPos + windowHeight;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        assert this.minecraft != null;
        if (this.minecraft.options.keyInventory.isActiveAndMatches(key) || keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
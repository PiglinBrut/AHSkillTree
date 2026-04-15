package ru.pb.ahst.config;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import ru.pb.ahst.AHSkillTree;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ItemRestrictionsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;

    // Ключ: ResourceLocation предмета → Set<BlockedAction>
    public static final Map<ResourceLocation, Set<BlockedAction>> ITEM_RESTRICTIONS = new HashMap<>();
    public static final Map<TagKey<Item>, Set<BlockedAction>> ITEM_TAG_RESTRICTIONS = new HashMap<>();
    public static final Map<ResourceLocation, Set<BlockedAction>> BLOCK_RESTRICTIONS = new HashMap<>();
    public static final Map<TagKey<Block>, Set<BlockedAction>> BLOCK_TAG_RESTRICTIONS = new HashMap<>();

    private static final Map<ResourceLocation, Set<BlockedAction>> TEMP_ITEM_RESTRICTIONS = new HashMap<>();
    private static final Map<TagKey<Item>, Set<BlockedAction>> TEMP_ITEM_TAG_RESTRICTIONS = new HashMap<>();
    private static final Map<ResourceLocation, Set<BlockedAction>> TEMP_BLOCK_RESTRICTIONS = new HashMap<>();
    private static final Map<TagKey<Block>, Set<BlockedAction>> TEMP_BLOCK_TAG_RESTRICTIONS = new HashMap<>();
    private static final Map<ResourceLocation, Set<BlockedAction>> TEMP_ARMOR_RESTRICTIONS = new HashMap<>();

    private static boolean useTempRestrictions = false;

    public static void clearTempRestrictions() {
        TEMP_ITEM_RESTRICTIONS.clear();
        TEMP_ITEM_TAG_RESTRICTIONS.clear();
        TEMP_BLOCK_RESTRICTIONS.clear();
        TEMP_BLOCK_TAG_RESTRICTIONS.clear();
        useTempRestrictions = false;
    }

    public static void addTempItemRestriction(ResourceLocation itemId, Set<BlockedAction> actions) {
        TEMP_ITEM_RESTRICTIONS.merge(itemId, new HashSet<>(actions), (old, neu) -> {
            old.addAll(neu);
            return old;
        });
        useTempRestrictions = true;
    }

    public static void addTempItemTagRestriction(TagKey<Item> tag, Set<BlockedAction> actions) {
        TEMP_ITEM_TAG_RESTRICTIONS.merge(tag, new HashSet<>(actions), (old, neu) -> {
            old.addAll(neu);
            return old;
        });
        useTempRestrictions = true;
    }

    public static void addTempBlockRestriction(ResourceLocation blockId, Set<BlockedAction> actions) {
        TEMP_BLOCK_RESTRICTIONS.merge(blockId, new HashSet<>(actions), (old, neu) -> {
            old.addAll(neu);
            return old;
        });
        useTempRestrictions = true;
    }

    public static void addTempBlockTagRestriction(TagKey<Block> tag, Set<BlockedAction> actions) {
        TEMP_BLOCK_TAG_RESTRICTIONS.merge(tag, new HashSet<>(actions), (old, neu) -> {
            old.addAll(neu);
            return old;
        });
        useTempRestrictions = true;
    }

    public static void addTempArmorRestriction(ResourceLocation armorId, Set<BlockedAction> actions) {
        TEMP_ARMOR_RESTRICTIONS.merge(armorId, new HashSet<>(actions), (old, neu) -> {
            old.addAll(neu);
            return old;
        });
        useTempRestrictions = true;
    }

    public static Map<ResourceLocation, Set<BlockedAction>> getTempItemRestrictions() {
        return Collections.unmodifiableMap(TEMP_ITEM_RESTRICTIONS);
    }

    public static Map<TagKey<Item>, Set<BlockedAction>> getTempItemTagRestrictions() {
        return Collections.unmodifiableMap(TEMP_ITEM_TAG_RESTRICTIONS);
    }

    public static Map<ResourceLocation, Set<BlockedAction>> getTempBlockRestrictions() {
        return Collections.unmodifiableMap(TEMP_BLOCK_RESTRICTIONS);
    }

    public static Map<TagKey<Block>, Set<BlockedAction>> getTempBlockTagRestrictions() {
        return Collections.unmodifiableMap(TEMP_BLOCK_TAG_RESTRICTIONS);
    }

    public static void init(Path configDir) {
        configPath = configDir.resolve(AHSkillTree.MOD_ID).resolve("AHItemRestrictions.json");
        loadConfig();
    }

    private static void loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                createDefaultConfig();
            }

            String json = Files.readString(configPath);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            ITEM_RESTRICTIONS.clear();
            ITEM_TAG_RESTRICTIONS.clear();
            BLOCK_RESTRICTIONS.clear();
            BLOCK_TAG_RESTRICTIONS.clear();

            if (root.has("restrictions")) {
                JsonArray restrictions = root.getAsJsonArray("restrictions");

                for (JsonElement elem : restrictions) {
                    JsonObject obj = elem.getAsJsonObject();
                    Set<BlockedAction> actions = parseActions(obj.getAsJsonArray("blocked_actions"));

                    // Предметы
                    if (obj.has("items")) {
                        for (JsonElement itemElem : obj.getAsJsonArray("items")) {
                            ResourceLocation id = ResourceLocation.parse(itemElem.getAsString());
                            ITEM_RESTRICTIONS.put(id, new HashSet<>(actions));
                        }
                    }

                    // Теги предметов
                    if (obj.has("item_tags")) {
                        for (JsonElement tagElem : obj.getAsJsonArray("item_tags")) {
                            String tagStr = tagElem.getAsString();
                            if (tagStr.startsWith("#")) {
                                tagStr = tagStr.substring(1);
                            }
                            TagKey<Item> tag = ItemTags.create(ResourceLocation.parse(tagStr));
                            ITEM_TAG_RESTRICTIONS.put(tag, new HashSet<>(actions));
                        }
                    }

                    // Блоки
                    if (obj.has("blocks")) {
                        for (JsonElement itemElem : obj.getAsJsonArray("blocks")) {
                            ResourceLocation id = ResourceLocation.parse(itemElem.getAsString());
                            BLOCK_RESTRICTIONS.put(id, new HashSet<>(actions));
                        }
                    }

                    // Теги блоков
                    if (obj.has("block_tags")) {
                        for (JsonElement tagElem : obj.getAsJsonArray("block_tags")) {
                            String tagStr = tagElem.getAsString();
                            if (tagStr.startsWith("#")) {
                                tagStr = tagStr.substring(1);
                            }
                            TagKey<Block> tag = BlockTags.create(ResourceLocation.parse(tagStr));
                            BLOCK_TAG_RESTRICTIONS.put(tag, new HashSet<>(actions));
                        }
                    }
                }
            }
        } catch (Exception e) {
            AHSkillTree.LOGGER.error("Failed to load item restrictions config", e);
        }
    }

    private static Set<BlockedAction> parseActions(JsonArray array) {
        Set<BlockedAction> actions = new HashSet<>();
        if (array == null) return actions;
        for (JsonElement e : array) {
            try {
                actions.add(BlockedAction.valueOf(e.getAsString().toUpperCase()));
            } catch (Exception ignored) {}
        }
        return actions;
    }

    private static void createDefaultConfig() {
        try {
            Files.createDirectories(configPath.getParent());

            JsonObject root = new JsonObject();
            JsonArray restrictions = new JsonArray();

            // Пример по умолчанию
            JsonObject swords = new JsonObject();
            JsonArray swordItems = new JsonArray();
            swordItems.add("minecraft:iron_sword");
            swordItems.add("minecraft:diamond_sword");
            swords.add("items", swordItems);
            JsonArray swordActions = new JsonArray();
            swordActions.add("attack_entity");
            swordActions.add("right_click");
            swords.add("blocked_actions", swordActions);
            restrictions.add(swords);

            JsonObject axes = new JsonObject();
            JsonArray axeItems = new JsonArray();
            axeItems.add("minecraft:iron_axe");
            axeItems.add("minecraft:diamond_axe");
            axes.add("items", axeItems);
            JsonArray axeActions = new JsonArray();
            axeActions.add("attack_entity");
            axes.add("blocked_actions", axeActions);
            restrictions.add(axes);

            root.add("restrictions", restrictions);
            Files.writeString(configPath, GSON.toJson(root));

        } catch (IOException e) {
            AHSkillTree.LOGGER.error("Failed to create default restrictions config", e);
        }
    }

    public static boolean isActionBlocked(ItemStack stack, BlockState block, BlockedAction action) {
        // Проверка предмета
        if (stack != null && !stack.isEmpty()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

            if (useTempRestrictions) {
                if (TEMP_ITEM_RESTRICTIONS.getOrDefault(itemId, Collections.emptySet()).contains(action)) {
                    return true;
                }

                for (var entry : TEMP_ITEM_TAG_RESTRICTIONS.entrySet()) {
                    if (stack.is(entry.getKey()) && entry.getValue().contains(action)) {
                        return true;
                    }
                }
            }

            // Проверка конкретного предмета
            if (ITEM_RESTRICTIONS.getOrDefault(itemId, Collections.emptySet()).contains(action)) {
                return true;
            }

            // Проверка тегов предмета
            for (var entry : ITEM_TAG_RESTRICTIONS.entrySet()) {
                if (stack.is(entry.getKey()) && entry.getValue().contains(action)) {
                    return true;
                }
            }
        }

        // Проверка блока
        if (block != null && !block.isEmpty()) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block.getBlock());

            if (useTempRestrictions) {
                if (TEMP_BLOCK_RESTRICTIONS.getOrDefault(blockId, Collections.emptySet()).contains(action)) {
                    return true;
                }

                for (var entry : TEMP_BLOCK_TAG_RESTRICTIONS.entrySet()) {
                    if (block.is(entry.getKey()) && entry.getValue().contains(action)) {
                        return true;
                    }
                }
            }

            // Проверка конкретного блока
            if (BLOCK_RESTRICTIONS.getOrDefault(blockId, Collections.emptySet()).contains(action)) {
                return true;
            }

            // Проверка тегов блока
            for (var entry : BLOCK_TAG_RESTRICTIONS.entrySet()) {
                if (block.is(entry.getKey()) && entry.getValue().contains(action)) {
                    return true;
                }
            }
        }

        return false;
    }
}
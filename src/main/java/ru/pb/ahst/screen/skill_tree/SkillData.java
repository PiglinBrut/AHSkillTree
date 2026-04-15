// SkillData.java
package ru.pb.ahst.screen.skill_tree;

import java.util.*;

public class SkillData {
    private final String id;
    private final String name;
    private final String description;
    private final int x;
    private final int y;
    private final Set<String> prerequisites;
    private final Set<String> conflicts;

    public SkillData(String id, String name, String description, int x, int y,
                     List<String> prerequisites, List<String> conflicts) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.x = x;
        this.y = y;
        this.prerequisites = new HashSet<>(prerequisites);
        this.conflicts = new HashSet<>(conflicts);
    }

    // Геттеры
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getX() { return x; }
    public int getY() { return y; }
    public Set<String> getPrerequisites() { return prerequisites; }
    public Set<String> getConflicts() { return conflicts; }
}
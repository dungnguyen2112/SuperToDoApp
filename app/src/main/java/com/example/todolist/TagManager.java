package com.example.todolist;

import android.content.Context;
import java.util.List;
import java.util.Random;

public class TagManager {
    private DatabaseHelper dbHelper;
    private static TagManager instance;

    // Predefined colors for tags
    private static final String[] TAG_COLORS = {
            "#2196F3", // Blue
            "#4CAF50", // Green
            "#FF9800", // Orange
            "#9C27B0", // Purple
            "#F44336", // Red
            "#607D8B", // Blue Grey
            "#795548", // Brown
            "#009688", // Teal
            "#FF5722", // Deep Orange
            "#3F51B5", // Indigo
            "#E91E63", // Pink
            "#CDDC39", // Lime
            "#FFC107", // Amber
            "#673AB7", // Deep Purple
            "#00BCD4"  // Cyan
    };

    private TagManager(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    public static synchronized TagManager getInstance(Context context) {
        if (instance == null) {
            instance = new TagManager(context.getApplicationContext());
        }
        return instance;
    }

    // Get all tags
    public List<Tag> getAllTags() {
        return dbHelper.getAllTags();
    }

    // Get tag by ID
    public Tag getTagById(int tagId) {
        return dbHelper.getTagById(tagId);
    }

    // Get tag by name
    public Tag getTagByName(String tagName) {
        return dbHelper.getTagByName(tagName);
    }

    // Create a new tag
    public Tag createTag(String tagName) {
        return createTag(tagName, getRandomColor());
    }

    public Tag createTag(String tagName, String color) {
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be empty");
        }

        String trimmedName = tagName.trim();
        
        // Check if tag already exists
        if (dbHelper.isTagNameExists(trimmedName)) {
            return dbHelper.getTagByName(trimmedName);
        }

        // Create new tag
        Tag newTag = new Tag(trimmedName, color);
        long id = dbHelper.addTag(newTag);
        newTag.setId((int) id);
        
        return newTag;
    }

    // Update tag
    public boolean updateTag(Tag tag) {
        if (tag == null || tag.getName() == null || tag.getName().trim().isEmpty()) {
            return false;
        }

        // Check if another tag with same name exists (excluding current tag)
        Tag existingTag = dbHelper.getTagByName(tag.getName().trim());
        if (existingTag != null && existingTag.getId() != tag.getId()) {
            return false; // Name already exists
        }

        return dbHelper.updateTag(tag) > 0;
    }

    // Delete tag
    public boolean deleteTag(int tagId) {
        return dbHelper.deleteTag(tagId) > 0;
    }

    // Check if tag name is valid and available
    public boolean isTagNameValid(String tagName) {
        return tagName != null && !tagName.trim().isEmpty();
    }

    public boolean isTagNameAvailable(String tagName) {
        return !dbHelper.isTagNameExists(tagName.trim());
    }

    public boolean isTagNameAvailable(String tagName, int excludeTagId) {
        Tag existingTag = dbHelper.getTagByName(tagName.trim());
        return existingTag == null || existingTag.getId() == excludeTagId;
    }

    // Get tags for specific task
    public List<Tag> getTagsForTask(int taskId) {
        return dbHelper.getTagsForTask(taskId);
    }

    // Add tag to task
    public void addTagToTask(int taskId, int tagId) {
        dbHelper.addTaskTag(taskId, tagId);
    }

    // Remove tag from task
    public void removeTagFromTask(int taskId, int tagId) {
        dbHelper.removeTaskTag(taskId, tagId);
    }

    // Update all tags for a task
    public void updateTaskTags(int taskId, List<Tag> tags) {
        dbHelper.updateTaskTags(taskId, tags);
    }

    // Get random color for new tag
    public String getRandomColor() {
        Random random = new Random();
        return TAG_COLORS[random.nextInt(TAG_COLORS.length)];
    }

    // Get all available colors
    public String[] getAllColors() {
        return TAG_COLORS.clone();
    }

    // Validate color format
    public boolean isValidColor(String color) {
        if (color == null) return false;
        return color.matches("^#[0-9A-Fa-f]{6}$");
    }

    // Get or create tag by name (useful for quick tag creation)
    public Tag getOrCreateTag(String tagName) {
        if (!isTagNameValid(tagName)) {
            return null;
        }

        String trimmedName = tagName.trim();
        Tag existingTag = getTagByName(trimmedName);
        
        if (existingTag != null) {
            return existingTag;
        }

        return createTag(trimmedName);
    }

    // Search tags by name
    public List<Tag> searchTagsByName(String query) {
        List<Tag> allTags = getAllTags();
        if (query == null || query.trim().isEmpty()) {
            return allTags;
        }

        String lowerQuery = query.toLowerCase().trim();
        List<Tag> filteredTags = new java.util.ArrayList<>();
        
        for (Tag tag : allTags) {
            if (tag.getName().toLowerCase().contains(lowerQuery)) {
                filteredTags.add(tag);
            }
        }
        
        return filteredTags;
    }
} 
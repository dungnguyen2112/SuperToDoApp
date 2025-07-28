package com.example.todolist;

import java.util.ArrayList;
import java.util.List;

public class Task {
    private int id;
    private String title;
    private String description;
    private String topic;
    private boolean isCompleted;
    private String createdDate;
    private String deadline;
    private boolean reminderEnabled;
    private String reminderTime; // Format: "yyyy-MM-dd HH:mm"
    private String completedDate; // Date when task was completed
    private List<Tag> tags; // List of tags associated with this task

    public Task() {
        this.tags = new ArrayList<>();
    }

    public Task(String title, String description, String topic) {
        this.title = title;
        this.description = description;
        this.topic = topic;
        this.isCompleted = false;
        this.reminderEnabled = false;
        this.createdDate = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
        this.tags = new ArrayList<>();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public String getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }

    public String getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(String completedDate) {
        this.completedDate = completedDate;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public void addTag(Tag tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    public void removeTag(Tag tag) {
        if (this.tags != null) {
            this.tags.remove(tag);
        }
    }

    public boolean hasTag(Tag tag) {
        return this.tags != null && this.tags.contains(tag);
    }
}

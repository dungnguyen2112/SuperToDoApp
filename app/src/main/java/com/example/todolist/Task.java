package com.example.todolist;

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

    public Task() {}

    public Task(String title, String description, String topic) {
        this.title = title;
        this.description = description;
        this.topic = topic;
        this.isCompleted = false;
        this.reminderEnabled = false;
        this.createdDate = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
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
}

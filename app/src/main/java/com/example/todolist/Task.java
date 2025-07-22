package com.example.todolist;

public class Task {
    private int id;
    private String title;
    private String description;
    private String topic;
    private boolean isCompleted;
    private String createdDate;

    public Task() {}

    public Task(String title, String description, String topic) {
        this.title = title;
        this.description = description;
        this.topic = topic;
        this.isCompleted = false;
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
}

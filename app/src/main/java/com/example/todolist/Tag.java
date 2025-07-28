package com.example.todolist;

public class Tag {
    private int id;
    private String name;
    private String color; // Hex color code (e.g., "#FF5722")

    public Tag() {}

    public Tag(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public Tag(int id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tag tag = (Tag) obj;
        return id == tag.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return name;
    }
} 
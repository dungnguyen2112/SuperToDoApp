package com.example.todolist;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "todo.db";
    private static final int DATABASE_VERSION = 3; // Increment version to trigger migration
    private static final String TABLE_TASKS = "tasks";

    // Columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_TOPIC = "topic";
    private static final String COLUMN_IS_COMPLETED = "is_completed";
    private static final String COLUMN_CREATED_DATE = "created_date";
    private static final String COLUMN_DEADLINE = "deadline";
    private static final String COLUMN_REMINDER_ENABLED = "reminder_enabled";
    private static final String COLUMN_REMINDER_TIME = "reminder_time";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @SuppressLint("Range")
    private Task createTaskFromCursor(Cursor cursor) {
        Task task = new Task();

        // Use getColumnIndexOrThrow for better error handling
        try {
            task.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
            task.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
            task.setTopic(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TOPIC)));
            task.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_COMPLETED)) == 1);
            task.setCreatedDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_DATE)));

            // Handle deadline column safely
            int deadlineIndex = cursor.getColumnIndex(COLUMN_DEADLINE);
            if (deadlineIndex != -1) {
                task.setDeadline(cursor.getString(deadlineIndex));
            } else {
                task.setDeadline(null);
            }

            // Handle reminder fields safely for backward compatibility
            int reminderEnabledIndex = cursor.getColumnIndex(COLUMN_REMINDER_ENABLED);
            int reminderTimeIndex = cursor.getColumnIndex(COLUMN_REMINDER_TIME);

            if (reminderEnabledIndex != -1) {
                task.setReminderEnabled(cursor.getInt(reminderEnabledIndex) == 1);
            } else {
                task.setReminderEnabled(false);
            }

            if (reminderTimeIndex != -1) {
                task.setReminderTime(cursor.getString(reminderTimeIndex));
            } else {
                task.setReminderTime(null);
            }

        } catch (IllegalArgumentException e) {
            // Log the error for debugging
            android.util.Log.e("DatabaseHelper", "Error reading cursor data: " + e.getMessage());
            return null;
        }
        
        return task;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_TASKS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT NOT NULL,"
                + COLUMN_DESCRIPTION + " TEXT,"
                + COLUMN_TOPIC + " TEXT,"
                + COLUMN_IS_COMPLETED + " INTEGER DEFAULT 0,"
                + COLUMN_CREATED_DATE + " TEXT,"
                + COLUMN_DEADLINE + " TEXT,"
                + COLUMN_REMINDER_ENABLED + " INTEGER DEFAULT 0,"
                + COLUMN_REMINDER_TIME + " TEXT"
                + ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add reminder columns to existing table
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_REMINDER_ENABLED + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_REMINDER_TIME + " TEXT");
        }
        if (oldVersion < 3) {
            // Add deadline column to existing table
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_DEADLINE + " TEXT");
        }
    }

    // CRUD Operations
    public long addTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, task.getTitle());
        values.put(COLUMN_DESCRIPTION, task.getDescription());
        values.put(COLUMN_TOPIC, task.getTopic());
        values.put(COLUMN_IS_COMPLETED, task.isCompleted() ? 1 : 0);
        values.put(COLUMN_CREATED_DATE, task.getCreatedDate());
        values.put(COLUMN_DEADLINE, task.getDeadline());
        values.put(COLUMN_REMINDER_ENABLED, task.isReminderEnabled() ? 1 : 0);
        values.put(COLUMN_REMINDER_TIME, task.getReminderTime());

        long id = db.insert(TABLE_TASKS, null, values);
        db.close();
        return id;
    }

    public List<Task> getAllTasks(int limit, int offset) {
        List<Task> tasks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " ORDER BY " + COLUMN_ID + " DESC LIMIT ? OFFSET ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(limit), String.valueOf(offset)});

        if (cursor.moveToFirst()) {
            do {
                Task task = createTaskFromCursor(cursor);
                if (task != null) {
                    tasks.add(task);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tasks;
    }

    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " ORDER BY " + COLUMN_ID + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Task task = createTaskFromCursor(cursor);
                if (task != null) {
                    tasks.add(task);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tasks;
    }

    public List<Task> getTasksByTopic(String topic, int limit, int offset) {
        List<Task> tasks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " WHERE " + COLUMN_TOPIC + " = ? ORDER BY " + COLUMN_ID + " DESC LIMIT ? OFFSET ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{topic, String.valueOf(limit), String.valueOf(offset)});

        if (cursor.moveToFirst()) {
            do {
                Task task = createTaskFromCursor(cursor);
                if (task != null) {
                    tasks.add(task);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tasks;
    }

    public List<Task> getTasksByTopic(String topic) {
        List<Task> tasks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " WHERE " + COLUMN_TOPIC + " = ? ORDER BY " + COLUMN_ID + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{topic});

        if (cursor.moveToFirst()) {
            do {
                Task task = createTaskFromCursor(cursor);
                if (task != null) {
                    tasks.add(task);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tasks;
    }

    public Task getTaskById(int taskId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " WHERE " + COLUMN_ID + " = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(taskId)});

        Task task = null;
        if (cursor.moveToFirst()) {
            task = createTaskFromCursor(cursor);
        }

        cursor.close();
        db.close();
        return task;
    }

    public int updateTask(Task task) {
        android.util.Log.d("DatabaseHelper", "Updating task with ID: " + task.getId());
        android.util.Log.d("DatabaseHelper", "New title: " + task.getTitle());
        android.util.Log.d("DatabaseHelper", "New description: " + task.getDescription());
        android.util.Log.d("DatabaseHelper", "New topic: " + task.getTopic());

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, task.getTitle());
        values.put(COLUMN_DESCRIPTION, task.getDescription());
        values.put(COLUMN_TOPIC, task.getTopic());
        values.put(COLUMN_IS_COMPLETED, task.isCompleted() ? 1 : 0);
        values.put(COLUMN_DEADLINE, task.getDeadline());
        values.put(COLUMN_REMINDER_ENABLED, task.isReminderEnabled() ? 1 : 0);
        values.put(COLUMN_REMINDER_TIME, task.getReminderTime());

        int result = db.update(TABLE_TASKS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(task.getId())});

        android.util.Log.d("DatabaseHelper", "Update query result: " + result + " rows affected");

        db.close();
        return result;
    }

    public int deleteTask(int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASKS, COLUMN_ID + " = ?", new String[]{String.valueOf(taskId)});
        db.close();
        return taskId;
    }

    public List<String> getAllTopics() {
        List<String> topics = new ArrayList<>();
        String selectQuery = "SELECT DISTINCT " + COLUMN_TOPIC + " FROM " + TABLE_TASKS + " WHERE " + COLUMN_TOPIC + " IS NOT NULL";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                topics.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return topics;
    }

    public int getTotalTaskCount() {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_TASKS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public int getTaskCountByTopic(String topic) {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_TASKS + " WHERE " + COLUMN_TOPIC + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, new String[]{topic});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }
}

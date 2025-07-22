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
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_TASKS = "tasks";

    // Columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_TOPIC = "topic";
    private static final String COLUMN_IS_COMPLETED = "is_completed";
    private static final String COLUMN_CREATED_DATE = "created_date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_TASKS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT NOT NULL,"
                + COLUMN_DESCRIPTION + " TEXT,"
                + COLUMN_TOPIC + " TEXT,"
                + COLUMN_IS_COMPLETED + " INTEGER DEFAULT 0,"
                + COLUMN_CREATED_DATE + " TEXT"
                + ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        onCreate(db);
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

        long id = db.insert(TABLE_TASKS, null, values);
        db.close();
        return id;
    }

    @SuppressLint("Range")
    public List<Task> getAllTasks(int limit, int offset) {
        List<Task> tasks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " ORDER BY " + COLUMN_ID + " DESC LIMIT ? OFFSET ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(limit), String.valueOf(offset)});

        if (cursor.moveToFirst()) {
            do {
                Task task = new Task();
                task.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
                task.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
                task.setDescription(cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)));
                task.setTopic(cursor.getString(cursor.getColumnIndex(COLUMN_TOPIC)));
                task.setCompleted(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_COMPLETED)) == 1);
                task.setCreatedDate(cursor.getString(cursor.getColumnIndex(COLUMN_CREATED_DATE)));
                tasks.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tasks;
    }

    @SuppressLint("Range")
    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " ORDER BY " + COLUMN_ID + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Task task = new Task();
                task.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
                task.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
                task.setDescription(cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)));
                task.setTopic(cursor.getString(cursor.getColumnIndex(COLUMN_TOPIC)));
                task.setCompleted(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_COMPLETED)) == 1);
                task.setCreatedDate(cursor.getString(cursor.getColumnIndex(COLUMN_CREATED_DATE)));
                tasks.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tasks;
    }

    @SuppressLint("Range")
    public List<Task> getTasksByTopic(String topic, int limit, int offset) {
        List<Task> tasks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " WHERE " + COLUMN_TOPIC + " = ? ORDER BY " + COLUMN_ID + " DESC LIMIT ? OFFSET ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{topic, String.valueOf(limit), String.valueOf(offset)});

        if (cursor.moveToFirst()) {
            do {
                Task task = new Task();
                task.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
                task.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
                task.setDescription(cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)));
                task.setTopic(cursor.getString(cursor.getColumnIndex(COLUMN_TOPIC)));
                task.setCompleted(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_COMPLETED)) == 1);
                task.setCreatedDate(cursor.getString(cursor.getColumnIndex(COLUMN_CREATED_DATE)));
                tasks.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tasks;
    }

    @SuppressLint("Range")
    public List<Task> getTasksByTopic(String topic) {
        List<Task> tasks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " WHERE " + COLUMN_TOPIC + " = ? ORDER BY " + COLUMN_ID + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{topic});

        if (cursor.moveToFirst()) {
            do {
                Task task = new Task();
                task.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
                task.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
                task.setDescription(cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)));
                task.setTopic(cursor.getString(cursor.getColumnIndex(COLUMN_TOPIC)));
                task.setCompleted(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_COMPLETED)) == 1);
                task.setCreatedDate(cursor.getString(cursor.getColumnIndex(COLUMN_CREATED_DATE)));
                tasks.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tasks;
    }

    @SuppressLint("Range")
    public Task getTaskById(int taskId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " WHERE " + COLUMN_ID + " = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(taskId)});

        Task task = null;
        if (cursor.moveToFirst()) {
            task = new Task();
            task.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
            task.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
            task.setDescription(cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)));
            task.setTopic(cursor.getString(cursor.getColumnIndex(COLUMN_TOPIC)));
            task.setCompleted(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_COMPLETED)) == 1);
            task.setCreatedDate(cursor.getString(cursor.getColumnIndex(COLUMN_CREATED_DATE)));
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

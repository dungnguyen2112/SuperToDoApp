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
    private static final int DATABASE_VERSION = 5;
    private static final String TABLE_TASKS = "tasks";
    private static final String TABLE_TAGS = "tags";
    private static final String TABLE_TASK_TAGS = "task_tags";

    // Tasks table columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_TOPIC = "topic";
    private static final String COLUMN_IS_COMPLETED = "is_completed";
    private static final String COLUMN_CREATED_DATE = "created_date";
    private static final String COLUMN_DEADLINE = "deadline";
    private static final String COLUMN_REMINDER_ENABLED = "reminder_enabled";
    private static final String COLUMN_REMINDER_TIME = "reminder_time";
    private static final String COLUMN_COMPLETED_DATE = "completed_date";

    // Tags table columns
    private static final String COLUMN_TAG_ID = "tag_id";
    private static final String COLUMN_TAG_NAME = "tag_name";
    private static final String COLUMN_TAG_COLOR = "tag_color";

    // Task_Tags table columns
    private static final String COLUMN_TASK_ID = "task_id";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @SuppressLint("Range")
    private Task createTaskFromCursor(Cursor cursor) {
        Task task = new Task();

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

            int completedDateIndex = cursor.getColumnIndex(COLUMN_COMPLETED_DATE);
            if (completedDateIndex != -1) {
                task.setCompletedDate(cursor.getString(completedDateIndex));
            } else {
                task.setCompletedDate(null);
            }

            // Load tags for this task
            task.setTags(getTagsForTask(task.getId()));

        } catch (IllegalArgumentException e) {
            android.util.Log.e("DatabaseHelper", "Error reading cursor data: " + e.getMessage());
            return null;
        }
        
        return task;
    }

    @SuppressLint("Range")
    private Tag createTagFromCursor(Cursor cursor) {
        Tag tag = new Tag();
        try {
            tag.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TAG_ID)));
            tag.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG_NAME)));
            tag.setColor(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG_COLOR)));
        } catch (IllegalArgumentException e) {
            android.util.Log.e("DatabaseHelper", "Error reading tag cursor data: " + e.getMessage());
            return null;
        }
        return tag;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tasks table
        String createTasksTable = "CREATE TABLE " + TABLE_TASKS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT NOT NULL,"
                + COLUMN_DESCRIPTION + " TEXT,"
                + COLUMN_TOPIC + " TEXT,"
                + COLUMN_IS_COMPLETED + " INTEGER DEFAULT 0,"
                + COLUMN_CREATED_DATE + " TEXT,"
                + COLUMN_DEADLINE + " TEXT,"
                + COLUMN_REMINDER_ENABLED + " INTEGER DEFAULT 0,"
                + COLUMN_REMINDER_TIME + " TEXT,"
                + COLUMN_COMPLETED_DATE + " TEXT"
                + ")";
        db.execSQL(createTasksTable);

        // Create tags table
        String createTagsTable = "CREATE TABLE " + TABLE_TAGS + "("
                + COLUMN_TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TAG_NAME + " TEXT NOT NULL UNIQUE,"
                + COLUMN_TAG_COLOR + " TEXT NOT NULL"
                + ")";
        db.execSQL(createTagsTable);

        // Create task_tags junction table
        String createTaskTagsTable = "CREATE TABLE " + TABLE_TASK_TAGS + "("
                + COLUMN_TASK_ID + " INTEGER,"
                + COLUMN_TAG_ID + " INTEGER,"
                + "PRIMARY KEY (" + COLUMN_TASK_ID + ", " + COLUMN_TAG_ID + "),"
                + "FOREIGN KEY (" + COLUMN_TASK_ID + ") REFERENCES " + TABLE_TASKS + "(" + COLUMN_ID + ") ON DELETE CASCADE,"
                + "FOREIGN KEY (" + COLUMN_TAG_ID + ") REFERENCES " + TABLE_TAGS + "(" + COLUMN_TAG_ID + ") ON DELETE CASCADE"
                + ")";
        db.execSQL(createTaskTagsTable);

        // Insert some default tags
        insertDefaultTags(db);
    }

    private void insertDefaultTags(SQLiteDatabase db) {
        String[] defaultTags = {
                "Công việc|#2196F3",
                "Cá nhân|#4CAF50", 
                "Học tập|#FF9800",
                "Giải trí|#9C27B0",
                "Khẩn cấp|#F44336"
        };

        for (String tagData : defaultTags) {
            String[] parts = tagData.split("\\|");
            ContentValues values = new ContentValues();
            values.put(COLUMN_TAG_NAME, parts[0]);
            values.put(COLUMN_TAG_COLOR, parts[1]);
            db.insert(TABLE_TAGS, null, values);
        }
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
        if (oldVersion < 4) {
            // Add completed date column to existing table
            db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN " + COLUMN_COMPLETED_DATE + " TEXT");
        }
        if (oldVersion < 5) {
            // Create tags and task_tags tables
            String createTagsTable = "CREATE TABLE " + TABLE_TAGS + "("
                    + COLUMN_TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_TAG_NAME + " TEXT NOT NULL UNIQUE,"
                    + COLUMN_TAG_COLOR + " TEXT NOT NULL"
                    + ")";
            db.execSQL(createTagsTable);

            String createTaskTagsTable = "CREATE TABLE " + TABLE_TASK_TAGS + "("
                    + COLUMN_TASK_ID + " INTEGER,"
                    + COLUMN_TAG_ID + " INTEGER,"
                    + "PRIMARY KEY (" + COLUMN_TASK_ID + ", " + COLUMN_TAG_ID + "),"
                    + "FOREIGN KEY (" + COLUMN_TASK_ID + ") REFERENCES " + TABLE_TASKS + "(" + COLUMN_ID + ") ON DELETE CASCADE,"
                    + "FOREIGN KEY (" + COLUMN_TAG_ID + ") REFERENCES " + TABLE_TAGS + "(" + COLUMN_TAG_ID + ") ON DELETE CASCADE"
                    + ")";
            db.execSQL(createTaskTagsTable);

            // Insert default tags
            insertDefaultTags(db);
        }
    }

    // CRUD Operations for Tasks
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
        values.put(COLUMN_COMPLETED_DATE, task.getCompletedDate());

        long id = db.insert(TABLE_TASKS, null, values);
        
        // Add tags if task has any
        if (task.getTags() != null && !task.getTags().isEmpty()) {
            for (Tag tag : task.getTags()) {
                addTaskTag((int)id, tag.getId());
            }
        }
        
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
        values.put(COLUMN_COMPLETED_DATE, task.getCompletedDate());

        int result = db.update(TABLE_TASKS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(task.getId())});

        android.util.Log.d("DatabaseHelper", "Update query result: " + result + " rows affected");

        // Update tags for this task
        if (result > 0) {
            updateTaskTags(task.getId(), task.getTags());
        }

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

    // Statistics methods for StatisticsActivity
    public List<Task> getCompletedTasks() {
        List<Task> tasks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " WHERE " + COLUMN_IS_COMPLETED + " = 1 ORDER BY " + COLUMN_ID + " DESC";
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

    public List<Task> getTasksCompletedInPeriod(String startDate, String endDate) {
        List<Task> tasks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + 
                " WHERE " + COLUMN_IS_COMPLETED + " = 1 AND " + 
                COLUMN_CREATED_DATE + " >= ? AND " + COLUMN_CREATED_DATE + " <= ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{startDate, endDate});

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

    public int getCompletedTasksCount() {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_TASKS + " WHERE " + COLUMN_IS_COMPLETED + " = 1";
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

    public int getPendingTasksCount() {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_TASKS + " WHERE " + COLUMN_IS_COMPLETED + " = 0";
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

    public List<Task> getTasksWithDeadlines() {
        List<Task> tasks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS + " WHERE " + COLUMN_DEADLINE + " IS NOT NULL AND " + COLUMN_DEADLINE + " != ''";
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

    // CRUD Operations for Tags
    public long addTag(Tag tag) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TAG_NAME, tag.getName());
        values.put(COLUMN_TAG_COLOR, tag.getColor());
        long id = db.insert(TABLE_TAGS, null, values);
        db.close();
        return id;
    }

    public Tag getTagById(int tagId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_TAGS + " WHERE " + COLUMN_TAG_ID + " = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(tagId)});

        Tag tag = null;
        if (cursor.moveToFirst()) {
            tag = createTagFromCursor(cursor);
        }

        cursor.close();
        db.close();
        return tag;
    }

    public List<Tag> getAllTags() {
        List<Tag> tags = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TAGS + " ORDER BY " + COLUMN_TAG_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Tag tag = createTagFromCursor(cursor);
                if (tag != null) {
                    tags.add(tag);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tags;
    }

    public int updateTag(Tag tag) {
        android.util.Log.d("DatabaseHelper", "Updating tag with ID: " + tag.getId());
        android.util.Log.d("DatabaseHelper", "New name: " + tag.getName());
        android.util.Log.d("DatabaseHelper", "New color: " + tag.getColor());

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TAG_NAME, tag.getName());
        values.put(COLUMN_TAG_COLOR, tag.getColor());

        int result = db.update(TABLE_TAGS, values, COLUMN_TAG_ID + " = ?",
                new String[]{String.valueOf(tag.getId())});

        android.util.Log.d("DatabaseHelper", "Update query result: " + result + " rows affected");

        db.close();
        return result;
    }

    public int deleteTag(int tagId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TAGS, COLUMN_TAG_ID + " = ?", new String[]{String.valueOf(tagId)});
        db.close();
        return tagId;
    }

    // CRUD Operations for Task_Tags
    public void addTaskTag(int taskId, int tagId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_ID, taskId);
        values.put(COLUMN_TAG_ID, tagId);
        db.insert(TABLE_TASK_TAGS, null, values);
        db.close();
    }

    public void removeTaskTag(int taskId, int tagId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASK_TAGS, COLUMN_TASK_ID + " = ? AND " + COLUMN_TAG_ID + " = ?", new String[]{String.valueOf(taskId), String.valueOf(tagId)});
        db.close();
    }

    public List<Tag> getTagsForTask(int taskId) {
        List<Tag> tags = new ArrayList<>();
        String selectQuery = "SELECT t.* FROM " + TABLE_TAGS + " t JOIN " + TABLE_TASK_TAGS + " tt ON t." + COLUMN_TAG_ID + " = tt." + COLUMN_TAG_ID + " WHERE tt." + COLUMN_TASK_ID + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(taskId)});

        if (cursor.moveToFirst()) {
            do {
                Tag tag = createTagFromCursor(cursor);
                if (tag != null) {
                    tags.add(tag);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tags;
    }

    // Helper method to update tags for a task
    public void updateTaskTags(int taskId, List<Tag> newTags) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // Remove all existing tags for this task
        db.delete(TABLE_TASK_TAGS, COLUMN_TASK_ID + " = ?", new String[]{String.valueOf(taskId)});
        
        // Add new tags
        if (newTags != null && !newTags.isEmpty()) {
            for (Tag tag : newTags) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_TASK_ID, taskId);
                values.put(COLUMN_TAG_ID, tag.getId());
                db.insert(TABLE_TASK_TAGS, null, values);
            }
        }
        
        db.close();
    }

    // Helper method to check if tag name already exists
    public boolean isTagNameExists(String tagName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT COUNT(*) FROM " + TABLE_TAGS + " WHERE " + COLUMN_TAG_NAME + " = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{tagName});
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        
        return count > 0;
    }

    // Get tag by name
    public Tag getTagByName(String tagName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_TAGS + " WHERE " + COLUMN_TAG_NAME + " = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{tagName});

        Tag tag = null;
        if (cursor.moveToFirst()) {
            tag = createTagFromCursor(cursor);
        }

        cursor.close();
        db.close();
        return tag;
    }
}

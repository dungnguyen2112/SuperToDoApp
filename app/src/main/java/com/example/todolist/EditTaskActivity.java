package com.example.todolist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditTaskActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private ReminderAlarmManager reminderAlarmManager;
    private EditText editTitle, editDescription, editTopic;
    private CheckBox checkboxReminder;
    private Task currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme before calling super.onCreate()
        ThemeManager.getInstance(this).initializeTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        databaseHelper = new DatabaseHelper(this);
        reminderAlarmManager = new ReminderAlarmManager(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Task");
        }

        initViews();
        loadTaskData();
        setupListeners();
    }

    private void initViews() {
        editTitle = findViewById(R.id.edit_title);
        editDescription = findViewById(R.id.edit_description);
        editTopic = findViewById(R.id.edit_topic);
        checkboxReminder = findViewById(R.id.checkbox_reminder);
    }

    private void loadTaskData() {
        int taskId = getIntent().getIntExtra("task_id", -1);

        android.util.Log.d("EditTaskActivity", "Loading task data - ID: " + taskId);

        if (taskId != -1) {
            // Load complete task data from database
            currentTask = databaseHelper.getTaskById(taskId);
            
            if (currentTask != null) {
                // Set text in EditText fields
                editTitle.setText(currentTask.getTitle() != null ? currentTask.getTitle() : "");
                editDescription.setText(currentTask.getDescription() != null ? currentTask.getDescription() : "");
                editTopic.setText(currentTask.getTopic() != null ? currentTask.getTopic() : "");
                
                // Set reminder checkbox - simplified UI
                checkboxReminder.setChecked(currentTask.isReminderEnabled());

                android.util.Log.d("EditTaskActivity", "Task loaded successfully with ID: " + currentTask.getId());
            } else {
                android.util.Log.e("EditTaskActivity", "Task not found in database");
                Toast.makeText(this, "Error: Task not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            android.util.Log.e("EditTaskActivity", "Invalid task ID: " + taskId);
            Toast.makeText(this, "Error: Invalid task data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupListeners() {
        findViewById(R.id.button_save).setOnClickListener(v -> saveTask());
        findViewById(R.id.button_cancel).setOnClickListener(v -> showCancelConfirmation());
        
        // No additional setup needed for reminder checkbox - just enable/disable automatic reminders
    }

    private void showCancelConfirmation() {
        // Check if there are unsaved changes
        String currentTitle = editTitle.getText().toString().trim();
        String currentDescription = editDescription.getText().toString().trim();
        String currentTopicText = editTopic.getText().toString().trim();

        boolean hasChanges = !currentTitle.equals(currentTask.getTitle() != null ? currentTask.getTitle() : "") ||
                           !currentDescription.equals(currentTask.getDescription() != null ? currentTask.getDescription() : "") ||
                           !currentTopicText.equals(currentTask.getTopic() != null ? currentTask.getTopic() : "");

        if (hasChanges) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Discard Changes?")
                .setMessage("Are you sure you want to discard your changes?")
                .setPositiveButton("Discard", (dialog, which) -> finish())
                .setNegativeButton("Continue Editing", null)
                .show();
        } else {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            showCancelConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveTask() {
        String title = editTitle.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String topic = editTopic.getText().toString().trim();

        android.util.Log.d("EditTaskActivity", "Saving task - Title: " + title);

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter task title", Toast.LENGTH_SHORT).show();
            editTitle.requestFocus();
            return;
        }

        if (currentTask == null) {
            Toast.makeText(this, "Error: Task data not loaded properly", Toast.LENGTH_SHORT).show();
            android.util.Log.e("EditTaskActivity", "currentTask is null!");
            return;
        }

        // Update task data
        currentTask.setTitle(title);
        currentTask.setDescription(description);
        currentTask.setTopic(topic.isEmpty() ? null : topic);
        
        // Update reminder data - simplified (no specific time needed)
        currentTask.setReminderEnabled(checkboxReminder.isChecked());

        android.util.Log.d("EditTaskActivity", "Updating task with ID: " + currentTask.getId());

        try {
            int result = databaseHelper.updateTask(currentTask);
            android.util.Log.d("EditTaskActivity", "Update result: " + result);

            if (result > 0) {
                // Update alarm/reminder
                reminderAlarmManager.updateReminder(currentTask);
                
                // Update widget immediately after task update
                android.util.Log.d("EditTaskActivity", "Triggering widget update after task edit");
                TaskWidgetProvider.updateAllWidgets(this);
                
                Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();

                // Return updated data
                Intent resultIntent = new Intent();
                resultIntent.putExtra("updated_task_id", currentTask.getId());
                resultIntent.putExtra("updated_task_title", currentTask.getTitle());
                resultIntent.putExtra("updated_task_description", currentTask.getDescription());
                resultIntent.putExtra("updated_task_topic", currentTask.getTopic());
                resultIntent.putExtra("updated_task_date", currentTask.getCreatedDate());
                resultIntent.putExtra("updated_task_completed", currentTask.isCompleted());
                resultIntent.putExtra("task_updated", true);
                setResult(RESULT_OK, resultIntent);

                finish();
            } else {
                Toast.makeText(this, "Failed to update task", Toast.LENGTH_SHORT).show();
                android.util.Log.e("EditTaskActivity", "Update failed - no rows affected");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error updating task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            android.util.Log.e("EditTaskActivity", "Exception updating task", e);
        }
    }
    

}

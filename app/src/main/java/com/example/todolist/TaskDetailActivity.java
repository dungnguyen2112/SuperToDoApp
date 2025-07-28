package com.example.todolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.flexbox.FlexboxLayout;
import android.view.View;
import android.graphics.Color;

public class TaskDetailActivity extends AppCompatActivity {
    private static final int EDIT_TASK_REQUEST_CODE = 100;

    private DatabaseHelper databaseHelper;
    private ReminderAlarmManager reminderAlarmManager;
    private Task currentTask;

    // UI elements
    private TextView textTitle, textDescription, textTopic, textDate, textStatus, textDeadline, textDeadlineStatus;
    private CheckBox checkboxCompleted;
    private MaterialButton buttonEdit, buttonDelete;
    private CardView cardTags;
    private FlexboxLayout flexboxTags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme before calling super.onCreate()
        ThemeManager.getInstance(this).initializeTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        databaseHelper = new DatabaseHelper(this);
        reminderAlarmManager = new ReminderAlarmManager(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Task Details");
        }

        initViews();
        loadTaskData();
        setupListeners();
    }

    private void initViews() {
        textTitle = findViewById(R.id.text_title);
        textDescription = findViewById(R.id.text_description);
        textTopic = findViewById(R.id.text_topic);
        textDate = findViewById(R.id.text_date);
        textStatus = findViewById(R.id.text_status);
        textDeadline = findViewById(R.id.text_deadline);
        textDeadlineStatus = findViewById(R.id.text_deadline_status);
        checkboxCompleted = findViewById(R.id.checkbox_completed);
        buttonEdit = findViewById(R.id.button_edit);
        buttonDelete = findViewById(R.id.button_delete);
        cardTags = findViewById(R.id.card_tags);
        flexboxTags = findViewById(R.id.flexboxTags);
    }

    private void loadTaskData() {
        Intent intent = getIntent();

        // Get task ID from intent
        int taskId = intent.getIntExtra("task_id", -1);

        if (taskId == -1) {
            Toast.makeText(this, "Error: Invalid task data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load complete task data from database
        currentTask = databaseHelper.getTaskById(taskId);
        
        if (currentTask == null) {
            Toast.makeText(this, "Error: Task not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Update UI
        updateUI();
    }

    private void updateUI() {
        if (currentTask == null) return;

        textTitle.setText(currentTask.getTitle() != null ? currentTask.getTitle() : "No title");
        textDescription.setText(currentTask.getDescription() != null && !currentTask.getDescription().isEmpty()
            ? currentTask.getDescription() : "No description provided");

        if (currentTask.getTopic() != null && !currentTask.getTopic().isEmpty()) {
            textTopic.setText(currentTask.getTopic().toUpperCase());
            textTopic.setVisibility(android.view.View.VISIBLE);
        } else {
            textTopic.setText("NO TOPIC");
            textTopic.setVisibility(android.view.View.VISIBLE);
        }

        textDate.setText(currentTask.getCreatedDate() != null ? currentTask.getCreatedDate() : "Unknown date");

        // Display deadline information
        updateDeadlineDisplay();

        // Display tags
        updateTagsDisplay();

        checkboxCompleted.setChecked(currentTask.isCompleted());
        updateStatusText();
    }

    private void updateStatusText() {
        if (currentTask.isCompleted()) {
            textStatus.setText("Task completed");
        } else {
            textStatus.setText("Mark as completed");
        }
    }

    private void updateDeadlineDisplay() {
        if (currentTask.getDeadline() != null && !currentTask.getDeadline().isEmpty()) {
            textDeadline.setText(currentTask.getDeadline());
            textDeadline.setVisibility(android.view.View.VISIBLE);
            
            // Check if task is overdue or coming due soon
            updateDeadlineStatus();
        } else {
            textDeadline.setText("No deadline set");
            textDeadline.setVisibility(android.view.View.VISIBLE);
            textDeadlineStatus.setVisibility(android.view.View.GONE);
        }
    }

    private void updateDeadlineStatus() {
        if (currentTask.getDeadline() == null || currentTask.getDeadline().isEmpty()) {
            textDeadlineStatus.setVisibility(android.view.View.GONE);
            return;
        }

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            java.util.Date deadlineDate = sdf.parse(currentTask.getDeadline());
            java.util.Date now = new java.util.Date();
            
            long timeDiff = deadlineDate.getTime() - now.getTime();
            long daysDiff = timeDiff / (1000 * 60 * 60 * 24);
            long hoursDiff = timeDiff / (1000 * 60 * 60);
            
            if (timeDiff < 0) {
                // Overdue
                long daysOverdue = Math.abs(daysDiff);
                if (daysOverdue > 0) {
                    textDeadlineStatus.setText("Overdue by " + daysOverdue + " day(s)");
                } else {
                    long hoursOverdue = Math.abs(hoursDiff);
                    textDeadlineStatus.setText("Overdue by " + hoursOverdue + " hour(s)");
                }
                textDeadlineStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
                textDeadlineStatus.setVisibility(android.view.View.VISIBLE);
            } else if (daysDiff <= 1) {
                // Due within 1 day
                if (daysDiff == 0) {
                    textDeadlineStatus.setText("Due today!");
                } else {
                    textDeadlineStatus.setText("Due in " + hoursDiff + " hour(s)");
                }
                textDeadlineStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark, getTheme()));
                textDeadlineStatus.setVisibility(android.view.View.VISIBLE);
            } else if (daysDiff <= 7) {
                // Due within a week
                textDeadlineStatus.setText("Due in " + daysDiff + " day(s)");
                textDeadlineStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_dark, getTheme()));
                textDeadlineStatus.setVisibility(android.view.View.VISIBLE);
            } else {
                // More than a week away
                textDeadlineStatus.setVisibility(android.view.View.GONE);
            }
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            textDeadlineStatus.setVisibility(android.view.View.GONE);
        }
    }

    private void updateTagsDisplay() {
        flexboxTags.removeAllViews();

        if (currentTask.getTags() != null && !currentTask.getTags().isEmpty()) {
            cardTags.setVisibility(View.VISIBLE);

            for (Tag tag : currentTask.getTags()) {
                View tagChip = getLayoutInflater().inflate(R.layout.item_tag_chip, flexboxTags, false);
                
                View colorDot = tagChip.findViewById(R.id.viewTagColorDot);
                TextView tagName = tagChip.findViewById(R.id.tvTagChipName);
                View removeButton = tagChip.findViewById(R.id.ivRemoveTag);

                try {
                    colorDot.setBackgroundColor(Color.parseColor(tag.getColor()));
                } catch (Exception e) {
                    colorDot.setBackgroundColor(Color.GRAY);
                }
                
                tagName.setText(tag.getName());
                removeButton.setVisibility(View.GONE); // Don't show remove button in detail view

                flexboxTags.addView(tagChip);
            }
        } else {
            cardTags.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        // Checkbox listener
        checkboxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentTask != null) {
                currentTask.setCompleted(isChecked);
                
                // Set completed date when task is marked as completed
                if (isChecked) {
                    currentTask.setCompletedDate(java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
                } else {
                    currentTask.setCompletedDate(null);
                }
                
                updateStatusText();

                // Update in database
                int result = databaseHelper.updateTask(currentTask);
                if (result > 0) {
                    String message = isChecked ? "Task marked as completed" : "Task marked as incomplete";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                    // Handle reminders based on task completion status
                    if (isChecked) {
                        // Cancel all reminders when task is completed
                        reminderAlarmManager.cancelReminder(currentTask.getId());
                    } else if (currentTask.isReminderEnabled()) {
                        // Re-enable reminders if task is unmarked and has reminders enabled
                        reminderAlarmManager.setReminder(currentTask);
                    }

                    // Update widget immediately when task status changes
                    TaskWidgetProvider.updateAllWidgets(this);

                    // Send result back to MainActivity
                    setResultAndNotifyChange();
                } else {
                    // Revert checkbox if update failed
                    checkboxCompleted.setChecked(!isChecked);
                    currentTask.setCompleted(!isChecked);
                    updateStatusText();
                    Toast.makeText(this, "Failed to update task status", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Edit button listener
        buttonEdit.setOnClickListener(v -> openEditActivity());

        // Delete button listener
        buttonDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void openEditActivity() {
        Intent intent = new Intent(this, EditTaskActivity.class);
        intent.putExtra("task_id", currentTask.getId());
        intent.putExtra("task_title", currentTask.getTitle());
        intent.putExtra("task_description", currentTask.getDescription());
        intent.putExtra("task_topic", currentTask.getTopic());
        intent.putExtra("task_date", currentTask.getCreatedDate());
        intent.putExtra("task_completed", currentTask.isCompleted());

        startActivityForResult(intent, EDIT_TASK_REQUEST_CODE);
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deleteTask())
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    private void deleteTask() {
        if (currentTask == null) {
            Toast.makeText(this, "Error: No task to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Cancel reminder alarm before deleting
            reminderAlarmManager.cancelReminder(currentTask.getId());
            
            int result = databaseHelper.deleteTask(currentTask.getId());
            if (result > 0) {
                // Update widget immediately after task deletion
                TaskWidgetProvider.updateAllWidgets(this);
                
                Toast.makeText(this, "Task deleted successfully", Toast.LENGTH_SHORT).show();

                // Send result back to MainActivity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("task_deleted", true);
                resultIntent.putExtra("deleted_task_id", currentTask.getId());
                setResult(RESULT_OK, resultIntent);

                finish();
            } else {
                Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error deleting task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            android.util.Log.e("TaskDetailActivity", "Error deleting task", e);
        }
    }

    private void setResultAndNotifyChange() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("task_updated", true);
        resultIntent.putExtra("updated_task_id", currentTask.getId());
        resultIntent.putExtra("updated_task_title", currentTask.getTitle());
        resultIntent.putExtra("updated_task_description", currentTask.getDescription());
        resultIntent.putExtra("updated_task_topic", currentTask.getTopic());
        resultIntent.putExtra("updated_task_date", currentTask.getCreatedDate());
        resultIntent.putExtra("updated_task_completed", currentTask.isCompleted());
        setResult(RESULT_OK, resultIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_TASK_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Task was updated in EditTaskActivity
            if (data.getBooleanExtra("task_updated", false)) {
                // Reload complete task data from database to get all updated fields including deadline and reminder
                int taskId = currentTask.getId();
                currentTask = databaseHelper.getTaskById(taskId);
                
                if (currentTask != null) {
                    // Update UI with new data
                    updateUI();

                    // Notify MainActivity about the changes
                    setResultAndNotifyChange();

                    Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error: Could not reload task data", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

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
import com.google.android.material.button.MaterialButton;

public class TaskDetailActivity extends AppCompatActivity {
    private static final int EDIT_TASK_REQUEST_CODE = 100;

    private DatabaseHelper databaseHelper;
    private Task currentTask;

    // UI elements
    private TextView textTitle, textDescription, textTopic, textDate, textStatus;
    private CheckBox checkboxCompleted;
    private MaterialButton buttonEdit, buttonDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme before calling super.onCreate()
        ThemeManager.getInstance(this).initializeTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        databaseHelper = new DatabaseHelper(this);

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
        checkboxCompleted = findViewById(R.id.checkbox_completed);
        buttonEdit = findViewById(R.id.button_edit);
        buttonDelete = findViewById(R.id.button_delete);
    }

    private void loadTaskData() {
        Intent intent = getIntent();

        // Get task data from intent
        int taskId = intent.getIntExtra("task_id", -1);
        String taskTitle = intent.getStringExtra("task_title");
        String taskDescription = intent.getStringExtra("task_description");
        String taskTopic = intent.getStringExtra("task_topic");
        String taskDate = intent.getStringExtra("task_date");
        boolean taskCompleted = intent.getBooleanExtra("task_completed", false);

        if (taskId == -1) {
            Toast.makeText(this, "Error: Invalid task data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Create task object
        currentTask = new Task();
        currentTask.setId(taskId);
        currentTask.setTitle(taskTitle);
        currentTask.setDescription(taskDescription);
        currentTask.setTopic(taskTopic);
        currentTask.setCreatedDate(taskDate);
        currentTask.setCompleted(taskCompleted);

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

    private void setupListeners() {
        // Checkbox listener
        checkboxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentTask != null) {
                currentTask.setCompleted(isChecked);
                updateStatusText();

                // Update in database
                int result = databaseHelper.updateTask(currentTask);
                if (result > 0) {
                    String message = isChecked ? "Task marked as completed" : "Task marked as incomplete";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

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
            int result = databaseHelper.deleteTask(currentTask.getId());
            if (result > 0) {
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
            // Update current task with edited data
            if (data.getBooleanExtra("task_updated", false)) {
                currentTask.setTitle(data.getStringExtra("updated_task_title"));
                currentTask.setDescription(data.getStringExtra("updated_task_description"));
                currentTask.setTopic(data.getStringExtra("updated_task_topic"));
                currentTask.setCreatedDate(data.getStringExtra("updated_task_date"));
                currentTask.setCompleted(data.getBooleanExtra("updated_task_completed", false));

                // Update UI with new data
                updateUI();

                // Notify MainActivity about the changes
                setResultAndNotifyChange();

                Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();
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
